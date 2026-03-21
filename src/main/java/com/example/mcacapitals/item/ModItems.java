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

    public static final RegistryObject<Item> HEIR_SCEPTER =
            ITEMS.register("heir_scepter", HeirScepterItem::new);

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

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}