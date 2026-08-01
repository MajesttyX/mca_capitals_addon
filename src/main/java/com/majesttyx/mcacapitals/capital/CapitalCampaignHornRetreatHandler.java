package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import forge.net.mca.server.world.data.Village;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID)
public final class CapitalCampaignHornRetreatHandler {

    private CapitalCampaignHornRetreatHandler() {
    }

    @SubscribeEvent
    public static void onHornStarted(
            LivingEntityUseItemEvent.Start event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)
                || !event.getItem().is(
                Items.GOAT_HORN
        )) {
            return;
        }

        ServerLevel level =
                player.serverLevel();

        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess
                        .getActiveCampaigns(level)) {
            if (!isEligibleRetreatSignal(
                    level,
                    player,
                    campaign
            )) {
                continue;
            }

            orderRetreat(
                    level,
                    player,
                    campaign
            );

            return;
        }
    }

    private static boolean isEligibleRetreatSignal(
            ServerLevel level,
            ServerPlayer player,
            CapitalCampaignRecord campaign
    ) {
        if (campaign == null
                || campaign.getPhase()
                != CapitalCampaignPhase.ACTIVE
                || campaign.getInitiatingPlayerId()
                == null
                || !campaign.getInitiatingPlayerId()
                .equals(player.getUUID())) {
            return false;
        }

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign
                                .getDefendingCapitalId()
                );

        Village defendingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        return defendingVillage != null
                && defendingVillage
                .isWithinBorder(player);
    }

    private static void orderRetreat(
            ServerLevel level,
            ServerPlayer player,
            CapitalCampaignRecord campaign
    ) {
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
                || !CapitalCampaignService
                .beginRetreat(
                        level,
                        campaign.getCampaignId(),
                        CapitalCampaignEndReason
                                .COMMANDER_ORDERED_RETREAT
                )) {
            return;
        }

        CapitalCampaignTargetingService
                .clearCampaignTargets(
                        level,
                        campaign
                );

        CapitalCampaignReturnService
                .processRetreat(
                        level,
                        campaign,
                        attackingCapital,
                        defendingCapital
                );

        String attackingName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                attackingCapital
                        );

        String defendingName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                defendingCapital
                        );

        String entry =
                player.getName().getString()
                        + " sounded the retreat within "
                        + defendingName
                        + ". The occupying force from "
                        + attackingName
                        + " withdrew from the capital.";

        CapitalChronicleService
                .addEntryWithoutHerald(
                        attackingCapital,
                        entry
                );

        CapitalChronicleService
                .addEntryWithoutHerald(
                        defendingCapital,
                        entry
                );

        player.sendSystemMessage(
                Component.literal(
                        "You sounded the retreat. Your surviving soldiers are returning home."
                )
        );

        notifyOtherPlayersInDefendingCapital(
                level,
                player,
                defendingCapital,
                attackingName
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        attackingCapital,
                        Component.literal(
                                "The attacking commander sounded the retreat from "
                                        + defendingName
                                        + ". The surviving force is returning home."
                        )
                );
    }

    private static void
    notifyOtherPlayersInDefendingCapital(
            ServerLevel level,
            ServerPlayer initiatingPlayer,
            CapitalRecord defendingCapital,
            String attackingName
    ) {
        Village village =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (village == null) {
            return;
        }

        for (ServerPlayer player :
                level.players()) {
            if (player == null
                    || player.getUUID().equals(
                    initiatingPlayer.getUUID()
            )
                    || !player.isAlive()
                    || player.isSpectator()
                    || !village.isWithinBorder(
                    player
            )) {
                continue;
            }

            player.sendSystemMessage(
                    Component.literal(
                            "The occupying force from "
                                    + attackingName
                                    + " has sounded a retreat."
                    )
            );
        }
    }
}