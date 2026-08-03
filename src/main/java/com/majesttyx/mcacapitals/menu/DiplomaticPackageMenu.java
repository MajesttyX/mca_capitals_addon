package com.majesttyx.mcacapitals.menu;

import com.majesttyx.mcacapitals.item.DiplomaticPackageItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.UUID;

public final class DiplomaticPackageMenu extends AbstractContainerMenu {

    private static final int PACKAGE_SLOT_COUNT = DiplomaticPackageItem.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = PACKAGE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final SimpleContainer packageInventory;
    private final Player player;
    private final UUID packageId;
    private final InteractionHand openingHand;
    private boolean loading;

    public DiplomaticPackageMenu(
            int containerId,
            Inventory playerInventory,
            DiplomaticPackageOpenData data
    ) {
        this(
                containerId,
                playerInventory,
                data.packageId(),
                data.mainHand()
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND
        );
    }

    public DiplomaticPackageMenu(
            int containerId,
            Inventory playerInventory,
            UUID packageId,
            InteractionHand openingHand
    ) {
        super(ModMenus.DIPLOMATIC_PACKAGE, containerId);
        this.player = playerInventory.player;
        this.packageId = packageId;
        this.openingHand = openingHand;
        this.packageInventory = new SimpleContainer(PACKAGE_SLOT_COUNT);
        loadContents();
        packageInventory.addListener(ignored -> saveContents());
        addPackageSlots();
        addPlayerInventory(playerInventory);
    }

    private void addPackageSlots() {
        int startX = 62;
        int y = 25;
        for (int slot = 0; slot < PACKAGE_SLOT_COUNT; slot++) {
            addSlot(new Slot(
                    packageInventory,
                    slot,
                    startX + slot * 18,
                    y
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DiplomaticPackageItem.mayStore(stack);
                }
            });
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventoryIndex = column + row * 9 + 9;
                addSlot(new PackageAwarePlayerSlot(
                        inventory,
                        inventoryIndex,
                        8 + column * 18,
                        58 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new PackageAwarePlayerSlot(
                    inventory,
                    column,
                    8 + column * 18,
                    116
            ));
        }
    }

    private void loadContents() {
        ItemStack packageStack = findPackageStack();
        if (packageStack.isEmpty()) {
            return;
        }
        ItemContainerContents stored = packageStack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        NonNullList<ItemStack> storedItems = NonNullList.withSize(
                PACKAGE_SLOT_COUNT,
                ItemStack.EMPTY
        );
        stored.copyInto(storedItems);
        loading = true;
        for (int slot = 0; slot < PACKAGE_SLOT_COUNT; slot++) {
            packageInventory.setItem(slot, storedItems.get(slot));
        }
        loading = false;
    }

    private void saveContents() {
        if (loading || player.level().isClientSide) {
            return;
        }
        ItemStack packageStack = findPackageStack();
        if (packageStack.isEmpty()) {
            return;
        }
        NonNullList<ItemStack> saved = NonNullList.withSize(
                PACKAGE_SLOT_COUNT,
                ItemStack.EMPTY
        );
        for (int slot = 0; slot < PACKAGE_SLOT_COUNT; slot++) {
            saved.set(slot, packageInventory.getItem(slot).copy());
        }
        packageStack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(saved)
        );
    }

    private ItemStack findPackageStack() {
        ItemStack held = player.getItemInHand(openingHand);
        if (DiplomaticPackageItem.hasPackageId(held, packageId)) {
            return held;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (DiplomaticPackageItem.hasPackageId(stack, packageId)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (DiplomaticPackageItem.hasPackageId(stack, packageId)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !findPackageStack().isEmpty();
    }

    @Override
    public void removed(Player player) {
        saveContents();
        super.removed(player);
    }

    @Override
    public void clicked(
            int slotId,
            int button,
            ClickType clickType,
            Player player
    ) {
        if (clickType == ClickType.SWAP
                && button == 40
                && DiplomaticPackageItem.hasPackageId(
                player.getOffhandItem(),
                packageId
        )) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < PACKAGE_SLOT_COUNT) {
            if (!moveItemStackTo(
                    source,
                    PLAYER_INVENTORY_START,
                    HOTBAR_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!DiplomaticPackageItem.mayStore(source)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(
                    source,
                    0,
                    PACKAGE_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }
        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, source);
        return copy;
    }

    private final class PackageAwarePlayerSlot extends Slot {

        private PackageAwarePlayerSlot(
                Inventory inventory,
                int index,
                int x,
                int y
        ) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !DiplomaticPackageItem.hasPackageId(getItem(), packageId);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !DiplomaticPackageItem.hasPackageId(stack, packageId);
        }
    }
}
