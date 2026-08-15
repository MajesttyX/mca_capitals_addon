package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.menu.DiplomaticPackageMenu;
import com.majesttyx.mcacapitals.menu.DiplomaticPackageOpenData;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public final class DiplomaticPackageItem extends Item {

    public static final int SLOT_COUNT = 3;
    private static final String PACKAGE_ID_KEY = "DiplomaticPackageId";

    public DiplomaticPackageItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .component(
                        DataComponents.CONTAINER,
                        ItemContainerContents.EMPTY
                ));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        UUID packageId = getOrCreatePackageId(stack);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new ExtendedScreenHandlerFactory<DiplomaticPackageOpenData>() {
                @Override
                public DiplomaticPackageOpenData getScreenOpeningData(ServerPlayer openingPlayer) {
                    return new DiplomaticPackageOpenData(
                            packageId,
                            hand == InteractionHand.MAIN_HAND
                    );
                }

                @Override
                public Component getDisplayName() {
                    return getName(stack);
                }

                @Override
                public AbstractContainerMenu createMenu(
                        int containerId,
                        Inventory inventory,
                        Player openingPlayer
                ) {
                    return new DiplomaticPackageMenu(
                            containerId,
                            inventory,
                            packageId,
                            hand
                    );
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("mcacapitals.system.diplomatic_package_item.diplomatic_package");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        ItemContainerContents contents = stack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        int occupied = 0;
        for (ItemStack stored : contents.nonEmptyItems()) {
            occupied++;
            tooltip.add(Component.translatable(
                    "mcacapitals.system.diplomatic_package_item.stored_item",
                    stored.getCount(),
                    stored.getHoverName()
            ).withStyle(ChatFormatting.GRAY));
        }
        if (occupied == 0) {
            tooltip.add(Component.translatable("mcacapitals.system.diplomatic_package_item.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(
                "mcacapitals.system.diplomatic_package_item.slots_filled",
                occupied,
                SLOT_COUNT
        ).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    public static UUID getOrCreatePackageId(ItemStack stack) {
        CompoundTag tag = ModItemStackData.getCustomData(stack);
        if (tag.hasUUID(PACKAGE_ID_KEY)) {
            return tag.getUUID(PACKAGE_ID_KEY);
        }
        UUID packageId = UUID.randomUUID();
        ModItemStackData.updateCustomData(
                stack,
                updated -> updated.putUUID(PACKAGE_ID_KEY, packageId)
        );
        return packageId;
    }

    public static boolean hasPackageId(ItemStack stack, UUID packageId) {
        if (stack == null
                || stack.isEmpty()
                || packageId == null
                || !stack.is(ModItems.DIPLOMATIC_PACKAGE.get())) {
            return false;
        }
        CompoundTag tag = ModItemStackData.getCustomData(stack);
        return tag.hasUUID(PACKAGE_ID_KEY)
                && packageId.equals(tag.getUUID(PACKAGE_ID_KEY));
    }

    public static boolean mayStore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.getItem().canFitInsideContainerItems()) {
            return false;
        }
        if (stack.is(ModItems.DIPLOMATIC_PACKAGE.get())) {
            return false;
        }
        if (stack.get(DataComponents.CONTAINER) != null) {
            return false;
        }
        return stack.get(DataComponents.BUNDLE_CONTENTS) == null;
    }
}
