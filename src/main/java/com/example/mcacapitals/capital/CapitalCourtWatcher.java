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
        if (capital == null) {
            return false;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        String newFingerprint = buildFingerprint(level, capital, residents);
        String oldFingerprint = CAPITAL_FINGERPRINTS.get(capital.getCapitalId());

        if (Objects.equals(newFingerprint, oldFingerprint)) {
            return false;
        }

        CAPITAL_FINGERPRINTS.put(capital.getCapitalId(), newFingerprint);

        if (capital.getSovereign() != null) {
            UUID oldConsort = capital.getConsort();
            UUID sovereignSpouse = MCAIntegrationBridge.getSpouse(level, capital.getSovereign());

            if (!Objects.equals(sovereignSpouse, oldConsort)) {
                CapitalCourtBuilder.applySovereignMarriage(level, capital);

                UUID newConsort = capital.getConsort();
                if (newConsort != null) {
                    String spouseName = MCAIntegrationBridge.getEntityByUuid(level, newConsort) != null
                            ? MCAIntegrationBridge.getEntityByUuid(level, newConsort).getName().getString()
                            : newConsort.toString();

                    CapitalChronicleService.addEntry(level, capital,
                            spouseName + " became consort of "
                                    + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
                } else if (oldConsort != null) {
                    String oldName = MCAIntegrationBridge.getEntityByUuid(level, oldConsort) != null
                            ? MCAIntegrationBridge.getEntityByUuid(level, oldConsort).getName().getString()
                            : oldConsort.toString();

                    CapitalChronicleService.addEntry(level, capital,
                            oldName + " ceased to be consort of "
                                    + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
                }
            }

            if (!CapitalSuccessionService.isHeirStillValid(level, capital)) {
                capital.setHeir(null);
            }

            CapitalFoundationService.refreshCourt(level, capital);
            CapitalDataAccess.markDirty(level);
            return true;
        }

        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalDataAccess.markDirty(level);
        return true;
    }

    public static void clearFingerprint(UUID capitalId) {
        CAPITAL_FINGERPRINTS.remove(capitalId);
    }

    public static void clearAllFingerprints() {
        CAPITAL_FINGERPRINTS.clear();
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
        sb.append("royalOrder=").append(capital.getRoyalSuccessionOrder()).append('|');
        sb.append("disinherited=").append(capital.getDisinheritedRoyalChildren()).append('|');
        sb.append("legitimized=").append(capital.getLegitimizedRoyalChildren()).append('|');

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