package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationKey;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import com.majesttyx.mcacapitals.data.CapitalWarCause;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;

public final class CapitalWarPlanningService {

    private CapitalWarPlanningService() {
    }

    public static CapitalWarCause resolveCause(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null
                || source == null
                || target == null
                || source.getCapitalId() == null
                || target.getCapitalId() == null) {
            return CapitalWarCause.UNJUST;
        }

        CapitalWarCause grievance = CapitalWarDataAccess.getGrievance(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        if (grievance != null) {
            return grievance;
        }

        if (hasAttackedAlly(level, source, target)) {
            return CapitalWarCause.ALLY_ATTACKED;
        }

        int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        if (score <= -90) {
            return CapitalWarCause.HOSTILE_RELATIONS;
        }

        return CapitalWarCause.UNJUST;
    }

    public static String describePlan(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        CapitalWarCause cause = resolveCause(level, source, target);
        return cause.isJustified()
                ? "Cause: " + cause.getDisplayName() + "."
                : "No recognized cause exists. This will be an unjust war and will damage relations with every known capital.";
    }

    public static String validateRecovery(
            ServerLevel level,
            CapitalRecord source
    ) {
        if (level == null
                || source == null
                || source.getCapitalId() == null) {
            return "The attacking capital is unavailable.";
        }

        long currentDay = CapitalWarDataAccess.currentDay(level);
        long availableDay = CapitalWarDataAccess.getCampaignAvailableDay(
                level,
                source.getCapitalId()
        );
        if (availableDay > currentDay) {
            return "The capital is recovering from its previous campaign and cannot plan another war until day "
                    + availableDay
                    + ".";
        }

        return null;
    }

    private static boolean hasAttackedAlly(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        UUID sourceId = source.getCapitalId();
        UUID targetId = target.getCapitalId();

        for (Map.Entry<CapitalRelationKey, CapitalRelationRecord> entry :
                CapitalDiplomacyDataAccess.getRelationshipsSnapshot(level)
                        .entrySet()) {
            CapitalRelationKey key = entry.getKey();
            CapitalRelationRecord relation = entry.getValue();
            if (key == null
                    || relation == null
                    || relation.getDiplomaticState()
                    != CapitalDiplomaticState.ALLIANCE) {
                continue;
            }

            UUID allyId = null;
            if (sourceId.equals(key.first())) {
                allyId = key.second();
            } else if (sourceId.equals(key.second())) {
                allyId = key.first();
            }

            if (allyId == null || allyId.equals(targetId)) {
                continue;
            }

            if (CapitalDiplomacyDataAccess.getDiplomaticState(
                    level,
                    allyId,
                    targetId
            ) == CapitalDiplomaticState.WAR) {
                return true;
            }
        }

        return false;
    }
}