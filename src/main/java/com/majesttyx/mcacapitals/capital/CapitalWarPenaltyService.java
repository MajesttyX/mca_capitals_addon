package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationshipEvent;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class CapitalWarPenaltyService {

    private static final int UNJUST_RELATIONSHIP_PENALTY = -10;
    private static final long UNJUST_DIPLOMATIC_PENALTY_DAYS = 10L;

    private static final int TRUCE_BREAK_RELATIONSHIP_PENALTY = -50;
    private static final int AGGRESSOR_ALLY_REACTION_CHANCE = 10;
    private static final int NON_ALLIED_REACTION_CHANCE = 35;
    private static final int TARGET_FRIEND_REACTION_CHANCE = 60;
    private static final int TARGET_ALLY_REACTION_CHANCE = 75;
    private static final int TARGET_FRIEND_RELATIONSHIP_MINIMUM = 100;

    private CapitalWarPenaltyService() {
    }

    public static void applyUnjustWarPenalty(
            ServerLevel level,
            CapitalRecord aggressor,
            CapitalRecord target
    ) {
        if (level == null
                || aggressor == null
                || target == null
                || aggressor.getCapitalId() == null
                || target.getCapitalId() == null) {
            return;
        }

        for (CapitalRecord known :
                CapitalManager.getAllCapitalsSnapshot().values()) {
            if (known == null
                    || known.getCapitalId() == null
                    || known.getCapitalId().equals(
                    aggressor.getCapitalId()
            )
                    || known.getCapitalId().equals(
                    target.getCapitalId()
            )) {
                continue;
            }

            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    aggressor.getCapitalId(),
                    known.getCapitalId(),
                    UNJUST_RELATIONSHIP_PENALTY,
                    CapitalRelationshipEvent.localizedReason(
                            "mcacapitals.relationship_reason.unjust_war_declared_against",
                            CapitalDiplomaticAgreementText.capitalName(
                                    level,
                                    target
                            )
                    ),
                    aggressor.getCapitalId()
            );
        }

        CapitalWarDataAccess.setUnjustPenalty(
                level,
                aggressor.getCapitalId(),
                UNJUST_DIPLOMATIC_PENALTY_DAYS
        );

        String aggressorName = CapitalDiplomaticAgreementText.capitalName(level, aggressor);
        String targetName = CapitalDiplomaticAgreementText.capitalName(level, target);

        CapitalChronicleService.addEvent(
                level,
                aggressor,
                CapitalChronicleEventId.UNJUST_WAR,
                aggressorName,
                targetName
        );
        CapitalChronicleService.addEvent(
                level,
                target,
                CapitalChronicleEventId.UNJUST_WAR,
                aggressorName,
                targetName
        );
    }

    public static void applyTruceBreakingPenalty(
            ServerLevel level,
            CapitalRecord aggressor,
            CapitalRecord target
    ) {
        if (level == null
                || aggressor == null
                || target == null
                || aggressor.getCapitalId() == null
                || target.getCapitalId() == null) {
            return;
        }

        UUID aggressorId = aggressor.getCapitalId();
        UUID targetId = target.getCapitalId();
        int condemnedBy = 0;

        for (CapitalRecord observer :
                CapitalManager.getAllCapitalsSnapshot().values()) {
            if (observer == null
                    || observer.getCapitalId() == null
                    || observer.getCapitalId().equals(aggressorId)
                    || observer.getCapitalId().equals(targetId)) {
                continue;
            }

            UUID observerId = observer.getCapitalId();

            int chance = truceBreakingReactionChance(
                    level,
                    aggressorId,
                    targetId,
                    observerId
            );

            if (level.random.nextInt(100) >= chance) {
                continue;
            }

            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    aggressorId,
                    observerId,
                    TRUCE_BREAK_RELATIONSHIP_PENALTY,
                    CapitalRelationshipEvent.localizedReason(
                            "mcacapitals.relationship_reason.truce_break_condemned",
                            CapitalDiplomaticAgreementText.capitalName(
                                    level,
                                    target
                            )
                    ),
                    aggressorId
            );

            condemnedBy++;
        }

        if (condemnedBy <= 0) {
            return;
        }

        String aggressorName = CapitalDiplomaticAgreementText.capitalName(level, aggressor);
        String targetName = CapitalDiplomaticAgreementText.capitalName(level, target);

        CapitalChronicleService.addEvent(
                level,
                aggressor,
                CapitalChronicleEventId.TRUCE_BROKEN,
                aggressorName,
                targetName,
                condemnedBy
        );
        CapitalChronicleService.addEvent(
                level,
                target,
                CapitalChronicleEventId.TRUCE_BROKEN,
                aggressorName,
                targetName,
                condemnedBy
        );
    }

    private static int truceBreakingReactionChance(
            ServerLevel level,
            UUID aggressorId,
            UUID targetId,
            UUID observerId
    ) {
        CapitalDiplomaticState observerTargetState =
                CapitalDiplomacyDataAccess.getDiplomaticState(
                        level,
                        observerId,
                        targetId
                );

        if (observerTargetState == CapitalDiplomaticState.ALLIANCE) {
            return TARGET_ALLY_REACTION_CHANCE;
        }

        int observerTargetScore =
                CapitalDiplomacyDataAccess.getRelationshipScore(
                        level,
                        observerId,
                        targetId
                );

        if (observerTargetScore >= TARGET_FRIEND_RELATIONSHIP_MINIMUM) {
            return TARGET_FRIEND_REACTION_CHANCE;
        }

        CapitalDiplomaticState observerAggressorState =
                CapitalDiplomacyDataAccess.getDiplomaticState(
                        level,
                        observerId,
                        aggressorId
                );

        if (observerAggressorState == CapitalDiplomaticState.ALLIANCE) {
            return AGGRESSOR_ALLY_REACTION_CHANCE;
        }

        return NON_ALLIED_REACTION_CHANCE;
    }
}