package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.item.ModItems;
import com.example.mcacapitals.item.RoyalCharterItem;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
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

    private static final int REQUIRED_POPULATION = 30;
    private static final int FOUNDING_RADIUS = 96;
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        boolean changed = normalizeCapitals(level);

        Set<Integer> qualifyingVillageIds = MCAIntegrationBridge.getVillageIdsAtOrAbovePopulation(level, REQUIRED_POPULATION);
        for (int villageId : qualifyingVillageIds) {
            if (!CapitalManager.hasCapitalForVillageId(villageId)) {
                UUID capitalId = UUID.randomUUID();
                CapitalRecord capital = new CapitalRecord(capitalId, villageId, null, false);
                capital.setState(CapitalState.PENDING);
                CapitalManager.getAllCapitals().put(capitalId, capital);
                CapitalChronicleService.addEntry(level, capital,
                        MCAIntegrationBridge.getVillageName(level, villageId) + " rose to capital status.");
                CapitalCourtWatcher.clearFingerprint(capitalId);
                changed = true;
            }
        }

        if (changed) {
            CapitalDataAccess.markDirty(level);
        }

        for (CapitalRecord capital : new ArrayList<>(CapitalManager.getAllCapitals().values())) {
            if (capital.getVillageId() != null && !MCAIntegrationBridge.hasVillage(level, capital.getVillageId())) {
                continue;
            }

            if (capital.getSovereign() == null && !capital.isMonarchyRejected()) {
                if (issueRoyalCharterIfNeeded(level, capital)) {
                    CapitalDataAccess.markDirty(level);
                }
            }

            if (capital.getSovereign() != null) {
                if (CapitalSuccessionService.handleSuccessionIfNeeded(level, capital)) {
                    CapitalDataAccess.markDirty(level);
                    continue;
                }
            }

            if (CapitalCourtWatcher.refreshIfChanged(level, capital)) {
                CapitalDataAccess.markDirty(level);
            }
        }
    }

    private boolean issueRoyalCharterIfNeeded(ServerLevel level, CapitalRecord capital) {
        if (capital.getVillageId() == null) {
            return false;
        }

        if (hasOutstandingRoyalCharter(level, capital.getCapitalId())) {
            return false;
        }

        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());

        ServerPlayer nearest = level.players().stream()
                .filter(player -> player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) <= (double) FOUNDING_RADIUS * FOUNDING_RADIUS)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D)))
                .orElse(null);

        if (nearest == null) {
            return false;
        }

        ItemStack charter = RoyalCharterItem.createForCapital(level, capital);
        if (charter.isEmpty()) {
            return false;
        }

        boolean inserted = nearest.addItem(charter);
        if (!inserted) {
            nearest.drop(charter, false);
        }

        nearest.sendSystemMessage(net.minecraft.network.chat.Component.literal(
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
        if (stack == null || !stack.is(ModItems.ROYAL_CHARTER.get()) || !stack.hasTag()) {
            return false;
        }

        String raw = stack.getTag().getString("CapitalId");
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
            if (villageId == null) {
                continue;
            }

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
        if (candidate.getSovereign() != null && existing.getSovereign() == null) {
            return true;
        }
        if (candidate.getSovereign() == null && existing.getSovereign() != null) {
            return false;
        }

        if (candidate.getState() == CapitalState.ACTIVE && existing.getState() != CapitalState.ACTIVE) {
            return true;
        }
        if (candidate.getState() != CapitalState.ACTIVE && existing.getState() == CapitalState.ACTIVE) {
            return false;
        }

        int candidateWeight = candidate.getRoyalChildren().size()
                + candidate.getDukes().size()
                + candidate.getLords().size()
                + candidate.getKnights().size();

        int existingWeight = existing.getRoyalChildren().size()
                + existing.getDukes().size()
                + existing.getLords().size()
                + existing.getKnights().size();

        if (candidateWeight != existingWeight) {
            return candidateWeight > existingWeight;
        }

        return candidate.getCapitalId().toString().compareTo(existing.getCapitalId().toString()) < 0;
    }
}