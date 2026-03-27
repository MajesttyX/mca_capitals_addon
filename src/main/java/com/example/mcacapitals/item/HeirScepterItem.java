package com.example.mcacapitals.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HeirScepterItem extends Item {

    public HeirScepterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Royal Scepter");
    }
}