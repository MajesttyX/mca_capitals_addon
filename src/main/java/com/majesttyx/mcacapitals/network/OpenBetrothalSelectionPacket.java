package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.BetrothalSelectionClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpenBetrothalSelectionPacket implements CustomPacketPayload {

    public static final Type<OpenBetrothalSelectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCACapitals.MODID, "open_betrothal_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBetrothalSelectionPacket> STREAM_CODEC =
            StreamCodec.ofMember(OpenBetrothalSelectionPacket::encode, OpenBetrothalSelectionPacket::decode);

    private final UUID capitalId;
    private final String villageName;
    private final List<Candidate> playerCandidates;
    private final List<Candidate> recommendationCandidates;

    public OpenBetrothalSelectionPacket(
            UUID capitalId,
            String villageName,
            List<Candidate> playerCandidates,
            List<Candidate> recommendationCandidates
    ) {
        this.capitalId = capitalId;
        this.villageName = villageName;
        this.playerCandidates = new ArrayList<>(playerCandidates);
        this.recommendationCandidates = new ArrayList<>(recommendationCandidates);
    }

    public UUID capitalId() {
        return capitalId;
    }

    public String villageName() {
        return villageName;
    }

    public List<Candidate> playerCandidates() {
        return playerCandidates;
    }

    public List<Candidate> recommendationCandidates() {
        return recommendationCandidates;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(capitalId);
        buffer.writeUtf(villageName);
        writeCandidates(buffer, playerCandidates);
        writeCandidates(buffer, recommendationCandidates);
    }

    private static OpenBetrothalSelectionPacket decode(RegistryFriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        List<Candidate> playerCandidates = readCandidates(buffer);
        List<Candidate> recommendationCandidates = readCandidates(buffer);

        return new OpenBetrothalSelectionPacket(
                capitalId,
                villageName,
                playerCandidates,
                recommendationCandidates
        );
    }

    public static void handle(OpenBetrothalSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> BetrothalSelectionClient.open(packet));
    }

    private static void writeCandidates(RegistryFriendlyByteBuf buffer, List<Candidate> candidates) {
        buffer.writeInt(candidates.size());
        for (Candidate candidate : candidates) {
            buffer.writeUUID(candidate.id);
            buffer.writeUtf(candidate.name);
        }
    }

    private static List<Candidate> readCandidates(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            candidates.add(new Candidate(buffer.readUUID(), buffer.readUtf()));
        }
        return candidates;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Candidate {
        private final UUID id;
        private final String name;

        public Candidate(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }
    }
}