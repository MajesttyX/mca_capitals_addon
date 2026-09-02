package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.DeclarationOfSeparationClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public class OpenDeclarationOfSeparationPacket implements CustomPacketPayload {
    public static final Type<OpenDeclarationOfSeparationPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_declaration_of_separation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDeclarationOfSeparationPacket> CODEC =
            StreamCodec.ofMember(OpenDeclarationOfSeparationPacket::encode, OpenDeclarationOfSeparationPacket::decode);
    private final UUID targetId; private final String targetName; private final String currentHouse; private final String currentHouseWords;
    public OpenDeclarationOfSeparationPacket(UUID targetId, String targetName, String currentHouse, String currentHouseWords) {
        this.targetId=targetId; this.targetName=targetName==null?"":targetName; this.currentHouse=currentHouse==null?"":currentHouse; this.currentHouseWords=currentHouseWords==null?"":currentHouseWords;
    }
    public UUID targetId(){return targetId;} public String targetName(){return targetName;} public String currentHouse(){return currentHouse;} public String currentHouseWords(){return currentHouseWords;}
    public void encode(RegistryFriendlyByteBuf b){b.writeUUID(targetId);b.writeUtf(targetName);b.writeUtf(currentHouse);b.writeUtf(currentHouseWords);}
    public static OpenDeclarationOfSeparationPacket decode(RegistryFriendlyByteBuf b){return new OpenDeclarationOfSeparationPacket(b.readUUID(),b.readUtf(),b.readUtf(),b.readUtf());}
    public static void handle(OpenDeclarationOfSeparationPacket p){DeclarationOfSeparationClient.open(p.targetId,p.targetName,p.currentHouse,p.currentHouseWords);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
