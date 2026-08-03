package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class SubmitDecreeOfTheHousePacket implements CustomPacketPayload {

    public static final Type<SubmitDecreeOfTheHousePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "submit_decree_of_the_house")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitDecreeOfTheHousePacket> CODEC =
            StreamCodec.ofMember(SubmitDecreeOfTheHousePacket::encode, SubmitDecreeOfTheHousePacket::decode);

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

    public UUID targetId() {
        return targetId;
    }

    public boolean playerTarget() {
        return playerTarget;
    }

    public String firstName() {
        return firstName;
    }

    public String currentSurname() {
        return currentSurname;
    }

    public String houseWords() {
        return houseWords;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(targetId);
        buffer.writeBoolean(playerTarget);
        buffer.writeUtf(firstName);
        buffer.writeUtf(currentSurname);
        buffer.writeUtf(houseWords);
    }

    public static SubmitDecreeOfTheHousePacket decode(RegistryFriendlyByteBuf buffer) {
        UUID targetId = buffer.readUUID();
        boolean playerTarget = buffer.readBoolean();
        String firstName = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        String houseWords = buffer.readUtf();

        return new SubmitDecreeOfTheHousePacket(targetId, playerTarget, firstName, currentSurname, houseWords);
    }

    public static void handle(SubmitDecreeOfTheHousePacket packet, ServerPlayer player) {
        if (packet == null || player == null) {
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}