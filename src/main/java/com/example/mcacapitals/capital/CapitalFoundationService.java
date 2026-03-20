package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CapitalFoundationService {

    private CapitalFoundationService() {
    }

    public static void appointVillagerSovereign(ServerLevel level, CapitalRecord capital, UUID villagerId, boolean female) {
        if (level == null || capital == null || villagerId == null) {
            return;
        }

        capital.setSovereign(villagerId);
        capital.setSovereignFemale(female);
        capital.setState(CapitalState.ACTIVE);

        refreshCourt(level, capital);

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    public static void appointPlayerSovereign(ServerLevel level, CapitalRecord capital, UUID playerId, boolean female) {
        if (level == null || capital == null || playerId == null) {
            return;
        }

        capital.setSovereign(playerId);
        capital.setSovereignFemale(female);
        capital.setState(CapitalState.ACTIVE);

        refreshCourt(level, capital);

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    public static void assignDuke(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (level == null || capital == null || villagerId == null) {
            return;
        }

        capital.addDuke(villagerId, MCAIntegrationBridge.isFemale(level, villagerId));

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalCourtBuilder.rebuildCourt(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    public static void refreshCourt(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return;
        }

        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return;
        }

        boolean sovereignFemale = capital.isSovereignFemale();
        UUID dowager = capital.getDowager();
        boolean dowagerFemale = capital.isDowagerFemale();

        Set<UUID> dukes = new HashSet<>(capital.getDukes());
        HashMap<UUID, Boolean> dukeFemale = new HashMap<>(capital.getDukeFemale());

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        CapitalCourtBuilder.rebuildCourt(level, capital, residents);

        capital.setSovereign(sovereign);
        capital.setSovereignFemale(sovereignFemale);
        capital.setDowager(dowager);
        capital.setDowagerFemale(dowagerFemale);

        capital.getDukes().clear();
        capital.getDukes().addAll(dukes);

        capital.getDukeFemale().clear();
        capital.getDukeFemale().putAll(dukeFemale);

        CapitalCourtBuilder.rebuildCourt(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);

        if (capital.getSovereign() != null) {
            capital.setState(CapitalState.ACTIVE);
        }

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalDataAccess.markDirty(level);
    }
}