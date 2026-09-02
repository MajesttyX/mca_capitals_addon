package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.house.DeclarationOfSeparationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class SubmitDeclarationOfSeparationPacket {
    private final UUID targetId;
    private final String newHouseName;
    private final String houseWords;

    public SubmitDeclarationOfSeparationPacket(UUID targetId, String newHouseName, String houseWords) {
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
        return new SubmitDeclarationOfSeparationPacket(buffer.readUUID(), buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(SubmitDeclarationOfSeparationPacket packet, ServerPlayer player) {
        if (player != null) {
            DeclarationOfSeparationService.foundNewHouse(player, packet.targetId, packet.newHouseName, packet.houseWords);
        }
    }
}
