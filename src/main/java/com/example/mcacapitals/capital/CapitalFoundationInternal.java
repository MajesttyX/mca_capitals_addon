package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalFoundationInternal {

    private CapitalFoundationInternal() {
    }

    static boolean abdicateSovereign(ServerLevel level, CapitalRecord capital) {
        UUID oldSovereign = capital.getSovereign();
        UUID oldConsort = capital.getConsort();

        boolean oldPlayerSovereign = capital.isPlayerSovereign();
        UUID oldPlayerSovereignId = capital.getPlayerSovereignId();

        String oldName = MCAIntegrationBridge.getEntityByUuid(level, oldSovereign) != null
                ? MCAIntegrationBridge.getEntityByUuid(level, oldSovereign).getName().getString()
                : oldSovereign.toString();

        Set<UUID> oldRoyalChildren = new LinkedHashSet<>(capital.getRoyalChildren());
        HashMap<UUID, Boolean> oldRoyalChildFemale = new HashMap<>(capital.getRoyalChildFemale());
        ArrayList<UUID> oldSuccessionOrder = new ArrayList<>(capital.getRoyalSuccessionOrder());

        Set<UUID> oldDukes = new LinkedHashSet<>(capital.getDukes());
        Map<UUID, Boolean> oldDukeFemale = new HashMap<>(capital.getDukeFemale());

        Set<UUID> oldLords = new LinkedHashSet<>(capital.getLords());
        Map<UUID, Boolean> oldLordFemale = new HashMap<>(capital.getLordFemale());

        Set<UUID> oldKnights = new LinkedHashSet<>(capital.getKnights());
        Map<UUID, Boolean> oldKnightFemale = new HashMap<>(capital.getKnightFemale());

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        UUID successor = CapitalSuccessionService.findAbdicationSuccessor(level, capital, residents);

        if (successor == null) {
            return false;
        }

        capital.setSovereign(successor);
        capital.setSovereignFemale(MCAIntegrationBridge.isFemale(level, successor));
        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setPlayerConsort(false);
        capital.setPlayerConsortId(null);
        capital.setPlayerConsortName(null);
        capital.setState(CapitalState.ACTIVE);

        CapitalSovereignAppointmentService.clearPlayerSovereignState(capital);

        refreshCourt(level, capital);

        if (oldPlayerSovereign && oldPlayerSovereignId != null && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, oldPlayerSovereignId, capital.getCapitalId());
        }

        restoreCollateralRoyalFamily(
                level,
                capital,
                oldSovereign,
                oldConsort,
                oldRoyalChildren,
                oldRoyalChildFemale
        );

        restoreFormerSovereignStatus(
                level,
                capital,
                oldSovereign,
                oldConsort,
                oldRoyalChildren,
                oldRoyalChildFemale,
                oldDukes,
                oldDukeFemale,
                oldLords,
                oldLordFemale,
                oldKnights,
                oldKnightFemale
        );

        rebuildSuccessionOrderForNewLine(
                level,
                capital,
                successor,
                oldRoyalChildren,
                oldSuccessionOrder
        );

        UUID nextHeir = resolveNextAbdicationHeir(level, capital, successor, residents);
        capital.setHeir(nextHeir);
        if (nextHeir != null) {
            capital.setHeirFemale(capital.isRoyalChildFemale(nextHeir));
            capital.setHeirMode(CapitalRecord.HeirMode.DYNASTIC);
        } else {
            capital.setHeirFemale(false);
            capital.setHeirMode(CapitalRecord.HeirMode.NONE);
        }

        Set<UUID> refreshedResidents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalRoyalHouseholdService.refreshDynasticHousehold(capital);
        CapitalNameService.refreshCapitalNames(level, capital, refreshedResidents);

        String successorName = MCAIntegrationBridge.getEntityByUuid(level, successor) != null
                ? MCAIntegrationBridge.getEntityByUuid(level, successor).getName().getString()
                : successor.toString();

        CapitalChronicleService.addEntry(level, capital,
                oldName + " abdicated the throne. " + successorName + " succeeded to rule "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
        return true;
    }

    static void refreshCourt(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return;
        }

        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return;
        }

        boolean sovereignFemale = capital.isSovereignFemale();
        UUID dowager = capital.getDowager();
        boolean dowagerFemale = capital.isDowagerFemale();

        boolean playerSovereign = capital.isPlayerSovereign();
        UUID playerSovereignId = capital.getPlayerSovereignId();
        String playerSovereignName = capital.getPlayerSovereignName();

        Set<UUID> directDukes = new HashSet<>(capital.getDukes());
        HashMap<UUID, Boolean> directDukeFemale = new HashMap<>(capital.getDukeFemale());

        Set<UUID> directLords = new HashSet<>(capital.getLords());
        HashMap<UUID, Boolean> directLordFemale = new HashMap<>(capital.getLordFemale());

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        CapitalCourtBuilder.rebuildCourt(level, capital, residents);

        capital.setSovereign(sovereign);
        capital.setSovereignFemale(sovereignFemale);
        capital.setDowager(dowager);
        capital.setDowagerFemale(dowagerFemale);

        capital.setPlayerSovereign(playerSovereign);
        capital.setPlayerSovereignId(playerSovereignId);
        capital.setPlayerSovereignName(playerSovereignName);

        for (UUID dukeId : directDukes) {
            capital.addDuke(dukeId, directDukeFemale.getOrDefault(dukeId, MCAIntegrationBridge.isFemale(level, dukeId)));
        }

        for (UUID lordId : directLords) {
            capital.addLord(lordId, directLordFemale.getOrDefault(lordId, MCAIntegrationBridge.isFemale(level, lordId)));
        }

        CapitalCourtBuilder.rebuildCourt(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalRoyalHouseholdService.refreshDynasticHousehold(capital);

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    static String resolvePlayerName(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null) {
            return "";
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        return player != null ? player.getName().getString() : playerId.toString();
    }

    private static void restoreFormerSovereignStatus(
            ServerLevel level,
            CapitalRecord capital,
            UUID oldSovereign,
            UUID oldConsort,
            Set<UUID> oldRoyalChildren,
            Map<UUID, Boolean> oldRoyalChildFemale,
            Set<UUID> oldDukes,
            Map<UUID, Boolean> oldDukeFemale,
            Set<UUID> oldLords,
            Map<UUID, Boolean> oldLordFemale,
            Set<UUID> oldKnights,
            Map<UUID, Boolean> oldKnightFemale
    ) {
        restoreRecoverableRank(
                level,
                capital,
                oldSovereign,
                oldRoyalChildren,
                oldRoyalChildFemale,
                oldDukes,
                oldDukeFemale,
                oldLords,
                oldLordFemale,
                oldKnights,
                oldKnightFemale,
                true
        );

        if (oldConsort != null) {
            restoreRecoverableRank(
                    level,
                    capital,
                    oldConsort,
                    oldRoyalChildren,
                    oldRoyalChildFemale,
                    oldDukes,
                    oldDukeFemale,
                    oldLords,
                    oldLordFemale,
                    oldKnights,
                    oldKnightFemale,
                    false
            );
        }
    }

    private static void restoreRecoverableRank(
            ServerLevel level,
            CapitalRecord capital,
            UUID personId,
            Set<UUID> oldRoyalChildren,
            Map<UUID, Boolean> oldRoyalChildFemale,
            Set<UUID> oldDukes,
            Map<UUID, Boolean> oldDukeFemale,
            Set<UUID> oldLords,
            Map<UUID, Boolean> oldLordFemale,
            Set<UUID> oldKnights,
            Map<UUID, Boolean> oldKnightFemale,
            boolean defaultToLord
    ) {
        if (personId == null || personId.equals(capital.getSovereign())) {
            return;
        }

        if (capital.isDisinheritedRoyalChild(personId)) {
            return;
        }

        if (oldRoyalChildren.contains(personId)) {
            capital.addRoyalChild(personId, oldRoyalChildFemale.getOrDefault(personId, MCAIntegrationBridge.isFemale(level, personId)));
            if (!capital.getRoyalSuccessionOrder().contains(personId)) {
                capital.getRoyalSuccessionOrder().add(personId);
            }
            return;
        }

        if (oldDukes.contains(personId)) {
            capital.addDuke(personId, oldDukeFemale.getOrDefault(personId, MCAIntegrationBridge.isFemale(level, personId)));
            return;
        }

        if (oldLords.contains(personId)) {
            capital.addLord(personId, oldLordFemale.getOrDefault(personId, MCAIntegrationBridge.isFemale(level, personId)));
            return;
        }

        if (oldKnights.contains(personId)) {
            capital.addKnight(personId, oldKnightFemale.getOrDefault(personId, MCAIntegrationBridge.isFemale(level, personId)));
            return;
        }

        if (defaultToLord && MCAIntegrationBridge.hasFamilyNode(level, personId)) {
            capital.addLord(personId, MCAIntegrationBridge.isFemale(level, personId));
        }
    }

    private static void restoreCollateralRoyalFamily(
            ServerLevel level,
            CapitalRecord capital,
            UUID oldSovereign,
            UUID oldConsort,
            Set<UUID> oldRoyalChildren,
            Map<UUID, Boolean> oldRoyalChildFemale
    ) {
        for (UUID childId : oldRoyalChildren) {
            if (childId == null || childId.equals(capital.getSovereign())) {
                continue;
            }
            if (capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }

            boolean childOfNewSovereign = MCAIntegrationBridge.isChildOf(level, childId, capital.getSovereign());
            boolean childOfFormerConsort = oldConsort != null && MCAIntegrationBridge.isChildOf(level, childId, oldConsort);
            boolean childOfFormerSovereign = oldSovereign != null && MCAIntegrationBridge.isChildOf(level, childId, oldSovereign);
            boolean legitimized = capital.isLegitimizedRoyalChild(childId);

            if (childOfNewSovereign || childOfFormerConsort || childOfFormerSovereign || legitimized) {
                capital.addRoyalChild(childId, oldRoyalChildFemale.getOrDefault(childId, MCAIntegrationBridge.isFemale(level, childId)));
            }
        }
    }

    private static void rebuildSuccessionOrderForNewLine(
            ServerLevel level,
            CapitalRecord capital,
            UUID successor,
            Set<UUID> oldRoyalChildren,
            List<UUID> oldSuccessionOrder
    ) {
        LinkedHashSet<UUID> reordered = new LinkedHashSet<>();

        for (UUID childId : MCAIntegrationBridge.getChildren(level, successor)) {
            if (childId == null || childId.equals(successor) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (MCAIntegrationBridge.isChildOf(level, childId, successor) || capital.isLegitimizedRoyalChild(childId)) {
                reordered.add(childId);
                if (!capital.isRoyalChild(childId)) {
                    capital.addRoyalChild(childId, MCAIntegrationBridge.isFemale(level, childId));
                }
            }
        }

        for (UUID childId : oldSuccessionOrder) {
            if (childId == null || childId.equals(successor) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.isRoyalChild(childId)) {
                continue;
            }
            reordered.add(childId);
        }

        for (UUID childId : oldRoyalChildren) {
            if (childId == null || childId.equals(successor) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.isRoyalChild(childId)) {
                continue;
            }
            reordered.add(childId);
        }

        capital.getRoyalSuccessionOrder().clear();
        capital.getRoyalSuccessionOrder().addAll(reordered);
    }

    private static UUID resolveNextAbdicationHeir(
            ServerLevel level,
            CapitalRecord capital,
            UUID successor,
            Set<UUID> residents
    ) {
        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(successor) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.isRoyalChild(childId) && !capital.isLegitimizedRoyalChild(childId)) {
                continue;
            }

            boolean childOfSuccessor = MCAIntegrationBridge.isChildOf(level, childId, successor);
            if (!childOfSuccessor) {
                continue;
            }

            if (residents.contains(childId) && MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(successor) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.isRoyalChild(childId) && !capital.isLegitimizedRoyalChild(childId)) {
                continue;
            }

            boolean childOfSuccessor = MCAIntegrationBridge.isChildOf(level, childId, successor);
            if (!childOfSuccessor) {
                continue;
            }

            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(successor) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.isRoyalChild(childId) && !capital.isLegitimizedRoyalChild(childId)) {
                continue;
            }
            if (residents.contains(childId) && MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(successor) || capital.isDisinheritedRoyalChild(childId)) {
                continue;
            }
            if (!capital.isRoyalChild(childId) && !capital.isLegitimizedRoyalChild(childId)) {
                continue;
            }
            if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                return childId;
            }
        }

        return null;
    }
}