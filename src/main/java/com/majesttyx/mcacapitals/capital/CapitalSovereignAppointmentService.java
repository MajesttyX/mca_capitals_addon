package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.item.RoyalScepterGrantService;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
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
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalHeraldService.tickHerald(level, capital, residents, false);

        if (previousPlayerSovereignId != null && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, previousPlayerSovereignId, capital.getCapitalId());
        }

        if (!villagerId.equals(previous)) {
            String title = female ? "Queen" : "King";
            String name = CapitalChronicleIdentitySnapshot.name(level, capital, villagerId);

            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.SOVEREIGN_ACCLAIMED,
                    name,
                    CapitalChronicleIdentitySnapshot.title(level, capital, villagerId),
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
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
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalHeraldService.tickHerald(level, capital, residents, false);

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            RoyalScepterGrantService.grantScepter(player);
        }

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
            String name = CapitalChronicleIdentitySnapshot.name(level, capital, playerId);

            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.THRONE_CLAIMED,
                    name,
                    CapitalChronicleIdentitySnapshot.title(level, capital, playerId),
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
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