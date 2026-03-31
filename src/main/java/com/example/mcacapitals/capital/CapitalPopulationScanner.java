package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.item.ModItems;
import com.example.mcacapitals.item.RoyalCharterItem;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.ModDataKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalPopulationScanner {

    private static final int REQUIRED_POPULATION = 25;
    private static final int FOUNDING_RADIUS = 96;
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!shouldProcessTick(event)) {
            return;
        }

        ServerLevel level = (ServerLevel) event.level;

        boolean changed = false;
        changed |= normalizeCapitalsPhase(level);
        changed |= scanForNewCapitals(level);
        markDirtyIfNeeded(level, changed);

        for (CapitalRecord capital : new ArrayList<>(CapitalManager.getAllCapitals().values())) {
            processCapital(level, capital);
        }
    }

    private boolean shouldProcessTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return false;
        }
        if (!(event.level instanceof ServerLevel)) {
            return false;
        }

        tickCounter++;
        if (tickCounter < 20) {
            return false;
        }

        tickCounter = 0;
        return true;
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
            CapitalManager.getAllCapitals().put(capitalId, capital);
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    MCAIntegrationBridge.getVillageName(level, villageId) + " rose to capital status."
            );
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

        refreshCourtState(level, capital);
        tickRoyalGuards(level, capital);
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

    private void refreshCourtState(ServerLevel level, CapitalRecord capital) {
        if (CapitalCourtWatcher.refreshIfChanged(level, capital)) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private void tickRoyalGuards(ServerLevel level, CapitalRecord capital) {
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        if (CapitalRoyalGuardService.tickRoyalGuards(level, capital, residents)) {
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
        if (hasOutstandingRoyalCharter(level, capital.getCapitalId())) return false;

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

    private boolean hasOutstandingRoyalCharter(ServerLevel level, UUID capitalId) {
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
        return false;
    }

    private boolean isRoyalCharterForCapital(ItemStack stack, UUID capitalId) {
        if (stack == null || !stack.is(ModItems.ROYAL_CHARTER.get()) || !stack.hasTag()) return false;
        String raw = stack.getTag().getString(ModDataKeys.CAPITAL_ID);
        return capitalId.toString().equals(raw);
    }

    private boolean normalizeCapitals(ServerLevel level) {
        boolean changed = false;

        for (CapitalRecord capital : new ArrayList<>(CapitalManager.getAllCapitals().values())) {
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
        for (CapitalRecord capital : CapitalManager.getAllCapitals().values()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null) continue;
            CapitalRecord existing = preferredByVillage.get(villageId);
            if (existing == null || isPreferred(capital, existing)) {
                preferredByVillage.put(villageId, capital);
            }
        }

        for (CapitalRecord capital : new ArrayList<>(CapitalManager.getAllCapitals().values())) {
            Integer villageId = capital.getVillageId();

            if (villageId == null) {
                if (capital.getSovereign() == null && capital.getConsort() == null && capital.getDowager() == null) {
                    CapitalManager.removeCapital(capital.getCapitalId());
                    CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
                    changed = true;
                }
                continue;
            }

            CapitalRecord preferred = preferredByVillage.get(villageId);
            if (preferred != null && !preferred.getCapitalId().equals(capital.getCapitalId())) {
                CapitalManager.removeCapital(capital.getCapitalId());
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
                changed = true;
            }
        }

        return changed;
    }

    private boolean isPreferred(CapitalRecord candidate, CapitalRecord existing) {
        if (candidate.getSovereign() != null && existing.getSovereign() == null) return true;
        if (candidate.getSovereign() == null && existing.getSovereign() != null) return false;
        if (candidate.getState() == CapitalState.ACTIVE && existing.getState() != CapitalState.ACTIVE) return true;
        if (candidate.getState() != CapitalState.ACTIVE && existing.getState() == CapitalState.ACTIVE) return false;

        int candidateWeight = candidate.getRoyalChildren().size() + candidate.getDukes().size() + candidate.getLords().size() + candidate.getKnights().size();
        int existingWeight = existing.getRoyalChildren().size() + existing.getDukes().size() + existing.getLords().size() + existing.getKnights().size();

        if (candidateWeight != existingWeight) return candidateWeight > existingWeight;
        return candidate.getCapitalId().toString().compareTo(existing.getCapitalId().toString()) < 0;
    }
}