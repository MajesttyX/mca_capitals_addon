package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.BetrothalSelectionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpenBetrothalSelectionPacket {

    private static final int MAX_CANDIDATES = 512;

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
        this.villageName = villageName == null ? "" : villageName;
        this.playerCandidates = new ArrayList<>(playerCandidates == null ? List.of() : playerCandidates);
        this.recommendationCandidates = new ArrayList<>(recommendationCandidates == null ? List.of() : recommendationCandidates);
    }

    public UUID capitalId() { return capitalId; }
    public String villageName() { return villageName; }
    public List<Candidate> playerCandidates() { return playerCandidates; }
    public List<Candidate> recommendationCandidates() { return recommendationCandidates; }

    public static void encode(OpenBetrothalSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.capitalId);
        buffer.writeUtf(packet.villageName);
        writeCandidates(buffer, packet.playerCandidates);
        writeCandidates(buffer, packet.recommendationCandidates);
    }

    public static OpenBetrothalSelectionPacket decode(FriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        return new OpenBetrothalSelectionPacket(
                capitalId,
                villageName,
                readCandidates(buffer),
                readCandidates(buffer)
        );
    }

    public static void handle(OpenBetrothalSelectionPacket packet) {
        if (packet != null) {
            BetrothalSelectionClient.open(packet);
        }
    }

    private static void writeCandidates(FriendlyByteBuf buffer, List<Candidate> candidates) {
        buffer.writeInt(candidates.size());
        for (Candidate candidate : candidates) {
            buffer.writeUUID(candidate.id);
            buffer.writeComponent(candidate.name);
        }
    }

    private static List<Candidate> readCandidates(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        if (size < 0 || size > MAX_CANDIDATES) {
            throw new IllegalArgumentException("Invalid betrothal candidate count: " + size);
        }
        List<Candidate> candidates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            candidates.add(new Candidate(buffer.readUUID(), buffer.readComponent()));
        }
        return candidates;
    }

    public static class Candidate {
        private final UUID id;
        private final Component name;

        public Candidate(UUID id, Component name) {
            this.id = id;
            this.name = name == null ? Component.empty() : name;
        }

        public UUID id() { return id; }
        public Component name() { return name; }
    }
}
