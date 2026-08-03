package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.item.RoyalCharterClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class OpenRoyalCharterDecisionPacket implements CustomPacketPayload {

    public static final Type<OpenRoyalCharterDecisionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_royal_charter_decision")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRoyalCharterDecisionPacket> CODEC =
            StreamCodec.ofMember(OpenRoyalCharterDecisionPacket::encode, OpenRoyalCharterDecisionPacket::decode);

    public OpenRoyalCharterDecisionPacket() {
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
    }

    public static OpenRoyalCharterDecisionPacket decode(RegistryFriendlyByteBuf buffer) {
        return new OpenRoyalCharterDecisionPacket();
    }

    public static void handle(OpenRoyalCharterDecisionPacket packet) {
        RoyalCharterClient.openDecisionScreen();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}