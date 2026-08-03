package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class CapitalTradeExchangeProcessor {

    private static final long CHECK_INTERVAL_TICKS = 1200L;

    private CapitalTradeExchangeProcessor() {
    }

    public static void onLevelTick(ServerLevel level) {
        if (level == null
                || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % CHECK_INTERVAL_TICKS != 0L) {
            return;
        }

        for (CapitalTradeAgreement agreement :
                CapitalAgreementDataAccess
                        .getTradeAgreementsSnapshot(level)
                        .values()) {
            if (!CapitalTradeAgreementTermService.processTerm(
                    level,
                    agreement
            )) {
                continue;
            }
            CapitalTradeExchangeService.processDueTrade(
                    level,
                    agreement
            );
        }
    }
}
