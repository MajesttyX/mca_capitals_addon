package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModItems {

    public static final Supplier<Item> ROYAL_SCEPTER =
            register("royal_scepter", RoyalScepterItem::new);

    public static final Supplier<Item> ROYAL_DISINHERITANCE =
            register("royal_disinheritance", RoyalDisinheritanceItem::new);

    public static final Supplier<Item> LEGITIMIZATION_DECREE =
            register("legitimization_decree", LegitimizationDecreeItem::new);

    public static final Supplier<Item> DECLARATION_OF_ABDICATION =
            register("declaration_of_abdication", DeclarationOfAbdicationItem::new);

    public static final Supplier<Item> BETROTHAL_DECREE =
            register("betrothal_decree", BetrothalDecreeItem::new);

    public static final Supplier<Item> BLANK_SUCCESSION_DECREE =
            register("blank_succession_decree", SuccessionDecreeItem::new);

    public static final Supplier<Item> ROYAL_CHARTER =
            register("royal_charter", RoyalCharterItem::new);

    public static final Supplier<Item> CAPITAL_CHRONICLE =
            register("capital_chronicle", CapitalChronicleItem::new);

    public static final Supplier<Item> DECREE_OF_THE_HOUSE =
            register("decree_of_the_house", DecreeOfTheHouseItem::new);

    public static final Supplier<Item> SEALED_PURSE =
            register("sealed_purse", SealedPurseItem::new);

    public static final Supplier<Item> ROYAL_PARDON =
            register("royal_pardon", RoyalPardonItem::new);

    public static final Supplier<Item> DIPLOMATIC_PACKAGE =
            register("diplomatic_package", DiplomaticPackageItem::new);

    public static final Supplier<Item> RED_ROYAL_SEAL =
            register("red_royal_seal", RoyalSealItem::new);

    public static final Supplier<Item> BLACK_ROYAL_SEAL =
            register("black_royal_seal", RoyalSealItem::new);

    public static final Supplier<Item> BLUE_ROYAL_SEAL =
            register("blue_royal_seal", RoyalSealItem::new);

    public static final Supplier<Item> GREEN_ROYAL_SEAL =
            register("green_royal_seal", RoyalSealItem::new);

    public static final Supplier<Item> CYAN_ROYAL_SEAL =
            register("cyan_royal_seal", RoyalSealItem::new);

    public static final Supplier<Item> PURPLE_ROYAL_SEAL =
            register("purple_royal_seal", RoyalSealItem::new);

    private ModItems() {
    }

    public static void register() {
    }

    private static <T extends Item> Supplier<T> register(
            String name,
            Supplier<T> factory
    ) {
        T item = Registry.register(
                BuiltInRegistries.ITEM,
                new ResourceLocation(MCACapitals.MODID, name),
                factory.get()
        );
        return () -> item;
    }
}
