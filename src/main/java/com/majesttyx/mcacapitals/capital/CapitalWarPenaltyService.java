package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
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
                    "Unjust war declared against "
                            + CapitalDiplomaticAgreementText.capitalName(
                            level,
                            target
                    ),
                    aggressor.getCapitalId()
            );
        }

        CapitalWarDataAccess.setUnjustPenalty(
                level,
                aggressor.getCapitalId(),
                UNJUST_DIPLOMATIC_PENALTY_DAYS
        );

        String entry = CapitalDiplomaticAgreementText.capitalName(
                level,
                aggressor
        ) + " began an unjust war against "
                + CapitalDiplomaticAgreementText.capitalName(level, target)
                + ", damaging its standing with every other known capital.";

        CapitalChronicleService.addEntry(level, aggressor, entry);
        CapitalChronicleService.addEntry(level, target, entry);
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
                    "Condemned the breaking of a Truce with "
                            + CapitalDiplomaticAgreementText.capitalName(
                            level,
                            target
                    ),
                    aggressorId
            );

            condemnedBy++;
        }

        if (condemnedBy <= 0) {
            return;
        }

        String entry = CapitalDiplomaticAgreementText.capitalName(
                level,
                aggressor
        ) + " broke its Truce with "
                + CapitalDiplomaticAgreementText.capitalName(level, target)
                + ". "
                + condemnedBy
                + (condemnedBy == 1
                ? " other capital condemned the breach."
                : " other capitals condemned the breach.");

        CapitalChronicleService.addEntry(level, aggressor, entry);
        CapitalChronicleService.addEntry(level, target, entry);
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