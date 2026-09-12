package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SubmitDecreeOfTheHousePacket {

    private static final int MAX_SURNAME_LENGTH = 40;
    private static final int MAX_HOUSE_WORDS_LENGTH = 80;

    private final UUID authorizationToken;
    private final String currentSurname;
    private final String houseWords;

    public SubmitDecreeOfTheHousePacket(
            UUID authorizationToken,
            String currentSurname,
            String houseWords
    ) {
        this.authorizationToken = authorizationToken;
        this.currentSurname = currentSurname == null ? "" : currentSurname;
        this.houseWords = houseWords == null ? "" : houseWords;
    }

    public static void encode(SubmitDecreeOfTheHousePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.authorizationToken);
        buffer.writeUtf(packet.currentSurname, MAX_SURNAME_LENGTH);
        buffer.writeUtf(packet.houseWords, MAX_HOUSE_WORDS_LENGTH);
    }

    public static SubmitDecreeOfTheHousePacket decode(FriendlyByteBuf buffer) {
        return new SubmitDecreeOfTheHousePacket(
                buffer.readUUID(),
                buffer.readUtf(MAX_SURNAME_LENGTH),
                buffer.readUtf(MAX_HOUSE_WORDS_LENGTH)
        );
    }

    public static void handle(SubmitDecreeOfTheHousePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            PendingNetworkInteractionService.PendingInteraction authorization =
                    PendingNetworkInteractionService.consume(
                            player,
                            PendingNetworkInteractionService.InteractionType.DECREE_OF_THE_HOUSE,
                            packet.authorizationToken
                    );

            if (authorization == null) {
                return;
            }

            DecreeOfTheHouseService.applyFromPacket(
                    player,
                    authorization.targetId(),
                    authorization.playerTarget(),
                    packet.currentSurname,
                    packet.houseWords
            );
        });
        context.setPacketHandled(true);
    }
}
