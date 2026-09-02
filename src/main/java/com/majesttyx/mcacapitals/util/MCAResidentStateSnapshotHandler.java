package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class MCAResidentStateSnapshotHandler {
    private MCAResidentStateSnapshotHandler(){}
    public static void onEntityUnload(Entity entity, ServerLevel level){
        if(entity==null||level==null||!MCAIntegrationBridge.isMCAVillagerEntity(entity)) return;
        Integer villageId=MCAIntegrationBridge.getVillageIdForResident(level,entity.getUUID());
        if(villageId==null||!CapitalManager.hasCapitalForVillageId(level,villageId)) return;
        MCAEntityBridge.captureResidentState(level,entity);
    }
}
