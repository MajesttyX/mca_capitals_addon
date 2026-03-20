package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;

public class CapitalTitleResolver {

    private CapitalTitleResolver() {
    }

    public static String getDisplayTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return "None";
        }

        boolean female = isFemaleForTitle(level, capital, entityId);

        if (entityId.equals(capital.getDowager())) {
            return female ? "Queen Dowager" : "Prince Consort";
        }

        if (entityId.equals(capital.getSovereign())) {
            return female ? "Queen" : "King";
        }

        if (entityId.equals(capital.getConsort())) {
            return female ? "Queen Consort" : "King Consort";
        }

        if (capital.isRoyalChild(entityId)) {
            return female ? "Princess" : "Prince";
        }

        if (capital.isDuke(entityId)) {
            return female ? "Duchess" : "Duke";
        }

        if (capital.isLord(entityId)) {
            return female ? "Lady" : "Lord";
        }

        if (capital.isKnight(entityId)) {
            return female ? "Dame" : "Sir";
        }

        return "Commoner";
    }

    public static CapitalRecord findCapitalForEntity(UUID entityId) {
        if (entityId == null) {
            return null;
        }

        for (Map.Entry<UUID, CapitalRecord> entry : CapitalManager.getAllCapitals().entrySet()) {
            CapitalRecord capital = entry.getValue();
            if (capital.containsEntity(entityId)) {
                return capital;
            }
        }

        return null;
    }

    public static String getDisplayTitleForEntity(ServerLevel level, UUID entityId) {
        CapitalRecord capital = findCapitalForEntity(entityId);
        if (capital == null) {
            return "Commoner";
        }

        return getDisplayTitle(level, capital, entityId);
    }

    private static boolean isFemaleForTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return false;
        }

        if (entityId.equals(capital.getDowager())) {
            return capital.isDowagerFemale();
        }
        if (entityId.equals(capital.getSovereign())) {
            return capital.isSovereignFemale();
        }
        if (entityId.equals(capital.getConsort())) {
            return capital.isConsortFemale();
        }
        if (capital.isRoyalChild(entityId)) {
            return capital.isRoyalChildFemale(entityId);
        }
        if (capital.isDuke(entityId)) {
            return capital.isDukeFemale(entityId);
        }
        if (capital.isLord(entityId)) {
            return capital.isLordFemale(entityId);
        }
        if (capital.isKnight(entityId)) {
            return capital.isKnightFemale(entityId);
        }

        return level != null && MCAIntegrationBridge.isFemale(level, entityId);
    }
}