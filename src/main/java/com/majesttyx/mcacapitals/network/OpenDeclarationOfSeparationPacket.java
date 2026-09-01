package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.DeclarationOfSeparationClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class OpenDeclarationOfSeparationPacket implements CustomPacketPayload {

    public static final Type<OpenDeclarationOfSeparationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MCACapitals.MODID,
                    "open_declaration_of_separation"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDeclarationOfSeparationPacket> STREAM_CODEC =
            StreamCodec.ofMember(
                    OpenDeclarationOfSeparationPacket::encode,
                    OpenDeclarationOfSeparationPacket::decode
            );

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

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(targetId);
        buffer.writeUtf(targetName);
        buffer.writeUtf(currentHouse);
        buffer.writeUtf(currentHouseWords);
    }

    private static OpenDeclarationOfSeparationPacket decode(
            RegistryFriendlyByteBuf buffer
    ) {
        return new OpenDeclarationOfSeparationPacket(
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf()
        );
    }

    public static void handle(
            OpenDeclarationOfSeparationPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() ->
                DeclarationOfSeparationClient.open(
                        packet.targetId,
                        packet.targetName,
                        packet.currentHouse,
                        packet.currentHouseWords
                )
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
