package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCommanderService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.MCAReputationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

final class CapitalPetitionTitleActions {

    private CapitalPetitionTitleActions() {
    }

    static void handleCommanderPetition(
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

        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, maxAudienceDistanceSqr)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        if (population < minPopulation) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.COMMANDER_POPULATION_TOO_LOW,
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
                    CapitalDialogueKey.COMMANDER_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        if (CapitalCommanderService.hasOtherPlayerCommander(level, capital, player.getUUID())) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.COMMANDER_ALREADY_GRANTED);
            return;
        }

        UUID currentPlayerCommander = CapitalCommanderService.getPlayerCommander(level, capital);
        if (currentPlayerCommander != null && currentPlayerCommander.equals(player.getUUID())) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.COMMANDER_ALREADY_HELD);
            return;
        }

        boolean appointed = CapitalCommanderService.appointPlayerCommander(level, capital, player);
        if (!appointed) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.COMMANDER_REASSIGN_FAILED);
            return;
        }

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.COMMANDER_SUCCESS,
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );
    }

    static void handleLordPetition(
            ServerPlayer player,
            Entity villagerEntity,
            int minHearts,
            int minMasterVillagers,
            double maxAudienceDistanceSqr
    ) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalPetitionRequirements.resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, maxAudienceDistanceSqr)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        NobleTitle currentTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, player.getUUID());
        if (currentTitle == NobleTitle.DUKE || currentTitle == NobleTitle.DUCHESS) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.LORD_ALREADY_HIGHER);
            return;
        }
        if (currentTitle == NobleTitle.LORD || currentTitle == NobleTitle.LADY) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.LORD_ALREADY_HELD);
            return;
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());
        int hearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (hearts < minHearts) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.LORD_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        int masters = CapitalPetitionRequirements.countMasterProfessionVillagers(level, residents);
        if (masters < minMasterVillagers) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.LORD_NOT_ENOUGH_MASTERS,
                    minMasterVillagers
            );
            return;
        }

        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);
        NobleTitle granted = female ? NobleTitle.LADY : NobleTitle.LORD;
        PlayerCapitalTitleService.grantTitle(level, capital, player.getUUID(), granted);

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        CapitalChronicleService.addEntry(
                level,
                capital,
                player.getName().getString() + " was raised to the dignity of "
                        + (female ? "Lady" : "Lord") + " in " + villageName + "."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.LORD_SUCCESS,
                female ? "Lady" : "Lord",
                villageName
        );
    }

    static void handleDukePetition(
            ServerPlayer player,
            Entity villagerEntity,
            int minHearts,
            int minPopulation,
            double maxAudienceDistanceSqr
    ) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalPetitionRequirements.resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, maxAudienceDistanceSqr)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        NobleTitle currentTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, player.getUUID());
        if (currentTitle == NobleTitle.DUKE || currentTitle == NobleTitle.DUCHESS) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.DUKE_ALREADY_HELD);
            return;
        }

        int population = MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId());
        if (population < minPopulation) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.DUKE_POPULATION_TOO_LOW,
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
                    CapitalDialogueKey.DUKE_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);
        NobleTitle granted = female ? NobleTitle.DUCHESS : NobleTitle.DUKE;
        PlayerCapitalTitleService.grantTitle(level, capital, player.getUUID(), granted);

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        CapitalChronicleService.addEntry(
                level,
                capital,
                player.getName().getString() + " was raised to the dignity of "
                        + (female ? "Duchess" : "Duke") + " in " + villageName + "."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.DUKE_SUCCESS,
                female ? "Duchess" : "Duke",
                villageName
        );
    }
}