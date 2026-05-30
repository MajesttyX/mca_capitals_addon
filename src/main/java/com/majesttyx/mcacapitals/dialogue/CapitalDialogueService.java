package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalDialogueService {

    private static final long NEWS_BRANCH_COOLDOWN_TICKS = 20L * 60L;
    private static final long SAME_EVENT_COOLDOWN_TICKS = 20L * 60L * 4L;

    private static final int MCA_PHRASE_CAPITAL_LINE_CHANCE = 55;

    private static final Set<String> MCA_GREET_RANK_KEYS = Set.of(
            "dialogue.greet.mayor",
            "dialogue.greet.monarch"
    );

    private static final Map<UUID, VillagerNewsState> VILLAGER_NEWS_STATE = new HashMap<>();
    private static final Map<String, String> MCA_PHRASE_BUCKETS = buildMcaPhraseBuckets();

    private CapitalDialogueService() {
    }

    public static String maybeResolveCapitalNewsDialogueId(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = villagerEntity.getUUID();

        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null || capital.getChronicleEntries().isEmpty()) {
            return null;
        }

        VillagerNewsState state = VILLAGER_NEWS_STATE.computeIfAbsent(villagerId, ignored -> new VillagerNewsState());
        long now = level.getGameTime();

        if (now - state.lastNewsSpokenTick < NEWS_BRANCH_COOLDOWN_TICKS) {
            return null;
        }

        List<CapitalDialogueEventModels.ChronicleEvent> candidates =
                CapitalDialogueChronicleLogic.findRecentNotableEvents(level, capital);
        if (candidates.isEmpty()) {
            return null;
        }

        CapitalDialogueEventModels.ChronicleEvent event = pickEventForVillager(level, villagerId, candidates, state);
        if (event == null) {
            return null;
        }

        if (!shouldSpeakEvent(level, villagerId, event.day(), event.type())) {
            return null;
        }

        state.lastNewsSpokenTick = now;
        state.lastEventType = event.type();
        state.lastEventDay = event.day();

        return dialogueBucketFor(event.type());
    }

    public static String maybeResolvePlayerSovereignDialogueId(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villagerEntity.getUUID());
        if (capital == null) {
            return null;
        }

        UUID playerSovereignId = capital.getPlayerSovereignId();
        if (playerSovereignId == null || !playerSovereignId.equals(player.getUUID())) {
            return null;
        }

        return CapitalDialogueRuntime.GENERAL_PLAYER_SOVEREIGN;
    }

    public static String maybeResolveCapitalRankDialogueId(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villagerEntity.getUUID());
        if (capital == null) {
            return null;
        }

        return rankDialogueBucketFor(level, capital, villagerEntity.getUUID());
    }

    public static String maybeFormatMcaPhraseLine(ServerPlayer player, Entity villagerEntity, String phraseKey) {
        if (player == null || villagerEntity == null || phraseKey == null || phraseKey.isBlank()) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villagerEntity.getUUID());
        if (capital == null) {
            return null;
        }

        String bucket = MCA_PHRASE_BUCKETS.get(phraseKey);
        if (bucket == null) {
            return null;
        }

        if (!MCA_GREET_RANK_KEYS.contains(phraseKey)
                && level.random.nextInt(100) >= MCA_PHRASE_CAPITAL_LINE_CHANCE) {
            return null;
        }

        return CapitalDialogueRuntime.formatManagedRuntimeLine(
                CapitalDialogueRuntime.runtimeKeyForBucket(bucket),
                player,
                villagerEntity,
                level,
                capital
        );
    }

    public static String formatCapitalIdleEveningChatter(ServerPlayer player, Entity villagerEntity, CapitalRecord capital) {
        if (player == null || villagerEntity == null || capital == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return null;
        }

        return CapitalDialogueRuntime.formatManagedRuntimeLine(
                CapitalDialogueRuntime.runtimeKeyForBucket(CapitalDialogueRuntime.MCA_CAPITAL_IDLE_EVENING_CHATTER),
                player,
                villagerEntity,
                player.serverLevel(),
                capital
        );
    }

    private static Map<String, String> buildMcaPhraseBuckets() {
        Map<String, String> buckets = new HashMap<>();

        buckets.put("villager.warning", CapitalDialogueRuntime.MCA_VILLAGER_WARNING);
        buckets.put("villager.cant_find_bed", CapitalDialogueRuntime.MCA_VILLAGER_CANT_FIND_BED);
        buckets.put("villager.hurt", CapitalDialogueRuntime.MCA_VILLAGER_HURT);
        buckets.put("villager.sickness", CapitalDialogueRuntime.MCA_VILLAGER_SICKNESS);
        buckets.put("villager.scream", CapitalDialogueRuntime.MCA_VILLAGER_SCREAM);
        buckets.put("villager.attack", CapitalDialogueRuntime.MCA_VILLAGER_ATTACK);
        buckets.put("villager.support", CapitalDialogueRuntime.MCA_VILLAGER_SUPPORT);
        buckets.put("villager.support.retreat", CapitalDialogueRuntime.MCA_VILLAGER_SUPPORT_RETREAT);
        buckets.put("villager.retreat", CapitalDialogueRuntime.MCA_VILLAGER_RETREAT);
        buckets.put("villager.kill", CapitalDialogueRuntime.MCA_VILLAGER_KILL);

        buckets.put("interaction.sethome.success", CapitalDialogueRuntime.MCA_INTERACTION_SETHOME_SUCCESS);
        buckets.put("interaction.sethome.bedfail.blocked", CapitalDialogueRuntime.MCA_INTERACTION_SETHOME_BEDFAIL_BLOCKED);
        buckets.put("interaction.gohome.success", CapitalDialogueRuntime.MCA_INTERACTION_GOHOME_SUCCESS);

        buckets.put("welcome", CapitalDialogueRuntime.MCA_WELCOME);
        buckets.put("welcomeFoe", CapitalDialogueRuntime.MCA_WELCOMEFOE);

        buckets.put("spouse.dialogue.chat.success", CapitalDialogueRuntime.MCA_SPOUSE_DIALOGUE_CHAT_SUCCESS);
        buckets.put("spouse.dialogue.chat.fail", CapitalDialogueRuntime.MCA_SPOUSE_DIALOGUE_CHAT_FAIL);

        buckets.put("dialogue.main.morning", CapitalDialogueRuntime.MCA_DIALOGUE_MAIN_MORNING);
        buckets.put("dialogue.main.evening", CapitalDialogueRuntime.MCA_DIALOGUE_MAIN_EVENING);
        buckets.put("dialogue.main.night", CapitalDialogueRuntime.MCA_DIALOGUE_MAIN_NIGHT);

        buckets.put("dialogue.goaway", CapitalDialogueRuntime.MCA_DIALOGUE_GOAWAY);
        buckets.put("dialogue.greet", CapitalDialogueRuntime.MCA_DIALOGUE_GREET);

        buckets.put("dialogue.stay.success", CapitalDialogueRuntime.MCA_DIALOGUE_STAY_SUCCESS);
        buckets.put("dialogue.stay.no_space", CapitalDialogueRuntime.MCA_DIALOGUE_STAY_NO_SPACE);

        buckets.put("villager.grieving", CapitalDialogueRuntime.MCA_VILLAGER_GRIEVING);

        buckets.put("dialogue.greet.mayor", CapitalDialogueRuntime.MCA_DIALOGUE_GREET);
        buckets.put("dialogue.greet.monarch", CapitalDialogueRuntime.MCA_DIALOGUE_GREET);

        return Map.copyOf(buckets);
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord byTitle = CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byTitle != null) {
            return byTitle;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }

    private static CapitalDialogueEventModels.ChronicleEvent pickEventForVillager(
            ServerLevel level,
            UUID villagerId,
            List<CapitalDialogueEventModels.ChronicleEvent> candidates,
            VillagerNewsState state
    ) {
        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        long now = level.getGameTime();
        List<CapitalDialogueEventModels.ChronicleEvent> filtered = new ArrayList<>(candidates.size());

        boolean sameEventStillCooling =
                state.lastEventType != null
                        && now - state.lastNewsSpokenTick < SAME_EVENT_COOLDOWN_TICKS;

        if (sameEventStillCooling) {
            for (CapitalDialogueEventModels.ChronicleEvent candidate : candidates) {
                if (candidate.type() != state.lastEventType || candidate.day() != state.lastEventDay) {
                    filtered.add(candidate);
                }
            }
        }

        List<CapitalDialogueEventModels.ChronicleEvent> pool = filtered.isEmpty() ? candidates : filtered;

        if (pool.size() == 1) {
            return pool.get(0);
        }

        List<Integer> weights = new ArrayList<>(pool.size());
        int totalWeight = 0;
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);

        for (int i = 0; i < pool.size(); i++) {
            CapitalDialogueEventModels.ChronicleEvent event = pool.get(i);
            int weight = baseWeightForIndex(i);

            long age = Math.max(0L, currentDay - event.day());
            if (age <= CapitalDialogueChronicleLogic.VERY_RECENT_DAYS) {
                weight += 3;
            }

            if (event.type() == CapitalDialogueEventModels.EventType.SOVEREIGN_DEATH
                    || event.type() == CapitalDialogueEventModels.EventType.THRONE_SEIZED
                    || event.type() == CapitalDialogueEventModels.EventType.ABDICATION
                    || event.type() == CapitalDialogueEventModels.EventType.PEACEFUL_TRANSFER) {
                weight += 2;
            }

            if (event.type() == CapitalDialogueEventModels.EventType.ROYAL_MARRIAGE
                    || event.type() == CapitalDialogueEventModels.EventType.ROYAL_BIRTH
                    || event.type() == CapitalDialogueEventModels.EventType.COURT_HERALD_APPOINTED) {
                weight += 1;
            }

            if (state.lastEventType != null
                    && event.type() == state.lastEventType
                    && event.day() == state.lastEventDay) {
                weight = 1;
            }

            weight = Math.max(1, weight);
            weights.add(weight);
            totalWeight += weight;
        }

        int roll = Math.floorMod((villagerId.toString() + ":" + currentDay + ":eventPick").hashCode(), totalWeight);

        int cursor = 0;
        for (int i = 0; i < pool.size(); i++) {
            cursor += weights.get(i);
            if (roll < cursor) {
                return pool.get(i);
            }
        }

        return pool.get(0);
    }

    private static int baseWeightForIndex(int index) {
        return switch (index) {
            case 0 -> 6;
            case 1 -> 3;
            case 2 -> 2;
            default -> 1;
        };
    }

    private static boolean shouldSpeakEvent(
            ServerLevel level,
            UUID villagerId,
            long eventDay,
            CapitalDialogueEventModels.EventType type
    ) {
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        long age = Math.max(0L, currentDay - eventDay);

        int chance = age <= CapitalDialogueChronicleLogic.VERY_RECENT_DAYS ? 45 : 20;

        if (type == CapitalDialogueEventModels.EventType.SOVEREIGN_DEATH
                || type == CapitalDialogueEventModels.EventType.THRONE_SEIZED
                || type == CapitalDialogueEventModels.EventType.ABDICATION
                || type == CapitalDialogueEventModels.EventType.PEACEFUL_TRANSFER) {
            chance += 10;
        }

        int roll = Math.floorMod((villagerId.toString() + ":" + currentDay + ":" + type.name()).hashCode(), 100);
        return roll < chance;
    }

    private static String dialogueBucketFor(CapitalDialogueEventModels.EventType type) {
        return switch (type) {
            case HEIR_APPARENT_NAMED -> CapitalDialogueRuntime.NEWS_HEIR_APPARENT_NAMED;
            case CROWN_CHILD_BORN -> CapitalDialogueRuntime.NEWS_CROWN_CHILD_BORN;
            case CAPITAL_FOUNDED -> CapitalDialogueRuntime.NEWS_CAPITAL_FOUNDED;
            case ROYAL_MARRIAGE -> CapitalDialogueRuntime.NEWS_ROYAL_MARRIAGE;
            case SOVEREIGN_DEATH -> CapitalDialogueRuntime.NEWS_SOVEREIGN_DEATH;
            case THRONE_SEIZED -> CapitalDialogueRuntime.NEWS_THRONE_SEIZED;
            case DISINHERITED -> CapitalDialogueRuntime.NEWS_DISINHERITED;
            case LEGITIMIZED -> CapitalDialogueRuntime.NEWS_LEGITIMIZED;
            case ABDICATION -> CapitalDialogueRuntime.NEWS_ABDICATION;
            case NEW_DUKE_OR_DUCHESS -> CapitalDialogueRuntime.NEWS_NEW_DUKE_OR_DUCHESS;
            case LORD_COMMANDER_APPOINTED -> CapitalDialogueRuntime.NEWS_LORD_COMMANDER_APPOINTED;
            case HAND_APPOINTED -> CapitalDialogueRuntime.NEWS_HAND_APPOINTED;
            case GRAND_MAESTER_APPOINTED -> CapitalDialogueRuntime.NEWS_GRAND_MAESTER_APPOINTED;
            case ROYAL_GUARD_APPOINTED -> CapitalDialogueRuntime.NEWS_ROYAL_GUARD_APPOINTED;
            case PEACEFUL_TRANSFER -> CapitalDialogueRuntime.NEWS_PEACEFUL_TRANSFER;
            case ROYAL_BIRTH -> CapitalDialogueRuntime.NEWS_ROYAL_BIRTH;
            case COURT_HERALD_APPOINTED -> CapitalDialogueRuntime.NEWS_COURT_HERALD_APPOINTED;
            case MOURNING_ENDED -> CapitalDialogueRuntime.NEWS_MOURNING_ENDED;
            case GENERIC_NOTABLE, NONE -> null;
        };
    }

    private static String rankDialogueBucketFor(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (level == null || capital == null || villagerId == null) {
            return null;
        }

        String title = CapitalTitleResolver.getDisplayTitle(level, capital, villagerId);

        if ("High King".equals(title)
                || "High Queen".equals(title)
                || "King".equals(title)
                || "Queen".equals(title)) {
            return CapitalDialogueRuntime.RANK_SOVEREIGN;
        }

        if ("Crown Prince".equals(title)
                || "Crown Princess".equals(title)
                || "Heir Apparent".equals(title)) {
            return CapitalDialogueRuntime.RANK_HEIR;
        }

        if ("Hand of the King".equals(title)
                || "Hand of the Queen".equals(title)) {
            return CapitalDialogueRuntime.RANK_HAND;
        }

        if ("Grand Maester".equals(title)) {
            return CapitalDialogueRuntime.RANK_GRAND_MAESTER;
        }

        if ("Lord Commander".equals(title)) {
            return CapitalDialogueRuntime.RANK_LORD_COMMANDER;
        }

        if ("Duke".equals(title) || "Duchess".equals(title)) {
            return CapitalDialogueRuntime.RANK_DUKE_OR_DUCHESS;
        }

        if ("Lord".equals(title) || "Lady".equals(title)) {
            return CapitalDialogueRuntime.RANK_LORD_OR_LADY;
        }

        if ("Queen Consort".equals(title)
                || "King Consort".equals(title)
                || "Princess Consort".equals(title)
                || "Prince Consort".equals(title)) {
            return CapitalDialogueRuntime.RANK_ROYAL_CONSORT;
        }

        if ("Dowager Queen".equals(title)
                || "Dowager King".equals(title)
                || "Dowager Princess".equals(title)
                || "Dowager Prince".equals(title)) {
            return CapitalDialogueRuntime.RANK_ROYAL_DOWAGER;
        }

        if ("Prince".equals(title) || "Princess".equals(title)) {
            return CapitalDialogueRuntime.RANK_ROYAL_CHILD;
        }

        if ("Sir".equals(title) || "Dame".equals(title)) {
            return CapitalDialogueRuntime.RANK_KNIGHT;
        }

        if ("Commoner".equals(title)) {
            return CapitalDialogueRuntime.RANK_COMMONER;
        }

        return null;
    }

    private static final class VillagerNewsState {
        private long lastNewsSpokenTick = Long.MIN_VALUE;
        private CapitalDialogueEventModels.EventType lastEventType = null;
        private long lastEventDay = Long.MIN_VALUE;
    }
}