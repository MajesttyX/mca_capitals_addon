package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalSuccessionService {

    private CapitalSuccessionService() {
    }

    public static boolean handleSuccessionIfNeeded(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return false;
        }

        if (isValidLivingSovereign(level, sovereign)) {
            return false;
        }

        UUID oldConsort = capital.getConsort();
        boolean oldConsortFemale = capital.isConsortFemale();

        String deadName = resolveName(level, sovereign);

        Set<UUID> oldRoyalChildren = new LinkedHashSet<>(capital.getRoyalChildren());
        Map<UUID, Boolean> oldRoyalChildFemale = new LinkedHashMap<>(capital.getRoyalChildFemale());
        List<UUID> oldSuccessionOrder = new ArrayList<>(capital.getRoyalSuccessionOrder());

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        UUID successor = findSuccessor(level, capital, residents);

        CapitalMourningService.startMourning(level, capital, deadName + " died.");

        if (successor == null) {
            capital.setSovereign(null);
            capital.setSovereignFemale(false);
            capital.setConsort(null);
            capital.setConsortFemale(false);
            capital.setHeir(null);
            capital.setHeirFemale(false);
            capital.setState(CapitalState.PENDING);

            if (isValidRelationshipPerson(level, oldConsort)) {
                capital.setDowager(oldConsort);
                capital.setDowagerFemale(oldConsortFemale);

                String dowagerName = resolveName(level, oldConsort);

                CapitalChronicleService.addEntry(level, capital,
                        deadName + " died. " + dowagerName + " was left as surviving consort while the throne stood vacant.");
            } else {
                CapitalChronicleService.addEntry(level, capital,
                        deadName + " died and no valid successor remained. "
                                + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + " fell vacant.");
            }

            capital.getRoyalChildren().clear();
            capital.getRoyalChildFemale().clear();
            for (UUID royalChild : oldRoyalChildren) {
                if (royalChild != null && !royalChild.equals(sovereign) && !capital.isDisinheritedRoyalChild(royalChild)) {
                    capital.addRoyalChild(royalChild, oldRoyalChildFemale.getOrDefault(royalChild, false));
                }
            }

            capital.getRoyalSuccessionOrder().clear();
            for (UUID childId : oldSuccessionOrder) {
                if (childId != null && !childId.equals(sovereign) && capital.getRoyalChildren().contains(childId)) {
                    capital.getRoyalSuccessionOrder().add(childId);
                }
            }
            for (UUID childId : capital.getRoyalChildren()) {
                if (childId != null && !childId.equals(sovereign) && !capital.getRoyalSuccessionOrder().contains(childId)) {
                    capital.getRoyalSuccessionOrder().add(childId);
                }
            }

            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
            return true;
        }

        capital.setSovereign(successor);
        capital.setSovereignFemale(MCAIntegrationBridge.isFemale(level, successor));

        if (isValidRelationshipPerson(level, oldConsort) && !oldConsort.equals(successor)) {
            capital.setDowager(oldConsort);
            capital.setDowagerFemale(oldConsortFemale);
        }

        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setState(CapitalState.ACTIVE);

        CapitalFoundationService.refreshCourt(level, capital);

        for (UUID royalChild : oldRoyalChildren) {
            if (royalChild == null || royalChild.equals(successor) || capital.isDisinheritedRoyalChild(royalChild)) {
                continue;
            }
            capital.addRoyalChild(royalChild, oldRoyalChildFemale.getOrDefault(royalChild, false));
        }

        capital.getRoyalSuccessionOrder().clear();
        for (UUID childId : oldSuccessionOrder) {
            if (childId == null || childId.equals(successor)) {
                continue;
            }
            if (capital.getRoyalChildren().contains(childId) && !capital.getRoyalSuccessionOrder().contains(childId)) {
                capital.getRoyalSuccessionOrder().add(childId);
            }
        }
        for (UUID childId : capital.getRoyalChildren()) {
            if (childId != null && !childId.equals(successor) && !capital.getRoyalSuccessionOrder().contains(childId)) {
                capital.getRoyalSuccessionOrder().add(childId);
            }
        }

        UUID nextHeir = findNextHeirAfterSuccession(level, capital, residents);
        capital.setHeir(nextHeir);
        if (nextHeir != null) {
            if (capital.getRoyalChildren().contains(nextHeir)) {
                capital.setHeirFemale(capital.isRoyalChildFemale(nextHeir));
            } else {
                capital.setHeirFemale(MCAIntegrationBridge.isFemale(level, nextHeir));
            }
        } else {
            capital.setHeirFemale(false);
        }

        String successorName = resolveName(level, successor);

        CapitalChronicleService.addEntry(level, capital,
                deadName + " died. " + successorName + " inherited the throne of "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        return true;
    }

    public static UUID findAbdicationSuccessor(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        UUID heir = capital.getHeir();
        if (isValidAbdicationCandidate(level, capital, heir)) {
            return heir;
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (residents.contains(id) && isValidAbdicationCandidate(level, capital, id)) {
                return id;
            }
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (isValidAbdicationCandidate(level, capital, id)) {
                return id;
            }
        }

        for (UUID id : capital.getDukes()) {
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getLords()) {
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getKnights()) {
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        return null;
    }

    public static boolean isHeirStillValid(ServerLevel level, CapitalRecord capital) {
        UUID heir = capital.getHeir();
        if (heir == null || capital.getSovereign() == null) {
            return false;
        }

        if (heir.equals(capital.getSovereign())) {
            return false;
        }

        if (capital.isDisinheritedRoyalChild(heir)) {
            return false;
        }

        if (capital.getRoyalChildren().contains(heir) || capital.isLegitimizedRoyalChild(heir)) {
            return MCAIntegrationBridge.hasFamilyNode(level, heir);
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        return residents.contains(heir) && MCAIntegrationBridge.hasFamilyNode(level, heir);
    }

    private static UUID findSuccessor(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        UUID heir = capital.getHeir();
        if (isValidSuccessionCandidate(level, heir) && !capital.isDisinheritedRoyalChild(heir)) {
            return heir;
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getDukes()) {
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getDukes()) {
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getLords()) {
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getLords()) {
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getKnights()) {
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getKnights()) {
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        return null;
    }

    private static UUID findNextHeirAfterSuccession(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (id == null || id.equals(capital.getSovereign()) || capital.isDisinheritedRoyalChild(id)) {
                continue;
            }
            if (residents.contains(id) && isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (id == null || id.equals(capital.getSovereign()) || capital.isDisinheritedRoyalChild(id)) {
                continue;
            }
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        return null;
    }

    private static List<UUID> orderedRoyalSuccessors(CapitalRecord capital) {
        LinkedHashSet<UUID> ordered = new LinkedHashSet<>();

        for (UUID id : capital.getRoyalSuccessionOrder()) {
            if (id != null && !capital.isDisinheritedRoyalChild(id)) {
                ordered.add(id);
            }
        }

        for (UUID id : capital.getRoyalChildren()) {
            if (id != null && !capital.isDisinheritedRoyalChild(id)) {
                ordered.add(id);
            }
        }

        return new ArrayList<>(ordered);
    }

    private static boolean isValidLivingSovereign(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return false;
        }

        if (MCAIntegrationBridge.isMCAVillager(level, entityId)) {
            return MCAIntegrationBridge.isAliveAdultOrChildVillager(level, entityId);
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }

    private static boolean isValidSuccessionCandidate(ServerLevel level, UUID entityId) {
        return entityId != null && MCAIntegrationBridge.hasFamilyNode(level, entityId);
    }

    private static boolean isValidAbdicationCandidate(ServerLevel level, CapitalRecord capital, UUID entityId) {
        return entityId != null
                && !entityId.equals(capital.getSovereign())
                && !capital.isDisinheritedRoyalChild(entityId)
                && MCAIntegrationBridge.hasFamilyNode(level, entityId);
    }

    private static boolean isValidRelationshipPerson(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity != null) {
            return entity.isAlive() && !entity.isRemoved();
        }

        return MCAIntegrationBridge.hasFamilyNode(level, entityId);
    }

    private static String resolveName(ServerLevel level, UUID id) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        return entity != null ? entity.getName().getString() : id.toString();
    }
}