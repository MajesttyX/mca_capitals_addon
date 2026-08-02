package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalRefugeeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRefugeeRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

final class CapitalRoleValidation {

    private CapitalRoleValidation() {
    }

    static boolean isExistingRoleStillResolvable(
            ServerLevel level,
            UUID villagerId,
            Set<UUID> residents
    ) {
        if (level == null
                || villagerId == null) {
            return false;
        }

        CapitalRefugeeRecord refugeeRecord =
                CapitalRefugeeDataAccess.getRecord(
                        level,
                        villagerId
                );

        if (refugeeRecord != null
                && refugeeRecord.isAwaitingAsylum()) {
            return false;
        }

        Entity loaded =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                villagerId
                        );

        if (loaded != null) {
            return MCAIntegrationBridge
                    .isAliveMCAVillagerEntity(
                            loaded
                    )
                    && (residents == null
                    || residents.contains(villagerId));
        }

        if (MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        villagerId
                )) {
            return false;
        }

        if (MCAIntegrationBridge
                .hasPersistentFamilyNode(
                        level,
                        villagerId
                )) {
            return true;
        }

        return residents != null
                && residents.contains(villagerId);
    }

    static boolean isLoadedDeadOrRemoved(
            ServerLevel level,
            UUID villagerId
    ) {
        if (level == null
                || villagerId == null) {
            return false;
        }

        Entity loaded =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                villagerId
                        );

        return loaded != null
                && (!loaded.isAlive()
                || loaded.isRemoved());
    }

    static boolean isCurrentlyLoaded(
            ServerLevel level,
            UUID villagerId
    ) {
        return level != null
                && villagerId != null
                && MCAIntegrationBridge
                .findLoadedEntityByUuid(
                        level,
                        villagerId
                ) != null;
    }
}