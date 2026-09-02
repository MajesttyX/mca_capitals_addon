package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MCACapitals.MODID);

    public static final RegistryObject<Item> ROYAL_SCEPTER =
            ITEMS.register("royal_scepter", RoyalScepterItem::new);

    public static final RegistryObject<Item> ROYAL_DISINHERITANCE =
            ITEMS.register("royal_disinheritance", RoyalDisinheritanceItem::new);

    public static final RegistryObject<Item> LEGITIMIZATION_DECREE =
            ITEMS.register("legitimization_decree", LegitimizationDecreeItem::new);

    public static final RegistryObject<Item> DECLARATION_OF_ABDICATION =
            ITEMS.register("declaration_of_abdication", DeclarationOfAbdicationItem::new);

    public static final RegistryObject<Item> BETROTHAL_DECREE =
            ITEMS.register("betrothal_decree", BetrothalDecreeItem::new);

    public static final RegistryObject<Item> BLANK_SUCCESSION_DECREE =
            ITEMS.register("blank_succession_decree", SuccessionDecreeItem::new);

    public static final RegistryObject<Item> ROYAL_CHARTER =
            ITEMS.register("royal_charter", RoyalCharterItem::new);

    public static final RegistryObject<Item> CAPITAL_CHRONICLE =
            ITEMS.register("capital_chronicle", CapitalChronicleItem::new);

    public static final RegistryObject<Item> BOOK_OF_HOUSES =
            ITEMS.register("book_of_houses", BookOfHousesItem::new);

    public static final RegistryObject<Item> SEALED_PURSE =
            ITEMS.register("sealed_purse", SealedPurseItem::new);

    public static final RegistryObject<Item> ROYAL_PARDON =
            ITEMS.register("royal_pardon", RoyalPardonItem::new);

    public static final RegistryObject<Item> DIPLOMATIC_PACKAGE =
            ITEMS.register("diplomatic_package", DiplomaticPackageItem::new);

    public static final RegistryObject<Item> DECREE_OF_THE_HOUSE =
            ITEMS.register("decree_of_the_house", DecreeOfTheHouseItem::new);

    public static final RegistryObject<Item> HOUSE_LEDGER =
            ITEMS.register("house_ledger", HouseLedgerItem::new);

    public static final RegistryObject<Item> RED_ROYAL_SEAL =
            ITEMS.register("red_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> BLACK_ROYAL_SEAL =
            ITEMS.register("black_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> BLUE_ROYAL_SEAL =
            ITEMS.register("blue_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> GREEN_ROYAL_SEAL =
            ITEMS.register("green_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> CYAN_ROYAL_SEAL =
            ITEMS.register("cyan_royal_seal", RoyalSealItem::new);

    public static final RegistryObject<Item> PURPLE_ROYAL_SEAL =
            ITEMS.register("purple_royal_seal", RoyalSealItem::new);

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}