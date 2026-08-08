package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class CapitalDiplomaticWorldService {
    private static final int NPC_INITIATIVE_CHANCE = 15;
    private static final long NPC_INITIATIVE_COOLDOWN_DAYS = 3L;

    private CapitalDiplomaticWorldService() {
    }

    static void tick(ServerLevel level) {
        if (level == null) {
            return;
        }

        long gameDay = currentDay(level);
        if (CapitalDiplomacyDataAccess
                .getLastRelationshipDriftDay(level) < gameDay) {
            processRelationshipDrift(level);
            CapitalDiplomacyDataAccess
                    .setLastRelationshipDriftDay(
                            level,
                            gameDay
                    );
        }

        if (CapitalDiplomacyDataAccess
                .getLastNpcInitiativeDay(level) < gameDay) {
            processNpcInitiatives(level, gameDay);
            CapitalDiplomacyDataAccess
                    .setLastNpcInitiativeDay(
                            level,
                            gameDay
                    );
        }
    }

    private static void processRelationshipDrift(
            ServerLevel level
    ) {
        List<CapitalRecord> capitals = activeCapitals();

        for (int firstIndex = 0;
             firstIndex < capitals.size();
             firstIndex++) {
            CapitalRecord first = capitals.get(firstIndex);

            for (int secondIndex = firstIndex + 1;
                 secondIndex < capitals.size();
                 secondIndex++) {
                CapitalRecord second = capitals.get(secondIndex);

                CapitalDiplomacyDataAccess
                        .getOrCreateRelationship(
                                level,
                                first.getCapitalId(),
                                second.getCapitalId()
                        );

                applyDrift(
                        level,
                        first,
                        second
                );
            }
        }
    }

    private static void applyDrift(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                first,
                second
        );

        int score = CapitalDiplomacyDataAccess
                .getRelationshipScore(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        CapitalDiplomaticState state =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                first.getCapitalId(),
                                second.getCapitalId()
                        );

        if (state == CapitalDiplomaticState.WAR) {
            return;
        }

        int roll = level.random.nextInt(100);
        int adjustment = 0;
        String reason = "";

        if (state == CapitalDiplomaticState.TRUCE) {
            if (score != 0 && roll < 20) {
                adjustment = score > 0 ? -1 : 1;
                reason = "Truce eased wartime hostility";
            }
        } else if (state == CapitalDiplomaticState.ALLIANCE) {
            if (roll < 25) {
                adjustment = 1;
                reason = "Alliance strengthened relations";
            } else if (roll < 27) {
                adjustment = -1;
                reason = "Minor tension within the alliance";
            }
        } else if (state
                == CapitalDiplomaticState.NON_AGGRESSION_PACT) {
            if (roll < 15) {
                adjustment = 1;
                reason = "Non-Aggression Pact strengthened relations";
            } else if (roll < 18) {
                adjustment = -1;
                reason = "Minor diplomatic friction";
            }
        } else {
            CapitalRelationshipBand band =
                    CapitalRelationshipBand.fromScore(score);

            switch (band) {
                case EXCELLENT, FRIENDLY, CORDIAL -> {
                    if (roll < 10) {
                        adjustment = 1;
                        reason = "Friendly relations gradually improved";
                    } else if (roll < 15) {
                        adjustment = -1;
                        reason = "Routine diplomatic friction";
                    }
                }
                case NEUTRAL -> {
                    if (roll < 5) {
                        adjustment = 1;
                        reason = "Routine contact improved relations";
                    } else if (roll < 10) {
                        adjustment = -1;
                        reason = "Routine contact strained relations";
                    }
                }
                case STRAINED, HOSTILE, BITTER_ENEMIES -> {
                    if (roll < 5) {
                        adjustment = 1;
                        reason = "Hostilities briefly eased";
                    } else if (roll < 15) {
                        adjustment = -1;
                        reason = "Hostile relations continued to worsen";
                    }
                }
            }
        }

        if (adjustment == 0) {
            return;
        }

        if (state == CapitalDiplomaticState.PEACE) {
            CapitalDiplomacyDataAccess
                    .adjustRelationshipOrganic(
                            level,
                            first.getCapitalId(),
                            second.getCapitalId(),
                            adjustment,
                            reason
                    );
        } else {
            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    first.getCapitalId(),
                    second.getCapitalId(),
                    adjustment,
                    reason,
                    null
            );
        }
    }

    private static void processNpcInitiatives(
            ServerLevel level,
            long gameDay
    ) {
        List<CapitalRecord> capitals = activeCapitals();

        for (CapitalRecord source : capitals) {
            if (!isFullyNpcGoverned(level, source)
                    || !CapitalBuildingService
                    .hasAmbassadorBuildings(level, source)
                    || CapitalAmbassadorService
                    .getAmbassador(level, source) == null
                    || CapitalDiplomacyDataAccess
                    .getNpcInitiativeAvailableDay(
                            level,
                            source.getCapitalId()
                    ) > gameDay
                    || level.random.nextInt(100)
                    >= NPC_INITIATIVE_CHANCE) {
                continue;
            }

            if (tryNpcInitiative(
                    level,
                    source,
                    capitals
            )) {
                CapitalDiplomacyDataAccess
                        .setNpcInitiativeAvailableDay(
                                level,
                                source.getCapitalId(),
                                gameDay
                                        + NPC_INITIATIVE_COOLDOWN_DAYS
                        );
            }
        }
    }

    private static boolean tryNpcInitiative(
            ServerLevel level,
            CapitalRecord source,
            List<CapitalRecord> capitals
    ) {
        List<ProposalCandidate> candidates =
                collectProposalCandidates(
                        level,
                        source,
                        capitals
                );

        if (!candidates.isEmpty()) {
            ProposalCandidate candidate = candidates.get(
                    level.random.nextInt(
                            candidates.size()
                    )
            );

            return createNpcProposal(
                    level,
                    source,
                    candidate.target(),
                    candidate.type()
            );
        }

        List<CapitalRecord> warningTargets =
                new ArrayList<>();

        for (CapitalRecord target : capitals) {
            if (target == null
                    || target.getCapitalId().equals(
                    source.getCapitalId()
            )) {
                continue;
            }

            int score = CapitalDiplomacyDataAccess
                    .getRelationshipScore(
                            level,
                            source.getCapitalId(),
                            target.getCapitalId()
                    );

            if (score <= -120
                    && CapitalDiplomaticAuthorityService
                    .getPlayerDecisionMaker(
                            level,
                            target
                    ) != null) {
                warningTargets.add(target);
            }
        }

        if (warningTargets.isEmpty()) {
            return false;
        }

        CapitalRecord target = warningTargets.get(
                level.random.nextInt(
                        warningTargets.size()
                )
        );

        UUID recipient = CapitalDiplomaticAuthorityService
                .getPlayerDecisionMaker(
                        level,
                        target
                );

        if (recipient == null) {
            return false;
        }

        CapitalDiplomaticAgreementCorrespondenceService
                .sendNotice(
                        level,
                        recipient,
                        "Relations Deteriorating",
                        CapitalDiplomaticAgreementText
                                .capitalName(level, source)
                                + " warns that relations with "
                                + CapitalDiplomaticAgreementText
                                .capitalName(level, target)
                                + " have become hostile."
                );

        CapitalChronicleService.addEntry(
                level,
                source,
                "A warning about deteriorating relations was sent to "
                        + CapitalDiplomaticAgreementText
                        .capitalName(level, target)
                        + "."
        );

        return true;
    }

    private static List<ProposalCandidate>
    collectProposalCandidates(
            ServerLevel level,
            CapitalRecord source,
            List<CapitalRecord> capitals
    ) {
        List<ProposalCandidate> candidates =
                new ArrayList<>();

        for (CapitalRecord target : capitals) {
            if (target == null
                    || target.getCapitalId().equals(
                    source.getCapitalId()
            )
                    || CapitalDiplomaticAgreementValidation
                    .getCurrentSovereignId(target) == null
                    || CapitalAgreementDataAccess
                    .findPendingBetween(
                            level,
                            source.getCapitalId(),
                            target.getCapitalId()
                    ) != null) {
                continue;
            }

            CapitalDiplomaticTruceService
                    .refreshExpiredTruce(
                            level,
                            source,
                            target
                    );

            int score = CapitalDiplomacyDataAccess
                    .getRelationshipScore(
                            level,
                            source.getCapitalId(),
                            target.getCapitalId()
                    );

            CapitalDiplomaticState state =
                    CapitalDiplomacyDataAccess
                            .getDiplomaticState(
                                    level,
                                    source.getCapitalId(),
                                    target.getCapitalId()
                            );

            for (DiplomaticProposalType type :
                    DiplomaticProposalType.values()) {
                if (score < type.getMinimumRelationship()) {
                    continue;
                }

                String failure =
                        CapitalDiplomaticAgreementValidation
                                .validateProposal(
                                        level,
                                        source,
                                        target,
                                        type,
                                        state,
                                        score
                                );

                if (failure == null) {
                    candidates.add(
                            new ProposalCandidate(
                                    target,
                                    type
                            )
                    );
                }
            }
        }

        return candidates;
    }

    private static boolean createNpcProposal(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposalType type
    ) {
        UUID sourceSovereignId =
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(source);

        UUID targetSovereignId =
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(target);

        if (sourceSovereignId == null
                || targetSovereignId == null) {
            return false;
        }

        DiplomaticProposal proposal;

        if (type == DiplomaticProposalType.ROYAL_BETROTHAL) {
            CapitalRoyalBetrothalService.Match match =
                    CapitalRoyalBetrothalService
                            .findMatch(
                                    level,
                                    source,
                                    target
                            );

            if (match == null) {
                return false;
            }

            proposal = new DiplomaticProposal(
                    UUID.randomUUID(),
                    source.getCapitalId(),
                    target.getCapitalId(),
                    sourceSovereignId,
                    targetSovereignId,
                    null,
                    type,
                    level.getGameTime(),
                    match.sourceRoyalId(),
                    match.targetRoyalId(),
                    match.relocatingRoyalId(),
                    match.destinationCapitalId()
            );
        } else {
            proposal = new DiplomaticProposal(
                    UUID.randomUUID(),
                    source.getCapitalId(),
                    target.getCapitalId(),
                    sourceSovereignId,
                    targetSovereignId,
                    type,
                    level.getGameTime()
            );
        }

        CapitalAgreementDataAccess.addProposal(
                level,
                proposal
        );

        CapitalChronicleService.addEntry(
                level,
                source,
                CapitalDiplomaticAgreementText
                        .capitalizedWithIndefiniteArticle(
                                type.getDisplayName()
                        )
                        + " was proposed to "
                        + CapitalDiplomaticAgreementText
                        .capitalName(level, target)
                        + "."
        );

        UUID targetPlayerId =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        if (targetPlayerId != null) {
            CapitalDiplomaticProposalService
                    .sendProposalToPlayer(
                            level,
                            proposal,
                            source,
                            target,
                            targetPlayerId
                    );
        } else {
            CapitalDiplomaticProposalResolutionService
                    .resolveNpcProposal(
                            level,
                            proposal
                    );
        }

        return true;
    }

    private static boolean isFullyNpcGoverned(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return capital != null
                && capital.getPlayerSovereignId() == null
                && capital.getSovereign() != null
                && CapitalDiplomaticAuthorityService
                .getPlayerDecisionMaker(
                        level,
                        capital
                ) == null;
    }

    private static List<CapitalRecord> activeCapitals() {
        List<CapitalRecord> result =
                new ArrayList<>();

        for (CapitalRecord capital :
                CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && capital.getCapitalId() != null
                    && capital.getState()
                    == CapitalState.ACTIVE) {
                result.add(capital);
            }
        }

        Collections.sort(
                result,
                (first, second) ->
                        first.getCapitalId()
                                .compareTo(
                                        second.getCapitalId()
                                )
        );

        return result;
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(
                1L,
                level.getDayTime() / 24000L + 1L
        );
    }

    private record ProposalCandidate(
            CapitalRecord target,
            DiplomaticProposalType type
    ) {
    }
}