package com.example.mcacapitals.capital;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CapitalManager {

    private static final Map<UUID, CapitalRecord> CAPITALS = new LinkedHashMap<>();

    private CapitalManager() {
    }

    public static void clear() {
        CAPITALS.clear();
    }

    public static void clearAll() {
        CAPITALS.clear();
    }

    public static boolean isEmpty() {
        return CAPITALS.isEmpty();
    }

    public static void putCapital(CapitalRecord capital) {
        if (capital == null || capital.getCapitalId() == null) {
            return;
        }
        CAPITALS.put(capital.getCapitalId(), capital);
    }

    public static CapitalRecord getCapital(UUID capitalId) {
        if (capitalId == null) {
            return null;
        }
        return CAPITALS.get(capitalId);
    }

    public static void removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return;
        }
        CAPITALS.remove(capitalId);
    }

    public static Map<UUID, CapitalRecord> getAllCapitals() {
        return Collections.unmodifiableMap(CAPITALS);
    }

    public static Map<UUID, CapitalRecord> getAllCapitalsSnapshot() {
        return new LinkedHashMap<>(CAPITALS);
    }

    public static Collection<CapitalRecord> getAllCapitalRecords() {
        return Collections.unmodifiableCollection(new ArrayList<>(CAPITALS.values()));
    }

    public static boolean hasCapitalForVillageId(Integer villageId) {
        return getCapitalByVillageId(villageId) != null;
    }

    public static CapitalRecord getCapitalForVillage(Integer villageId) {
        return getCapitalByVillageId(villageId);
    }

    public static CapitalRecord getCapitalByVillageId(Integer villageId) {
        if (villageId == null) {
            return null;
        }

        for (CapitalRecord capital : CAPITALS.values()) {
            if (villageId.equals(capital.getVillageId())) {
                return capital;
            }
        }

        return null;
    }

    public static CapitalRecord getCapitalBySovereign(UUID sovereignId) {
        if (sovereignId == null) {
            return null;
        }

        for (CapitalRecord capital : CAPITALS.values()) {
            if (sovereignId.equals(capital.getSovereign())
                    || sovereignId.equals(capital.getPlayerSovereignId())) {
                return capital;
            }
        }

        return null;
    }

    public static CapitalRecord getCapitalForResident(UUID residentId) {
        if (residentId == null) {
            return null;
        }

        for (CapitalRecord capital : CAPITALS.values()) {
            if (belongsToCapital(capital, residentId)) {
                return capital;
            }
        }

        return null;
    }

    private static boolean belongsToCapital(CapitalRecord capital, UUID residentId) {
        if (capital == null || residentId == null) {
            return false;
        }

        return residentId.equals(capital.getSovereign())
                || residentId.equals(capital.getConsort())
                || residentId.equals(capital.getDowager())
                || residentId.equals(capital.getHeir())
                || residentId.equals(capital.getCommander())
                || residentId.equals(capital.getPlayerSovereignId())
                || residentId.equals(capital.getPlayerConsortId())
                || capital.isRoyalChild(residentId)
                || capital.isDisinheritedRoyalChild(residentId)
                || capital.isLegitimizedRoyalChild(residentId)
                || capital.isRoyalHouseholdMember(residentId)
                || capital.isDuke(residentId)
                || capital.isLord(residentId)
                || capital.isKnight(residentId)
                || capital.isRoyalGuard(residentId)
                || capital.isDisgracedRoyalGuard(residentId);
    }
}