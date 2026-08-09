package com.majesttyx.mcacapitals.menu;

import com.majesttyx.mcacapitals.MCACapitals;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModMenus {
    public static final ExtendedScreenHandlerType<DiplomaticPackageMenu> DIPLOMATIC_PACKAGE = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(MCACapitals.MODID, "diplomatic_package"),
            new ExtendedScreenHandlerType<>(DiplomaticPackageMenu::new)
    );

    private ModMenus() {
    }

    public static void register() {
    }
}
