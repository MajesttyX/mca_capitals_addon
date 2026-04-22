package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public final class CapitalHandService {

    public static final int REQUIRED_POPULATION = 20;

    private CapitalHandService() {
    }

    public static boolean tickHand(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null || residents == null) {
            return false;
        }

        boolean changed = false;
        UUID previousHand = capital.getHand();

        if (!CapitalHandSelection.isValidHand(level, capital, previousHand, residents)) {
            if (previousHand != null) {
                capital.setHand(null);
                capital.setHandFemale(false);
                changed = true;
            }
        }

        if (capital.getHand() == null
                && !capital.isPlayerSovereign()
                && capital.getSovereign() != null
                && CapitalHandSelection.isEligibleForNewHand(level, capital)) {
            UUID newHand = CapitalHandSelection.findBestHandCandidate(level, capital, residents);
            if (newHand != null) {
                capital.setHand(newHand);
                capital.setHandFemale(MCAIntegrationBridge.isFemale(level, newHand));

                String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
                String officeName = capital.isSovereignFemale() ? "Hand of the Queen" : "Hand of the King";
                String handName = resolveName(level, newHand);

                CapitalChronicleService.addEntry(
                        level,
                        capital,
                        handName + " was appointed " + officeName + " of " + villageName + "."
                );
                changed = true;
            }
        }

        if (previousHand != null && capital.getHand() == null && !capital.isPlayerSovereign()) {
            String officeName = capital.isSovereignFemale() ? "Hand of the Queen" : "Hand of the King";
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    "The office of " + officeName + " stands vacant in "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
            );
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static boolean isEligibleHandCandidate(ServerLevel level, CapitalRecord capital, UUID candidateId, Set<UUID> residents) {
        return CapitalHandSelection.isEligibleHandCandidate(level, capital, candidateId, residents);
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }
}