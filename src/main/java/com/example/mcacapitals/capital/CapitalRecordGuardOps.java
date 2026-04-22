package com.example.mcacapitals.capital;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordGuardOps {

    private CapitalRecordGuardOps() {
    }

    static Set<UUID> getRoyalGuards(CapitalRecord record) {
        return record.guard.royalGuards;
    }

    static Map<UUID, Boolean> getRoyalGuardFemale(CapitalRecord record) {
        return record.guard.royalGuardFemale;
    }

    static boolean isRoyalGuard(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.guard.royalGuards, id);
    }

    static boolean isRoyalGuardFemale(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.isFlaggedFemale(record.guard.royalGuardFemale, id);
    }

    static void addRoyalGuard(CapitalRecord record, UUID id, boolean female, UUID liege) {
        if (id != null) {
            CapitalRecordMembers.putMember(record.guard.royalGuards, record.guard.royalGuardFemale, id, female);
            record.guard.royalGuardLiege = liege;
        }
    }

    static void removeRoyalGuard(CapitalRecord record, UUID id) {
        if (id != null) {
            CapitalRecordMembers.removeMember(record.guard.royalGuards, record.guard.royalGuardFemale, id);
            record.guard.royalGuardPatrolling.remove(id);
            record.guard.royalGuardPatrolAnchors.remove(id);
            record.guard.royalGuardDutyModes.remove(id);
        }
    }

    static Set<UUID> getDisgracedRoyalGuards(CapitalRecord record) {
        return record.guard.disgracedRoyalGuards;
    }

    static boolean isDisgracedRoyalGuard(CapitalRecord record, UUID id) {
        return CapitalRecordMembers.containsMember(record.guard.disgracedRoyalGuards, id);
    }

    static void disgraceRoyalGuard(CapitalRecord record, UUID id) {
        if (id != null) {
            removeRoyalGuard(record, id);
            record.guard.disgracedRoyalGuards.add(id);
        }
    }

    static UUID getRoyalGuardLiege(CapitalRecord record) {
        return record.guard.royalGuardLiege;
    }

    static void setRoyalGuardLiege(CapitalRecord record, UUID royalGuardLiege) {
        record.guard.royalGuardLiege = royalGuardLiege;
    }

    static Set<UUID> getRoyalGuardPatrolling(CapitalRecord record) {
        return record.guard.royalGuardPatrolling;
    }

    static Map<UUID, CapitalRecord.GuardDutyMode> getRoyalGuardDutyModes(CapitalRecord record) {
        return record.guard.royalGuardDutyModes;
    }

    static CapitalRecord.GuardDutyMode getRoyalGuardDutyMode(CapitalRecord record, UUID id) {
        return id == null
                ? CapitalRecord.GuardDutyMode.FOLLOW_SOVEREIGN
                : record.guard.royalGuardDutyModes.getOrDefault(id, CapitalRecord.GuardDutyMode.FOLLOW_SOVEREIGN);
    }

    static void setRoyalGuardDutyMode(CapitalRecord record, UUID id, CapitalRecord.GuardDutyMode mode) {
        if (id != null) {
            CapitalRecord.GuardDutyMode resolvedMode = mode == null
                    ? CapitalRecord.GuardDutyMode.FOLLOW_SOVEREIGN
                    : mode;
            record.guard.royalGuardDutyModes.put(id, resolvedMode);
            if (resolvedMode == CapitalRecord.GuardDutyMode.PATROL_ANCHOR) {
                record.guard.royalGuardPatrolling.add(id);
            } else {
                record.guard.royalGuardPatrolling.remove(id);
            }
        }
    }

    static Map<UUID, BlockPos> getRoyalGuardPatrolAnchors(CapitalRecord record) {
        return record.guard.royalGuardPatrolAnchors;
    }

    static BlockPos getRoyalGuardPatrolAnchor(CapitalRecord record, UUID id) {
        return id == null ? null : record.guard.royalGuardPatrolAnchors.get(id);
    }

    static void setRoyalGuardPatrolAnchor(CapitalRecord record, UUID id, BlockPos anchor) {
        if (id != null) {
            if (anchor == null) {
                record.guard.royalGuardPatrolAnchors.remove(id);
            } else {
                record.guard.royalGuardPatrolAnchors.put(id, anchor);
            }
        }
    }

    static long getLastRoyalGuardPromptDay(CapitalRecord record) {
        return record.guard.lastRoyalGuardPromptDay;
    }

    static void setLastRoyalGuardPromptDay(CapitalRecord record, long value) {
        record.guard.lastRoyalGuardPromptDay = value;
    }

    static UUID getPendingPlayerGuardSelectionRequester(CapitalRecord record) {
        return record.guard.pendingPlayerGuardSelectionRequester;
    }

    static void setPendingPlayerGuardSelectionRequester(CapitalRecord record, UUID value) {
        record.guard.pendingPlayerGuardSelectionRequester = value;
    }
}