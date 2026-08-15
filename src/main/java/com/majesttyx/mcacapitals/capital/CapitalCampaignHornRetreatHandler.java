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

    public static void onUseItem(ServerPlayer player, InteractionHand hand) {
        if (player == null || hand == null || !player.getItemInHand(hand).is(Items.GOAT_HORN)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        for (CapitalCampaignRecord campaign : CapitalCampaignDataAccess.getActiveCampaigns(level)) {
            if (campaign == null
                    || campaign.getPhase() != CapitalCampaignPhase.ACTIVE
                    || campaign.getInitiatingPlayerId() == null
                    || !campaign.getInitiatingPlayerId().equals(player.getUUID())) {
                continue;
            }

            CapitalRecord attackingCapital = CapitalManager.getCapital(campaign.getAttackingCapitalId());
            CapitalRecord defendingCapital = CapitalManager.getCapital(campaign.getDefendingCapitalId());
            if (attackingCapital == null
                    || defendingCapital == null
                    || !CapitalPlayerNotificationService.isPlayerWithinCapital(level, defendingCapital, player)) {
                continue;
            }

            if (!CapitalCampaignService.beginRetreat(
                    level,
                    campaign.getCampaignId(),
                    CapitalCampaignEndReason.COMMANDER_ORDERED_RETREAT
            )) {
                return;
            }

            CapitalCampaignTargetingService.clearCampaignTargets(level, campaign);
            CapitalCampaignReturnService.processRetreat(level, campaign, attackingCapital, defendingCapital);

            String attackingName = CapitalDiplomaticAgreementText.capitalName(level, attackingCapital);
            String defendingName = CapitalDiplomaticAgreementText.capitalName(level, defendingCapital);

            CapitalChronicleService.addEventWithoutHerald(
                    level, attackingCapital, CapitalChronicleEventId.CAMPAIGN_RETREAT_ORDERED,
                    player.getName().getString(), defendingName, attackingName
            );
            CapitalChronicleService.addEventWithoutHerald(
                    level, defendingCapital, CapitalChronicleEventId.CAMPAIGN_RETREAT_ORDERED,
                    player.getName().getString(), defendingName, attackingName
            );

            player.sendSystemMessage(Component.translatable("mcacapitals.system.campaign.retreat.self"));
            notifyOtherPlayersInDefendingCapital(level, player, defendingCapital, attackingName);
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level, attackingCapital,
                    Component.translatable("mcacapitals.system.campaign.retreat.attacker", defendingName)
            );
            return;
        }
    }

    private static void notifyOtherPlayersInDefendingCapital(
            ServerLevel level,
            ServerPlayer initiatingPlayer,
            CapitalRecord defendingCapital,
            String attackingName
    ) {
        for (ServerPlayer player : level.players()) {
            if (player == null
                    || player.getUUID().equals(initiatingPlayer.getUUID())
                    || !player.isAlive()
                    || player.isSpectator()
                    || !CapitalPlayerNotificationService.isPlayerWithinCapital(level, defendingCapital, player)) {
                continue;
            }
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.system.campaign.retreat.defender",
                    attackingName
            ));
        }
    }
}
