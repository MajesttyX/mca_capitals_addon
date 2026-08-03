package com.majesttyx.mcacapitals.menu;

import com.majesttyx.mcacapitals.MCACapitals;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModMenus {

    public static final ExtendedScreenHandlerType<DiplomaticPackageMenu, DiplomaticPackageOpenData>
            DIPLOMATIC_PACKAGE = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(
                    MCACapitals.MODID,
                    "diplomatic_package"
            ),
            new ExtendedScreenHandlerType<>(
                    DiplomaticPackageMenu::new,
                    DiplomaticPackageOpenData.STREAM_CODEC
            )
    );

    private ModMenus() {
    }

    public static void register() {
    }
}
