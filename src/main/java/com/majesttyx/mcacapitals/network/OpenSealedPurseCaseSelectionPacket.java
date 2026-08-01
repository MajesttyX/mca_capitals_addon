package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.client.SealedPurseCaseSelectionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenSealedPurseCaseSelectionPacket {

    private final UUID capitalId;
    private final String villageName;
    private final List<CaseEntry> cases;

    public OpenSealedPurseCaseSelectionPacket(UUID capitalId, String villageName, List<CaseEntry> cases) {
        this.capitalId = capitalId;
        this.villageName = villageName == null ? "" : villageName;
        this.cases = new ArrayList<>(cases);
    }

    public UUID capitalId() {
        return capitalId;
    }

    public String villageName() {
        return villageName;
    }

    public List<CaseEntry> cases() {
        return cases;
    }

    public static void encode(OpenSealedPurseCaseSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.capitalId);
        buffer.writeUtf(packet.villageName);
        buffer.writeInt(packet.cases.size());

        for (CaseEntry caseEntry : packet.cases) {
            buffer.writeUUID(caseEntry.id());
            buffer.writeUtf(caseEntry.name());
            buffer.writeUtf(caseEntry.status());
        }
    }

    public static OpenSealedPurseCaseSelectionPacket decode(FriendlyByteBuf buffer) {
        UUID capitalId = buffer.readUUID();
        String villageName = buffer.readUtf();
        int size = buffer.readInt();

        List<CaseEntry> cases = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cases.add(new CaseEntry(buffer.readUUID(), buffer.readUtf(), buffer.readUtf()));
        }

        return new OpenSealedPurseCaseSelectionPacket(capitalId, villageName, cases);
    }

    public static void handle(
            OpenSealedPurseCaseSelectionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> SealedPurseCaseSelectionClient.open(packet)
        ));
        context.setPacketHandled(true);
    }

    public record CaseEntry(UUID id, String name, String status) {
    }
}