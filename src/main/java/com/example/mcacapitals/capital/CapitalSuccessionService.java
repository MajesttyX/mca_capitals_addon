package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public class CapitalSuccessionService {

    private CapitalSuccessionService() {
    }

    public static boolean handleSuccessionIfNeeded(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return false;
        }

        boolean sovereignValid = MCAIntegrationBridge.isMCAVillager(level, sovereign)
                && MCAIntegrationBridge.isAliveAdultOrChildVillager(level, sovereign);

        if (sovereignValid) {
            return false;
        }

        UUID oldConsort = capital.getConsort();
        boolean oldConsortFemale = capital.isConsortFemale();

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        UUID successor = findSuccessor(capital, residents);

        if (successor == null) {
            capital.setSovereign(null);
            capital.setSovereignFemale(false);
            capital.setConsort(null);
            capital.setConsortFemale(false);
            capital.setHeir(null);
            capital.setState(CapitalState.PENDING);

            if (isValidLivingEntity(level, oldConsort)) {
                capital.setDowager(oldConsort);
                capital.setDowagerFemale(oldConsortFemale);
            } else {
                capital.setDowager(null);
                capital.setDowagerFemale(false);
            }

            CapitalDataAccess.markDirty(level);
            return true;
        }

        capital.setSovereign(successor);
        capital.setSovereignFemale(MCAIntegrationBridge.isFemale(level, successor));

        if (isValidLivingEntity(level, oldConsort) && !oldConsort.equals(successor)) {
            capital.setDowager(oldConsort);
            capital.setDowagerFemale(oldConsortFemale);
        } else {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }

        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setState(CapitalState.FOUNDED);

        CapitalFoundationService.refreshCourt(level, capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        return true;
    }

    private static UUID findSuccessor(CapitalRecord capital, Set<UUID> residents) {
        UUID heir = capital.getHeir();
        if (heir != null) {
            return heir;
        }

        UUID royalChild = capital.getRoyalChildren().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .findFirst()
                .orElse(null);
        if (royalChild != null) {
            return royalChild;
        }

        UUID duke = capital.getDukes().stream()
                .filter(residents::contains)
                .sorted(Comparator.comparing(UUID::toString))
                .findFirst()
                .orElse(null);
        if (duke != null) {
            return duke;
        }

        UUID lord = capital.getLords().stream()
                .filter(residents::contains)
                .sorted(Comparator.comparing(UUID::toString))
                .findFirst()
                .orElse(null);
        if (lord != null) {
            return lord;
        }

        return capital.getKnights().stream()
                .filter(residents::contains)
                .sorted(Comparator.comparing(UUID::toString))
                .findFirst()
                .orElse(null);
    }

    private static boolean isValidLivingEntity(ServerLevel level, UUID entityId) {
        return MCAIntegrationBridge.isAliveAdultOrChildVillager(level, entityId);
    }
}