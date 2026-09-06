package com.majesttyx.mcacapitals.dialogue;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public final class CapitalDialogueSpeaker {

    private CapitalDialogueSpeaker() {
    }

    public static void speakVillager(ServerPlayer player, Entity speaker, Component line) {
        if (player == null || line == null) {
            return;
        }

        if (speaker instanceof VillagerEntityMCA villager) {
            villager.sendChatMessage(line.copy(), player);
            return;
        }

        player.sendSystemMessage(formatVillagerSpeech(speaker, line));
    }

    public static void speakVillager(
            ServerPlayer player,
            Entity speaker,
            CapitalDialogueKey key,
            Object... args
    ) {
        RandomSource random = resolveRandom(speaker);
        Component line = CapitalDialogueLibrary.getRandomLine(speaker, key, random, args);
        speakVillager(player, speaker, line);
    }

    public static Component formatVillagerSpeech(Entity speaker, Component line) {
        if (line == null) {
            return Component.empty();
        }

        if (speaker == null) {
            return line;
        }

        return Component.translatable(
                "mcacapitals.chat.villager",
                speaker.getDisplayName(),
                line
        );
    }

    private static RandomSource resolveRandom(Entity speaker) {
        if (speaker != null) {
            ServerLevel level = speaker instanceof ServerPlayer serverPlayer
                    ? serverPlayer.serverLevel()
                    : speaker.level() instanceof ServerLevel serverLevel ? serverLevel : null;

            if (level != null) {
                return level.random;
            }
        }

        return RandomSource.create();
    }
}
