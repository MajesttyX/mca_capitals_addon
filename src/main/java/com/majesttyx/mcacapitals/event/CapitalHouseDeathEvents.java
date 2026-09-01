package com.majesttyx.mcacapitals.event;

import com.majesttyx.mcacapitals.house.CapitalHouseRegistryService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = "mcacapitals")
public final class CapitalHouseDeathEvents {

    private CapitalHouseDeathEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {
        Entity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        CapitalHouseRegistryService.recordDeath(
                level,
                entity.getUUID()
        );
    }
}
