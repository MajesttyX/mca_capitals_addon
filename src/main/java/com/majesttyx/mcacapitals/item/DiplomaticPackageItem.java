package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.menu.DiplomaticPackageMenu;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DiplomaticPackageItem extends Item {

    public static final int SLOT_COUNT = 3;

    private static final String PACKAGE_ID_KEY =
            "DiplomaticPackageId";

    private static final String CONTENTS_KEY =
            "DiplomaticPackageContents";

    private static final String SLOT_KEY =
            "Slot";

    public DiplomaticPackageItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        UUID packageId = getOrCreatePackageId(stack);

        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new DiplomaticPackageMenu(
                                            containerId,
                                            inventory,
                                            packageId,
                                            hand
                                    ),
                            getName(stack)
                    ),
                    buffer -> {
                        buffer.writeUUID(packageId);
                        buffer.writeBoolean(
                                hand == InteractionHand.MAIN_HAND
                        );
                    }
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("mcacapitals.system.diplomatic_package_item.diplomatic_package");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        List<ItemStack> contents = readContents(stack);
        int occupied = 0;

        for (ItemStack stored : contents) {
            if (stored.isEmpty()) {
                continue;
            }

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
                updated -> updated.putUUID(
                        PACKAGE_ID_KEY,
                        packageId
                )
        );

        return packageId;
    }

    public static boolean hasPackageId(
            ItemStack stack,
            UUID packageId
    ) {
        if (stack == null
                || stack.isEmpty()
                || packageId == null
                || !stack.is(ModItems.DIPLOMATIC_PACKAGE.get())) {
            return false;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);

        return tag.hasUUID(PACKAGE_ID_KEY)
                && packageId.equals(
                tag.getUUID(PACKAGE_ID_KEY)
        );
    }

    public static List<ItemStack> readContents(
            ItemStack packageStack
    ) {
        List<ItemStack> contents = new ArrayList<>(SLOT_COUNT);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            contents.add(ItemStack.EMPTY);
        }

        if (packageStack == null
                || packageStack.isEmpty()
                || !packageStack.is(
                ModItems.DIPLOMATIC_PACKAGE.get()
        )) {
            return contents;
        }

        CompoundTag tag =
                ModItemStackData.getCustomData(packageStack);

        ListTag stored = tag.getList(
                CONTENTS_KEY,
                Tag.TAG_COMPOUND
        );

        for (int index = 0; index < stored.size(); index++) {
            CompoundTag itemTag = stored.getCompound(index);
            int slot = itemTag.getByte(SLOT_KEY) & 255;

            if (slot < 0 || slot >= SLOT_COUNT) {
                continue;
            }

            ItemStack stack = ItemStack.of(itemTag);

            if (mayStore(stack)) {
                contents.set(slot, stack);
            }
        }

        return contents;
    }

    public static void writeContents(
            ItemStack packageStack,
            List<ItemStack> contents
    ) {
        if (packageStack == null
                || packageStack.isEmpty()
                || !packageStack.is(
                ModItems.DIPLOMATIC_PACKAGE.get()
        )) {
            return;
        }

        ModItemStackData.updateCustomData(
                packageStack,
                tag -> {
                    ListTag stored = new ListTag();

                    for (int slot = 0;
                         slot < SLOT_COUNT;
                         slot++) {
                        ItemStack stack = contents != null
                                && slot < contents.size()
                                ? contents.get(slot)
                                : ItemStack.EMPTY;

                        if (stack == null
                                || stack.isEmpty()
                                || !mayStore(stack)) {
                            continue;
                        }

                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putByte(SLOT_KEY, (byte) slot);
                        stack.copy().save(itemTag);
                        stored.add(itemTag);
                    }

                    if (stored.isEmpty()) {
                        tag.remove(CONTENTS_KEY);
                    } else {
                        tag.put(CONTENTS_KEY, stored);
                    }
                }
        );
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

        if (stack.is(Items.BUNDLE)) {
            return false;
        }

        CompoundTag blockEntityData =
                stack.getTagElement("BlockEntityTag");

        return blockEntityData == null
                || !blockEntityData.contains(
                "Items",
                Tag.TAG_LIST
        );
    }
}