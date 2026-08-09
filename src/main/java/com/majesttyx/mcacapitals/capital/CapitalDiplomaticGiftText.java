package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

final class CapitalDiplomaticGiftText {

    private CapitalDiplomaticGiftText() {
    }

    static String getCapitalName(ServerLevel level, CapitalRecord capital) {
        if (capital == null || capital.getVillageId() == null) {
            return "Unknown Capital";
        }
        return MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
    }

    static String formatDuration(long ticks) {
        long totalHours = Math.max(1L, (ticks + 999L) / 1000L);
        long days = totalHours / 24L;
        long hours = totalHours % 24L;

        if (days > 0L && hours > 0L) {
            return days
                    + (days == 1L ? " day, " : " days, ")
                    + hours
                    + (hours == 1L ? " hour" : " hours");
        }
        if (days > 0L) {
            return days + (days == 1L ? " day" : " days");
        }
        return hours + (hours == 1L ? " hour" : " hours");
    }
}
