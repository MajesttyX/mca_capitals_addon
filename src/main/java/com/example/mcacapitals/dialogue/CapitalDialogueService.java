package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalTitleResolver;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.mca.cobalt.network.NetworkHandler;
import net.mca.entity.VillagerEntityMCA;
import net.mca.network.s2c.InteractionDialogueQuestionResponse;
import net.mca.network.s2c.InteractionDialogueResponse;
import net.mca.resources.Dialogues;
import net.mca.resources.data.dialogue.Question;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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

    public static boolean tryOpenCapitalNewsDialogue(ServerPlayer player, VillagerEntityMCA villager) {
        if (player == null || villager == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, villager.getUUID());
        if (capital == null) {
            return false;
        }

        List<ChronicleEvent> candidates = findRecentNotableEvents(level, capital);
        if (candidates.isEmpty()) {
            return false;
        }

        ChronicleEvent event = pickEventForVillager(level, villager.getUUID(), candidates);
        if (event == null) {
            return false;
        }

        if (!shouldSpeakEvent(level, villager.getUUID(), event.day(), event.type())) {
            return false;
        }

        String line = buildVillagerLine(villager.getUUID(), event);
        if (line == null || line.isBlank()) {
            return false;
        }

        Question mainQuestion = Dialogues.getInstance().getQuestion("main");
        if (mainQuestion == null) {
            return false;
        }

        NetworkHandler.sendToPlayer(new InteractionDialogueQuestionResponse(false, Component.literal(line)), player);
        NetworkHandler.sendToPlayer(new InteractionDialogueResponse(mainQuestion, player, villager), player);
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
        String[] templates = templatesFor(event.type());
        if (templates.length == 0) {
            return eventText;
        }

        int index = Math.floorMod((villagerId.toString() + "|" + event.day() + "|" + event.type().name()).hashCode(), templates.length);
        return String.format(Locale.ROOT, templates[index], eventText);
    }

    private static String[] templatesFor(EventType type) {
        return switch (type) {
            case MARRIAGE -> new String[]{
                    "Have you heard the happy news? %s",
                    "The whole capital has been speaking of the wedding. %s",
                    "There has been much celebration of late. %s",
                    "Word around the capital is joyful for once. %s",
                    "People can talk of little else just now. %s"
            };
            case DEATH_OR_SUCCESSION -> new String[]{
                    "Word travels fast here. %s",
                    "The whole realm has been unsettled of late. %s",
                    "There has been grave talk throughout the capital. %s",
                    "Everyone has been whispering of it. %s",
                    "No one in the capital can ignore it. %s"
            };
            case MOURNING_DECLARED -> new String[]{
                    "A hush has fallen over the capital. %s",
                    "You can feel the sorrow in every street. %s",
                    "The whole court is dressed in mourning now. %s",
                    "There has been sombre talk all through the capital. %s",
                    "No one has been in good spirits since it happened. %s"
            };
            case MOURNING_ENDED -> new String[]{
                    "It seems the capital is beginning to breathe again. %s",
                    "Folk say the mourning has finally passed. %s",
                    "The mood in the capital has begun to lift. %s",
                    "There has been quieter talk now that the mourning is over. %s",
                    "At last, the capital has stepped out from its mourning. %s"
            };
            case HEIR_NAMED -> new String[]{
                    "The court has been speaking of succession again. %s",
                    "Everyone seems to know who stands next in line now. %s",
                    "There has been much talk over the naming of an heir. %s",
                    "The capital is full of whispers about the future of the crown. %s",
                    "That bit of royal news has travelled very quickly. %s"
            };
            case DISINHERITED -> new String[]{
                    "The court has been restless ever since. %s",
                    "There has been scandal enough for a month. %s",
                    "No shortage of whispers after that decree. %s",
                    "The realm does love its gossip when succession is involved. %s",
                    "That news spread through the capital like wildfire. %s"
            };
            case LEGITIMIZED -> new String[]{
                    "The court has been full of talk about bloodlines and claims. %s",
                    "That royal decree has given everyone something to chatter over. %s",
                    "There has been no end of whispering since the proclamation. %s",
                    "People are already debating what it means for the realm. %s",
                    "The capital has taken keen interest in that decision. %s"
            };
            case THRONE_CHANGE -> new String[]{
                    "The capital has had no shortage of talk about the crown. %s",
                    "The whole realm seems to be discussing the throne. %s",
                    "That change at court has everyone murmuring. %s",
                    "No one here has stopped talking about the crown since it happened. %s",
                    "All eyes have been on the royal court of late. %s"
            };
            case CAPITAL_FOUNDED -> new String[]{
                    "This place has risen in standing. %s",
                    "There has been proud talk all around the capital. %s",
                    "Folk here have been speaking boldly since the news. %s",
                    "You can tell the people here stand a little taller now. %s",
                    "The whole settlement has taken pride in that news. %s"
            };
            case GENERIC_NOTABLE -> new String[]{
                    "Have you heard? %s",
                    "Word travels fast here. %s",
                    "There has been much talk of late. %s",
                    "The whole capital is speaking of it. %s",
                    "That has certainly given people something to talk about. %s"
            };
            case NONE -> new String[0];
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