package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.item.SealedPurseHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record SelectSealedPurseCasePacket(UUID capitalId, UUID targetId)
        implements CustomPacketPayload {

    public static final Type<SelectSealedPurseCasePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    MCACapitals.MODID,
                    "select_sealed_purse_case"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectSealedPurseCasePacket> CODEC =
            StreamCodec.ofMember(
                    SelectSealedPurseCasePacket::encode,
                    SelectSealedPurseCasePacket::decode
            );

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(capitalId);
        buffer.writeUUID(targetId);
    }

    private static SelectSealedPurseCasePacket decode(RegistryFriendlyByteBuf buffer) {
        return new SelectSealedPurseCasePacket(buffer.readUUID(), buffer.readUUID());
    }

    public static void handle(SelectSealedPurseCasePacket packet, ServerPlayer player) {
        if (packet != null && player != null) {
            SealedPurseHandler.handleSelectedCase(player, packet.capitalId(), packet.targetId());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
