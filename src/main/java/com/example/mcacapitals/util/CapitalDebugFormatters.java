package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalResidentScanner;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.UUID;

public final class CapitalDebugFormatters {

    private CapitalDebugFormatters() {
    }

    public static String describe(ServerLevel level, UUID id) {
        if (id == null) {
            return "null";
        }

        return MCAIntegrationBridge.describeEntity(level, id);
    }

    public static String formatUuidCollection(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return "[]";
        }
        return ids.toString();
    }

    public static String describeHeirMode(ServerLevel level, CapitalRecord capital) {
        UUID heir = capital.getHeir();
        if (heir == null) {
            return "none";
        }

        UUID expectedDynasticHeir = firstDynasticHeir(level, capital);
        if (expectedDynasticHeir == null) {
            return "manual";
        }

        if (heir.equals(expectedDynasticHeir)) {
            return "dynastic";
        }

        return "manual";
    }

    public static UUID firstDynasticHeir(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return null;
        }

        var residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(sovereign) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.getRoyalChildren().contains(childId)) {
                continue;
            }
            if (residents.contains(childId) && MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(sovereign) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.getRoyalChildren().contains(childId)) {
                continue;
            }
            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        return null;
    }
}