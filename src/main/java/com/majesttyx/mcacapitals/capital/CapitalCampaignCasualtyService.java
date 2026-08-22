package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.conczin.mca.server.world.data.Village;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapitalCampaignCasualtyService {

    private static final long MISSING_DEFENDER_GRACE_TICKS =
            20L * 5L;

    private static final long REPAIR_INTERVAL_TICKS =
            20L;

    private static final long CLEANUP_INTERVAL_TICKS =
            20L * 60L;

    private CapitalCampaignCasualtyService() {
    }

    public static void onLivingDeath(
            Entity entity
    ) {
        if (entity == null
                || !(entity.level()
                instanceof ServerLevel level)
                || !MCAIntegrationBridge
                .isMCAVillagerEntity(entity)) {
            return;
        }

        UUID villagerId = entity.getUUID();

        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess
                        .getActiveCampaigns(level)) {
            if (campaign == null) {
                continue;
            }

            if (campaign.containsAttacker(villagerId)) {
                markAttackerDefeated(
                        level,
                        campaign.getCampaignId(),
                        villagerId
                );
            }

            if (campaign.containsDefender(villagerId)) {
                markDefenderDefeated(
                        level,
                        campaign.getCampaignId(),
                        villagerId
                );
            }
        }
    }

    public static void onLevelTick(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        if (level.getGameTime()
                % REPAIR_INTERVAL_TICKS == 0L) {
            pruneDefeatedFieldDefenders(level);
        }

        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.overworld();

        if (level == overworld
                && overworld.getGameTime()
                % CLEANUP_INTERVAL_TICKS == 0L) {
            Set<UUID> liveCampaignIds =
                    CapitalCampaignDataAccess
                            .getSnapshot(overworld)
                            .keySet();

            get(overworld).retainCampaigns(
                    liveCampaignIds
            );
        }
    }

    private static void pruneDefeatedFieldDefenders(
            ServerLevel level
    ) {
        for (CapitalCampaignRecord campaign :
                CapitalCampaignDataAccess
                        .getActiveCampaigns(level)) {
            if (campaign == null
                    || campaign.getPhase()
                    != CapitalCampaignPhase.ACTIVE
                    || campaign.getDefenderIds()
                    .isEmpty()) {
                continue;
            }

            CapitalRecord defendingCapital =
                    CapitalManager.getCapital(
                            campaign
                                    .getDefendingCapitalId()
                    );

            if (defendingCapital == null) {
                continue;
            }

            List<UUID> survivingDefenders =
                    new ArrayList<>();

            for (UUID defenderId :
                    campaign.getDefenderIds()) {
                if (!isFieldDefenderDefeated(
                        level,
                        campaign,
                        defendingCapital,
                        defenderId
                )) {
                    survivingDefenders.add(
                            defenderId
                    );
                }
            }

            if (survivingDefenders.size()
                    == campaign
                    .getDefenderIds()
                    .size()) {
                continue;
            }

            campaign.setDefenderIds(
                    survivingDefenders
            );

            CapitalCampaignDataAccess
                    .get(level)
                    .setDirty();
        }
    }

    static boolean isAttackerDefeated(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            UUID attackerId
    ) {
        if (level == null
                || campaign == null
                || attackerId == null) {
            return true;
        }

        if (get(level).isAttackerDefeated(
                campaign.getCampaignId(),
                attackerId
        )) {
            return true;
        }

        if (MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        attackerId
                )) {
            markAttackerDefeated(
                    level,
                    campaign.getCampaignId(),
                    attackerId
            );

            return true;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                attackerId
                        );

        if (entity != null
                && (!entity.isAlive()
                || entity.isRemoved())) {
            markAttackerDefeated(
                    level,
                    campaign.getCampaignId(),
                    attackerId
            );

            return true;
        }

        return false;
    }

    static boolean isFieldDefenderDefeated(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital,
            UUID defenderId
    ) {
        if (level == null
                || campaign == null
                || defenderId == null) {
            return true;
        }

        if (get(level).isDefenderDefeated(
                campaign.getCampaignId(),
                defenderId
        )) {
            return true;
        }

        if (MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        defenderId
                )) {
            markDefenderDefeated(
                    level,
                    campaign.getCampaignId(),
                    defenderId
            );

            return true;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                defenderId
                        );

        if (entity != null) {
            if (!entity.isAlive()
                    || entity.isRemoved()) {
                markDefenderDefeated(
                        level,
                        campaign.getCampaignId(),
                        defenderId
                );

                return true;
            }

            return false;
        }

        if (mayRepairMissingDefender(
                level,
                campaign,
                defendingCapital
        )) {
            markDefenderDefeated(
                    level,
                    campaign.getCampaignId(),
                    defenderId
            );

            return true;
        }

        return false;
    }

    private static boolean mayRepairMissingDefender(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital
    ) {
        if (campaign.getPhase()
                != CapitalCampaignPhase.ACTIVE
                || level.getGameTime()
                - campaign.getActivatedAt()
                < MISSING_DEFENDER_GRACE_TICKS) {
            return false;
        }

        Village village =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (village == null) {
            return false;
        }

        for (ServerPlayer player : level.players()) {
            if (player != null
                    && player.isAlive()
                    && !player.isSpectator()
                    && village.isWithinBorder(player)) {
                return true;
            }
        }

        return false;
    }

    private static void markAttackerDefeated(
            ServerLevel level,
            UUID campaignId,
            UUID attackerId
    ) {
        get(level).markAttackerDefeated(
                campaignId,
                attackerId
        );
    }

    private static void markDefenderDefeated(
            ServerLevel level,
            UUID campaignId,
            UUID defenderId
    ) {
        get(level).markDefenderDefeated(
                campaignId,
                defenderId
        );
    }

    private static CasualtySavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CasualtySavedData::load,
                        CasualtySavedData::new,
                        CasualtySavedData.DATA_NAME
                );
    }

    private static final class CasualtySavedData
            extends SavedData {

        private static final String DATA_NAME =
                "mcacapitals_campaign_casualties";

        private static final String KEY_CAMPAIGNS =
                "Campaigns";

        private static final String KEY_CAMPAIGN_ID =
                "CampaignId";

        private static final String KEY_DEFEATED_ATTACKERS =
                "DefeatedAttackers";

        private static final String KEY_DEFEATED_DEFENDERS =
                "DefeatedDefenders";

        private static final String KEY_ENTITY_ID =
                "EntityId";

        private final Map<UUID, CasualtyEntry> entries =
                new LinkedHashMap<>();

        boolean isAttackerDefeated(
                UUID campaignId,
                UUID attackerId
        ) {
            CasualtyEntry entry =
                    entries.get(campaignId);

            return entry != null
                    && entry.defeatedAttackers()
                    .contains(attackerId);
        }

        boolean isDefenderDefeated(
                UUID campaignId,
                UUID defenderId
        ) {
            CasualtyEntry entry =
                    entries.get(campaignId);

            return entry != null
                    && entry.defeatedDefenders()
                    .contains(defenderId);
        }

        void markAttackerDefeated(
                UUID campaignId,
                UUID attackerId
        ) {
            if (campaignId == null
                    || attackerId == null) {
                return;
            }

            CasualtyEntry entry =
                    entries.computeIfAbsent(
                            campaignId,
                            ignored ->
                                    new CasualtyEntry()
                    );

            if (entry.defeatedAttackers()
                    .add(attackerId)) {
                setDirty();
            }
        }

        void markDefenderDefeated(
                UUID campaignId,
                UUID defenderId
        ) {
            if (campaignId == null
                    || defenderId == null) {
                return;
            }

            CasualtyEntry entry =
                    entries.computeIfAbsent(
                            campaignId,
                            ignored ->
                                    new CasualtyEntry()
                    );

            if (entry.defeatedDefenders()
                    .add(defenderId)) {
                setDirty();
            }
        }

        void retainCampaigns(
                Set<UUID> campaignIds
        ) {
            Set<UUID> retained =
                    campaignIds == null
                            ? Set.of()
                            : new LinkedHashSet<>(
                            campaignIds
                    );

            if (entries.keySet()
                    .retainAll(retained)) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(
                CompoundTag tag
        ) {
            ListTag campaignsTag =
                    new ListTag();

            for (Map.Entry<
                    UUID,
                    CasualtyEntry
                    > entry :
                    entries.entrySet()) {
                CompoundTag campaignTag =
                        new CompoundTag();

                campaignTag.putUUID(
                        KEY_CAMPAIGN_ID,
                        entry.getKey()
                );

                campaignTag.put(
                        KEY_DEFEATED_ATTACKERS,
                        saveIds(
                                entry.getValue()
                                        .defeatedAttackers()
                        )
                );

                campaignTag.put(
                        KEY_DEFEATED_DEFENDERS,
                        saveIds(
                                entry.getValue()
                                        .defeatedDefenders()
                        )
                );

                campaignsTag.add(campaignTag);
            }

            tag.put(
                    KEY_CAMPAIGNS,
                    campaignsTag
            );

            return tag;
        }

        static CasualtySavedData load(
                CompoundTag tag
        ) {
            CasualtySavedData data =
                    new CasualtySavedData();

            ListTag campaignsTag =
                    tag.getList(
                            KEY_CAMPAIGNS,
                            Tag.TAG_COMPOUND
                    );

            for (Tag rawCampaign :
                    campaignsTag) {
                CompoundTag campaignTag =
                        (CompoundTag) rawCampaign;

                if (!campaignTag.hasUUID(
                        KEY_CAMPAIGN_ID
                )) {
                    continue;
                }

                CasualtyEntry entry =
                        new CasualtyEntry();

                entry.defeatedAttackers()
                        .addAll(
                                loadIds(
                                        campaignTag
                                                .getList(
                                                        KEY_DEFEATED_ATTACKERS,
                                                        Tag.TAG_COMPOUND
                                                )
                                )
                        );

                entry.defeatedDefenders()
                        .addAll(
                                loadIds(
                                        campaignTag
                                                .getList(
                                                        KEY_DEFEATED_DEFENDERS,
                                                        Tag.TAG_COMPOUND
                                                )
                                )
                        );

                data.entries.put(
                        campaignTag.getUUID(
                                KEY_CAMPAIGN_ID
                        ),
                        entry
                );
            }

            return data;
        }

        private static ListTag saveIds(
                Set<UUID> ids
        ) {
            ListTag tag = new ListTag();

            for (UUID id : ids) {
                CompoundTag entry =
                        new CompoundTag();

                entry.putUUID(
                        KEY_ENTITY_ID,
                        id
                );

                tag.add(entry);
            }

            return tag;
        }

        private static List<UUID> loadIds(
                ListTag tag
        ) {
            List<UUID> result =
                    new ArrayList<>();

            for (Tag raw : tag) {
                CompoundTag entry =
                        (CompoundTag) raw;

                if (entry.hasUUID(
                        KEY_ENTITY_ID
                )) {
                    result.add(
                            entry.getUUID(
                                    KEY_ENTITY_ID
                            )
                    );
                }
            }

            return result;
        }
    }

    private record CasualtyEntry(
            Set<UUID> defeatedAttackers,
            Set<UUID> defeatedDefenders
    ) {
        private CasualtyEntry() {
            this(
                    new LinkedHashSet<>(),
                    new LinkedHashSet<>()
            );
        }
    }
}
