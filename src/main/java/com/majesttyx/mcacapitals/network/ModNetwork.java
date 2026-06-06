package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static int nextId = 0;
    private static boolean registered = false;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
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
                nextId++,
                OpenCapitalChroniclePacket.class,
                OpenCapitalChroniclePacket::encode,
                OpenCapitalChroniclePacket::decode,
                OpenCapitalChroniclePacket::handle
        );

        CHANNEL.registerMessage(
                nextId++,
                OpenBetrothalSelectionPacket.class,
                OpenBetrothalSelectionPacket::encode,
                OpenBetrothalSelectionPacket::decode,
                OpenBetrothalSelectionPacket::handle
        );

        CHANNEL.registerMessage(
                nextId++,
                OpenRoyalCharterDecisionPacket.class,
                OpenRoyalCharterDecisionPacket::encode,
                OpenRoyalCharterDecisionPacket::decode,
                OpenRoyalCharterDecisionPacket::handle
        );

        CHANNEL.registerMessage(
                nextId++,
                OpenPlayerHouseSetupPacket.class,
                OpenPlayerHouseSetupPacket::encode,
                OpenPlayerHouseSetupPacket::decode,
                OpenPlayerHouseSetupPacket::handle
        );

        CHANNEL.registerMessage(
                nextId++,
                OpenDecreeOfTheHousePacket.class,
                OpenDecreeOfTheHousePacket::encode,
                OpenDecreeOfTheHousePacket::decode,
                OpenDecreeOfTheHousePacket::handle
        );

        CHANNEL.registerMessage(
                nextId++,
                SubmitDecreeOfTheHousePacket.class,
                SubmitDecreeOfTheHousePacket::encode,
                SubmitDecreeOfTheHousePacket::decode,
                SubmitDecreeOfTheHousePacket::handle
        );

        CHANNEL.registerMessage(
                nextId++,
                SyncVillagerIdentityPacket.class,
                SyncVillagerIdentityPacket::encode,
                SyncVillagerIdentityPacket::decode,
                SyncVillagerIdentityPacket::handle
        );

        registered = true;
    }

    public static void sendToPlayer(ServerPlayer player, OpenCapitalChroniclePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenBetrothalSelectionPacket packet) {
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

    public static void sendToPlayer(ServerPlayer player, SyncVillagerIdentityPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}