package com.example.mcacapitals.capital;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CapitalManager {

    private static final Map<UUID, CapitalRecord> CAPITALS = new LinkedHashMap<>();

    private CapitalManager() {
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

    public static boolean hasCapitalForVillageId(Integer villageId) {
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