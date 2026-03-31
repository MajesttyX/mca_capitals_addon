package com.example.mcacapitals.item;

import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.ModDataKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.Comparator;
import java.util.UUID;

public class RoyalCharterItem extends Item {

    public RoyalCharterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RoyalCharterClient.openDecisionScreen());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static ItemStack createForCapital(ServerLevel level, CapitalRecord capital) {
        if (capital == null || capital.getVillageId() == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(ModItems.ROYAL_CHARTER.get());
        CompoundTag tag = stack.getOrCreateTag();

        tag.putString(ModDataKeys.CAPITAL_ID, capital.getCapitalId().toString());
        tag.putInt(ModDataKeys.VILLAGE_ID, capital.getVillageId());
        tag.putString(ModDataKeys.VILLAGE_NAME, MCAIntegrationBridge.getVillageName(level, capital.getVillageId()));

        ListTag candidates = new ListTag();
        MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId()).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(villagerId -> {
                    Entity entity = MCAIntegrationBridge.getEntityByUuid(level, villagerId);
                    if (!MCAIntegrationBridge.isMCAVillager(level, villagerId) || entity == null || !entity.isAlive() || entity.isRemoved()) {
                        return;
                    }

                    CompoundTag candidateTag = new CompoundTag();
                    candidateTag.putString(ModDataKeys.VILLAGER_ID, villagerId.toString());
                    candidateTag.putString(ModDataKeys.VILLAGER_NAME, entity.getName().getString());
                    candidates.add(candidateTag);
                });

        tag.put(ModDataKeys.CANDIDATES, candidates);
        return stack;
    }
}