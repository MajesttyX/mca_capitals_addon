package com.majesttyx.mcacapitals.network;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalPlayerAuthorityResolver;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.client.BlueprintAuthorityClientCache;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.MCAReputationBridge;
import forge.net.conczin.mca.server.world.data.Village;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

public final class SyncBlueprintAuthorityPacket {

    private final int villageId;
    private final boolean activeCapital;
    private final Component displayTitle;
    private final int permissionMask;
    private final int population;
    private final int reputation;
    private final int masterProfessionals;
    private final int sovereignReputation;
    private final boolean villagerSovereign;

    public SyncBlueprintAuthorityPacket(
            int villageId,
            boolean activeCapital,
            Component displayTitle,
            int permissionMask,
            int population,
            int reputation,
            int masterProfessionals,
            int sovereignReputation,
            boolean villagerSovereign
    ) {
        this.villageId = villageId;
        this.activeCapital = activeCapital;
        this.displayTitle = displayTitle == null
                ? Component.translatable("mcacapitals.dynamic.rank.stranger")
                : displayTitle;
        this.permissionMask = permissionMask;
        this.population = population;
        this.reputation = reputation;
        this.masterProfessionals = masterProfessionals;
        this.sovereignReputation = sovereignReputation;
        this.villagerSovereign = villagerSovereign;
    }

    public static SyncBlueprintAuthorityPacket create(ServerPlayer player, Village village) {
        if (player == null || village == null) {
            return new SyncBlueprintAuthorityPacket(
                    -1,
                    false,
                    Component.translatable("mcacapitals.dynamic.rank.stranger"),
                    0,
                    0,
                    0,
                    0,
                    0,
                    false
            );
        }

        int villageId = village.getId();
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalManager.getCapitalByVillageId(level, villageId);
        boolean activeCapital = capital != null && capital.getState() == CapitalState.ACTIVE;

        if (!activeCapital) {
            return new SyncBlueprintAuthorityPacket(
                    villageId,
                    false,
                    Component.translatable("mcacapitals.dynamic.rank.stranger"),
                    0,
                    village.getPopulation(),
                    0,
                    0,
                    0,
                    false
            );
        }

        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(level, villageId);
        int reputation = MCAReputationBridge.getCapitalHeartsScore(level, residents, player.getUUID());
        int population = MCAIntegrationBridge.getVillagePopulation(level, villageId);
        int masterProfessionals = countMasterProfessionals(level, residents);

        UUID sovereignId = capital.getSovereign();
        boolean villagerSovereign = sovereignId != null && MCAIntegrationBridge.isAliveMCAVillager(level, sovereignId);
        int sovereignReputation = villagerSovereign
                ? MCAReputationBridge.getHeartsWithVillager(level, sovereignId, player.getUUID())
                : 0;

        CapitalPlayerAuthorityResolver.ResolvedAuthority authority =
                CapitalPlayerAuthorityResolver.resolve(level, capital, player.getUUID());

        return new SyncBlueprintAuthorityPacket(
                villageId,
                true,
                authority.displayTitle(),
                authority.permissionMask(),
                population,
                reputation,
                masterProfessionals,
                sovereignReputation,
                villagerSovereign
        );
    }

    private static int countMasterProfessionals(ServerLevel level, Set<UUID> residents) {
        int count = 0;
        for (UUID residentId : residents) {
            if (MCAIntegrationBridge.isMasterProfessionVillager(level, residentId)) {
                count++;
            }
        }
        return count;
    }

    public int villageId() {
        return villageId;
    }

    public boolean activeCapital() {
        return activeCapital;
    }

    public Component displayTitle() {
        return displayTitle;
    }

    public int permissionMask() {
        return permissionMask;
    }

    public int population() {
        return population;
    }

    public int reputation() {
        return reputation;
    }

    public int masterProfessionals() {
        return masterProfessionals;
    }

    public int sovereignReputation() {
        return sovereignReputation;
    }

    public boolean villagerSovereign() {
        return villagerSovereign;
    }

    public boolean hasPermission(CapitalPlayerAuthorityResolver.Permission permission) {
        return permission != null && (permissionMask & (1 << permission.ordinal())) != 0;
    }

    public static void encode(SyncBlueprintAuthorityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.villageId);
        buffer.writeBoolean(packet.activeCapital);
        buffer.writeComponent(packet.displayTitle);
        buffer.writeInt(packet.permissionMask);
        buffer.writeInt(packet.population);
        buffer.writeInt(packet.reputation);
        buffer.writeInt(packet.masterProfessionals);
        buffer.writeInt(packet.sovereignReputation);
        buffer.writeBoolean(packet.villagerSovereign);
    }

    public static SyncBlueprintAuthorityPacket decode(FriendlyByteBuf buffer) {
        return new SyncBlueprintAuthorityPacket(
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readComponent(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(
            SyncBlueprintAuthorityPacket packet,
            java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier
    ) {
        net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> BlueprintAuthorityClientCache.put(packet)
        ));
        context.setPacketHandled(true);
    }
}
