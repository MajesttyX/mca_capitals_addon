package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SubmitDecreeOfTheHousePacket {

    private final UUID targetId;
    private final String firstName;
    private final String currentSurname;
    private final String houseWords;

    public SubmitDecreeOfTheHousePacket(UUID targetId, String firstName, String currentSurname, String houseWords) {
        this.targetId = targetId;
        this.firstName = firstName == null ? "" : firstName;
        this.currentSurname = currentSurname == null ? "" : currentSurname;
        this.houseWords = houseWords == null ? "" : houseWords;
    }

    public static void encode(SubmitDecreeOfTheHousePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetId);
        buffer.writeUtf(packet.firstName);
        buffer.writeUtf(packet.currentSurname);
        buffer.writeUtf(packet.houseWords);
    }

    public static SubmitDecreeOfTheHousePacket decode(FriendlyByteBuf buffer) {
        UUID targetId = buffer.readUUID();
        String firstName = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        String houseWords = buffer.readUtf();

        return new SubmitDecreeOfTheHousePacket(targetId, firstName, currentSurname, houseWords);
    }

    public static void handle(SubmitDecreeOfTheHousePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DecreeOfTheHouseService.applyFromPacket(
                        player,
                        packet.targetId,
                        packet.firstName,
                        packet.currentSurname,
                        packet.houseWords
                );
            }
        });
        context.setPacketHandled(true);
    }
}