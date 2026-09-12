package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "2";

    private static final int OPEN_CAPITAL_CHRONICLE = 0;
    private static final int OPEN_AMBASSADOR_COMMUNICATION = 1;
    private static final int OPEN_BETROTHAL_SELECTION = 2;
    private static final int OPEN_ACCUSATION_SELECTION = 3;
    private static final int OPEN_SEALED_PURSE_CASE_SELECTION = 4;
    private static final int OPEN_ROYAL_CHARTER_DECISION = 5;
    private static final int OPEN_PLAYER_HOUSE_SETUP = 6;
    private static final int OPEN_DECREE_OF_THE_HOUSE = 7;
    private static final int SYNC_VILLAGER_IDENTITY = 8;
    private static final int SYNC_BLUEPRINT_AUTHORITY = 9;
    private static final int SUBMIT_DECREE_OF_THE_HOUSE = 10;
    private static final int OPEN_DECLARATION_OF_SEPARATION = 11;
    private static final int SUBMIT_DECLARATION_OF_SEPARATION = 12;

    private static boolean registered = false;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MCACapitals.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        CHANNEL.registerMessage(
                OPEN_CAPITAL_CHRONICLE,
                OpenCapitalChroniclePacket.class,
                OpenCapitalChroniclePacket::encode,
                OpenCapitalChroniclePacket::decode,
                OpenCapitalChroniclePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                OPEN_AMBASSADOR_COMMUNICATION,
                OpenAmbassadorCommunicationPacket.class,
                OpenAmbassadorCommunicationPacket::encode,
                OpenAmbassadorCommunicationPacket::decode,
                OpenAmbassadorCommunicationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                OPEN_BETROTHAL_SELECTION,
                OpenBetrothalSelectionPacket.class,
                OpenBetrothalSelectionPacket::encode,
                OpenBetrothalSelectionPacket::decode,
                OpenBetrothalSelectionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                OPEN_ACCUSATION_SELECTION,
                OpenAccusationSelectionPacket.class,
                OpenAccusationSelectionPacket::encode,
                OpenAccusationSelectionPacket::decode,
                OpenAccusationSelectionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                OPEN_SEALED_PURSE_CASE_SELECTION,
                OpenSealedPurseCaseSelectionPacket.class,
                OpenSealedPurseCaseSelectionPacket::encode,
                OpenSealedPurseCaseSelectionPacket::decode,
                OpenSealedPurseCaseSelectionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                OPEN_ROYAL_CHARTER_DECISION,
                OpenRoyalCharterDecisionPacket.class,
                OpenRoyalCharterDecisionPacket::encode,
                OpenRoyalCharterDecisionPacket::decode,
                OpenRoyalCharterDecisionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                OPEN_PLAYER_HOUSE_SETUP,
                OpenPlayerHouseSetupPacket.class,
                OpenPlayerHouseSetupPacket::encode,
                OpenPlayerHouseSetupPacket::decode,
                OpenPlayerHouseSetupPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                OPEN_DECREE_OF_THE_HOUSE,
                OpenDecreeOfTheHousePacket.class,
                OpenDecreeOfTheHousePacket::encode,
                OpenDecreeOfTheHousePacket::decode,
                OpenDecreeOfTheHousePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                SYNC_VILLAGER_IDENTITY,
                SyncVillagerIdentityPacket.class,
                SyncVillagerIdentityPacket::encode,
                SyncVillagerIdentityPacket::decode,
                SyncVillagerIdentityPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                SYNC_BLUEPRINT_AUTHORITY,
                SyncBlueprintAuthorityPacket.class,
                SyncBlueprintAuthorityPacket::encode,
                SyncBlueprintAuthorityPacket::decode,
                SyncBlueprintAuthorityPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                SUBMIT_DECREE_OF_THE_HOUSE,
                SubmitDecreeOfTheHousePacket.class,
                SubmitDecreeOfTheHousePacket::encode,
                SubmitDecreeOfTheHousePacket::decode,
                SubmitDecreeOfTheHousePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                OPEN_DECLARATION_OF_SEPARATION,
                OpenDeclarationOfSeparationPacket.class,
                OpenDeclarationOfSeparationPacket::encode,
                OpenDeclarationOfSeparationPacket::decode,
                OpenDeclarationOfSeparationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                SUBMIT_DECLARATION_OF_SEPARATION,
                SubmitDeclarationOfSeparationPacket.class,
                SubmitDeclarationOfSeparationPacket::encode,
                SubmitDeclarationOfSeparationPacket::decode,
                SubmitDeclarationOfSeparationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        registered = true;
    }

    public static void sendToPlayer(ServerPlayer player, OpenCapitalChroniclePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenAmbassadorCommunicationPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenBetrothalSelectionPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenAccusationSelectionPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenSealedPurseCaseSelectionPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenRoyalCharterDecisionPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenPlayerHouseSetupPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenDecreeOfTheHousePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenDeclarationOfSeparationPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, SyncVillagerIdentityPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, SyncBlueprintAuthorityPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(SubmitDecreeOfTheHousePacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(SubmitDeclarationOfSeparationPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
