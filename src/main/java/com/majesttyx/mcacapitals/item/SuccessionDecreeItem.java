package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
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
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.system.succession_decree_item.already_bound_to",
                    getBoundCapitalNameComponent(stack)
            ));
            return InteractionResultHolder.success(stack);
        }

        CapitalRecord capital = resolveCapitalAtPlayer(serverLevel, serverPlayer);
        if (capital == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.succession_decree_item.right_click_within_a_capital_to_bind_this_decree"));
            return InteractionResultHolder.fail(stack);
        }

        bind(stack, capital, MCAIntegrationBridge.getVillageName(serverLevel, capital.getVillageId()));

        player.sendSystemMessage(Component.translatable(
                "mcacapitals.system.succession_decree_item.bound_to_success",
                getBoundCapitalNameComponent(stack)
        ));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isBound(stack)) {
            return Component.translatable(
                    "mcacapitals.system.succession_decree_item.bound_name",
                    getBoundCapitalNameComponent(stack)
            );
        }

        return Component.translatable("mcacapitals.system.succession_decree_item.blank_succession_decree");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (isBound(stack)) {
            tooltipComponents.add(Component.translatable(
                    "mcacapitals.system.succession_decree_item.bound_to_tooltip",
                    getBoundCapitalNameComponent(stack)
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("mcacapitals.system.succession_decree_item.allows_the_sovereign_to_peacefully_transfer_rule_of_this_capital").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            tooltipComponents.add(Component.translatable("mcacapitals.system.succession_decree_item.right_click_within_a_capital_to_bind_this_decree_to_that_capital").withStyle(ChatFormatting.GRAY));
        }

        tooltipComponents.add(Component.translatable("mcacapitals.system.succession_decree_item.shift_right_click_an_eligible_villager_or_player_to_use").withStyle(ChatFormatting.GRAY));
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_BOUND_CAPITAL_ID);
    }

    public static UUID getBoundCapitalId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_BOUND_CAPITAL_ID) ? tag.getUUID(TAG_BOUND_CAPITAL_ID) : null;
    }

    public static Integer getBoundVillageId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_BOUND_VILLAGE_ID) ? tag.getInt(TAG_BOUND_VILLAGE_ID) : null;
    }

    public static String getBoundCapitalName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        String name = tag == null ? null : tag.getString(TAG_BOUND_CAPITAL_NAME);
        return name == null || name.isBlank() ? "Unknown Capital" : name;
    }

    private static Component getBoundCapitalNameComponent(ItemStack stack) {
        String name = getBoundCapitalName(stack);
        if (name == null || name.isBlank() || "Unknown Capital".equals(name)) {
            return Component.translatable("mcacapitals.system.common.unknown_capital");
        }
        return Component.literal(name);
    }

    private static void bind(ItemStack stack, CapitalRecord capital, String capitalName) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_BOUND_CAPITAL_ID, capital.getCapitalId());
        if (capital.getVillageId() != null) {
            tag.putInt(TAG_BOUND_VILLAGE_ID, capital.getVillageId());
        }
        tag.putString(TAG_BOUND_CAPITAL_NAME, capitalName == null || capitalName.isBlank() ? "Unknown Capital" : capitalName);
    }

    private static CapitalRecord resolveCapitalAtPlayer(ServerLevel level, ServerPlayer player) {
        Optional<Integer> villageId = MCAIntegrationBridge.getLastSeenVillageId(level, player);
        if (villageId.isEmpty()) {
            return null;
        }

        CapitalRecord capital = CapitalManager.getCapitalByVillageId(level, villageId.get());
        if (capital == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isPlayerInVillage(level, player, capital.getVillageId())) {
            return null;
        }

        return capital;
    }
}