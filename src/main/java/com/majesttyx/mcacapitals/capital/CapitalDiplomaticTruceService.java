package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationKey;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

final class CapitalDiplomaticTruceService {

    static final long TRUCE_DURATION_TICKS = 48000L;

    private CapitalDiplomaticTruceService() {
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

        CapitalRelationRecord relation = CapitalDiplomacyDataAccess.getRelationshipsSnapshot(level)
                .get(CapitalRelationKey.of(first.getCapitalId(), second.getCapitalId()));
        if (relation == null
                || relation.getDiplomaticState() != CapitalDiplomaticState.TRUCE
                || relation.getTruceUntil() <= 0L
                || level.getGameTime() < relation.getTruceUntil()) {
            return;
        }

        CapitalDiplomacyDataAccess.setDiplomaticState(
                level,
                first.getCapitalId(),
                second.getCapitalId(),
                CapitalDiplomaticState.PEACE,
                0L
        );

        String firstName = CapitalDiplomaticAgreementText.capitalName(level, first);
        String secondName = CapitalDiplomaticAgreementText.capitalName(level, second);
        CapitalChronicleService.addEvent(
                level,
                first,
                CapitalChronicleEventId.TRUCE_EXPIRED,
                firstName,
                secondName
        );
        CapitalChronicleService.addEvent(
                level,
                second,
                CapitalChronicleEventId.TRUCE_EXPIRED,
                firstName,
                secondName
        );
    }

    static void expireTruces(ServerLevel level) {
        if (level == null) {
            return;
        }
        for (Map.Entry<CapitalRelationKey, CapitalRelationRecord> entry :
                CapitalDiplomacyDataAccess.getRelationshipsSnapshot(level).entrySet()) {
            CapitalRelationRecord relation = entry.getValue();
            if (relation == null
                    || relation.getDiplomaticState() != CapitalDiplomaticState.TRUCE
                    || relation.getTruceUntil() <= 0L
                    || level.getGameTime() < relation.getTruceUntil()) {
                continue;
            }
            CapitalRecord first = CapitalManager.getCapital(entry.getKey().first());
            CapitalRecord second = CapitalManager.getCapital(entry.getKey().second());
            if (first != null && second != null) {
                refreshExpiredTruce(level, first, second);
            } else {
                CapitalDiplomacyDataAccess.setDiplomaticState(
                        level,
                        entry.getKey().first(),
                        entry.getKey().second(),
                        CapitalDiplomaticState.PEACE,
                        0L
                );
            }
        }
    }
}
