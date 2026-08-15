package com.majesttyx.mcacapitals.capital;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class CapitalDiplomaticDelayService {

    public static final long MINIMUM_DELAY_TICKS = 1200L;
    public static final long MAXIMUM_DELAY_TICKS = 6000L;

    private CapitalDiplomaticDelayService() {
    }

    public static long schedule(ServerLevel level) {
        if (level == null) {
            return MINIMUM_DELAY_TICKS;
        }

        int range = (int) (MAXIMUM_DELAY_TICKS - MINIMUM_DELAY_TICKS + 1L);
        return level.getGameTime()
                + MINIMUM_DELAY_TICKS
                + level.random.nextInt(range);
    }

    public static boolean isReady(ServerLevel level, long availableAt) {
        return level != null && level.getGameTime() >= Math.max(0L, availableAt);
    }

    public static Component dispatchMessage() {
        return Component.translatable(
                "mcacapitals.diplomacy.correspondence.dispatched"
        );
    }
}