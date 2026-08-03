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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class CapitalCampaignCourtDecisionService {

    private static final int PEACE_REQUEST_CHANCE_PERCENT = 60;
    private static final long CROWN_RALLY_TICKS = 20L * 5L;

    private static final Set<String> HIGH_RANKING_TITLES = Set.of(
            "HIGH QUEEN", "HIGH KING", "QUEEN CONSORT", "KING CONSORT",
            "CROWN PRINCESS", "CROWN PRINCE", "PRINCESS", "PRINCE",
            "DOWAGER QUEEN", "DOWAGER KING", "DOWAGER PRINCESS", "DOWAGER PRINCE",
            "DUCHESS", "DUKE", "DOWAGER DUCHESS", "DOWAGER DUKE", "LADY", "LORD"
    );

    private CapitalCampaignCourtDecisionService() {
    }

    public static boolean resolveIfChildSovereign(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        if (!isBabyOrToddlerSovereign(level, defendingCapital)) {
            return false;
        }

        boolean suingForPeace = level.random.nextInt(100) < PEACE_REQUEST_CHANCE_PERCENT;
        DecisionMessage decision = buildDecisionMessage(level, defendingCapital, suingForPeace);
        recordDecision(attackingCapital, defendingCapital, decision.chronicleEntry());
        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                defendingCapital,
                Component.literal(decision.chatMessage())
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

            establishPeace(level, attackingCapital, defendingCapital);
            if (CapitalCampaignService.beginRetreat(
                    level,
                    campaign.getCampaignId(),
                    CapitalCampaignEndReason.DEFENDERS_SURRENDERED)) {
                CapitalCampaignTargetingService.clearCampaignTargets(level, campaign);
            }
            return true;
        }

        campaign.markDefendingSovereignRefusedPeace();
        campaign.beginCrownRally(level.getGameTime(), level.getGameTime() + CROWN_RALLY_TICKS);
        CapitalCampaignDataAccess.get(level).setDirty();
        return true;
    }

    private static DecisionMessage buildDecisionMessage(
            ServerLevel level,
            CapitalRecord defendingCapital,
            boolean suingForPeace
    ) {
        String defendingName = CapitalDiplomaticAgreementText.capitalName(level, defendingCapital);
        String sovereignName = resolveTitledName(
                level,
                defendingCapital,
                defendingCapital.getSovereign()
        );
        DecisionSpeaker speaker = findProxySpeaker(level, defendingCapital);
        String decisionText = suingForPeace
                ? defendingName + " sues for peace. The occupying force will withdraw."
                : defendingName + " refuses peace. The Crown will continue the fight after a 5-second rally.";

        if (speaker != null) {
            return new DecisionMessage(
                    speaker.displayName() + " speaks for " + sovereignName + ": " + decisionText,
                    speaker.displayName()
                            + " spoke for "
                            + sovereignName
                            + (suingForPeace
                            ? " and sued for peace after the capital's field defenders fell."
                            : " and refused peace after the capital's field defenders fell, ordering the Crown to continue the fight.")
            );
        }

        return new DecisionMessage(
                "The court of " + defendingName + " speaks for " + sovereignName + ": " + decisionText,
                "The court of "
                        + defendingName
                        + " spoke for "
                        + sovereignName
                        + (suingForPeace
                        ? " and sued for peace after the capital's field defenders fell."
                        : " and refused peace after the capital's field defenders fell, ordering the Crown to continue the fight.")
        );
    }

    private static void establishPeace(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        CapitalAgreementDataAccess.removeProposalsBetween(
                level,
                attackingCapital.getCapitalId(),
                defendingCapital.getCapitalId()
        );
        CapitalDiplomacyDataAccess.setDiplomaticState(
                level,
                attackingCapital.getCapitalId(),
                defendingCapital.getCapitalId(),
                CapitalDiplomaticState.PEACE,
                0L
        );
    }

    private static void recordDecision(
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            String entry
    ) {
        CapitalChronicleService.addEntryWithoutHerald(attackingCapital, entry);
        CapitalChronicleService.addEntryWithoutHerald(defendingCapital, entry);
    }

    private static boolean isBabyOrToddlerSovereign(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (capital == null || capital.getSovereign() == null) {
            return false;
        }
        String ageState = MCAIntegrationBridge.getAgeState(level, capital.getSovereign());
        return "BABY".equalsIgnoreCase(ageState) || "TODDLER".equalsIgnoreCase(ageState);
    }

    private static DecisionSpeaker findProxySpeaker(
            ServerLevel level,
            CapitalRecord capital
    ) {
        LinkedHashSet<UUID> orderedCandidates = new LinkedHashSet<>();
        addIfPresent(orderedCandidates, capital.getHand());
        addIfPresent(orderedCandidates, capital.getCommander());
        addIfPresent(orderedCandidates, capital.getMasterOfLaws());
        addIfPresent(orderedCandidates, capital.getGrandMaester());
        addIfPresent(orderedCandidates, capital.getHerald());
        orderedCandidates.addAll(capital.getRoyalGuards());

        for (UUID candidateId : orderedCandidates) {
            DecisionSpeaker speaker = resolveEligibleSpeaker(level, capital, candidateId);
            if (speaker != null) {
                return speaker;
            }
        }

        List<UUID> residents = new ArrayList<>(
                CapitalResidentScanner.scanResidents(level, capital.getCapitalId())
        );
        residents.sort((first, second) -> first.toString().compareTo(second.toString()));

        for (UUID residentId : residents) {
            if (residentId == null || residentId.equals(capital.getSovereign())) {
                continue;
            }
            String title = CapitalTitleResolver.getDisplayTitle(level, capital, residentId);
            if (title == null
                    || !HIGH_RANKING_TITLES.contains(title.trim().toUpperCase(Locale.ROOT))) {
                continue;
            }
            DecisionSpeaker speaker = resolveEligibleSpeaker(level, capital, residentId);
            if (speaker != null) {
                return speaker;
            }
        }
        return null;
    }

    private static DecisionSpeaker resolveEligibleSpeaker(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId
    ) {
        if (candidateId == null
                || candidateId.equals(capital.getSovereign())
                || !MCAIntegrationBridge.isTeenOrAdultVillager(level, candidateId)) {
            return null;
        }
        Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, candidateId);
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return null;
        }
        return new DecisionSpeaker(candidateId, entity.getName().getString());
    }

    private static String resolveTitledName(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId
    ) {
        if (villagerId == null) {
            return "the Crown";
        }
        Entity entity = MCAIntegrationBridge.findLoadedEntityByUuid(level, villagerId);
        if (entity != null) {
            return entity.getName().getString();
        }

        String baseName = CapitalNameService.resolveDisplayName(level, capital, villagerId);
        String title = CapitalTitleResolver.getDisplayTitle(level, capital, villagerId);
        if (title == null
                || title.isBlank()
                || "NONE".equalsIgnoreCase(title)
                || "COMMONER".equalsIgnoreCase(title)
                || baseName.startsWith(title + " ")) {
            return baseName;
        }
        return title + " " + baseName;
    }

    private static void addIfPresent(Set<UUID> ids, UUID id) {
        if (id != null) {
            ids.add(id);
        }
    }

    private record DecisionSpeaker(UUID villagerId, String displayName) {
    }

    private record DecisionMessage(String chatMessage, String chronicleEntry) {
    }
}
