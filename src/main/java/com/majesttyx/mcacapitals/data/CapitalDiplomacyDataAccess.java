package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalDiplomacyDataAccess {

    private CapitalDiplomacyDataAccess() {
    }

    public static CapitalDiplomacySavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CapitalDiplomacySavedData::load,
                        CapitalDiplomacySavedData::new,
                        CapitalDiplomacySavedData.DATA_NAME
                );
    }

    public static UUID getAmbassador(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return null;
        }

        return get(level).getAmbassador(capitalId);
    }

    public static void setAmbassador(
            ServerLevel level,
            UUID capitalId,
            UUID ambassadorId
    ) {
        if (level == null || capitalId == null) {
            return;
        }

        get(level).setAmbassador(
                capitalId,
                ambassadorId
        );
    }

    public static boolean clearAmbassador(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return false;
        }

        return get(level).clearAmbassador(capitalId);
    }

    public static Map<UUID, UUID>
    getAmbassadorsSnapshot(
            ServerLevel level
    ) {
        if (level == null) {
            return Map.of();
        }

        return get(level).getAmbassadorsSnapshot();
    }

    public static CapitalRelationRecord
    getOrCreateRelationship(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null
                || firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return null;
        }

        return get(level).getOrCreateRelationship(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static int getRelationshipScore(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return 0;
        }

        return get(level).getRelationshipScore(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static CapitalDiplomaticState
    getDiplomaticState(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null) {
            return CapitalDiplomaticState.PEACE;
        }

        return get(level).getDiplomaticState(
                firstCapitalId,
                secondCapitalId
        );
    }

    public static int adjustRelationship(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId,
            int amount,
            String reason,
            UUID initiatingCapitalId
    ) {
        if (level == null
                || firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return 0;
        }

        long gameDay = Math.max(
                1L,
                level.getDayTime() / 24000L + 1L
        );

        return get(level).adjustRelationship(
                firstCapitalId,
                secondCapitalId,
                amount,
                reason,
                gameDay,
                initiatingCapitalId
        );
    }

    public static int adjustRelationshipOrganic(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId,
            int amount,
            String reason
    ) {
        if (level == null
                || firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return 0;
        }

        long gameDay = Math.max(
                1L,
                level.getDayTime() / 24000L + 1L
        );

        return get(level).adjustRelationshipOrganic(
                firstCapitalId,
                secondCapitalId,
                amount,
                reason,
                gameDay
        );
    }

    public static Map<CapitalRelationKey, CapitalRelationRecord>
    getRelationshipsSnapshot(ServerLevel level) {
        if (level == null) {
            return Map.of();
        }

        return get(level).getRelationshipsSnapshot();
    }

    public static List<CapitalRelationshipEvent>
    getRelationshipHistory(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId
    ) {
        if (level == null
                || firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return List.of();
        }

        CapitalRelationRecord record =
                get(level).getRelationship(
                        firstCapitalId,
                        secondCapitalId
                );

        return record == null
                ? List.of()
                : List.copyOf(record.getHistory());
    }

    public static long getLastRelationshipDriftDay(
            ServerLevel level
    ) {
        return level == null
                ? 0L
                : get(level).getLastRelationshipDriftDay();
    }

    public static void setLastRelationshipDriftDay(
            ServerLevel level,
            long gameDay
    ) {
        if (level != null) {
            get(level).setLastRelationshipDriftDay(
                    gameDay
            );
        }
    }

    public static long getLastNpcInitiativeDay(
            ServerLevel level
    ) {
        return level == null
                ? 0L
                : get(level).getLastNpcInitiativeDay();
    }

    public static void setLastNpcInitiativeDay(
            ServerLevel level,
            long gameDay
    ) {
        if (level != null) {
            get(level).setLastNpcInitiativeDay(
                    gameDay
            );
        }
    }

    public static long getNpcInitiativeAvailableDay(
            ServerLevel level,
            UUID capitalId
    ) {
        return level == null || capitalId == null
                ? 0L
                : get(level).getNpcInitiativeAvailableDay(
                capitalId
        );
    }

    public static void setNpcInitiativeAvailableDay(
            ServerLevel level,
            UUID capitalId,
            long availableDay
    ) {
        if (level != null && capitalId != null) {
            get(level).setNpcInitiativeAvailableDay(
                    capitalId,
                    availableDay
            );
        }
    }

    public static void setDiplomaticState(
            ServerLevel level,
            UUID firstCapitalId,
            UUID secondCapitalId,
            CapitalDiplomaticState state,
            long truceUntil
    ) {
        if (level == null
                || firstCapitalId == null
                || secondCapitalId == null
                || firstCapitalId.equals(secondCapitalId)) {
            return;
        }

        get(level).setDiplomaticState(
                firstCapitalId,
                secondCapitalId,
                state,
                truceUntil
        );
    }

    public static long getGiftCooldownRemaining(
            ServerLevel level,
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        if (level == null) {
            return 0L;
        }

        return get(level).getGiftCooldownRemaining(
                sourceCapitalId,
                targetCapitalId,
                level.getGameTime()
        );
    }

    public static void beginGiftCooldown(
            ServerLevel level,
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        if (level == null
                || sourceCapitalId == null
                || targetCapitalId == null
                || sourceCapitalId.equals(targetCapitalId)) {
            return;
        }

        get(level).beginGiftCooldown(
                sourceCapitalId,
                targetCapitalId,
                level.getGameTime()
        );
    }

    public static void cleanExpiredGiftCooldowns(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        get(level).removeExpiredGiftCooldowns(
                level.getGameTime()
        );
    }

    public static boolean clearGiftCooldown(
            ServerLevel level,
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        return level != null
                && get(level).clearGiftCooldown(
                sourceCapitalId,
                targetCapitalId
        );
    }

    public static boolean removeCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        return level != null
                && capitalId != null
                && get(level).removeCapital(capitalId);
    }

    public static void addShipment(
            ServerLevel level,
            DiplomaticShipment shipment
    ) {
        if (level == null || shipment == null) {
            return;
        }

        get(level).addShipment(shipment);
    }

    public static DiplomaticShipment getShipment(
            ServerLevel level,
            UUID shipmentId
    ) {
        if (level == null || shipmentId == null) {
            return null;
        }

        return get(level).getShipment(shipmentId);
    }

    public static boolean removeShipment(
            ServerLevel level,
            UUID shipmentId
    ) {
        if (level == null || shipmentId == null) {
            return false;
        }

        return get(level).removeShipment(shipmentId);
    }

    public static List<DiplomaticShipment>
    getPendingPlayerShipments(
            ServerLevel level,
            UUID targetCapitalId
    ) {
        if (level == null
                || targetCapitalId == null) {
            return List.of();
        }

        return get(level).getPendingPlayerShipments(
                targetCapitalId
        );
    }
}