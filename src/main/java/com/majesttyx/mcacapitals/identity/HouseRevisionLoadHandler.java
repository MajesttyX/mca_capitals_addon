package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MCACapitals.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HouseRevisionLoadHandler {

    private HouseRevisionLoadHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        HouseRevisionService.reconcileEntity(level, entity);
    }
}
