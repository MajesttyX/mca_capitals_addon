package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class HouseRevisionLoadHandler {

    private HouseRevisionLoadHandler() {
    }

    public static void onEntityJoinLevel(Entity entity, ServerLevel level) {
        if (level == null || entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        HouseRevisionService.reconcileEntity(level, entity);
    }
}
