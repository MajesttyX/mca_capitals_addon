package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class CapitalTradeProfileService {

    private CapitalTradeProfileService() {
    }

    static List<ItemStack> createShipment(
            ServerLevel contextLevel,
            CapitalRecord capital,
            long tradeCycle
    ) {
        if (contextLevel == null
                || capital == null
                || capital.getVillageId() == null
                || capital.getCapitalId() == null) {
            return List.of();
        }

        ServerLevel villageLevel = findVillageLevel(contextLevel, capital.getVillageId());
        if (villageLevel == null) {
            return List.of();
        }

        Map<Item, Integer> weights = new LinkedHashMap<>();
        addBiomeResources(villageLevel, capital, weights);
        addProfessionResources(villageLevel, capital, weights);
        addBuildingResources(villageLevel, capital, weights);

        add(weights, Items.BREAD, 2);
        add(weights, Items.WHEAT, 2);
        add(weights, Items.OAK_LOG, 2);
        add(weights, Items.COBBLESTONE, 2);

        long seed = capital.getCapitalId().getMostSignificantBits()
                ^ capital.getCapitalId().getLeastSignificantBits()
                ^ Long.rotateLeft(tradeCycle, 19);
        RandomSource random = RandomSource.create(seed);
        int desiredItems = 3 + random.nextInt(5);
        int selectionCount = Math.min(desiredItems, weights.size());
        List<ItemStack> shipment = new ArrayList<>(selectionCount);
        Map<Item, Integer> available = new LinkedHashMap<>(weights);

        for (int index = 0; index < selectionCount; index++) {
            Item selected = selectWeighted(available, random);
            if (selected == null) {
                break;
            }

            int maximum = Math.min(16, selected.getDefaultMaxStackSize());
            int count = maximum <= 1
                    ? 1
                    : 2 + random.nextInt(Math.max(1, maximum - 1));
            shipment.add(new ItemStack(selected, Math.min(16, count)));
            available.remove(selected);
        }

        return List.copyOf(shipment);
    }

    private static void addBiomeResources(
            ServerLevel level,
            CapitalRecord capital,
            Map<Item, Integer> weights
    ) {
        BlockPos center = MCAIntegrationBridge.getVillageCenter(level, capital.getVillageId());
        String biome = level.getBiome(center)
                .unwrapKey()
                .map(key -> key.location().getPath().toLowerCase(Locale.ROOT))
                .orElse("");

        if (containsAny(biome, "desert", "badlands", "eroded_badlands", "wooded_badlands")) {
            add(weights, Items.SAND, 10);
            add(weights, Items.RED_SAND, 8);
            add(weights, Items.CACTUS, 10);
            add(weights, Items.TERRACOTTA, 8);
            add(weights, Items.DEAD_BUSH, 4);
        }
        if (containsAny(biome, "savanna")) {
            add(weights, Items.ACACIA_LOG, 12);
            add(weights, Items.ACACIA_PLANKS, 8);
            add(weights, Items.LEATHER, 5);
            add(weights, Items.WHEAT, 5);
        }
        if (containsAny(biome, "taiga", "grove", "snowy", "frozen", "ice_spikes")) {
            add(weights, Items.SPRUCE_LOG, 12);
            add(weights, Items.SPRUCE_PLANKS, 8);
            add(weights, Items.SWEET_BERRIES, 10);
            add(weights, Items.SNOWBALL, 6);
            add(weights, Items.PACKED_ICE, 4);
        }
        if (containsAny(biome, "jungle", "bamboo")) {
            add(weights, Items.JUNGLE_LOG, 12);
            add(weights, Items.BAMBOO, 12);
            add(weights, Items.COCOA_BEANS, 10);
            add(weights, Items.MELON_SLICE, 8);
        }
        if (containsAny(biome, "mangrove")) {
            add(weights, Items.MANGROVE_LOG, 12);
            add(weights, Items.MANGROVE_ROOTS, 7);
            add(weights, Items.MUD, 7);
            add(weights, Items.CLAY_BALL, 5);
        } else if (containsAny(biome, "swamp")) {
            add(weights, Items.OAK_LOG, 8);
            add(weights, Items.CLAY_BALL, 8);
            add(weights, Items.SLIME_BALL, 4);
            add(weights, Items.LILY_PAD, 5);
        }
        if (containsAny(biome, "cherry")) {
            add(weights, Items.CHERRY_LOG, 12);
            add(weights, Items.CHERRY_PLANKS, 8);
            add(weights, Items.PINK_PETALS, 8);
        }
        if (containsAny(biome, "birch")) {
            add(weights, Items.BIRCH_LOG, 12);
            add(weights, Items.BIRCH_PLANKS, 8);
        }
        if (containsAny(biome, "dark_forest")) {
            add(weights, Items.DARK_OAK_LOG, 12);
            add(weights, Items.DARK_OAK_PLANKS, 8);
            add(weights, Items.RED_MUSHROOM, 6);
            add(weights, Items.BROWN_MUSHROOM, 6);
        }
        if (containsAny(biome, "ocean", "river", "beach", "shore")) {
            add(weights, Items.COD, 10);
            add(weights, Items.SALMON, 10);
            add(weights, Items.KELP, 10);
            add(weights, Items.DRIED_KELP, 7);
            add(weights, Items.PRISMARINE_SHARD, 3);
        }
        if (containsAny(biome, "plains", "meadow", "forest", "flower_forest", "sunflower")) {
            add(weights, Items.OAK_LOG, 10);
            add(weights, Items.APPLE, 7);
            add(weights, Items.WHEAT, 9);
            add(weights, Items.HAY_BLOCK, 6);
            add(weights, Items.HONEYCOMB, 4);
        }
    }

    private static void addProfessionResources(
            ServerLevel level,
            CapitalRecord capital,
            Map<Item, Integer> weights
    ) {
        for (UUID residentId : MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId())) {
            String describedProfession = MCAIntegrationBridge.describeProfession(level, residentId);
            String profession = describedProfession == null
                    ? ""
                    : describedProfession.toLowerCase(Locale.ROOT);

            if (profession.contains("farmer")) {
                add(weights, Items.WHEAT, 7);
                add(weights, Items.BREAD, 6);
                add(weights, Items.CARROT, 5);
                add(weights, Items.POTATO, 5);
                add(weights, Items.BEETROOT, 4);
            }
            if (profession.contains("fisher")) {
                add(weights, Items.COD, 7);
                add(weights, Items.SALMON, 7);
                add(weights, Items.COOKED_COD, 4);
                add(weights, Items.STRING, 3);
            }
            if (profession.contains("fletcher") || profession.contains("archer")) {
                add(weights, Items.ARROW, 7);
                add(weights, Items.FLINT, 5);
                add(weights, Items.BOW, 2);
            }
            if (profession.contains("shepherd")) {
                add(weights, Items.WHITE_WOOL, 7);
                add(weights, Items.STRING, 5);
                add(weights, Items.WHITE_CARPET, 4);
            }
            if (profession.contains("leatherworker")) {
                add(weights, Items.LEATHER, 8);
                add(weights, Items.LEATHER_CHESTPLATE, 2);
                add(weights, Items.SADDLE, 1);
            }
            if (profession.contains("butcher")) {
                add(weights, Items.COOKED_BEEF, 6);
                add(weights, Items.COOKED_PORKCHOP, 6);
                add(weights, Items.COOKED_CHICKEN, 5);
            }
            if (profession.contains("armorer")) {
                add(weights, Items.IRON_INGOT, 6);
                add(weights, Items.IRON_HELMET, 2);
                add(weights, Items.IRON_CHESTPLATE, 2);
                add(weights, Items.SHIELD, 2);
            }
            if (profession.contains("weaponsmith")) {
                add(weights, Items.IRON_INGOT, 6);
                add(weights, Items.IRON_SWORD, 2);
                add(weights, Items.IRON_AXE, 2);
            }
            if (profession.contains("toolsmith")) {
                add(weights, Items.IRON_INGOT, 6);
                add(weights, Items.IRON_PICKAXE, 2);
                add(weights, Items.IRON_SHOVEL, 2);
            }
            if (profession.contains("librarian")) {
                add(weights, Items.PAPER, 7);
                add(weights, Items.BOOK, 6);
                add(weights, Items.BOOKSHELF, 3);
            }
            if (profession.contains("cartographer")) {
                add(weights, Items.PAPER, 7);
                add(weights, Items.MAP, 4);
                add(weights, Items.COMPASS, 3);
            }
            if (profession.contains("cleric")) {
                add(weights, Items.REDSTONE, 5);
                add(weights, Items.GLOWSTONE_DUST, 4);
                add(weights, Items.EXPERIENCE_BOTTLE, 2);
            }
            if (profession.contains("mason")) {
                add(weights, Items.BRICK, 7);
                add(weights, Items.STONE, 6);
                add(weights, Items.TERRACOTTA, 5);
            }
            if (profession.contains("guard")) {
                add(weights, Items.IRON_INGOT, 4);
                add(weights, Items.SHIELD, 2);
                add(weights, Items.ARROW, 4);
            }
        }
    }

    private static void addBuildingResources(
            ServerLevel level,
            CapitalRecord capital,
            Map<Item, Integer> weights
    ) {
        int villageId = capital.getVillageId();
        addForBuilding(level, villageId, weights, "blacksmith", Items.IRON_INGOT, 10, Items.COAL, 7, Items.IRON_SWORD, 3);
        addForBuilding(level, villageId, weights, "armorer", Items.IRON_INGOT, 8, Items.IRON_HELMET, 4, Items.SHIELD, 4);
        addForBuilding(level, villageId, weights, "armory", Items.ARROW, 8, Items.SHIELD, 4, Items.IRON_CHESTPLATE, 3);
        addForBuilding(level, villageId, weights, "weaponsmith", Items.IRON_SWORD, 5, Items.IRON_AXE, 4, Items.IRON_INGOT, 8);
        addForBuilding(level, villageId, weights, "toolsmith", Items.IRON_PICKAXE, 5, Items.IRON_SHOVEL, 4, Items.IRON_INGOT, 8);
        addForBuilding(level, villageId, weights, "bakery", Items.BREAD, 12, Items.COOKIE, 7, Items.CAKE, 3);
        addForBuilding(level, villageId, weights, "library", Items.PAPER, 10, Items.BOOK, 8, Items.BOOKSHELF, 4);
        addForBuilding(level, villageId, weights, "bookkeeper", Items.PAPER, 8, Items.BOOK, 6, Items.INK_SAC, 5);
        addForBuilding(level, villageId, weights, "cartographer", Items.PAPER, 9, Items.MAP, 5, Items.COMPASS, 4);
        addForBuilding(level, villageId, weights, "fishermans_hut", Items.COD, 10, Items.SALMON, 9, Items.KELP, 8);
        addForBuilding(level, villageId, weights, "fletcher", Items.ARROW, 12, Items.FLINT, 7, Items.BOW, 4);
        addForBuilding(level, villageId, weights, "leatherworker", Items.LEATHER, 10, Items.LEATHER_CHESTPLATE, 4, Items.SADDLE, 2);
        addForBuilding(level, villageId, weights, "mason", Items.BRICK, 10, Items.STONE, 8, Items.TERRACOTTA, 6);
        addForBuilding(level, villageId, weights, "weaving_mill", Items.WHITE_WOOL, 10, Items.STRING, 8, Items.WHITE_CARPET, 6);
        addForBuilding(level, villageId, weights, "butcher", Items.COOKED_BEEF, 8, Items.COOKED_PORKCHOP, 8, Items.COOKED_CHICKEN, 7);
        addForBuilding(level, villageId, weights, "inn", Items.BREAD, 8, Items.COOKED_BEEF, 5, Items.BAKED_POTATO, 5);
        addForBuilding(level, villageId, weights, "infirmary", Items.HONEY_BOTTLE, 6, Items.GOLDEN_CARROT, 4, Items.GLISTERING_MELON_SLICE, 3);
    }

    private static void addForBuilding(
            ServerLevel level,
            int villageId,
            Map<Item, Integer> weights,
            String buildingType,
            Item first,
            int firstWeight,
            Item second,
            int secondWeight,
            Item third,
            int thirdWeight
    ) {
        int count = MCAIntegrationBridge.countBuildingsOfType(level, villageId, buildingType);
        if (count <= 0) {
            return;
        }
        add(weights, first, firstWeight * count);
        add(weights, second, secondWeight * count);
        add(weights, third, thirdWeight * count);
    }

    private static ServerLevel findVillageLevel(ServerLevel contextLevel, int villageId) {
        if (MCAIntegrationBridge.hasVillage(contextLevel, villageId)) {
            return contextLevel;
        }
        for (ServerLevel level : contextLevel.getServer().getAllLevels()) {
            if (MCAIntegrationBridge.hasVillage(level, villageId)) {
                return level;
            }
        }
        return null;
    }

    private static Item selectWeighted(Map<Item, Integer> weights, RandomSource random) {
        int total = 0;
        for (int weight : weights.values()) {
            total += Math.max(0, weight);
        }
        if (total <= 0) {
            return null;
        }

        int roll = random.nextInt(total);
        for (Map.Entry<Item, Integer> entry : weights.entrySet()) {
            roll -= Math.max(0, entry.getValue());
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void add(Map<Item, Integer> weights, Item item, int weight) {
        if (item != null && weight > 0) {
            weights.merge(item, weight, Integer::sum);
        }
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}