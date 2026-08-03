package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.ChronicleBookClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class OpenCapitalChroniclePacket implements CustomPacketPayload {

    public static final Type<OpenCapitalChroniclePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_capital_chronicle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCapitalChroniclePacket> CODEC =
            StreamCodec.ofMember(OpenCapitalChroniclePacket::encode, OpenCapitalChroniclePacket::decode);

    private final ItemStack bookStack;

    public OpenCapitalChroniclePacket(ItemStack bookStack) {
        this.bookStack = bookStack == null ? ItemStack.EMPTY : bookStack.copy();
    }

    public ItemStack bookStack() {
        return bookStack;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.bookStack);
    }

    public static OpenCapitalChroniclePacket decode(RegistryFriendlyByteBuf buffer) {
        return new OpenCapitalChroniclePacket(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    public static void handle(OpenCapitalChroniclePacket packet) {
        if (packet != null) {
            ChronicleBookClient.openBook(packet.bookStack);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}