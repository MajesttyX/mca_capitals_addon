package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalRoyalBetrothalService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalSavedData;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.MCARelationshipBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BetrothalDecreeHandler {

    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final double SEARCH_RADIUS = 5.0D;

    public static boolean tryGiftBetrothalDecree(ServerPlayer player, Entity target, ItemStack held) {
        if (player == null || target == null || held == null) {
            return false;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        if (!held.is(ModItems.BETROTHAL_DECREE.get())) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            return false;
        }

        ItemStack actualHeld = player.getMainHandItem();
        if (!actualHeld.is(ModItems.BETROTHAL_DECREE.get())) {
            return false;
        }

        if (actualHeld.getCount() < 2) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.betrothal_decree_handler.you_need_two_betrothal_decrees_to_arrange_this_match"));
            return true;
        }

        UUID firstId = target.getUUID();

        if (PendingVillagerBetrothalAccess.hasPendingBetrothal(level, firstId)) {
            UUID partnerId = PendingVillagerBetrothalAccess.getPartner(level, firstId);
            Entity partner = MCAIntegrationBridge.getEntityByUuid(level, partnerId);

            if (partner != null && partner.isAlive() && !partner.isRemoved()) {
                player.sendSystemMessage(Component.translatable(
                        "mcacapitals.system.betrothal_decree_handler.already_betrothed_to",
                        target.getName(),
                        partner.getName()
                ));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "mcacapitals.system.betrothal_decree_handler.already_betrothed_elsewhere",
                        target.getName()
                ));
            }
            return true;
        }

        List<Entity> nearbyVillagers = new ArrayList<>(
                MCAIntegrationBridge.getNearbyMCAVillagers(level, target.getBoundingBox().inflate(SEARCH_RADIUS))
        );

        nearbyVillagers.removeIf(entity ->
                entity == null
                        || !entity.isAlive()
                        || entity.isRemoved()
                        || entity.getUUID().equals(firstId)
        );

        nearbyVillagers.sort(Comparator.comparingDouble(target::distanceToSqr));

        Component lastFailure = null;

        for (Entity candidate : nearbyVillagers) {
            MCARelationshipBridge.BetrothalResult validation =
                    MCARelationshipBridge.validatePendingVillagerBetrothal(level, target, candidate);

            if (!validation.success()) {
                if (validation.message() != null && !validation.message().getString().isBlank()) {
                    lastFailure = validation.message();
                }
                continue;
            }

            PendingVillagerBetrothalAccess.setPendingBetrothal(level, firstId, candidate.getUUID());

            if (!player.getAbilities().instabuild) {
                actualHeld.shrink(2);
            }

            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.system.betrothal_decree_handler.now_betrothed_to",
                    target.getName(),
                    candidate.getName()
            ));

            level.broadcastEntityEvent(target, (byte) 12);
            level.broadcastEntityEvent(candidate, (byte) 12);
            return true;
        }

        if (lastFailure == null || lastFailure.getString().isBlank()) {
            lastFailure = Component.translatable(
                    "mcacapitals.system.betrothal_decree_handler.no_suitable_nearby_villager"
            );
        }

        player.sendSystemMessage(lastFailure);
        return true;
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        CapitalRoyalBetrothalService.tickEscorts(level);

        List<PendingVillagerBetrothalSavedData.PendingPair> pairs =
                PendingVillagerBetrothalAccess.getPairs(level);

        for (PendingVillagerBetrothalSavedData.PendingPair pair : pairs) {
            UUID firstId = pair.first();
            UUID secondId = pair.second();

            if (shouldClearPendingPair(level, firstId, secondId)) {
                PendingVillagerBetrothalAccess.removePendingBetrothal(level, firstId, secondId);
                continue;
            }

            Entity firstVillager = MCAIntegrationBridge.getEntityByUuid(level, firstId);
            Entity secondVillager = MCAIntegrationBridge.getEntityByUuid(level, secondId);

            if (!isResolvableVillager(firstVillager) || !isResolvableVillager(secondVillager)) {
                continue;
            }

            if (!isAdult(level, firstId) || !isAdult(level, secondId)) {
                continue;
            }

            Integer firstVillageId =
                    MCAIntegrationBridge
                            .getVillageIdForResident(
                                    level,
                                    firstId
                            );

            Integer secondVillageId =
                    MCAIntegrationBridge
                            .getVillageIdForResident(
                                    level,
                                    secondId
                            );

            if (firstVillageId == null
                    || !firstVillageId.equals(
                    secondVillageId
            )
                    || CapitalManager.getCapitalByVillageId(
                    firstVillageId
            ) == null) {
                continue;
            }

            PendingVillagerBetrothalSavedData.RoyalEscortRecord escort =
                    PendingVillagerBetrothalAccess
                            .getRoyalEscort(
                                    level,
                                    firstId,
                                    secondId
                            );

            if (escort != null
                    && CapitalDiplomacyDataAccess
                    .getDiplomaticState(
                            level,
                            escort.originCapitalId(),
                            escort.destinationCapitalId()
                    ) == CapitalDiplomaticState.WAR) {
                continue;
            }

            MCARelationshipBridge.BetrothalResult result =
                    MCARelationshipBridge.marryVillagerToVillagerDirect(firstVillager, secondVillager);

            if (!result.success()) {
                continue;
            }

            handleMarriageResult(level, firstVillager, secondVillager);

            if (escort != null) {
                CapitalRoyalBetrothalService
                        .completeRoyalMarriage(
                                level,
                                escort,
                                firstVillager,
                                secondVillager
                        );
            }

            PendingVillagerBetrothalAccess.removePendingBetrothal(level, firstId, secondId);
        }
    }

    private boolean shouldClearPendingPair(ServerLevel level, UUID firstId, UUID secondId) {
        if (firstId == null || secondId == null) {
            return true;
        }

        if (isDeadOrGone(level, firstId) || isDeadOrGone(level, secondId)) {
            return true;
        }

        UUID firstSpouse = MCAIntegrationBridge.getSpouse(level, firstId);
        UUID secondSpouse = MCAIntegrationBridge.getSpouse(level, secondId);

        if (firstSpouse != null || secondSpouse != null) {
            return !(secondId.equals(firstSpouse) && firstId.equals(secondSpouse));
        }

        return false;
    }

    private boolean isDeadOrGone(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return true;
        }

        if (MCAIntegrationBridge.isFamilyNodeDeceased(level, entityId)) {
            return true;
        }

        if (MCAIntegrationBridge.hasPersistentFamilyNode(level, entityId)) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity == null || !entity.isAlive() || entity.isRemoved();
    }

    private boolean isResolvableVillager(Entity entity) {
        return entity != null
                && entity.isAlive()
                && !entity.isRemoved()
                && MCAIntegrationBridge.isMCAVillagerEntity(entity);
    }

    private boolean isAdult(ServerLevel level, UUID entityId) {
        return "ADULT".equalsIgnoreCase(MCAIntegrationBridge.getAgeState(level, entityId));
    }

    private void handleMarriageResult(ServerLevel level, Entity firstVillager, Entity secondVillager) {
        UUID firstId = firstVillager.getUUID();
        UUID secondId = secondVillager.getUUID();

        Integer firstVillageId = MCAIntegrationBridge.getVillageIdForResident(level, firstId);
        Integer secondVillageId = MCAIntegrationBridge.getVillageIdForResident(level, secondId);

        if (firstVillageId == null || !firstVillageId.equals(secondVillageId)) {
            return;
        }

        CapitalRecord capital = CapitalManager.getCapitalByVillageId(firstVillageId);
        if (capital == null) {
            return;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.CAPITAL_MARRIAGE,
                CapitalChronicleIdentitySnapshot.name(level, capital, firstVillager.getUUID()),
                CapitalChronicleIdentitySnapshot.name(level, capital, secondVillager.getUUID()),
                villageName
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }
}