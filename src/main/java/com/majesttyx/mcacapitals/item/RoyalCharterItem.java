package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class RoyalCharterItem extends Item {

    public RoyalCharterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            RoyalCharterClient.openDecisionScreen();
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
}