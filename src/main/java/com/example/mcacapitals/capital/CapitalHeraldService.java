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
                capital.setHeraldDisplayName(resolveBaseName(level, newHerald));
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
            String baseName = resolveBaseNameFromCurrentName(herald.getName().getString(), capital.getHerald().toString());
            capital.setHeraldDisplayName(baseName);
            return "Court Herald " + baseName;
        }

        String storedName = capital.getHeraldDisplayName();
        if (storedName != null && !storedName.isBlank()) {
            return "Court Herald " + storedName.trim();
        }

        return "Court Herald";
    }

    private static String resolveRawName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }

    private static String resolveBaseName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return resolveBaseNameFromCurrentName(entity != null ? entity.getName().getString() : null, entityId.toString());
    }

    private static String resolveBaseNameFromCurrentName(String currentName, String fallback) {
        if (currentName == null || currentName.isBlank()) {
            return fallback;
        }

        String result = currentName.trim();
        String[] prefixes = {
                "Court Herald",
                "High Queen",
                "High King",
                "Queen Consort",
                "King Consort",
                "Dowager Queen",
                "Dowager King",
                "Heir Apparent",
                "Crown Princess",
                "Crown Prince",
                "Dowager Princess",
                "Dowager Prince",
                "Princess Consort",
                "Prince Consort",
                "Hand of the Queen",
                "Hand of the King",
                "Grand Maester",
                "Maester",
                "Lord Commander",
                "Commander",
                "Dowager Duchess",
                "Dowager Duke",
                "Duchess",
                "Duke",
                "Lady",
                "Lord",
                "Dame",
                "Sir",
                "Princess",
                "Prince",
                "Queen",
                "King"
        };

        boolean stripped;
        do {
            stripped = false;
            for (String prefix : prefixes) {
                if (result.equals(prefix)) {
                    result = "";
                    stripped = true;
                    break;
                }
                String titledPrefix = prefix + " ";
                if (result.startsWith(titledPrefix)) {
                    result = result.substring(titledPrefix.length()).trim();
                    stripped = true;
                    break;
                }
            }
        } while (stripped);

        return result.isBlank() ? fallback : result;
    }
}