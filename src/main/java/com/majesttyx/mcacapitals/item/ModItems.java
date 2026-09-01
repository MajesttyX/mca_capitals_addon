package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.MCACapitals;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MCACapitals.MODID);

    public static final DeferredItem<RoyalScepterItem> ROYAL_SCEPTER =
            ITEMS.register(
                    "royal_scepter",
                    RoyalScepterItem::new
            );

    public static final DeferredItem<RoyalDisinheritanceItem>
            ROYAL_DISINHERITANCE =
            ITEMS.register(
                    "royal_disinheritance",
                    RoyalDisinheritanceItem::new
            );

    public static final DeferredItem<LegitimizationDecreeItem>
            LEGITIMIZATION_DECREE =
            ITEMS.register(
                    "legitimization_decree",
                    LegitimizationDecreeItem::new
            );

    public static final DeferredItem<DeclarationOfAbdicationItem>
            DECLARATION_OF_ABDICATION =
            ITEMS.register(
                    "declaration_of_abdication",
                    DeclarationOfAbdicationItem::new
            );

    public static final DeferredItem<BetrothalDecreeItem>
            BETROTHAL_DECREE =
            ITEMS.register(
                    "betrothal_decree",
                    BetrothalDecreeItem::new
            );

    public static final DeferredItem<SuccessionDecreeItem>
            BLANK_SUCCESSION_DECREE =
            ITEMS.register(
                    "blank_succession_decree",
                    SuccessionDecreeItem::new
            );

    public static final DeferredItem<DecreeOfTheHouseItem>
            DECREE_OF_THE_HOUSE =
            ITEMS.register(
                    "decree_of_the_house",
                    DecreeOfTheHouseItem::new
            );

    public static final DeferredItem<HouseLedgerItem>
            HOUSE_LEDGER =
            ITEMS.register(
                    "house_ledger",
                    HouseLedgerItem::new
            );

    public static final DeferredItem<RoyalCharterItem>
            ROYAL_CHARTER =
            ITEMS.register(
                    "royal_charter",
                    RoyalCharterItem::new
            );

    public static final DeferredItem<CapitalChronicleItem>
            CAPITAL_CHRONICLE =
            ITEMS.register(
                    "capital_chronicle",
                    CapitalChronicleItem::new
            );

    public static final DeferredItem<BookOfHousesItem>
            BOOK_OF_HOUSES =
            ITEMS.register(
                    "book_of_houses",
                    BookOfHousesItem::new
            );

    public static final DeferredItem<SealedPurseItem>
            SEALED_PURSE =
            ITEMS.register(
                    "sealed_purse",
                    SealedPurseItem::new
            );

    public static final DeferredItem<RoyalPardonItem>
            ROYAL_PARDON =
            ITEMS.register(
                    "royal_pardon",
                    RoyalPardonItem::new
            );

    public static final DeferredItem<DiplomaticPackageItem>
            DIPLOMATIC_PACKAGE =
            ITEMS.register(
                    "diplomatic_package",
                    DiplomaticPackageItem::new
            );

    public static final DeferredItem<RoyalSealItem>
            RED_ROYAL_SEAL =
            ITEMS.register(
                    "red_royal_seal",
                    RoyalSealItem::new
            );

    public static final DeferredItem<RoyalSealItem>
            BLACK_ROYAL_SEAL =
            ITEMS.register(
                    "black_royal_seal",
                    RoyalSealItem::new
            );

    public static final DeferredItem<RoyalSealItem>
            BLUE_ROYAL_SEAL =
            ITEMS.register(
                    "blue_royal_seal",
                    RoyalSealItem::new
            );

    public static final DeferredItem<RoyalSealItem>
            GREEN_ROYAL_SEAL =
            ITEMS.register(
                    "green_royal_seal",
                    RoyalSealItem::new
            );

    public static final DeferredItem<RoyalSealItem>
            CYAN_ROYAL_SEAL =
            ITEMS.register(
                    "cyan_royal_seal",
                    RoyalSealItem::new
            );

    public static final DeferredItem<RoyalSealItem>
            PURPLE_ROYAL_SEAL =
            ITEMS.register(
                    "purple_royal_seal",
                    RoyalSealItem::new
            );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
