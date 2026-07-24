package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalInterregnumRecord;
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

final class CapitalInterregnumSuccessionResolver {

    private CapitalInterregnumSuccessionResolver() {
    }

    static boolean resolve(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord interregnum
    ) {
        if (level == null
                || capital == null
                || interregnum == null
                || capital.getCapitalId() == null
                || !capital.getCapitalId().equals(
                interregnum.getCapitalId()
        )) {
            return false;
        }

        UUID deceasedSovereign =
                interregnum.getDeceasedSovereignId();

        UUID oldConsort = capital.getConsort();
        boolean oldConsortFemale = capital.isConsortFemale();

        Set<UUID> oldRoyalChildren =
                new LinkedHashSet<>(capital.getRoyalChildren());

        Map<UUID, Boolean> oldRoyalChildFemale =
                new LinkedHashMap<>(capital.getRoyalChildFemale());

        List<UUID> oldSuccessionOrder =
                new ArrayList<>(capital.getRoyalSuccessionOrder());

        Set<UUID> residents =
                CapitalResidentScanner.scanResidents(
                        level,
                        capital.getCapitalId()
                );

        UUID successor = findSuccessor(
                level,
                capital,
                residents
        );

        if (successor == null) {
            resolveVacancy(
                    level,
                    capital,
                    interregnum,
                    deceasedSovereign,
                    oldConsort,
                    oldConsortFemale,
                    oldRoyalChildren,
                    oldRoyalChildFemale,
                    oldSuccessionOrder
            );

            return true;
        }

        resolveAccession(
                level,
                capital,
                interregnum,
                deceasedSovereign,
                successor,
                oldConsort,
                oldConsortFemale,
                oldRoyalChildren,
                oldRoyalChildFemale,
                oldSuccessionOrder,
                residents
        );

        return true;
    }

    private static void resolveVacancy(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord interregnum,
            UUID deceasedSovereign,
            UUID oldConsort,
            boolean oldConsortFemale,
            Set<UUID> oldRoyalChildren,
            Map<UUID, Boolean> oldRoyalChildFemale,
            List<UUID> oldSuccessionOrder
    ) {
        capital.setSovereign(null);
        capital.setSovereignFemale(false);
        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setHeir(null);
        capital.setHeirFemale(false);
        capital.setHeirMode(CapitalRecord.HeirMode.NONE);
        capital.setState(CapitalState.PENDING);

        clearDeadPlayerSovereignState(
                level,
                capital,
                interregnum
        );

        if (isValidRelationshipPerson(
                level,
                oldConsort
        )) {
            capital.setDowager(oldConsort);
            capital.setDowagerFemale(oldConsortFemale);

            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    "The wartime interregnum ended without a valid successor. "
                            + resolveName(level, oldConsort)
                            + " remained as surviving consort while the throne stood vacant."
            );
        } else {
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    "The wartime interregnum ended without a valid successor. "
                            + CapitalDiplomaticAgreementText.capitalName(
                            level,
                            capital
                    )
                            + " fell vacant."
            );
        }

        restoreRoyalChildren(
                capital,
                deceasedSovereign,
                null,
                oldRoyalChildren,
                oldRoyalChildFemale
        );

        rebuildSuccessionOrder(
                capital,
                deceasedSovereign,
                null,
                oldSuccessionOrder
        );

        finish(level, capital);
    }

    private static void resolveAccession(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord interregnum,
            UUID deceasedSovereign,
            UUID successor,
            UUID oldConsort,
            boolean oldConsortFemale,
            Set<UUID> oldRoyalChildren,
            Map<UUID, Boolean> oldRoyalChildFemale,
            List<UUID> oldSuccessionOrder,
            Set<UUID> residents
    ) {
        boolean successorWasManualHeir =
                successor.equals(capital.getHeir())
                        && capital.getHeirMode()
                        == CapitalRecord.HeirMode.MANUAL;

        capital.setSovereign(successor);
        capital.setSovereignFemale(
                MCAIntegrationBridge.isFemale(
                        level,
                        successor
                )
        );

        if (isValidRelationshipPerson(
                level,
                oldConsort
        )
                && !oldConsort.equals(successor)) {
            capital.setDowager(oldConsort);
            capital.setDowagerFemale(oldConsortFemale);
        }

        capital.setConsort(null);
        capital.setConsortFemale(false);
        capital.setState(CapitalState.ACTIVE);

        clearDeadPlayerSovereignState(
                level,
                capital,
                interregnum
        );

        CapitalFoundationService.refreshCourt(
                level,
                capital
        );

        restoreRoyalChildren(
                capital,
                deceasedSovereign,
                successor,
                oldRoyalChildren,
                oldRoyalChildFemale
        );

        rebuildSuccessionOrder(
                capital,
                deceasedSovereign,
                successor,
                oldSuccessionOrder
        );

        UUID nextHeir = findNextHeirAfterSuccession(
                level,
                capital,
                residents
        );

        capital.setHeir(nextHeir);

        if (nextHeir != null) {
            capital.setHeirFemale(
                    capital.getRoyalChildren().contains(nextHeir)
                            ? capital.isRoyalChildFemale(nextHeir)
                            : MCAIntegrationBridge.isFemale(
                            level,
                            nextHeir
                    )
            );

            capital.setHeirMode(
                    CapitalRecord.HeirMode.DYNASTIC
            );
        } else {
            capital.setHeirFemale(false);
            capital.setHeirMode(CapitalRecord.HeirMode.NONE);
        }

        String successorName = resolveName(
                level,
                successor
        );

        String capitalName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        capital
                );

        CapitalChronicleService.addEntry(
                level,
                capital,
                successorWasManualHeir
                        ? "The wartime interregnum ended. "
                        + successorName
                        + ", previously named Heir Apparent, inherited the throne of "
                        + capitalName
                        + "."
                        : "The wartime interregnum ended. "
                        + successorName
                        + " inherited the throne of "
                        + capitalName
                        + "."
        );

        finish(level, capital);
    }

    private static UUID findSuccessor(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        UUID heir = capital.getHeir();

        if (isValidSuccessionHeir(
                level,
                capital,
                heir
        )) {
            return heir;
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (residents.contains(id)
                    && isValidSuccessionCandidate(
                    level,
                    id
            )) {
                return id;
            }
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getDukes()) {
            if (residents.contains(id)
                    && isValidSuccessionCandidate(
                    level,
                    id
            )) {
                return id;
            }
        }

        for (UUID id : capital.getDukes()) {
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getLords()) {
            if (residents.contains(id)
                    && isValidSuccessionCandidate(
                    level,
                    id
            )) {
                return id;
            }
        }

        for (UUID id : capital.getLords()) {
            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        for (UUID id : capital.getKnights()) {
            if (residents.contains(id)
                    && isValidSuccessionCandidate(
                    level,
                    id
            )) {
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

    private static UUID findNextHeirAfterSuccession(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (id == null
                    || id.equals(capital.getSovereign())
                    || capital.isDisinheritedRoyalChild(id)) {
                continue;
            }

            if (residents.contains(id)
                    && isValidSuccessionCandidate(
                    level,
                    id
            )) {
                return id;
            }
        }

        for (UUID id : orderedRoyalSuccessors(capital)) {
            if (id == null
                    || id.equals(capital.getSovereign())
                    || capital.isDisinheritedRoyalChild(id)) {
                continue;
            }

            if (isValidSuccessionCandidate(level, id)) {
                return id;
            }
        }

        return null;
    }

    private static void restoreRoyalChildren(
            CapitalRecord capital,
            UUID deceasedSovereign,
            UUID successor,
            Set<UUID> oldRoyalChildren,
            Map<UUID, Boolean> oldRoyalChildFemale
    ) {
        capital.getRoyalChildren().clear();
        capital.getRoyalChildFemale().clear();

        for (UUID royalChild : oldRoyalChildren) {
            if (royalChild == null
                    || royalChild.equals(deceasedSovereign)
                    || royalChild.equals(successor)
                    || capital.isDisinheritedRoyalChild(
                    royalChild
            )) {
                continue;
            }

            capital.addRoyalChild(
                    royalChild,
                    oldRoyalChildFemale.getOrDefault(
                            royalChild,
                            false
                    )
            );
        }
    }

    private static void rebuildSuccessionOrder(
            CapitalRecord capital,
            UUID deceasedSovereign,
            UUID successor,
            List<UUID> oldSuccessionOrder
    ) {
        capital.getRoyalSuccessionOrder().clear();

        for (UUID childId : oldSuccessionOrder) {
            if (childId == null
                    || childId.equals(deceasedSovereign)
                    || childId.equals(successor)
                    || !capital.getRoyalChildren()
                    .contains(childId)) {
                continue;
            }

            capital.getRoyalSuccessionOrder().add(childId);
        }

        for (UUID childId : capital.getRoyalChildren()) {
            if (childId != null
                    && !childId.equals(deceasedSovereign)
                    && !childId.equals(successor)
                    && !capital.getRoyalSuccessionOrder()
                    .contains(childId)) {
                capital.getRoyalSuccessionOrder().add(childId);
            }
        }
    }

    private static List<UUID> orderedRoyalSuccessors(
            CapitalRecord capital
    ) {
        LinkedHashSet<UUID> ordered =
                new LinkedHashSet<>();

        for (UUID id :
                capital.getRoyalSuccessionOrder()) {
            if (id != null
                    && !capital.isDisinheritedRoyalChild(id)) {
                ordered.add(id);
            }
        }

        for (UUID id : capital.getRoyalChildren()) {
            if (id != null
                    && !capital.isDisinheritedRoyalChild(id)) {
                ordered.add(id);
            }
        }

        return new ArrayList<>(ordered);
    }

    private static boolean isValidSuccessionHeir(
            ServerLevel level,
            CapitalRecord capital,
            UUID heir
    ) {
        if (heir == null
                || capital.isDisinheritedRoyalChild(heir)) {
            return false;
        }

        if (capital.getHeirMode()
                == CapitalRecord.HeirMode.MANUAL) {
            return isValidSuccessionCandidate(
                    level,
                    heir
            );
        }

        return (capital.getRoyalChildren().contains(heir)
                || capital.isLegitimizedRoyalChild(heir))
                && isValidSuccessionCandidate(
                level,
                heir
        );
    }

    private static boolean isValidSuccessionCandidate(
            ServerLevel level,
            UUID entityId
    ) {
        return entityId != null
                && MCAIntegrationBridge
                .hasPersistentFamilyNode(
                        level,
                        entityId
                )
                && !MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        entityId
                );
    }

    private static boolean isValidRelationshipPerson(
            ServerLevel level,
            UUID entityId
    ) {
        if (entityId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge
                .getEntityByUuid(
                        level,
                        entityId
                );

        if (entity != null) {
            return entity.isAlive()
                    && !entity.isRemoved();
        }

        return MCAIntegrationBridge
                .hasPersistentFamilyNode(
                        level,
                        entityId
                )
                && !MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        entityId
                );
    }

    private static void clearDeadPlayerSovereignState(
            ServerLevel level,
            CapitalRecord capital,
            CapitalInterregnumRecord interregnum
    ) {
        if (!interregnum.wasPlayerSovereign()) {
            return;
        }

        CapitalSovereignAppointmentService
                .clearPlayerSovereignState(capital);

        if (interregnum.getFormerPlayerSovereignId()
                != null
                && capital.getCapitalId() != null) {
            PlayerCapitalTitleService.clear(
                    level,
                    interregnum.getFormerPlayerSovereignId(),
                    capital.getCapitalId()
            );
        }
    }

    private static String resolveName(
            ServerLevel level,
            UUID entityId
    ) {
        Entity entity = MCAIntegrationBridge
                .getEntityByUuid(
                        level,
                        entityId
                );

        return entity == null
                ? entityId.toString()
                : entity.getName().getString();
    }

    private static void finish(
            ServerLevel level,
            CapitalRecord capital
    ) {
        CapitalRoyalHouseholdService
                .refreshDynasticHousehold(capital);

        CapitalCourtWatcher.clearFingerprint(
                capital.getCapitalId()
        );

        CapitalDataAccess.markDirty(level);
    }
}