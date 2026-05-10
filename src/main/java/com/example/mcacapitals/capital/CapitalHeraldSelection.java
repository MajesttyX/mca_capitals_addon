package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalHeraldSelection {

    private CapitalHeraldSelection() {
    }

    static List<UUID> getHeraldPool(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        List<UUID> result = new ArrayList<>();
        if (residents == null) {
            return result;
        }

        for (UUID residentId : residents) {
            if (isEligibleHerald(level, capital, residentId, residents)) {
                result.add(residentId);
            }
        }

        Collections.sort(result);
        return result;
    }

    static UUID findHeraldCandidate(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        List<UUID> pool = getHeraldPool(level, capital, residents);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(0);
    }

    static boolean isValidHerald(ServerLevel level, CapitalRecord capital, UUID villagerId, Set<UUID> residents) {
        if (level == null || capital == null || villagerId == null) {
            return false;
        }
        if (!MCAIntegrationBridge.isAliveMCAVillager(level, villagerId)) {
            return false;
        }
        return isEligibleHeraldIgnoringTransientResidentScan(level, capital, villagerId);
    }

    static boolean isEligibleHerald(ServerLevel level, CapitalRecord capital, UUID villagerId, Set<UUID> residents) {
        if (level == null || capital == null || villagerId == null) {
            return false;
        }
        if (residents == null || !residents.contains(villagerId)) {
            return false;
        }
        if (!MCAIntegrationBridge.isAliveMCAVillager(level, villagerId)) {
            return false;
        }
        return isEligibleHeraldIgnoringTransientResidentScan(level, capital, villagerId);
    }

    private static boolean isEligibleHeraldIgnoringTransientResidentScan(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (MCAIntegrationBridge.isMasterProfessionVillager(level, villagerId)) {
            return false;
        }
        if (villagerId.equals(capital.getSovereign())
                || villagerId.equals(capital.getConsort())
                || villagerId.equals(capital.getDowager())
                || villagerId.equals(capital.getHeir())
                || villagerId.equals(capital.getCommander())
                || villagerId.equals(capital.getHand())
                || villagerId.equals(capital.getGrandMaester())) {
            return false;
        }
        if (capital.isRoyalChild(villagerId)
                || capital.isLegitimizedRoyalChild(villagerId)
                || capital.isPrinceConsort(villagerId)
                || capital.isDowagerPrince(villagerId)
                || capital.isDuke(villagerId)
                || capital.isMarriageDuke(villagerId)
                || capital.isDowagerDuke(villagerId)
                || capital.isLord(villagerId)
                || capital.isKnight(villagerId)
                || capital.isRoyalGuard(villagerId)) {
            return false;
        }
        return true;
    }
}