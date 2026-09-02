package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.DeclarationOfSeparationClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenDeclarationOfSeparationPacket {

    private final UUID targetId;
    private final String targetName;
    private final String currentHouse;
    private final String currentHouseWords;

    public OpenDeclarationOfSeparationPacket(
            UUID targetId,
            String targetName,
            String currentHouse,
            String currentHouseWords
    ) {
        this.targetId = targetId;
        this.targetName = targetName == null ? "" : targetName;
        this.currentHouse = currentHouse == null ? "" : currentHouse;
        this.currentHouseWords = currentHouseWords == null ? "" : currentHouseWords;
    }

    public static void encode(OpenDeclarationOfSeparationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetId);
        buffer.writeUtf(packet.targetName);
        buffer.writeUtf(packet.currentHouse);
        buffer.writeUtf(packet.currentHouseWords);
    }

    public static OpenDeclarationOfSeparationPacket decode(FriendlyByteBuf buffer) {
        return new OpenDeclarationOfSeparationPacket(
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf()
        );
    }

    public static void handle(
            OpenDeclarationOfSeparationPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> DeclarationOfSeparationClient.open(
                        packet.targetId,
                        packet.targetName,
                        packet.currentHouse,
                        packet.currentHouseWords
                )
        ));
        context.setPacketHandled(true);
    }
}
