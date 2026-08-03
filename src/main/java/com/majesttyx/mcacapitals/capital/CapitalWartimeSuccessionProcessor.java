package com.majesttyx.mcacapitals.capital;

import net.minecraft.server.level.ServerLevel;

public final class CapitalWartimeSuccessionProcessor {

    private static final long PROCESS_INTERVAL_TICKS = 20L;

    private CapitalWartimeSuccessionProcessor() {
    }

    public static void onLevelTick(ServerLevel level) {
        if (level == null
                || level.getGameTime() % PROCESS_INTERVAL_TICKS != 0L) {
            return;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null || capital.getCapitalId() == null) {
                continue;
            }
            CapitalWartimeSuccessionService.handleIfNeeded(level, capital);
        }
    }
}
