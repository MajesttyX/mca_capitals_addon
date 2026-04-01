package com.example.mcacapitals.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public final class CapitalDialogueSpeaker {
    private CapitalDialogueSpeaker() {
    }

    public static void speakVillager(ServerPlayer player, Entity speaker, String line) {
        if (player == null) {
            return;
        }

        player.sendSystemMessage(Component.literal(formatVillagerSpeech(speaker, line)));
    }

    public static void speakVillager(ServerPlayer player, Entity speaker, CapitalDialogueKey key, Object... args) {
        RandomSource random = resolveRandom(speaker);
        String line = CapitalDialogueLibrary.getRandomLine(key, random, args);
        speakVillager(player, speaker, line);
    }

    public static String formatVillagerSpeech(Entity speaker, String line) {
        String speakerName = resolveSpeakerName(speaker);
        String cleanedLine = line == null ? "" : line.trim();

        if (speakerName.isBlank()) {
            return cleanedLine;
        }

        return speakerName + ": " + cleanedLine;
    }

    private static String resolveSpeakerName(Entity speaker) {
        if (speaker == null || speaker.getName() == null) {
            return "";
        }

        String name = speaker.getName().getString();
        return name == null ? "" : name.trim();
    }

    private static RandomSource resolveRandom(Entity speaker) {
        if (speaker != null) {
            ServerLevel level = speaker instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    ? serverPlayer.serverLevel()
                    : speaker.level() instanceof ServerLevel serverLevel ? serverLevel : null;

            if (level != null) {
                return level.random;
            }
        }

        return RandomSource.create();
    }
}