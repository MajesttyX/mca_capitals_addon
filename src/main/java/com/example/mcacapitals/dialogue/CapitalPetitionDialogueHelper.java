package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

final class CapitalPetitionDialogueHelper {

    private CapitalPetitionDialogueHelper() {
    }

    static void sendCapitalDialogue(ServerPlayer player, ServerLevel level, CapitalRecord capital, CapitalDialogueKey key, Object... args) {
        Entity speaker = resolveCapitalSpeakerEntity(level, capital);
        RandomSource random = level != null ? level.random : RandomSource.create();
        String line = CapitalDialogueLibrary.getRandomLine(key, random, args);

        if (speaker != null) {
            String spokenLine = CapitalDialogueSpeaker.formatVillagerSpeech(speaker, line);
            MCACapitals.LOGGER.info("[MCACapitals] Petition response: {}", spokenLine);
            player.sendSystemMessage(Component.literal(spokenLine));
            return;
        }

        MCACapitals.LOGGER.info("[MCACapitals] Petition response: {}", line);
        player.sendSystemMessage(Component.literal(line));
    }

    static void sendDialogueKeyAndClose(ServerPlayer player, Entity villagerEntity, CapitalDialogueKey key, Object... args) {
        String line = CapitalDialogueLibrary.getRandomLine(
                key,
                villagerEntity != null && villagerEntity.level() != null ? villagerEntity.level().random : null,
                args
        );
        sendDialogueLineAndClose(player, villagerEntity, line);
    }

    private static Entity resolveCapitalSpeakerEntity(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getSovereign() == null) {
            return null;
        }
        return MCAIntegrationBridge.getEntityByUuid(level, capital.getSovereign());
    }

    private static void sendDialogueLineAndClose(ServerPlayer player, Entity villagerEntity, String line) {
        String spokenLine = CapitalDialogueSpeaker.formatVillagerSpeech(villagerEntity, line);
        MCACapitals.LOGGER.info("[MCACapitals] Petition response: {}", spokenLine);
        player.sendSystemMessage(Component.literal(spokenLine));
        tryStopInteracting(villagerEntity);
    }

    private static void tryStopInteracting(Entity villagerEntity) {
        if (!MCAIntegrationBridge.stopInteracting(villagerEntity)) {
            MCACapitals.LOGGER.warn("[MCACapitals] Failed to stop MCA interaction cleanly");
        }
    }
}