package com.example.mcacapitals;

import com.example.mcacapitals.capital.CapitalPopulationScanner;
import com.example.mcacapitals.item.DeclarationOfAbdicationHandler;
import com.example.mcacapitals.item.HeirScepterHandler;
import com.example.mcacapitals.item.LegitimizationDecreeHandler;
import com.example.mcacapitals.item.ModItems;
import com.example.mcacapitals.item.RoyalDisinheritanceHandler;
import com.example.mcacapitals.network.ModNetwork;
import com.example.mcacapitals.util.AbdicationPromptCommands;
import com.example.mcacapitals.util.CapitalDebugCommands;
import com.example.mcacapitals.util.CapitalFoundingCommands;
import com.example.mcacapitals.util.CapitalLifecycleHandler;
import com.example.mcacapitals.util.CapitalRoyalGuardCommands;
import com.example.mcacapitals.util.CapitalTestCommands;
import com.example.mcacapitals.util.RoyalGuardInteractionHandler;
import com.example.mcacapitals.util.RoyalScepterCommands;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModItems.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(new CapitalPopulationScanner());
        MinecraftForge.EVENT_BUS.register(new HeirScepterHandler());
        MinecraftForge.EVENT_BUS.register(new RoyalDisinheritanceHandler());
        MinecraftForge.EVENT_BUS.register(new LegitimizationDecreeHandler());
        MinecraftForge.EVENT_BUS.register(new DeclarationOfAbdicationHandler());
        MinecraftForge.EVENT_BUS.register(new CapitalLifecycleHandler());
        MinecraftForge.EVENT_BUS.register(new RoyalGuardInteractionHandler());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CapitalTestCommands.register(event.getDispatcher());
        CapitalDebugCommands.register(event.getDispatcher());
        AbdicationPromptCommands.register(event.getDispatcher());
        CapitalFoundingCommands.register(event.getDispatcher());
        CapitalRoyalGuardCommands.register(event.getDispatcher());
        RoyalScepterCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.HEIR_SCEPTER);
            event.accept(ModItems.ROYAL_DISINHERITANCE);
            event.accept(ModItems.LEGITIMIZATION_DECREE);
            event.accept(ModItems.DECLARATION_OF_ABDICATION);
            event.accept(ModItems.ROYAL_CHARTER);
            event.accept(ModItems.CAPITAL_CHRONICLE);
        }
    }
}