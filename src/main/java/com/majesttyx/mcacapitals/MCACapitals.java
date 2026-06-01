package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.capital.CapitalPopulationScanner;
import com.majesttyx.mcacapitals.config.MCACapitalsConfig;
import com.majesttyx.mcacapitals.dialogue.CapitalAmbientDialogueHandler;
import com.majesttyx.mcacapitals.item.BetrothalDecreeHandler;
import com.majesttyx.mcacapitals.item.DeclarationOfAbdicationHandler;
import com.majesttyx.mcacapitals.item.LegitimizationDecreeHandler;
import com.majesttyx.mcacapitals.item.ModCreativeTabs;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.item.RoyalDisinheritanceHandler;
import com.majesttyx.mcacapitals.item.RoyalScepterHandler;
import com.majesttyx.mcacapitals.item.SuccessionDecreeHandler;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.util.AbdicationPromptCommands;
import com.majesttyx.mcacapitals.util.CapitalDebugCommands;
import com.majesttyx.mcacapitals.util.CapitalFoundingCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseFoundationCommands;
import com.majesttyx.mcacapitals.util.CapitalIdentityCommands;
import com.majesttyx.mcacapitals.util.CapitalLifecycleHandler;
import com.majesttyx.mcacapitals.util.CapitalPetitionCommands;
import com.majesttyx.mcacapitals.util.CapitalRoyalGuardCommands;
import com.majesttyx.mcacapitals.util.CapitalTestCommands;
import com.majesttyx.mcacapitals.util.RoyalGuardInteractionHandler;
import com.majesttyx.mcacapitals.util.RoyalScepterCommands;
import com.majesttyx.mcacapitals.util.SuccessionDecreeCommands;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(MCACapitals.MODID)
public class MCACapitals {

    public static final String MODID = "mcacapitals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MCACapitals(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MCACapitalsConfig.SPEC);

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(ModNetwork::register);

        NeoForge.EVENT_BUS.register(new CapitalPopulationScanner());
        NeoForge.EVENT_BUS.register(new CapitalAmbientDialogueHandler());
        NeoForge.EVENT_BUS.register(new RoyalScepterHandler());
        NeoForge.EVENT_BUS.register(new RoyalDisinheritanceHandler());
        NeoForge.EVENT_BUS.register(new LegitimizationDecreeHandler());
        NeoForge.EVENT_BUS.register(new DeclarationOfAbdicationHandler());
        NeoForge.EVENT_BUS.register(new BetrothalDecreeHandler());
        NeoForge.EVENT_BUS.register(new SuccessionDecreeHandler());
        NeoForge.EVENT_BUS.register(new CapitalLifecycleHandler());
        NeoForge.EVENT_BUS.register(new RoyalGuardInteractionHandler());
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CapitalTestCommands.register(event.getDispatcher());
        CapitalIdentityCommands.register(event.getDispatcher());
        CapitalHouseCommands.register(event.getDispatcher());
        CapitalHouseFoundationCommands.register(event.getDispatcher());
        CapitalDebugCommands.register(event.getDispatcher());
        AbdicationPromptCommands.register(event.getDispatcher());
        CapitalFoundingCommands.register(event.getDispatcher());
        CapitalRoyalGuardCommands.register(event.getDispatcher());
        CapitalPetitionCommands.register(event.getDispatcher());
        RoyalScepterCommands.register(event.getDispatcher());
        SuccessionDecreeCommands.register(event.getDispatcher());
    }
}