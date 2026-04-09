package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
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

    static boolean isEligibleForNewCommander(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getVillageId() == null) {
            return false;
        }
        return MCAIntegrationBridge.getVillagePopulation(level, capital.getVillageId()) >= CapitalCommanderService.REQUIRED_POPULATION;
    }

    static UUID findBestCommanderCandidate(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        BlockPos center = capital.getVillageId() != null
                ? MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId())
                : BlockPos.ZERO;

        List<UUID> candidates = new ArrayList<>();

        for (UUID residentId : residents) {
            if (!MCAIntegrationBridge.isMCAGuard(level, residentId)) {
                continue;
            }
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, residentId);
            if (!MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)) {
                continue;
            }
            candidates.add(residentId);
        }

        candidates.sort(Comparator
                .comparingDouble((UUID id) -> {
                    Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
                    return entity == null
                            ? Double.MAX_VALUE
                            : entity.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
                })
                .thenComparing(UUID::toString));

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    static boolean isValidCommander(ServerLevel level, UUID commanderId, Set<UUID> residents) {
        if (commanderId == null) {
            return false;
        }

        if (residents != null && !residents.contains(commanderId)) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, commanderId);
        return MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)
                && MCAIntegrationBridge.isMCAGuard(level, commanderId);
    }
}