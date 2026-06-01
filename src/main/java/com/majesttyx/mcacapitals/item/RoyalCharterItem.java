package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenPlayerHouseSetupPacket;
import com.majesttyx.mcacapitals.network.OpenRoyalCharterDecisionPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class RoyalCharterItem extends Item {

    public RoyalCharterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            openCharterFlow(serverLevel, serverPlayer, stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Used to found a monarchy in an eligible village.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Right-click to choose the first sovereign.").withStyle(ChatFormatting.GRAY));
    }

    private static void openCharterFlow(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModItemStackData.hasCustomData(stack)) {
            ModNetwork.sendToPlayer(player, new OpenRoyalCharterDecisionPacket());
            return;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);
        UUID capitalId = parseUuid(tag.getString(ModDataKeys.CAPITAL_ID));
        String villageName = tag.getString(ModDataKeys.VILLAGE_NAME);

        if (capitalId == null) {
            ModNetwork.sendToPlayer(player, new OpenRoyalCharterDecisionPacket());
            return;
        }

        if (!PlayerHouseService.hasHouse(level, player.getUUID())) {
            ModNetwork.sendToPlayer(player, new OpenPlayerHouseSetupPacket(capitalId, villageName));
            return;
        }

        ModNetwork.sendToPlayer(player, new OpenRoyalCharterDecisionPacket());
    }

    public static ItemStack createForCapital(ServerLevel level, CapitalRecord capital) {
        if (capital == null || capital.getVillageId() == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(ModItems.ROYAL_CHARTER.get());
        CompoundTag tag = new CompoundTag();

        tag.putString(ModDataKeys.CAPITAL_ID, capital.getCapitalId().toString());
        tag.putInt(ModDataKeys.VILLAGE_ID, capital.getVillageId());
        tag.putString(ModDataKeys.VILLAGE_NAME, MCAIntegrationBridge.getVillageName(level, capital.getVillageId()));

        ListTag candidates = new ListTag();
        MCAIntegrationBridge.getVillageResidentNames(level, capital.getVillageId()).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> addCandidate(candidates, entry.getKey(), entry.getValue()));

        tag.put(ModDataKeys.CANDIDATES, candidates);
        ModItemStackData.setCustomData(stack, tag);
        return stack;
    }

    private static void addCandidate(ListTag candidates, UUID villagerId, String villagerName) {
        if (candidates == null || villagerId == null) {
            return;
        }

        String resolvedName = villagerName == null || villagerName.isBlank()
                ? villagerId.toString()
                : villagerName;

        CompoundTag candidateTag = new CompoundTag();
        candidateTag.putString(ModDataKeys.VILLAGER_ID, villagerId.toString());
        candidateTag.putString(ModDataKeys.VILLAGER_NAME, resolvedName);
        candidates.add(candidateTag);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}