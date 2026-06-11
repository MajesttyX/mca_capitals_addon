package com.majesttyx.mcacapitals.event;

import com.majesttyx.mcacapitals.capital.CapitalDeathTransitionService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class CapitalDeathEvents {

    private CapitalDeathEvents() {
    }

    public static void onLivingDeath(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        CapitalDeathTransitionService.handleVillagerDeath(serverLevel, entity);
    }
}