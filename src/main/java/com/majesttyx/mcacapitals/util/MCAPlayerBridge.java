package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.FamilyTree;
import net.conczin.mca.server.world.data.FamilyTreeNode;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class MCAPlayerBridge {

    private static final Set<UUID> LOGGED_PLAYER_GENDER_RESOLUTION = ConcurrentHashMap.newKeySet();

    private MCAPlayerBridge() {
    }

    static Optional<Integer> getLastSeenVillageId(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return Optional.empty();
        }

        try {
            return PlayerSaveData.get(player).getLastSeenVillageId();
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#getLastSeenVillageId:direct",
                    "Failed to query MCA PlayerSaveData#getLastSeenVillageId directly ({})",
                    t.toString()
            );
            return Optional.empty();
        }
    }

    static boolean isPlayerInVillage(ServerLevel level, ServerPlayer player, Integer villageId) {
        if (level == null || player == null || villageId == null) {
            return false;
        }

        return getLastSeenVillageId(level, player)
                .map(id -> id.equals(villageId))
                .orElse(false);
    }

    static boolean isPlayerFemale(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return false;
        }

        try {
            PlayerSaveData saveData = PlayerSaveData.get(player);
            CompoundTag entityData = saveData.getEntityData();

            Gender saveDataGender = saveData.getGender();
            Gender trackedDataGender = resolveTrackedGender(entityData);
            Gender familyTreeGender = resolveFamilyTreeGender(level, player.getUUID());

            Gender resolvedGender = resolveBestGender(saveDataGender, trackedDataGender, familyTreeGender);

            if (saveDataGender == Gender.UNASSIGNED && trackedDataGender != Gender.UNASSIGNED) {
                normalizePlayerSaveGender(player, saveData, entityData, trackedDataGender);
                familyTreeGender = normalizeFamilyTreeGender(level, player, trackedDataGender);
            }

            logResolvedGenderOnce(player, saveData, saveDataGender, trackedDataGender, familyTreeGender, resolvedGender);

            return resolvedGender == Gender.FEMALE;
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#isPlayerFemale:direct",
                    "Failed to query MCA player gender directly ({})",
                    t.toString()
            );
            return false;
        }
    }

    private static Gender resolveBestGender(Gender saveDataGender, Gender trackedDataGender, Gender familyTreeGender) {
        if (saveDataGender == Gender.FEMALE || saveDataGender == Gender.MALE || saveDataGender == Gender.NEUTRAL) {
            return saveDataGender;
        }

        if (trackedDataGender == Gender.FEMALE || trackedDataGender == Gender.MALE || trackedDataGender == Gender.NEUTRAL) {
            return trackedDataGender;
        }

        if (familyTreeGender == Gender.FEMALE || familyTreeGender == Gender.MALE || familyTreeGender == Gender.NEUTRAL) {
            return familyTreeGender;
        }

        return Gender.UNASSIGNED;
    }

    private static Gender resolveTrackedGender(CompoundTag entityData) {
        if (entityData == null) {
            return Gender.UNASSIGNED;
        }

        if (entityData.contains("Gender")) {
            return Gender.byId(entityData.getInt("Gender"));
        }

        if (entityData.contains("gender")) {
            return Gender.byId(entityData.getInt("gender"));
        }

        return Gender.UNASSIGNED;
    }

    private static Gender resolveFamilyTreeGender(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null) {
            return Gender.UNASSIGNED;
        }

        try {
            FamilyTree familyTree = FamilyTree.get(level);
            Optional<FamilyTreeNode> node = familyTree.getOrEmpty(playerId);
            return node.map(FamilyTreeNode::gender).orElse(Gender.UNASSIGNED);
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#resolveFamilyTreeGender:direct",
                    "Failed to query MCA FamilyTreeNode gender directly ({})",
                    t.toString()
            );
            return Gender.UNASSIGNED;
        }
    }

    private static void normalizePlayerSaveGender(ServerPlayer player, PlayerSaveData saveData, CompoundTag entityData, Gender gender) {
        if (player == null || saveData == null || gender == null || gender == Gender.UNASSIGNED) {
            return;
        }

        CompoundTag normalized = entityData == null ? new CompoundTag() : entityData;
        normalized.putInt("gender", gender.getId());
        normalized.putInt("Gender", gender.getId());

        saveData.setEntityData(normalized);
        saveData.setEntityDataSet(true);
        saveData.setDirty();

        MCACapitals.LOGGER.info(
                "[MCACapitals] Normalized MCA player gender data. player='{}', gender='{}', dataName='{}', genderId={}",
                player.getGameProfile().getName(),
                gender.name(),
                gender.getDataName(),
                gender.getId()
        );
    }

    private static Gender normalizeFamilyTreeGender(ServerLevel level, ServerPlayer player, Gender gender) {
        if (level == null || player == null || gender == null || gender == Gender.UNASSIGNED) {
            return Gender.UNASSIGNED;
        }

        try {
            FamilyTree familyTree = FamilyTree.get(level);
            FamilyTreeNode node = familyTree.getOrCreate(player);
            node.setGender(gender);
            familyTree.setDirty();
            return gender;
        } catch (Throwable t) {
            MCAReflectionHelper.warnOnce(
                    "MCAPlayerBridge#normalizeFamilyTreeGender:direct",
                    "Failed to normalize MCA FamilyTreeNode gender directly ({})",
                    t.toString()
            );
            return Gender.UNASSIGNED;
        }
    }

    private static void logResolvedGenderOnce(
            ServerPlayer player,
            PlayerSaveData saveData,
            Gender saveDataGender,
            Gender trackedDataGender,
            Gender familyTreeGender,
            Gender resolvedGender
    ) {
        if (player == null || saveData == null) {
            return;
        }

        if (!LOGGED_PLAYER_GENDER_RESOLUTION.add(player.getUUID())) {
            return;
        }

        CompoundTag entityData = saveData.getEntityData();
        int lowercaseGenderId = entityData != null && entityData.contains("gender")
                ? entityData.getInt("gender")
                : -1;
        int trackedGenderId = entityData != null && entityData.contains("Gender")
                ? entityData.getInt("Gender")
                : -1;

        MCACapitals.LOGGER.info(
                "[MCACapitals] MCA player gender resolved. player='{}', playerSaveDataGender='{}', playerSaveDataDataName='{}', entityDataSet={}, entityData.gender={}, entityData.Gender={}, trackedDataGender='{}', trackedDataDataName='{}', familyTreeGender='{}', familyTreeDataName='{}', resolvedGender='{}', resolvedDataName='{}'",
                player.getGameProfile().getName(),
                saveDataGender == null ? "null" : saveDataGender.name(),
                saveDataGender == null ? "null" : saveDataGender.getDataName(),
                saveData.isEntityDataSet(),
                lowercaseGenderId,
                trackedGenderId,
                trackedDataGender == null ? "null" : trackedDataGender.name(),
                trackedDataGender == null ? "null" : trackedDataGender.getDataName(),
                familyTreeGender == null ? "null" : familyTreeGender.name(),
                familyTreeGender == null ? "null" : familyTreeGender.getDataName(),
                resolvedGender == null ? "null" : resolvedGender.name(),
                resolvedGender == null ? "null" : resolvedGender.getDataName()
        );
    }
}