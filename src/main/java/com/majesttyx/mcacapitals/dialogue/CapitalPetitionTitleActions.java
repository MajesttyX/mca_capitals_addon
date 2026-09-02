package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCommanderService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalHeraldService;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.item.RoyalScepterGrantService;
import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.MCAReputationBridge;
import net.minecraft.network.chat.Component;
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

    static void handleHandPetition(
            ServerPlayer player,
            Entity villagerEntity,
            int minVillageHearts,
            int minSovereignHearts,
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

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId());

        int villageHearts = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        if (villageHearts < minVillageHearts) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.HAND_LOW_STANDING,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
            return;
        }

        int sovereignHearts = MCAReputationBridge.getHeartsWithVillager(level, capital.getSovereign(), player.getUUID());
        if (sovereignHearts < minSovereignHearts) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.HAND_LOW_SOVEREIGN_STANDING,
                    minSovereignHearts
            );
            return;
        }

        if (PlayerCapitalTitleService.isHand(level, capital, player.getUUID())) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.HAND_ALREADY_HELD);
            return;
        }

        UUID existingPlayerHand = PlayerCapitalTitleService.getHandHolder(level, capital);
        if (existingPlayerHand != null && !existingPlayerHand.equals(player.getUUID())) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.HAND_ALREADY_GRANTED);
            return;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        Component officeName = Component.translatable(
                capital.isSovereignFemale()
                        ? "mcacapitals.dynamic.office.hand.female"
                        : "mcacapitals.dynamic.office.hand.male"
        );
        UUID previousHand = capital.getHand();

        if (previousHand != null && !previousHand.equals(player.getUUID())) {
            String formerName = CapitalChronicleIdentitySnapshot.name(level, capital, previousHand);
            if (!formerName.isBlank()) {
                CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.OFFICE_RELIEVED, formerName, CapitalChronicleIdentitySnapshot.handOffice(level, capital), villageName);
            }
        }

        PlayerCapitalTitleService.revokeHandForCapital(level, capital);
        PlayerCapitalTitleService.grantHand(level, capital, player.getUUID());
        capital.setHand(player.getUUID());
        capital.setHandFemale(MCAIntegrationBridge.isPlayerFemale(level, player));

        Set<UUID> scannedResidents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalHeraldService.refreshHeraldAfterStatusChange(level, capital, scannedResidents);
        CapitalNameService.refreshCapitalNames(level, capital, scannedResidents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        RoyalScepterGrantService.grantScepter(player);

        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.HAND_APPOINTED, player.getName().getString(), CapitalChronicleIdentitySnapshot.handOffice(level, capital), villageName);

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.HAND_SUCCESS,
                officeName,
                villageName
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
        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.LORD_ELEVATED,
                player.getName().getString(),
                CapitalChronicleIdentitySnapshot.genderedTitle(
                        "lord",
                        female
                                ? CapitalDialogueGenderResolver.ResolvedGender.FEMALE
                                : CapitalDialogueGenderResolver.ResolvedGender.MALE
                ),
                villageName,
                CapitalChronicleIdentitySnapshot.style(level, capital, player.getUUID())
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.LORD_SUCCESS,
                Component.translatable(
                        female
                                ? "mcacapitals.dynamic.title.lord.female"
                                : "mcacapitals.dynamic.title.lord.male"
                ),
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
        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.DUKE_ELEVATED,
                player.getName().getString(),
                CapitalChronicleIdentitySnapshot.genderedTitle(
                        "duke",
                        female
                                ? CapitalDialogueGenderResolver.ResolvedGender.FEMALE
                                : CapitalDialogueGenderResolver.ResolvedGender.MALE
                ),
                villageName,
                CapitalChronicleIdentitySnapshot.style(level, capital, player.getUUID())
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                player,
                villagerEntity,
                CapitalDialogueKey.DUKE_SUCCESS,
                Component.translatable(
                        female
                                ? "mcacapitals.dynamic.title.duke.female"
                                : "mcacapitals.dynamic.title.duke.male"
                ),
                villageName
        );
    }
}