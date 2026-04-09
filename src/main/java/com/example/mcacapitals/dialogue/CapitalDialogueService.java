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

public class CapitalDialogueService {

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

        List<CapitalDialogueEventModels.ChronicleEvent> candidates =
                CapitalDialogueChronicleLogic.findRecentNotableEvents(level, capital.getChronicleEntries());
        if (candidates.isEmpty()) {
            return null;
        }

        CapitalDialogueEventModels.ChronicleEvent event = pickEventForVillager(level, villagerEntity.getUUID(), candidates);
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

    private static CapitalDialogueEventModels.ChronicleEvent pickEventForVillager(
            ServerLevel level,
            UUID villagerId,
            List<CapitalDialogueEventModels.ChronicleEvent> candidates
    ) {
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
            CapitalDialogueEventModels.ChronicleEvent event = candidates.get(i);
            int weight = baseWeightForIndex(i);

            long age = Math.max(0L, currentDay - event.day());
            if (age <= CapitalDialogueChronicleLogic.VERY_RECENT_DAYS) {
                weight += 3;
            }

            if (event.type() == CapitalDialogueEventModels.EventType.DEATH_OR_SUCCESSION
                    || event.type() == CapitalDialogueEventModels.EventType.MOURNING_DECLARED) {
                weight += 2;
            }

            if (event.type() == CapitalDialogueEventModels.EventType.MARRIAGE) {
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

    private static boolean shouldSpeakEvent(
            ServerLevel level,
            UUID villagerId,
            long eventDay,
            CapitalDialogueEventModels.EventType type
    ) {
        long currentDay = Math.max(1L, level.getDayTime() / 24000L + 1L);
        long age = Math.max(0L, currentDay - eventDay);

        int chance;
        if (age <= CapitalDialogueChronicleLogic.VERY_RECENT_DAYS) {
            chance = 45;
        } else {
            chance = 20;
        }

        if (type == CapitalDialogueEventModels.EventType.DEATH_OR_SUCCESSION
                || type == CapitalDialogueEventModels.EventType.MOURNING_DECLARED) {
            chance += 10;
        }

        int roll = Math.floorMod((villagerId.toString() + ":" + currentDay + ":" + type.name()).hashCode(), 100);
        return roll < chance;
    }

    private static String buildVillagerLine(UUID villagerId, CapitalDialogueEventModels.ChronicleEvent event) {
        String eventText = CapitalDialogueTextLogic.ensureSentence(event.text());
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

    private static CapitalDialogueKey dialogueKeyFor(CapitalDialogueEventModels.EventType type) {
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
}