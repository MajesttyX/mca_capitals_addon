package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalCampaignCourtDecisionService {

    private static final int
            PEACE_REQUEST_CHANCE_PERCENT = 60;

    private static final long
            CROWN_RALLY_TICKS =
            20L * 5L;

    private static final Set<CapitalTitleResolver.ResolvedTitleId>
            HIGH_RANKING_TITLES =
            Set.of(
                    CapitalTitleResolver.ResolvedTitleId.HIGH_SOVEREIGN,
                    CapitalTitleResolver.ResolvedTitleId.SOVEREIGN_CONSORT,
                    CapitalTitleResolver.ResolvedTitleId.CROWN_HEIR,
                    CapitalTitleResolver.ResolvedTitleId.ROYAL_CHILD,
                    CapitalTitleResolver.ResolvedTitleId.SOVEREIGN_DOWAGER,
                    CapitalTitleResolver.ResolvedTitleId.DOWAGER_PRINCE,
                    CapitalTitleResolver.ResolvedTitleId.DUKE,
                    CapitalTitleResolver.ResolvedTitleId.DOWAGER_DUKE,
                    CapitalTitleResolver.ResolvedTitleId.LORD
            );

    private CapitalCampaignCourtDecisionService() {
    }

    public static boolean resolveIfChildSovereign(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        if (!isBabyOrToddlerSovereign(
                level,
                defendingCapital
        )) {
            return false;
        }

        boolean suingForPeace =
                level.random.nextInt(100)
                        < PEACE_REQUEST_CHANCE_PERCENT;

        DecisionMessage decision =
                buildDecisionMessage(
                        level,
                        defendingCapital,
                        suingForPeace
                );

        recordDecision(
                level,
                attackingCapital,
                defendingCapital,
                decision
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        decision.chatMessage()
                );

        if (suingForPeace) {
            if (campaign.getWarGoal()
                    == com.majesttyx.mcacapitals.data.CapitalWarGoal.DEPOSITION
                    && defendingCapital.getSovereign() != null) {
                CapitalWartimeSuccessionService.beginDepositionInterregnum(
                        level,
                        defendingCapital,
                        "after the defending court sued for peace.",
                        campaign.getInitiatingPlayerId()
                );
            }

            establishPeace(
                    level,
                    attackingCapital,
                    defendingCapital
            );

            if (CapitalCampaignService.beginRetreat(
                    level,
                    campaign.getCampaignId(),
                    CapitalCampaignEndReason
                            .DEFENDERS_SURRENDERED
            )) {
                CapitalCampaignTargetingService
                        .clearCampaignTargets(
                                level,
                                campaign
                        );

            }

            return true;
        }

        campaign.markDefendingSovereignRefusedPeace();

        campaign.beginCrownRally(
                level.getGameTime(),
                level.getGameTime()
                        + CROWN_RALLY_TICKS
        );

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();


        return true;
    }

    private static DecisionMessage buildDecisionMessage(
            ServerLevel level,
            CapitalRecord defendingCapital,
            boolean suingForPeace
    ) {
        String defendingName = CapitalDiplomaticAgreementText.capitalName(level, defendingCapital);
        CapitalChronicleEntry.Argument sovereignName =
                resolveTitledNameChronicleArgument(
                        level,
                        defendingCapital,
                        defendingCapital.getSovereign()
                );
        Component sovereignDisplay = resolveTitledNameComponent(level, defendingCapital, defendingCapital.getSovereign());
        DecisionSpeaker speaker = findProxySpeaker(level, defendingCapital);
        CapitalChronicleEntry.Argument speakerName = speaker != null
                ? CapitalChronicleService.literal(speaker.displayName())
                : CapitalChronicleService.translatableSnapshot(
                        "mcacapitals.system.campaign.court_of",
                        defendingName
                );
        Component speakerDisplay = speaker != null
                ? speaker.displayComponent()
                : Component.translatable(
                        "mcacapitals.system.campaign.court_of",
                        defendingName
                );

        String key = suingForPeace
                ? "mcacapitals.system.campaign.child_sues_for_peace"
                : "mcacapitals.system.campaign.child_refuses_peace";

        CapitalChronicleEventId eventId = suingForPeace
                ? CapitalChronicleEventId.CHILD_SOVEREIGN_SUED_FOR_PEACE
                : CapitalChronicleEventId.CHILD_SOVEREIGN_REFUSED_PEACE;

        return new DecisionMessage(
                Component.translatable(key, speakerDisplay, sovereignDisplay, defendingName),
                eventId,
                speakerName,
                sovereignName
        );
    }

    private static void establishPeace(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        CapitalAgreementDataAccess
                .removeProposalsBetween(
                        level,
                        attackingCapital.getCapitalId(),
                        defendingCapital.getCapitalId()
                );

        CapitalDiplomacyDataAccess
                .setDiplomaticState(
                        level,
                        attackingCapital.getCapitalId(),
                        defendingCapital.getCapitalId(),
                        CapitalDiplomaticState.PEACE,
                        0L
                );
    }

    private static void recordDecision(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            DecisionMessage decision
    ) {
        CapitalChronicleService.addEventWithoutHerald(
                level,
                attackingCapital,
                decision.eventId(),
                decision.speakerName(),
                decision.sovereignName()
        );

        CapitalChronicleService.addEventWithoutHerald(
                level,
                defendingCapital,
                decision.eventId(),
                decision.speakerName(),
                decision.sovereignName()
        );
    }

    private static boolean isBabyOrToddlerSovereign(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (capital == null
                || capital.getSovereign()
                == null) {
            return false;
        }

        String ageState =
                MCAIntegrationBridge.getAgeState(
                        level,
                        capital.getSovereign()
                );

        return "BABY".equalsIgnoreCase(
                ageState
        )
                || "TODDLER".equalsIgnoreCase(
                ageState
        );
    }

    private static DecisionSpeaker findProxySpeaker(
            ServerLevel level,
            CapitalRecord capital
    ) {
        LinkedHashSet<UUID> orderedCandidates =
                new LinkedHashSet<>();

        addIfPresent(
                orderedCandidates,
                capital.getHand()
        );

        addIfPresent(
                orderedCandidates,
                capital.getCommander()
        );

        addIfPresent(
                orderedCandidates,
                capital.getMasterOfLaws()
        );

        addIfPresent(
                orderedCandidates,
                capital.getGrandMaester()
        );

        addIfPresent(
                orderedCandidates,
                capital.getHerald()
        );

        orderedCandidates.addAll(
                capital.getRoyalGuards()
        );

        for (UUID candidateId :
                orderedCandidates) {
            DecisionSpeaker speaker =
                    resolveEligibleSpeaker(
                            level,
                            capital,
                            candidateId
                    );

            if (speaker != null) {
                return speaker;
            }
        }

        List<UUID> residents =
                new ArrayList<>(
                        CapitalResidentScanner
                                .scanResidents(
                                        level,
                                        capital
                                                .getCapitalId()
                                )
                );

        residents.sort(
                (first, second) ->
                        first.toString()
                                .compareTo(
                                        second.toString()
                                )
        );

        for (UUID residentId : residents) {
            if (residentId == null
                    || residentId.equals(
                    capital.getSovereign()
            )) {
                continue;
            }

            CapitalTitleResolver.ResolvedTitleId titleId =
                    CapitalTitleResolver
                            .getResolvedTitleId(
                                    level,
                                    capital,
                                    residentId
                            );

            if (!HIGH_RANKING_TITLES.contains(titleId)) {
                continue;
            }

            DecisionSpeaker speaker =
                    resolveEligibleSpeaker(
                            level,
                            capital,
                            residentId
                    );

            if (speaker != null) {
                return speaker;
            }
        }

        return null;
    }

    private static DecisionSpeaker
    resolveEligibleSpeaker(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId
    ) {
        if (candidateId == null
                || candidateId.equals(
                capital.getSovereign()
        )
                || !MCAIntegrationBridge
                .isTeenOrAdultVillager(
                        level,
                        candidateId
                )) {
            return null;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                candidateId
                        );

        if (entity == null
                || !entity.isAlive()
                || entity.isRemoved()) {
            return null;
        }

        return new DecisionSpeaker(
                candidateId,
                entity.getName().getString(),
                entity.getName()
        );
    }


    private static Component resolveTitledNameComponent(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId
    ) {
        if (villagerId == null) {
            return Component.translatable(
                    "mcacapitals.system.campaign.the_crown"
            );
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                villagerId
                        );

        if (entity != null) {
            return entity.getName();
        }

        Component baseNameComponent =
                CapitalNameService.resolveDisplayNameComponent(
                        level,
                        capital,
                        villagerId
                );

        CapitalTitleResolver.ResolvedTitleId titleId =
                CapitalTitleResolver.getResolvedTitleId(
                        level,
                        capital,
                        villagerId
                );

        if (titleId == CapitalTitleResolver.ResolvedTitleId.NONE
                || titleId == CapitalTitleResolver.ResolvedTitleId.COMMONER) {
            return baseNameComponent;
        }

        return Component.translatable(
                "mcacapitals.dynamic.name.titled",
                CapitalTitleResolver.getDisplayTitleComponent(
                        level,
                        capital,
                        villagerId
                ),
                baseNameComponent
        );
    }

    private static CapitalChronicleEntry.Argument
    resolveTitledNameChronicleArgument(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId
    ) {
        if (villagerId == null) {
            return CapitalChronicleService.translatableSnapshot(
                    "mcacapitals.system.campaign.the_crown"
            );
        }

        return CapitalChronicleService.literal(
                resolveTitledNameComponent(
                        level,
                        capital,
                        villagerId
                ).getString()
        );
    }

    private static void addIfPresent(
            Set<UUID> ids,
            UUID id
    ) {
        if (id != null) {
            ids.add(id);
        }
    }

    private record DecisionSpeaker(
            UUID villagerId,
            String displayName,
            Component displayComponent
    ) {
    }

    private record DecisionMessage(
            Component chatMessage,
            CapitalChronicleEventId eventId,
            CapitalChronicleEntry.Argument speakerName,
            CapitalChronicleEntry.Argument sovereignName
    ) {
    }
}
