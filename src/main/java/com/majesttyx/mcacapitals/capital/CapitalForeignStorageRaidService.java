package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarCause;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CapitalForeignStorageRaidService {

    public static final int RELATIONSHIP_PENALTY = -65;

    private static final Map<UUID, UUID> ACTIVE_INCIDENTS = new ConcurrentHashMap<>();

    private CapitalForeignStorageRaidService() {
    }


    public static StorageSnapshot snapshotBlock(
            ServerLevel level,
            net.minecraft.core.BlockPos pos
    ) {
        if (level == null || pos == null) {
            return StorageSnapshot.none();
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return StorageSnapshot.none();
        }
        CapitalRecord owner = resolveStorageOwner(level, container);
        if (owner == null) {
            return StorageSnapshot.none();
        }
        List<ItemStack> contents = new ArrayList<>();
        for (int index = 0; index < container.getContainerSize(); index++) {
            ItemStack stack = container.getItem(index);
            if (!stack.isEmpty()) {
                merge(contents, stack);
            }
        }
        return new StorageSnapshot(owner, List.copyOf(contents));
    }

    public static boolean blocksAutomatedExtraction(Container source) {
        if (source == null) {
            return false;
        }
        Set<BlockEntity> blockEntities = new LinkedHashSet<>();
        collectBlockEntities(source, blockEntities, new LinkedHashSet<>());
        for (BlockEntity blockEntity : blockEntities) {
            if (blockEntity.getLevel() instanceof ServerLevel level
                    && resolveStorageOwner(level, blockEntity) != null) {
                return true;
            }
        }
        return false;
    }

    public static void refreshIncident(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID ownerId = ACTIVE_INCIDENTS.get(player.getUUID());
        if (ownerId == null) {
            return;
        }
        CapitalRecord owner = CapitalManager.getCapital(ownerId);
        if (owner == null || !isInsideStorage(player.serverLevel(), owner, player)) {
            ACTIVE_INCIDENTS.remove(player.getUUID(), ownerId);
        }
    }

    public static StorageSnapshot snapshot(
            ServerLevel level,
            AbstractContainerMenu menu
    ) {
        if (level == null || menu == null) {
            return StorageSnapshot.none();
        }

        Set<Container> containers = Collections.newSetFromMap(new IdentityHashMap<>());
        CapitalRecord owner = null;

        for (Slot slot : menu.slots) {
            if (slot == null || slot.container == null) {
                continue;
            }

            CapitalRecord candidate = resolveStorageOwner(level, slot.container);
            if (candidate == null) {
                continue;
            }

            if (owner == null) {
                owner = candidate;
            } else if (!owner.getCapitalId().equals(candidate.getCapitalId())) {
                return StorageSnapshot.none();
            }
            containers.add(slot.container);
        }

        if (owner == null || containers.isEmpty()) {
            return StorageSnapshot.none();
        }

        List<ItemStack> contents = new ArrayList<>();
        for (Container container : containers) {
            for (int index = 0; index < container.getContainerSize(); index++) {
                ItemStack stack = container.getItem(index);
                if (!stack.isEmpty()) {
                    merge(contents, stack);
                }
            }
        }

        return new StorageSnapshot(owner, List.copyOf(contents));
    }

    public static boolean removedItem(
            StorageSnapshot before,
            StorageSnapshot after
    ) {
        if (before == null
                || after == null
                || before.owner() == null
                || after.owner() == null
                || !before.owner().getCapitalId().equals(after.owner().getCapitalId())) {
            return false;
        }

        for (ItemStack original : before.contents()) {
            int remaining = countMatching(after.contents(), original);
            if (remaining < original.getCount()) {
                return true;
            }
        }
        return false;
    }

    private static void merge(List<ItemStack> stacks, ItemStack incoming) {
        for (ItemStack existing : stacks) {
            if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                existing.grow(incoming.getCount());
                return;
            }
        }
        stacks.add(incoming.copy());
    }

    private static int countMatching(List<ItemStack> stacks, ItemStack target) {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean isForeign(
            ServerPlayer player,
            StorageSnapshot snapshot
    ) {
        if (player == null || snapshot == null || snapshot.owner() == null) {
            return false;
        }
        CapitalRecord declared = PlayerCapitalAllegianceService.getDeclaredCapital(
                player.serverLevel(),
                player.getUUID()
        );
        return declared != null
                && !declared.getCapitalId().equals(snapshot.owner().getCapitalId());
    }

    public static boolean recordRaid(
            ServerPlayer player,
            CapitalRecord storageOwner
    ) {
        if (player == null
                || storageOwner == null
                || storageOwner.getCapitalId() == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord declared = PlayerCapitalAllegianceService.getDeclaredCapital(
                level,
                player.getUUID()
        );
        if (declared == null
                || declared.getCapitalId().equals(storageOwner.getCapitalId())) {
            return false;
        }
        UUID activeOwner = ACTIVE_INCIDENTS.get(player.getUUID());
        if (storageOwner.getCapitalId().equals(activeOwner)) {
            return false;
        }

        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                declared.getCapitalId(),
                storageOwner.getCapitalId(),
                RELATIONSHIP_PENALTY,
                "Foreign Storage raided",
                declared.getCapitalId()
        );
        CapitalWarDataAccess.recordGrievance(
                level,
                storageOwner.getCapitalId(),
                declared.getCapitalId(),
                CapitalWarCause.FOREIGN_STORAGE_RAID,
                0L
        );
        ACTIVE_INCIDENTS.put(player.getUUID(), storageOwner.getCapitalId());
        return true;
    }

    private static boolean isInsideStorage(
            ServerLevel level,
            CapitalRecord capital,
            ServerPlayer player
    ) {
        if (level == null
                || capital == null
                || capital.getVillageId() == null
                || player == null) {
            return false;
        }
        for (AABB bounds : com.majesttyx.mcacapitals.util.MCAIntegrationBridge
                .getBuildingBoundsOfType(
                        level,
                        capital.getVillageId(),
                        CapitalBuildingService.STORAGE
                )) {
            if (bounds.contains(player.position())) {
                return true;
            }
        }
        return false;
    }

    private static CapitalRecord resolveStorageOwner(
            ServerLevel level,
            Container container
    ) {
        Set<BlockEntity> blockEntities = new LinkedHashSet<>();
        collectBlockEntities(container, blockEntities, new LinkedHashSet<>());
        for (BlockEntity blockEntity : blockEntities) {
            CapitalRecord owner = resolveStorageOwner(level, blockEntity);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    private static CapitalRecord resolveStorageOwner(
            ServerLevel level,
            BlockEntity blockEntity
    ) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null
                    || capital.getState() != CapitalState.ACTIVE
                    || capital.getVillageId() == null) {
                continue;
            }
            for (AABB bounds : com.majesttyx.mcacapitals.util.MCAIntegrationBridge
                    .getBuildingBoundsOfType(
                            level,
                            capital.getVillageId(),
                            CapitalBuildingService.STORAGE
                    )) {
                if (bounds.contains(
                        blockEntity.getBlockPos().getX() + 0.5D,
                        blockEntity.getBlockPos().getY() + 0.5D,
                        blockEntity.getBlockPos().getZ() + 0.5D
                )) {
                    return capital;
                }
            }
        }
        return null;
    }

    private static void collectBlockEntities(
            Object value,
            Set<BlockEntity> destination,
            Set<Object> visited
    ) {
        if (value == null || !visited.add(value)) {
            return;
        }
        if (value instanceof BlockEntity blockEntity) {
            destination.add(blockEntity);
        }
        Class<?> type = value.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!Container.class.isAssignableFrom(field.getType())
                        && !BlockEntity.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    collectBlockEntities(field.get(value), destination, visited);
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    public record StorageSnapshot(CapitalRecord owner, List<ItemStack> contents) {
        private static StorageSnapshot none() {
            return new StorageSnapshot(null, List.of());
        }
    }
}