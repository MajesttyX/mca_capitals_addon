package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class CapitalDiplomaticAgreementProcessor {

    private static final long PROCESS_INTERVAL_TICKS = 20L;

    private CapitalDiplomaticAgreementProcessor() {
    }

    public static void onLevelTick(ServerLevel level) {
        if (level == null
                || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % PROCESS_INTERVAL_TICKS != 0L) {
            return;
        }

        CapitalDiplomaticTruceService.expireTruces(level);
        CapitalDiplomaticWorldService.tick(level);

        for (DiplomaticProposal proposal :
                CapitalAgreementDataAccess.getProposalsSnapshot(level).values()) {
            CapitalDiplomaticProposalService.processPendingProposal(level, proposal);
        }

        CapitalRoyalBetrothalService.tickEscorts(level);
    }
}
