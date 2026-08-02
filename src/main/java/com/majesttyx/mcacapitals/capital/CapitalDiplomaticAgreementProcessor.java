package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CapitalDiplomaticAgreementProcessor {
    @SubscribeEvent
    public void onLevelTick(
            TickEvent.LevelTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        if (level != level.getServer().overworld()) {
            return;
        }

        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        CapitalDiplomaticAgreementService.expireTruces(level);
        CapitalDiplomaticTradeAgreementService.tick(level);
        CapitalDiplomaticWorldService.tick(level);

        for (DiplomaticProposal proposal :
                CapitalAgreementDataAccess
                        .getProposalsSnapshot(level)
                        .values()) {
            CapitalDiplomaticAgreementService
                    .processPendingProposal(
                            level,
                            proposal
                    );
        }
    }
}
