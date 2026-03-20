package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CapitalCourtWatcher {

    private static final Map<UUID, String> CAPITAL_FINGERPRINTS = new HashMap<>();

    private CapitalCourtWatcher() {
    }

    public static boolean refreshIfChanged(ServerLevel level, CapitalRecord capital) {
        if (capital.getSovereign() == null) {
            return false;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        UUID sovereign = capital.getSovereign();
        UUID sovereignSpouse = MCAIntegrationBridge.getSpouse(level, sovereign);
        UUID currentConsort = capital.getConsort();

        if (!Objects.equals(sovereignSpouse, currentConsort)) {
            CapitalCourtBuilder.applySovereignMarriage(level, capital);
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CAPITAL_FINGERPRINTS.put(capital.getCapitalId(), buildFingerprint(level, capital, residents));
            CapitalDataAccess.markDirty(level);
            return true;
        }

        if (capital.getDowager() != null) {
            if (capital.getDowager().equals(capital.getSovereign())
                    || capital.getDowager().equals(capital.getConsort())) {
                CapitalFoundationService.refreshCourt(level, capital);
                CAPITAL_FINGERPRINTS.put(capital.getCapitalId(), buildFingerprint(level, capital, residents));
                CapitalDataAccess.markDirty(level);
                return true;
            }
        }

        String newFingerprint = buildFingerprint(level, capital, residents);
        String oldFingerprint = CAPITAL_FINGERPRINTS.get(capital.getCapitalId());

        if (!newFingerprint.equals(oldFingerprint)) {
            CAPITAL_FINGERPRINTS.put(capital.getCapitalId(), newFingerprint);
            CapitalFoundationService.refreshCourt(level, capital);
            return true;
        }

        return false;
    }

    public static void clearFingerprint(UUID capitalId) {
        CAPITAL_FINGERPRINTS.remove(capitalId);
    }

    private static String buildFingerprint(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        StringBuilder sb = new StringBuilder();

        sb.append("capital=").append(capital.getCapitalId()).append('|');
        sb.append("villageId=").append(capital.getVillageId()).append('|');
        sb.append("state=").append(capital.getState()).append('|');
        sb.append("sovereign=").append(capital.getSovereign()).append('|');
        sb.append("consort=").append(capital.getConsort()).append('|');
        sb.append("dowager=").append(capital.getDowager()).append('|');
        sb.append("heir=").append(capital.getHeir()).append('|');

        Set<UUID> watchSet = new HashSet<>(residents);

        if (capital.getSovereign() != null) {
            watchSet.add(capital.getSovereign());
        }
        if (capital.getConsort() != null) {
            watchSet.add(capital.getConsort());
        }
        if (capital.getDowager() != null) {
            watchSet.add(capital.getDowager());
        }
        if (capital.getHeir() != null) {
            watchSet.add(capital.getHeir());
        }

        watchSet.addAll(capital.getRoyalChildren());
        watchSet.addAll(capital.getDukes());
        watchSet.addAll(capital.getLords());
        watchSet.addAll(capital.getKnights());

        for (UUID entityId : watchSet.stream().sorted().toList()) {
            sb.append(entityId).append(':');
            sb.append("resident=").append(residents.contains(entityId)).append(',');
            sb.append("isMCA=").append(MCAIntegrationBridge.isMCAVillager(level, entityId)).append(',');
            sb.append("hasFamilyNode=").append(MCAIntegrationBridge.hasFamilyNode(level, entityId)).append(',');
            sb.append("isFemale=").append(MCAIntegrationBridge.isFemale(level, entityId)).append(',');
            sb.append("isAlive=").append(MCAIntegrationBridge.isAliveAdultOrChildVillager(level, entityId)).append(',');
            sb.append("isGuard=").append(MCAIntegrationBridge.isMCAGuard(level, entityId)).append(',');
            sb.append("isMaster=").append(MCAIntegrationBridge.isMasterProfessionVillager(level, entityId)).append(',');

            UUID spouse = MCAIntegrationBridge.getSpouse(level, entityId);
            sb.append("spouse=").append(spouse == null ? "none" : spouse).append(',');

            sb.append("childCount=").append(MCAIntegrationBridge.getChildren(level, entityId).size()).append(',');
            sb.append("profession=").append(MCAIntegrationBridge.describeProfession(level, entityId)).append(',');
            sb.append('|');
        }

        return sb.toString();
    }
}