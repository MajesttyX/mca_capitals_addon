package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
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
}
