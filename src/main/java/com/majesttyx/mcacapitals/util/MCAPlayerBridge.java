package com.majesttyx.mcacapitals.util;

import net.conczin.mca.entity.ai.Messenger;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

final class MCAPlayerBridge {

    private MCAPlayerBridge() {
    }

    static Optional<Integer> getLastSeenVillageId(
            ServerLevel level,
            ServerPlayer player
    ) {
        if (level == null || player == null) {
            return Optional.empty();
        }

        try {
            PlayerSaveData saveData =
                    PlayerSaveData.get(player);

            return saveData == null
                    ? Optional.empty()
                    : saveData.getLastSeenVillageId();
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#getLastSeenVillageId:7.7.32",
                    "Failed to query MCA 7.7.32 PlayerSaveData#getLastSeenVillageId ({})",
                    t.toString()
            );

            return Optional.empty();
        }
    }

    static String getDialogueName(
            ServerPlayer player
    ) {
        if (player == null) {
            return "";
        }

        try {
            String name =
                    Messenger.getName(player);

            if (name != null
                    && !name.isBlank()) {
                return name.trim();
            }
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#getDialogueName:messenger",
                    "Failed to resolve MCA dialogue player name through Messenger#getName ({})",
                    t.toString()
            );
        }

        try {
            PlayerSaveData saveData =
                    PlayerSaveData.get(player);

            if (saveData != null
                    && saveData.getFamilyEntry() != null) {
                String name =
                        saveData.getFamilyEntry()
                                .getName();

                if (name != null
                        && !name.isBlank()) {
                    return name.trim();
                }
            }
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#getDialogueName:familyTree:7.7.32",
                    "Failed to resolve MCA 7.7.32 dialogue player name from PlayerSaveData ({})",
                    t.toString()
            );
        }

        return player.getName().getString();
    }

    static boolean isPlayerInVillage(
            ServerLevel level,
            ServerPlayer player,
            Integer villageId
    ) {
        if (level == null
                || player == null
                || villageId == null) {
            return false;
        }

        return getLastSeenVillageId(
                level,
                player
        )
                .map(id -> id.equals(villageId))
                .orElse(false);
    }

    static boolean isPlayerFemale(
            ServerLevel level,
            ServerPlayer player
    ) {
        if (level == null
                || player == null) {
            return false;
        }

        try {
            PlayerSaveData saveData =
                    PlayerSaveData.get(player);

            if (saveData == null) {
                return false;
            }

            Gender gender =
                    saveData.getGender();

            if (gender == Gender.FEMALE) {
                return true;
            }

            if (gender == Gender.MALE
                    || gender == Gender.NEUTRAL) {
                return false;
            }

            if (saveData.getFamilyEntry() != null) {
                return saveData.getFamilyEntry()
                        .gender()
                        == Gender.FEMALE;
            }
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#isPlayerFemale:7.7.32",
                    "Failed to query MCA 7.7.32 PlayerSaveData gender ({})",
                    t.toString()
            );
        }

        return false;
    }
}