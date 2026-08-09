package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPublicCrownStatus;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalMasterOfLawsSelection {

    private CapitalMasterOfLawsSelection() {
    }

    public static UUID select(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        UUID duke = firstEligibleDuke(level, capital, residents, false);
        if (duke != null) {
            return duke;
        }

        UUID knight = firstEligibleKnightOrDame(level, capital, residents);
        if (knight != null) {
            return knight;
        }

        UUID duchess = firstEligibleDuke(level, capital, residents, true);
        if (duchess != null) {
            return duchess;
        }

        return firstEligibleCommoner(level, capital, residents);
    }

    public static boolean isEligible(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            UUID entityId
    ) {
        if (level == null
                || capital == null
                || residents == null
                || entityId == null
                || !residents.contains(entityId)) {
            return false;
        }

        return isEligibleLoadedCandidate(level, capital, entityId);
    }

    /**
     * Validates an already-appointed Master of Laws without treating a temporary
     * resident-scan miss or an unloaded villager as a vacancy.
     */
    public static boolean isValidCurrentHolder(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            UUID entityId
    ) {
        if (level == null || capital == null || entityId == null) {
            return false;
        }

        if (!CapitalRoleValidation.isExistingRoleStillResolvable(level, entityId, residents)) {
            return false;
        }

        if (!isTrustedOfficeEligible(level, capital, entityId)
                || isAmbassador(level, entityId)
                || hasIncompatibleOffice(capital, entityId)) {
            return false;
        }

        if (!CapitalRoleValidation.isCurrentlyLoaded(level, entityId)) {
            return true;
        }

        Integer assignedVillageId = MCAIntegrationBridge.getVillageIdForResident(level, entityId);
        if (assignedVillageId != null
                && capital.getVillageId() != null
                && !capital.getVillageId().equals(assignedVillageId)) {
            return false;
        }

        if (assignedVillageId == null
                && residents != null
                && !residents.contains(entityId)) {
            return false;
        }

        return MCAIntegrationBridge.isTeenOrAdultVillager(level, entityId)
                && MCAIntegrationBridge.isAliveMCAVillager(level, entityId);
    }

    private static boolean isEligibleLoadedCandidate(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (!isTrustedOfficeEligible(level, capital, entityId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(level, entityId)
                || !MCAIntegrationBridge.isAliveMCAVillager(level, entityId)) {
            return false;
        }

        if (isAmbassador(level, entityId)) {
            return false;
        }

        return !hasIncompatibleOffice(capital, entityId);
    }

    private static boolean hasIncompatibleOffice(CapitalRecord capital, UUID entityId) {
        return entityId.equals(capital.getSovereign())
                || entityId.equals(capital.getConsort())
                || entityId.equals(capital.getDowager())
                || entityId.equals(capital.getHeir())
                || entityId.equals(capital.getHand())
                || entityId.equals(capital.getCommander())
                || entityId.equals(capital.getHerald())
                || entityId.equals(capital.getGrandMaester())
                || capital.isRoyalGuard(entityId);
    }

    private static UUID firstEligibleDuke(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            boolean female
    ) {
        List<UUID> candidates = sorted(level, capital, capital.getDukes());

        for (UUID candidate : candidates) {
            if (!isEligible(level, capital, residents, candidate)) {
                continue;
            }

            if (MCAIntegrationBridge.isFemale(level, candidate) == female) {
                return candidate;
            }
        }

        return null;
    }

    private static UUID firstEligibleKnightOrDame(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        List<UUID> candidates = sorted(level, capital, capital.getKnights());

        for (UUID candidate : candidates) {
            if (isEligible(level, capital, residents, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static UUID firstEligibleCommoner(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        List<UUID> candidates = sorted(level, capital, residents);

        for (UUID candidate : candidates) {
            if (!isEligible(level, capital, residents, candidate)) {
                continue;
            }

            if (capital.isDuke(candidate)
                    || capital.isLord(candidate)
                    || capital.isKnight(candidate)
                    || capital.isRoyalChild(candidate)
                    || capital.isDisinheritedRoyalChild(candidate)
                    || capital.isLegitimizedRoyalChild(candidate)) {
                continue;
            }

            return candidate;
        }

        return null;
    }

    private static List<UUID> sorted(ServerLevel level, CapitalRecord capital, Set<UUID> values) {
        List<UUID> result = new ArrayList<>();

        if (values != null) {
            for (UUID value : values) {
                if (value != null) {
                    result.add(value);
                }
            }
        }

        result.sort(Comparator
                .comparing((UUID id) -> !isRecognizedFriend(level, capital, id))
                .thenComparing(UUID::toString));

        return result;
    }

    private static boolean isRecognizedFriend(ServerLevel level, CapitalRecord capital, UUID entityId) {
        return level != null
                && capital != null
                && capital.getCapitalId() != null
                && entityId != null
                && CapitalJusticeDataAccess.getPublicStatus(level, capital.getCapitalId(), entityId)
                == CapitalPublicCrownStatus.RECOGNIZED_FRIEND;
    }

    private static boolean isTrustedOfficeEligible(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || capital.getCapitalId() == null || entityId == null) {
            return false;
        }

        UUID capitalId = capital.getCapitalId();

        return CapitalJusticeDataAccess.getPublicStatus(level, capitalId, entityId)
                != CapitalPublicCrownStatus.DISCOVERED_ENEMY
                && !CapitalJusticeDataAccess.hasArrestWarrant(level, capitalId, entityId)
                && !CapitalJusticeDataAccess.isDetainedPrisoner(level, capitalId, entityId)
                && !MCAExecutionBridge.isMarkedForExecution(level, entityId);
    }

    private static boolean isAmbassador(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return false;
        }

        return CapitalDiplomacyDataAccess.getAmbassadorsSnapshot(level)
                .containsValue(entityId);
    }
}
