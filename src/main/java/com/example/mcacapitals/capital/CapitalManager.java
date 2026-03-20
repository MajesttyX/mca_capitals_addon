package com.example.mcacapitals.capital;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CapitalManager {

    private static final Map<UUID, CapitalRecord> CAPITALS = new LinkedHashMap<>();

    private CapitalManager() {
    }

    public static CapitalRecord createCapital(UUID capitalId, UUID sovereignId, boolean sovereignFemale) {
        CapitalRecord record = new CapitalRecord(capitalId, sovereignId, sovereignFemale);
        CAPITALS.put(capitalId, record);
        return record;
    }

    public static CapitalRecord createCapital(UUID capitalId, Integer villageId, UUID sovereignId, boolean sovereignFemale) {
        CapitalRecord record = new CapitalRecord(capitalId, villageId, sovereignId, sovereignFemale);
        CAPITALS.put(capitalId, record);
        return record;
    }

    public static Map<UUID, CapitalRecord> getAllCapitals() {
        return CAPITALS;
    }

    public static CapitalRecord getCapital(UUID capitalId) {
        return CAPITALS.get(capitalId);
    }

    public static CapitalRecord getCapitalBySovereign(UUID sovereignId) {
        for (CapitalRecord capital : CAPITALS.values()) {
            if (capital.getSovereign() != null && capital.getSovereign().equals(sovereignId)) {
                return capital;
            }
        }
        return null;
    }

    public static CapitalRecord getCapitalByVillageId(int villageId) {
        for (CapitalRecord capital : CAPITALS.values()) {
            if (capital.getVillageId() != null && capital.getVillageId() == villageId) {
                return capital;
            }
        }
        return null;
    }

    public static boolean hasCapitalForSovereign(UUID sovereignId) {
        return getCapitalBySovereign(sovereignId) != null;
    }

    public static boolean hasCapitalForVillageId(int villageId) {
        return getCapitalByVillageId(villageId) != null;
    }

    public static boolean hasCapitalId(UUID capitalId) {
        return CAPITALS.containsKey(capitalId);
    }

    public static void removeCapital(UUID capitalId) {
        CAPITALS.remove(capitalId);
    }

    public static void clearAll() {
        CAPITALS.clear();
    }
}