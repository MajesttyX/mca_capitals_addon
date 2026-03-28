package com.example.mcacapitals.capital;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CapitalManager {

    private static final Map<UUID, CapitalRecord> CAPITALS = new LinkedHashMap<>();

    private CapitalManager() {
    }

    public static Map<UUID, CapitalRecord> getAllCapitals() {
        return CAPITALS;
    }

    public static Map<UUID, CapitalRecord> getAllCapitalsView() {
        return Collections.unmodifiableMap(CAPITALS);
    }

    public static Map<UUID, CapitalRecord> getAllCapitalsSnapshot() {
        return new LinkedHashMap<>(CAPITALS);
    }

    public static Collection<CapitalRecord> getAllCapitalRecords() {
        return Collections.unmodifiableCollection(CAPITALS.values());
    }

    public static CapitalRecord getCapital(UUID capitalId) {
        return capitalId == null ? null : CAPITALS.get(capitalId);
    }

    public static boolean containsCapital(UUID capitalId) {
        return capitalId != null && CAPITALS.containsKey(capitalId);
    }

    public static CapitalRecord putCapital(CapitalRecord capital) {
        if (capital == null || capital.getCapitalId() == null) {
            return null;
        }
        return CAPITALS.put(capital.getCapitalId(), capital);
    }

    public static CapitalRecord putCapital(UUID capitalId, CapitalRecord capital) {
        if (capitalId == null || capital == null) {
            return null;
        }
        return CAPITALS.put(capitalId, capital);
    }

    public static CapitalRecord removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return null;
        }
        return CAPITALS.remove(capitalId);
    }

    public static void replaceAll(Map<UUID, CapitalRecord> capitals) {
        CAPITALS.clear();
        if (capitals != null && !capitals.isEmpty()) {
            CAPITALS.putAll(capitals);
        }
    }

    public static void clear() {
        CAPITALS.clear();
    }

    public static void clearAll() {
        clear();
    }

    public static boolean isEmpty() {
        return CAPITALS.isEmpty();
    }

    public static int size() {
        return CAPITALS.size();
    }

    public static boolean hasCapitalForVillageId(int villageId) {
        for (CapitalRecord capital : CAPITALS.values()) {
            Integer existingVillageId = capital.getVillageId();
            if (existingVillageId != null && existingVillageId == villageId) {
                return true;
            }
        }
        return false;
    }

    public static CapitalRecord getCapitalForVillageId(int villageId) {
        for (CapitalRecord capital : CAPITALS.values()) {
            Integer existingVillageId = capital.getVillageId();
            if (existingVillageId != null && existingVillageId == villageId) {
                return capital;
            }
        }
        return null;
    }

    public static CapitalRecord getCapitalByVillageId(Integer villageId) {
        if (villageId == null) {
            return null;
        }
        return getCapitalForVillageId(villageId);
    }

    public static CapitalRecord getCapitalForSovereign(UUID sovereignId) {
        if (sovereignId == null) {
            return null;
        }

        for (CapitalRecord capital : CAPITALS.values()) {
            if (sovereignId.equals(capital.getSovereign())) {
                return capital;
            }
        }
        return null;
    }

    public static CapitalRecord getCapitalBySovereign(UUID sovereignId) {
        return getCapitalForSovereign(sovereignId);
    }

    public static CapitalRecord getCapitalForResident(UUID residentId) {
        if (residentId == null) {
            return null;
        }

        for (CapitalRecord capital : CAPITALS.values()) {
            if (residentId.equals(capital.getSovereign())
                    || residentId.equals(capital.getConsort())
                    || residentId.equals(capital.getDowager())
                    || residentId.equals(capital.getHeir())
                    || capital.getRoyalChildren().contains(residentId)
                    || capital.getDukes().contains(residentId)
                    || capital.getLords().contains(residentId)
                    || capital.getKnights().contains(residentId)
                    || residentId.equals(capital.getCommander())
                    || capital.getRoyalGuards().contains(residentId)
                    || capital.getDisgracedRoyalGuards().contains(residentId)) {
                return capital;
            }
        }
        return null;
    }
}