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

        UUID newConsort = null;
        boolean newConsortFemale = false;
        UUID newHeir = null;

        Set<UUID> newRoyalChildren = new LinkedHashSet<>();
        Map<UUID, Boolean> newRoyalChildFemale = new LinkedHashMap<>();

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
                capital.addRoyalChild(childId, MCAIntegrationBridge.isFemale(level, childId));
            }
        }

        for (UUID existingRoyalChild : capital.getRoyalChildren()) {
            if (existingRoyalChild == null || capital.isDisinheritedRoyalChild(existingRoyalChild)) {
                continue;
            }
            if (capital.isLegitimizedRoyalChild(existingRoyalChild) || MCAIntegrationBridge.isChildOf(level, existingRoyalChild, sovereign)) {
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

        Set<UUID> allRelevant = new LinkedHashSet<>(residents);
        allRelevant.addAll(newRoyalChildren);

        if (newConsort != null) {
            allRelevant.add(newConsort);
        }

        for (UUID residentId : allRelevant) {
            if (residentId.equals(sovereign)) {
                continue;
            }

            if (newConsort != null && residentId.equals(newConsort)) {
                continue;
            }

            if (newRoyalChildren.contains(residentId)) {
                continue;
            }

            if (capital.isDuke(residentId)) {
                continue;
            }

            if (!residents.contains(residentId)) {
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

        UUID sovereignBefore = capital.getSovereign();
        boolean sovereignFemaleBefore = capital.isSovereignFemale();
        UUID dowagerBefore = capital.getDowager();
        boolean dowagerFemaleBefore = capital.isDowagerFemale();
        CapitalState stateBefore = capital.getState();

        capital.replaceDynamicRoles(
                newConsort,
                newConsortFemale,
                newHeir,
                newRoyalChildren,
                newRoyalChildFemale,
                newLords,
                newLordFemale,
                newKnights,
                newKnightFemale
        );

        capital.setSovereign(sovereignBefore);
        capital.setSovereignFemale(sovereignFemaleBefore);
        capital.setDowager(dowagerBefore);
        capital.setDowagerFemale(dowagerFemaleBefore);

        if (sovereignBefore != null) {
            capital.setState(CapitalState.ACTIVE);
        } else {
            capital.setState(stateBefore);
        }

        if (capital.getDowager() != null && capital.getDowager().equals(capital.getSovereign())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }

        if (capital.getDowager() != null && capital.getDowager().equals(capital.getConsort())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }

        for (UUID childId : newRoyalChildren) {
            if (!oldRoyalChildren.contains(childId)) {
                String name = resolveName(level, childId);
                CapitalChronicleService.addEntry(level, capital,
                        "A royal child, " + name + ", was entered into the dynastic record of "
                                + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
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

        UUID heirBefore = capital.getHeir();
        Set<UUID> royalChildrenBefore = new LinkedHashSet<>(capital.getRoyalChildren());
        Map<UUID, Boolean> royalChildFemaleBefore = new LinkedHashMap<>(capital.getRoyalChildFemale());
        Set<UUID> lordsBefore = new LinkedHashSet<>(capital.getLords());
        Map<UUID, Boolean> lordFemaleBefore = new LinkedHashMap<>(capital.getLordFemale());
        Set<UUID> knightsBefore = new LinkedHashSet<>(capital.getKnights());
        Map<UUID, Boolean> knightFemaleBefore = new LinkedHashMap<>(capital.getKnightFemale());

        capital.replaceDynamicRoles(
                validConsort,
                spouseFemale,
                heirBefore,
                royalChildrenBefore,
                royalChildFemaleBefore,
                lordsBefore,
                lordFemaleBefore,
                knightsBefore,
                knightFemaleBefore
        );

        if (capital.getDowager() != null && capital.getDowager().equals(capital.getConsort())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
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