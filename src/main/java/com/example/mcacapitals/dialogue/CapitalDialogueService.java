package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalTitleResolver;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class CapitalDialogueService {

    private static final int RECENT_REPORT_DAYS = 7;
    private static final int VERY_RECENT_DAYS = 2;
    private static final int MAX_CANDIDATE_EVENTS = 3;

    private static final String[] KNOWN_TITLES = {
            "Queen Dowager",
            "Prince Consort",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Princess",
            "Prince",
            "Duchess",
            "Duke",
            "Lady",
            "Lord",
            "Dame",
            "Sir",
            "Queen",
            "King"
    };

    private CapitalDialogueService() {
    }

    public static String maybeBuildCapitalNewsSpeech(ServerPlayer player, Entity villagerEntity, String originalMessageKey) {
        if (player == null || villagerEntity == null || originalMessageKey == null || originalMessageKey.isBlank()) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return null;
        }

        if (!isLikelyGeneralDialogueKey(originalMessageKey)) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villagerEntity.getUUID());
        if (capital == null) {
            return null;
        }

        List<ChronicleEvent> candidates = findRecentNotableEvents(level, capital);
        if (candidates.isEmpty()) {
            return null;
        }

        ChronicleEvent event = pickEventForVillager(level, villagerEntity.getUUID(), candidates);
        if (event == null) {
            return null;
        }

        if (!shouldSpeakEvent(level, villagerEntity.getUUID(), event.day(), event.type())) {
            return null;
        }

        String line = buildVillagerLine(villagerEntity.getUUID(), event);
        if (line == null || line.isBlank()) {
            return null;
        }

        return CapitalDialogueSpeaker.formatVillagerSpeech(villagerEntity, line);
    }

    private static boolean isLikelyGeneralDialogueKey(String messageKey) {
        String key = messageKey.toLowerCase(Locale.ROOT);

        if (!key.startsWith("dialogue.")) {
            return false;
        }

        if (key.startsWith("dialogue.location.")) {
            return false;
        }

        if (key.contains(".warning")
                || key.contains(".hurt")
                || key.contains(".badly_hurt")
                || key.contains(".divorce")
                || key.contains(".procreate")
                || key.contains(".armor")
                || key.contains(".profession")
                || key.contains(".adopt")
                || key.contains(".ridehorse")) {
            return false;
        }

        return true;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord byTitle = CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byTitle != null) {
            return byTitle;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }

    private static List<ChronicleEvent> findRecentNotableEvents(ServerLevel level, CapitalRecord capital) {
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        List<String> entries = capital.getChronicleEntries();
        List<ChronicleEvent> result = new ArrayList<>();

        for (int i = entries.size() - 1; i >= 0 && result.size() < MAX_CANDIDATE_EVENTS; i--) {
            ChronicleEvent parsed = parseChronicleEntry(entries.get(i));
            if (parsed == null) {
                continue;
            }

            if (currentDay - parsed.day() > RECENT_REPORT_DAYS) {
                continue;
            }

            String cleaned = sanitizeChronicleText(parsed.text());
            EventType type = classifyEvent(cleaned);
            if (type == EventType.NONE) {
                continue;
            }

            result.add(new ChronicleEvent(parsed.day(), cleaned, type));
        }

        return result;
    }

    private static ChronicleEvent pickEventForVillager(ServerLevel level, UUID villagerId, List<ChronicleEvent> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        List<Integer> weights = new ArrayList<>(candidates.size());
        int totalWeight = 0;
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);

        for (int i = 0; i < candidates.size(); i++) {
            ChronicleEvent event = candidates.get(i);
            int weight = baseWeightForIndex(i);

            long age = Math.max(0L, currentDay - event.day());
            if (age <= VERY_RECENT_DAYS) {
                weight += 3;
            }

            if (event.type() == EventType.DEATH_OR_SUCCESSION || event.type() == EventType.MOURNING_DECLARED) {
                weight += 2;
            }

            if (event.type() == EventType.MARRIAGE) {
                weight += 1;
            }

            weight = Math.max(1, weight);
            weights.add(weight);
            totalWeight += weight;
        }

        int roll = Math.floorMod((villagerId.toString() + ":" + currentDay + ":eventPick").hashCode(), totalWeight);

        int cursor = 0;
        for (int i = 0; i < candidates.size(); i++) {
            cursor += weights.get(i);
            if (roll < cursor) {
                return candidates.get(i);
            }
        }

        return candidates.get(0);
    }

    private static int baseWeightForIndex(int index) {
        return switch (index) {
            case 0 -> 6;
            case 1 -> 3;
            case 2 -> 2;
            default -> 1;
        };
    }

    private static boolean shouldSpeakEvent(ServerLevel level, UUID villagerId, long eventDay, EventType type) {
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        long age = Math.max(0L, currentDay - eventDay);

        int chance;
        if (age <= VERY_RECENT_DAYS) {
            chance = 45;
        } else {
            chance = 20;
        }

        if (type == EventType.DEATH_OR_SUCCESSION || type == EventType.MOURNING_DECLARED) {
            chance += 10;
        }

        int roll = Math.floorMod((villagerId.toString() + ":" + currentDay + ":" + type.name()).hashCode(), 100);
        return roll < chance;
    }

    private static String buildVillagerLine(UUID villagerId, ChronicleEvent event) {
        String eventText = ensureSentence(event.text());
        CapitalDialogueKey key = dialogueKeyFor(event.type());
        int count = CapitalDialogueLibrary.getLineCount(key);

        if (count <= 0) {
            return eventText;
        }

        int index = Math.floorMod(
                (villagerId.toString() + "|" + event.day() + "|" + event.type().name()).hashCode(),
                count
        );

        return CapitalDialogueLibrary.getIndexedLine(key, index, eventText);
    }

    private static CapitalDialogueKey dialogueKeyFor(EventType type) {
        return switch (type) {
            case MARRIAGE -> CapitalDialogueKey.NEWS_MARRIAGE;
            case DEATH_OR_SUCCESSION -> CapitalDialogueKey.NEWS_DEATH_OR_SUCCESSION;
            case MOURNING_DECLARED -> CapitalDialogueKey.NEWS_MOURNING_DECLARED;
            case MOURNING_ENDED -> CapitalDialogueKey.NEWS_MOURNING_ENDED;
            case HEIR_NAMED -> CapitalDialogueKey.NEWS_HEIR_NAMED;
            case DISINHERITED -> CapitalDialogueKey.NEWS_DISINHERITED;
            case LEGITIMIZED -> CapitalDialogueKey.NEWS_LEGITIMIZED;
            case THRONE_CHANGE -> CapitalDialogueKey.NEWS_THRONE_CHANGE;
            case CAPITAL_FOUNDED -> CapitalDialogueKey.NEWS_CAPITAL_FOUNDED;
            case GENERIC_NOTABLE -> CapitalDialogueKey.NEWS_GENERIC_NOTABLE;
            case NONE -> CapitalDialogueKey.NEWS_GENERIC_NOTABLE;
        };
    }

    private static EventType classifyEvent(String text) {
        String normalized = normalize(text);

        if (normalized.contains("was married to")) {
            return EventType.MARRIAGE;
        }
        if (normalized.contains("mourning was declared") || normalized.contains("entered mourning")) {
            return EventType.MOURNING_DECLARED;
        }
        if (normalized.contains("came to an end")) {
            return EventType.MOURNING_ENDED;
        }
        if (normalized.contains("was named heir")) {
            return EventType.HEIR_NAMED;
        }
        if (normalized.contains("was disinherited")) {
            return EventType.DISINHERITED;
        }
        if (normalized.contains("was legitimized")) {
            return EventType.LEGITIMIZED;
        }
        if (normalized.contains("claimed the throne as") || normalized.contains("abdicated the throne")) {
            return EventType.THRONE_CHANGE;
        }
        if (normalized.contains("rose to capital status")) {
            return EventType.CAPITAL_FOUNDED;
        }
        if (normalized.contains("died") || normalized.contains("inherited the throne")) {
            return EventType.DEATH_OR_SUCCESSION;
        }

        if (normalized.contains("throne") || normalized.contains("crown") || normalized.contains("realm")) {
            return EventType.GENERIC_NOTABLE;
        }

        return EventType.NONE;
    }

    private static String sanitizeChronicleText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String result = text.trim();

        result = result.replace(", a prince/princess of the realm,", "");
        result = result.replace(", a prince/princess of the realm", "");
        result = result.replace(", a royal child of the realm,", "");
        result = result.replace(", a royal child of the realm", "");

        result = collapseDuplicateTitles(result);

        result = result.replaceAll("\\s{2,}", " ").trim();
        result = result.replace(" .", ".");

        return result;
    }

    private static String collapseDuplicateTitles(String text) {
        String result = text;

        boolean changed = true;
        while (changed) {
            changed = false;

            for (String title : KNOWN_TITLES) {
                String escaped = Pattern.quote(title);

                String doubled = "(?i)\\b" + escaped + "\\s+" + escaped + "\\b";
                String beforeName = "(?i)\\b" + escaped + "\\s+" + escaped + "\\s+([A-Z][A-Za-z'\\-]*)";
                String tripled = "(?i)\\b" + escaped + "\\s+" + escaped + "\\s+" + escaped + "\\b";

                String updated = result.replaceAll(tripled, title);
                updated = updated.replaceAll(doubled, title);
                updated = updated.replaceAll(beforeName, title + " $1");

                if (!updated.equals(result)) {
                    result = updated;
                    changed = true;
                }
            }
        }

        return result;
    }

    private static ChronicleEvent parseChronicleEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }

        String trimmed = entry.trim();
        if (!trimmed.startsWith("Day ")) {
            return null;
        }

        int colon = trimmed.indexOf(':');
        if (colon <= 4) {
            return null;
        }

        try {
            long day = Long.parseLong(trimmed.substring(4, colon).trim());
            String text = trimmed.substring(colon + 1).trim();
            if (text.isEmpty()) {
                return null;
            }
            return new ChronicleEvent(day, text, EventType.NONE);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String ensureSentence(String text) {
        if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) {
            return text;
        }
        return text + ".";
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private enum EventType {
        NONE,
        MARRIAGE,
        DEATH_OR_SUCCESSION,
        MOURNING_DECLARED,
        MOURNING_ENDED,
        HEIR_NAMED,
        DISINHERITED,
        LEGITIMIZED,
        THRONE_CHANGE,
        CAPITAL_FOUNDED,
        GENERIC_NOTABLE
    }

    private record ChronicleEvent(long day, String text, EventType type) {
    }
}