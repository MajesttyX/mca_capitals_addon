package com.example.mcacapitals.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class MCARelationshipOps {

    private MCARelationshipOps() {
    }

    static MCARelationshipBridge.BetrothalResult promise(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return MCARelationshipBridge.BetrothalResult.failure("That betrothal could not be arranged.");
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return MCARelationshipBridge.BetrothalResult.failure("Only an MCA noble may be chosen for betrothal.");
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(player.serverLevel(), villagerEntity.getUUID())) {
            return MCARelationshipBridge.BetrothalResult.failure("Only a teen or adult noble may be chosen for betrothal.");
        }

        Object playerData = MCARelationshipReflection.getPlayerSaveData(player);
        if (playerData == null) {
            return MCARelationshipBridge.BetrothalResult.failure("The realm could not consult MCA's courtship records.");
        }

        Object relationships = MCAReflectionHelper.invoke(villagerEntity, "getRelationships");
        if (relationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure("That villager's relationship state could not be read.");
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isMarriedTo", player)) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble is already married to you.");
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble is already married.");
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isEngagedWith", player)) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble is already engaged to you.");
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isPromisedTo", player)) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble is already betrothed to you.");
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isEngaged")) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble is already engaged.");
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isPromised")) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble is already promised elsewhere.");
        }

        if (MCARelationshipReflection.booleanCall(playerData, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure("You are already married.");
        }

        int heartsRequired = MCARelationshipReflection.getBouquetHeartsRequirement();
        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(player.serverLevel(), villagerEntity.getUUID(), player.getUUID());
        if (hearts < heartsRequired) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble does not yet return your affection strongly enough.");
        }

        Object attracted = MCARelationshipReflection.invokeCompatible(villagerEntity, "canBeAttractedTo", playerData);
        if (attracted instanceof Boolean b && !b) {
            return MCARelationshipBridge.BetrothalResult.failure("That noble would not accept such a match.");
        }

        MCARelationshipReflection.invokeCompatible(playerData, "promise", villagerEntity);
        MCARelationshipReflection.invokeCompatible(relationships, "promise", player);

        Object brain = MCAReflectionHelper.invoke(villagerEntity, "getVillagerBrain");
        if (brain != null) {
            MCARelationshipReflection.invokeCompatible(brain, "modifyMoodValue", 5);
        }

        return MCARelationshipBridge.BetrothalResult.ok();
    }

    static MCARelationshipBridge.BetrothalResult promiseVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return MCARelationshipBridge.BetrothalResult.failure("That betrothal recommendation could not be arranged.");
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure("Only MCA villagers may be joined by recommendation.");
        }

        if (firstVillager.getUUID().equals(secondVillager.getUUID())) {
            return MCARelationshipBridge.BetrothalResult.failure("A villager cannot be recommended for betrothal to themself.");
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");

        if (firstRelationships == null || secondRelationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure("One of the villagers' relationship records could not be read.");
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure("Those villagers are already married to one another.");
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarried") || MCARelationshipReflection.booleanCall(secondRelationships, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure("One of those villagers is already married.");
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isEngagedWith", secondVillager)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isEngagedWith", firstVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure("Those villagers are already engaged to one another.");
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isPromisedTo", secondVillager)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isPromisedTo", firstVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure("Those villagers are already betrothed to one another.");
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isEngaged") || MCARelationshipReflection.booleanCall(secondRelationships, "isEngaged")) {
            return MCARelationshipBridge.BetrothalResult.failure("One of those villagers is already engaged.");
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isPromised") || MCARelationshipReflection.booleanCall(secondRelationships, "isPromised")) {
            return MCARelationshipBridge.BetrothalResult.failure("One of those villagers is already promised elsewhere.");
        }

        Object firstAttracted = MCARelationshipReflection.invokeCompatible(firstVillager, "canBeAttractedTo", secondVillager);
        if (firstAttracted instanceof Boolean b && !b) {
            return MCARelationshipBridge.BetrothalResult.failure("The first villager would not accept such a match.");
        }

        Object secondAttracted = MCARelationshipReflection.invokeCompatible(secondVillager, "canBeAttractedTo", firstVillager);
        if (secondAttracted instanceof Boolean b && !b) {
            return MCARelationshipBridge.BetrothalResult.failure("The second villager would not accept such a match.");
        }

        MCARelationshipReflection.invokeCompatible(firstRelationships, "promise", secondVillager);
        MCARelationshipReflection.invokeCompatible(secondRelationships, "promise", firstVillager);

        Object firstBrain = MCAReflectionHelper.invoke(firstVillager, "getVillagerBrain");
        if (firstBrain != null) {
            MCARelationshipReflection.invokeCompatible(firstBrain, "modifyMoodValue", 5);
        }

        Object secondBrain = MCAReflectionHelper.invoke(secondVillager, "getVillagerBrain");
        if (secondBrain != null) {
            MCARelationshipReflection.invokeCompatible(secondBrain, "modifyMoodValue", 5);
        }

        return MCARelationshipBridge.BetrothalResult.ok();
    }

    static boolean areVillagersBetrothedToEachOther(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return false;
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");
        if (firstRelationships == null || secondRelationships == null) {
            return false;
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return false;
        }

        return (MCARelationshipReflection.booleanCall(firstRelationships, "isPromisedTo", secondVillager)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isPromisedTo", firstVillager))
                || (MCARelationshipReflection.booleanCall(firstRelationships, "isEngagedWith", secondVillager)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isEngagedWith", firstVillager));
    }

    static MCARelationshipBridge.BetrothalResult marryVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return MCARelationshipBridge.BetrothalResult.failure("That marriage could not be arranged.");
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure("Only MCA villagers may be married by this bridge.");
        }

        if (!areVillagersBetrothedToEachOther(firstVillager, secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure("Those villagers are not currently betrothed to one another.");
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");
        if (firstRelationships == null || secondRelationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure("One of the villagers' relationship records could not be read.");
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        tryRelationshipStep(firstRelationships, firstVillager, secondVillager, "engage");
        tryRelationshipStep(secondRelationships, secondVillager, firstVillager, "engage");

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        tryRelationshipStep(firstRelationships, firstVillager, secondVillager, "marry");
        tryRelationshipStep(secondRelationships, secondVillager, firstVillager, "marry");

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondVillager)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstVillager)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        return MCARelationshipBridge.BetrothalResult.failure("Those villagers could not yet be advanced from betrothal to marriage.");
    }

    static boolean isActuallyMarried(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return false;
        }

        Object playerData = MCARelationshipReflection.getPlayerSaveData(player);
        if (playerData == null) {
            return false;
        }

        Object relationships = MCAReflectionHelper.invoke(villagerEntity, "getRelationships");
        if (relationships == null) {
            return false;
        }

        if (!MCARelationshipReflection.booleanCall(playerData, "isMarried")) {
            return false;
        }

        return MCARelationshipReflection.booleanCall(relationships, "isMarriedTo", player);
    }

    static boolean isActuallyMarriedToPlayer(ServerPlayer player, UUID villagerId) {
        if (player == null || villagerId == null) {
            return false;
        }

        Entity villager = MCAIntegrationBridge.getEntityByUuid(player.serverLevel(), villagerId);
        return isActuallyMarried(player, villager);
    }

    private static void tryRelationshipStep(Object relationships, Entity self, Entity other, String methodName) {
        if (relationships == null || self == null || other == null || methodName == null) {
            return;
        }

        MCARelationshipReflection.invokeCompatible(relationships, methodName, other);
        MCARelationshipReflection.invokeCompatible(relationships, methodName, other.getUUID());
        MCARelationshipReflection.invokeCompatible(self, methodName, other);
        MCARelationshipReflection.invokeCompatible(self, methodName, other.getUUID());
    }
}