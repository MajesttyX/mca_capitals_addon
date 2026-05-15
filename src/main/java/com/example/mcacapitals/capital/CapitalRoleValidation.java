package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

final class CapitalRoleValidation {

    private CapitalRoleValidation() {
    }

    static boolean isExistingRoleStillResolvable(ServerLevel level, UUID villagerId, Set<UUID> residents) {
        if (level == null || villagerId == null) {
            return false;
        }

        Entity loaded = MCAIntegrationBridge.findLoadedEntityByUuid(level, villagerId);
        if (loaded != null) {
            return MCAIntegrationBridge.isAliveMCAVillagerEntity(loaded);
        }

        if (MCAIntegrationBridge.isFamilyNodeDeceased(level, villagerId)) {
            return false;
        }

        if (MCAIntegrationBridge.hasPersistentFamilyNode(level, villagerId)) {
            return true;
        }

        return residents != null && residents.contains(villagerId);
    }

    static boolean isLoadedDeadOrRemoved(ServerLevel level, UUID villagerId) {
        if (level == null || villagerId == null) {
            return false;
        }

        Entity loaded = MCAIntegrationBridge.findLoadedEntityByUuid(level, villagerId);
        return loaded != null && (!loaded.isAlive() || loaded.isRemoved());
    }

    static boolean isCurrentlyLoaded(ServerLevel level, UUID villagerId) {
        return level != null && villagerId != null && MCAIntegrationBridge.findLoadedEntityByUuid(level, villagerId) != null;
    }
}