package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.house.DeclarationOfSeparationService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class SubmitDeclarationOfSeparationPacket implements CustomPacketPayload {

    public static final Type<SubmitDeclarationOfSeparationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MCACapitals.MODID,
                    "submit_declaration_of_separation"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitDeclarationOfSeparationPacket> STREAM_CODEC =
            StreamCodec.ofMember(
                    SubmitDeclarationOfSeparationPacket::encode,
                    SubmitDeclarationOfSeparationPacket::decode
            );

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

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(targetId);
        buffer.writeUtf(newHouseName);
        buffer.writeUtf(houseWords);
    }

    private static SubmitDeclarationOfSeparationPacket decode(
            RegistryFriendlyByteBuf buffer
    ) {
        return new SubmitDeclarationOfSeparationPacket(
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readUtf()
        );
    }

    public static void handle(
            SubmitDeclarationOfSeparationPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DeclarationOfSeparationService.foundNewHouse(
                        player,
                        packet.targetId,
                        packet.newHouseName,
                        packet.houseWords
                );
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
