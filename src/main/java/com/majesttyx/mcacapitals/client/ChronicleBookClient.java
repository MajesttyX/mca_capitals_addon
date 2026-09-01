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

    private static final String BOOK_KIND_KEY =
            "mcacapitals_book_kind";
    private static final String BOOK_KIND_HOUSES =
            "houses";

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

        boolean houseBook =
                isBookOfHouses(bookToOpen);

        if (!houseBook) {
            localizeBookMetadata(bookToOpen);
        }

        BookViewScreen.BookAccess access =
                BookViewScreen.BookAccess.fromItem(
                        bookToOpen
                );

        if (access == null) {
            return;
        }

        Component screenTitle =
                houseBook
                        ? Component.translatable(
                                "item.mcacapitals.book_of_houses"
                        )
                        : Component.translatable(
                                "item.mcacapitals.capital_chronicle"
                        );

        minecraft.setScreen(
                new DoublePageBookScreen(
                        access,
                        screenTitle,
                        !houseBook
                )
        );
    }

    private static boolean isBookOfHouses(
            ItemStack bookStack
    ) {
        CompoundTag data =
                ModItemStackData.getCustomData(
                        bookStack
                );

        return BOOK_KIND_HOUSES.equals(
                data.getString(
                        BOOK_KIND_KEY
                )
        );
    }

    private static void localizeBookMetadata(ItemStack bookStack) {
        WrittenBookContent content =
                bookStack.get(
                        DataComponents.WRITTEN_BOOK_CONTENT
                );

        if (content == null) {
            return;
        }

        CompoundTag data =
                ModItemStackData.getCustomData(
                        bookStack
                );

        String storedCapitalName =
                data.getString(
                        ModDataKeys.VILLAGE_NAME
                );

        Component capitalName =
                storedCapitalName == null
                        || storedCapitalName.isBlank()
                        || "Unknown Village".equals(
                                storedCapitalName
                        )
                        || "Unknown Capital".equals(
                                storedCapitalName
                        )
                        ? Component.translatable(
                                "mcacapitals.chronicle.unknown_capital"
                        )
                        : Component.literal(
                                storedCapitalName
                        );

        String title = Component.translatable(
                "mcacapitals.chronicle.book.title",
                capitalName
        ).getString();

        String author = Component.translatable(
                "mcacapitals.chronicle.book.author"
        ).getString();

        bookStack.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(title),
                        author,
                        content.generation(),
                        content.pages(),
                        content.resolved()
                )
        );
    }
}
