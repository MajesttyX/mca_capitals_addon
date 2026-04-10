package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalCourtApplier {

    private CapitalCourtApplier() {
    }

    static void applyComputedCourt(
            ServerLevel level,
            CapitalRecord capital,
            UUID newConsort,
            boolean newConsortFemale,
            UUID newHeir,
            Set<UUID> newRoyalChildren,
            Map<UUID, Boolean> newRoyalChildFemale,
            Set<UUID> newDukes,
            Map<UUID, Boolean> newDukeFemale,
            Set<UUID> newLords,
            Map<UUID, Boolean> newLordFemale,
            Set<UUID> newKnights,
            Map<UUID, Boolean> newKnightFemale
    ) {
        capital.setConsort(newConsort);
        capital.setConsortFemale(newConsortFemale);

        if (newConsort != null && !MCAIntegrationBridge.isMCAVillager(level, newConsort)) {
            capital.setPlayerConsort(true);
            capital.setPlayerConsortId(newConsort);
            capital.setPlayerConsortName(CapitalFoundationInternal.resolvePlayerName(level, newConsort));
        } else {
            capital.setPlayerConsort(false);
            capital.setPlayerConsortId(null);
            capital.setPlayerConsortName(null);
        }

        capital.getRoyalChildren().clear();
        capital.getRoyalChildFemale().clear();
        for (UUID childId : newRoyalChildren) {
            capital.getRoyalChildren().add(childId);
            capital.getRoyalChildFemale().put(childId, newRoyalChildFemale.getOrDefault(childId, false));
        }

        capital.getDukes().clear();
        capital.getDukeFemale().clear();
        for (UUID dukeId : newDukes) {
            capital.getDukes().add(dukeId);
            capital.getDukeFemale().put(dukeId, newDukeFemale.getOrDefault(dukeId, false));
        }

        capital.getLords().clear();
        capital.getLordFemale().clear();
        for (UUID lordId : newLords) {
            capital.getLords().add(lordId);
            capital.getLordFemale().put(lordId, newLordFemale.getOrDefault(lordId, false));
        }

        capital.getKnights().clear();
        capital.getKnightFemale().clear();
        for (UUID knightId : newKnights) {
            capital.getKnights().add(knightId);
            capital.getKnightFemale().put(knightId, newKnightFemale.getOrDefault(knightId, false));
        }

        capital.setHeir(newHeir);
        if (newHeir != null) {
            boolean heirFemale = newRoyalChildFemale.getOrDefault(newHeir, MCAIntegrationBridge.isFemale(level, newHeir));
            capital.setHeirFemale(heirFemale);
        } else {
            capital.setHeirFemale(false);
        }

        if (capital.getSovereign() != null) {
            capital.setState(CapitalState.ACTIVE);
        }
    }
}