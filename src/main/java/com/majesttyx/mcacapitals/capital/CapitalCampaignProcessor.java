package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

@EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalCampaignProcessor {

    private static final long CHECK_INTERVAL_TICKS =
            10L;

    private CapitalCampaignProcessor() {
    }

    @SubscribeEvent
    public static void onLevelTick(
            LevelTickEvent.Post event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)) {
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