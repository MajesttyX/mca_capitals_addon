package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.ChronicleBookClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenCapitalChroniclePacket implements CustomPacketPayload {

    public static final Type<OpenCapitalChroniclePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_capital_chronicle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCapitalChroniclePacket> STREAM_CODEC =
            StreamCodec.ofMember(OpenCapitalChroniclePacket::encode, OpenCapitalChroniclePacket::decode);

    private final ItemStack bookStack;

    public OpenCapitalChroniclePacket(ItemStack bookStack) {
        this.bookStack = bookStack.copy();
    }

    public ItemStack bookStack() {
        return bookStack;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, bookStack);
    }

    private static OpenCapitalChroniclePacket decode(RegistryFriendlyByteBuf buffer) {
        return new OpenCapitalChroniclePacket(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    public static void handle(OpenCapitalChroniclePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ChronicleBookClient.openBook(packet.bookStack));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}