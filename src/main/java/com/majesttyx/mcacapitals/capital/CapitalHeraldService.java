package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

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

    public static boolean tickHerald(ServerLevel level, CapitalRecord capital, Set<UUID> residents, boolean announceAppointment) {
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
                capital.setHeraldDisplayName(resolveBaseName(level, capital, newHerald));
                if (announceAppointment) {
                    CapitalChronicleService.addEvent(
                            level,
                            capital,
                            CapitalChronicleEventId.COURT_HERALD_APPOINTED,
                            resolveBaseName(level, capital, newHerald),
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
        Entity herald = MCAIntegrationBridge.getEntityByUuid(level, heraldId);
        if (herald != null) {
            String baseName = resolveBaseName(level, capital, heraldId);
            capital.setHeraldDisplayName(baseName);
            return titledName(office, baseName);
        }

        String storedName = capital.getHeraldDisplayName();
        if (storedName != null && !storedName.isBlank()) {
            return titledName(office, storedName.trim());
        }

        return office;
    }

    private static Component titledName(Component office, String baseName) {
        return Component.translatable(
                "mcacapitals.dynamic.name.titled",
                office,
                Component.literal(baseName == null ? "" : baseName)
        );
    }

    private static String resolveBaseName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || entityId == null) {
            return entityId == null ? "Unknown" : entityId.toString();
        }

        String resolved = CapitalNameService.resolveDisplayName(level, capital, entityId);
        if (resolved != null && !resolved.isBlank()) {
            return resolved.trim();
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity != null && entity.getName() != null) {
            String fallback = entity.getName().getString();
            if (fallback != null && !fallback.isBlank()) {
                return fallback.trim();
            }
        }

        return entityId.toString();
    }
}
