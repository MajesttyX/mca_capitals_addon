package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

        UUID newConsort = null;
        boolean newConsortFemale = false;
        UUID newHeir = null;

        Set<UUID> newRoyalChildren = new HashSet<>();
        Map<UUID, Boolean> newRoyalChildFemale = new HashMap<>();

        Set<UUID> newLords = new HashSet<>();
        Map<UUID, Boolean> newLordFemale = new HashMap<>();

        Set<UUID> newKnights = new HashSet<>();
        Map<UUID, Boolean> newKnightFemale = new HashMap<>();

        UUID spouse = MCAIntegrationBridge.getSpouse(level, sovereign);
        if (isValidRelationshipSpouse(level, spouse)) {
            newConsort = spouse;
            newConsortFemale = MCAIntegrationBridge.isFemale(level, spouse);
        }

        for (UUID childId : MCAIntegrationBridge.getChildren(level, sovereign)) {
            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                newRoyalChildren.add(childId);
                newRoyalChildFemale.put(childId, MCAIntegrationBridge.isFemale(level, childId));
            }
        }

        for (UUID residentId : residents) {
            if (residentId.equals(sovereign)) {
                continue;
            }

            if (MCAIntegrationBridge.isChildOf(level, residentId, sovereign)) {
                newRoyalChildren.add(residentId);
                newRoyalChildFemale.put(residentId, MCAIntegrationBridge.isFemale(level, residentId));
            }
        }

        newHeir = newRoyalChildren.stream()
                .filter(residents::contains)
                .sorted(Comparator.comparing(UUID::toString))
                .findFirst()
                .orElse(null);

        if (newHeir == null) {
            newHeir = newRoyalChildren.stream()
                    .sorted(Comparator.comparing(UUID::toString))
                    .findFirst()
                    .orElse(null);
        }

        Set<UUID> allRelevant = new HashSet<>(residents);
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
    }

    public static void applySovereignMarriage(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, sovereign);
        UUID validConsort = isValidRelationshipSpouse(level, spouse) ? spouse : null;
        boolean spouseFemale = validConsort != null && MCAIntegrationBridge.isFemale(level, validConsort);

        UUID heirBefore = capital.getHeir();
        Set<UUID> royalChildrenBefore = new HashSet<>(capital.getRoyalChildren());
        Map<UUID, Boolean> royalChildFemaleBefore = new HashMap<>(capital.getRoyalChildFemale());
        Set<UUID> lordsBefore = new HashSet<>(capital.getLords());
        Map<UUID, Boolean> lordFemaleBefore = new HashMap<>(capital.getLordFemale());
        Set<UUID> knightsBefore = new HashSet<>(capital.getKnights());
        Map<UUID, Boolean> knightFemaleBefore = new HashMap<>(capital.getKnightFemale());

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

    private static boolean isValidRelationshipSpouse(ServerLevel level, UUID spouseId) {
        if (spouseId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, spouseId);
        if (entity != null) {
            return entity.isAlive() && !entity.isRemoved();
        }

        return MCAIntegrationBridge.hasFamilyNode(level, spouseId);
    }
}