package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.VillagerIdentityClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncVillagerIdentityPacket {

    private final UUID villagerId;
    private final String originVillageName;
    private final String originSource;
    private final String currentSurname;
    private final String displayTitle;
    private final String royalGuardOrderLine;
    private final boolean houseFounded;
    private final String houseName;
    private final String houseWords;
    private final String houseWordsPersonality;

    public SyncVillagerIdentityPacket(
            UUID villagerId,
            String originVillageName,
            String originSource,
            String currentSurname,
            String displayTitle,
            String royalGuardOrderLine,
            boolean houseFounded,
            String houseName,
            String houseWords,
            String houseWordsPersonality
    ) {
        this.villagerId = villagerId;
        this.originVillageName = originVillageName == null ? "" : originVillageName;
        this.originSource = originSource == null ? "" : originSource;
        this.currentSurname = currentSurname == null ? "" : currentSurname;
        this.displayTitle = displayTitle == null ? "" : displayTitle;
        this.royalGuardOrderLine = royalGuardOrderLine == null ? "" : royalGuardOrderLine;
        this.houseFounded = houseFounded;
        this.houseName = houseName == null ? "" : houseName;
        this.houseWords = houseWords == null ? "" : houseWords;
        this.houseWordsPersonality = houseWordsPersonality == null ? "" : houseWordsPersonality;
    }

    public UUID villagerId() {
        return villagerId;
    }

    public String originVillageName() {
        return originVillageName;
    }

    public String originSource() {
        return originSource;
    }

    public String currentSurname() {
        return currentSurname;
    }

    public String displayTitle() {
        return displayTitle;
    }

    public String royalGuardOrderLine() {
        return royalGuardOrderLine;
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

    public String houseWordsPersonality() {
        return houseWordsPersonality;
    }

    public static void encode(SyncVillagerIdentityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.villagerId);
        buffer.writeUtf(packet.originVillageName);
        buffer.writeUtf(packet.originSource);
        buffer.writeUtf(packet.currentSurname);
        buffer.writeUtf(packet.displayTitle);
        buffer.writeUtf(packet.royalGuardOrderLine);
        buffer.writeBoolean(packet.houseFounded);
        buffer.writeUtf(packet.houseName);
        buffer.writeUtf(packet.houseWords);
        buffer.writeUtf(packet.houseWordsPersonality);
    }

    public static SyncVillagerIdentityPacket decode(FriendlyByteBuf buffer) {
        UUID villagerId = buffer.readUUID();
        String originVillageName = buffer.readUtf();
        String originSource = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        String displayTitle = buffer.readUtf();
        String royalGuardOrderLine = buffer.readUtf();
        boolean houseFounded = buffer.readBoolean();
        String houseName = buffer.readUtf();
        String houseWords = buffer.readUtf();
        String houseWordsPersonality = buffer.readUtf();

        return new SyncVillagerIdentityPacket(
                villagerId,
                originVillageName,
                originSource,
                currentSurname,
                displayTitle,
                royalGuardOrderLine,
                houseFounded,
                houseName,
                houseWords,
                houseWordsPersonality
        );
    }

    public static void handle(SyncVillagerIdentityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> VillagerIdentityClientCache.put(packet));
        context.setPacketHandled(true);
    }
}