package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CapitalFoundationService {

    private CapitalFoundationService() {
    }

    public static void appointVillagerSovereign(ServerLevel level, CapitalRecord capital, UUID villagerId, boolean female) {
        if (level == null || capital == null || villagerId == null) {
            return;
        }

        UUID previous = capital.getSovereign();

        capital.setSovereign(villagerId);
        capital.setSovereignFemale(female);
        capital.setState(CapitalState.ACTIVE);
        capital.setMonarchyRejected(false);

        refreshCourt(level, capital);

        if (!villagerId.equals(previous)) {
            String title = female ? "Queen" : "King";
            String name = MCAIntegrationBridge.getEntityByUuid(level, villagerId) != null
                    ? MCAIntegrationBridge.getEntityByUuid(level, villagerId).getName().getString()
                    : villagerId.toString();

            CapitalChronicleService.addEntry(level, capital,
                    name + " was acclaimed as " + title + " of "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
        }

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    public static void appointPlayerSovereign(ServerLevel level, CapitalRecord capital, UUID playerId, boolean female) {
        if (level == null || capital == null || playerId == null) {
            return;
        }

        UUID previous = capital.getSovereign();

        capital.setSovereign(playerId);
        capital.setSovereignFemale(female);
        capital.setState(CapitalState.ACTIVE);
        capital.setMonarchyRejected(false);

        refreshCourt(level, capital);

        if (!playerId.equals(previous)) {
            String title = female ? "Queen" : "King";
            String name = level.getServer().getPlayerList().getPlayer(playerId) != null
                    ? level.getServer().getPlayerList().getPlayer(playerId).getName().getString()
                    : playerId.toString();

            CapitalChronicleService.addEntry(level, capital,
                    name + " claimed the throne as " + title + " of "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");
        }

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    public static void assignDuke(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (level == null || capital == null || villagerId == null) {
            return;
        }

        capital.addDuke(villagerId, MCAIntegrationBridge.isFemale(level, villagerId));

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalCourtBuilder.rebuildCourt(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    public static boolean abdicateSovereign(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getSovereign() == null) {
            return false;
        }

        UUID oldSovereign = capital.getSovereign();
        UUID oldConsort = capital.getConsort();

        String oldName = MCAIntegrationBridge.getEntityByUuid(level, oldSovereign) != null
                ? MCAIntegrationBridge.getEntityByUuid(level, oldSovereign).getName().getString()
                : oldSovereign.toString();

        Set<UUID> oldRoyalChildren = new HashSet<>(capital.getRoyalChildren());
        HashMap<UUID, Boolean> oldRoyalChildFemale = new HashMap<>(capital.getRoyalChildFemale());
        ArrayList<UUID> oldSuccessionOrder = new ArrayList<>(capital.getRoyalSuccessionOrder());

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        UUID successor = CapitalSuccessionService.findAbdicationSuccessor(level, capital, residents);

        if (successor == null) {
            return false;
        }

        capital.setSovereign(successor);
        capital.setSovereignFemale(MCAIntegrationBridge.isFemale(level, successor));
        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setState(CapitalState.ACTIVE);

        refreshCourt(level, capital);

        for (UUID childId : oldRoyalChildren) {
            if (childId == null || childId.equals(successor) || childId.equals(oldSovereign) || childId.equals(oldConsort)) {
                continue;
            }
            capital.addRoyalChild(childId, oldRoyalChildFemale.getOrDefault(childId, false));
        }

        capital.getRoyalSuccessionOrder().clear();
        for (UUID childId : oldSuccessionOrder) {
            if (childId == null || childId.equals(successor) || childId.equals(oldSovereign) || childId.equals(oldConsort)) {
                continue;
            }
            if (capital.getRoyalChildren().contains(childId) && !capital.getRoyalSuccessionOrder().contains(childId)) {
                capital.getRoyalSuccessionOrder().add(childId);
            }
        }
        for (UUID childId : capital.getRoyalChildren()) {
            if (!capital.getRoyalSuccessionOrder().contains(childId)) {
                capital.getRoyalSuccessionOrder().add(childId);
            }
        }

        UUID nextHeir = null;
        for (UUID childId : capital.getRoyalSuccessionOrder()) {
            if (childId == null || childId.equals(capital.getSovereign())) {
                continue;
            }
            if (residents.contains(childId) && MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                nextHeir = childId;
                break;
            }
        }
        if (nextHeir == null) {
            for (UUID childId : capital.getRoyalSuccessionOrder()) {
                if (childId == null || childId.equals(capital.getSovereign())) {
                    continue;
                }
                if (MCAIntegrationBridge.hasFamilyNode(level, childId)) {
                    nextHeir = childId;
                    break;
                }
            }
        }
        capital.setHeir(nextHeir);

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

    public static void refreshCourt(ServerLevel level, CapitalRecord capital) {
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

        Set<UUID> dukes = new HashSet<>(capital.getDukes());
        HashMap<UUID, Boolean> dukeFemale = new HashMap<>(capital.getDukeFemale());

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        CapitalCourtBuilder.rebuildCourt(level, capital, residents);

        capital.setSovereign(sovereign);
        capital.setSovereignFemale(sovereignFemale);
        capital.setDowager(dowager);
        capital.setDowagerFemale(dowagerFemale);

        capital.getDukes().clear();
        capital.getDukes().addAll(dukes);

        capital.getDukeFemale().clear();
        capital.getDukeFemale().putAll(dukeFemale);

        CapitalCourtBuilder.rebuildCourt(level, capital, residents);
        CapitalNameService.refreshCapitalNames(level, capital, residents);

        if (capital.getSovereign() != null) {
            capital.setState(CapitalState.ACTIVE);
        }

        CapitalManager.getAllCapitals().put(capital.getCapitalId(), capital);
        CapitalDataAccess.markDirty(level);
    }
}