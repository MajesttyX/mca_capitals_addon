package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalCommanderSelection {

    private CapitalCommanderSelection() {
    }

    static boolean isEligibleForNewCommander(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getVillageId() == null) {
            return false;
        }

        return MCAIntegrationBridge.getVillagePopulation(
                level,
                capital.getVillageId()
        ) >= CapitalCommanderService.REQUIRED_POPULATION;
    }

    static UUID findBestCommanderCandidate(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || residents == null) {
            return null;
        }

        BlockPos center = capital.getVillageId() != null
                ? MCAIntegrationBridge.getVillageCenter(
                level,
                capital.getVillageId()
        )
                : BlockPos.ZERO;

        List<UUID> candidates = new ArrayList<>();

        for (UUID residentId : residents) {
            if (!isEligibleCandidate(
                    level,
                    capital,
                    residentId,
                    residents
            )) {
                continue;
            }

            candidates.add(residentId);
        }

        candidates.sort(
                Comparator
                        .comparing(
                                (UUID id) ->
                                        !CapitalCrownJusticeService
                                                .isRecognizedFriend(
                                                        level,
                                                        capital,
                                                        id
                                                )
                        )
                        .thenComparingDouble(id -> {
                            Entity entity =
                                    MCAIntegrationBridge.getEntityByUuid(
                                            level,
                                            id
                                    );

                            return entity == null
                                    ? Double.MAX_VALUE
                                    : entity.distanceToSqr(
                                    center.getX() + 0.5D,
                                    center.getY() + 0.5D,
                                    center.getZ() + 0.5D
                            );
                        })
                        .thenComparing(UUID::toString)
        );

        return candidates.isEmpty()
                ? null
                : candidates.get(0);
    }

    static boolean isEligibleCandidate(
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

        if (!CapitalCrownJusticeService.isTrustedOfficeEligible(
                level,
                capital,
                candidateId
        )) {
            return false;
        }

        if (hasConflictingOffice(
                level,
                capital,
                candidateId
        )) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAGuard(
                level,
                candidateId
        )) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(
                level,
                candidateId
        );

        return MCAIntegrationBridge.isAliveMCAVillagerEntity(
                entity
        );
    }

    static boolean isValidCommander(
            ServerLevel level,
            CapitalRecord capital,
            UUID commanderId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || commanderId == null) {
            return false;
        }

        if (residents != null
                && !residents.contains(commanderId)) {
            return false;
        }

        if (!CapitalCrownJusticeService.isTrustedOfficeEligible(
                level,
                capital,
                commanderId
        )) {
            return false;
        }

        if (hasConflictingOffice(
                level,
                capital,
                commanderId
        )) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(
                level,
                commanderId
        );

        return MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)
                && MCAIntegrationBridge.isMCAGuard(
                level,
                commanderId
        );
    }

    private static boolean hasConflictingOffice(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId
    ) {
        if (candidateId.equals(capital.getSovereign())
                || candidateId.equals(capital.getPlayerSovereignId())
                || candidateId.equals(capital.getHand())
                || candidateId.equals(capital.getGrandMaester())
                || candidateId.equals(capital.getHerald())
                || candidateId.equals(capital.getMasterOfLaws())
                || capital.isRoyalGuard(candidateId)) {
            return true;
        }

        return CapitalAmbassadorService.isAmbassador(
                level,
                capital,
                candidateId
        );
    }
}