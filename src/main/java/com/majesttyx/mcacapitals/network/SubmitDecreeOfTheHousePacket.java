package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class SubmitDecreeOfTheHousePacket {

    private final UUID targetId;
    private final boolean playerTarget;
    private final String firstName;
    private final String currentSurname;
    private final String houseWords;

    public SubmitDecreeOfTheHousePacket(UUID targetId, boolean playerTarget, String firstName, String currentSurname, String houseWords) {
        this.targetId = targetId;
        this.playerTarget = playerTarget;
        this.firstName = firstName == null ? "" : firstName;
        this.currentSurname = currentSurname == null ? "" : currentSurname;
        this.houseWords = houseWords == null ? "" : houseWords;
    }

    public static void encode(SubmitDecreeOfTheHousePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetId);
        buffer.writeBoolean(packet.playerTarget);
        buffer.writeUtf(packet.firstName);
        buffer.writeUtf(packet.currentSurname);
        buffer.writeUtf(packet.houseWords);
    }

    public static SubmitDecreeOfTheHousePacket decode(FriendlyByteBuf buffer) {
        UUID targetId = buffer.readUUID();
        boolean playerTarget = buffer.readBoolean();
        String firstName = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        String houseWords = buffer.readUtf();

        return new SubmitDecreeOfTheHousePacket(targetId, playerTarget, firstName, currentSurname, houseWords);
    }

    public static void handle(SubmitDecreeOfTheHousePacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }

        DecreeOfTheHouseService.applyFromPacket(
                player,
                packet.targetId,
                packet.playerTarget,
                packet.firstName,
                packet.currentSurname,
                packet.houseWords
        );
    }
}