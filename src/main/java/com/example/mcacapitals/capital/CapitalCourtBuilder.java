package com.example.mcacapitals.capital;

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
        List<UUID> discoveredRoyalBirthOrder = new ArrayList<>();

        Set<UUID> newDukes = new LinkedHashSet<>();
        Map<UUID, Boolean> newDukeFemale = new LinkedHashMap<>();

        Set<UUID> newLords = new LinkedHashSet<>();
        Map<UUID, Boolean> newLordFemale = new LinkedHashMap<>();

        Set<UUID> newKnights = new LinkedHashSet<>();
        Map<UUID, Boolean> newKnightFemale = new LinkedHashMap<>();

        UUID spouse = MCAIntegrationBridge.getSpouse(level, sovereign);
        if (isValidRelationshipPerson(level, spouse)
                && CapitalCourtMarriageResolver.isValidMarriedConsort(level, sovereign, spouse)) {
            newConsort = spouse;
            newConsortFemale = MCAIntegrationBridge.isFemale(level, spouse);
        }

        collectRoyalChildren(
                level,
                capital,
                sovereign,
                existingDowager,
                newRoyalChildren,
                newRoyalChildFemale,
                discoveredRoyalBirthOrder
        );

        synchronizeRoyalSuccessionOrder(capital, newRoyalChildren, discoveredRoyalBirthOrder);
        newHeir = resolveHeir(level, capital, residents, sovereign, newRoyalChildren);

        preserveDirectDukes(level, capital, preservedDirectDukes, preservedDirectDukeFemale, existingDowager, newDukes, newDukeFemale);

        Set<UUID> allRelevant = buildAllRelevantResidents(residents, preservedDirectDukes, newRoyalChildren, newConsort, existingDowager);
        classifyCourtResidents(level, capital, residents, allRelevant, sovereign, newConsort, existingDowager, newHeir,
                newRoyalChildren, newDukes, newLords, newLordFemale, newKnights, newKnightFemale);

        CapitalCourtMarriageResolver.addMarriageDerivedTitles(
                level,
                residents,
                capital,
                newDukes,
                newDukeFemale,
                newLords,
                newLordFemale,
                newKnights,
                newKnightFemale
        );

        CapitalCourtApplier.applyComputedCourt(
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

        restoreAndCleanDowager(capital, existingDowager, existingDowagerFemale);
        writeRoyalChildChronicleEntries(level, capital, oldRoyalChildren, newRoyalChildren);
    }

    public static void applySovereignMarriage(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, sovereign);
        UUID validConsort = (isValidRelationshipPerson(level, spouse)
                && CapitalCourtMarriageResolver.isValidMarriedConsort(level, sovereign, spouse)) ? spouse : null;
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

    static boolean isValidRelationshipPerson(ServerLevel level, UUID personId) {
        if (personId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, personId);
        if (entity != null) {
            return entity.isAlive() && !entity.isRemoved();
        }

        return MCAIntegrationBridge.hasFamilyNode(level, personId);
    }

    private static void collectRoyalChildren(
            ServerLevel level,
            CapitalRecord capital,
            UUID sovereign,
            UUID existingDowager,
            Set<UUID> newRoyalChildren,
            Map<UUID, Boolean> newRoyalChildFemale,
            List<UUID> discoveredRoyalBirthOrder
    ) {
        for (UUID childId : MCAIntegrationBridge.getChildren(level, sovereign)) {
            if (childId == null || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }

            boolean dynasticChild = MCAIntegrationBridge.isChildOf(level, childId, sovereign);
            if (dynasticChild || capital.isLegitimizedRoyalChild(childId)) {
                if (newRoyalChildren.add(childId)) {
                    discoveredRoyalBirthOrder.add(childId);
                }
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
                if (newRoyalChildren.add(existingRoyalChild) && !discoveredRoyalBirthOrder.contains(existingRoyalChild)) {
                    discoveredRoyalBirthOrder.add(existingRoyalChild);
                }
                newRoyalChildFemale.put(existingRoyalChild, capital.isRoyalChildFemale(existingRoyalChild));
            }
        }
    }

    private static void synchronizeRoyalSuccessionOrder(
            CapitalRecord capital,
            Set<UUID> newRoyalChildren,
            List<UUID> discoveredRoyalBirthOrder
    ) {
        LinkedHashSet<UUID> merged = new LinkedHashSet<>();

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId != null
                    && newRoyalChildren.contains(childId)
                    && !capital.isDisinheritedRoyalChild(childId)) {
                merged.add(childId);
            }
        }

        for (UUID childId : discoveredRoyalBirthOrder) {
            if (childId != null
                    && newRoyalChildren.contains(childId)
                    && !capital.isDisinheritedRoyalChild(childId)) {
                merged.add(childId);
            }
        }

        for (UUID childId : newRoyalChildren) {
            if (childId != null && !capital.isDisinheritedRoyalChild(childId)) {
                merged.add(childId);
            }
        }

        capital.setRoyalSuccessionOrder(new ArrayList<>(merged));
    }

    private static UUID resolveHeir(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            UUID sovereign,
            Set<UUID> newRoyalChildren
    ) {
        UUID existingHeir = capital.getHeir();

        if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL) {
            if (isValidManualHeirCandidate(level, existingHeir, sovereign, residents, newRoyalChildren)) {
                return existingHeir;
            }
        } else {
            if (isValidDynasticHeirCandidate(level, existingHeir, sovereign, newRoyalChildren)) {
                return existingHeir;
            }
        }

        UUID newHeir = firstValidRoyalChild(capital.getRoyalSuccessionOrder(), residents, sovereign, level, newRoyalChildren);
        if (newHeir == null) {
            newHeir = firstValidRoyalChild(capital.getRoyalSuccessionOrder(), null, sovereign, level, newRoyalChildren);
        }

        if (newHeir != null) {
            capital.setHeirMode(CapitalRecord.HeirMode.DYNASTIC);
        }

        return newHeir;
    }

    private static void preserveDirectDukes(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> preservedDirectDukes,
            Map<UUID, Boolean> preservedDirectDukeFemale,
            UUID existingDowager,
            Set<UUID> newDukes,
            Map<UUID, Boolean> newDukeFemale
    ) {
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
    }

    private static Set<UUID> buildAllRelevantResidents(
            Set<UUID> residents,
            Set<UUID> preservedDirectDukes,
            Set<UUID> newRoyalChildren,
            UUID newConsort,
            UUID existingDowager
    ) {
        Set<UUID> allRelevant = new LinkedHashSet<>(residents);
        allRelevant.addAll(newRoyalChildren);
        allRelevant.addAll(preservedDirectDukes);
        if (newConsort != null) {
            allRelevant.add(newConsort);
        }
        if (existingDowager != null) {
            allRelevant.add(existingDowager);
        }
        return allRelevant;
    }

    private static void classifyCourtResidents(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            Set<UUID> allRelevant,
            UUID sovereign,
            UUID newConsort,
            UUID existingDowager,
            UUID newHeir,
            Set<UUID> newRoyalChildren,
            Set<UUID> newDukes,
            Set<UUID> newLords,
            Map<UUID, Boolean> newLordFemale,
            Set<UUID> newKnights,
            Map<UUID, Boolean> newKnightFemale
    ) {
        for (UUID residentId : allRelevant) {
            if (shouldSkipCourtClassification(capital, residents, residentId, sovereign, newConsort, existingDowager, newHeir, newRoyalChildren, newDukes)) {
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
    }

    private static boolean shouldSkipCourtClassification(
            CapitalRecord capital,
            Set<UUID> residents,
            UUID residentId,
            UUID sovereign,
            UUID newConsort,
            UUID existingDowager,
            UUID newHeir,
            Set<UUID> newRoyalChildren,
            Set<UUID> newDukes
    ) {
        if (residentId == null) {
            return true;
        }
        if (residentId.equals(sovereign)) {
            return true;
        }
        if (newConsort != null && residentId.equals(newConsort)) {
            return true;
        }
        if (existingDowager != null && residentId.equals(existingDowager)) {
            return true;
        }
        if (newHeir != null && residentId.equals(newHeir)) {
            return true;
        }
        if (residentId.equals(capital.getCommander())) {
            return true;
        }
        if (newRoyalChildren.contains(residentId)) {
            return true;
        }
        if (!residents.contains(residentId)) {
            return true;
        }
        return newDukes.contains(residentId);
    }

    private static void restoreAndCleanDowager(CapitalRecord capital, UUID existingDowager, boolean existingDowagerFemale) {
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
    }

    private static void writeRoyalChildChronicleEntries(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> oldRoyalChildren,
            Set<UUID> newRoyalChildren
    ) {
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

    private static boolean isValidDynasticHeirCandidate(
            ServerLevel level,
            UUID candidate,
            UUID sovereign,
            Set<UUID> validRoyalChildren
    ) {
        if (candidate == null || candidate.equals(sovereign)) {
            return false;
        }

        if (!validRoyalChildren.contains(candidate)) {
            return false;
        }

        return MCAIntegrationBridge.hasFamilyNode(level, candidate);
    }

    private static boolean isValidManualHeirCandidate(
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

    private static String resolveName(ServerLevel level, UUID id) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        return entity != null ? entity.getName().getString() : id.toString();
    }
}