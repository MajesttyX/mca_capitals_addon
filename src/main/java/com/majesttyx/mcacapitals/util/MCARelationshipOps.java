package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class MCARelationshipOps {

    private MCARelationshipOps() {
    }

    static MCARelationshipBridge.BetrothalResult promise(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_betrothal_could_not_be_arranged"));
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.only_an_mca_noble_may_be_chosen_for_betrothal"));
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(player.serverLevel(), villagerEntity.getUUID())) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.only_a_teen_or_adult_noble_may_be_chosen_for_betrothal"));
        }

        Object playerData = MCARelationshipReflection.getPlayerSaveData(player);
        if (playerData == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.the_realm_could_not_consult_mcas_courtship_records"));
        }

        Object relationships = MCAReflectionHelper.invoke(villagerEntity, "getRelationships");
        if (relationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_villagers_relationship_state_could_not_be_read"));
        }

        UUID playerId = player.getUUID();

        if (MCARelationshipReflection.booleanCall(relationships, "isMarriedTo", playerId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_is_already_married_to_you"));
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_is_already_married"));
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isEngagedWith", playerId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_is_already_engaged_to_you"));
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isPromisedTo", playerId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_is_already_betrothed_to_you"));
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isEngaged")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_is_already_engaged"));
        }

        if (MCARelationshipReflection.booleanCall(relationships, "isPromised")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_is_already_promised_elsewhere"));
        }

        if (MCARelationshipReflection.booleanCall(playerData, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.you_are_already_married"));
        }

        int heartsRequired = MCARelationshipReflection.getBouquetHeartsRequirement();
        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(player.serverLevel(), villagerEntity.getUUID(), playerId);
        if (hearts < heartsRequired) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_does_not_yet_return_your_affection_strongly_enough"));
        }

        Object attracted = MCARelationshipReflection.invokeCompatible(villagerEntity, "canBeAttractedTo", playerData);
        if (attracted instanceof Boolean b && !b) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_noble_would_not_accept_such_a_match"));
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
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_betrothal_recommendation_could_not_be_arranged"));
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.only_mca_villagers_may_be_joined_by_recommendation"));
        }

        UUID firstId = firstVillager.getUUID();
        UUID secondId = secondVillager.getUUID();

        if (firstId.equals(secondId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.a_villager_cannot_be_recommended_for_betrothal_to_themself"));
        }

        ServerLevel level = resolveServerLevel(firstVillager, secondVillager);
        if (level == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.the_world_context_for_that_betrothal_could_not_be_resolved"));
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");

        if (firstRelationships == null || secondRelationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_the_villagers_relationship_records_could_not_be_read"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_already_married_to_one_another"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarried") || MCARelationshipReflection.booleanCall(secondRelationships, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_those_villagers_is_already_married"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isEngagedWith", secondId)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isEngagedWith", firstId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_already_engaged_to_one_another"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isPromisedTo", secondId)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isPromisedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_already_betrothed_to_one_another"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isEngaged") || MCARelationshipReflection.booleanCall(secondRelationships, "isEngaged")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_those_villagers_is_already_engaged"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isPromised") || MCARelationshipReflection.booleanCall(secondRelationships, "isPromised")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_those_villagers_is_already_promised_elsewhere"));
        }

        if (areRelatives(level, firstRelationships, firstVillager, secondRelationships, secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_related_and_cannot_be_joined_by_betrothal"));
        }

        Object firstAttracted = MCARelationshipReflection.invokeCompatible(firstVillager, "canBeAttractedTo", secondVillager);
        if (firstAttracted instanceof Boolean b && !b) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.the_first_villager_would_not_accept_such_a_match"));
        }

        Object secondAttracted = MCARelationshipReflection.invokeCompatible(secondVillager, "canBeAttractedTo", firstVillager);
        if (secondAttracted instanceof Boolean b && !b) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.the_second_villager_would_not_accept_such_a_match"));
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

    static MCARelationshipBridge.BetrothalResult promiseVillagerToVillagerByDecree(Entity firstVillager, Entity secondVillager) {
        return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.villager_decree_betrothals_now_use_pending_betrothal_data_instead_of_mca_promise_state"));
    }

    static MCARelationshipBridge.BetrothalResult validatePendingVillagerBetrothal(ServerLevel level, Entity firstVillager, Entity secondVillager) {
        if (level == null || firstVillager == null || secondVillager == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_betrothal_decree_could_not_be_carried_out"));
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.only_mca_villagers_may_be_joined_by_betrothal_decree"));
        }

        UUID firstId = firstVillager.getUUID();
        UUID secondId = secondVillager.getUUID();

        if (firstId.equals(secondId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.a_villager_cannot_be_betrothed_to_themself"));
        }

        if (PendingVillagerBetrothalAccess.hasPendingBetrothal(level, firstId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.the_first_villager_is_already_betrothed_elsewhere"));
        }

        if (PendingVillagerBetrothalAccess.hasPendingBetrothal(level, secondId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.the_second_villager_is_already_betrothed_elsewhere"));
        }

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");

        if (firstRelationships == null || secondRelationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_the_villagers_relationship_records_could_not_be_read"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_already_married_to_one_another"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarried")
                || MCARelationshipReflection.booleanCall(secondRelationships, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_those_villagers_is_already_married"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isEngagedWith", secondId)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isEngagedWith", firstId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_already_engaged_to_one_another"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isPromisedTo", secondId)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isPromisedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_already_betrothed_to_one_another"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isEngaged")
                || MCARelationshipReflection.booleanCall(secondRelationships, "isEngaged")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_those_villagers_is_already_engaged"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isPromised")
                || MCARelationshipReflection.booleanCall(secondRelationships, "isPromised")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_those_villagers_is_already_promised_elsewhere"));
        }

        if (areRelatives(level, firstRelationships, firstVillager, secondRelationships, secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_related_and_cannot_be_joined_by_betrothal_decree"));
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

        UUID firstId = firstVillager.getUUID();
        UUID secondId = secondVillager.getUUID();

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");
        if (firstRelationships == null || secondRelationships == null) {
            return false;
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return false;
        }

        return (MCARelationshipReflection.booleanCall(firstRelationships, "isPromisedTo", secondId)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isPromisedTo", firstId))
                || (MCARelationshipReflection.booleanCall(firstRelationships, "isEngagedWith", secondId)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isEngagedWith", firstId));
    }

    static MCARelationshipBridge.BetrothalResult marryVillagerToVillager(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_marriage_could_not_be_arranged"));
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.only_mca_villagers_may_be_married_by_this_bridge"));
        }

        if (!areVillagersBetrothedToEachOther(firstVillager, secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_not_currently_betrothed_to_one_another"));
        }

        UUID firstId = firstVillager.getUUID();
        UUID secondId = secondVillager.getUUID();

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");
        if (firstRelationships == null || secondRelationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_the_villagers_relationship_records_could_not_be_read"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        tryRelationshipStep(firstRelationships, firstVillager, secondVillager, "engage");
        tryRelationshipStep(secondRelationships, secondVillager, firstVillager, "engage");

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        tryRelationshipStep(firstRelationships, firstVillager, secondVillager, "marry");
        tryRelationshipStep(secondRelationships, secondVillager, firstVillager, "marry");

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_could_not_yet_be_advanced_from_betrothal_to_marriage"));
    }

    static MCARelationshipBridge.BetrothalResult marryVillagerToVillagerDirect(Entity firstVillager, Entity secondVillager) {
        if (firstVillager == null || secondVillager == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.that_marriage_could_not_be_arranged"));
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(firstVillager) || !MCAIntegrationBridge.isMCAVillagerEntity(secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.only_mca_villagers_may_be_married_by_this_bridge"));
        }

        ServerLevel level = resolveServerLevel(firstVillager, secondVillager);
        if (level == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.the_world_context_for_that_marriage_could_not_be_resolved"));
        }

        if (!"ADULT".equalsIgnoreCase(MCAIntegrationBridge.getAgeState(level, firstVillager.getUUID()))
                || !"ADULT".equalsIgnoreCase(MCAIntegrationBridge.getAgeState(level, secondVillager.getUUID()))) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.both_villagers_must_be_adults_before_the_marriage_may_proceed"));
        }

        UUID firstId = firstVillager.getUUID();
        UUID secondId = secondVillager.getUUID();

        Object firstRelationships = MCAReflectionHelper.invoke(firstVillager, "getRelationships");
        Object secondRelationships = MCAReflectionHelper.invoke(secondVillager, "getRelationships");
        if (firstRelationships == null || secondRelationships == null) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_the_villagers_relationship_records_could_not_be_read"));
        }

        if (areRelatives(level, firstRelationships, firstVillager, secondRelationships, secondVillager)) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_are_related_and_cannot_be_married"));
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarried")
                || MCARelationshipReflection.booleanCall(secondRelationships, "isMarried")) {
            return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.one_of_those_villagers_is_already_married"));
        }

        tryRelationshipStep(firstRelationships, firstVillager, secondVillager, "marry");
        tryRelationshipStep(secondRelationships, secondVillager, firstVillager, "marry");

        if (MCARelationshipReflection.booleanCall(firstRelationships, "isMarriedTo", secondId)
                && MCARelationshipReflection.booleanCall(secondRelationships, "isMarriedTo", firstId)) {
            return MCARelationshipBridge.BetrothalResult.ok();
        }

        return MCARelationshipBridge.BetrothalResult.failure(Component.translatable("mcacapitals.system.mca_relationship.those_villagers_could_not_be_advanced_into_marriage"));
    }

    static boolean isActuallyMarried(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
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

        return MCARelationshipReflection.booleanCall(relationships, "isMarriedTo", player.getUUID());
    }

    static boolean isActuallyMarriedToPlayer(ServerPlayer player, UUID villagerId) {
        if (player == null || villagerId == null) {
            return false;
        }

        Entity villager = MCAIntegrationBridge.getEntityByUuid(player.serverLevel(), villagerId);
        return isActuallyMarried(player, villager);
    }

    private static ServerLevel resolveServerLevel(Entity firstVillager, Entity secondVillager) {
        if (firstVillager != null && firstVillager.level() instanceof ServerLevel level) {
            return level;
        }
        if (secondVillager != null && secondVillager.level() instanceof ServerLevel level) {
            return level;
        }
        return null;
    }

    private static boolean areRelatives(
            ServerLevel level,
            Object firstRelationships,
            Entity firstVillager,
            Object secondRelationships,
            Entity secondVillager
    ) {
        if (level != null
                && firstVillager != null
                && secondVillager != null
                && MCAIntegrationBridge.areCloselyRelatedForMarriage(level, firstVillager.getUUID(), secondVillager.getUUID())) {
            return true;
        }

        return MCARelationshipReflection.booleanCall(firstRelationships, "isRelative", secondVillager)
                || MCARelationshipReflection.booleanCall(firstRelationships, "isRelative", secondVillager.getUUID())
                || MCARelationshipReflection.booleanCall(secondRelationships, "isRelative", firstVillager)
                || MCARelationshipReflection.booleanCall(secondRelationships, "isRelative", firstVillager.getUUID());
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