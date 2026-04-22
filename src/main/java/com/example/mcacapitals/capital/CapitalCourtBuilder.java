package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

        UUID previousConsort = capital.getConsort();

        Set<UUID> oldRoyalChildren = new LinkedHashSet<>(capital.getRoyalChildren());
        Set<UUID> preservedDirectDukes = new LinkedHashSet<>(capital.getDukes());
        Map<UUID, Boolean> preservedDirectDukeFemale = new LinkedHashMap<>(capital.getDukeFemale());

        Set<UUID> preservedDirectLords = new LinkedHashSet<>(capital.getLords());
        Map<UUID, Boolean> preservedDirectLordFemale = new LinkedHashMap<>(capital.getLordFemale());

        UUID existingDowager = capital.getDowager();
        boolean existingDowagerFemale = capital.isDowagerFemale();

        UUID newConsort = null;
        boolean newConsortFemale = false;
        UUID newHeir = null;

        Set<UUID> newRoyalChildren = new LinkedHashSet<>();
        Map<UUID, Boolean> newRoyalChildFemale = new LinkedHashMap<>();
        List<UUID> discoveredRoyalBirthOrder = new ArrayList<>();

        Map<UUID, UUID> newPrinceConsortSources = new LinkedHashMap<>();
        Map<UUID, Boolean> newPrinceConsortFemale = new LinkedHashMap<>();

        Set<UUID> newDukes = new LinkedHashSet<>();
        Map<UUID, Boolean> newDukeFemale = new LinkedHashMap<>();

        Map<UUID, UUID> newMarriageDukeSources = new LinkedHashMap<>();
        Map<UUID, Boolean> newMarriageDukeFemale = new LinkedHashMap<>();

        Set<UUID> newLords = new LinkedHashSet<>();
        Map<UUID, Boolean> newLordFemale = new LinkedHashMap<>();

        Set<UUID> newKnights = new LinkedHashSet<>();
        Map<UUID, Boolean> newKnightFemale = new LinkedHashMap<>();

        UUID spouse = CapitalCourtMarriageResolver.findActualSpouse(level, sovereign);
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

        collectPrinceConsortSources(
                level,
                capital,
                residents,
                newRoyalChildren,
                newHeir,
                newPrinceConsortSources,
                newPrinceConsortFemale
        );

        preserveDirectDukes(level, capital, preservedDirectDukes, preservedDirectDukeFemale, existingDowager, newDukes, newDukeFemale);
        preserveDirectLords(level, capital, preservedDirectLords, preservedDirectLordFemale, existingDowager, newLords, newLordFemale);

        Set<UUID> allRelevant = buildAllRelevantResidents(residents, preservedDirectDukes, preservedDirectLords, newRoyalChildren, newConsort, existingDowager);
        classifyCourtResidents(level, capital, residents, allRelevant, sovereign, newConsort, existingDowager, newHeir,
                newRoyalChildren, newDukes, newLords, newLordFemale, newKnights, newKnightFemale);

        CapitalCourtMarriageResolver.collectMarriageDukeSources(
                level,
                residents,
                capital,
                newDukes,
                newMarriageDukeSources,
                newMarriageDukeFemale
        );

        CapitalCourtApplier.applyComputedCourt(
                level,
                capital,
                newConsort,
                newConsortFemale,
                newHeir,
                newRoyalChildren,
                newRoyalChildFemale,
                newPrinceConsortSources,
                newPrinceConsortFemale,
                newDukes,
                newDukeFemale,
                newMarriageDukeSources,
                newMarriageDukeFemale,
                newLords,
                newLordFemale,
                newKnights,
                newKnightFemale
        );

        if (newConsort != null && !newConsort.equals(previousConsort)) {
            ServerPlayer livePlayerSpouse = CapitalCourtMarriageResolver.findActualPlayerSpouse(level, sovereign);

            if (livePlayerSpouse != null) {
                capital.setPlayerConsort(true);
                capital.setPlayerConsortId(livePlayerSpouse.getUUID());
                capital.setPlayerConsortName(livePlayerSpouse.getGameProfile().getName());
                capital.setConsort(livePlayerSpouse.getUUID());
                capital.setConsortFemale(false);
                newConsort = livePlayerSpouse.getUUID();
            } else if (!MCAIntegrationBridge.isMCAVillager(level, newConsort)) {
                capital.setPlayerConsort(true);
                capital.setPlayerConsortId(newConsort);
                capital.setPlayerConsortName(resolveBestOnlinePlayerName(level));
            }

            if (MCAIntegrationBridge.isMCAVillager(level, newConsort)) {
                String sovereignName = resolveName(level, sovereign);
                String consortName = CapitalCourtMarriageResolver.resolveSpouseName(level, sovereign);

                CapitalChronicleService.addEntry(
                        level,
                        capital,
                        sovereignName + " was married to " + consortName + "."
                );
            }
        }

        restoreAndCleanDowager(capital, existingDowager, existingDowagerFemale);
        writeRoyalChildChronicleEntries(level, capital, oldRoyalChildren, newRoyalChildren);
    }

    public static void applySovereignMarriage(ServerLevel level, CapitalRecord capital) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return;
        }

        UUID previousConsort = capital.getConsort();

        UUID spouse = CapitalCourtMarriageResolver.findActualSpouse(level, sovereign);
        UUID validConsort = (isValidRelationshipPerson(level, spouse)
                && CapitalCourtMarriageResolver.isValidMarriedConsort(level, sovereign, spouse)) ? spouse : null;
        boolean spouseFemale = validConsort != null && MCAIntegrationBridge.isFemale(level, validConsort);

        ServerPlayer livePlayerSpouse = CapitalCourtMarriageResolver.findActualPlayerSpouse(level, sovereign);
        if (livePlayerSpouse != null) {
            validConsort = livePlayerSpouse.getUUID();
            spouseFemale = false;
        }

        capital.setConsort(validConsort);
        capital.setConsortFemale(spouseFemale);

        if (livePlayerSpouse != null) {
            capital.setPlayerConsort(true);
            capital.setPlayerConsortId(livePlayerSpouse.getUUID());
            capital.setPlayerConsortName(livePlayerSpouse.getGameProfile().getName());
        } else if (validConsort != null && !MCAIntegrationBridge.isMCAVillager(level, validConsort)) {
            capital.setPlayerConsort(true);
            capital.setPlayerConsortId(validConsort);
            capital.setPlayerConsortName(resolveBestOnlinePlayerName(level));
        } else {
            capital.setPlayerConsort(false);
            capital.setPlayerConsortId(null);
            capital.setPlayerConsortName(null);
        }

        if (capital.getDowager() != null && capital.getDowager().equals(capital.getConsort())) {
            capital.setDowager(null);
            capital.setDowagerFemale(false);
        }

        if (capital.getSovereign() != null) {
            capital.setState(CapitalState.ACTIVE);
        }

        if (validConsort != null
                && !validConsort.equals(previousConsort)
                && MCAIntegrationBridge.isMCAVillager(level, validConsort)) {
            String sovereignName = resolveName(level, sovereign);
            String consortName = CapitalCourtMarriageResolver.resolveSpouseName(level, sovereign);

            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    sovereignName + " was married to " + consortName + "."
            );
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

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(personId);
        if (player != null) {
            return true;
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

            if (newRoyalChildren.add(existingRoyalChild) && !discoveredRoyalBirthOrder.contains(existingRoyalChild)) {
                discoveredRoyalBirthOrder.add(existingRoyalChild);
            }

            boolean female = capital.getRoyalChildFemale().getOrDefault(
                    existingRoyalChild,
                    existingDowager != null && MCAIntegrationBridge.isChildOf(level, existingRoyalChild, existingDowager)
                            ? MCAIntegrationBridge.isFemale(level, existingRoyalChild)
                            : MCAIntegrationBridge.isFemale(level, existingRoyalChild)
            );
            newRoyalChildFemale.put(existingRoyalChild, female);
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
            if (childId != null
                    && !capital.isDisinheritedRoyalChild(childId)) {
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
        }

        UUID directChildHeir = firstValidDirectChildOfSovereign(level, capital, residents, sovereign, newRoyalChildren);
        if (directChildHeir != null) {
            capital.setHeirMode(CapitalRecord.HeirMode.DYNASTIC);
            return directChildHeir;
        }

        if (capital.getHeirMode() != CapitalRecord.HeirMode.MANUAL) {
            if (isValidDynasticHeirCandidate(level, existingHeir, sovereign, newRoyalChildren)
                    && MCAIntegrationBridge.isChildOf(level, existingHeir, sovereign)) {
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

    private static UUID firstValidDirectChildOfSovereign(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            UUID sovereign,
            Set<UUID> newRoyalChildren
    ) {
        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(sovereign)) {
                continue;
            }
            if (capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!newRoyalChildren.contains(childId)) {
                continue;
            }
            if (!MCAIntegrationBridge.isChildOf(level, childId, sovereign)) {
                continue;
            }
            if (residents != null && !residents.contains(childId)) {
                continue;
            }
            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(sovereign)) {
                continue;
            }
            if (capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!newRoyalChildren.contains(childId)) {
                continue;
            }
            if (!MCAIntegrationBridge.isChildOf(level, childId, sovereign)) {
                continue;
            }
            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        return null;
    }

    private static void collectPrinceConsortSources(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            Set<UUID> newRoyalChildren,
            UUID newHeir,
            Map<UUID, UUID> newPrinceConsortSources,
            Map<UUID, Boolean> newPrinceConsortFemale
    ) {
        Set<UUID> princeSources = new LinkedHashSet<>(newRoyalChildren);
        if (newHeir != null) {
            princeSources.add(newHeir);
        }

        for (UUID princeId : princeSources) {
            if (princeId == null) {
                continue;
            }

            UUID spouse = CapitalCourtMarriageResolver.findActualVillagerSpouse(level, princeId);
            if (spouse == null) {
                continue;
            }
            if (!CapitalCourtMarriageResolver.isValidMarriedConsort(level, princeId, spouse)) {
                continue;
            }
            if (residents != null && !residents.contains(spouse)) {
                continue;
            }
            if (spouse.equals(capital.getSovereign())
                    || spouse.equals(capital.getConsort())
                    || spouse.equals(capital.getDowager())
                    || spouse.equals(capital.getCommander())
                    || spouse.equals(capital.getHeir())) {
                continue;
            }

            newPrinceConsortSources.put(spouse, princeId);
            newPrinceConsortFemale.put(spouse, MCAIntegrationBridge.isFemale(level, spouse));
        }
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

    private static void preserveDirectLords(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> preservedDirectLords,
            Map<UUID, Boolean> preservedDirectLordFemale,
            UUID existingDowager,
            Set<UUID> newLords,
            Map<UUID, Boolean> newLordFemale
    ) {
        for (UUID lordId : preservedDirectLords) {
            if (lordId == null) {
                continue;
            }
            if (existingDowager != null && existingDowager.equals(lordId)) {
                continue;
            }
            newLords.add(lordId);
            newLordFemale.put(lordId, preservedDirectLordFemale.getOrDefault(lordId, MCAIntegrationBridge.isFemale(level, lordId)));
        }
    }

    private static Set<UUID> buildAllRelevantResidents(
            Set<UUID> residents,
            Set<UUID> preservedDirectDukes,
            Set<UUID> preservedDirectLords,
            Set<UUID> newRoyalChildren,
            UUID newConsort,
            UUID existingDowager
    ) {
        Set<UUID> allRelevant = new LinkedHashSet<>(residents);
        allRelevant.addAll(newRoyalChildren);
        allRelevant.addAll(preservedDirectDukes);
        allRelevant.addAll(preservedDirectLords);
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
            if (shouldSkipCourtClassification(capital, residents, residentId, sovereign, newConsort, existingDowager, newHeir, newRoyalChildren, newDukes, newLords)) {
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
            Set<UUID> newDukes,
            Set<UUID> newLords
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
        if (newDukes.contains(residentId)) {
            return true;
        }
        return newLords.contains(residentId);
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

        if (!MCAIntegrationBridge.hasFamilyNode(level, candidate)) {
            return false;
        }

        if (residents != null && residents.contains(candidate)) {
            return true;
        }

        return validRoyalChildren.contains(candidate) || isValidRelationshipPerson(level, candidate);
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity != null) {
            return entity.getName().getString();
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(entityId);
        if (player != null) {
            return player.getName().getString();
        }

        return "Unknown";
    }

    private static String resolveBestOnlinePlayerName(ServerLevel level) {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        return players.isEmpty() ? "Unknown" : players.get(0).getName().getString();
    }
}