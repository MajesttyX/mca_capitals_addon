package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModNetwork {

    public static final ResourceLocation OPEN_CAPITAL_CHRONICLE = new ResourceLocation(MCACapitals.MODID, "open_capital_chronicle");
    public static final ResourceLocation OPEN_BETROTHAL_SELECTION = new ResourceLocation(MCACapitals.MODID, "open_betrothal_selection");
    public static final ResourceLocation OPEN_ROYAL_CHARTER_DECISION = new ResourceLocation(MCACapitals.MODID, "open_royal_charter_decision");
    public static final ResourceLocation OPEN_PLAYER_HOUSE_SETUP = new ResourceLocation(MCACapitals.MODID, "open_player_house_setup");
    public static final ResourceLocation OPEN_DECREE_OF_THE_HOUSE = new ResourceLocation(MCACapitals.MODID, "open_decree_of_the_house");
    public static final ResourceLocation SUBMIT_DECREE_OF_THE_HOUSE = new ResourceLocation(MCACapitals.MODID, "submit_decree_of_the_house");
    public static final ResourceLocation SYNC_VILLAGER_IDENTITY = new ResourceLocation(MCACapitals.MODID, "sync_villager_identity");

    public static final ClientChannel CHANNEL = new ClientChannel();

    private ModNetwork() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SUBMIT_DECREE_OF_THE_HOUSE, (server, player, handler, buffer, responseSender) -> {
            SubmitDecreeOfTheHousePacket packet = SubmitDecreeOfTheHousePacket.decode(buffer);
            server.execute(() -> SubmitDecreeOfTheHousePacket.handle(packet, player));
        });
    }

    @Environment(EnvType.CLIENT)
    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(OPEN_CAPITAL_CHRONICLE, (client, handler, buffer, responseSender) -> {
            OpenCapitalChroniclePacket packet = OpenCapitalChroniclePacket.decode(buffer);
            client.execute(() -> OpenCapitalChroniclePacket.handle(packet));
        });

        ClientPlayNetworking.registerGlobalReceiver(OPEN_BETROTHAL_SELECTION, (client, handler, buffer, responseSender) -> {
            OpenBetrothalSelectionPacket packet = OpenBetrothalSelectionPacket.decode(buffer);
            client.execute(() -> OpenBetrothalSelectionPacket.handle(packet));
        });

        ClientPlayNetworking.registerGlobalReceiver(OPEN_ROYAL_CHARTER_DECISION, (client, handler, buffer, responseSender) -> {
            OpenRoyalCharterDecisionPacket packet = OpenRoyalCharterDecisionPacket.decode(buffer);
            client.execute(() -> OpenRoyalCharterDecisionPacket.handle(packet));
        });

        ClientPlayNetworking.registerGlobalReceiver(OPEN_PLAYER_HOUSE_SETUP, (client, handler, buffer, responseSender) -> {
            OpenPlayerHouseSetupPacket packet = OpenPlayerHouseSetupPacket.decode(buffer);
            client.execute(() -> OpenPlayerHouseSetupPacket.handle(packet));
        });

        ClientPlayNetworking.registerGlobalReceiver(OPEN_DECREE_OF_THE_HOUSE, (client, handler, buffer, responseSender) -> {
            OpenDecreeOfTheHousePacket packet = OpenDecreeOfTheHousePacket.decode(buffer);
            client.execute(() -> OpenDecreeOfTheHousePacket.handle(packet));
        });

        ClientPlayNetworking.registerGlobalReceiver(SYNC_VILLAGER_IDENTITY, (client, handler, buffer, responseSender) -> {
            SyncVillagerIdentityPacket packet = SyncVillagerIdentityPacket.decode(buffer);
            client.execute(() -> SyncVillagerIdentityPacket.handle(packet));
        });
    }

    public static void sendToPlayer(ServerPlayer player, OpenCapitalChroniclePacket packet) {
        sendToPlayer(player, OPEN_CAPITAL_CHRONICLE, buffer -> OpenCapitalChroniclePacket.encode(packet, buffer));
    }

    public static void sendToPlayer(ServerPlayer player, OpenBetrothalSelectionPacket packet) {
        sendToPlayer(player, OPEN_BETROTHAL_SELECTION, buffer -> OpenBetrothalSelectionPacket.encode(packet, buffer));
    }

    public static void sendToPlayer(ServerPlayer player, OpenRoyalCharterDecisionPacket packet) {
        sendToPlayer(player, OPEN_ROYAL_CHARTER_DECISION, buffer -> OpenRoyalCharterDecisionPacket.encode(packet, buffer));
    }

    public static void sendToPlayer(ServerPlayer player, OpenPlayerHouseSetupPacket packet) {
        sendToPlayer(player, OPEN_PLAYER_HOUSE_SETUP, buffer -> OpenPlayerHouseSetupPacket.encode(packet, buffer));
    }

    public static void sendToPlayer(ServerPlayer player, OpenDecreeOfTheHousePacket packet) {
        sendToPlayer(player, OPEN_DECREE_OF_THE_HOUSE, buffer -> OpenDecreeOfTheHousePacket.encode(packet, buffer));
    }

    public static void sendToPlayer(ServerPlayer player, SyncVillagerIdentityPacket packet) {
        sendToPlayer(player, SYNC_VILLAGER_IDENTITY, buffer -> SyncVillagerIdentityPacket.encode(packet, buffer));
    }

    private static void sendToPlayer(ServerPlayer player, ResourceLocation id, PacketWriter writer) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        writer.write(buffer);
        ServerPlayNetworking.send(player, id, buffer);
    }

    @Environment(EnvType.CLIENT)
    public static void sendToServer(SubmitDecreeOfTheHousePacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SubmitDecreeOfTheHousePacket.encode(packet, buffer);
        ClientPlayNetworking.send(SUBMIT_DECREE_OF_THE_HOUSE, buffer);
    }

    private interface PacketWriter {
        void write(FriendlyByteBuf buffer);
    }

    public static final class ClientChannel {
        private ClientChannel() {
        }

        @Environment(EnvType.CLIENT)
        public void sendToServer(SubmitDecreeOfTheHousePacket packet) {
            ModNetwork.sendToServer(packet);
        }
    }
}