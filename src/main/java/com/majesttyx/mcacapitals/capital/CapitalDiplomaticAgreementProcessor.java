package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class CapitalDiplomaticAgreementProcessor {

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level != level.getServer().overworld()) {
            return;
        }

        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        CapitalDiplomaticAgreementService.expireTruces(level);
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