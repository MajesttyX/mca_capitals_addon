package com.majesttyx.mcacapitals.house;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalHouseDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CapitalHouseSuccessionService {

    private CapitalHouseSuccessionService() {
    }

    public static boolean reconcile(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return false;
        }

        boolean changed = false;

        for (CapitalHouseRecord house :
                CapitalHouseDataAccess.getHouses(
                        level,
                        capital.getCapitalId()
                )) {
            if (house == null
                    || house.getTier() == CapitalHouseTier.ROYAL) {
                continue;
            }

            changed |= reconcileHouse(
                    level,
                    house
            );
        }

        if (changed) {
            CapitalHouseDataAccess.markDirty(level);
        }

        return changed;
    }

    private static boolean reconcileHouse(
            ServerLevel level,
            CapitalHouseRecord house
    ) {
        UUID currentHead = house.getHeadId();

        if (isValidLivingBloodlineMember(
                level,
                house,
                currentHead
        )) {
            if (!house.isActive()) {
                house.setActive(true);
                house.addHistory(
                        new CapitalHouseHistoryEntry(
                                CapitalHouseHistoryType.RESTORED,
                                level.getGameTime(),
                                currentHead,
                                null
                        )
                );
                return true;
            }
            return false;
        }

        UUID successor = findBloodlineSuccessor(
                level,
                house
        );

        if (successor != null) {
            boolean changed = false;

            if (!Objects.equals(
                    house.getHeadId(),
                    successor
            )) {
                house.setHeadId(successor);
                house.addHistory(
                        new CapitalHouseHistoryEntry(
                                CapitalHouseHistoryType.HEAD_CHANGED,
                                level.getGameTime(),
                                successor,
                                null
                        )
                );
                changed = true;
            }

            if (!house.isActive()) {
                house.setActive(true);
                house.addHistory(
                        new CapitalHouseHistoryEntry(
                                CapitalHouseHistoryType.RESTORED,
                                level.getGameTime(),
                                successor,
                                null
                        )
                );
                changed = true;
            }

            return changed;
        }

        boolean changed = false;

        if (house.getHeadId() != null) {
            house.setHeadId(null);
            changed = true;
        }

        if (house.isActive()) {
            house.setActive(false);
            house.addHistory(
                    new CapitalHouseHistoryEntry(
                            CapitalHouseHistoryType.BECAME_EXTINCT,
                            level.getGameTime(),
                            lastRecordedHead(house),
                            null
                    )
            );
            changed = true;
        }

        return changed;
    }

    private static UUID findBloodlineSuccessor(
            ServerLevel level,
            CapitalHouseRecord house
    ) {
        UUID founder = house.getFounderId();
        if (founder == null) {
            return null;
        }

        List<UUID> ordered =
                new ArrayList<>();

        collectDescendantsInInheritanceOrder(
                level,
                house,
                founder,
                ordered,
                new HashSet<>()
        );

        for (UUID candidate : ordered) {
            if (isValidLivingBloodlineMember(
                    level,
                    house,
                    candidate
            )) {
                return candidate;
            }
        }

        return null;
    }

    private static void collectDescendantsInInheritanceOrder(
            ServerLevel level,
            CapitalHouseRecord house,
            UUID parent,
            List<UUID> output,
            Set<UUID> visited
    ) {
        if (parent == null || !visited.add(parent)) {
            return;
        }

        List<UUID> children =
                new ArrayList<>(
                        MCAIntegrationBridge.getChildren(
                                level,
                                parent
                        )
                );

        children.sort(
                Comparator
                        .comparingLong(
                                (UUID child) -> firstHouseEntryTime(
                                        house,
                                        child
                                )
                        )
                        .thenComparing(
                                child -> child.toString()
                        )
        );

        for (UUID child : children) {
            if (child == null) {
                continue;
            }

            if (house.isCurrentMember(child)
                    || house.isFormerMember(child)) {
                output.add(child);
            }

            collectDescendantsInInheritanceOrder(
                    level,
                    house,
                    child,
                    output,
                    visited
            );
        }
    }

    private static boolean isValidLivingBloodlineMember(
            ServerLevel level,
            CapitalHouseRecord house,
            UUID candidate
    ) {
        if (candidate == null
                || !house.isCurrentMember(candidate)) {
            return false;
        }

        if (MCAIntegrationBridge.isFamilyNodeDeceased(
                level,
                candidate
        )) {
            return false;
        }

        UUID founder = house.getFounderId();
        return founder != null
                && (candidate.equals(founder)
                || isDescendantOf(
                        level,
                        candidate,
                        founder,
                        new HashSet<>()
                ));
    }

    private static boolean isDescendantOf(
            ServerLevel level,
            UUID candidate,
            UUID ancestor,
            Set<UUID> visited
    ) {
        if (candidate == null
                || ancestor == null
                || !visited.add(candidate)) {
            return false;
        }

        for (UUID parent :
                MCAIntegrationBridge.getParents(
                        level,
                        candidate
                )) {
            if (ancestor.equals(parent)) {
                return true;
            }

            if (isDescendantOf(
                    level,
                    parent,
                    ancestor,
                    visited
            )) {
                return true;
            }
        }

        return false;
    }

    private static long firstHouseEntryTime(
            CapitalHouseRecord house,
            UUID memberId
    ) {
        long best = Long.MAX_VALUE;

        for (CapitalHouseHistoryEntry entry :
                house.getHistory()) {
            if (entry == null
                    || !Objects.equals(
                    entry.subjectId(),
                    memberId
            )) {
                continue;
            }

            if (entry.type()
                    == CapitalHouseHistoryType.MEMBER_JOINED
                    || entry.type()
                    == CapitalHouseHistoryType.FOUNDED) {
                best = Math.min(
                        best,
                        entry.gameTime()
                );
            }
        }

        return best;
    }

    private static UUID lastRecordedHead(
            CapitalHouseRecord house
    ) {
        List<CapitalHouseHistoryEntry> history =
                house.getHistory();

        for (int i = history.size() - 1;
             i >= 0;
             i--) {
            CapitalHouseHistoryEntry entry =
                    history.get(i);

            if (entry != null
                    && entry.type()
                    == CapitalHouseHistoryType.HEAD_CHANGED
                    && entry.subjectId() != null) {
                return entry.subjectId();
            }
        }

        return house.getFounderId();
    }
}
