package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.VillagerIdentityClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SyncVillagerIdentityPacket {

    private final UUID villagerId;
    private final Component originVillageName;
    private final String originSource;
    private final String currentSurname;
    private final String baseName;
    private final String displayTitleId;
    private final Component displayTitle;
    private final Component royalGuardOrderLine;
    private final String courtOfficeId;
    private final Component courtOfficeLine;
    private final boolean houseFounded;
    private final String houseName;
    private final String houseWords;
    private final String houseWordsPersonality;

    public SyncVillagerIdentityPacket(
            UUID villagerId,
            Component originVillageName,
            String originSource,
            String currentSurname,
            String baseName,
            String displayTitleId,
            Component displayTitle,
            Component royalGuardOrderLine,
            String courtOfficeId,
            Component courtOfficeLine,
            boolean houseFounded,
            String houseName,
            String houseWords,
            String houseWordsPersonality
    ) {
        this.villagerId = villagerId;
        this.originVillageName = originVillageName == null ? Component.empty() : originVillageName;
        this.originSource = originSource == null ? "" : originSource;
        this.currentSurname = currentSurname == null ? "" : currentSurname;
        this.baseName = baseName == null ? "" : baseName;
        this.displayTitleId = displayTitleId == null ? "" : displayTitleId;
        this.displayTitle = displayTitle == null ? Component.empty() : displayTitle;
        this.royalGuardOrderLine = royalGuardOrderLine == null ? Component.empty() : royalGuardOrderLine;
        this.courtOfficeId = courtOfficeId == null ? "" : courtOfficeId;
        this.courtOfficeLine = courtOfficeLine == null ? Component.empty() : courtOfficeLine;
        this.houseFounded = houseFounded;
        this.houseName = houseName == null ? "" : houseName;
        this.houseWords = houseWords == null ? "" : houseWords;
        this.houseWordsPersonality = houseWordsPersonality == null ? "" : houseWordsPersonality;
    }

    public UUID villagerId() {
        return villagerId;
    }

    public Component originVillageName() {
        return originVillageName;
    }

    public String originSource() {
        return originSource;
    }

    public String currentSurname() {
        return currentSurname;
    }

    public String baseName() {
        return baseName;
    }

    public String displayTitleId() {
        return displayTitleId;
    }

    public Component displayTitle() {
        return displayTitle;
    }

    public Component royalGuardOrderLine() {
        return royalGuardOrderLine;
    }

    public String courtOfficeId() {
        return courtOfficeId;
    }

    public Component courtOfficeLine() {
        return courtOfficeLine;
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
        buffer.writeComponent(packet.originVillageName);
        buffer.writeUtf(packet.originSource);
        buffer.writeUtf(packet.currentSurname);
        buffer.writeUtf(packet.baseName);
        buffer.writeUtf(packet.displayTitleId);
        buffer.writeComponent(packet.displayTitle);
        buffer.writeComponent(packet.royalGuardOrderLine);
        buffer.writeUtf(packet.courtOfficeId);
        buffer.writeComponent(packet.courtOfficeLine);
        buffer.writeBoolean(packet.houseFounded);
        buffer.writeUtf(packet.houseName);
        buffer.writeUtf(packet.houseWords);
        buffer.writeUtf(packet.houseWordsPersonality);
    }

    public static SyncVillagerIdentityPacket decode(FriendlyByteBuf buffer) {
        UUID villagerId = buffer.readUUID();
        Component originVillageName = buffer.readComponent();
        String originSource = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        String baseName = buffer.readUtf();
        String displayTitleId = buffer.readUtf();
        Component displayTitle = buffer.readComponent();
        Component royalGuardOrderLine = buffer.readComponent();
        String courtOfficeId = buffer.readUtf();
        Component courtOfficeLine = buffer.readComponent();
        boolean houseFounded = buffer.readBoolean();
        String houseName = buffer.readUtf();
        String houseWords = buffer.readUtf();
        String houseWordsPersonality = buffer.readUtf();

        return new SyncVillagerIdentityPacket(
                villagerId,
                originVillageName,
                originSource,
                currentSurname,
                baseName,
                displayTitleId,
                displayTitle,
                royalGuardOrderLine,
                courtOfficeId,
                courtOfficeLine,
                houseFounded,
                houseName,
                houseWords,
                houseWordsPersonality
        );
    }

    public static void handle(
            SyncVillagerIdentityPacket packet,
            java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier
    ) {
        net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> VillagerIdentityClientCache.put(packet)
        ));
        context.setPacketHandled(true);
    }

}
