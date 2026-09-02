package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.house.DeclarationOfSeparationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SubmitDeclarationOfSeparationPacket {

    private final UUID targetId;
    private final String newHouseName;
    private final String houseWords;

    public SubmitDeclarationOfSeparationPacket(
            UUID targetId,
            String newHouseName,
            String houseWords
    ) {
        this.targetId = targetId;
        this.newHouseName = newHouseName == null ? "" : newHouseName;
        this.houseWords = houseWords == null ? "" : houseWords;
    }

    public static void encode(SubmitDeclarationOfSeparationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetId);
        buffer.writeUtf(packet.newHouseName);
        buffer.writeUtf(packet.houseWords);
    }

    public static SubmitDeclarationOfSeparationPacket decode(FriendlyByteBuf buffer) {
        return new SubmitDeclarationOfSeparationPacket(
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readUtf()
        );
    }

    public static void handle(
            SubmitDeclarationOfSeparationPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DeclarationOfSeparationService.foundNewHouse(
                        player,
                        packet.targetId,
                        packet.newHouseName,
                        packet.houseWords
                );
            }
        });
        context.setPacketHandled(true);
    }
}
