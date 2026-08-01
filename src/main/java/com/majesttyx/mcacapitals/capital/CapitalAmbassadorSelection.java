package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalAmbassadorSelection {

    private CapitalAmbassadorSelection() {
    }

    static UUID findCandidate(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || residents == null) {
            return null;
        }

        List<UUID> candidates = new ArrayList<>();

        for (UUID residentId : residents) {
            if (isEligible(
                    level,
                    capital,
                    residentId,
                    residents
            )) {
                candidates.add(residentId);
            }
        }

        candidates.sort(
                Comparator.comparing(UUID::toString)
        );

        return candidates.isEmpty()
                ? null
                : candidates.get(0);
    }

    static boolean isValid(
            ServerLevel level,
            CapitalRecord capital,
            UUID ambassadorId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || ambassadorId == null) {
            return false;
        }

        if (!CapitalRoleValidation
                .isExistingRoleStillResolvable(
                        level,
                        ambassadorId,
                        residents
                )) {
            return false;
        }

        if (!CapitalRoleValidation.isCurrentlyLoaded(
                level,
                ambassadorId
        )) {
            return true;
        }

        return isEligibleIgnoringResidentScan(
                level,
                capital,
                ambassadorId
        );
    }

    static boolean isEligible(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || candidateId == null
                || residents == null
                || !residents.contains(candidateId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isAliveMCAVillager(
                level,
                candidateId
        )) {
            return false;
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(
                level,
                candidateId
        )) {
            return false;
        }

        if (MCAIntegrationBridge.isMCAGuard(
                level,
                candidateId
        )
                || MCAIntegrationBridge
                .isMasterProfessionVillager(
                        level,
                        candidateId
                )) {
            return false;
        }

        return isEligibleIgnoringResidentScan(
                level,
                capital,
                candidateId
        );
    }

    private static boolean isEligibleIgnoringResidentScan(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId
    ) {
        if (candidateId.equals(capital.getSovereign())
                || candidateId.equals(capital.getConsort())
                || candidateId.equals(capital.getDowager())
                || candidateId.equals(capital.getHeir())
                || candidateId.equals(capital.getCommander())
                || candidateId.equals(capital.getHand())
                || candidateId.equals(capital.getHerald())
                || candidateId.equals(capital.getGrandMaester())
                || candidateId.equals(capital.getMasterOfLaws())) {
            return false;
        }

        if (capital.isRoyalChild(candidateId)
                || capital.isDisinheritedRoyalChild(candidateId)
                || capital.isLegitimizedRoyalChild(candidateId)
                || capital.isPrinceConsort(candidateId)
                || capital.isDowagerPrince(candidateId)
                || capital.isDuke(candidateId)
                || capital.isMarriageDuke(candidateId)
                || capital.isDowagerDuke(candidateId)
                || capital.isLord(candidateId)
                || capital.isKnight(candidateId)
                || capital.isRoyalGuard(candidateId)
                || capital.isDisgracedRoyalGuard(candidateId)) {
            return false;
        }

        return !CapitalRoleValidation.isLoadedDeadOrRemoved(
                level,
                candidateId
        );
    }
}