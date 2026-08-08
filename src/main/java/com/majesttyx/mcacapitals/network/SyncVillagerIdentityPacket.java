package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.VillagerIdentityClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class SyncVillagerIdentityPacket implements CustomPacketPayload {

    public static final Type<SyncVillagerIdentityPacket> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            MCACapitals.MODID,
                            "sync_villager_identity"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SyncVillagerIdentityPacket
            > CODEC =
            StreamCodec.ofMember(
                    SyncVillagerIdentityPacket::encode,
                    SyncVillagerIdentityPacket::decode
            );

    private final UUID villagerId;
    private final String originVillageName;
    private final String originSource;
    private final String currentSurname;
    private final String displayTitle;
    private final String royalGuardOrderLine;
    private final String courtOfficeLine;
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
            String courtOfficeLine,
            boolean houseFounded,
            String houseName,
            String houseWords,
            String houseWordsPersonality
    ) {
        this.villagerId = villagerId;
        this.originVillageName =
                originVillageName == null
                        ? ""
                        : originVillageName;
        this.originSource =
                originSource == null
                        ? ""
                        : originSource;
        this.currentSurname =
                currentSurname == null
                        ? ""
                        : currentSurname;
        this.displayTitle =
                displayTitle == null
                        ? ""
                        : displayTitle;
        this.royalGuardOrderLine =
                royalGuardOrderLine == null
                        ? ""
                        : royalGuardOrderLine;
        this.courtOfficeLine =
                courtOfficeLine == null
                        ? ""
                        : courtOfficeLine;
        this.houseFounded = houseFounded;
        this.houseName =
                houseName == null
                        ? ""
                        : houseName;
        this.houseWords =
                houseWords == null
                        ? ""
                        : houseWords;
        this.houseWordsPersonality =
                houseWordsPersonality == null
                        ? ""
                        : houseWordsPersonality;
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

    public String courtOfficeLine() {
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

    public void encode(
            RegistryFriendlyByteBuf buffer
    ) {
        buffer.writeUUID(villagerId);
        buffer.writeUtf(originVillageName);
        buffer.writeUtf(originSource);
        buffer.writeUtf(currentSurname);
        buffer.writeUtf(displayTitle);
        buffer.writeUtf(royalGuardOrderLine);
        buffer.writeUtf(courtOfficeLine);
        buffer.writeBoolean(houseFounded);
        buffer.writeUtf(houseName);
        buffer.writeUtf(houseWords);
        buffer.writeUtf(houseWordsPersonality);
    }

    public static SyncVillagerIdentityPacket decode(
            RegistryFriendlyByteBuf buffer
    ) {
        UUID villagerId =
                buffer.readUUID();

        String originVillageName =
                buffer.readUtf();

        String originSource =
                buffer.readUtf();

        String currentSurname =
                buffer.readUtf();

        String displayTitle =
                buffer.readUtf();

        String royalGuardOrderLine =
                buffer.readUtf();

        String courtOfficeLine =
                buffer.readUtf();

        boolean houseFounded =
                buffer.readBoolean();

        String houseName =
                buffer.readUtf();

        String houseWords =
                buffer.readUtf();

        String houseWordsPersonality =
                buffer.readUtf();

        return new SyncVillagerIdentityPacket(
                villagerId,
                originVillageName,
                originSource,
                currentSurname,
                displayTitle,
                royalGuardOrderLine,
                courtOfficeLine,
                houseFounded,
                houseName,
                houseWords,
                houseWordsPersonality
        );
    }

    public static void handle(
            SyncVillagerIdentityPacket packet
    ) {
        VillagerIdentityClientCache.put(
                packet
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}