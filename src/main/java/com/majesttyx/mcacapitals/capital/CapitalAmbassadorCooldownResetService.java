package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRouteKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalAmbassadorCooldownResetService {

    private static final long DAY_LENGTH_TICKS = 24000L;
    private static final long DAILY_RESET_TIME = 1000L;

    private CapitalAmbassadorCooldownResetService() {
    }

    @SubscribeEvent
    public static void onLevelTick(
            TickEvent.LevelTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        if (level != level.getServer().overworld()) {
            return;
        }

        long timeOfDay =
                Math.floorMod(
                        level.getDayTime(),
                        DAY_LENGTH_TICKS
                );

        if (timeOfDay != DAILY_RESET_TIME) {
            return;
        }

        clearAmbassadorActionCooldowns(level);
    }

    private static void clearAmbassadorActionCooldowns(
            ServerLevel level
    ) {
        Map<CapitalRouteKey, Long> giftCooldowns =
                CapitalDiplomacyDataAccess
                        .get(level)
                        .getGiftCooldownsSnapshot();

        for (CapitalRouteKey route :
                giftCooldowns.keySet()) {
            if (route == null) {
                continue;
            }

            CapitalDiplomacyDataAccess
                    .clearGiftCooldown(
                            level,
                            route.sourceCapitalId(),
                            route.targetCapitalId()
                    );
        }
    }
}
