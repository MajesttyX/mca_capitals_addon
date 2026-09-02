package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;

public final class CapitalHeraldService {

    private CapitalHeraldService() {
    }

    public static boolean tickHerald(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        return tickHerald(level, capital, residents, true);
    }

    public static boolean refreshHeraldAfterStatusChange(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        return tickHerald(level, capital, residents, true);
    }

    public static boolean tickHerald(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            boolean announceAppointment
    ) {
        if (level == null || capital == null || residents == null) {
            return false;
        }

        boolean changed = false;
        UUID previousHerald = capital.getHerald();

        if (!CapitalHeraldSelection.isValidHerald(level, capital, previousHerald, residents)) {
            if (previousHerald != null) {
                capital.setHerald(null);
                capital.setHeraldFemale(false);
                capital.setHeraldDisplayName(null);
                changed = true;
            }
        }

        if (capital.getHerald() == null && capital.getSovereign() != null) {
            UUID newHerald = CapitalHeraldSelection.findHeraldCandidate(level, capital, residents);
            if (newHerald != null) {
                capital.setHerald(newHerald);
                capital.setHeraldFemale(MCAIntegrationBridge.isFemale(level, newHerald));

                String baseName = CapitalNameService.resolveDisplayName(level, capital, newHerald);
                capital.setHeraldDisplayName(baseName);

                if (announceAppointment) {
                    CapitalChronicleService.addEvent(
                            level,
                            capital,
                            CapitalChronicleEventId.COURT_HERALD_APPOINTED,
                            baseName,
                            MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
                    );
                }
                changed = true;
            }
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static Component resolveHeraldSpeakerName(ServerLevel level, CapitalRecord capital) {
        Component office = Component.translatable("mcacapitals.dynamic.office.court_herald");
        if (level == null || capital == null || capital.getHerald() == null) {
            return office;
        }

        UUID heraldId = capital.getHerald();
        String baseName = CapitalNameService.resolveDisplayName(level, capital, heraldId);

        if (isUsableStoredName(baseName, heraldId)) {
            capital.setHeraldDisplayName(baseName.trim());
            return Component.translatable(
                    "mcacapitals.dynamic.name.titled",
                    office,
                    Component.literal(baseName.trim())
            );
        }

        String storedName = capital.getHeraldDisplayName();
        if (isUsableStoredName(storedName, heraldId)) {
            return Component.translatable(
                    "mcacapitals.dynamic.name.titled",
                    office,
                    Component.literal(storedName.trim())
            );
        }

        return office;
    }

    private static boolean isUsableStoredName(String name, UUID entityId) {
        if (name == null || name.isBlank()) {
            return false;
        }

        String value = name.trim();
        return entityId == null || !entityId.toString().equals(value);
    }
}
