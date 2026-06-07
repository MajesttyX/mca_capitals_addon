package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.PlayerHouseSetupClient;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class OpenPlayerHouseSetupPacket {

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

    public static void encode(OpenPlayerHouseSetupPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.capitalId);
        buffer.writeUtf(packet.villageName);
    }

    public static OpenPlayerHouseSetupPacket decode(FriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        return new OpenPlayerHouseSetupPacket(capitalId, villageName);
    }

    public static void handle(OpenPlayerHouseSetupPacket packet) {
        PlayerHouseSetupClient.open(packet);
    }
}