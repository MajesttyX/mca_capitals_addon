package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import fabric.net.mca.entity.VillagerEntityMCA;
import fabric.net.mca.entity.ai.relationship.Gender;
import fabric.net.mca.server.world.data.FamilyTree;
import fabric.net.mca.server.world.data.PlayerSaveData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class CapitalDialogueGenderResolver {

    public enum ResolvedGender {
        MALE,
        FEMALE,
        NEUTRAL
    }

    private CapitalDialogueGenderResolver() {
    }

    public static ResolvedGender resolve(ServerLevel level, CapitalRecord capital, UUID entityId) {
        Gender gender = resolveMcaGender(level, entityId);
        if (gender == Gender.FEMALE) {
            return ResolvedGender.FEMALE;
        }
        if (gender == Gender.MALE) {
            return ResolvedGender.MALE;
        }
        if (gender == Gender.NEUTRAL) {
            return ResolvedGender.NEUTRAL;
        }

        if (capital != null && entityId != null) {
            Boolean stored = resolveStoredFemale(capital, entityId);
            if (stored != null) {
                return stored ? ResolvedGender.FEMALE : ResolvedGender.MALE;
            }
        }

        return ResolvedGender.NEUTRAL;
    }

    private static Gender resolveMcaGender(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return Gender.UNASSIGNED;
        }

        try {
            if (level.getServer() != null) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(entityId);
                if (player != null) {
                    PlayerSaveData saveData = PlayerSaveData.get(player);
                    Gender playerGender = saveData.getGender();
                    if (playerGender != null && playerGender != Gender.UNASSIGNED) {
                        return playerGender;
                    }

                    Gender trackedGender = resolveTrackedGender(saveData.getEntityData());
                    if (trackedGender != Gender.UNASSIGNED) {
                        return trackedGender;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof VillagerEntityMCA villager) {
                Gender villagerGender = villager.getGenetics().getGender();
                if (villagerGender != null && villagerGender != Gender.UNASSIGNED) {
                    return villagerGender;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Optional<fabric.net.mca.server.world.data.FamilyTreeNode> node =
                    FamilyTree.get(level).getOrEmpty(entityId);
            return node.map(fabric.net.mca.server.world.data.FamilyTreeNode::gender)
                    .orElse(Gender.UNASSIGNED);
        } catch (Throwable ignored) {
            return Gender.UNASSIGNED;
        }
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

    private static Boolean resolveStoredFemale(CapitalRecord capital, UUID entityId) {
        if (entityId.equals(capital.getSovereign())) {
            return capital.isSovereignFemale();
        }
        if (entityId.equals(capital.getConsort())) {
            return capital.isConsortFemale();
        }
        if (entityId.equals(capital.getDowager())) {
            return capital.isDowagerFemale();
        }
        if (entityId.equals(capital.getCommander())) {
            return capital.isCommanderFemale();
        }
        if (entityId.equals(capital.getHand())) {
            return capital.isHandFemale();
        }
        if (entityId.equals(capital.getHerald())) {
            return capital.isHeraldFemale();
        }
        if (entityId.equals(capital.getGrandMaester())) {
            return capital.isGrandMaesterFemale();
        }
        if (capital.isRoyalChild(entityId)) {
            return capital.isRoyalChildFemale(entityId);
        }
        if (capital.isPrinceConsort(entityId)) {
            return capital.isPrinceConsortFemale(entityId);
        }
        if (capital.isRoyalGuard(entityId)) {
            return capital.isRoyalGuardFemale(entityId);
        }
        if (capital.isDuke(entityId)) {
            return capital.isDukeFemale(entityId);
        }
        if (capital.isMarriageDuke(entityId)) {
            return capital.isMarriageDukeFemale(entityId);
        }
        if (capital.isDowagerDuke(entityId)) {
            return capital.isDowagerDukeFemale(entityId);
        }
        if (capital.isDowagerPrince(entityId)) {
            return capital.isDowagerPrinceFemale(entityId);
        }
        if (capital.isKnight(entityId)) {
            return capital.isKnightFemale(entityId);
        }
        if (capital.isLord(entityId)) {
            return capital.isLordFemale(entityId);
        }
        return null;
    }
}
