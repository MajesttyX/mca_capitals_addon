package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import fabric.net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class CapitalCampaignProcessor {

    private static final long CHECK_INTERVAL_TICKS =
            10L;

    private CapitalCampaignProcessor() {
    }

    public static void onLevelTick(ServerLevel level) {
        if (level == null) {
            return;
        }

        if (level.getGameTime()
                % CHECK_INTERVAL_TICKS != 0L) {
            return;
        }

        CapitalCampaignReturnService
                .restoreWaitingAttackers(level);

        List<CapitalCampaignRecord> campaigns =
                CapitalCampaignDataAccess
                        .getActiveCampaigns(level);

        CapitalCampaignCivilianResponseService
                .tickLevel(
                        level,
                        campaigns
                );

        for (CapitalCampaignRecord campaign :
                campaigns) {
            CapitalRecord attackingCapital =
                    CapitalManager.getCapital(
                            campaign
                                    .getAttackingCapitalId()
                    );

            CapitalRecord defendingCapital =
                    CapitalManager.getCapital(
                            campaign
                                    .getDefendingCapitalId()
                    );

            if (attackingCapital == null
                    || defendingCapital == null
                    || attackingCapital.getVillageId()
                    == null
                    || defendingCapital.getVillageId()
                    == null
                    || !CapitalManager.isCapitalInLevel(attackingCapital, level)
                    || !CapitalManager.isCapitalInLevel(defendingCapital, level)
                    || VillageManager.get(level)
                    .getOrEmpty(
                            attackingCapital
                                    .getVillageId()
                    )
                    .isEmpty()
                    || VillageManager.get(level)
                    .getOrEmpty(
                            defendingCapital
                                    .getVillageId()
                    )
                    .isEmpty()) {
                continue;
            }

            CapitalCampaignBattleService
                    .processCampaign(
                            level,
                            campaign
                    );
        }
    }
}