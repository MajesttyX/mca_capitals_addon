package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class RoyalDecreeBetrothalClientHelper {

    private RoyalDecreeBetrothalClientHelper() {
    }

    public static BetrothalDisplayData getPendingRoyalDecreeBetrothal(UUID villagerId) {
        if (villagerId == null) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return null;
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (!PendingVillagerBetrothalAccess.hasPendingBetrothal(level, villagerId)) {
                continue;
            }

            UUID partnerId = PendingVillagerBetrothalAccess.getPartner(level, villagerId);
            if (partnerId == null) {
                continue;
            }

            Entity partner = MCAIntegrationBridge.getEntityByUuid(level, partnerId);
            String storedPartnerName = PendingVillagerBetrothalAccess.getPartnerName(level, villagerId);
            Component partnerName = partner != null
                    ? partner.getName()
                    : storedPartnerName == null || storedPartnerName.isBlank()
                    ? Component.translatable("mcacapitals.system.common.unknown")
                    : Component.literal(storedPartnerName);

            return new BetrothalDisplayData(villagerId, partnerId, partnerName);
        }

        return null;
    }

    public record BetrothalDisplayData(UUID villagerId, UUID partnerId, Component partnerName) {
    }
}