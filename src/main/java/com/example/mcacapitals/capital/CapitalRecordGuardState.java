package com.example.mcacapitals.capital;

import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalRecordGuardState {
    final Set<UUID> royalGuards = new LinkedHashSet<>();
    final Map<UUID, Boolean> royalGuardFemale = new LinkedHashMap<>();
    final Set<UUID> disgracedRoyalGuards = new LinkedHashSet<>();
    UUID royalGuardLiege;
    final Set<UUID> royalGuardPatrolling = new LinkedHashSet<>();
    final Map<UUID, BlockPos> royalGuardPatrolAnchors = new LinkedHashMap<>();
    final Map<UUID, CapitalRecord.GuardDutyMode> royalGuardDutyModes = new LinkedHashMap<>();
    long lastRoyalGuardPromptDay;
    UUID pendingPlayerGuardSelectionRequester;
}