package com.example.mcacapitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ChronicleBookClient {

    private ChronicleBookClient() {
    }

    public static void openBook(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ItemStack bookToOpen = stack.copy();
        if (!bookToOpen.is(Items.WRITTEN_BOOK)) {
            return;
        }

        minecraft.setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(bookToOpen)));
    }
}