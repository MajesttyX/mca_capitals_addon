package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalCampaignDataAccess {

    private CapitalCampaignDataAccess() {
    }

    public static CapitalCampaignSavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CapitalCampaignSavedData::load,
                        CapitalCampaignSavedData::new,
                        CapitalCampaignSavedData.DATA_NAME
                );
    }

    public static CapitalCampaignRecord getCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        if (level == null || campaignId == null) {
            return null;
        }

        return get(level).getCampaign(campaignId);
    }

    public static CapitalCampaignRecord
    getCampaignForCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return null;
        }

        return get(level).getCampaignForCapital(
                capitalId
        );
    }

    public static CapitalCampaignRecord
    getCampaignForAttacker(
            ServerLevel level,
            UUID villagerId
    ) {
        if (level == null || villagerId == null) {
            return null;
        }

        return get(level).getCampaignForAttacker(
                villagerId
        );
    }

    public static boolean addCampaign(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        return level != null
                && campaign != null
                && get(level).addCampaign(campaign);
    }

    public static boolean removeCampaign(
            ServerLevel level,
            UUID campaignId
    ) {
        return level != null
                && campaignId != null
                && get(level).removeCampaign(
                campaignId
        );
    }

    public static List<CapitalCampaignRecord>
    getActiveCampaigns(
            ServerLevel level
    ) {
        if (level == null) {
            return List.of();
        }

        return get(level).getActiveCampaigns();
    }

    public static Map<UUID, CapitalCampaignRecord>
    getSnapshot(
            ServerLevel level
    ) {
        if (level == null) {
            return Map.of();
        }

        return get(level).getSnapshot();
    }

    public static boolean removeCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        return level != null
                && capitalId != null
                && get(level).removeCapital(
                capitalId
        );
    }
}