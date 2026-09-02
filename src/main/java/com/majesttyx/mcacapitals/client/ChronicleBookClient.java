package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

public class ChronicleBookClient {
    private static final String BOOK_KIND_KEY = "mcacapitals_book_kind";
    private static final String BOOK_KIND_HOUSES = "houses";
    private ChronicleBookClient() {}
    public static void open(ItemStack stack) { openBook(stack); }
    public static void openBook(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || stack == null || stack.isEmpty()) return;
        ItemStack bookToOpen = stack.copy();
        if (!bookToOpen.is(Items.WRITTEN_BOOK)) return;
        boolean houseBook = isBookOfHouses(bookToOpen);
        if (!houseBook) localizeBookMetadata(bookToOpen);
        BookViewScreen.BookAccess access = BookViewScreen.BookAccess.fromItem(bookToOpen);
        if (access == null) return;
        Component title = houseBook ? Component.translatable("item.mcacapitals.book_of_houses")
                : Component.translatable("item.mcacapitals.capital_chronicle");
        minecraft.setScreen(new DoublePageBookScreen(access, title, !houseBook));
    }
    private static boolean isBookOfHouses(ItemStack stack) {
        CompoundTag data = ModItemStackData.getCustomData(stack);
        return BOOK_KIND_HOUSES.equals(data.getString(BOOK_KIND_KEY));
    }
    private static void localizeBookMetadata(ItemStack stack) {
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) return;
        CompoundTag data = ModItemStackData.getCustomData(stack);
        String stored = data.getString(ModDataKeys.VILLAGE_NAME);
        Component capitalName = stored == null || stored.isBlank() || "Unknown Village".equals(stored) || "Unknown Capital".equals(stored)
                ? Component.translatable("mcacapitals.chronicle.unknown_capital") : Component.literal(stored);
        String title = Component.translatable("mcacapitals.chronicle.book.title", capitalName).getString();
        String author = Component.translatable("mcacapitals.chronicle.book.author").getString();
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(title), author, content.generation(), content.pages(), content.resolved()));
    }
}
