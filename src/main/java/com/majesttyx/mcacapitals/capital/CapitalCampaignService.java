package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalWarGoal;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class CapitalCampaignService {

    public static final long RETURN_FAILSAFE_TICKS = 20L * 30L;

    private CapitalCampaignService() {
    }

    public static int launchCampaign(
            ServerPlayer player,
            UUID ambassadorId,
            UUID defendingCapitalId
    ) {
        return launchCampaign(
                player,
                ambassadorId,
                defendingCapitalId,
                CapitalWarGoal.PUNITIVE
        );
    }

    public static int launchCampaign(
            ServerPlayer player,
            UUID ambassadorId,
            UUID defendingCapitalId,
            CapitalWarGoal warGoal
    ) {
        if (player == null
                || ambassadorId == null
                || defendingCapitalId == null) {
            return 0;
        }

        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(
                        player,
                        ambassadorId
                );

        if (!audience.valid()) {
            player.sendSystemMessage(Component.literal(
                    audience.failureMessage()
            ));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord attackingCapital = audience.sourceCapital();
        CapitalRecord defendingCapital =
                CapitalManager.getCapital(defendingCapitalId);

        String targetFailure =
                CapitalDiplomaticAgreementValidation.validateTarget(
                        attackingCapital,
                        defendingCapital
                );

        if (targetFailure != null) {
            player.sendSystemMessage(Component.literal(targetFailure));
            return 0;
        }

        CampaignCreationResult result = createCampaign(
                level,
                attackingCapital,
                defendingCapital,
                player.getUUID(),
                warGoal
        );

        if (!result.successful()) {
            player.sendSystemMessage(Component.literal(
                    result.failureMessage()
            ));
            return 0;
        }

        String targetName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        defendingCapital
                );

        player.sendSystemMessage(Component.literal(
                "The "
                        + result.campaign().getWarGoal().getDisplayName()
                        + " against "
                        + targetName
                        + " has been planned for "
                        + result.campaign().getWarCause().getDisplayName()
                        + ". Enter that capital yourself to begin a 20-second assembly. "
                        + "The campaign will attempt to gather "
                        + result.campaign().getTargetAttackerCount()
                        + " Guards and Archers, up to the maximum of "
                        + CapitalCampaignRecord.MAX_ATTACKERS
                        + ", before the force deploys."
        ));

        return 1;
    }

    public static CampaignCreationResult createCampaign(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        UUID initiatingPlayerId =
                CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                        level,
                        attackingCapital
                );

        return createCampaign(
                level,
                attackingCapital,
                defendingCapital,
                initiatingPlayerId,
                CapitalWarGoal.PUNITIVE
        );
    }

    public static CampaignCreationResult createCampaign(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            UUID initiatingPlayerId
    ) {
        return createCampaign(
                level,
                attackingCapital,
                defendingCapital,
                initiatingPlayerId,
                CapitalWarGoal.PUNITIVE
        );
    }

    public static CampaignCreationResult createCampaign(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            UUID initiatingPlayerId,
            CapitalWarGoal warGoal
    ) {
        String recoveryFailure =
                CapitalWarPlanningService.validateRecovery(
                        level,
                        attackingCapital
                );

        if (recoveryFailure != null) {
            return CampaignCreationResult.failure(recoveryFailure);
        }

        CapitalCampaignEligibilityService.Validation validation =
                CapitalCampaignEligibilityService.validateCampaign(
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
                        level.getGameTime(),
                        CapitalWarPlanningService.resolveCause(
                                level,
                                attackingCapital,
                                defendingCapital
                        ),
                        warGoal == null
                                ? CapitalWarGoal.PUNITIVE
                                : warGoal
                );

        if (!CapitalCampaignDataAccess.addCampaign(level, campaign)) {
            return CampaignCreationResult.failure(
                    "The campaign could not be recorded."
            );
        }

        String attackingName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        attackingCapital
                );
        String defendingName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        defendingCapital
                );
        String entry =
                attackingName
                        + " planned a "
                        + campaign.getWarGoal().getDisplayName()
                        + " against "
                        + defendingName
                        + " for "
                        + campaign.getWarCause().getDisplayName()
                        + ".";

        CapitalChronicleService.addEntry(level, attackingCapital, entry);
        CapitalChronicleService.addEntry(level, defendingCapital, entry);

        return CampaignCreationResult.success(campaign);
    }

    public static CapitalCampaignRecord getCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        return CapitalCampaignDataAccess.getCampaign(level, campaignId);
    }

    public static CapitalCampaignRecord getCampaignForCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalCampaignDataAccess.getCampaignForCapital(
                level,
                capitalId
        );
    }

    public static CapitalCampaignRecord getCampaignForAttacker(
            ServerLevel level,
            UUID villagerId
    ) {
        return CapitalCampaignDataAccess.getCampaignForAttacker(
                level,
                villagerId
        );
    }

    public static boolean isCampaignAttacker(
            ServerLevel level,
            UUID villagerId
    ) {
        return getCampaignForAttacker(level, villagerId) != null;
    }

    public static boolean isCapitalInCampaign(
            ServerLevel level,
            UUID capitalId
    ) {
        return getCampaignForCapital(level, capitalId) != null;
    }

    public static boolean areOpposingCampaignCombatants(
            ServerLevel level,
            UUID firstId,
            UUID secondId
    ) {
        if (level == null
                || firstId == null
                || secondId == null
                || firstId.equals(secondId)) {
            return false;
        }

        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess.getActiveCampaigns(level)) {
            if (campaign == null
                    || campaign.getPhase()
                    != CapitalCampaignPhase.ACTIVE) {
                continue;
            }

            boolean firstAttacker = campaign.containsAttacker(firstId);
            boolean secondAttacker = campaign.containsAttacker(secondId);

            if (firstAttacker == secondAttacker) {
                continue;
            }

            UUID defenderId = firstAttacker ? secondId : firstId;
            if (isCampaignDefender(campaign, defenderId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCampaignDefender(
            CapitalCampaignRecord campaign,
            UUID villagerId
    ) {
        if (campaign.containsDefender(villagerId)) {
            return true;
        }

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign.getDefendingCapitalId()
                );

        if (defendingCapital == null) {
            return false;
        }

        if (defendingCapital.isRoyalGuard(villagerId)) {
            return campaign.didDefendingSovereignRefusePeace();
        }

        return campaign.didDefendingSovereignRefusePeace()
                && villagerId.equals(defendingCapital.getSovereign());
    }

    public static boolean activateCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        CapitalCampaignRecord campaign =
                CapitalCampaignDataAccess.getCampaign(level, campaignId);

        if (campaign == null) {
            return false;
        }

        campaign.activate(level.getGameTime());
        CapitalCampaignDataAccess.get(level).setDirty();
        return true;
    }

    public static boolean beginRetreat(
            ServerLevel level,
            UUID campaignId,
            CapitalCampaignEndReason reason
    ) {
        CapitalCampaignRecord campaign =
                CapitalCampaignDataAccess.getCampaign(level, campaignId);

        if (campaign == null) {
            return false;
        }

        long now = level.getGameTime();
        campaign.beginRetreat(
                now,
                now + RETURN_FAILSAFE_TICKS,
                reason
        );
        CapitalCampaignDataAccess.get(level).setDirty();
        return true;
    }

    public static boolean completeCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        CapitalCampaignRecord campaign =
                CapitalCampaignDataAccess.getCampaign(level, campaignId);

        if (campaign != null) {
            CapitalWarSettlementService.resolve(level, campaign);

            CapitalRecord attackingCapital =
                    CapitalManager.getCapital(
                            campaign.getAttackingCapitalId()
                    );

            CapitalRecord defendingCapital =
                    CapitalManager.getCapital(
                            campaign.getDefendingCapitalId()
                    );

            CapitalCampaignReturnService.returnDefendersHome(
                    level,
                    campaign,
                    defendingCapital
            );

            CapitalCampaignAssemblyService.releaseSourceTicket(
                    level,
                    campaign,
                    attackingCapital
            );
        }

        return CapitalCampaignDataAccess.removeCampaign(
                level,
                campaignId
        );
    }

    public static List<UUID> getEligibleAttackers(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return CapitalCampaignEligibilityService.findEligibleAttackers(
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

        static CampaignCreationResult failure(String message) {
            return new CampaignCreationResult(
                    false,
                    null,
                    message
            );
        }
    }
}
