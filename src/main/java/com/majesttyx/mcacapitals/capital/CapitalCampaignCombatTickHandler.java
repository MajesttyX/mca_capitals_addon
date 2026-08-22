package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class CapitalCampaignCombatTickHandler {

    private CapitalCampaignCombatTickHandler() {
    }

    public static void onLevelTick(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        Set<UUID> combatantIds =
                new LinkedHashSet<>();

        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess
                        .getActiveCampaigns(level)) {
            if (campaign == null
                    || campaign.getPhase()
                    != CapitalCampaignPhase.ACTIVE) {
                continue;
            }

            combatantIds.addAll(
                    campaign.getAttackerIds()
            );
            combatantIds.addAll(
                    campaign.getDefenderIds()
            );

            if (campaign
                    .didDefendingSovereignRefusePeace()) {
                CapitalRecord defendingCapital =
                        CapitalManager.getCapital(
                                campaign
                                        .getDefendingCapitalId()
                        );

                if (defendingCapital != null) {
                    combatantIds.addAll(
                            defendingCapital
                                    .getRoyalGuards()
                    );

                    if (defendingCapital
                            .getSovereign() != null) {
                        combatantIds.add(
                                defendingCapital
                                        .getSovereign()
                        );
                    }
                }
            }
        }

        for (UUID combatantId : combatantIds) {
            Entity entity = MCAIntegrationBridge
                    .findLoadedEntityByUuid(
                            level,
                            combatantId
                    );

            if (entity instanceof VillagerEntityMCA villager) {
                CapitalCampaignCombatService
                        .enforceCombatState(villager);
            }
        }
    }
}
