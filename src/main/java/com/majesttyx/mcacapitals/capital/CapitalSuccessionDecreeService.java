package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

public final class CapitalSuccessionDecreeService {

    private CapitalSuccessionDecreeService() {
    }

    public static boolean transferToVillager(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null) {
            return false;
        }

        UUID previousSovereign = capital.getSovereign();
        UUID previousPlayerSovereignId = capital.getPlayerSovereignId();

        String previousName = resolveName(level, previousSovereign);
        String targetName = resolveName(level, targetId);
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        CapitalSovereignAppointmentService.clearPlayerSovereignState(capital);
        clearPreviousDynastyForPeacefulTransfer(capital);

        capital.setSovereign(targetId);
        capital.setSovereignFemale(MCAIntegrationBridge.isFemale(level, targetId));
        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);
        capital.setMonarchyRejected(false);

        CapitalFoundationInternal.refreshCourt(level, capital);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalHeraldService.tickHerald(level, capital, residents, false);
        CapitalNameService.refreshCapitalNames(level, capital, residents);

        if (previousPlayerSovereignId != null && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, previousPlayerSovereignId, capital.getCapitalId());
        }

        CapitalChronicleService.addEntry(
                level,
                capital,
                previousName + " peacefully transferred the crown of " + villageName + " to " + targetName + " by Succession Decree."
        );

        CapitalManager.putCapital(capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
        return true;
    }

    public static boolean transferToPlayer(ServerLevel level, CapitalRecord capital, ServerPlayer targetPlayer) {
        if (level == null || capital == null || targetPlayer == null) {
            return false;
        }

        UUID previousSovereign = capital.getSovereign();
        UUID previousPlayerSovereignId = capital.getPlayerSovereignId();
        UUID targetId = targetPlayer.getUUID();

        String previousName = resolveName(level, previousSovereign);
        String targetName = targetPlayer.getName().getString();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        clearPreviousDynastyForPeacefulTransfer(capital);

        capital.setSovereign(targetId);
        capital.setSovereignFemale(MCAIntegrationBridge.isPlayerFemale(level, targetPlayer));
        capital.setState(CapitalState.ACTIVE);
        capital.setMonarchyRejected(false);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(targetId);
        capital.setPlayerSovereignName(targetName);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setConsort(null);
        capital.setConsortFemale(false);

        CapitalFoundationInternal.refreshCourt(level, capital);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalHeraldService.tickHerald(level, capital, residents, false);
        CapitalNameService.refreshCapitalNames(level, capital, residents);

        if (previousPlayerSovereignId != null
                && !previousPlayerSovereignId.equals(targetId)
                && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, previousPlayerSovereignId, capital.getCapitalId());
        }

        if (capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, targetId, capital.getCapitalId());
        }

        CapitalChronicleService.addEntry(
                level,
                capital,
                previousName + " peacefully transferred the crown of " + villageName + " to " + targetName + " by Succession Decree."
        );

        CapitalManager.putCapital(capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static void clearPreviousDynastyForPeacefulTransfer(CapitalRecord capital) {
        if (capital == null) {
            return;
        }

        capital.setHeir(null);
        capital.setHeirFemale(false);
        capital.setHeirMode(CapitalRecord.HeirMode.NONE);

        capital.getRoyalChildren().clear();
        capital.getRoyalChildFemale().clear();

        capital.getLegitimizedRoyalChildren().clear();
        capital.getLegitimizedRoyalChildFemale().clear();

        capital.getDisinheritedRoyalChildren().clear();
        capital.getRoyalSuccessionOrder().clear();

        capital.getPrinceConsortSources().clear();
        capital.getPrinceConsortFemale().clear();

        capital.getDowagerPrinceSources().clear();
        capital.getDowagerPrinceFemale().clear();

        capital.clearRoyalHousehold();
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return "Unknown";
        }

        if (level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entityId);
            if (player != null) {
                return player.getName().getString();
            }
        }

        if (MCAIntegrationBridge.getEntityByUuid(level, entityId) != null) {
            return MCAIntegrationBridge.getEntityByUuid(level, entityId).getName().getString();
        }

        return entityId.toString();
    }
}