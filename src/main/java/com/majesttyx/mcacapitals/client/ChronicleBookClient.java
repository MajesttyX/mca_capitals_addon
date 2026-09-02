package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ChronicleBookClient {

    private static final String BOOK_KIND_KEY = "mcacapitals_book_kind";
    private static final String BOOK_KIND_HOUSES = "houses";

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

        boolean houseBook = isBookOfHouses(bookToOpen);
        if (!houseBook) {
            localizeBookMetadata(bookToOpen);
        }

        BookViewScreen.BookAccess access = new BookViewScreen.WrittenBookAccess(bookToOpen);
        Component screenTitle = houseBook
                ? Component.translatable("item.mcacapitals.book_of_houses")
                : Component.translatable("item.mcacapitals.capital_chronicle");

        minecraft.setScreen(new DoublePageBookScreen(
                access,
                screenTitle,
                !houseBook
        ));
    }

    private static boolean isBookOfHouses(ItemStack bookStack) {
        CompoundTag data = ModItemStackData.getCustomData(bookStack);
        return BOOK_KIND_HOUSES.equals(data.getString(BOOK_KIND_KEY));
    }

    private static void localizeBookMetadata(ItemStack bookStack) {
        CompoundTag tag = bookStack.getOrCreateTag();
        String storedCapitalName = tag.getString(ModDataKeys.VILLAGE_NAME);
        Component capitalName = storedCapitalName == null
                || storedCapitalName.isBlank()
                || "Unknown Village".equals(storedCapitalName)
                || "Unknown Capital".equals(storedCapitalName)
                ? Component.translatable("mcacapitals.chronicle.unknown_capital")
                : Component.literal(storedCapitalName);

        tag.putString(
                ModDataKeys.BOOK_TITLE,
                Component.translatable("mcacapitals.chronicle.book.title", capitalName).getString()
        );
        tag.putString(
                ModDataKeys.BOOK_AUTHOR,
                Component.translatable("mcacapitals.chronicle.book.author").getString()
        );
        bookStack.setTag(tag);
    }
}
