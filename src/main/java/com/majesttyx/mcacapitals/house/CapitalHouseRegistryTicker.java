package com.majesttyx.mcacapitals.house;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalState;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;

public final class CapitalHouseRegistryTicker {

    private static final int SYNC_INTERVAL_TICKS = 20;

    private CapitalHouseRegistryTicker() {
    }

    public static void onLevelTick(ServerLevel level) {
        if (level == null || level.getGameTime() % SYNC_INTERVAL_TICKS != 0L) {
            return;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalsSnapshot().values()) {
            if (capital == null
                    || capital.getCapitalId() == null
                    || capital.getState() != CapitalState.ACTIVE
                    || !CapitalManager.isCapitalInLevel(capital, level)) {
                continue;
            }

            Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
            CapitalHouseRegistryService.synchronize(level, capital, residents);
            CapitalHouseSuccessionService.reconcile(level, capital);
        }
    }
}
