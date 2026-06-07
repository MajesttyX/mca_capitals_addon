package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.item.ModCreativeTabs;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.util.FabricEventRegistrar;
import com.majesttyx.mcacapitals.util.AbdicationPromptCommands;
import com.majesttyx.mcacapitals.util.CapitalDebugCommands;
import com.majesttyx.mcacapitals.util.CapitalFoundingCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseFoundationCommands;
import com.majesttyx.mcacapitals.util.CapitalIdentityCommands;
import com.majesttyx.mcacapitals.util.CapitalPetitionCommands;
import com.majesttyx.mcacapitals.util.CapitalRoyalGuardCommands;
import com.majesttyx.mcacapitals.util.CapitalTestCommands;
import com.majesttyx.mcacapitals.util.RoyalScepterCommands;
import com.majesttyx.mcacapitals.util.SuccessionDecreeCommands;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;

public class MCACapitals implements ModInitializer {

    public static final String MODID = "mcacapitals";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ModItems.register();
        ModCreativeTabs.register();
        ModNetwork.registerServerReceivers();
        FabricEventRegistrar.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CapitalTestCommands.register(dispatcher);
            CapitalHouseCommands.register(dispatcher);
            CapitalIdentityCommands.register(dispatcher);
            CapitalHouseFoundationCommands.register(dispatcher);
            CapitalDebugCommands.register(dispatcher);
            AbdicationPromptCommands.register(dispatcher);
            CapitalFoundingCommands.register(dispatcher);
            CapitalRoyalGuardCommands.register(dispatcher);
            CapitalPetitionCommands.register(dispatcher);
            RoyalScepterCommands.register(dispatcher);
            SuccessionDecreeCommands.register(dispatcher);
        });
    }
}