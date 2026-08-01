package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;
import java.util.UUID;

public final class CapitalWarDataAccess {

    private CapitalWarDataAccess() {
    }

    public static CapitalWarSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CapitalWarSavedData::load,
                        CapitalWarSavedData::new,
                        CapitalWarSavedData.DATA_NAME
                );
    }

    public static void recordGrievance(
            ServerLevel level,
            UUID sourceCapitalId,
            UUID targetCapitalId,
            CapitalWarCause cause,
            long durationDays
    ) {
        if (level == null) {
            return;
        }

        long currentDay = currentDay(level);
        get(level).recordGrievance(
                sourceCapitalId,
                targetCapitalId,
                cause,
                durationDays <= 0L
                        ? 0L
                        : currentDay + durationDays
        );
    }

    public static CapitalWarCause getGrievance(
            ServerLevel level,
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        return level == null
                ? null
                : get(level).getGrievance(
                sourceCapitalId,
                targetCapitalId,
                currentDay(level)
        );
    }

    public static void consumeGrievance(
            ServerLevel level,
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        if (level != null) {
            get(level).consumeGrievance(
                    sourceCapitalId,
                    targetCapitalId
            );
        }
    }

    public static long getCampaignAvailableDay(
            ServerLevel level,
            UUID capitalId
    ) {
        return level == null
                ? 0L
                : get(level).getCampaignAvailableDay(capitalId);
    }

    public static void setCampaignRecovery(
            ServerLevel level,
            UUID capitalId,
            long durationDays
    ) {
        if (level != null && capitalId != null) {
            get(level).setCampaignAvailableDay(
                    capitalId,
                    currentDay(level) + Math.max(0L, durationDays)
            );
        }
    }

    public static long getUnjustPenaltyUntilDay(
            ServerLevel level,
            UUID capitalId
    ) {
        return level == null
                ? 0L
                : get(level).getUnjustPenaltyUntilDay(capitalId);
    }

    public static void setUnjustPenalty(
            ServerLevel level,
            UUID capitalId,
            long durationDays
    ) {
        if (level != null && capitalId != null) {
            get(level).setUnjustPenaltyUntilDay(
                    capitalId,
                    currentDay(level) + Math.max(0L, durationDays)
            );
        }
    }

    public static boolean hasActiveUnjustPenalty(
            ServerLevel level,
            UUID capitalId
    ) {
        return level != null
                && getUnjustPenaltyUntilDay(level, capitalId)
                >= currentDay(level);
    }

    public static boolean removeCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        return level != null
                && capitalId != null
                && get(level).removeCapital(capitalId);
    }

    public static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }
}