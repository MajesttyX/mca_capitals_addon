package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.VillagerIdentityClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class SyncVillagerIdentityPacket implements CustomPacketPayload {

    public static final Type<SyncVillagerIdentityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "sync_villager_identity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncVillagerIdentityPacket> CODEC =
            StreamCodec.ofMember(SyncVillagerIdentityPacket::encode, SyncVillagerIdentityPacket::decode);

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

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(villagerId);
        ComponentSerialization.STREAM_CODEC.encode(buffer, originVillageName);
        buffer.writeUtf(originSource);
        buffer.writeUtf(currentSurname);
        buffer.writeUtf(baseName);
        buffer.writeUtf(displayTitleId);
        ComponentSerialization.STREAM_CODEC.encode(buffer, displayTitle);
        ComponentSerialization.STREAM_CODEC.encode(buffer, royalGuardOrderLine);
        buffer.writeUtf(courtOfficeId);
        ComponentSerialization.STREAM_CODEC.encode(buffer, courtOfficeLine);
        buffer.writeBoolean(houseFounded);
        buffer.writeUtf(houseName);
        buffer.writeUtf(houseWords);
        buffer.writeUtf(houseWordsPersonality);
    }

    private static SyncVillagerIdentityPacket decode(RegistryFriendlyByteBuf buffer) {
        UUID villagerId = buffer.readUUID();
        Component originVillageName = ComponentSerialization.STREAM_CODEC.decode(buffer);
        String originSource = buffer.readUtf();
        String currentSurname = buffer.readUtf();
        String baseName = buffer.readUtf();
        String displayTitleId = buffer.readUtf();
        Component displayTitle = ComponentSerialization.STREAM_CODEC.decode(buffer);
        Component royalGuardOrderLine = ComponentSerialization.STREAM_CODEC.decode(buffer);
        String courtOfficeId = buffer.readUtf();
        Component courtOfficeLine = ComponentSerialization.STREAM_CODEC.decode(buffer);
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

    public static void handle(SyncVillagerIdentityPacket packet) {
        VillagerIdentityClientCache.put(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
