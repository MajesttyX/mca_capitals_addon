package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MoveState;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalCampaignReturnService {

    private static final SavedData.Factory<StoredReturnData>
            STORAGE_FACTORY =
            new SavedData.Factory<>(
                    StoredReturnData::new,
                    StoredReturnData::load,
                    null
            );

    private CapitalCampaignReturnService() {
    }

    static boolean processRetreat(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        Village attackingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                attackingCapital
                        );

        Village defendingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (attackingVillage == null
                || defendingVillage == null) {
            return false;
        }

        returnDefendersHome(
                level,
                campaign,
                defendingCapital,
                defendingVillage
        );

        StoredReturnData storage =
                getStorage(level);

        boolean homeOccupied =
                hasPlayerInside(
                        level,
                        attackingVillage
                );

        boolean changed = false;
        int homeIndex = 0;

        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (campaign.hasAttackerReturned(
                    attackerId
            )
                    || isKnownDead(
                    level,
                    attackerId
            )) {
                continue;
            }

            if (storage.contains(
                    attackingCapital.getCapitalId(),
                    attackerId
            )) {
                campaign.markAttackerReturned(
                        attackerId
                );
                changed = true;
                continue;
            }

            if (!(MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            attackerId
                    )
                    instanceof VillagerEntityMCA attacker)
                    || !attacker.isAlive()
                    || attacker.isRemoved()) {
                continue;
            }

            restoreVisibleState(attacker);
            CapitalCampaignTargetingService
                    .clearCombatTarget(attacker);

            if (homeOccupied
                    || attackingVillage
                    .isWithinBorder(attacker)) {
                teleportHome(
                        level,
                        attackingVillage,
                        attacker,
                        homeIndex++
                );

                campaign.markAttackerReturned(
                        attackerId
                );
                changed = true;
                continue;
            }

            CompoundTag entityData =
                    new CompoundTag();

            if (!attacker.save(entityData)) {
                continue;
            }

            storage.store(
                    attackingCapital.getCapitalId(),
                    attackerId,
                    entityData
            );

            attacker.discard();

            campaign.markAttackerReturned(
                    attackerId
            );
            changed = true;
        }

        if (changed) {
            CapitalCampaignDataAccess
                    .get(level)
                    .setDirty();
        }

        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (!campaign.hasAttackerReturned(attackerId)
                    && !isKnownDead(level, attackerId)) {
                return false;
            }
        }

        return true;
    }

    static void returnDefendersHome(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital
    ) {
        if (level == null
                || campaign == null
                || defendingCapital == null) {
            return;
        }

        Village defendingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (defendingVillage == null) {
            return;
        }

        returnDefendersHome(
                level,
                campaign,
                defendingCapital,
                defendingVillage
        );
    }

    static void restoreWaitingAttackers(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        StoredReturnData storage =
                getStorage(level);

        for (UUID capitalId :
                storage.getStoredCapitalIds()) {
            CapitalRecord capital =
                    CapitalManager.getCapital(capitalId);

            if (capital == null) {
                continue;
            }

            Village village =
                    CapitalCampaignEligibilityService
                            .getVillage(
                                    level,
                                    capital
                            );

            if (village == null
                    || !hasPlayerInside(
                    level,
                    village
            )) {
                continue;
            }

            int index = 0;

            for (UUID attackerId :
                    storage.getStoredAttackerIds(
                            capitalId
                    )) {
                Entity loaded =
                        MCAIntegrationBridge
                                .findLoadedEntityByUuid(
                                        level,
                                        attackerId
                                );

                if (loaded
                        instanceof VillagerEntityMCA attacker
                        && attacker.isAlive()
                        && !attacker.isRemoved()) {
                    restoreVisibleState(attacker);
                    teleportHome(
                            level,
                            village,
                            attacker,
                            index++
                    );
                    storage.remove(
                            capitalId,
                            attackerId
                    );
                    continue;
                }

                if (restoreStoredAttacker(
                        level,
                        village,
                        storage,
                        capitalId,
                        attackerId,
                        index++
                )) {
                    storage.remove(
                            capitalId,
                            attackerId
                    );
                }
            }
        }
    }

    private static void returnDefendersHome(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital,
            Village defendingVillage
    ) {
        List<UUID> defenderIds =
                new ArrayList<>(
                        campaign.getDefenderIds()
                );

        if (campaign
                .didDefendingSovereignRefusePeace()) {
            defenderIds.addAll(
                    defendingCapital.getRoyalGuards()
            );

            if (defendingCapital.getSovereign()
                    != null) {
                defenderIds.add(
                        defendingCapital.getSovereign()
                );
            }
        }

        int index = 0;

        for (UUID defenderId : defenderIds) {
            if (!(MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            defenderId
                    )
                    instanceof VillagerEntityMCA defender)
                    || !defender.isAlive()
                    || defender.isRemoved()
                    || defendingVillage
                    .isWithinBorder(defender)) {
                continue;
            }

            restoreVisibleState(defender);
            CapitalCampaignTargetingService
                    .clearCombatTarget(defender);

            teleportHome(
                    level,
                    defendingVillage,
                    defender,
                    index++
            );
        }
    }

    private static boolean restoreStoredAttacker(
            ServerLevel level,
            Village village,
            StoredReturnData storage,
            UUID capitalId,
            UUID attackerId,
            int index
    ) {
        CompoundTag entityData =
                storage.get(
                        capitalId,
                        attackerId
                );

        if (entityData == null) {
            return false;
        }

        BlockPos destination =
                homePosition(
                        level,
                        village,
                        index
                );

        Entity entity =
                EntityType.loadEntityRecursive(
                        entityData,
                        level,
                        loaded -> {
                            loaded.moveTo(
                                    destination.getX() + 0.5D,
                                    destination.getY(),
                                    destination.getZ() + 0.5D,
                                    loaded.getYRot(),
                                    loaded.getXRot()
                            );
                            return loaded;
                        }
                );

        if (!(entity
                instanceof VillagerEntityMCA attacker)
                || !attackerId.equals(
                attacker.getUUID()
        )) {
            return false;
        }

        restoreVisibleState(attacker);

        if (!level.addWithUUID(attacker)) {
            return false;
        }

        attacker.refreshDimensions();
        return true;
    }

    private static void teleportHome(
            ServerLevel level,
            Village village,
            VillagerEntityMCA villager,
            int index
    ) {
        BlockPos destination =
                homePosition(
                        level,
                        village,
                        index
                );

        restoreVisibleState(villager);

        villager.teleportTo(
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D
        );

        villager.refreshDimensions();
        refreshClientTracking(
                level,
                villager
        );
    }

    private static BlockPos homePosition(
            ServerLevel level,
            Village village,
            int index
    ) {
        BlockPos center =
                new BlockPos(
                        village.getCenter()
                );

        int offsetX = index % 3 - 1;
        int offsetZ = index / 3 - 1;
        int x = center.getX() + offsetX * 2;
        int z = center.getZ() + offsetZ * 2;
        int y = level.getHeight(
                Heightmap.Types
                        .MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );

        BlockPos destination =
                new BlockPos(x, y, z);

        if (village.isWithinBorder(
                destination,
                0
        )) {
            return destination;
        }

        int centerY = level.getHeight(
                Heightmap.Types
                        .MOTION_BLOCKING_NO_LEAVES,
                center.getX(),
                center.getZ()
        );

        return new BlockPos(
                center.getX(),
                centerY,
                center.getZ()
        );
    }

    private static boolean hasPlayerInside(
            ServerLevel level,
            Village village
    ) {
        for (ServerPlayer player :
                level.players()) {
            if (player != null
                    && player.isAlive()
                    && !player.isSpectator()
                    && village.isWithinBorder(player)) {
                return true;
            }
        }

        return false;
    }

    private static void restoreVisibleState(
            VillagerEntityMCA villager
    ) {
        villager.getNavigation().stop();
        villager.stopUsingItem();

        if (villager.isSleeping()) {
            villager.stopSleeping();
        }

        villager.stopRiding();
        villager.setNoAi(false);
        villager.setAggressive(false);
        villager.setInvisible(false);
        villager.removeEffect(
                MobEffects.INVISIBILITY
        );
        villager.setPose(Pose.STANDING);
        villager.setDeltaMovement(Vec3.ZERO);
        villager.setPersistenceRequired();

        if (villager.getVillagerBrain()
                .getMoveState() != MoveState.MOVE) {
            villager.getVillagerBrain()
                    .setMoveState(
                            MoveState.MOVE,
                            null
                    );
        }
    }

    private static void refreshClientTracking(
            ServerLevel level,
            VillagerEntityMCA villager
    ) {
        level.getChunkSource()
                .removeEntity(villager);

        level.getChunkSource()
                .addEntity(villager);
    }

    private static boolean isKnownDead(
            ServerLevel level,
            UUID villagerId
    ) {
        if (villagerId == null) {
            return true;
        }

        if (MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        villagerId
                )) {
            return true;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                villagerId
                        );

        return entity != null
                && (!entity.isAlive()
                || entity.isRemoved());
    }

    private static StoredReturnData getStorage(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        STORAGE_FACTORY,
                        StoredReturnData.DATA_NAME
                );
    }

    private static final class StoredReturnData
            extends SavedData {

        private static final String DATA_NAME =
                "mcacapitals_campaign_return_storage";
        private static final String KEY_CAPITALS =
                "Capitals";
        private static final String KEY_CAPITAL_ID =
                "CapitalId";
        private static final String KEY_ATTACKERS =
                "Attackers";
        private static final String KEY_ATTACKER_ID =
                "AttackerId";
        private static final String KEY_ENTITY_DATA =
                "EntityData";

        private final Map<UUID, Map<UUID, CompoundTag>>
                storedAttackers =
                new LinkedHashMap<>();

        private void store(
                UUID capitalId,
                UUID attackerId,
                CompoundTag entityData
        ) {
            if (capitalId == null
                    || attackerId == null
                    || entityData == null) {
                return;
            }

            storedAttackers
                    .computeIfAbsent(
                            capitalId,
                            ignored ->
                                    new LinkedHashMap<>()
                    )
                    .put(
                            attackerId,
                            entityData.copy()
                    );

            setDirty();
        }

        private boolean contains(
                UUID capitalId,
                UUID attackerId
        ) {
            Map<UUID, CompoundTag> capital =
                    storedAttackers.get(capitalId);

            return capital != null
                    && capital.containsKey(attackerId);
        }

        private CompoundTag get(
                UUID capitalId,
                UUID attackerId
        ) {
            Map<UUID, CompoundTag> capital =
                    storedAttackers.get(capitalId);

            if (capital == null) {
                return null;
            }

            CompoundTag data =
                    capital.get(attackerId);

            return data == null
                    ? null
                    : data.copy();
        }

        private Set<UUID> getStoredCapitalIds() {
            return Set.copyOf(
                    storedAttackers.keySet()
            );
        }

        private Set<UUID> getStoredAttackerIds(
                UUID capitalId
        ) {
            Map<UUID, CompoundTag> capital =
                    storedAttackers.get(capitalId);

            return capital == null
                    ? Set.of()
                    : Set.copyOf(
                    capital.keySet()
            );
        }

        private void remove(
                UUID capitalId,
                UUID attackerId
        ) {
            Map<UUID, CompoundTag> capital =
                    storedAttackers.get(capitalId);

            if (capital == null
                    || capital.remove(attackerId)
                    == null) {
                return;
            }

            if (capital.isEmpty()) {
                storedAttackers.remove(capitalId);
            }

            setDirty();
        }

        @Override
        public CompoundTag save(
                CompoundTag tag,
                HolderLookup.Provider registries
        ) {
            ListTag capitalsTag =
                    new ListTag();

            for (Map.Entry<
                    UUID,
                    Map<UUID, CompoundTag>
                    > capitalEntry :
                    storedAttackers.entrySet()) {
                CompoundTag capitalTag =
                        new CompoundTag();

                capitalTag.putUUID(
                        KEY_CAPITAL_ID,
                        capitalEntry.getKey()
                );

                ListTag attackersTag =
                        new ListTag();

                for (Map.Entry<
                        UUID,
                        CompoundTag
                        > attackerEntry :
                        capitalEntry
                                .getValue()
                                .entrySet()) {
                    CompoundTag attackerTag =
                            new CompoundTag();

                    attackerTag.putUUID(
                            KEY_ATTACKER_ID,
                            attackerEntry.getKey()
                    );

                    attackerTag.put(
                            KEY_ENTITY_DATA,
                            attackerEntry
                                    .getValue()
                                    .copy()
                    );

                    attackersTag.add(attackerTag);
                }

                capitalTag.put(
                        KEY_ATTACKERS,
                        attackersTag
                );

                capitalsTag.add(capitalTag);
            }

            tag.put(
                    KEY_CAPITALS,
                    capitalsTag
            );

            return tag;
        }

        private static StoredReturnData load(
                CompoundTag tag,
                HolderLookup.Provider registries
        ) {
            StoredReturnData data =
                    new StoredReturnData();

            ListTag capitalsTag =
                    tag.getList(
                            KEY_CAPITALS,
                            Tag.TAG_COMPOUND
                    );

            for (Tag rawCapital :
                    capitalsTag) {
                CompoundTag capitalTag =
                        (CompoundTag) rawCapital;

                if (!capitalTag.hasUUID(
                        KEY_CAPITAL_ID
                )) {
                    continue;
                }

                UUID capitalId =
                        capitalTag.getUUID(
                                KEY_CAPITAL_ID
                        );

                Map<UUID, CompoundTag> attackers =
                        new LinkedHashMap<>();

                ListTag attackersTag =
                        capitalTag.getList(
                                KEY_ATTACKERS,
                                Tag.TAG_COMPOUND
                        );

                for (Tag rawAttacker :
                        attackersTag) {
                    CompoundTag attackerTag =
                            (CompoundTag) rawAttacker;

                    if (!attackerTag.hasUUID(
                            KEY_ATTACKER_ID
                    )
                            || !attackerTag.contains(
                            KEY_ENTITY_DATA,
                            Tag.TAG_COMPOUND
                    )) {
                        continue;
                    }

                    attackers.put(
                            attackerTag.getUUID(
                                    KEY_ATTACKER_ID
                            ),
                            attackerTag.getCompound(
                                    KEY_ENTITY_DATA
                            ).copy()
                    );
                }

                if (!attackers.isEmpty()) {
                    data.storedAttackers.put(
                            capitalId,
                            attackers
                    );
                }
            }

            return data;
        }
    }
}