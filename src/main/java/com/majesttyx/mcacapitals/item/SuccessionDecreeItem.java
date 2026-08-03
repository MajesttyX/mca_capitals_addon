package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SuccessionDecreeItem extends Item {

    public static final String TAG_BOUND_CAPITAL_ID = "BoundCapitalId";
    public static final String TAG_BOUND_VILLAGE_ID = "BoundVillageId";
    public static final String TAG_BOUND_CAPITAL_NAME = "BoundCapitalName";

    public SuccessionDecreeItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (isBound(stack)) {
            player.sendSystemMessage(Component.literal("This decree is already bound to " + getBoundCapitalName(stack) + "."));
            return InteractionResultHolder.success(stack);
        }

        CapitalRecord capital = resolveCapitalAtPlayer(serverLevel, serverPlayer);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("Right-click within a capital to bind this decree."));
            return InteractionResultHolder.fail(stack);
        }

        bind(stack, capital, MCAIntegrationBridge.getVillageName(serverLevel, capital.getVillageId()));

        player.sendSystemMessage(Component.literal("Succession Decree bound to " + getBoundCapitalName(stack) + "."));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isBound(stack)) {
            return Component.literal("Succession Decree of " + getBoundCapitalName(stack));
        }

        return Component.literal("Blank Succession Decree");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (isBound(stack)) {
            tooltipComponents.add(Component.literal("Bound to: " + getBoundCapitalName(stack)).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("Allows the sovereign to peacefully transfer rule of this capital.").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            tooltipComponents.add(Component.literal("Right-click within a capital to bind this decree to that capital.").withStyle(ChatFormatting.GRAY));
        }

        tooltipComponents.add(Component.literal("Shift-right-click an eligible villager or player to use.").withStyle(ChatFormatting.GRAY));
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = ModItemStackData.getCustomData(stack);
        return tag.hasUUID(TAG_BOUND_CAPITAL_ID);
    }

    public static UUID getBoundCapitalId(ItemStack stack) {
        CompoundTag tag = ModItemStackData.getCustomData(stack);
        return tag.hasUUID(TAG_BOUND_CAPITAL_ID) ? tag.getUUID(TAG_BOUND_CAPITAL_ID) : null;
    }

    public static Integer getBoundVillageId(ItemStack stack) {
        CompoundTag tag = ModItemStackData.getCustomData(stack);
        return tag.contains(TAG_BOUND_VILLAGE_ID) ? tag.getInt(TAG_BOUND_VILLAGE_ID) : null;
    }

    public static String getBoundCapitalName(ItemStack stack) {
        CompoundTag tag = ModItemStackData.getCustomData(stack);
        String name = tag.getString(TAG_BOUND_CAPITAL_NAME);
        return name == null || name.isBlank() ? "Unknown Capital" : name;
    }

    private static void bind(ItemStack stack, CapitalRecord capital, String capitalName) {
        ModItemStackData.updateCustomData(stack, tag -> {
            tag.putUUID(TAG_BOUND_CAPITAL_ID, capital.getCapitalId());
            if (capital.getVillageId() != null) {
                tag.putInt(TAG_BOUND_VILLAGE_ID, capital.getVillageId());
            }
            tag.putString(TAG_BOUND_CAPITAL_NAME, capitalName == null || capitalName.isBlank() ? "Unknown Capital" : capitalName);
        });
    }

    private static CapitalRecord resolveCapitalAtPlayer(ServerLevel level, ServerPlayer player) {
        Optional<Integer> villageId = MCAIntegrationBridge.getLastSeenVillageId(level, player);
        if (villageId.isEmpty()) {
            return null;
        }

        CapitalRecord capital = CapitalManager.getCapitalByVillageId(villageId.get());
        if (capital == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isPlayerInVillage(level, player, capital.getVillageId())) {
            return null;
        }

        return capital;
    }
}