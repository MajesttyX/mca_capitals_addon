package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class CapitalTradeExchangeProcessor {

    private static final long CHECK_INTERVAL_TICKS =
            1200L;

    @SubscribeEvent
    public void onLevelTick(
            LevelTickEvent.Post event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        if (level
                != level.getServer().overworld()) {
            return;
        }

        if (level.getGameTime()
                % CHECK_INTERVAL_TICKS != 0L) {
            return;
        }

        for (CapitalTradeAgreement agreement :
                CapitalAgreementDataAccess
                        .getTradeAgreementsSnapshot(
                                level
                        )
                        .values()) {
            CapitalTradeExchangeService
                    .processDueTrade(
                            level,
                            agreement
                    );
        }
    }
}