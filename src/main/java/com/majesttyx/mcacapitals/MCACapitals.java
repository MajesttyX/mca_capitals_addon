package com.majesttyx.mcacapitals;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementProcessor;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticShipmentProcessor;
import com.majesttyx.mcacapitals.capital.CapitalExileDiscoveryHandler;
import com.majesttyx.mcacapitals.capital.CapitalPopulationScanner;
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
import com.majesttyx.mcacapitals.util.CapitalLawCommands;
import com.majesttyx.mcacapitals.util.CapitalLifecycleHandler;
import com.majesttyx.mcacapitals.util.CapitalOathsCommands;
import com.majesttyx.mcacapitals.util.CapitalPetitionCommands;
import com.majesttyx.mcacapitals.util.CapitalRoyalGuardCommands;
import com.majesttyx.mcacapitals.util.CapitalTestCommands;
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

    public static final String MODID =
            "mcacapitals";

    public static final Logger LOGGER =
            LogUtils.getLogger();

    public MCACapitals(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                MCACapitalsConfig.SPEC
        );

        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(
                ModNetwork::register
        );

        NeoForge.EVENT_BUS.register(
                new CapitalPopulationScanner()
        );

        NeoForge.EVENT_BUS.register(
                new CapitalExileDiscoveryHandler()
        );

        NeoForge.EVENT_BUS.register(
                new CapitalPrisonerHandler()
        );

        NeoForge.EVENT_BUS.register(
                new CapitalAmbientDialogueHandler()
        );

        NeoForge.EVENT_BUS.register(
                new CapitalDiplomaticShipmentProcessor()
        );

        NeoForge.EVENT_BUS.register(
                new CapitalDiplomaticAgreementProcessor()
        );

        NeoForge.EVENT_BUS.register(
                new CapitalTradeExchangeProcessor()
        );

        NeoForge.EVENT_BUS.register(
                new RoyalScepterHandler()
        );

        NeoForge.EVENT_BUS.register(
                new RoyalDisinheritanceHandler()
        );

        NeoForge.EVENT_BUS.register(
                new LegitimizationDecreeHandler()
        );

        NeoForge.EVENT_BUS.register(
                new DeclarationOfAbdicationHandler()
        );

        NeoForge.EVENT_BUS.register(
                new BetrothalDecreeHandler()
        );

        NeoForge.EVENT_BUS.register(
                new SuccessionDecreeHandler()
        );

        NeoForge.EVENT_BUS.register(
                new DecreeOfTheHouseHandler()
        );

        NeoForge.EVENT_BUS.register(
                new RoyalPardonHandler()
        );

        NeoForge.EVENT_BUS.register(
                new CapitalLifecycleHandler()
        );

        NeoForge.EVENT_BUS.addListener(
                this::registerCommands
        );
    }

    private void registerCommands(
            RegisterCommandsEvent event
    ) {
        CapitalTestCommands.register(
                event.getDispatcher()
        );

        CapitalIdentityCommands.register(
                event.getDispatcher()
        );

        CapitalHouseCommands.register(
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

        CapitalDiplomaticResponseCommands.register(
                event.getDispatcher()
        );

        CapitalDiplomaticAgreementCommands.register(
                event.getDispatcher()
        );

        CapitalDiplomaticProposalResponseCommands.register(
                event.getDispatcher()
        );

        RoyalScepterCommands.register(
                event.getDispatcher()
        );

        SuccessionDecreeCommands.register(
                event.getDispatcher()
        );
    }
}