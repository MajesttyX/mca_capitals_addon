package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalCourtBuilder {

    private CapitalCourtBuilder() {
    }

    public static void rebuildCourt(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return;
        }

        Set<UUID> oldRoyalChildren = new LinkedHashSet<>(capital.getRoyalChildren());
        Set<UUID> preservedDirectDukes = new LinkedHashSet<>(capital.getDukes());
        Map<UUID, Boolean> preservedDirectDukeFemale = new LinkedHashMap<>(capital.getDukeFemale());

        UUID existingDowager = capital.getDowager();
        boolean existingDowagerFemale = capital.isDowagerFemale();

        UUID newConsort = null;
        boolean newConsortFemale = false;
        UUID newHeir = null;

        Set<UUID> newRoyalChildren = new LinkedHashSet<>();
        Map<UUID, Boolean> newRoyalChildFemale = new LinkedHashMap<>();

        Set<UUID> newDukes = new LinkedHashSet<>();
        Map<UUID, Boolean> newDukeFemale = new LinkedHashMap<>();

        Set<UUID> newLords = new LinkedHashSet<>();
        Map<UUID, Boolean> newLordFemale = new LinkedHashMap<>();

        Set<UUID> newKnights = new LinkedHashSet<>();
        Map<UUID, Boolean> newKnightFemale = new LinkedHashMap<>();

        UUID spouse = MCAIntegrationBridge.getSpouse(level, sovereign);
        if (isValidRelationshipPerson(level, spouse)) {
            newConsort = spouse;
            newConsortFemale = MCAIntegrationBridge.isFemale(level, spouse);
        }

        for (UUID childId : MCAIntegrationBridge.getChildren(level, sovereign)) {
            if (childId == null || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }

            boolean dynasticChild = MCAIntegrationBridge.isChildOf(level, childId, sovereign);
            if (dynasticChild || capital.isLegitimizedRoyalChild(childId)) {
                newRoyalChildren.add(childId);
                newRoyalChildFemale.put(childId, MCAIntegrationBridge.isFemale(level, childId));
            }
        }

        for (UUID existingRoyalChild : capital.getRoyalChildren()) {
            if (existingRoyalChild == null || capital.isDisinheritedRoyalChild(existingRoyalChild)) {
                continue;
            }

            if (capital.isLegitimizedRoyalChild(existingRoyalChild)
                    || MCAIntegrationBridge.isChildOf(level, existingRoyalChild, sovereign)
                    || (existingDowager != null && MCAIntegrationBridge.isChildOf(level, existingRoyalChild, existingDowager))) {
                newRoyalChildren.add(existingRoyalChild);
                newRoyalChildFemale.put(existingRoyalChild, capital.isRoyalChildFemale(existingRoyalChild));
            }
        }

        UUID existingHeir = capital.getHeir();
        if (isValidHeirCandidate(level, existingHeir, sovereign, residents, newRoyalChildren)) {
            newHeir = existingHeir;
        } else {
            newHeir = firstValidRoyalChild(capital.getRoyalSuccessionOrder(), residents, sovereign, level, newRoyalChildren);
            if (newHeir == null) {
                newHeir = firstValidRoyalChild(capital.getRoyalSuccessionOrder(), null, sovereign, level, newRoyalChildren);
            }
        }

        for (UUID dukeId : preservedDirectDukes) {
            if (dukeId == null) {
                continue;
            }
            if (existingDowager != null && existingDowager.equals(dukeId)) {
                continue;
            }
            newDukes.add(dukeId);
            newDukeFemale.put(dukeId, preservedDirectDukeFemale.getOrDefault(dukeId, MCAIntegrationBridge.isFemale(level, dukeId)));
        }

        Set<UUID> allRelevant = new LinkedHashSet<>(residents);
        allRelevant.addAll(newRoyalChildren);
        allRelevant.addAll(preservedDirectDukes);
        if (newConsort != null) {
            allRelevant.add(newConsort);
        }
        if (existingDowager != null) {
            allRelevant.add(existingDowager);
        }

        for (UUID residentId : allRelevant) {
            if (residentId == null) {
                continue;
            }
            if (residentId.equals(sovereign)) {
                continue;
            }
            if (newConsort != null && residentId.equals(newConsort)) {
                continue;
            }
            if (existingDowager != null && residentId.equals(existingDowager)) {
                continue;
            }
            if (newHeir != null && residentId.equals(newHeir)) {
                continue;
            }
            if (residentId.equals(capital.getCommander())) {
                continue;
            }
            if (newRoyalChildren.contains(residentId)) {
                continue;
            }
            if (!residents.contains(residentId)) {
                continue;
            }
            if (newDukes.contains(residentId)) {
                continue;
            }

            if (MCAIntegrationBridge.isMCAGuard(level, residentId)) {
                newKnights.add(residentId);
                newKnightFemale.put(residentId, MCAIntegrationBridge.isFemale(level, residentId));
                continue;
            }

            if (MCAIntegrationBridge.isMasterProfessionVillager(level, residentId)) {
                newLords.add(residentId);
                newLordFemale.put(residentId, MCAIntegrationBridge.isFemale(level, residentId));
            }
        }

        addMarriageDerivedTitles(level, residents, capital, newDukes, newDukeFemale, newLords, newLordFemale, newKnights, newKnightFemale);

        applyComputedCourt(
                level,
                capital,
                newConsort,
                newConsortFemale,
                newHeir,
                newRoyalChildren,
                newRoyalChildFemale,
                newDukes,
                newDukeFemale,
                newLords,
                newLordFemale,
                newKnights,
                newKnightFemale
        );

        capital.setDowager(existingDowager);
        capital.setDowagerFemale(existingDowagerFemale);

        if (capital.getDowager() != null && capital.getDowager().equals(capital.getSovereign())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }

        if (capital.getDowager() != null && capital.getDowager().equals(capital.getConsort())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }

        if (capital.getDowager() != null) {
            capital.getKnights().remove(capital.getDowager());
            capital.getKnightFemale().remove(capital.getDowager());
            capital.getLords().remove(capital.getDowager());
            capital.getLordFemale().remove(capital.getDowager());
            capital.getDukes().remove(capital.getDowager());
            capital.getDukeFemale().remove(capital.getDowager());
        }

        for (UUID childId : newRoyalChildren) {
            if (!oldRoyalChildren.contains(childId)) {
                String name = resolveName(level, childId);
                CapitalChronicleService.addEntry(
                        level,
                        capital,
                        "A royal child, " + name + ", was entered into the dynastic record of "
                                + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
                );
            }
        }
    }

    public static void applySovereignMarriage(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, sovereign);
        UUID validConsort = isValidRelationshipPerson(level, spouse) ? spouse : null;
        boolean spouseFemale = validConsort != null && MCAIntegrationBridge.isFemale(level, validConsort);

        capital.setConsort(validConsort);
        capital.setConsortFemale(spouseFemale);

        if (capital.getDowager() != null && capital.getDowager().equals(capital.getConsort())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }

        if (capital.getSovereign() != null) {
            capital.setState(CapitalState.ACTIVE);
        }
    }

    private static void addMarriageDerivedTitles(
            ServerLevel level,
            Set<UUID> residents,
            CapitalRecord capital,
            Set<UUID> dukes,
            Map<UUID, Boolean> dukeFemale,
            Set<UUID> lords,
            Map<UUID, Boolean> lordFemale,
            Set<UUID> knights,
            Map<UUID, Boolean> knightFemale
    ) {
        for (UUID dukeId : new LinkedHashSet<>(dukes)) {
            addSpouse(level, residents, capital, dukeId, dukes, dukeFemale);
        }

        for (UUID lordId : new LinkedHashSet<>(lords)) {
            addSpouse(level, residents, capital, lordId, lords, lordFemale);
        }
    }

    private static void addSpouse(
            ServerLevel level,
            Set<UUID> residents,
            CapitalRecord capital,
            UUID sourceId,
            Set<UUID> targetSet,
            Map<UUID, Boolean> femaleMap
    ) {
        UUID spouse = MCAIntegrationBridge.getSpouse(level, sourceId);
        if (!isValidRelationshipPerson(level, spouse)) {
            return;
        }
        if (residents != null && !residents.contains(spouse)) {
            return;
        }
        if (spouse.equals(capital.getSovereign())
                || spouse.equals(capital.getConsort())
                || spouse.equals(capital.getDowager())
                || spouse.equals(capital.getCommander())
                || spouse.equals(capital.getHeir())) {
            return;
        }

        targetSet.add(spouse);
        femaleMap.put(spouse, MCAIntegrationBridge.isFemale(level, spouse));
    }

    private static void applyComputedCourt(
            ServerLevel level,
            CapitalRecord capital,
            UUID newConsort,
            boolean newConsortFemale,
            UUID newHeir,
            Set<UUID> newRoyalChildren,
            Map<UUID, Boolean> newRoyalChildFemale,
            Set<UUID> newDukes,
            Map<UUID, Boolean> newDukeFemale,
            Set<UUID> newLords,
            Map<UUID, Boolean> newLordFemale,
            Set<UUID> newKnights,
            Map<UUID, Boolean> newKnightFemale
    ) {
        capital.setConsort(newConsort);
        capital.setConsortFemale(newConsortFemale);

        capital.getRoyalChildren().clear();
        capital.getRoyalChildFemale().clear();
        for (UUID childId : newRoyalChildren) {
            capital.getRoyalChildren().add(childId);
            capital.getRoyalChildFemale().put(childId, newRoyalChildFemale.getOrDefault(childId, false));
        }

        capital.getDukes().clear();
        capital.getDukeFemale().clear();
        for (UUID dukeId : newDukes) {
            capital.getDukes().add(dukeId);
            capital.getDukeFemale().put(dukeId, newDukeFemale.getOrDefault(dukeId, false));
        }

        capital.getLords().clear();
        capital.getLordFemale().clear();
        for (UUID lordId : newLords) {
            capital.getLords().add(lordId);
            capital.getLordFemale().put(lordId, newLordFemale.getOrDefault(lordId, false));
        }

        capital.getKnights().clear();
        capital.getKnightFemale().clear();
        for (UUID knightId : newKnights) {
            capital.getKnights().add(knightId);
            capital.getKnightFemale().put(knightId, newKnightFemale.getOrDefault(knightId, false));
        }

        capital.setHeir(newHeir);
        if (newHeir != null) {
            boolean heirFemale = newRoyalChildFemale.getOrDefault(newHeir, MCAIntegrationBridge.isFemale(level, newHeir));
            capital.setHeirFemale(heirFemale);
        } else {
            capital.setHeirFemale(false);
        }

        if (capital.getSovereign() != null) {
            capital.setState(CapitalState.ACTIVE);
        }
    }

    private static UUID firstValidRoyalChild(
            Iterable<UUID> orderedRoyalChildren,
            Set<UUID> residents,
            UUID sovereign,
            ServerLevel level,
            Set<UUID> validRoyalChildren
    ) {
        for (UUID childId : orderedRoyalChildren) {
            if (childId == null || childId.equals(sovereign)) {
                continue;
            }
            if (!validRoyalChildren.contains(childId)) {
                continue;
            }
            if (residents != null && !residents.contains(childId)) {
                continue;
            }
            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }
        return null;
    }

    private static boolean isValidHeirCandidate(
            ServerLevel level,
            UUID candidate,
            UUID sovereign,
            Set<UUID> residents,
            Set<UUID> validRoyalChildren
    ) {
        if (candidate == null || candidate.equals(sovereign)) {
            return false;
        }

        if (validRoyalChildren.contains(candidate)) {
            return MCAIntegrationBridge.hasFamilyNode(level, candidate);
        }

        return residents.contains(candidate) && MCAIntegrationBridge.hasFamilyNode(level, candidate);
    }

    private static boolean isValidRelationshipPerson(ServerLevel level, UUID personId) {
        if (personId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, personId);
        if (entity != null) {
            return entity.isAlive() && !entity.isRemoved();
        }

        return MCAIntegrationBridge.hasFamilyNode(level, personId);
    }

    private static String resolveName(ServerLevel level, UUID id) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        return entity != null ? entity.getName().getString() : id.toString();
    }
}