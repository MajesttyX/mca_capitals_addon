package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalMaesterSelection {

    private CapitalMaesterSelection() {
    }

    static boolean isEligibleForNewGrandMaester(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null || capital == null || capital.getVillageId() == null) {
            return false;
        }

        return MCAIntegrationBridge.getVillagePopulation(
                level,
                capital.getVillageId()
        ) >= CapitalMaesterService.REQUIRED_POPULATION;
    }

    static List<UUID> getMaesterPool(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        List<UUID> result = new ArrayList<>();

        if (residents == null) {
            return result;
        }

        for (UUID residentId : residents) {
            if (isMaester(level, capital, residentId, residents)) {
                result.add(residentId);
            }
        }

        Collections.sort(result);
        return result;
    }

    static UUID findGrandMaesterCandidate(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        List<UUID> pool = getMaesterPool(level, capital, residents);

        if (pool.isEmpty()) {
            return null;
        }

        return pool.get(level.random.nextInt(pool.size()));
    }

    static boolean isMaester(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents
    ) {
        if (level == null || capital == null || villagerId == null) {
            return false;
        }

        if (residents == null || !residents.contains(villagerId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isMasterClericVillager(level, villagerId)) {
            return false;
        }

        if (CapitalAmbassadorService.isAmbassador(level, villagerId)) {
            return false;
        }

        if (villagerId.equals(capital.getSovereign())
                || villagerId.equals(capital.getConsort())
                || villagerId.equals(capital.getDowager())
                || villagerId.equals(capital.getHeir())
                || villagerId.equals(capital.getCommander())
                || villagerId.equals(capital.getHand())) {
            return false;
        }

        if (capital.isRoyalChild(villagerId)
                || capital.isLegitimizedRoyalChild(villagerId)
                || capital.isPrinceConsort(villagerId)
                || capital.isDowagerPrince(villagerId)
                || capital.isDuke(villagerId)
                || capital.isMarriageDuke(villagerId)
                || capital.isDowagerDuke(villagerId)) {
            return false;
        }

        return true;
    }

    static boolean isValidGrandMaester(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents
    ) {
        return isMaester(level, capital, villagerId, residents);
    }
}