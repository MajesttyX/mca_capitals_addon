package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationKey;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

final class CapitalDiplomaticTruceService {

    static final long TRUCE_DURATION_TICKS =
            48000L;

    private CapitalDiplomaticTruceService() {
    }

    static void expireTruces(ServerLevel level) {
        if (level == null) {
            return;
        }

        long currentTime = level.getGameTime();

        for (Map.Entry<
                CapitalRelationKey,
                CapitalRelationRecord
                > entry :
                CapitalDiplomacyDataAccess
                        .get(level)
                        .getRelationshipsSnapshot()
                        .entrySet()) {
            CapitalRelationRecord relation =
                    entry.getValue();

            if (relation == null
                    || relation.getDiplomaticState()
                    != CapitalDiplomaticState.TRUCE
                    || relation.getTruceUntil() <= 0L
                    || relation.getTruceUntil()
                    > currentTime) {
                continue;
            }

            CapitalRecord first =
                    CapitalManager.getCapital(
                            entry.getKey().first()
                    );

            CapitalRecord second =
                    CapitalManager.getCapital(
                            entry.getKey().second()
                    );

            CapitalDiplomacyDataAccess.setDiplomaticState(
                    level,
                    entry.getKey().first(),
                    entry.getKey().second(),
                    CapitalDiplomaticState.PEACE,
                    0L
            );

            if (first == null || second == null) {
                continue;
            }

            String firstName =
                    CapitalDiplomaticAgreementText.capitalName(
                            level,
                            first
                    );

            String secondName =
                    CapitalDiplomaticAgreementText.capitalName(
                            level,
                            second
                    );

            String chronicle =
                    "The truce between "
                            + firstName
                            + " and "
                            + secondName
                            + " expired.";

            CapitalChronicleService.addEntry(
                    level,
                    first,
                    chronicle
            );

            CapitalChronicleService.addEntry(
                    level,
                    second,
                    chronicle
            );

            notifyCurrentPlayerSovereign(
                    level,
                    first,
                    "Truce Expired",
                    "The truce with "
                            + secondName
                            + " has expired."
            );

            notifyCurrentPlayerSovereign(
                    level,
                    second,
                    "Truce Expired",
                    "The truce with "
                            + firstName
                            + " has expired."
            );
        }
    }

    static void refreshExpiredTruce(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second
    ) {
        if (level == null
                || first == null
                || second == null
                || first.getCapitalId() == null
                || second.getCapitalId() == null) {
            return;
        }

        CapitalRelationRecord relation =
                CapitalDiplomacyDataAccess.getOrCreateRelationship(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        if (relation != null
                && relation.getDiplomaticState()
                == CapitalDiplomaticState.TRUCE
                && relation.getTruceUntil() > 0L
                && relation.getTruceUntil()
                <= level.getGameTime()) {
            CapitalDiplomacyDataAccess.setDiplomaticState(
                    level,
                    first.getCapitalId(),
                    second.getCapitalId(),
                    CapitalDiplomaticState.PEACE,
                    0L
            );
        }
    }

    private static void notifyCurrentPlayerSovereign(
            ServerLevel level,
            CapitalRecord capital,
            String title,
            String message
    ) {
        if (capital.getPlayerSovereignId() != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            capital.getPlayerSovereignId(),
                            title,
                            message
                    );
        }
    }
}