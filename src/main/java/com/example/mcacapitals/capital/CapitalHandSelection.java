package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

final class CapitalHandSelection {

    private CapitalHandSelection() {
    }

    static boolean isEligibleForNewHand(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getVillageId() == null) {
            return false;
        }

        return MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId()) >= CapitalHandService.REQUIRED_POPULATION;
    }

    static UUID findBestHandCandidate(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        UUID duke = capital.getDukes().stream()
                .filter(id -> isEligibleHandCandidate(level, capital, id, residents))
                .max(candidateComparator())
                .orElse(null);
        if (duke != null) {
            return duke;
        }

        UUID lord = capital.getLords().stream()
                .filter(id -> isEligibleHandCandidate(level, capital, id, residents))
                .max(candidateComparator())
                .orElse(null);
        if (lord != null) {
            return lord;
        }

        return capital.getKnights().stream()
                .filter(id -> isEligibleHandCandidate(level, capital, id, residents))
                .max(candidateComparator())
                .orElse(null);
    }

    static boolean isValidHand(ServerLevel level, CapitalRecord capital, UUID handId, Set<UUID> residents) {
        if (level == null || capital == null || handId == null) {
            return false;
        }
        if (!CapitalRoleValidation.isExistingRoleStillResolvable(level, handId, residents)) {
            return false;
        }
        if (handId.equals(capital.getSovereign())
                || handId.equals(capital.getConsort())
                || handId.equals(capital.getDowager())
                || handId.equals(capital.getHeir())
                || handId.equals(capital.getCommander())) {
            return false;
        }
        if (capital.isRoyalChild(handId)
                || capital.isLegitimizedRoyalChild(handId)
                || capital.isPrinceConsort(handId)
                || capital.isDowagerPrince(handId)
                || capital.isDowagerDuke(handId)
                || capital.isMarriageDuke(handId)) {
            return false;
        }

        if (CapitalRoleValidation.isLoadedDeadOrRemoved(level, handId)) {
            return false;
        }

        if (!CapitalRoleValidation.isCurrentlyLoaded(level, handId)) {
            return true;
        }

        return capital.isDuke(handId) || capital.isLord(handId) || capital.isKnight(handId);
    }

    static boolean isEligibleHandCandidate(ServerLevel level, CapitalRecord capital, UUID candidateId, Set<UUID> residents) {
        if (level == null || capital == null || candidateId == null) {
            return false;
        }
        if (residents == null || !residents.contains(candidateId)) {
            return false;
        }
        if (!MCAIntegrationBridge.isMCAVillager(level, candidateId)) {
            return false;
        }
        if (candidateId.equals(capital.getSovereign())
                || candidateId.equals(capital.getConsort())
                || candidateId.equals(capital.getDowager())
                || candidateId.equals(capital.getHeir())
                || candidateId.equals(capital.getCommander())) {
            return false;
        }
        if (capital.isRoyalChild(candidateId)
                || capital.isLegitimizedRoyalChild(candidateId)
                || capital.isPrinceConsort(candidateId)
                || capital.isDowagerPrince(candidateId)
                || capital.isDowagerDuke(candidateId)
                || capital.isMarriageDuke(candidateId)) {
            return false;
        }
        return capital.isDuke(candidateId) || capital.isLord(candidateId) || capital.isKnight(candidateId);
    }

    private static Comparator<UUID> candidateComparator() {
        return Comparator.comparing(UUID::toString);
    }
}