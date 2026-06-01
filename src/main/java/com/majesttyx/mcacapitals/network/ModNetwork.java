package com.majesttyx.mcacapitals.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                OpenCapitalChroniclePacket.TYPE,
                OpenCapitalChroniclePacket.STREAM_CODEC,
                OpenCapitalChroniclePacket::handle
        );

        registrar.playToClient(
                OpenBetrothalSelectionPacket.TYPE,
                OpenBetrothalSelectionPacket.STREAM_CODEC,
                OpenBetrothalSelectionPacket::handle
        );

        registrar.playToClient(
                OpenRoyalCharterDecisionPacket.TYPE,
                OpenRoyalCharterDecisionPacket.STREAM_CODEC,
                OpenRoyalCharterDecisionPacket::handle
        );

        registrar.playToClient(
                OpenPlayerHouseSetupPacket.TYPE,
                OpenPlayerHouseSetupPacket.STREAM_CODEC,
                OpenPlayerHouseSetupPacket::handle
        );

        registrar.playToClient(
                SyncVillagerIdentityPacket.TYPE,
                SyncVillagerIdentityPacket.STREAM_CODEC,
                SyncVillagerIdentityPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, OpenCapitalChroniclePacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenBetrothalSelectionPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenRoyalCharterDecisionPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToPlayer(ServerPlayer player, OpenPlayerHouseSetupPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToPlayer(ServerPlayer player, SyncVillagerIdentityPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}