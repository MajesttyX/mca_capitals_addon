package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.house.DeclarationOfSeparationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SubmitDeclarationOfSeparationPacket {

    private static final int MAX_HOUSE_NAME_LENGTH = 20;
    private static final int MAX_HOUSE_WORDS_LENGTH = 80;

    private final UUID authorizationToken;
    private final String newHouseName;
    private final String houseWords;

    public SubmitDeclarationOfSeparationPacket(
            UUID authorizationToken,
            String newHouseName,
            String houseWords
    ) {
        this.authorizationToken = authorizationToken;
        this.newHouseName = newHouseName == null ? "" : newHouseName;
        this.houseWords = houseWords == null ? "" : houseWords;
    }

    public static void encode(SubmitDeclarationOfSeparationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.authorizationToken);
        buffer.writeUtf(packet.newHouseName, MAX_HOUSE_NAME_LENGTH);
        buffer.writeUtf(packet.houseWords, MAX_HOUSE_WORDS_LENGTH);
    }

    public static SubmitDeclarationOfSeparationPacket decode(FriendlyByteBuf buffer) {
        return new SubmitDeclarationOfSeparationPacket(
                buffer.readUUID(),
                buffer.readUtf(MAX_HOUSE_NAME_LENGTH),
                buffer.readUtf(MAX_HOUSE_WORDS_LENGTH)
        );
    }

    public static void handle(
            SubmitDeclarationOfSeparationPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            PendingNetworkInteractionService.PendingInteraction authorization =
                    PendingNetworkInteractionService.consume(
                            player,
                            PendingNetworkInteractionService.InteractionType.DECLARATION_OF_SEPARATION,
                            packet.authorizationToken
                    );

            if (authorization == null) {
                return;
            }

            DeclarationOfSeparationService.foundNewHouse(
                    player,
                    authorization.targetId(),
                    packet.newHouseName,
                    packet.houseWords
            );
        });
        context.setPacketHandled(true);
    }
}
