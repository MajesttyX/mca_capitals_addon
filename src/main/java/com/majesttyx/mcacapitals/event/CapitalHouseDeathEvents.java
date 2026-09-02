package com.majesttyx.mcacapitals.event;

import com.majesttyx.mcacapitals.house.CapitalHouseRegistryService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class CapitalHouseDeathEvents {
    private CapitalHouseDeathEvents(){}
    public static void onLivingDeath(LivingEntity entity){
        if(entity==null||!(entity.level() instanceof ServerLevel level)||!MCAIntegrationBridge.isMCAVillagerEntity(entity)) return;
        CapitalHouseRegistryService.recordDeath(level,entity.getUUID());
    }
}
