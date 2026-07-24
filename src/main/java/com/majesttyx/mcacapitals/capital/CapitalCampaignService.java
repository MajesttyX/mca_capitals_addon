package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class CapitalCampaignService {

    public static final long RETURN_FAILSAFE_TICKS =
            20L * 30L;

    private CapitalCampaignService() {
    }

    public static int launchCampaign(
            ServerPlayer player,
            UUID ambassadorId,
            UUID defendingCapitalId
    ) {
        if (player == null
                || ambassadorId == null
                || defendingCapitalId == null) {
            return 0;
        }

        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            player.sendSystemMessage(
                    Component.literal(
                            audience.failureMessage()
                    )
            );

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord attackingCapital =
                audience.sourceCapital();

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        defendingCapitalId
                );

        String targetFailure =
                CapitalDiplomaticAgreementValidation
                        .validateTarget(
                                attackingCapital,
                                defendingCapital
                        );

        if (targetFailure != null) {
            player.sendSystemMessage(
                    Component.literal(targetFailure)
            );

            return 0;
        }

        CampaignCreationResult result =
                createCampaign(
                        level,
                        attackingCapital,
                        defendingCapital,
                        player.getUUID()
                );

        if (!result.successful()) {
            player.sendSystemMessage(
                    Component.literal(
                            result.failureMessage()
                    )
            );

            return 0;
        }

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                defendingCapital
                        );

        player.sendSystemMessage(
                Component.literal(
                        "The attack on "
                                + targetName
                                + " has been planned. Travel into that capital yourself; when you cross its border, war will begin and "
                                + result.campaign()
                                .getAttackerIds()
                                .size()
                                + " Guards and Archers will arrive to fight beside you."
                )
        );

        return 1;
    }

    public static CampaignCreationResult createCampaign(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        UUID playerSovereignId =
                attackingCapital == null
                        ? null
                        : attackingCapital
                        .getPlayerSovereignId();

        return createCampaign(
                level,
                attackingCapital,
                defendingCapital,
                playerSovereignId
        );
    }

    public static CampaignCreationResult createCampaign(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            UUID initiatingPlayerId
    ) {
        CapitalCampaignEligibilityService.Validation validation =
                CapitalCampaignEligibilityService
                        .validateCampaign(
                                level,
                                attackingCapital,
                                defendingCapital,
                                initiatingPlayerId
                        );

        if (!validation.valid()) {
            return CampaignCreationResult.failure(
                    validation.failureMessage()
            );
        }

        CapitalCampaignRecord campaign =
                new CapitalCampaignRecord(
                        UUID.randomUUID(),
                        attackingCapital.getCapitalId(),
                        defendingCapital.getCapitalId(),
                        initiatingPlayerId,
                        validation.attackers(),
                        level.getGameTime()
                );

        if (!CapitalCampaignDataAccess.addCampaign(
                level,
                campaign
        )) {
            return CampaignCreationResult.failure(
                    "The attack could not be reserved because one of its capitals or guards is already committed elsewhere."
            );
        }

        String attackingName =
                MCAIntegrationBridge.getVillageName(
                        level,
                        attackingCapital.getVillageId()
                );

        String defendingName =
                MCAIntegrationBridge.getVillageName(
                        level,
                        defendingCapital.getVillageId()
                );

        if (attackingName == null
                || attackingName.isBlank()) {
            attackingName =
                    "The attacking capital";
        }

        if (defendingName == null
                || defendingName.isBlank()) {
            defendingName =
                    "the defending capital";
        }

        String entry =
                attackingName
                        + " prepared "
                        + campaign.getAttackerIds().size()
                        + " Guards and Archers for a player-led attack on "
                        + defendingName
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                attackingCapital,
                entry
        );

        return CampaignCreationResult.success(
                campaign
        );
    }

    public static CapitalCampaignRecord
    getCampaignForCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        capitalId
                );
    }

    public static CapitalCampaignRecord
    getCampaignForAttacker(
            ServerLevel level,
            UUID villagerId
    ) {
        return CapitalCampaignDataAccess
                .getCampaignForAttacker(
                        level,
                        villagerId
                );
    }

    public static boolean isCampaignAttacker(
            ServerLevel level,
            UUID villagerId
    ) {
        return getCampaignForAttacker(
                level,
                villagerId
        ) != null;
    }

    public static boolean isCapitalInCampaign(
            ServerLevel level,
            UUID capitalId
    ) {
        return getCampaignForCapital(
                level,
                capitalId
        ) != null;
    }

    public static boolean activateCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        CapitalCampaignRecord campaign =
                CapitalCampaignDataAccess
                        .getCampaign(
                                level,
                                campaignId
                        );

        if (campaign == null) {
            return false;
        }

        campaign.activate(
                level.getGameTime()
        );

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

        return true;
    }

    public static boolean beginRetreat(
            ServerLevel level,
            UUID campaignId,
            CapitalCampaignEndReason reason
    ) {
        CapitalCampaignRecord campaign =
                CapitalCampaignDataAccess
                        .getCampaign(
                                level,
                                campaignId
                        );

        if (campaign == null) {
            return false;
        }

        long now =
                level.getGameTime();

        campaign.beginRetreat(
                now,
                now + RETURN_FAILSAFE_TICKS,
                reason
        );

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

        return true;
    }

    public static boolean completeCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        return CapitalCampaignDataAccess
                .removeCampaign(
                        level,
                        campaignId
                );
    }

    public static List<UUID> getEligibleAttackers(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return CapitalCampaignEligibilityService
                .findEligibleAttackers(
                        level,
                        capital
                );
    }

    public record CampaignCreationResult(
            boolean successful,
            CapitalCampaignRecord campaign,
            String failureMessage
    ) {

        static CampaignCreationResult success(
                CapitalCampaignRecord campaign
        ) {
            return new CampaignCreationResult(
                    true,
                    campaign,
                    null
            );
        }

        static CampaignCreationResult failure(
                String message
        ) {
            return new CampaignCreationResult(
                    false,
                    null,
                    message
            );
        }
    }
}