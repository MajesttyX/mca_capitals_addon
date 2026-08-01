package com.majesttyx.mcacapitals.capital;

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
        UUID duke =
                firstEligibleDuke(
                        level,
                        capital,
                        residents,
                        false
                );

        if (duke != null) {
            return duke;
        }

        UUID knight =
                firstEligibleKnightOrDame(
                        level,
                        capital,
                        residents
                );

        if (knight != null) {
            return knight;
        }

        UUID duchess =
                firstEligibleDuke(
                        level,
                        capital,
                        residents,
                        true
                );

        if (duchess != null) {
            return duchess;
        }

        return firstEligibleCommoner(
                level,
                capital,
                residents
        );
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
                || entityId == null) {
            return false;
        }

        if (!residents.contains(entityId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(
                level,
                entityId
        )
                || !MCAIntegrationBridge.isAliveMCAVillager(
                level,
                entityId
        )) {
            return false;
        }

        if (CapitalAmbassadorService.isAmbassador(
                level,
                entityId
        )) {
            return false;
        }

        if (entityId.equals(capital.getSovereign())
                || entityId.equals(capital.getConsort())
                || entityId.equals(capital.getDowager())
                || entityId.equals(capital.getHeir())
                || entityId.equals(capital.getHand())
                || entityId.equals(capital.getCommander())
                || entityId.equals(capital.getHerald())
                || entityId.equals(capital.getGrandMaester())
                || capital.isRoyalGuard(entityId)) {
            return false;
        }

        return true;
    }

    private static UUID firstEligibleDuke(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            boolean female
    ) {
        List<UUID> candidates =
                sorted(capital.getDukes());

        for (UUID candidate : candidates) {
            if (!isEligible(
                    level,
                    capital,
                    residents,
                    candidate
            )) {
                continue;
            }

            if (MCAIntegrationBridge.isFemale(
                    level,
                    candidate
            ) == female) {
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
        List<UUID> candidates =
                sorted(capital.getKnights());

        for (UUID candidate : candidates) {
            if (isEligible(
                    level,
                    capital,
                    residents,
                    candidate
            )) {
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
        List<UUID> candidates =
                sorted(residents);

        for (UUID candidate : candidates) {
            if (!isEligible(
                    level,
                    capital,
                    residents,
                    candidate
            )) {
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

    private static List<UUID> sorted(
            Set<UUID> values
    ) {
        List<UUID> result =
                new ArrayList<>();

        if (values != null) {
            for (UUID value : values) {
                if (value != null) {
                    result.add(value);
                }
            }
        }

        result.sort(
                Comparator.comparing(UUID::toString)
        );

        return result;
    }
}