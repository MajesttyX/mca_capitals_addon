package com.majesttyx.mcacapitals.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HouseLedgerItem extends Item {

    public HouseLedgerItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                "item.mcacapitals.house_ledger"
        );
    }
}
