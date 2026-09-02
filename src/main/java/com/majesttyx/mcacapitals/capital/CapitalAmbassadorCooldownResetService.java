package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRouteKey;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;

public final class CapitalAmbassadorCooldownResetService {
    private static final long DAY_LENGTH_TICKS=24000L, DAILY_RESET_TIME=1000L;
    private CapitalAmbassadorCooldownResetService(){}
    public static void onLevelTick(ServerLevel level){
        if(level==null||level!=level.getServer().overworld()) return;
        if(Math.floorMod(level.getDayTime(),DAY_LENGTH_TICKS)!=DAILY_RESET_TIME) return;
        Map<CapitalRouteKey,Long> cooldowns=CapitalDiplomacyDataAccess.get(level).getGiftCooldownsSnapshot();
        for(CapitalRouteKey route:cooldowns.keySet()) if(route!=null) CapitalDiplomacyDataAccess.clearGiftCooldown(level,route.sourceCapitalId(),route.targetCapitalId());
    }
}
