package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalPopulationScanner {

    private static final int REQUIRED_POPULATION = 30;
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        tickCounter++;
        if (tickCounter < 100) {
            return;
        }
        tickCounter = 0;

        boolean changed = normalizeCapitals(level);

        Set<Integer> qualifyingVillageIds = MCAIntegrationBridge.getVillageIdsAtOrAbovePopulation(level, REQUIRED_POPULATION);
        for (int villageId : qualifyingVillageIds) {
            if (!CapitalManager.hasCapitalForVillageId(villageId)) {
                UUID capitalId = UUID.randomUUID();
                CapitalRecord capital = new CapitalRecord(capitalId, villageId, null, false);
                capital.setState(CapitalState.PENDING);
                CapitalManager.getAllCapitals().put(capitalId, capital);
                CapitalCourtWatcher.clearFingerprint(capitalId);
                changed = true;
            }
        }

        if (changed) {
            CapitalDataAccess.markDirty(level);
        }

        for (CapitalRecord capital : new ArrayList<>(CapitalManager.getAllCapitals().values())) {

            if (capital.getVillageId() != null && !MCAIntegrationBridge.hasVillage(level, capital.getVillageId())) {
                continue;
            }

            if (capital.getSovereign() != null) {
                if (CapitalSuccessionService.handleSuccessionIfNeeded(level, capital)) {
                    CapitalDataAccess.markDirty(level);
                    continue;
                }
            }

            if (CapitalCourtWatcher.refreshIfChanged(level, capital)) {
                CapitalDataAccess.markDirty(level);
            }
        }
    }

    private boolean normalizeCapitals(ServerLevel level) {
        boolean changed = false;

        for (CapitalRecord capital : new ArrayList<>(CapitalManager.getAllCapitals().values())) {
            if (capital.getVillageId() == null) {
                Integer resolvedVillageId = null;

                if (capital.getSovereign() != null) {
                    resolvedVillageId = MCAIntegrationBridge.getVillageIdForResident(level, capital.getSovereign());
                }

                if (resolvedVillageId == null && capital.getConsort() != null) {
                    resolvedVillageId = MCAIntegrationBridge.getVillageIdForResident(level, capital.getConsort());
                }

                if (resolvedVillageId != null) {
                    capital.setVillageId(resolvedVillageId);
                    changed = true;
                }
            }
        }

        Map<Integer, CapitalRecord> preferredByVillage = new HashMap<>();

        for (CapitalRecord capital : CapitalManager.getAllCapitals().values()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null) {
                continue;
            }

            CapitalRecord existing = preferredByVillage.get(villageId);
            if (existing == null || isPreferred(capital, existing)) {
                preferredByVillage.put(villageId, capital);
            }
        }

        for (CapitalRecord capital : new ArrayList<>(CapitalManager.getAllCapitals().values())) {
            Integer villageId = capital.getVillageId();

            if (villageId == null) {
                if (capital.getSovereign() == null && capital.getConsort() == null && capital.getDowager() == null) {
                    CapitalManager.removeCapital(capital.getCapitalId());
                    CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
                    changed = true;
                }
                continue;
            }

            CapitalRecord preferred = preferredByVillage.get(villageId);
            if (preferred != null && !preferred.getCapitalId().equals(capital.getCapitalId())) {
                CapitalManager.removeCapital(capital.getCapitalId());
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
                changed = true;
            }
        }

        return changed;
    }

    private boolean isPreferred(CapitalRecord candidate, CapitalRecord existing) {
        if (candidate.getSovereign() != null && existing.getSovereign() == null) {
            return true;
        }
        if (candidate.getSovereign() == null && existing.getSovereign() != null) {
            return false;
        }

        if (candidate.getState() == CapitalState.ACTIVE && existing.getState() != CapitalState.ACTIVE) {
            return true;
        }
        if (candidate.getState() != CapitalState.ACTIVE && existing.getState() == CapitalState.ACTIVE) {
            return false;
        }

        int candidateWeight = candidate.getRoyalChildren().size()
                + candidate.getDukes().size()
                + candidate.getLords().size()
                + candidate.getKnights().size();

        int existingWeight = existing.getRoyalChildren().size()
                + existing.getDukes().size()
                + existing.getLords().size()
                + existing.getKnights().size();

        if (candidateWeight != existingWeight) {
            return candidateWeight > existingWeight;
        }

        return candidate.getCapitalId().toString().compareTo(existing.getCapitalId().toString()) < 0;
    }
}