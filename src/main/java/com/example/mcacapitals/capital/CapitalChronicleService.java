package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CapitalChronicleService {

    private static final int CHARS_PER_PAGE = 220;

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
        tag.putString("CapitalId", capital.getCapitalId().toString());
        tag.putInt("VillageId", capital.getVillageId() == null ? -1 : capital.getVillageId());
        tag.putString("VillageName", MCAIntegrationBridge.getVillageName(level, capital.getVillageId()));
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

        return pages;
    }
}