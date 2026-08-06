package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.item.ModCreativeTabs;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.menu.ModMenus;
import com.majesttyx.mcacapitals.util.FabricEventRegistrar;
import com.majesttyx.mcacapitals.util.AbdicationPromptCommands;
import com.majesttyx.mcacapitals.util.CapitalAmbassadorUrgentMatterCommands;
import com.majesttyx.mcacapitals.util.CapitalAsylumCommands;
import com.majesttyx.mcacapitals.util.CapitalCampaignCommands;
import com.majesttyx.mcacapitals.util.CapitalCharterCommands;
import com.majesttyx.mcacapitals.util.CapitalDebugCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomaticGiftCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomacyCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomaticResponseCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomaticProposalResponseCommands;
import com.majesttyx.mcacapitals.util.CapitalFoundingCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseFoundationCommands;
import com.majesttyx.mcacapitals.util.CapitalIdentityCommands;
import com.majesttyx.mcacapitals.util.CapitalJusticeCommands;
import com.majesttyx.mcacapitals.util.CapitalPetitionCommands;
import com.majesttyx.mcacapitals.util.CapitalRoyalGuardCommands;
import com.majesttyx.mcacapitals.util.CapitalRoyalEscortCommands;
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
        ModMenus.register();
        ModCreativeTabs.register();
        ModNetwork.registerServerReceivers();
        FabricEventRegistrar.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CapitalTestCommands.register(dispatcher);
            CapitalHouseCommands.register(dispatcher);
            CapitalIdentityCommands.register(dispatcher);
            CapitalJusticeCommands.register(dispatcher);
            CapitalHouseFoundationCommands.register(dispatcher);
            CapitalDebugCommands.register(dispatcher);
            CapitalDiplomaticGiftCommands.register(dispatcher);
            CapitalDiplomacyCommands.register(dispatcher);
            CapitalDiplomaticResponseCommands.register(dispatcher);
            CapitalDiplomaticProposalResponseCommands.register(dispatcher);
            AbdicationPromptCommands.register(dispatcher);
            CapitalFoundingCommands.register(dispatcher);
            CapitalRoyalGuardCommands.register(dispatcher);
            CapitalRoyalEscortCommands.register(dispatcher);
            CapitalAsylumCommands.register(dispatcher);
            CapitalAmbassadorUrgentMatterCommands.register(dispatcher);
            CapitalCampaignCommands.register(dispatcher);
            CapitalCharterCommands.register(dispatcher);
            CapitalPetitionCommands.register(dispatcher);
            RoyalScepterCommands.register(dispatcher);
            SuccessionDecreeCommands.register(dispatcher);
        });
    }
}