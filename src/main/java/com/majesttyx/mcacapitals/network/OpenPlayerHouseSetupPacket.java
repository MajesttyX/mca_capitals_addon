package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.PlayerHouseSetupClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

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

    public static void handle(OpenPlayerHouseSetupPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> PlayerHouseSetupClient.open(packet)
        ));
        context.setPacketHandled(true);
    }
}