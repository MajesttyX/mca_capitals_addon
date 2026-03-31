package com.example.mcacapitals.capital;

import com.example.mcacapitals.MCACapitals;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.ModDataKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CapitalChronicleService {

    private static final int CHARS_PER_PAGE = 220;
    private static final int MAX_PAGES = 100;

    private CapitalChronicleService() {
    }

    public static void addEntry(ServerLevel level, CapitalRecord capital, String text) {
        if (level == null || capital == null || text == null || text.isBlank()) {
            return;
        }

        long day = Math.max(1L, level.getDayTime() / 24000L + 1L);
        capital.addChronicleEntry("Day " + day + ": " + text);
    }

    public static void bindChronicleItem(ServerLevel level, CapitalRecord capital, ItemStack stack) {
        if (level == null || capital == null || stack == null) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ModDataKeys.CAPITAL_ID, capital.getCapitalId().toString());
        tag.putInt(ModDataKeys.VILLAGE_ID, capital.getVillageId() == null ? -1 : capital.getVillageId());
        tag.putString(ModDataKeys.VILLAGE_NAME, MCAIntegrationBridge.getVillageName(level, capital.getVillageId()));
    }

    public static void writeChronicleBook(ServerLevel level, CapitalRecord capital, ItemStack stack) {
        if (level == null || capital == null || stack == null) {
            return;
        }

        List<String> pages = createPages(level, capital);
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ModDataKeys.BOOK_TITLE, "Chronicle of " + villageName);
        tag.putString(ModDataKeys.BOOK_AUTHOR, "The Royal Chancery");
        tag.putBoolean(ModDataKeys.BOOK_RESOLVED, true);
        tag.putInt(ModDataKeys.BOOK_GENERATION, 0);

        ListTag pageList = new ListTag();
        int count = 0;
        for (String page : pages) {
            if (count >= MAX_PAGES) {
                break;
            }
            String json = Component.Serializer.toJson(Component.literal(page));
            pageList.add(StringTag.valueOf(json));
            count++;
        }

        if (pageList.isEmpty()) {
            pageList.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("No entries."))));
        }

        tag.put(ModDataKeys.BOOK_PAGES, pageList);

        MCACapitals.LOGGER.info(
                "[CapitalChronicle] Wrote book for village '{}' with {} pages.",
                villageName,
                pageList.size()
        );
    }

    public static List<String> createPages(ServerLevel level, CapitalRecord capital) {
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        List<String> blocks = new ArrayList<>();
        blocks.add("Chronicle of " + villageName + "\n\nState: " + capital.getState());

        if (capital.getChronicleEntries().isEmpty()) {
            blocks.add("No major events have yet been recorded.");
        } else {
            blocks.addAll(capital.getChronicleEntries());
        }

        List<String> pages = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String block : blocks) {
            String addition = current.length() == 0 ? block : "\n\n" + block;

            if (current.length() + addition.length() > CHARS_PER_PAGE && current.length() > 0) {
                pages.add(current.toString());
                current = new StringBuilder(block);
            } else {
                current.append(addition);
            }
        }

        if (current.length() > 0) {
            pages.add(current.toString());
        }

        if (pages.isEmpty()) {
            pages.add("No entries.");
        }

        if (pages.size() > MAX_PAGES) {
            return new ArrayList<>(pages.subList(0, MAX_PAGES));
        }

        return pages;
    }
}