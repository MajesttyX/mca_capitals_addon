package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class CapitalDialogueChronicleLogic {

    static final int RECENT_REPORT_DAYS = 7;
    static final int VERY_RECENT_DAYS = 2;
    static final int MAX_CANDIDATE_EVENTS = 5;

    private CapitalDialogueChronicleLogic() {
    }

    static List<CapitalDialogueEventModels.ChronicleEvent> findRecentNotableEvents(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return findRecentNotableEvents(level, capital, capital == null ? List.of() : capital.getChronicleEntries());
    }

    static List<CapitalDialogueEventModels.ChronicleEvent> findRecentNotableEvents(
            ServerLevel level,
            List<String> entries
    ) {
        return findRecentNotableEvents(level, null, entries);
    }

    private static List<CapitalDialogueEventModels.ChronicleEvent> findRecentNotableEvents(
            ServerLevel level,
            CapitalRecord capital,
            List<String> entries
    ) {
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        List<CapitalDialogueEventModels.ChronicleEvent> result = new ArrayList<>();

        for (int i = entries.size() - 1; i >= 0 && result.size() < MAX_CANDIDATE_EVENTS; i--) {
            CapitalDialogueEventModels.ChronicleEvent parsed = parseChronicleEntry(entries.get(i));
            if (parsed == null) {
                continue;
            }

            if (currentDay - parsed.day() > RECENT_REPORT_DAYS) {
                continue;
            }

            String cleaned = CapitalDialogueTextLogic.sanitizeChronicleText(parsed.text());
            CapitalDialogueEventModels.EventType type = classifyEvent(level, capital, cleaned);
            if (type == CapitalDialogueEventModels.EventType.NONE) {
                continue;
            }

            result.add(new CapitalDialogueEventModels.ChronicleEvent(parsed.day(), cleaned, type));
        }

        return result;
    }

    static CapitalDialogueEventModels.ChronicleEvent findLatestNotableEvent(
            ServerLevel level,
            CapitalRecord capital
    ) {
        List<CapitalDialogueEventModels.ChronicleEvent> events = findRecentNotableEvents(level, capital);
        return events.isEmpty() ? null : events.get(0);
    }

    static CapitalDialogueEventModels.ChronicleEvent findLatestNotableEvent(
            ServerLevel level,
            List<String> entries
    ) {
        List<CapitalDialogueEventModels.ChronicleEvent> events = findRecentNotableEvents(level, entries);
        return events.isEmpty() ? null : events.get(0);
    }

    static CapitalDialogueEventModels.ChronicleEvent parseChronicleEntry(String entry) {
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
            return new CapitalDialogueEventModels.ChronicleEvent(day, text, CapitalDialogueEventModels.EventType.NONE);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static CapitalDialogueEventModels.EventType classifyEvent(String text) {
        return classifyEvent(null, null, text);
    }

    static CapitalDialogueEventModels.EventType classifyEvent(ServerLevel level, CapitalRecord capital, String text) {
        String normalized = normalize(text);

        if (normalized.contains("was entered into the dynastic record")) {
            return isCurrentCrownChildBirth(level, capital, text)
                    ? CapitalDialogueEventModels.EventType.CROWN_CHILD_BORN
                    : CapitalDialogueEventModels.EventType.ROYAL_BIRTH;
        }

        if (normalized.contains("was named heir apparent") || normalized.contains("was named heir")) {
            return CapitalDialogueEventModels.EventType.HEIR_APPARENT_NAMED;
        }

        if (normalized.contains("rose to capital status")
                || normalized.contains("was acclaimed as king of")
                || normalized.contains("was acclaimed as queen of")) {
            return CapitalDialogueEventModels.EventType.CAPITAL_FOUNDED;
        }

        if (normalized.contains("was married to") || normalized.contains("were married in")) {
            return CapitalDialogueEventModels.EventType.ROYAL_MARRIAGE;
        }

        if (normalized.contains("mourning period in") && normalized.contains("came to an end")) {
            return CapitalDialogueEventModels.EventType.MOURNING_ENDED;
        }

        if (normalized.contains("mourning was declared")
                || normalized.contains("entered mourning")
                || normalized.contains(" died")
                || normalized.startsWith("died")) {
            return CapitalDialogueEventModels.EventType.SOVEREIGN_DEATH;
        }

        if (normalized.contains("claimed the throne as")) {
            return CapitalDialogueEventModels.EventType.THRONE_SEIZED;
        }

        if (normalized.contains("was disinherited")) {
            return CapitalDialogueEventModels.EventType.DISINHERITED;
        }

        if (normalized.contains("was legitimized")) {
            return CapitalDialogueEventModels.EventType.LEGITIMIZED;
        }

        if (normalized.contains("abdicated the throne")) {
            return CapitalDialogueEventModels.EventType.ABDICATION;
        }

        if (normalized.contains("inherited the throne")) {
            return CapitalDialogueEventModels.EventType.PEACEFUL_TRANSFER;
        }

        if (normalized.contains("was elevated to the ducal rank")
                || normalized.contains("was raised to ducal rank")
                || normalized.contains("was granted the ducal rank")) {
            return CapitalDialogueEventModels.EventType.NEW_DUKE_OR_DUCHESS;
        }

        if (normalized.contains("was appointed commander of the royal army")
                || normalized.contains("was appointed commander of the royal guard")) {
            return CapitalDialogueEventModels.EventType.LORD_COMMANDER_APPOINTED;
        }

        if (normalized.contains("was appointed hand of the")) {
            return CapitalDialogueEventModels.EventType.HAND_APPOINTED;
        }

        if (normalized.contains("was appointed grand maester of")) {
            return CapitalDialogueEventModels.EventType.GRAND_MAESTER_APPOINTED;
        }

        if (normalized.contains("was appointed court herald of")) {
            return CapitalDialogueEventModels.EventType.COURT_HERALD_APPOINTED;
        }

        if (normalized.contains("was named to the royal guard of")) {
            return CapitalDialogueEventModels.EventType.ROYAL_GUARD_APPOINTED;
        }

        if (normalized.contains("throne") || normalized.contains("crown") || normalized.contains("realm")) {
            return CapitalDialogueEventModels.EventType.GENERIC_NOTABLE;
        }

        return CapitalDialogueEventModels.EventType.NONE;
    }

    private static boolean isCurrentCrownChildBirth(ServerLevel level, CapitalRecord capital, String text) {
        if (level == null || capital == null || text == null || capital.getHeir() == null) {
            return false;
        }

        String bornName = extractSubjectBefore(text, " was entered into the dynastic record");
        if (bornName.isBlank()) {
            return false;
        }

        String heirName = resolveName(level, capital.getHeir());
        if (heirName.isBlank()) {
            return false;
        }

        return normalize(bornName).equals(normalize(heirName));
    }

    private static String extractSubjectBefore(String text, String marker) {
        String normalized = normalize(text);
        int index = normalized.indexOf(marker);
        if (index <= 0) {
            return "";
        }

        return CapitalDialogueTextLogic.sanitizeChronicleText(text.substring(0, index)).trim();
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity == null || entity.getName() == null) {
            return "";
        }

        return CapitalDialogueTextLogic.sanitizeChronicleText(entity.getName().getString()).trim();
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}