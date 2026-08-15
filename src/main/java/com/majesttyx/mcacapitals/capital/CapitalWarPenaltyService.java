package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationshipEvent;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import net.minecraft.server.level.ServerLevel;

public final class CapitalWarPenaltyService {

    private static final int UNJUST_RELATIONSHIP_PENALTY = -10;
    private static final long UNJUST_DIPLOMATIC_PENALTY_DAYS = 10L;

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

        String aggressorName = CapitalDiplomaticAgreementText.capitalName(
                level,
                aggressor
        );
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
}
