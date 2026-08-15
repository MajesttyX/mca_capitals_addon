package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEntry;
import com.majesttyx.mcacapitals.capital.CapitalChronicleEventType;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
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

            CapitalChronicleEventType type = parsed.semantic()
                    ? parsed.type()
                    : classifyEvent(level, capital, parsed.text().getString());
            if (type == CapitalChronicleEventType.NONE) {
                continue;
            }

            result.add(new CapitalDialogueEventModels.ChronicleEvent(
                    parsed.day(),
                    parsed.text(),
                    type,
                    parsed.semantic()
            ));
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

        CapitalChronicleEntry semantic = CapitalChronicleService.decodeSemanticEntry(entry);
        if (semantic != null) {
            return new CapitalDialogueEventModels.ChronicleEvent(
                    semantic.day(),
                    semantic.render(),
                    semantic.type(),
                    true
            );
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
            return new CapitalDialogueEventModels.ChronicleEvent(
                    day,
                    Component.literal(text),
                    CapitalChronicleEventType.NONE,
                    false
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static CapitalChronicleEventType classifyEvent(String text) {
        return classifyEvent(null, null, text);
    }

    static CapitalChronicleEventType classifyEvent(ServerLevel level, CapitalRecord capital, String text) {
        String normalized = normalize(text);

        if (normalized.contains("was entered into the dynastic record")) {
            return isCurrentCrownChildBirth(level, capital, text)
                    ? CapitalChronicleEventType.CROWN_CHILD_BORN
                    : CapitalChronicleEventType.ROYAL_BIRTH;
        }

        if (normalized.contains("was named heir apparent") || normalized.contains("was named heir")) {
            return CapitalChronicleEventType.HEIR_APPARENT_NAMED;
        }

        if (normalized.contains("rose to capital status")
                || normalized.contains("was acclaimed as king of")
                || normalized.contains("was acclaimed as queen of")) {
            return CapitalChronicleEventType.CAPITAL_FOUNDED;
        }

        if (normalized.contains("was married to") || normalized.contains("were married in")) {
            return CapitalChronicleEventType.ROYAL_MARRIAGE;
        }

        if (normalized.contains("mourning period") && normalized.contains("came to an end")) {
            return CapitalChronicleEventType.MOURNING_ENDED;
        }

        if (normalized.contains("died")
                && (normalized.contains("sovereign")
                || normalized.contains("king")
                || normalized.contains("queen")
                || normalized.contains("court entered mourning"))) {
            return CapitalChronicleEventType.SOVEREIGN_DEATH;
        }

        if (normalized.contains("seized the throne") || normalized.contains("claimed the throne")) {
            return CapitalChronicleEventType.THRONE_SEIZED;
        }

        if (normalized.contains("was disinherited")) {
            return CapitalChronicleEventType.DISINHERITED;
        }

        if (normalized.contains("was legitimized")) {
            return CapitalChronicleEventType.LEGITIMIZED;
        }

        if (normalized.contains("abdicated the throne")) {
            return CapitalChronicleEventType.ABDICATION;
        }

        if (normalized.contains("inherited the throne")
                || normalized.contains("peacefully transferred the crown")) {
            return CapitalChronicleEventType.PEACEFUL_TRANSFER;
        }

        if (normalized.contains("was elevated to the ducal rank")
                || normalized.contains("was raised to ducal rank")
                || normalized.contains("was granted the ducal rank")
                || normalized.contains("was raised to the dignity of duchess")
                || normalized.contains("was raised to the dignity of duke")) {
            return CapitalChronicleEventType.NEW_DUKE_OR_DUCHESS;
        }

        if (normalized.contains("was appointed commander of the royal army")
                || normalized.contains("was appointed commander of the royal guard")
                || normalized.contains("was appointed commander of the army")) {
            return CapitalChronicleEventType.LORD_COMMANDER_APPOINTED;
        }

        if (normalized.contains("was appointed hand of the")
                || normalized.contains("was appointed hand of the crown")) {
            return CapitalChronicleEventType.HAND_APPOINTED;
        }

        if (normalized.contains("was appointed grand maester of")) {
            return CapitalChronicleEventType.GRAND_MAESTER_APPOINTED;
        }

        if (normalized.contains("was appointed court herald of")
                || normalized.contains("now serves as court herald of")) {
            return CapitalChronicleEventType.COURT_HERALD_APPOINTED;
        }

        if (normalized.contains("was named to the royal guard of")) {
            return CapitalChronicleEventType.ROYAL_GUARD_APPOINTED;
        }

        if (normalized.contains("throne") || normalized.contains("crown") || normalized.contains("realm")) {
            return CapitalChronicleEventType.GENERIC_NOTABLE;
        }

        return CapitalChronicleEventType.NONE;
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
