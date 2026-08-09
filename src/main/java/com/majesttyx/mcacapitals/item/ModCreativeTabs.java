package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final Supplier<CreativeModeTab> MAIN_TAB = register(
            "main",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("creativetab.mcacapitals.main"))
                    .icon(() -> new ItemStack(ModItems.PURPLE_ROYAL_SEAL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ROYAL_SCEPTER.get());
                        output.accept(ModItems.ROYAL_DISINHERITANCE.get());
                        output.accept(ModItems.LEGITIMIZATION_DECREE.get());
                        output.accept(ModItems.DECLARATION_OF_ABDICATION.get());
                        output.accept(ModItems.BETROTHAL_DECREE.get());
                        output.accept(ModItems.BLANK_SUCCESSION_DECREE.get());
                        output.accept(ModItems.DECREE_OF_THE_HOUSE.get());
                        output.accept(ModItems.ROYAL_CHARTER.get());
                        output.accept(ModItems.CAPITAL_CHRONICLE.get());
                        output.accept(ModItems.SEALED_PURSE.get());
                        output.accept(ModItems.ROYAL_PARDON.get());
                        output.accept(ModItems.DIPLOMATIC_PACKAGE.get());
                        output.accept(ModItems.RED_ROYAL_SEAL.get());
                        output.accept(ModItems.BLACK_ROYAL_SEAL.get());
                        output.accept(ModItems.BLUE_ROYAL_SEAL.get());
                        output.accept(ModItems.GREEN_ROYAL_SEAL.get());
                        output.accept(ModItems.CYAN_ROYAL_SEAL.get());
                        output.accept(ModItems.PURPLE_ROYAL_SEAL.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register() {
    }

    private static Supplier<CreativeModeTab> register(
            String name,
            Supplier<CreativeModeTab> factory
    ) {
        CreativeModeTab tab = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(MCACapitals.MODID, name),
                factory.get()
        );
        return () -> tab;
    }
}
