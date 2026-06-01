package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.DecreeOfTheHouseClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class OpenDecreeOfTheHousePacket implements CustomPacketPayload {

    public static final Type<OpenDecreeOfTheHousePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_decree_of_the_house"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDecreeOfTheHousePacket> STREAM_CODEC =
            StreamCodec.ofMember(OpenDecreeOfTheHousePacket::encode, OpenDecreeOfTheHousePacket::decode);

    private final UUID targetId;
    private final String firstName;
    private final String currentSurname;
    private final boolean houseFounded;
    private final String houseName;
    private final String houseWords;

    public OpenDecreeOfTheHousePacket(
            UUID targetId,
            String firstName,
            String currentSurname,
            boolean houseFounded,
            String houseName,
            String houseWords
    ) {
        this.targetId = targetId;
        this.firstName = firstName == null ? "" : firstName;
        this.currentSurname = currentSurname == null ? "" : currentSurname;
        this.houseFounded = houseFounded;
        this.houseName = houseName == null ? "" : houseName;
        this.houseWords = houseWords == null ? "" : houseWords;
    }

    public UUID targetId() {
        return targetId;
    }

    public String firstName() {
        return firstName;
    }

    public String currentSurname() {
        return currentSurname;
    }

    public boolean houseFounded() {
        return houseFounded;
    }

    public String houseName() {
        return houseName;
    }

    public String houseWords() {
        return houseWords;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(targetId);
        buffer.writeUtf(firstName);
        buffer.writeUtf(currentSurname);
        buffer.writeBoolean(houseFounded);
        buffer.writeUtf(houseName);
        buffer.writeUtf(houseWords);
    }

    private static OpenDecreeOfTheHousePacket decode(RegistryFriendlyByteBuf buffer) {
        UUID targetId = buffer.readUUID();
        String firstName = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        boolean houseFounded = buffer.readBoolean();
        String houseName = buffer.readUtf();
        String houseWords = buffer.readUtf();

        return new OpenDecreeOfTheHousePacket(
                targetId,
                firstName,
                currentSurname,
                houseFounded,
                houseName,
                houseWords
        );
    }

    public static void handle(OpenDecreeOfTheHousePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DecreeOfTheHouseClient.open(packet));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}