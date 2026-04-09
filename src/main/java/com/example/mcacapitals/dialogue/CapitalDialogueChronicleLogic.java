package com.example.mcacapitals.dialogue;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CapitalDialogueChronicleLogic {

    static final int RECENT_REPORT_DAYS = 7;
    static final int VERY_RECENT_DAYS = 2;
    static final int MAX_CANDIDATE_EVENTS = 3;

    private CapitalDialogueChronicleLogic() {
    }

    static List<CapitalDialogueEventModels.ChronicleEvent> findRecentNotableEvents(
            ServerLevel level,
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
            CapitalDialogueEventModels.EventType type = classifyEvent(cleaned);
            if (type == CapitalDialogueEventModels.EventType.NONE) {
                continue;
            }

            result.add(new CapitalDialogueEventModels.ChronicleEvent(parsed.day(), cleaned, type));
        }

        return result;
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
        String normalized = normalize(text);

        if (normalized.contains("was married to")) {
            return CapitalDialogueEventModels.EventType.MARRIAGE;
        }
        if (normalized.contains("mourning was declared") || normalized.contains("entered mourning")) {
            return CapitalDialogueEventModels.EventType.MOURNING_DECLARED;
        }
        if (normalized.contains("came to an end")) {
            return CapitalDialogueEventModels.EventType.MOURNING_ENDED;
        }
        if (normalized.contains("was named heir")) {
            return CapitalDialogueEventModels.EventType.HEIR_NAMED;
        }
        if (normalized.contains("was disinherited")) {
            return CapitalDialogueEventModels.EventType.DISINHERITED;
        }
        if (normalized.contains("was legitimized")) {
            return CapitalDialogueEventModels.EventType.LEGITIMIZED;
        }
        if (normalized.contains("claimed the throne as") || normalized.contains("abdicated the throne")) {
            return CapitalDialogueEventModels.EventType.THRONE_CHANGE;
        }
        if (normalized.contains("rose to capital status")) {
            return CapitalDialogueEventModels.EventType.CAPITAL_FOUNDED;
        }
        if (normalized.contains("died") || normalized.contains("inherited the throne")) {
            return CapitalDialogueEventModels.EventType.DEATH_OR_SUCCESSION;
        }

        if (normalized.contains("throne") || normalized.contains("crown") || normalized.contains("realm")) {
            return CapitalDialogueEventModels.EventType.GENERIC_NOTABLE;
        }

        return CapitalDialogueEventModels.EventType.NONE;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}