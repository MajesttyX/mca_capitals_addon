package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

final class CapitalSovereignAppointmentService {

    private CapitalSovereignAppointmentService() {
    }

    static void appointVillagerSovereign(ServerLevel level, CapitalRecord capital, UUID villagerId, boolean female) {
        UUID previous = capital.getSovereign();
        UUID previousPlayerSovereignId = capital.getPlayerSovereignId();

        clearPlayerSovereignState(capital);

        capital.setSovereign(villagerId);
        capital.setSovereignFemale(female);
        capital.setState(CapitalState.ACTIVE);
        capital.setMonarchyRejected(false);

        CapitalFoundationInternal.refreshCourt(level, capital);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        if (previousPlayerSovereignId != null && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, previousPlayerSovereignId, capital.getCapitalId());
        }

        if (!villagerId.equals(previous)) {
            String title = female ? "Queen" : "King";
            String name = MCAIntegrationBridge.getEntityByUuid(level, villagerId) != null
                    ? MCAIntegrationBridge.getEntityByUuid(level, villagerId).getName().getString()
                    : villagerId.toString();

            CapitalChronicleService.addEntry(level, capital,
                    name + " was acclaimed as " + title + " of "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
        }

        CapitalManager.putCapital(capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    static void appointPlayerSovereign(ServerLevel level, CapitalRecord capital, UUID playerId, boolean female) {
        UUID previous = capital.getSovereign();
        UUID previousPlayerSovereignId = capital.getPlayerSovereignId();

        capital.setSovereign(playerId);
        capital.setSovereignFemale(female);
        capital.setState(CapitalState.ACTIVE);
        capital.setMonarchyRejected(false);

        capital.setPlayerSovereign(true);
        capital.setPlayerSovereignId(playerId);
        capital.setPlayerSovereignName(CapitalFoundationInternal.resolvePlayerName(level, playerId));
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);

        CapitalFoundationInternal.refreshCourt(level, capital);
        CapitalRoyalHouseholdService.beginNewRegime(capital);

        if (previousPlayerSovereignId != null
                && !previousPlayerSovereignId.equals(playerId)
                && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, previousPlayerSovereignId, capital.getCapitalId());
        }

        if (capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, playerId, capital.getCapitalId());
        }

        if (!playerId.equals(previous)) {
            String title = female ? "Queen" : "King";
            String name = CapitalFoundationInternal.resolvePlayerName(level, playerId);

            CapitalChronicleService.addEntry(level, capital,
                    name + " claimed the throne as " + title + " of "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
        }

        CapitalManager.putCapital(capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    static void clearPlayerSovereignState(CapitalRecord capital) {
        capital.setPlayerSovereign(false);
        capital.setPlayerSovereignId(null);
        capital.setPlayerSovereignName(null);
    }
}