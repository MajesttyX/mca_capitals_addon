package com.majesttyx.mcacapitals.capital;

import fabric.net.conczin.mca.server.world.data.Building;
import fabric.net.conczin.mca.server.world.data.Village;
import fabric.net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CapitalDiplomaticStorageService {

    private static final int TRADE_EXPORT_COUNT = 8;
    private static final int TRADE_RESERVE_COUNT = 8;

    private CapitalDiplomaticStorageService() {
    }

    public static boolean deposit(
            ServerLevel contextLevel,
            CapitalRecord capital,
            List<ItemStack> contents
    ) {
        if (contextLevel == null
                || capital == null
                || capital.getVillageId() == null
                || contents == null
                || contents.isEmpty()) {
            return false;
        }

        StorageTarget target = findVillage(
                contextLevel,
                capital
        );

        return target != null
                && queueDelivery(target, contents);
    }

    public static TradeExchangeResult exchange(
            ServerLevel contextLevel,
            CapitalRecord firstCapital,
            CapitalRecord secondCapital,
            long selectionSeed
    ) {
        if (contextLevel == null
                || firstCapital == null
                || secondCapital == null
                || firstCapital.getVillageId() == null
                || secondCapital.getVillageId() == null
                || firstCapital.getCapitalId() == null
                || secondCapital.getCapitalId() == null
                || firstCapital.getCapitalId().equals(
                secondCapital.getCapitalId()
        )) {
            return TradeExchangeResult.failure();
        }

        StorageTarget firstTarget = findVillage(
                contextLevel,
                firstCapital
        );

        StorageTarget secondTarget = findVillage(
                contextLevel,
                secondCapital
        );

        if (firstTarget == null || secondTarget == null) {
            return TradeExchangeResult.failure();
        }

        List<StorageOffer> firstOffers = collectOffers(
                firstTarget,
                selectionSeed
                        ^ firstCapital.getCapitalId()
                        .getMostSignificantBits()
        );

        List<StorageOffer> secondOffers = collectOffers(
                secondTarget,
                selectionSeed
                        ^ secondCapital.getCapitalId()
                        .getLeastSignificantBits()
        );

        OfferPair pair = selectPair(
                firstOffers,
                secondOffers,
                selectionSeed
        );

        if (pair == null
                || !pair.first().isStillValid()
                || !pair.second().isStillValid()) {
            return TradeExchangeResult.failure();
        }

        ItemStack firstExport =
                pair.first().exportStack();

        ItemStack secondExport =
                pair.second().exportStack();

        pair.first().removeExportedItems();
        pair.second().removeExportedItems();

        if (!queueDelivery(
                secondTarget,
                List.of(firstExport)
        )) {
            pair.first().restoreExportedItems(
                    firstExport
            );

            pair.second().restoreExportedItems(
                    secondExport
            );

            return TradeExchangeResult.failure();
        }

        if (!queueDelivery(
                firstTarget,
                List.of(secondExport)
        )) {
            removeQueuedDelivery(
                    secondTarget,
                    firstExport
            );

            pair.first().restoreExportedItems(
                    firstExport
            );

            pair.second().restoreExportedItems(
                    secondExport
            );

            return TradeExchangeResult.failure();
        }

        return new TradeExchangeResult(
                true,
                firstExport,
                secondExport
        );
    }

    public static ReparationsResult transferReparations(
            ServerLevel contextLevel,
            CapitalRecord losingCapital,
            CapitalRecord winningCapital,
            long selectionSeed
    ) {
        if (contextLevel == null
                || losingCapital == null
                || winningCapital == null
                || losingCapital.getVillageId() == null
                || winningCapital.getVillageId() == null) {
            return ReparationsResult.failure();
        }

        StorageTarget source = findVillage(
                contextLevel,
                losingCapital
        );
        StorageTarget destination = findVillage(
                contextLevel,
                winningCapital
        );
        if (source == null || destination == null) {
            return ReparationsResult.failure();
        }

        List<StorageOffer> offers = collectOffers(source, selectionSeed);
        List<StorageOffer> selected = new ArrayList<>();
        List<ItemStack> transferred = new ArrayList<>();

        for (StorageOffer offer : offers) {
            if (selected.size() >= 3) {
                break;
            }
            if (offer.isStillValid()) {
                selected.add(offer);
                transferred.add(offer.exportStack());
            }
        }

        if (selected.isEmpty()) {
            return ReparationsResult.failure();
        }

        for (StorageOffer offer : selected) {
            offer.removeExportedItems();
        }

        if (!queueDelivery(destination, transferred)) {
            for (int index = 0; index < selected.size(); index++) {
                selected.get(index).restoreExportedItems(
                        transferred.get(index)
                );
            }
            return ReparationsResult.failure();
        }

        return new ReparationsResult(true, List.copyOf(transferred));
    }

    private static List<StorageOffer> collectOffers(
            StorageTarget target,
            long selectionSeed
    ) {
        Set<BlockPos> storageBlocks =
                new LinkedHashSet<>();

        target.village()
                .getBuildingsOfType(
                        CapitalBuildingService.STORAGE
                )
                .map(Building::getBlockPosStream)
                .forEach(stream -> {
                    try (stream) {
                        stream.forEach(
                                storageBlocks::add
                        );
                    }
                });

        List<StorageOffer> offers =
                new ArrayList<>();

        for (BlockPos pos : storageBlocks) {
            if (!target.level().hasChunkAt(pos)) {
                continue;
            }

            BlockEntity blockEntity =
                    target.level()
                            .getBlockEntity(pos);

            if (!(blockEntity
                    instanceof ChestBlockEntity chest)) {
                continue;
            }

            collectContainerOffers(
                    chest,
                    pos,
                    offers
            );
        }

        offers.sort(
                Comparator.comparing(
                        StorageOffer::sortKey
                )
        );

        if (offers.size() > 1) {
            int offset = Math.floorMod(
                    Long.hashCode(selectionSeed),
                    offers.size()
            );

            if (offset > 0) {
                List<StorageOffer> rotated =
                        new ArrayList<>(
                                offers.size()
                        );

                rotated.addAll(
                        offers.subList(
                                offset,
                                offers.size()
                        )
                );

                rotated.addAll(
                        offers.subList(
                                0,
                                offset
                        )
                );

                return rotated;
            }
        }

        return offers;
    }

    private static void collectContainerOffers(
            Container container,
            BlockPos pos,
            List<StorageOffer> offers
    ) {
        for (int slot = 0;
             slot < container.getContainerSize();
             slot++) {
            ItemStack stack =
                    container.getItem(slot);

            if (!isEligibleTradeStack(stack)) {
                continue;
            }

            offers.add(
                    new StorageOffer(
                            container,
                            slot,
                            stack.copy(),
                            pos
                    )
            );
        }
    }

    private static boolean isEligibleTradeStack(
            ItemStack stack
    ) {
        return stack != null
                && !stack.isEmpty()
                && stack.isStackable()
                && !stack.isDamageableItem()
                && !stack.hasTag()
                && stack.getItem()
                .canFitInsideContainerItems()
                && stack.getCount()
                >= TRADE_EXPORT_COUNT
                + TRADE_RESERVE_COUNT;
    }

    private static OfferPair selectPair(
            List<StorageOffer> firstOffers,
            List<StorageOffer> secondOffers,
            long selectionSeed
    ) {
        if (firstOffers.isEmpty()
                || secondOffers.isEmpty()) {
            return null;
        }

        int firstOffset = Math.floorMod(
                Long.hashCode(selectionSeed),
                firstOffers.size()
        );

        int secondOffset = Math.floorMod(
                Long.hashCode(~selectionSeed),
                secondOffers.size()
        );

        for (int firstIndex = 0;
             firstIndex < firstOffers.size();
             firstIndex++) {
            StorageOffer first = firstOffers.get(
                    (firstOffset + firstIndex)
                            % firstOffers.size()
            );

            for (int secondIndex = 0;
                 secondIndex < secondOffers.size();
                 secondIndex++) {
                StorageOffer second =
                        secondOffers.get(
                                (secondOffset
                                        + secondIndex)
                                        % secondOffers.size()
                        );

                if (!ItemStack
                        .isSameItemSameTags(
                                first.snapshot(),
                                second.snapshot()
                        )) {
                    return new OfferPair(
                            first,
                            second
                    );
                }
            }
        }

        return null;
    }

    private static boolean queueDelivery(
            StorageTarget target,
            List<ItemStack> contents
    ) {
        boolean added = false;

        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            target.village()
                    .storageBuffer
                    .add(stack.copy());

            added = true;
        }

        if (!added) {
            return false;
        }

        target.village().markDirty();
        target.village().onEnter(
                target.level()
        );

        return true;
    }

    private static void removeQueuedDelivery(
            StorageTarget target,
            ItemStack deliveredStack
    ) {
        if (deliveredStack == null
                || deliveredStack.isEmpty()) {
            return;
        }

        int remaining =
                deliveredStack.getCount();

        for (int index =
             target.village()
                     .storageBuffer
                     .size() - 1;
             index >= 0 && remaining > 0;
             index--) {
            ItemStack buffered =
                    target.village()
                            .storageBuffer
                            .get(index);

            if (!ItemStack
                    .isSameItemSameTags(
                            buffered,
                            deliveredStack
                    )) {
                continue;
            }

            int removed = Math.min(
                    remaining,
                    buffered.getCount()
            );

            buffered.shrink(removed);
            remaining -= removed;

            if (buffered.isEmpty()) {
                target.village()
                        .storageBuffer
                        .remove(index);
            }
        }

        target.village().markDirty();
    }

    private static StorageTarget findVillage(
            ServerLevel contextLevel,
            CapitalRecord capital
    ) {
        if (contextLevel == null || capital == null || capital.getVillageId() == null) {
            return null;
        }

        ServerLevel capitalLevel = CapitalManager.getCapitalLevel(contextLevel.getServer(), capital);
        if (capitalLevel != null) {
            Village village = VillageManager.get(capitalLevel)
                    .getOrEmpty(capital.getVillageId())
                    .orElse(null);
            return village == null ? null : new StorageTarget(capitalLevel, village);
        }

        return findLegacyVillage(contextLevel, capital.getVillageId());
    }

    private static StorageTarget findLegacyVillage(
            ServerLevel contextLevel,
            int villageId
    ) {
        Village local = VillageManager.get(contextLevel)
                .getOrEmpty(villageId)
                .orElse(null);
        if (local != null) {
            return new StorageTarget(contextLevel, local);
        }

        for (ServerLevel level : contextLevel.getServer().getAllLevels()) {
            if (level == contextLevel) {
                continue;
            }
            Village village = VillageManager.get(level)
                    .getOrEmpty(villageId)
                    .orElse(null);
            if (village != null) {
                return new StorageTarget(level, village);
            }
        }
        return null;
    }

    public record ReparationsResult(
            boolean successful,
            List<ItemStack> transferredItems
    ) {
        private static ReparationsResult failure() {
            return new ReparationsResult(false, List.of());
        }
    }

    public record TradeExchangeResult(
            boolean successful,
            ItemStack firstExport,
            ItemStack secondExport
    ) {

        private static TradeExchangeResult
        failure() {
            return new TradeExchangeResult(
                    false,
                    ItemStack.EMPTY,
                    ItemStack.EMPTY
            );
        }
    }

    private record StorageTarget(
            ServerLevel level,
            Village village
    ) {
    }

    private record OfferPair(
            StorageOffer first,
            StorageOffer second
    ) {
    }

    private record StorageOffer(
            Container container,
            int slot,
            ItemStack snapshot,
            BlockPos position
    ) {

        private String sortKey() {
            return snapshot.getDescriptionId()
                    + "|"
                    + position.asLong()
                    + "|"
                    + slot;
        }

        private boolean isStillValid() {
            ItemStack current =
                    container.getItem(slot);

            return ItemStack
                    .isSameItemSameTags(
                            current,
                            snapshot
                    )
                    && current.getCount()
                    >= TRADE_EXPORT_COUNT
                    + TRADE_RESERVE_COUNT;
        }

        private ItemStack exportStack() {
            return snapshot.copyWithCount(
                    TRADE_EXPORT_COUNT
            );
        }

        private void removeExportedItems() {
            container.getItem(slot).shrink(
                    TRADE_EXPORT_COUNT
            );

            container.setChanged();
        }

        private void restoreExportedItems(
                ItemStack exported
        ) {
            if (exported == null
                    || exported.isEmpty()) {
                return;
            }

            ItemStack current =
                    container.getItem(slot);

            if (current.isEmpty()) {
                container.setItem(
                        slot,
                        exported.copy()
                );
            } else if (ItemStack
                    .isSameItemSameTags(
                            current,
                            exported
                    )) {
                current.grow(
                        exported.getCount()
                );
            }

            container.setChanged();
        }
    }
}