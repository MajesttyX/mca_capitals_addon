package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;

public class CapitalFoundationService {

    private CapitalFoundationService() {
    }

    public static void appointVillagerSovereign(ServerLevel level, CapitalRecord capital, UUID villagerId, boolean female) {
        if (level == null || capital == null || villagerId == null) {
            return;
        }

        CapitalSovereignAppointmentService.appointVillagerSovereign(level, capital, villagerId, female);
    }

    public static void appointPlayerSovereign(ServerLevel level, CapitalRecord capital, UUID playerId, boolean female) {
        if (level == null || capital == null || playerId == null) {
            return;
        }

        CapitalSovereignAppointmentService.appointPlayerSovereign(level, capital, playerId, female);
    }

    public static void assignDuke(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (level == null || capital == null || villagerId == null) {
            return;
        }

        capital.addDuke(villagerId, MCAIntegrationBridge.isFemale(level, villagerId));

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalCourtBuilder.rebuildCourt(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);

        String dukeName = MCAIntegrationBridge.getEntityByUuid(level, villagerId) != null
                ? MCAIntegrationBridge.getEntityByUuid(level, villagerId).getName().getString()
                : villagerId.toString();

        UUID spouse = MCAIntegrationBridge.getSpouse(level, villagerId);
        if (spouse != null && residents.contains(spouse)) {
            String spouseName = MCAIntegrationBridge.getEntityByUuid(level, spouse) != null
                    ? MCAIntegrationBridge.getEntityByUuid(level, spouse).getName().getString()
                    : spouse.toString();

            CapitalChronicleService.addEntry(level, capital,
                    dukeName + " was raised to ducal rank, and " + spouseName
                            + " entered the court by marriage.");
        } else {
            CapitalChronicleService.addEntry(level, capital,
                    dukeName + " was raised to ducal rank in "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
        }

        CapitalManager.putCapital(capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    public static boolean abdicateSovereign(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getSovereign() == null) {
            return false;
        }

        return CapitalFoundationInternal.abdicateSovereign(level, capital);
    }

    public static void refreshCourt(ServerLevel level, CapitalRecord capital) {
        CapitalFoundationInternal.refreshCourt(level, capital);
    }
}