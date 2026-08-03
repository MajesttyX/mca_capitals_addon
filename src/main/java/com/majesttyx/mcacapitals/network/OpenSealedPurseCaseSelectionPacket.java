package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.client.SealedPurseCaseSelectionClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OpenSealedPurseCaseSelectionPacket implements CustomPacketPayload {

    private static final int MAX_CASES = 512;

    public static final Type<OpenSealedPurseCaseSelectionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    MCACapitals.MODID,
                    "open_sealed_purse_case_selection"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSealedPurseCaseSelectionPacket> CODEC =
            StreamCodec.ofMember(
                    OpenSealedPurseCaseSelectionPacket::encode,
                    OpenSealedPurseCaseSelectionPacket::decode
            );

    private final UUID capitalId;
    private final String villageName;
    private final List<CaseEntry> cases;

    public OpenSealedPurseCaseSelectionPacket(
            UUID capitalId,
            String villageName,
            List<CaseEntry> cases
    ) {
        this.capitalId = capitalId;
        this.villageName = villageName == null ? "" : villageName;
        this.cases = new ArrayList<>(cases == null ? List.of() : cases);
    }

    public UUID capitalId() {
        return capitalId;
    }

    public String villageName() {
        return villageName;
    }

    public List<CaseEntry> cases() {
        return List.copyOf(cases);
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(capitalId);
        buffer.writeUtf(villageName);
        buffer.writeInt(cases.size());

        for (CaseEntry caseEntry : cases) {
            buffer.writeUUID(caseEntry.id());
            buffer.writeUtf(caseEntry.name());
            buffer.writeUtf(caseEntry.status());
        }
    }

    private static OpenSealedPurseCaseSelectionPacket decode(RegistryFriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        int size = buffer.readInt();
        if (size < 0 || size > MAX_CASES) {
            throw new IllegalArgumentException("Invalid Sealed Purse case count: " + size);
        }

        List<CaseEntry> cases = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            cases.add(new CaseEntry(
                    buffer.readUUID(),
                    buffer.readUtf(),
                    buffer.readUtf()
            ));
        }

        return new OpenSealedPurseCaseSelectionPacket(capitalId, villageName, cases);
    }

    public static void handle(OpenSealedPurseCaseSelectionPacket packet) {
        if (packet != null) {
            SealedPurseCaseSelectionClient.open(packet);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record CaseEntry(UUID id, String name, String status) {
        public CaseEntry {
            name = name == null || name.isBlank() ? "Unknown Villager" : name;
            status = status == null || status.isBlank() ? "Active Case" : status;
        }
    }
}
