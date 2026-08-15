package com.majesttyx.mcacapitals.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetwork {

    public static final ClientChannel CHANNEL = new ClientChannel();

    private static boolean payloadTypesRegistered;
    private static boolean serverReceiversRegistered;
    private static boolean clientReceiversRegistered;

    private ModNetwork() {
    }

    public static synchronized void registerServerReceivers() {
        registerPayloadTypes();
        if (serverReceiversRegistered) {
            return;
        }
        serverReceiversRegistered = true;

        ServerPlayNetworking.registerGlobalReceiver(
                SubmitDecreeOfTheHousePacket.TYPE,
                (packet, context) -> context.server().execute(
                        () -> SubmitDecreeOfTheHousePacket.handle(packet, context.player())
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SelectSealedPurseCasePacket.TYPE,
                (packet, context) -> context.server().execute(
                        () -> SelectSealedPurseCasePacket.handle(packet, context.player())
                )
        );
    }

    private static synchronized void registerPayloadTypes() {
        if (payloadTypesRegistered) {
            return;
        }
        payloadTypesRegistered = true;

        PayloadTypeRegistry.playC2S().register(
                SubmitDecreeOfTheHousePacket.TYPE,
                SubmitDecreeOfTheHousePacket.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                SelectSealedPurseCasePacket.TYPE,
                SelectSealedPurseCasePacket.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                OpenCapitalChroniclePacket.TYPE,
                OpenCapitalChroniclePacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenBetrothalSelectionPacket.TYPE,
                OpenBetrothalSelectionPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenAccusationSelectionPacket.TYPE,
                OpenAccusationSelectionPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenRoyalCharterDecisionPacket.TYPE,
                OpenRoyalCharterDecisionPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenPlayerHouseSetupPacket.TYPE,
                OpenPlayerHouseSetupPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenDecreeOfTheHousePacket.TYPE,
                OpenDecreeOfTheHousePacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                SyncVillagerIdentityPacket.TYPE,
                SyncVillagerIdentityPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                SyncBlueprintAuthorityPacket.TYPE,
                SyncBlueprintAuthorityPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenSealedPurseCaseSelectionPacket.TYPE,
                OpenSealedPurseCaseSelectionPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenAmbassadorCommunicationPacket.TYPE,
                OpenAmbassadorCommunicationPacket.CODEC
        );
    }

    @Environment(EnvType.CLIENT)
    public static synchronized void registerClientReceivers() {
        registerPayloadTypes();
        if (clientReceiversRegistered) {
            return;
        }
        clientReceiversRegistered = true;

        ClientPlayNetworking.registerGlobalReceiver(
                OpenCapitalChroniclePacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenCapitalChroniclePacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenBetrothalSelectionPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenBetrothalSelectionPacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenAccusationSelectionPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenAccusationSelectionPacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenRoyalCharterDecisionPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenRoyalCharterDecisionPacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenPlayerHouseSetupPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenPlayerHouseSetupPacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenDecreeOfTheHousePacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenDecreeOfTheHousePacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncVillagerIdentityPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> SyncVillagerIdentityPacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncBlueprintAuthorityPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> SyncBlueprintAuthorityPacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenSealedPurseCaseSelectionPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenSealedPurseCaseSelectionPacket.handle(packet)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenAmbassadorCommunicationPacket.TYPE,
                (packet, context) -> context.client().execute(
                        () -> OpenAmbassadorCommunicationPacket.handle(packet)
                )
        );
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        if (player != null && packet != null) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    @Environment(EnvType.CLIENT)
    public static void sendToServer(CustomPacketPayload packet) {
        if (packet != null) {
            ClientPlayNetworking.send(packet);
        }
    }

    public static final class ClientChannel {
        private ClientChannel() {
        }

        @Environment(EnvType.CLIENT)
        public void sendToServer(CustomPacketPayload packet) {
            ModNetwork.sendToServer(packet);
        }
    }
}
