package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

final class CapitalDiplomaticGiftText {

    private CapitalDiplomaticGiftText() {
    }

    static String getCapitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (capital == null || capital.getVillageId() == null) {
            return "Unknown Capital";
        }

        ServerLevel capitalLevel = CapitalManager.resolveCapitalLevel(level, capital);
        return MCAIntegrationBridge.getVillageName(
                capitalLevel,
                capital.getVillageId()
        );
    }

    static Component getCapitalNameComponent(
            ServerLevel level,
            CapitalRecord capital
    ) {
        String name = getCapitalName(level, capital);
        return name == null
                || name.isBlank()
                || "Unknown Capital".equals(name)
                ? Component.translatable("mcacapitals.diplomacy.unknown_capital")
                : Component.literal(name);
    }

    static Component formatDuration(long ticks) {
        long totalHours = Math.max(
                1L,
                (ticks + 999L) / 1000L
        );

        long days = totalHours / 24L;
        long hours = totalHours % 24L;

        if (days > 0L && hours > 0L) {
            return Component.translatable(
                    "mcacapitals.diplomacy.duration.days_hours",
                    days,
                    Component.translatable(
                            days == 1L
                                    ? "mcacapitals.diplomacy.duration.day"
                                    : "mcacapitals.diplomacy.duration.days"
                    ),
                    hours,
                    Component.translatable(
                            hours == 1L
                                    ? "mcacapitals.diplomacy.duration.hour"
                                    : "mcacapitals.diplomacy.duration.hours"
                    )
            );
        }

        if (days > 0L) {
            return Component.translatable(
                    "mcacapitals.diplomacy.duration.single_unit",
                    days,
                    Component.translatable(
                            days == 1L
                                    ? "mcacapitals.diplomacy.duration.day"
                                    : "mcacapitals.diplomacy.duration.days"
                    )
            );
        }

        return Component.translatable(
                "mcacapitals.diplomacy.duration.single_unit",
                hours,
                Component.translatable(
                        hours == 1L
                                ? "mcacapitals.diplomacy.duration.hour"
                                : "mcacapitals.diplomacy.duration.hours"
                )
        );
    }
}
