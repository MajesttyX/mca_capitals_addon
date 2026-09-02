package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRouteKey;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

public final class CapitalAmbassadorCooldownResetService {

    private static final long DAY_LENGTH_TICKS = 24000L;
    private static final long DAILY_RESET_TIME = 1000L;

    private CapitalAmbassadorCooldownResetService() {
    }

    public static void onLevelTick(ServerLevel level) {
        if (level == null || level != level.getServer().overworld()) {
            return;
        }

        long timeOfDay = Math.floorMod(level.getDayTime(), DAY_LENGTH_TICKS);
        if (timeOfDay != DAILY_RESET_TIME) {
            return;
        }

        clearAmbassadorActionCooldowns(level);
    }

    private static void clearAmbassadorActionCooldowns(ServerLevel level) {
        Map<CapitalRouteKey, Long> giftCooldowns =
                CapitalDiplomacyDataAccess.get(level).getGiftCooldownsSnapshot();

        for (CapitalRouteKey route : giftCooldowns.keySet()) {
            if (route == null) {
                continue;
            }

            CapitalDiplomacyDataAccess.clearGiftCooldown(
                    level,
                    route.sourceCapitalId(),
                    route.targetCapitalId()
            );
        }
    }
}
