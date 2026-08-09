package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalCampaignSavedData
        extends SavedData {
    public static final String DATA_NAME =
            "mcacapitals_campaigns";

    private static final String KEY_CAMPAIGNS =
            "Campaigns";

    private final Map<
            UUID,
            CapitalCampaignRecord
            > campaigns =
            new LinkedHashMap<>();

    public CapitalCampaignRecord getCampaign(
            UUID campaignId
    ) {
        if (campaignId == null) {
            return null;
        }

        return campaigns.get(campaignId);
    }

    public CapitalCampaignRecord getCampaignForCapital(
            UUID capitalId
    ) {
        if (capitalId == null) {
            return null;
        }

        for (CapitalCampaignRecord campaign :
                campaigns.values()) {
            if (campaign != null
                    && campaign.isActiveCampaign()
                    && campaign.containsCapital(
                    capitalId
            )) {
                return campaign;
            }
        }

        return null;
    }

    public CapitalCampaignRecord getCampaignForAttacker(
            UUID villagerId
    ) {
        if (villagerId == null) {
            return null;
        }

        for (CapitalCampaignRecord campaign :
                campaigns.values()) {
            if (campaign != null
                    && campaign.isActiveCampaign()
                    && campaign.containsAttacker(
                    villagerId
            )) {
                return campaign;
            }
        }
        return null;
    }

    public boolean addCampaign(
            CapitalCampaignRecord campaign
    ) {
        if (campaign == null
                || getCampaignForCapital(
                campaign.getAttackingCapitalId()
        ) != null
                || getCampaignForCapital(
                campaign.getDefendingCapitalId()
        ) != null) {
            return false;
        }
        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (getCampaignForAttacker(attackerId)
                    != null) {
                return false;
            }
        }

        campaigns.put(
                campaign.getCampaignId(),
                campaign
        );

        setDirty();
        return true;
    }

    public boolean removeCampaign(
            UUID campaignId
    ) {
        if (campaignId == null) {
            return false;
        }
        boolean removed =
                campaigns.remove(campaignId)
                        != null;

        if (removed) {
            setDirty();
        }

        return removed;
    }

    public List<CapitalCampaignRecord>
    getActiveCampaigns() {
        List<CapitalCampaignRecord> result =
                new ArrayList<>();
        for (CapitalCampaignRecord campaign :
                campaigns.values()) {
            if (campaign != null
                    && campaign.isActiveCampaign()) {
                result.add(campaign);
            }
        }

        return List.copyOf(result);
    }

    public Map<UUID, CapitalCampaignRecord>
    getSnapshot() {
        return new LinkedHashMap<>(campaigns);
    }

    public boolean removeCapital(
            UUID capitalId
    ) {
        if (capitalId == null) {
            return false;
        }
        boolean removed =
                campaigns.entrySet().removeIf(
                        entry ->
                                entry.getValue() != null
                                        && entry
                                        .getValue()
                                        .containsCapital(
                                                capitalId
                                        )
                );

        if (removed) {
            setDirty();
        }
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag campaignsTag =
                new ListTag();

        for (CapitalCampaignRecord campaign :
                campaigns.values()) {
            if (campaign != null) {
                campaignsTag.add(
                        campaign.save()
                );
            }
        }
        tag.put(
                KEY_CAMPAIGNS,
                campaignsTag
        );

        return tag;
    }

    public static CapitalCampaignSavedData load(CompoundTag tag) {
        CapitalCampaignSavedData data =
                new CapitalCampaignSavedData();

        ListTag campaignsTag = tag.getList(
                KEY_CAMPAIGNS,
                Tag.TAG_COMPOUND
        );
        for (Tag rawCampaign : campaignsTag) {
            CapitalCampaignRecord campaign =
                    CapitalCampaignRecord.load(
                            (CompoundTag) rawCampaign
                    );

            if (campaign != null) {
                data.campaigns.put(
                        campaign.getCampaignId(),
                        campaign
                );
            }
        }

        return data;
    }
}
