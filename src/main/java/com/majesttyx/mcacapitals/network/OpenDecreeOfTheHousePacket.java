package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.DecreeOfTheHouseClient;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class OpenDecreeOfTheHousePacket {

    private final UUID targetId;
    private final boolean playerTarget;
    private final String firstName;
    private final String currentSurname;
    private final boolean houseFounded;
    private final String houseName;
    private final String houseWords;

    public OpenDecreeOfTheHousePacket(
            UUID targetId,
            boolean playerTarget,
            String firstName,
            String currentSurname,
            boolean houseFounded,
            String houseName,
            String houseWords
    ) {
        this.targetId = targetId;
        this.playerTarget = playerTarget;
        this.firstName = firstName == null ? "" : firstName;
        this.currentSurname = currentSurname == null ? "" : currentSurname;
        this.houseFounded = houseFounded;
        this.houseName = houseName == null ? "" : houseName;
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

    public boolean houseFounded() {
        return houseFounded;
    }

    public String houseName() {
        return houseName;
    }

    public String houseWords() {
        return houseWords;
    }

    public static void encode(OpenDecreeOfTheHousePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetId);
        buffer.writeBoolean(packet.playerTarget);
        buffer.writeUtf(packet.firstName);
        buffer.writeUtf(packet.currentSurname);
        buffer.writeBoolean(packet.houseFounded);
        buffer.writeUtf(packet.houseName);
        buffer.writeUtf(packet.houseWords);
    }

    public static OpenDecreeOfTheHousePacket decode(FriendlyByteBuf buffer) {
        UUID targetId = buffer.readUUID();
        boolean playerTarget = buffer.readBoolean();
        String firstName = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        boolean houseFounded = buffer.readBoolean();
        String houseName = buffer.readUtf();
        String houseWords = buffer.readUtf();

        return new OpenDecreeOfTheHousePacket(
                targetId,
                playerTarget,
                firstName,
                currentSurname,
                houseFounded,
                houseName,
                houseWords
        );
    }

    public static void handle(OpenDecreeOfTheHousePacket packet) {
        DecreeOfTheHouseClient.open(packet);
    }
}