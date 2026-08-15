package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

final class CapitalPetitionDialogueHelper {

    private CapitalPetitionDialogueHelper() {
    }

    static void sendCapitalDialogue(
            ServerPlayer player,
            ServerLevel level,
            CapitalRecord capital,
            CapitalDialogueKey key,
            Object... args
    ) {
        if (player == null) {
            return;
        }

        Entity speaker = resolveCapitalSpeakerEntity(level, capital);
        if (speaker == null) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Petition response '{}' had no loaded sovereign speaker",
                    key
            );
            player.sendSystemMessage(Component.translatable("mcacapitals.system.petition.speaker_unavailable"));
            return;
        }

        RandomSource random = level != null ? level.random : RandomSource.create();
        Component line = CapitalDialogueLibrary.getRandomLine(speaker, key, random, args);
        if (line == null) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Petition response '{}' was skipped because no supported personality dialogue could be resolved",
                    key
            );
            return;
        }

        MCACapitals.LOGGER.info("[MCACapitals] Petition response key={}", key);
        player.sendSystemMessage(CapitalDialogueSpeaker.formatVillagerSpeech(speaker, line));
    }

    static void sendDialogueKeyAndClose(
            ServerPlayer player,
            Entity villagerEntity,
            CapitalDialogueKey key,
            Object... args
    ) {
        if (player == null) {
            tryStopInteracting(villagerEntity);
            return;
        }

        Component line = CapitalDialogueLibrary.getRandomLine(
                villagerEntity,
                key,
                villagerEntity != null && villagerEntity.level() != null
                        ? villagerEntity.level().random
                        : null,
                args
        );

        if (line != null) {
            MCACapitals.LOGGER.info("[MCACapitals] Petition response key={}", key);
            player.sendSystemMessage(CapitalDialogueSpeaker.formatVillagerSpeech(villagerEntity, line));
        } else {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Petition response '{}' was skipped because no supported personality dialogue could be resolved",
                    key
            );
        }

        tryStopInteracting(villagerEntity);
    }

    private static Entity resolveCapitalSpeakerEntity(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getSovereign() == null) {
            return null;
        }
        return MCAIntegrationBridge.getEntityByUuid(level, capital.getSovereign());
    }

    private static void tryStopInteracting(Entity villagerEntity) {
        if (!MCAIntegrationBridge.stopInteracting(villagerEntity)) {
            MCACapitals.LOGGER.warn("[MCACapitals] Failed to stop MCA interaction cleanly");
        }
    }
}
