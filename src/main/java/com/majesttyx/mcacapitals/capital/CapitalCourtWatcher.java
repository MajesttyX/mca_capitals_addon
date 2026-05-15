package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        return refreshIfChanged(level, capital, CapitalResidentScanner.scanResidents(level, capital.getCapitalId()));
    }

    public static boolean refreshIfChanged(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (capital == null) {
            return false;
        }

        Set<UUID> resolvedResidents = residents != null ? residents : CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        String newFingerprint = buildFingerprint(level, capital, resolvedResidents);
        String oldFingerprint = CAPITAL_FINGERPRINTS.get(capital.getCapitalId());

        if (Objects.equals(newFingerprint, oldFingerprint)) {
            return false;
        }

        UUID oldConsort = capital.getConsort();
        UUID oldDowager = capital.getDowager();
        UUID oldHeir = capital.getHeir();

        cleanupSubordinateDowagers(level, capital);
        recordRoyalMarriageEntries(level, capital, resolvedResidents);

        CAPITAL_FINGERPRINTS.put(capital.getCapitalId(), newFingerprint);

        if (capital.getSovereign() != null) {
            UUID sovereignSpouse = CapitalCourtMarriageResolver.findActualSpouse(level, capital.getSovereign());
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

        CapitalNameService.refreshCapitalNames(level, capital, resolvedResidents);
        CapitalDataAccess.markDirty(level);
        return true;
    }

    public static void seedCurrentState(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getCapitalId() == null) {
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CAPITAL_FINGERPRINTS.put(capital.getCapitalId(), buildFingerprint(level, capital, residents));
        ROYAL_SPOUSE_SNAPSHOTS.put(capital.getCapitalId(), buildRoyalSpouseSnapshot(level, capital));
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

        Map<UUID, UUID> previousSnapshot = ROYAL_SPOUSE_SNAPSHOTS.get(capital.getCapitalId());
        Map<UUID, UUID> currentSnapshot = buildRoyalSpouseSnapshot(level, capital);

        for (Map.Entry<UUID, UUID> entry : currentSnapshot.entrySet()) {
            UUID nobleId = entry.getKey();
            UUID currentSpouse = entry.getValue();

            if (previousSnapshot == null) {
                continue;
            }

            UUID previousSpouse = previousSnapshot.get(nobleId);
            if (Objects.equals(previousSpouse, currentSpouse)) {
                continue;
            }

            if (currentSpouse == null) {
                continue;
            }

            boolean spouseIsResident = residents != null && residents.contains(currentSpouse);
            boolean spouseIsPlayer = !MCAIntegrationBridge.isMCAVillager(level, currentSpouse);

            if (!spouseIsResident && !spouseIsPlayer) {
                continue;
            }

            if (!CapitalCourtMarriageResolver.isValidMarriedConsort(level, nobleId, currentSpouse)) {
                continue;
            }

            String nobleName = stripKnownTitles(resolveBaseName(level, capital, nobleId));
            String spouseName = stripKnownTitles(CapitalCourtMarriageResolver.resolveSpouseName(level, nobleId));

            if (!hasMarriageEntry(capital, nobleName, spouseName)) {
                CapitalChronicleService.addEntry(level, capital,
                        nobleName + " was married to " + spouseName + ".");
            }
        }

        ROYAL_SPOUSE_SNAPSHOTS.put(capital.getCapitalId(), currentSnapshot);
    }

    private static Map<UUID, UUID> buildRoyalSpouseSnapshot(ServerLevel level, CapitalRecord capital) {
        Map<UUID, UUID> snapshot = new HashMap<>();
        if (level == null || capital == null) {
            return snapshot;
        }

        Set<UUID> trackedNobles = new HashSet<>();
        trackedNobles.addAll(capital.getRoyalChildren());
        trackedNobles.addAll(capital.getDukes());
        trackedNobles.addAll(capital.getLords());
        trackedNobles.addAll(capital.getKnights());

        for (UUID nobleId : trackedNobles) {
            if (nobleId == null) {
                continue;
            }
            snapshot.put(nobleId, CapitalCourtMarriageResolver.findActualSpouse(level, nobleId));
        }

        return snapshot;
    }

    private static void cleanupSubordinateDowagers(ServerLevel level, CapitalRecord capital) {
        for (UUID holder : new HashSet<>(capital.getDowagerPrinceSources().keySet())) {
            if (holder == null || isConfirmedDead(level, holder)) {
                capital.removeDowagerPrinceSource(holder);
                continue;
            }

            UUID actualLivingSpouse = CapitalCourtMarriageResolver.findActualSpouse(level, holder);
            if (actualLivingSpouse != null && !actualLivingSpouse.equals(capital.getDowagerPrinceSource(holder))) {
                capital.removeDowagerPrinceSource(holder);
            }
        }

        for (UUID holder : new HashSet<>(capital.getDowagerDukeSources().keySet())) {
            if (holder == null || isConfirmedDead(level, holder)) {
                capital.removeDowagerDukeSource(holder);
                continue;
            }

            UUID actualLivingSpouse = CapitalCourtMarriageResolver.findActualSpouse(level, holder);
            if (actualLivingSpouse != null && !actualLivingSpouse.equals(capital.getDowagerDukeSource(holder))) {
                capital.removeDowagerDukeSource(holder);
            }
        }
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
        sb.append("princeConsortSources=").append(capital.getPrinceConsortSources()).append('|');
        sb.append("marriageDukeSources=").append(capital.getMarriageDukeSources()).append('|');
        sb.append("dowagerPrinceSources=").append(capital.getDowagerPrinceSources()).append('|');
        sb.append("dowagerDukeSources=").append(capital.getDowagerDukeSources()).append('|');

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
        watchSet.addAll(capital.getPrinceConsortSources().keySet());
        watchSet.addAll(capital.getDowagerPrinceSources().keySet());
        watchSet.addAll(capital.getDukes());
        watchSet.addAll(capital.getMarriageDukeSources().keySet());
        watchSet.addAll(capital.getDowagerDukeSources().keySet());
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

            UUID spouse = CapitalCourtMarriageResolver.findActualSpouse(level, entityId);
            sb.append("spouse=").append(spouse == null ? "none" : spouse).append(',');
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

        String baseName = stripKnownTitles(resolveBaseName(level, capital, id));
        String title = CapitalTitleResolver.getDisplayTitleForEntity(level, id);

        if (title == null || title.isBlank() || "Commoner".equalsIgnoreCase(title) || "None".equalsIgnoreCase(title)) {
            return baseName;
        }

        CapitalRecord sourceCapital = CapitalTitleResolver.findCapitalForEntity(level, id);
        if (sourceCapital != null && sourceCapital.isRoyalGuard(id) && ("Sir".equals(title) || "Dame".equals(title))) {
            String suffix = sourceCapital.isSovereignFemale() ? " of the Queensguard" : " of the Kingsguard";
            return title + " " + baseName + suffix;
        }

        return title + " " + baseName;
    }

    private static String resolveBaseName(ServerLevel level, CapitalRecord capital, UUID id) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        if (entity != null) {
            String name = entity.getName().getString();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }

        if (capital != null && capital.isPlayerConsort() && id.equals(capital.getPlayerConsortId())) {
            String storedName = capital.getPlayerConsortName();
            if (storedName != null && !storedName.isBlank()) {
                return storedName;
            }
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        if (player != null) {
            String profileName = player.getGameProfile().getName();
            if (profileName != null && !profileName.isBlank()) {
                return profileName;
            }
        }

        return "Unknown";
    }

    private static boolean hasMarriageEntry(CapitalRecord capital, String nobleName, String spouseName) {
        String needle = nobleName + " was married to " + spouseName + ".";
        for (String entry : capital.getChronicleEntries()) {
            if (needle.equals(entry) || entry.endsWith(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String stripKnownTitles(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();
        String[] knownTitles = {
                "High Queen",
                "High King",
                "Dowager Queen",
                "Dowager King",
                "Queen Consort",
                "King Consort",
                "Heir Apparent",
                "Crown Princess",
                "Crown Prince",
                "Dowager Princess",
                "Dowager Prince",
                "Princess Consort",
                "Prince Consort",
                "Princess",
                "Prince",
                "Dowager Duchess",
                "Dowager Duke",
                "Duchess",
                "Duke",
                "Commander",
                "Lady",
                "Lord",
                "Dame",
                "Sir",
                "Queen",
                "King"
        };

        for (String knownTitle : knownTitles) {
            String prefix = knownTitle + " ";
            if (result.startsWith(prefix)) {
                return result.substring(prefix.length()).trim();
            }
        }

        return result;
    }
}