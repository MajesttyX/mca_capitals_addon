package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.capital.CapitalCommanderService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.MCAReputationBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

final class CapitalPetitionSovereignActions {

    private CapitalPetitionSovereignActions() {
    }

    static void handleThronePetition(
            ServerPlayer player,
            Entity villagerEntity,
            int minPopulation,
            int minHearts,
            double maxAudienceDistanceSqr
    ) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalPetitionRequirements.resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (capital.getVillageId() == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_MISSING_VILLAGE);
            return;
        }

        if (capital.getSovereign() == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_NO_SOVEREIGN);
            return;
        }

        if (!villagerEntity.getUUID().equals(capital.getSovereign())) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_NOT_REIGNING);
            return;
        }

        if (capital.isPlayerSovereign()) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_PLAYER_HELD);
            return;
        }

        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, maxAudienceDistanceSqr)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        if (population < minPopulation) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.THRONE_POPULATION_TOO_LOW,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                    minPopulation
            );
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (hearts < minHearts) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.THRONE_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        CapitalRecord existingPlayerCapital = CapitalManager.getCapitalBySovereign(player.getUUID());
        if (existingPlayerCapital != null && !capital.getCapitalId().equals(existingPlayerCapital.getCapitalId())) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.THRONE_ALREADY_RULES_OTHER);
            return;
        }

        CapitalPetitionOutcomes.peacefulTransferByPetition(level, capital, player, villagerEntity.getUUID());

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.THRONE_SUCCESS,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );
    }

    static void handleSeizeThrone(
            ServerPlayer player,
            Entity villagerEntity,
            int minReputation,
            int commanderHearts,
            double maxAudienceDistanceSqr,
            ResourceLocation requiredAdvancementId
    ) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalPetitionRequirements.resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NO_SOVEREIGN);
            return;
        }

        if (capital.getVillageId() == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_MISSING_VILLAGE);
            return;
        }

        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, maxAudienceDistanceSqr)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NOT_IN_AUDIENCE);
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int reputation = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (reputation < minReputation) {
            CapitalPetitionOutcomes.applyCapitalPenalty(level, residents, player.getUUID(), -50);
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_LOW_REPUTATION);
            return;
        }

        if (!CapitalPetitionRequirements.hasAdvancement(player, requiredAdvancementId)) {
            CapitalPetitionOutcomes.applyCapitalPenalty(level, residents, player.getUUID(), -50);
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NO_ADVANCEMENT);
            return;
        }

        boolean playerIsCommander = PlayerCapitalTitleService.isCommander(level, capital, player.getUUID());
        boolean commanderAligned = CapitalPetitionRequirements.hasCommanderAllegiance(
                level,
                capital,
                player.getUUID(),
                commanderHearts
        );

        if (!playerIsCommander && !commanderAligned) {
            CapitalPetitionOutcomes.applyCapitalPenalty(level, residents, player.getUUID(), -50);
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.SEIZE_THRONE_NO_COMMANDER_SUPPORT);
            return;
        }

        CapitalPetitionOutcomes.performCoup(level, capital, player);

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.SEIZE_THRONE_SUCCESS,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );
    }
}