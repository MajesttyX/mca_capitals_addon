package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.PlayerHouseSetupClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class OpenPlayerHouseSetupPacket implements CustomPacketPayload {

    public static final Type<OpenPlayerHouseSetupPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_player_house_setup")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPlayerHouseSetupPacket> CODEC =
            StreamCodec.ofMember(OpenPlayerHouseSetupPacket::encode, OpenPlayerHouseSetupPacket::decode);

    private final UUID capitalId;
    private final String villageName;

    public OpenPlayerHouseSetupPacket(UUID capitalId, String villageName) {
        this.capitalId = capitalId;
        this.villageName = villageName == null ? "" : villageName;
    }

    public UUID capitalId() {
        return capitalId;
    }

    public String villageName() {
        return villageName;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(capitalId);
        buffer.writeUtf(villageName);
    }

    public static OpenPlayerHouseSetupPacket decode(RegistryFriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        return new OpenPlayerHouseSetupPacket(capitalId, villageName);
    }

    public static void handle(OpenPlayerHouseSetupPacket packet) {
        PlayerHouseSetupClient.open(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}