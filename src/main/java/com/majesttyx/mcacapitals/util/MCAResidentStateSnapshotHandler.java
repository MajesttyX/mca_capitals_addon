package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MCAResidentStateSnapshotHandler {

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(event.getEntity())) {
            return;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(
                level,
                event.getEntity().getUUID()
        );
        if (villageId == null || !CapitalManager.hasCapitalForVillageId(level, villageId)) {
            return;
        }

        MCAEntityBridge.captureResidentState(level, event.getEntity());
    }
}
