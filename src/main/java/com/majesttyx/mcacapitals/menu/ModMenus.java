package com.majesttyx.mcacapitals.menu;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    MCACapitals.MODID
            );

    public static final DeferredHolder<MenuType<?>, MenuType<DiplomaticPackageMenu>>
            DIPLOMATIC_PACKAGE =
            MENUS.register(
                    "diplomatic_package",
                    () -> IMenuTypeExtension.create(
                            DiplomaticPackageMenu::new
                    )
            );

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}