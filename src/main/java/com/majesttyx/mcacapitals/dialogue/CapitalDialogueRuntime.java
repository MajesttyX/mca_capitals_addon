package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalDialogueRuntime {

    public static final String RUNTIME_PREFIX = "mcacapitals_runtime_";

    public static final String GENERAL_ALL_RANKS = "mcacapitals_chat_capital_topic";
    public static final String GENERAL_FAIL = "mcacapitals_chat_capital_fail";
    public static final String GENERAL_SUCCESS = "mcacapitals_chat_general_success";
    public static final String GENERAL_PLAYER_SOVEREIGN = "mcacapitals_chat_general_player_sovereign";

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

    public static final String NEWS_HEIR_NAMED = "mcacapitals_chat_news_heir_named";
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

    private static final Map<String, Integer> BUCKET_SIZES = buildBucketSizes();
    private static final List<String> KNOWN_TITLES = List.of(
            "High Queen", "High King", "Dowager Queen", "Dowager King",
            "Queen Consort", "King Consort", "Heir Apparent", "Crown Princess", "Crown Prince",
            "Dowager Princess", "Dowager Prince", "Princess Consort", "Prince Consort",
            "Hand of the Queen", "Hand of the King",
            "Grand Maester", "Maester", "Court Herald", "Lord Commander",
            "Dowager Duchess", "Dowager Duke", "Duchess", "Duke", "Princess", "Prince",
            "Lady", "Lord", "Dame", "Sir", "Queen", "King"
    );

    private static final Map<UUID, LastLineState> LAST_LINE_STATE = new HashMap<>();

    private CapitalDialogueRuntime() {
    }

    public static boolean isManagedRuntimeKey(String key) {
        return key != null && key.startsWith(RUNTIME_PREFIX);
    }

    public static String runtimeKeyForBucket(String bucketKey) {
        return RUNTIME_PREFIX + bucketKey;
    }

    public static String formatManagedRuntimeLine(
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
        int bucketSize = BUCKET_SIZES.getOrDefault(bucketKey, 0);
        if (bucketSize <= 0) {
            return null;
        }

        int index = pickLineIndex(speaker.getUUID(), bucketKey, bucketSize, level);
        String templateKey = "dialogue." + bucketKey + "_" + String.format("%02d", index + 1);
        String template = Language.getInstance().getOrDefault(templateKey);
        if (template == null || template.isBlank() || template.equals(templateKey)) {
            return null;
        }

        DialogueContext context = DialogueContext.create(level, capital, player, speaker);
        return applyTags(template, context);
    }

    private static int pickLineIndex(UUID speakerId, String bucketKey, int size, ServerLevel level) {
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

    private static String applyTags(String template, DialogueContext context) {
        String result = template;
        for (Map.Entry<String, String> entry : context.values.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Map<String, Integer> buildBucketSizes() {
        Map<String, Integer> sizes = new HashMap<>();

        sizes.put(GENERAL_ALL_RANKS, 29);
        sizes.put(GENERAL_FAIL, 25);
        sizes.put(GENERAL_SUCCESS, 47);
        sizes.put(GENERAL_PLAYER_SOVEREIGN, 6);

        sizes.put(RANK_COMMONER, 32);
        sizes.put(RANK_KNIGHT, 23);
        sizes.put(RANK_LORD_COMMANDER, 24);
        sizes.put(RANK_LORD_OR_LADY, 16);
        sizes.put(RANK_DUKE_OR_DUCHESS, 20);
        sizes.put(RANK_HEIR, 25);
        sizes.put(RANK_ROYAL_CHILD, 17);
        sizes.put(RANK_SOVEREIGN, 20);
        sizes.put(RANK_HAND, 25);
        sizes.put(RANK_GRAND_MAESTER, 13);
        sizes.put(RANK_ROYAL_CONSORT, 30);
        sizes.put(RANK_ROYAL_DOWAGER, 16);

        sizes.put(NEWS_HEIR_NAMED, 18);
        sizes.put(NEWS_CAPITAL_FOUNDED, 31);
        sizes.put(NEWS_ROYAL_MARRIAGE, 25);
        sizes.put(NEWS_SOVEREIGN_DEATH, 30);
        sizes.put(NEWS_THRONE_SEIZED, 26);
        sizes.put(NEWS_DISINHERITED, 14);
        sizes.put(NEWS_LEGITIMIZED, 13);
        sizes.put(NEWS_ABDICATION, 23);
        sizes.put(NEWS_NEW_DUKE_OR_DUCHESS, 16);
        sizes.put(NEWS_LORD_COMMANDER_APPOINTED, 16);
        sizes.put(NEWS_HAND_APPOINTED, 28);
        sizes.put(NEWS_GRAND_MAESTER_APPOINTED, 16);
        sizes.put(NEWS_ROYAL_GUARD_APPOINTED, 15);
        sizes.put(NEWS_PEACEFUL_TRANSFER, 17);
        sizes.put(NEWS_ROYAL_BIRTH, 27);
        sizes.put(NEWS_COURT_HERALD_APPOINTED, 22);
        sizes.put(NEWS_MOURNING_ENDED, 15);

        return sizes;
    }

    private static final class DialogueContext {
        private final Map<String, String> values = new HashMap<>();

        private static DialogueContext create(
                ServerLevel level,
                CapitalRecord capital,
                ServerPlayer player,
                Entity speaker
        ) {
            DialogueContext context = new DialogueContext();

            CapitalDialogueEventModels.ChronicleEvent latestEvent =
                    CapitalDialogueChronicleLogic.findLatestNotableEvent(level, capital.getChronicleEntries());

            context.put("{capital_name}", safeVillageName(level, capital));
            context.put("{capital_population}", Integer.toString(safePopulation(level, capital)));
            context.put("{mourning_status}", capital.isMourningActive() ? "in mourning" : "at peace again");

            context.put("{speaker_name}", resolveEntityName(level, capital, speaker == null ? null : speaker.getUUID()));
            context.put("{speaker_title}", speaker == null ? "Commoner" : safeDisplayTitle(level, capital, speaker.getUUID()));

            context.put("{sovereign_name}", resolveSovereignDisplayName(level, capital));
            context.put("{sovereign_title}", resolveSovereignTitle(level, capital));
            context.put("{consort_name}", resolveRoyalDisplayName(level, capital, capital.getConsort()));
            context.put("{consort_title}", safeDisplayTitle(level, capital, capital.getConsort()));
            context.put("{heir_name}", resolveRoyalDisplayName(level, capital, capital.getHeir()));
            context.put("{heir_title}", safeDisplayTitle(level, capital, capital.getHeir()));
            context.put("{hand_name}", resolveEntityName(level, capital, capital.getHand()));
            context.put("{hand_title}", safeDisplayTitle(level, capital, capital.getHand()));
            context.put("{commander_name}", resolveEntityName(level, capital, capital.getCommander()));
            context.put("{commander_title}", safeDisplayTitle(level, capital, capital.getCommander()));
            context.put("{herald_name}", resolveEntityName(level, capital, capital.getHerald()));
            context.put("{grand_maester_name}", resolveEntityName(level, capital, capital.getGrandMaester()));

            context.put("{royal_child_count}", Integer.toString(capital.getRoyalChildren().size()));
            context.put("{royal_household_count}", Integer.toString(capital.getRoyalHousehold().size()));

            context.put("{latest_event}", latestEvent == null ? "recent court business" : latestEvent.text());
            context.put("{latest_event_type}", latestEvent == null ? "court business" : latestEvent.type().name().toLowerCase());
            context.put("{days_since_latest_event}", latestEvent == null ? "0" : Long.toString(Math.max(0L, currentDay(level) - latestEvent.day())));

            return context;
        }

        private void put(String key, String value) {
            values.put(key, value == null || value.isBlank() ? fallbackFor(key) : value);
        }

        private static String fallbackFor(String key) {
            return switch (key) {
                case "{capital_name}" -> "the capital";
                case "{capital_population}" -> "0";
                case "{mourning_status}" -> "at peace again";
                case "{speaker_name}" -> "someone";
                case "{speaker_title}" -> "Commoner";
                case "{sovereign_name}" -> "the sovereign";
                case "{sovereign_title}" -> "Sovereign";
                case "{consort_name}" -> "the consort";
                case "{consort_title}" -> "Consort";
                case "{heir_name}" -> "the heir";
                case "{heir_title}" -> "Heir";
                case "{hand_name}" -> "the Hand";
                case "{hand_title}" -> "the Hand";
                case "{commander_name}" -> "the commander";
                case "{commander_title}" -> "Lord Commander";
                case "{herald_name}" -> "the herald";
                case "{grand_maester_name}" -> "the Grand Maester";
                case "{royal_child_count}" -> "0";
                case "{royal_household_count}" -> "0";
                case "{latest_event}" -> "recent court business";
                case "{latest_event_type}" -> "court business";
                case "{days_since_latest_event}" -> "0";
                default -> "";
            };
        }

        private static long currentDay(ServerLevel level) {
            return Math.max(1L, level.getDayTime() / 24000L + 1L);
        }

        private static int safePopulation(ServerLevel level, CapitalRecord capital) {
            Integer villageId = capital.getVillageId();
            return villageId == null ? 0 : Math.max(0, MCAIntegrationBridge.getVillagePopulation(level, villageId));
        }

        private static String safeVillageName(ServerLevel level, CapitalRecord capital) {
            Integer villageId = capital.getVillageId();
            String name = villageId == null ? null : MCAIntegrationBridge.getVillageName(level, villageId);
            return name == null || name.isBlank() ? "the capital" : name;
        }

        private static String resolveSovereignDisplayName(ServerLevel level, CapitalRecord capital) {
            if (capital.isPlayerSovereign()
                    && capital.getPlayerSovereignName() != null
                    && !capital.getPlayerSovereignName().isBlank()) {
                String playerName = stripKnownTitles(capital.getPlayerSovereignName());
                String playerTitle = capital.getPlayerSovereignId() == null
                        ? ""
                        : safeDisplayTitle(level, capital, capital.getPlayerSovereignId());
                return composeStyledName(playerTitle, playerName);
            }
            return resolveRoyalDisplayName(level, capital, capital.getSovereign());
        }

        private static String resolveSovereignTitle(ServerLevel level, CapitalRecord capital) {
            if (capital.isPlayerSovereign() && capital.getPlayerSovereignId() != null) {
                return safeDisplayTitle(level, capital, capital.getPlayerSovereignId());
            }
            return safeDisplayTitle(level, capital, capital.getSovereign());
        }

        private static String resolveRoyalDisplayName(ServerLevel level, CapitalRecord capital, UUID id) {
            String name = resolveEntityName(level, capital, id);
            String title = safeDisplayTitle(level, capital, id);
            return composeStyledName(title, name);
        }

        private static String composeStyledName(String title, String name) {
            boolean hasTitle = title != null && !title.isBlank();
            boolean hasName = name != null && !name.isBlank();

            if (hasTitle && hasName) {
                return title + " " + name;
            }
            if (hasTitle) {
                return title;
            }
            if (hasName) {
                return name;
            }
            return "";
        }

        private static String resolveEntityName(ServerLevel level, CapitalRecord capital, UUID id) {
            if (id == null) {
                return "";
            }

            if (capital.isPlayerSovereign()
                    && capital.getPlayerSovereignId() != null
                    && capital.getPlayerSovereignId().equals(id)
                    && capital.getPlayerSovereignName() != null
                    && !capital.getPlayerSovereignName().isBlank()) {
                return stripKnownTitles(capital.getPlayerSovereignName());
            }

            if (level.getServer() != null) {
                ServerPlayer onlinePlayer = level.getServer().getPlayerList().getPlayer(id);
                if (onlinePlayer != null) {
                    return stripKnownTitles(onlinePlayer.getName().getString());
                }
            }

            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
            if (entity != null && entity.getName() != null) {
                return stripKnownTitles(entity.getName().getString());
            }

            return "";
        }

        private static String safeDisplayTitle(ServerLevel level, CapitalRecord capital, UUID id) {
            if (id == null) {
                return "";
            }
            String title = CapitalTitleResolver.getDisplayTitle(level, capital, id);
            return title == null || title.isBlank() || "None".equals(title) ? "" : title;
        }

        private static String stripKnownTitles(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }

            String result = value.trim();

            if (result.endsWith(" of the Kingsguard")) {
                result = result.substring(0, result.length() - " of the Kingsguard".length()).trim();
            }
            if (result.endsWith(" of the Queensguard")) {
                result = result.substring(0, result.length() - " of the Queensguard".length()).trim();
            }

            boolean changed = true;
            while (changed) {
                changed = false;
                for (String title : KNOWN_TITLES) {
                    String prefix = title + " ";
                    if (result.startsWith(prefix)) {
                        result = result.substring(prefix.length()).trim();
                        changed = true;
                        break;
                    }
                }
            }

            return result;
        }
    }

    private static final class LastLineState {
        private String lastBucket = "";
        private int lastIndex = -1;
    }
}