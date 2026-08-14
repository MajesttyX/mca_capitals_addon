package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationKey;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import com.majesttyx.mcacapitals.data.CapitalWarCause;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;

public final class CapitalWarPlanningService {

    private static final int ENTRENCHED_HOSTILITY_THRESHOLD = -200;

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

        CapitalWarCause grievance =
                CapitalWarDataAccess.getGrievance(
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

        int score =
                CapitalDiplomacyDataAccess.getRelationshipScore(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                );

        if (score <= ENTRENCHED_HOSTILITY_THRESHOLD) {
            return CapitalWarCause.HOSTILE_RELATIONS;
        }

        return CapitalWarCause.UNJUST;
    }

    public static Component describePlan(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        CapitalWarCause cause = resolveCause(
                level,
                source,
                target
        );

        Component description = cause.isJustified()
                ? Component.translatable(
                        "mcacapitals.war.plan.cause",
                        cause.getDisplayComponent()
                )
                : Component.translatable("mcacapitals.war.plan.unjust");

        if (level != null
                && source != null
                && target != null
                && source.getCapitalId() != null
                && target.getCapitalId() != null
                && CapitalDiplomacyDataAccess.getDiplomaticState(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        ) == CapitalDiplomaticState.TRUCE) {
            return Component.translatable(
                    "mcacapitals.war.plan.with_truce_warning",
                    description
            );
        }

        return description;
    }

    public static Component validateRecovery(
            ServerLevel level,
            CapitalRecord source
    ) {
        if (level == null
                || source == null
                || source.getCapitalId() == null) {
            return Component.translatable("mcacapitals.war.validation.attacking_capital_unavailable");
        }

        long currentDay =
                CapitalWarDataAccess.currentDay(level);

        long availableDay =
                CapitalWarDataAccess.getCampaignAvailableDay(
                        level,
                        source.getCapitalId()
                );

        if (availableDay > currentDay) {
            return Component.translatable(
                    "mcacapitals.war.validation.recovering_until_day",
                    availableDay
            );
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
                CapitalDiplomacyDataAccess
                        .getRelationshipsSnapshot(level)
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
