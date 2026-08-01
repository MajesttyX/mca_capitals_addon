package com.majesttyx.mcacapitals.menu;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    ForgeRegistries.MENU_TYPES,
                    MCACapitals.MODID
            );

    public static final RegistryObject<MenuType<DiplomaticPackageMenu>>
            DIPLOMATIC_PACKAGE =
            MENUS.register(
                    "diplomatic_package",
                    () -> IForgeMenuType.create(
                            DiplomaticPackageMenu::new
                    )
            );

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}