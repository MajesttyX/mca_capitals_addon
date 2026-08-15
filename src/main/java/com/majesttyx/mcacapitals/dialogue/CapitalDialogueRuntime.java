package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CapitalDialogueRuntime {

    public static final String RUNTIME_PREFIX = "mcacapitals_runtime_";

    public static final String GENERAL_ALL_RANKS = "mcacapitals_chat_capital_topic";
    public static final String GENERAL_FAIL = "mcacapitals_chat_capital_fail";
    public static final String GENERAL_SUCCESS = "mcacapitals_chat_general_success";
    public static final String GENERAL_PLAYER_SOVEREIGN = "mcacapitals_chat_general_player_sovereign";

    public static final String POLITICAL_HINT_FRIEND = "mcacapitals_chat_political_hint_friend";
    public static final String POLITICAL_HINT_ENEMY = "mcacapitals_chat_political_hint_enemy";
    public static final String POLITICAL_PRIVATE_FRIEND = "mcacapitals_chat_political_private_friend";
    public static final String POLITICAL_PRIVATE_ENEMY = "mcacapitals_chat_political_private_enemy";

    public static final String RANK_COMMONER = "mcacapitals_chat_rank_commoner";
    public static final String RANK_KNIGHT = "mcacapitals_chat_rank_knight";
    public static final String RANK_LORD_COMMANDER = "mcacapitals_chat_rank_lord_commander";
    public static final String RANK_LORD_OR_LADY = "mcacapitals_chat_rank_lord_or_lady";
    public static final String RANK_DUKE_OR_DUCHESS = "mcacapitals_chat_rank_duke_or_duchess";
    public static final String RANK_HEIR = "mcacapitals_chat_rank_heir";
    public static final String RANK_ROYAL_CHILD = "mcacapitals_chat_rank_royal_child";
    public static final String RANK_SOVEREIGN = "mcacapitals_chat_rank_sovereign";
    public static final String RANK_HAND = "mcacapitals_chat_rank_hand";
    public static final String RANK_GRAND_MAESTER = "mcacapitals_chat_rank_grand_maester";
    public static final String RANK_ROYAL_CONSORT = "mcacapitals_chat_rank_royal_consort";
    public static final String RANK_ROYAL_DOWAGER = "mcacapitals_chat_rank_royal_dowager";

    public static final String NEWS_HEIR_APPARENT_NAMED = "mcacapitals_chat_news_heir_apparent_named";
    public static final String NEWS_CROWN_CHILD_BORN = "mcacapitals_chat_news_crown_child_born";
    public static final String NEWS_CAPITAL_FOUNDED = "mcacapitals_chat_news_capital_founded";
    public static final String NEWS_ROYAL_MARRIAGE = "mcacapitals_chat_news_royal_marriage";
    public static final String NEWS_SOVEREIGN_DEATH = "mcacapitals_chat_news_sovereign_death";
    public static final String NEWS_THRONE_SEIZED = "mcacapitals_chat_news_throne_seized";
    public static final String NEWS_DISINHERITED = "mcacapitals_chat_news_disinherited";
    public static final String NEWS_LEGITIMIZED = "mcacapitals_chat_news_legitimized";
    public static final String NEWS_ABDICATION = "mcacapitals_chat_news_abdication";
    public static final String NEWS_NEW_DUKE_OR_DUCHESS = "mcacapitals_chat_news_new_duke_or_duchess";
    public static final String NEWS_LORD_COMMANDER_APPOINTED = "mcacapitals_chat_news_lord_commander_appointed";
    public static final String NEWS_HAND_APPOINTED = "mcacapitals_chat_news_hand_of_the_sovereign_appointed";
    public static final String NEWS_GRAND_MAESTER_APPOINTED = "mcacapitals_chat_news_grand_maester_appointed";
    public static final String NEWS_ROYAL_GUARD_APPOINTED = "mcacapitals_chat_news_royal_guard_appointed";
    public static final String NEWS_PEACEFUL_TRANSFER = "mcacapitals_chat_news_peaceful_transfer";
    public static final String NEWS_ROYAL_BIRTH = "mcacapitals_chat_news_royal_birth";
    public static final String NEWS_COURT_HERALD_APPOINTED = "mcacapitals_chat_news_court_herald_appointed";
    public static final String NEWS_MOURNING_ENDED = "mcacapitals_chat_news_mourning_ended";

    public static final String MCA_VILLAGER_WARNING = "mcacapitals_villager_warning";
    public static final String MCA_VILLAGER_CANT_FIND_BED = "mcacapitals_villager_cant_find_bed";
    public static final String MCA_VILLAGER_HURT = "mcacapitals_villager_hurt";
    public static final String MCA_VILLAGER_SICKNESS = "mcacapitals_villager_sickness";
    public static final String MCA_VILLAGER_SCREAM = "mcacapitals_villager_scream";
    public static final String MCA_VILLAGER_ATTACK = "mcacapitals_villager_attack";
    public static final String MCA_VILLAGER_SUPPORT = "mcacapitals_villager_support";
    public static final String MCA_VILLAGER_SUPPORT_RETREAT = "mcacapitals_villager_support_retreat";
    public static final String MCA_VILLAGER_RETREAT = "mcacapitals_villager_retreat";
    public static final String MCA_VILLAGER_KILL = "mcacapitals_villager_kill";
    public static final String MCA_INTERACTION_SETHOME_SUCCESS = "mcacapitals_interaction_sethome_success";
    public static final String MCA_INTERACTION_SETHOME_BEDFAIL_BLOCKED = "mcacapitals_interaction_sethome_bedfail_blocked";
    public static final String MCA_INTERACTION_GOHOME_SUCCESS = "mcacapitals_interaction_gohome_success";
    public static final String MCA_WELCOME = "mcacapitals_welcome";
    public static final String MCA_WELCOMEFOE = "mcacapitals_welcomeFoe";
    public static final String MCA_SPOUSE_DIALOGUE_CHAT_SUCCESS = "mcacapitals_spouse_dialogue_chat_success";
    public static final String MCA_SPOUSE_DIALOGUE_CHAT_FAIL = "mcacapitals_spouse_dialogue_chat_fail";
    public static final String MCA_DIALOGUE_MAIN_MORNING = "mcacapitals_dialogue_main_morning";
    public static final String MCA_DIALOGUE_MAIN_EVENING = "mcacapitals_dialogue_main_evening";
    public static final String MCA_DIALOGUE_MAIN_NIGHT = "mcacapitals_dialogue_main_night";
    public static final String MCA_DIALOGUE_GOAWAY = "mcacapitals_dialogue_goaway";
    public static final String MCA_DIALOGUE_GREET = "mcacapitals_dialogue_greet";
    public static final String MCA_DIALOGUE_STAY_SUCCESS = "mcacapitals_dialogue_stay_success";
    public static final String MCA_DIALOGUE_STAY_NO_SPACE = "mcacapitals_dialogue_stay_no_space";
    public static final String MCA_VILLAGER_GRIEVING = "mcacapitals_villager_grieving";

    private static final Map<UUID, LastLineState> LAST_LINE_STATE = new HashMap<>();
    private static final Set<String> WARNED_MISSING_FRIENDLY_POOLS = ConcurrentHashMap.newKeySet();

    private CapitalDialogueRuntime() {
    }

    public static boolean isManagedRuntimeKey(String key) {
        return key != null && key.startsWith(RUNTIME_PREFIX);
    }

    public static String runtimeKeyForBucket(String bucketKey) {
        return RUNTIME_PREFIX + bucketKey;
    }

    public static Component formatManagedRuntimeComponent(
            String runtimeKey,
            ServerPlayer player,
            Entity speaker,
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (runtimeKey == null || player == null || speaker == null || level == null || capital == null) {
            return null;
        }

        String bucketKey = runtimeKey.substring(RUNTIME_PREFIX.length());
        String personality = CapitalDialoguePersonalityResolver.resolve(speaker);
        if (personality == null) {
            return null;
        }

        List<String> translationKeys = personalityKeys(personality, bucketKey);
        String selectionBucket = personality + ":" + bucketKey;

        if (translationKeys.isEmpty()) {
            String friendly = CapitalDialoguePersonalityResolver.defaultPersonality();
            translationKeys = personalityKeys(friendly, bucketKey);
            selectionBucket = friendly + ":" + bucketKey;

            if (translationKeys.isEmpty()) {
                warnMissingFriendlyPool(bucketKey);
                return null;
            }
        }

        int index = pickLineIndex(
                speaker.getUUID(),
                selectionBucket,
                translationKeys.size(),
                level
        );

        CapitalDialogueContext context = CapitalDialogueContext.create(level, capital, player, speaker);
        return Component.translatable(translationKeys.get(index), context.arguments());
    }

    private static List<String> personalityKeys(String personality, String bucketKey) {
        return CapitalDialogueTranslationIndex.findDotNumberedKeys(
                personalityBase(personality, bucketKey)
        );
    }

    private static String personalityBase(String personality, String bucketKey) {
        if (POLITICAL_HINT_FRIEND.equals(bucketKey)) {
            return politicalBase(personality, "hint_friend");
        }
        if (POLITICAL_HINT_ENEMY.equals(bucketKey)) {
            return politicalBase(personality, "hint_enemy");
        }
        if (POLITICAL_PRIVATE_FRIEND.equals(bucketKey)) {
            return politicalBase(personality, "private_friend");
        }
        if (POLITICAL_PRIVATE_ENEMY.equals(bucketKey)) {
            return politicalBase(personality, "private_enemy");
        }

        String category = isRankBucket(bucketKey) ? "rank_offices" : "chat_chatter";
        return "mcacapitals.dialogue."
                + personality
                + "."
                + category
                + ".runtime."
                + bucketKey.toLowerCase(Locale.ROOT);
    }

    private static String politicalBase(String personality, String pool) {
        return "mcacapitals.dialogue."
                + personality
                + ".chat_chatter.political."
                + pool;
    }

    private static boolean isRankBucket(String bucketKey) {
        return bucketKey != null && bucketKey.startsWith("mcacapitals_chat_rank_");
    }

    private static int pickLineIndex(
            UUID speakerId,
            String bucketKey,
            int size,
            ServerLevel level
    ) {
        if (size <= 1) {
            return 0;
        }

        int index = level.random.nextInt(size);
        LastLineState state = LAST_LINE_STATE.computeIfAbsent(speakerId, ignored -> new LastLineState());

        if (bucketKey.equals(state.lastBucket) && index == state.lastIndex) {
            index = (index + 1) % size;
        }

        state.lastBucket = bucketKey;
        state.lastIndex = index;
        return index;
    }

    private static void warnMissingFriendlyPool(String bucketKey) {
        if (WARNED_MISSING_FRIENDLY_POOLS.add(bucketKey)) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Missing Friendly Talk of the Town dialogue pool '{}'",
                    bucketKey
            );
        }
    }

    private static final class LastLineState {
        private String lastBucket = "";
        private int lastIndex = -1;
    }
}
