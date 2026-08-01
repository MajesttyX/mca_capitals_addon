package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapitalPlayerWarrantDataAccess {

    private CapitalPlayerWarrantDataAccess() {
    }

    public static CapitalPlayerWarrantSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CapitalPlayerWarrantSavedData::load,
                        CapitalPlayerWarrantSavedData::new,
                        CapitalPlayerWarrantSavedData.DATA_NAME
                );
    }

    public static void setLeaveOrder(
            ServerLevel level,
            UUID playerId,
            UUID capitalId,
            long remainingTicks,
            String caseKey
    ) {
        if (level != null) {
            get(level).setLeaveOrder(playerId, capitalId, remainingTicks, caseKey);
        }
    }

    public static Map<UUID, CapitalPlayerWarrantSavedData.LeaveOrder> getLeaveOrders(
            ServerLevel level,
            UUID playerId
    ) {
        return level == null || playerId == null
                ? Map.of()
                : get(level).getLeaveOrders(playerId);
    }

    public static boolean updateLeaveOrder(
            ServerLevel level,
            UUID playerId,
            UUID capitalId,
            long remainingTicks
    ) {
        return level != null
                && get(level).updateLeaveOrder(playerId, capitalId, remainingTicks);
    }

    public static boolean clearLeaveOrder(
            ServerLevel level,
            UUID playerId,
            UUID capitalId
    ) {
        return level != null
                && get(level).clearLeaveOrder(playerId, capitalId);
    }

    public static boolean issueWarrant(ServerLevel level, UUID playerId, UUID capitalId) {
        return level != null && get(level).issueWarrant(playerId, capitalId);
    }

    public static boolean hasWarrant(ServerLevel level, UUID playerId, UUID capitalId) {
        return level != null && get(level).hasWarrant(playerId, capitalId);
    }

    public static Set<UUID> getWarrantCapitals(ServerLevel level, UUID playerId) {
        return level == null || playerId == null
                ? Set.of()
                : get(level).getWarrantCapitals(playerId);
    }

    public static boolean clearWarrant(ServerLevel level, UUID playerId, UUID capitalId) {
        return level != null && get(level).clearWarrant(playerId, capitalId);
    }

    public static void setSentence(
            ServerLevel level,
            UUID playerId,
            UUID capitalId,
            long remainingTicks
    ) {
        if (level != null) {
            get(level).setSentence(playerId, capitalId, remainingTicks);
        }
    }

    public static Map<UUID, Long> getSentences(ServerLevel level, UUID playerId) {
        return level == null || playerId == null
                ? Map.of()
                : get(level).getSentences(playerId);
    }

    public static boolean updateSentence(
            ServerLevel level,
            UUID playerId,
            UUID capitalId,
            long remainingTicks
    ) {
        return level != null
                && get(level).updateSentence(playerId, capitalId, remainingTicks);
    }

    public static boolean markCasePenalized(ServerLevel level, String caseKey) {
        return level != null && get(level).markCasePenalized(caseKey);
    }

    public static boolean isCasePenalized(ServerLevel level, String caseKey) {
        return level != null && get(level).isCasePenalized(caseKey);
    }
}