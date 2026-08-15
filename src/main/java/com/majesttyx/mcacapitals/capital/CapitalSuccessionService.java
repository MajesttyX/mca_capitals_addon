package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
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

        if (isSurvivalPlayerSovereignDeath(level, capital)) {
            return false;
        }

        if (isValidLivingSovereign(level, sovereign)) {
            return false;
        }

        boolean oldPlayerSovereign = capital.isPlayerSovereign();
        UUID oldPlayerSovereignId = capital.getPlayerSovereignId();

        UUID oldConsort = capital.getConsort();
        boolean oldConsortFemale = capital.isConsortFemale();

        String deadName = CapitalChronicleIdentitySnapshot.name(level, capital, sovereign);
        CapitalChronicleEntry.Argument deadTitle =
                CapitalChronicleIdentitySnapshot.title(level, capital, sovereign);
        CapitalChronicleEntry.Argument deadStyle =
                CapitalChronicleIdentitySnapshot.style(level, capital, sovereign);

        Set<UUID> oldRoyalChildren = new LinkedHashSet<>(capital.getRoyalChildren());
        Map<UUID, Boolean> oldRoyalChildFemale = new LinkedHashMap<>(capital.getRoyalChildFemale());
        List<UUID> oldSuccessionOrder = new ArrayList<>(capital.getRoyalSuccessionOrder());

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        UUID successor = findSuccessor(level, capital, residents);

        CapitalMourningService.startMourning(level, capital, deadName);

        if (successor == null) {
            capital.setSovereign(null);
            capital.setSovereignFemale(false);
            capital.setConsort(null);
            capital.setConsortFemale(false);
            capital.setHeir(null);
            capital.setHeirFemale(false);
            capital.setHeirMode(CapitalRecord.HeirMode.NONE);
            capital.setState(CapitalState.PENDING);

            clearDeadPlayerSovereignState(level, capital, oldPlayerSovereign, oldPlayerSovereignId);

            if (isValidRelationshipPerson(level, oldConsort)) {
                capital.setDowager(oldConsort);
                capital.setDowagerFemale(oldConsortFemale);

                String dowagerName = CapitalChronicleIdentitySnapshot.name(level, capital, oldConsort);

                CapitalChronicleService.addEvent(
                        level,
                        capital,
                        CapitalChronicleEventId.SOVEREIGN_DIED_CONSORT_SURVIVES,
                        deadName,
                        dowagerName,
                        MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                        deadTitle,
                        deadStyle,
                        CapitalChronicleIdentitySnapshot.title(level, capital, oldConsort),
                        CapitalChronicleIdentitySnapshot.style(level, capital, oldConsort)
                );
            } else {
                CapitalChronicleService.addEvent(
                        level,
                        capital,
                        CapitalChronicleEventId.SOVEREIGN_DIED_NO_SUCCESSOR,
                        deadName,
                        MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                        deadTitle,
                        deadStyle
                );
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

            CapitalRoyalHouseholdService.refreshDynasticHousehold(capital);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
            return true;
        }

        boolean successorWasManualHeir = successor.equals(capital.getHeir()) && capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL;

        capital.setSovereign(successor);
        capital.setSovereignFemale(MCAIntegrationBridge.isFemale(level, successor));

        if (isValidRelationshipPerson(level, oldConsort) && !oldConsort.equals(successor)) {
            capital.setDowager(oldConsort);
            capital.setDowagerFemale(oldConsortFemale);
        }

        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setState(CapitalState.ACTIVE);

        clearDeadPlayerSovereignState(level, capital, oldPlayerSovereign, oldPlayerSovereignId);

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
            capital.setHeirMode(CapitalRecord.HeirMode.DYNASTIC);
        } else {
            capital.setHeirFemale(false);
            capital.setHeirMode(CapitalRecord.HeirMode.NONE);
        }

        CapitalRoyalHouseholdService.refreshDynasticHousehold(capital);

        String successorName = CapitalChronicleIdentitySnapshot.name(level, capital, successor);
        CapitalChronicleEntry.Argument successorTitle =
                CapitalChronicleIdentitySnapshot.title(level, capital, successor);
        CapitalChronicleEntry.Argument successorStyle =
                CapitalChronicleIdentitySnapshot.style(level, capital, successor);

        if (successorWasManualHeir) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.SOVEREIGN_DIED_HEIR_INHERITED,
                    deadName,
                    successorName,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                    deadTitle,
                    deadStyle,
                    successorTitle,
                    successorStyle
            );
        } else {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.SOVEREIGN_DIED_SUCCESSOR_INHERITED,
                    deadName,
                    successorName,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                    deadTitle,
                    deadStyle,
                    successorTitle,
                    successorStyle
            );
        }

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        return true;
    }

    public static UUID findAbdicationSuccessor(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        UUID heir = capital.getHeir();
        if (isValidAbdicationHeir(level, capital, heir)) {
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

        if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL) {
            if (capital.getRoyalChildren().contains(heir) || capital.isLegitimizedRoyalChild(heir)) {
                return isValidSuccessionCandidate(level, heir);
            }

            Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
            return residents.contains(heir) && isValidSuccessionCandidate(level, heir);
        }

        return (capital.getRoyalChildren().contains(heir) || capital.isLegitimizedRoyalChild(heir))
                && isValidSuccessionCandidate(level, heir);
    }

    private static UUID findSuccessor(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        UUID heir = capital.getHeir();
        if (isValidSuccessionHeir(level, capital, heir)) {
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

    private static boolean isSurvivalPlayerSovereignDeath(ServerLevel level, CapitalRecord capital) {
        return capital.isPlayerSovereign()
                && level != null
                && level.getServer() != null
                && !level.getServer().isHardcore();
    }

    private static void clearDeadPlayerSovereignState(ServerLevel level, CapitalRecord capital, boolean oldPlayerSovereign, UUID oldPlayerSovereignId) {
        if (!oldPlayerSovereign) {
            return;
        }

        CapitalSovereignAppointmentService.clearPlayerSovereignState(capital);

        if (oldPlayerSovereignId != null && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(level, oldPlayerSovereignId, capital.getCapitalId());
        }
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

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity != null) {
            return entity.isAlive() && !entity.isRemoved();
        }

        if (!MCAIntegrationBridge.hasPersistentFamilyNode(level, entityId)) {
            return false;
        }

        return !MCAIntegrationBridge.isFamilyNodeDeceased(level, entityId);
    }

    private static boolean isValidSuccessionCandidate(ServerLevel level, UUID entityId) {
        return entityId != null
                && MCAIntegrationBridge.hasPersistentFamilyNode(level, entityId)
                && !MCAIntegrationBridge.isFamilyNodeDeceased(level, entityId);
    }

    private static boolean isValidAbdicationCandidate(ServerLevel level, CapitalRecord capital, UUID entityId) {
        return entityId != null
                && !entityId.equals(capital.getSovereign())
                && !capital.isDisinheritedRoyalChild(entityId)
                && MCAIntegrationBridge.hasPersistentFamilyNode(level, entityId)
                && !MCAIntegrationBridge.isFamilyNodeDeceased(level, entityId);
    }

    private static boolean isValidSuccessionHeir(ServerLevel level, CapitalRecord capital, UUID heir) {
        if (heir == null || capital.isDisinheritedRoyalChild(heir)) {
            return false;
        }

        if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL) {
            return isValidSuccessionCandidate(level, heir);
        }

        return (capital.getRoyalChildren().contains(heir) || capital.isLegitimizedRoyalChild(heir))
                && isValidSuccessionCandidate(level, heir);
    }

    private static boolean isValidAbdicationHeir(ServerLevel level, CapitalRecord capital, UUID heir) {
        if (heir == null || heir.equals(capital.getSovereign()) || capital.isDisinheritedRoyalChild(heir)) {
            return false;
        }

        if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL) {
            return isValidAbdicationCandidate(level, capital, heir);
        }

        return (capital.getRoyalChildren().contains(heir) || capital.isLegitimizedRoyalChild(heir))
                && isValidAbdicationCandidate(level, capital, heir);
    }

    private static boolean isValidRelationshipPerson(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity != null) {
            return entity.isAlive() && !entity.isRemoved();
        }

        return MCAIntegrationBridge.hasPersistentFamilyNode(level, entityId)
                && !MCAIntegrationBridge.isFamilyNodeDeceased(level, entityId);
    }


}
