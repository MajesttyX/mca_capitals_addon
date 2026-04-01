package com.example.mcacapitals.item;

import com.example.mcacapitals.MCACapitals;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MCACapitals.MODID);

    public static final RegistryObject<Item> ROYAL_SCEPTER =
            ITEMS.register("heir_scepter", RoyalScepterItem::new);

    public static final RegistryObject<Item> ROYAL_DISINHERITANCE =
            ITEMS.register("royal_disinheritance", RoyalDisinheritanceItem::new);

    public static final RegistryObject<Item> LEGITIMIZATION_DECREE =
            ITEMS.register("legitimization_decree", LegitimizationDecreeItem::new);

    public static final RegistryObject<Item> DECLARATION_OF_ABDICATION =
            ITEMS.register("declaration_of_abdication", DeclarationOfAbdicationItem::new);

    public static final RegistryObject<Item> ROYAL_CHARTER =
            ITEMS.register("royal_charter", RoyalCharterItem::new);

    public static final RegistryObject<Item> CAPITAL_CHRONICLE =
            ITEMS.register("capital_chronicle", CapitalChronicleItem::new);

    public static final RegistryObject<Item> RED_ROYAL_SEAL =
            ITEMS.register("red_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> BLACK_ROYAL_SEAL =
            ITEMS.register("black_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> BLUE_ROYAL_SEAL =
            ITEMS.register("blue_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> GREEN_ROYAL_SEAL =
            ITEMS.register("green_royal_seal", RoyalSealItem::new);

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}