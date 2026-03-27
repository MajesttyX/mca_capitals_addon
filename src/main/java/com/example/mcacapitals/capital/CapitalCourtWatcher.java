package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CapitalCourtWatcher {

    private static final Map<UUID, String> CAPITAL_FINGERPRINTS = new HashMap<>();
    private static final Map<UUID, Map<UUID, UUID>> ROYAL_SPOUSE_SNAPSHOTS = new HashMap<>();

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

        UUID oldConsort = capital.getConsort();
        UUID oldDowager = capital.getDowager();
        UUID oldHeir = capital.getHeir();

        recordRoyalMarriageEntries(level, capital, residents);

        CAPITAL_FINGERPRINTS.put(capital.getCapitalId(), newFingerprint);

        if (capital.getSovereign() != null) {
            UUID sovereignSpouse = MCAIntegrationBridge.getSpouse(level, capital.getSovereign());
            if (!Objects.equals(sovereignSpouse, capital.getConsort())) {
                CapitalCourtBuilder.applySovereignMarriage(level, capital);
            }

            if (oldConsort != null && capital.getConsort() == null && isConfirmedDead(level, oldConsort)) {
                String name = resolveDisplayName(level, capital, oldConsort);
                CapitalMourningService.startMourning(level, capital, name + " died.");
                CapitalChronicleService.addEntry(level, capital, name + " died and the court entered mourning.");
            }

            if (oldDowager != null && isConfirmedDead(level, oldDowager)) {
                String name = resolveDisplayName(level, capital, oldDowager);
                capital.setDowager(null);
                capital.setDowagerFemale(false);
                CapitalMourningService.startMourning(level, capital, name + " died.");
                CapitalChronicleService.addEntry(level, capital, name + " died and the court entered mourning.");
            }

            if (!CapitalSuccessionService.isHeirStillValid(level, capital)) {
                if (oldHeir != null && isConfirmedDead(level, oldHeir)) {
                    String name = resolveDisplayName(level, capital, oldHeir);
                    CapitalMourningService.startMourning(level, capital, name + " died.");
                    CapitalChronicleService.addEntry(level, capital, name + " died and the court entered mourning.");
                }
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
        ROYAL_SPOUSE_SNAPSHOTS.remove(capitalId);
    }

    public static void clearAllFingerprints() {
        CAPITAL_FINGERPRINTS.clear();
        ROYAL_SPOUSE_SNAPSHOTS.clear();
    }

    private static void recordRoyalMarriageEntries(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null) {
            return;
        }

        Set<UUID> trackedNobles = new HashSet<>();
        if (capital.getSovereign() != null) {
            trackedNobles.add(capital.getSovereign());
        }
        trackedNobles.addAll(capital.getRoyalChildren());
        trackedNobles.addAll(capital.getDukes());
        trackedNobles.addAll(capital.getLords());
        trackedNobles.addAll(capital.getKnights());

        Map<UUID, UUID> previousSnapshot = ROYAL_SPOUSE_SNAPSHOTS.get(capital.getCapitalId());
        Map<UUID, UUID> currentSnapshot = new HashMap<>();

        for (UUID nobleId : trackedNobles) {
            if (nobleId == null) {
                continue;
            }

            UUID currentSpouse = MCAIntegrationBridge.getSpouse(level, nobleId);
            currentSnapshot.put(nobleId, currentSpouse);

            if (previousSnapshot == null) {
                continue;
            }

            UUID previousSpouse = previousSnapshot.get(nobleId);
            if (Objects.equals(previousSpouse, currentSpouse)) {
                continue;
            }

            if (currentSpouse == null || !residents.contains(currentSpouse)) {
                continue;
            }

            String nobleName = resolveDisplayName(level, capital, nobleId);
            String spouseName = resolveDisplayName(level, capital, currentSpouse);

            CapitalChronicleService.addEntry(level, capital,
                    nobleName + " was married to " + spouseName + ".");
        }

        ROYAL_SPOUSE_SNAPSHOTS.put(capital.getCapitalId(), currentSnapshot);
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
        sb.append("mourningActive=").append(capital.isMourningActive()).append('|');
        sb.append("mourningEndDay=").append(capital.getMourningEndDay()).append('|');
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
            sb.append("title=").append(CapitalTitleResolver.getDisplayTitle(level, capital, entityId)).append(',');
            sb.append('|');
        }

        return sb.toString();
    }

    private static boolean isConfirmedDead(ServerLevel level, UUID id) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        return entity != null && (!entity.isAlive() || entity.isRemoved());
    }

    private static String resolveDisplayName(ServerLevel level, CapitalRecord capital, UUID id) {
        if (id == null) {
            return "Unknown";
        }

        String baseName = stripKnownTitles(resolveBaseName(level, id));
        String title = CapitalTitleResolver.getDisplayTitle(level, capital, id);

        if (title == null || title.isBlank() || "Commoner".equalsIgnoreCase(title) || "None".equalsIgnoreCase(title)) {
            return baseName;
        }

        return title + " " + baseName;
    }

    private static String resolveBaseName(ServerLevel level, UUID id) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        return entity != null ? entity.getName().getString() : id.toString();
    }

    private static String stripKnownTitles(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();
        String[] knownTitles = {
                "Queen Dowager",
                "Prince Consort",
                "Queen Consort",
                "King Consort",
                "Heir Apparent",
                "Princess",
                "Prince",
                "Duchess",
                "Duke",
                "Lady",
                "Lord",
                "Dame",
                "Sir",
                "Queen",
                "King"
        };

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String title : knownTitles) {
                String prefix = title + " ";
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }

        return result.isBlank() ? "Unnamed" : result;
    }
}