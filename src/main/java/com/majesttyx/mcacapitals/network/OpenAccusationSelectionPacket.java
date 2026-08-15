package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.AccusationSelectionClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpenAccusationSelectionPacket implements CustomPacketPayload {

    public static final Type<OpenAccusationSelectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_accusation_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAccusationSelectionPacket> CODEC =
            StreamCodec.ofMember(OpenAccusationSelectionPacket::encode, OpenAccusationSelectionPacket::decode);

    private final UUID capitalId;
    private final String villageName;
    private final List<Candidate> candidates;

    public OpenAccusationSelectionPacket(UUID capitalId, String villageName, List<Candidate> candidates) {
        this.capitalId = capitalId;
        this.villageName = villageName == null ? "" : villageName;
        this.candidates = new ArrayList<>(candidates);
    }

    public UUID capitalId() {
        return capitalId;
    }

    public String villageName() {
        return villageName;
    }

    public List<Candidate> candidates() {
        return candidates;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(capitalId);
        buffer.writeUtf(villageName);
        buffer.writeInt(candidates.size());

        for (Candidate candidate : candidates) {
            buffer.writeUUID(candidate.id());
            buffer.writeUtf(candidate.name());
        }
    }

    private static OpenAccusationSelectionPacket decode(RegistryFriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        int size = buffer.readInt();

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            candidates.add(new Candidate(buffer.readUUID(), buffer.readUtf()));
        }

        return new OpenAccusationSelectionPacket(capitalId, villageName, candidates);
    }

    public static void handle(OpenAccusationSelectionPacket packet) {
        AccusationSelectionClient.open(packet);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Candidate(UUID id, String name) {
    }
}