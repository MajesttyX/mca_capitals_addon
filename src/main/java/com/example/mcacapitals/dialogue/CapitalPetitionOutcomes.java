package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalRoyalGuardService;
import com.example.mcacapitals.capital.CapitalRoyalHouseholdService;
import com.example.mcacapitals.capital.CapitalState;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class CapitalPetitionOutcomes {

    private CapitalPetitionOutcomes() {
    }

    static void performCoup(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
        UUID formerSovereignId = capital.getSovereign();
        String formerSovereignName = resolveLoadedName(level, formerSovereignId);
        String playerName = player.getName().getString();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);

        Set<UUID> formerRoyalFamily = collectRoyalFamily(capital);
        formerRoyalFamily.remove(player.getUUID());

        CapitalFoundationService.appointPlayerSovereign(level, capital, player.getUUID(), female);
        CapitalRoyalGuardService.clearRoyalGuardsForTransfer(level, capital);

        stripFormerRoyalFamily(capital);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(player.getUUID());
        capital.setPlayerSovereignName(playerName);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        applyHeartsPenaltyToFamily(level, formerRoyalFamily, player.getUUID(), -200);

        CapitalChronicleService.addEntry(
                level,
                capital,
                playerName + " seized the throne of " + villageName
                        + " from " + formerSovereignName + " by force."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    static void applyCapitalPenalty(ServerLevel level, Set<UUID> residents, UUID playerId, int delta) {
        if (level == null || residents == null || playerId == null) {
            return;
        }

        for (UUID residentId : residents) {
            adjustHearts(level, residentId, playerId, delta);
        }
    }

    static void peacefulTransferByPetition(ServerLevel level, CapitalRecord capital, ServerPlayer player, UUID formerSovereignId) {
        String formerSovereignName = resolveLoadedName(level, formerSovereignId);
        String playerName = player.getName().getString();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        boolean female = MCAIntegrationBridge.isPlayerFemale(level, player);

        CapitalFoundationService.appointPlayerSovereign(level, capital, player.getUUID(), female);
        CapitalRoyalGuardService.clearRoyalGuardsForTransfer(level, capital);

        stripFormerRoyalFamily(capital);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(player.getUUID());
        capital.setPlayerSovereignName(playerName);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        CapitalChronicleService.addEntry(
                level,
                capital,
                formerSovereignName + " accepted the petition of " + playerName
                        + ", and the throne of " + villageName + " passed peacefully into new hands."
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    private static Set<UUID> collectRoyalFamily(CapitalRecord capital) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (capital == null) {
            return ids;
        }

        if (capital.getSovereign() != null) ids.add(capital.getSovereign());
        if (capital.getConsort() != null) ids.add(capital.getConsort());
        if (capital.getDowager() != null) ids.add(capital.getDowager());
        if (capital.getHeir() != null) ids.add(capital.getHeir());

        ids.addAll(capital.getRoyalChildren());
        ids.addAll(capital.getDisinheritedRoyalChildren());
        ids.addAll(capital.getLegitimizedRoyalChildren());

        return ids;
    }

    private static void stripFormerRoyalFamily(CapitalRecord capital) {
        if (capital == null) {
            return;
        }

        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setDowager(null);
        capital.setDowagerFemale(false);
        capital.setHeir(null);
        capital.setHeirFemale(false);
        capital.setHeirMode(CapitalRecord.HeirMode.NONE);

        capital.getRoyalChildren().clear();
        capital.getRoyalChildFemale().clear();
        capital.getDisinheritedRoyalChildren().clear();
        capital.getLegitimizedRoyalChildren().clear();
        capital.getLegitimizedRoyalChildFemale().clear();
        capital.getRoyalSuccessionOrder().clear();
        capital.clearRoyalHousehold();
    }

    private static void applyHeartsPenaltyToFamily(ServerLevel level, Set<UUID> familyIds, UUID playerId, int delta) {
        if (level == null || familyIds == null || playerId == null) {
            return;
        }

        for (UUID familyId : familyIds) {
            adjustHearts(level, familyId, playerId, delta);
        }
    }

    private static void adjustHearts(ServerLevel level, UUID villagerId, UUID playerId, int delta) {
        if (!MCAIntegrationBridge.adjustHearts(level, villagerId, playerId, delta)) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Failed to adjust hearts for villager='{}' player='{}'",
                    villagerId,
                    playerId
            );
        }
    }

    private static String resolveLoadedName(ServerLevel level, UUID entityId) {
        var entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }
}