package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.item.SealedPurseHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public record SelectSealedPurseCasePacket(UUID capitalId, UUID targetId) {
    public static void encode(SelectSealedPurseCasePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.capitalId());
        buffer.writeUUID(packet.targetId());
    }

    public static SelectSealedPurseCasePacket decode(FriendlyByteBuf buffer) {
        return new SelectSealedPurseCasePacket(buffer.readUUID(), buffer.readUUID());
    }

    public static void handle(SelectSealedPurseCasePacket packet, ServerPlayer player) {
        if (packet != null && player != null) SealedPurseHandler.handleSelectedCase(player, packet.capitalId(), packet.targetId());
    }
}
