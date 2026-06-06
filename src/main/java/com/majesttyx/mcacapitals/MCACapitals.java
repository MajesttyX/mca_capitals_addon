package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.capital.CapitalPopulationScanner;
import com.majesttyx.mcacapitals.config.MCACapitalsConfig;
import com.majesttyx.mcacapitals.dialogue.CapitalAmbientDialogueHandler;
import com.majesttyx.mcacapitals.item.BetrothalDecreeHandler;
import com.majesttyx.mcacapitals.item.DeclarationOfAbdicationHandler;
import com.majesttyx.mcacapitals.item.DecreeOfTheHouseHandler;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MCACapitals.MODID)
public class MCACapitals {

    public static final String MODID = "mcacapitals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MCACapitals() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MCACapitalsConfig.SPEC);

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new CapitalPopulationScanner());
        MinecraftForge.EVENT_BUS.register(new CapitalAmbientDialogueHandler());
        MinecraftForge.EVENT_BUS.register(new RoyalScepterHandler());
        MinecraftForge.EVENT_BUS.register(new RoyalDisinheritanceHandler());
        MinecraftForge.EVENT_BUS.register(new LegitimizationDecreeHandler());
        MinecraftForge.EVENT_BUS.register(new DeclarationOfAbdicationHandler());
        MinecraftForge.EVENT_BUS.register(new BetrothalDecreeHandler());
        MinecraftForge.EVENT_BUS.register(new SuccessionDecreeHandler());
        MinecraftForge.EVENT_BUS.register(new DecreeOfTheHouseHandler());
        MinecraftForge.EVENT_BUS.register(new CapitalLifecycleHandler());
        MinecraftForge.EVENT_BUS.register(new RoyalGuardInteractionHandler());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CapitalTestCommands.register(event.getDispatcher());
        CapitalHouseCommands.register(event.getDispatcher());
        CapitalIdentityCommands.register(event.getDispatcher());
        CapitalHouseFoundationCommands.register(event.getDispatcher());
        CapitalDebugCommands.register(event.getDispatcher());
        AbdicationPromptCommands.register(event.getDispatcher());
        CapitalFoundingCommands.register(event.getDispatcher());
        CapitalRoyalGuardCommands.register(event.getDispatcher());
        CapitalPetitionCommands.register(event.getDispatcher());
        RoyalScepterCommands.register(event.getDispatcher());
        SuccessionDecreeCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}