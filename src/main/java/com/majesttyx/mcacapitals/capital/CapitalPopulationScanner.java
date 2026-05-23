package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.item.RoyalCharterItem;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalPopulationScanner {

    private static final int REQUIRED_POPULATION = 15;
    private static final int FOUNDING_RADIUS = 96;
    private static final int SCAN_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!shouldProcessTick(event)) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        CapitalResidentScanner.clearCache(level);

        boolean changed = false;
        changed |= normalizeCapitalsPhase(level);
        changed |= scanForNewCapitals(level);
        markDirtyIfNeeded(level, changed);

        for (CapitalRecord capital : CapitalManager.getAllCapitalsSnapshot().values()) {
            processCapital(level, capital);
        }
    }

    private boolean shouldProcessTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return false;
        }

        long gameTime = level.getGameTime();
        return gameTime % SCAN_INTERVAL_TICKS == 0L;
    }

    private boolean normalizeCapitalsPhase(ServerLevel level) {
        return normalizeCapitals(level);
    }

    private boolean scanForNewCapitals(ServerLevel level) {
        boolean changed = false;

        Set<Integer> qualifyingVillageIds = MCAIntegrationBridge.getVillageIdsAtOrAbovePopulation(level, REQUIRED_POPULATION);
        for (int villageId : qualifyingVillageIds) {
            if (CapitalManager.hasCapitalForVillageId(villageId)) {
                continue;
            }

            UUID capitalId = UUID.randomUUID();
            CapitalRecord capital = new CapitalRecord(capitalId, villageId, null, false);
            capital.setState(CapitalState.PENDING);
            CapitalManager.putCapital(capital);
            CapitalCourtWatcher.clearFingerprint(capitalId);
            changed = true;
        }

        return changed;
    }

    private void processCapital(ServerLevel level, CapitalRecord capital) {
        if (shouldSkipCapital(level, capital)) {
            return;
        }

        issuePendingCharters(level, capital);

        if (processSuccession(level, capital)) {
            tickMourning(level, capital);
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        refreshCourtState(level, capital, residents);
        tickRecommendedBetrothals(level, capital);
        tickRoyalGuards(level, capital, residents);
        tickCommander(level, capital, residents);
        tickHand(level, capital, residents);
        tickHerald(level, capital, residents);
        tickGrandMaester(level, capital, residents);
        tickMourning(level, capital);
    }

    private boolean shouldSkipCapital(ServerLevel level, CapitalRecord capital) {
        return capital.getVillageId() != null && !MCAIntegrationBridge.hasVillage(level, capital.getVillageId());
    }

    private void issuePendingCharters(ServerLevel level, CapitalRecord capital) {
        if (capital.getSovereign() == null && !capital.isMonarchyRejected()) {
            if (issueRoyalCharterIfNeeded(level, capital)) {
                CapitalDataAccess.markDirty(level);
            }
        }
    }

    private boolean processSuccession(ServerLevel level, CapitalRecord capital) {
        if (capital.getSovereign() == null) {
            return false;
        }

        if (CapitalSuccessionService.handleSuccessionIfNeeded(level, capital)) {
            CapitalDataAccess.markDirty(level);
            return true;
        }

        return false;
    }

    private void refreshCourtState(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (CapitalCourtWatcher.refreshIfChanged(level, capital, residents)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickRecommendedBetrothals(ServerLevel level, CapitalRecord capital) {
        if (CapitalRecommendedBetrothalService.tick(level, capital)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickRoyalGuards(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (CapitalRoyalGuardService.tickRoyalGuards(level, capital, residents)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickCommander(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (CapitalCommanderService.tickCommander(level, capital, residents)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickHand(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (CapitalHandService.tickHand(level, capital, residents)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickHerald(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (CapitalHeraldService.tickHerald(level, capital, residents)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickGrandMaester(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (CapitalMaesterService.tickGrandMaester(level, capital, residents)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickMourning(ServerLevel level, CapitalRecord capital) {
        CapitalMourningService.tickMourning(level, capital);
    }

    private void markDirtyIfNeeded(ServerLevel level, boolean changed) {
        if (changed) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private boolean issueRoyalCharterIfNeeded(ServerLevel level, CapitalRecord capital) {
        if (capital.getVillageId() == null) return false;
        if (hasOutstandingRoyalCharter(level, capital)) return false;

        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        ServerPlayer nearest = level.players().stream()
                .filter(player -> CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player))
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(
                        center.getX() + 0.5D,
                        center.getY() + 0.5D,
                        center.getZ() + 0.5D
                )))
                .orElse(null);

        if (nearest == null) return false;

        ItemStack charter = RoyalCharterItem.createForCapital(level, capital);
        if (charter.isEmpty()) return false;

        boolean inserted = nearest.addItem(charter);
        if (!inserted) nearest.drop(charter, false);

        nearest.sendSystemMessage(Component.literal(
                "The people of " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
                        + " seek a sovereign. A Royal Charter has been placed in your hands."
        ));
        return true;
    }

    private boolean hasOutstandingRoyalCharter(ServerLevel level, CapitalRecord capital) {
        UUID capitalId = capital.getCapitalId();

        for (ServerPlayer player : level.players()) {
            for (ItemStack stack : player.getInventory().items) {
                if (isRoyalCharterForCapital(stack, capitalId)) {
                    return true;
                }
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (isRoyalCharterForCapital(stack, capitalId)) {
                    return true;
                }
            }
        }

        return hasDroppedRoyalCharterInCapital(level, capital);
    }

    private boolean hasDroppedRoyalCharterInCapital(ServerLevel level, CapitalRecord capital) {
        if (capital.getVillageId() == null) {
            return false;
        }

        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        AABB searchBox = new AABB(center).inflate(FOUNDING_RADIUS);

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, searchBox)) {
            if (itemEntity == null || !itemEntity.isAlive()) {
                continue;
            }

            if (isRoyalCharterForCapital(itemEntity.getItem(), capital.getCapitalId())) {
                return true;
            }
        }

        return false;
    }

    private boolean isRoyalCharterForCapital(ItemStack stack, UUID capitalId) {
        if (stack == null || !stack.is(ModItems.ROYAL_CHARTER.get()) || !ModItemStackData.hasCustomData(stack)) {
            return false;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);
        String raw = tag.getString(ModDataKeys.CAPITAL_ID);
        return capitalId.toString().equals(raw);
    }

    private boolean normalizeCapitals(ServerLevel level) {
        boolean changed = false;

        for (CapitalRecord capital : CapitalManager.getAllCapitalsSnapshot().values()) {
            if (capital.getVillageId() == null) {
                Integer resolvedVillageId = null;
                if (capital.getSovereign() != null) {
                    resolvedVillageId = MCAIntegrationBridge.getVillageIdForResident(level, capital.getSovereign());
                }
                if (resolvedVillageId == null && capital.getConsort() != null) {
                    resolvedVillageId = MCAIntegrationBridge.getVillageIdForResident(level, capital.getConsort());
                }
                if (resolvedVillageId != null) {
                    capital.setVillageId(resolvedVillageId);
                    changed = true;
                }
            }
        }

        Map<Integer, CapitalRecord> preferredByVillage = new HashMap<>();
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null) continue;
            CapitalRecord existing = preferredByVillage.get(villageId);
            if (existing == null || isPreferred(capital, existing)) {
                preferredByVillage.put(villageId, capital);
            }
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalsSnapshot().values()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null) continue;
            CapitalRecord preferred = preferredByVillage.get(villageId);
            if (preferred != null && preferred != capital) {
                CapitalManager.removeCapital(capital.getCapitalId());
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
                changed = true;
            }
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalsSnapshot().values()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null) continue;

            int population = MCAIntegrationBridge.getVillagePopulation(level, villageId);
            if (population >= REQUIRED_POPULATION && capital.getState() != CapitalState.ACTIVE) {
                capital.setState(CapitalState.ACTIVE);
                changed = true;
            }
        }

        return changed;
    }

    private boolean isPreferred(CapitalRecord candidate, CapitalRecord existing) {
        if (candidate.getState() == CapitalState.ACTIVE && existing.getState() != CapitalState.ACTIVE) {
            return true;
        }
        if (candidate.getState() != CapitalState.ACTIVE && existing.getState() == CapitalState.ACTIVE) {
            return false;
        }
        if (candidate.getSovereign() != null && existing.getSovereign() == null) {
            return true;
        }
        if (candidate.getSovereign() == null && existing.getSovereign() != null) {
            return false;
        }
        return candidate.getCapitalId().toString().compareTo(existing.getCapitalId().toString()) < 0;
    }
}