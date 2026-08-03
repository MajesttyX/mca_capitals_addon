package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ChronicleBookClient {

    private ChronicleBookClient() {
    }

    public static void open(ItemStack bookToOpen) {
        openBook(bookToOpen);
    }

    public static void openBook(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack bookToOpen = stack.copy();
        if (!bookToOpen.is(Items.WRITTEN_BOOK)) {
            return;
        }

        BookViewScreen.BookAccess access = BookViewScreen.BookAccess.fromItem(bookToOpen);
        if (access == null || access.getPageCount() <= 0) {
            access = createBookAccessFromCustomData(minecraft, bookToOpen);
        }

        if (access == null || access.getPageCount() <= 0) {
            return;
        }

        minecraft.setScreen(new BookViewScreen(access));
    }

    private static BookViewScreen.BookAccess createBookAccessFromCustomData(Minecraft minecraft, ItemStack stack) {
        if (!ModItemStackData.hasCustomData(stack)) {
            return null;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);
        ListTag pageTags = tag.getList(ModDataKeys.BOOK_PAGES, Tag.TAG_STRING);
        if (pageTags.isEmpty()) {
            return null;
        }

        List<Component> pages = new ArrayList<>();

        for (int i = 0; i < pageTags.size(); i++) {
            String rawPage = pageTags.getString(i);
            if (rawPage == null || rawPage.isBlank()) {
                continue;
            }

            pages.add(parsePage(minecraft, rawPage));
        }

        if (pages.isEmpty()) {
            return null;
        }

        return new BookViewScreen.BookAccess(pages);
    }

    private static Component parsePage(Minecraft minecraft, String rawPage) {
        if (minecraft != null && minecraft.level != null) {
            try {
                Component parsed = Component.Serializer.fromJson(rawPage, minecraft.level.registryAccess());
                if (parsed != null) {
                    return parsed;
                }
            } catch (Throwable ignored) {
            }
        }

        return Component.literal(rawPage);
    }
}