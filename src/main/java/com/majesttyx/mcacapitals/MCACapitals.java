package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorUrgentMatterHandler;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementProcessor;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticShipmentProcessor;
import com.majesttyx.mcacapitals.capital.CapitalExileDiscoveryHandler;
import com.majesttyx.mcacapitals.capital.CapitalPopulationScanner;
import com.majesttyx.mcacapitals.capital.CapitalPlayerLegalHandler;
import com.majesttyx.mcacapitals.capital.CapitalPrisonerHandler;
import com.majesttyx.mcacapitals.capital.CapitalTradeExchangeProcessor;
import com.majesttyx.mcacapitals.config.MCACapitalsConfig;
import com.majesttyx.mcacapitals.dialogue.CapitalAmbientDialogueHandler;
import com.majesttyx.mcacapitals.item.BetrothalDecreeHandler;
import com.majesttyx.mcacapitals.item.DeclarationOfAbdicationHandler;
import com.majesttyx.mcacapitals.item.DecreeOfTheHouseHandler;
import com.majesttyx.mcacapitals.item.LegitimizationDecreeHandler;
import com.majesttyx.mcacapitals.item.ModCreativeTabs;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.item.RoyalDisinheritanceHandler;
import com.majesttyx.mcacapitals.item.RoyalPardonHandler;
import com.majesttyx.mcacapitals.item.RoyalScepterHandler;
import com.majesttyx.mcacapitals.item.SuccessionDecreeHandler;
import com.majesttyx.mcacapitals.menu.ModMenus;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.util.AbdicationPromptCommands;
import com.majesttyx.mcacapitals.util.CapitalAmbassadorUrgentMatterCommands;
import com.majesttyx.mcacapitals.util.CapitalAsylumCommands;
import com.majesttyx.mcacapitals.util.CapitalCampaignCommands;
import com.majesttyx.mcacapitals.util.CapitalCharterCommands;
import com.majesttyx.mcacapitals.util.CapitalDebugCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomaticAgreementCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomaticGiftCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomaticProposalResponseCommands;
import com.majesttyx.mcacapitals.util.CapitalDiplomaticResponseCommands;
import com.majesttyx.mcacapitals.util.CapitalFoundingCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseCommands;
import com.majesttyx.mcacapitals.util.CapitalHouseFoundationCommands;
import com.majesttyx.mcacapitals.util.CapitalIdentityCommands;
import com.majesttyx.mcacapitals.util.CapitalIntegrationTestCommands;
import com.majesttyx.mcacapitals.util.CapitalJusticeCommands;
import com.majesttyx.mcacapitals.util.CapitalLawCommands;
import com.majesttyx.mcacapitals.util.CapitalLifecycleHandler;
import com.majesttyx.mcacapitals.util.CapitalOathsCommands;
import com.majesttyx.mcacapitals.util.CapitalPetitionCommands;
import com.majesttyx.mcacapitals.util.CapitalRoyalEscortCommands;
import com.majesttyx.mcacapitals.util.CapitalRoyalGuardCommands;
import com.majesttyx.mcacapitals.util.CapitalTestCommands;
import com.majesttyx.mcacapitals.util.RoyalScepterCommands;
import com.majesttyx.mcacapitals.util.RoyalScepterOfficeCommands;
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

    public static final String MODID =
            "mcacapitals";

    public static final Logger LOGGER =
            LogUtils.getLogger();

    public MCACapitals() {
        IEventBus modEventBus =
                FMLJavaModLoadingContext
                        .get()
                        .getModEventBus();

        ModLoadingContext
                .get()
                .registerConfig(
                        ModConfig.Type.COMMON,
                        MCACapitalsConfig.SPEC
                );

        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(
                this::commonSetup
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalPopulationScanner()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalExileDiscoveryHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalPrisonerHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalPlayerLegalHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalAmbientDialogueHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalDiplomaticShipmentProcessor()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalAmbassadorUrgentMatterHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalDiplomaticAgreementProcessor()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalTradeExchangeProcessor()
        );

        MinecraftForge.EVENT_BUS.register(
                new RoyalScepterHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new RoyalDisinheritanceHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new LegitimizationDecreeHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new DeclarationOfAbdicationHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new BetrothalDecreeHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new SuccessionDecreeHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new DecreeOfTheHouseHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new RoyalPardonHandler()
        );

        MinecraftForge.EVENT_BUS.register(
                new CapitalLifecycleHandler()
        );

        MinecraftForge.EVENT_BUS.addListener(
                this::registerCommands
        );
    }

    private void registerCommands(
            RegisterCommandsEvent event
    ) {
        CapitalTestCommands.register(
                event.getDispatcher()
        );

        CapitalHouseCommands.register(
                event.getDispatcher()
        );

        CapitalIdentityCommands.register(
                event.getDispatcher()
        );

        CapitalHouseFoundationCommands.register(
                event.getDispatcher()
        );

        CapitalDebugCommands.register(
                event.getDispatcher()
        );

        AbdicationPromptCommands.register(
                event.getDispatcher()
        );

        CapitalFoundingCommands.register(
                event.getDispatcher()
        );

        CapitalCharterCommands.register(
                event.getDispatcher()
        );

        CapitalJusticeCommands.register(
                event.getDispatcher()
        );

        CapitalLawCommands.register(
                event.getDispatcher()
        );

        CapitalOathsCommands.register(
                event.getDispatcher()
        );

        CapitalRoyalGuardCommands.register(
                event.getDispatcher()
        );

        CapitalPetitionCommands.register(
                event.getDispatcher()
        );

        CapitalDiplomaticGiftCommands.register(
                event.getDispatcher()
        );

        CapitalAmbassadorUrgentMatterCommands.register(
                event.getDispatcher()
        );

        CapitalDiplomaticResponseCommands.register(
                event.getDispatcher()
        );

        CapitalDiplomaticAgreementCommands.register(
                event.getDispatcher()
        );

        CapitalRoyalEscortCommands.register(
                event.getDispatcher()
        );

        CapitalDiplomaticProposalResponseCommands.register(
                event.getDispatcher()
        );

        CapitalAsylumCommands.register(
                event.getDispatcher()
        );

        CapitalCampaignCommands.register(
                event.getDispatcher()
        );

        CapitalIntegrationTestCommands.register(
                event.getDispatcher()
        );

        RoyalScepterCommands.register(
                event.getDispatcher()
        );

        RoyalScepterOfficeCommands.register(
                event.getDispatcher()
        );

        SuccessionDecreeCommands.register(
                event.getDispatcher()
        );
    }

    private void commonSetup(
            FMLCommonSetupEvent event
    ) {
        event.enqueueWork(
                ModNetwork::register
        );
    }
}
