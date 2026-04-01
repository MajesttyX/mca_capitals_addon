package com.example.mcacapitals.item;

import com.example.mcacapitals.MCACapitals;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCACapitals.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.mcacapitals.main"))
                    .icon(() -> new ItemStack(ModItems.RED_ROYAL_SEAL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ROYAL_SCEPTER.get());
                        output.accept(ModItems.ROYAL_DISINHERITANCE.get());
                        output.accept(ModItems.LEGITIMIZATION_DECREE.get());
                        output.accept(ModItems.DECLARATION_OF_ABDICATION.get());
                        output.accept(ModItems.ROYAL_CHARTER.get());
                        output.accept(ModItems.CAPITAL_CHRONICLE.get());
                        output.accept(ModItems.RED_ROYAL_SEAL.get());
                        output.accept(ModItems.BLACK_ROYAL_SEAL.get());
                        output.accept(ModItems.BLUE_ROYAL_SEAL.get());
                        output.accept(ModItems.GREEN_ROYAL_SEAL.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}