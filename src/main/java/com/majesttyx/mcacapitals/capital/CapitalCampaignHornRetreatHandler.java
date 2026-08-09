package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class CapitalCampaignHornRetreatHandler {

    private CapitalCampaignHornRetreatHandler() {
    }

    public static void onUseItem(
            ServerPlayer player,
            InteractionHand hand
    ) {
        if (player == null
                || hand == null
                || !player.getItemInHand(hand).is(Items.GOAT_HORN)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess.getActiveCampaigns(level)) {
            if (campaign == null
                    || campaign.getPhase() != CapitalCampaignPhase.ACTIVE
                    || campaign.getInitiatingPlayerId() == null
                    || !campaign.getInitiatingPlayerId().equals(
                    player.getUUID()
            )) {
                continue;
            }

            CapitalRecord attackingCapital = CapitalManager.getCapital(
                    campaign.getAttackingCapitalId()
            );
            CapitalRecord defendingCapital = CapitalManager.getCapital(
                    campaign.getDefendingCapitalId()
            );
            if (attackingCapital == null
                    || defendingCapital == null
                    || !CapitalPlayerNotificationService
                    .isPlayerWithinCapital(
                            level,
                            defendingCapital,
                            player
                    )) {
                continue;
            }

            if (!CapitalCampaignService.beginRetreat(
                    level,
                    campaign.getCampaignId(),
                    CapitalCampaignEndReason.COMMANDER_ORDERED_RETREAT
            )) {
                return;
            }

            CapitalCampaignTargetingService.clearCampaignTargets(
                    level,
                    campaign
            );
            CapitalCampaignReturnService.processRetreat(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital
            );

            String entry =
                    player.getName().getString()
                            + " sounded the horn within "
                            + CapitalDiplomaticAgreementText.capitalName(
                            level,
                            defendingCapital
                    )
                            + ", ordering the campaign force to retreat.";
            CapitalChronicleService.addEntry(
                    level,
                    attackingCapital,
                    entry
            );
            CapitalChronicleService.addEntry(
                    level,
                    defendingCapital,
                    entry
            );
            player.sendSystemMessage(Component.literal(
                    "The horn recalled the campaign force. Surviving attackers are returning home."
            ));
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    defendingCapital,
                    Component.literal(
                            "The attacking force has sounded a retreat."
                    )
            );
            return;
        }
    }
}
