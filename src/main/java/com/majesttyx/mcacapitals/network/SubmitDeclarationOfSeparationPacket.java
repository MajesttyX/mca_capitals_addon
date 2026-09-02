package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.house.DeclarationOfSeparationService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class SubmitDeclarationOfSeparationPacket implements CustomPacketPayload {
    public static final Type<SubmitDeclarationOfSeparationPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "submit_declaration_of_separation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitDeclarationOfSeparationPacket> CODEC =
            StreamCodec.ofMember(SubmitDeclarationOfSeparationPacket::encode, SubmitDeclarationOfSeparationPacket::decode);
    private final UUID targetId; private final String newHouseName; private final String houseWords;
    public SubmitDeclarationOfSeparationPacket(UUID targetId,String newHouseName,String houseWords){this.targetId=targetId;this.newHouseName=newHouseName==null?"":newHouseName;this.houseWords=houseWords==null?"":houseWords;}
    public UUID targetId(){return targetId;} public String newHouseName(){return newHouseName;} public String houseWords(){return houseWords;}
    public void encode(RegistryFriendlyByteBuf b){b.writeUUID(targetId);b.writeUtf(newHouseName);b.writeUtf(houseWords);}
    public static SubmitDeclarationOfSeparationPacket decode(RegistryFriendlyByteBuf b){return new SubmitDeclarationOfSeparationPacket(b.readUUID(),b.readUtf(),b.readUtf());}
    public static void handle(SubmitDeclarationOfSeparationPacket p, ServerPlayer player){if(p!=null&&player!=null)DeclarationOfSeparationService.foundNewHouse(player,p.targetId,p.newHouseName,p.houseWords);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
