package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
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
                changed = true;
            }
        }

        if (capital.getHerald() == null && capital.getSovereign() != null) {
            UUID newHerald = CapitalHeraldSelection.findHeraldCandidate(level, capital, residents);
            if (newHerald != null) {
                capital.setHerald(newHerald);
                capital.setHeraldFemale(MCAIntegrationBridge.isFemale(level, newHerald));
                if (announceAppointment) {
                    CapitalChronicleService.addEntry(
                            level,
                            capital,
                            resolveRawName(level, newHerald) + " now serves as Court Herald of "
                                    + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
                    );
                }
                changed = true;
            }
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static String resolveHeraldSpeakerName(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getHerald() == null) {
            return "Court Herald";
        }

        Entity herald = MCAIntegrationBridge.getEntityByUuid(level, capital.getHerald());
        if (herald != null) {
            String currentName = herald.getName().getString();
            if (currentName.startsWith("Court Herald ")) {
                return currentName;
            }
            return "Court Herald " + currentName;
        }

        return "Court Herald";
    }

    private static String resolveRawName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }
}