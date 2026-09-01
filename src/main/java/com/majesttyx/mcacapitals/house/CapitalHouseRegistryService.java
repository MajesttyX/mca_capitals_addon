package com.majesttyx.mcacapitals.house;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalHouseDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentityData;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CapitalHouseRegistryService {

    private CapitalHouseRegistryService() {
    }

    public static boolean synchronize(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null || capital == null || capital.getCapitalId() == null) {
            return false;
        }

        boolean changed = false;
        LinkedHashSet<UUID> candidates = collectCandidates(capital, residents);

        for (UUID candidateId : candidates) {
            if (candidateId == null) {
                continue;
            }

            ResolvedHouse resolved = resolveHouse(level, capital, candidateId);
            if (resolved == null || resolved.houseName().isBlank()) {
                continue;
            }

            CapitalHouseRecord previousHouse =
                    CapitalHouseDataAccess.findHouseForMember(
                            level,
                            capital.getCapitalId(),
                            candidateId
                    );

            CapitalHouseRecord house =
                    findResolvedHouse(level, capital, resolved);

            if (house == null) {
                house = createResolvedHouse(
                        level,
                        capital,
                        resolved
                );
                if (house == null) {
                    continue;
                }
                changed = true;
            }

            if (previousHouse != null
                    && !previousHouse.getHouseId().equals(house.getHouseId())) {
                changed |= transitionBetweenHouses(
                        level,
                        capital,
                        candidateId,
                        previousHouse,
                        house,
                        resolved
                );
            }

            if (!house.isActive()) {
                house.setActive(true);
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.RESTORED,
                        level.getGameTime(),
                        candidateId,
                        null
                ));
                changed = true;
            }

            if (house.getFounderId() == null && resolved.founderId() != null) {
                house.setFounderId(resolved.founderId());
                changed = true;
            }

            if (house.getFoundedGameTime() <= 0L
                    && resolved.foundedGameTime() > 0L) {
                house.setFoundedGameTime(resolved.foundedGameTime());
                changed = true;
            }

            if (house.addCurrentMember(candidateId)) {
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.MEMBER_JOINED,
                        level.getGameTime(),
                        candidateId,
                        null
                ));
                changed = true;
            }

            CapitalHouseTier desiredTier =
                    desiredTier(capital, candidateId);

            if (desiredTier == CapitalHouseTier.ROYAL) {
                changed |= makeRoyal(
                        level,
                        house,
                        candidateId
                );
            } else if (desiredTier.ordinal() > house.getTier().ordinal()) {
                house.setTier(desiredTier);
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.MEMBER_ELEVATED,
                        level.getGameTime(),
                        candidateId,
                        null
                ));
                changed = true;
            }

            if (desiredTier == CapitalHouseTier.GREAT
                    && house.getHeadId() == null) {
                house.setHeadId(candidateId);
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.HEAD_CHANGED,
                        level.getGameTime(),
                        candidateId,
                        null
                ));
                changed = true;
            } else if (desiredTier == CapitalHouseTier.NOBLE
                    && house.getHeadId() == null
                    && capital.isLord(candidateId)) {
                house.setHeadId(candidateId);
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.HEAD_CHANGED,
                        level.getGameTime(),
                        candidateId,
                        null
                ));
                changed = true;
            }

            if (capital.isDisinheritedRoyalChild(candidateId)
                    && !house.hasHistory(
                    CapitalHouseHistoryType.MEMBER_DISINHERITED,
                    candidateId
            )) {
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.MEMBER_DISINHERITED,
                        level.getGameTime(),
                        candidateId,
                        null
                ));
                changed = true;
            }
        }

        changed |= normalizeRoyalHouse(level, capital);

        if (changed) {
            CapitalHouseDataAccess.markDirty(level);
        }

        return changed;
    }

    public static boolean recordDeath(
            ServerLevel level,
            UUID memberId
    ) {
        if (level == null || memberId == null) {
            return false;
        }

        boolean changed = false;

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null || capital.getCapitalId() == null) {
                continue;
            }

            CapitalHouseRecord house =
                    CapitalHouseDataAccess.findHouseForMember(
                            level,
                            capital.getCapitalId(),
                            memberId
                    );

            if (house == null) {
                continue;
            }

            if (house.removeCurrentMember(memberId)) {
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.MEMBER_LEFT,
                        level.getGameTime(),
                        memberId,
                        null
                ));
                changed = true;
            }

            if (Objects.equals(house.getHeadId(), memberId)) {
                house.setHeadId(null);
                changed = true;
            }

            if (house.getCurrentMembers().isEmpty()
                    && house.isActive()) {
                house.setActive(false);
                house.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.BECAME_EXTINCT,
                        level.getGameTime(),
                        memberId,
                        null
                ));
                changed = true;
            }
        }

        if (changed) {
            CapitalHouseDataAccess.markDirty(level);
        }

        return changed;
    }

    public static boolean recordDisinheritance(
            ServerLevel level,
            CapitalRecord capital,
            UUID memberId
    ) {
        if (level == null || capital == null || memberId == null) {
            return false;
        }

        synchronize(level, capital, null);

        CapitalHouseRecord house =
                CapitalHouseDataAccess.findHouseForMember(
                        level,
                        capital.getCapitalId(),
                        memberId
                );

        if (house == null) {
            ResolvedHouse resolved =
                    resolveHouse(level, capital, memberId);

            if (resolved == null) {
                return false;
            }

            house = createResolvedHouse(
                    level,
                    capital,
                    resolved
            );

            if (house == null) {
                return false;
            }

            house.addCurrentMember(memberId);
        }

        if (house.hasHistory(
                CapitalHouseHistoryType.MEMBER_DISINHERITED,
                memberId
        )) {
            return false;
        }

        house.addHistory(new CapitalHouseHistoryEntry(
                CapitalHouseHistoryType.MEMBER_DISINHERITED,
                level.getGameTime(),
                memberId,
                null
        ));

        CapitalHouseDataAccess.markDirty(level);
        return true;
    }

    public static boolean snapshotRegimeBeforeSuccession(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null || capital == null) {
            return false;
        }

        return synchronize(level, capital, null);
    }

    private static boolean transitionBetweenHouses(
            ServerLevel level,
            CapitalRecord capital,
            UUID memberId,
            CapitalHouseRecord previousHouse,
            CapitalHouseRecord newHouse,
            ResolvedHouse resolved
    ) {
        boolean changed = false;

        if (previousHouse.removeCurrentMember(memberId)) {
            previousHouse.addHistory(new CapitalHouseHistoryEntry(
                    CapitalHouseHistoryType.MEMBER_LEFT,
                    level.getGameTime(),
                    memberId,
                    newHouse.getHouseId()
            ));
            changed = true;
        }

        if (Objects.equals(previousHouse.getHeadId(), memberId)) {
            previousHouse.setHeadId(null);
            changed = true;
        }

        boolean foundedOwnNewHouse =
                Objects.equals(resolved.founderId(), memberId);

        if (foundedOwnNewHouse
                && newHouse.getParentHouseId() == null
                && !previousHouse.getHouseId().equals(newHouse.getHouseId())) {
            newHouse.setParentHouseId(previousHouse.getHouseId());

            if (!newHouse.hasHistory(
                    CapitalHouseHistoryType.BRANCH_FOUNDED,
                    memberId,
                    previousHouse.getHouseId()
            )) {
                newHouse.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.BRANCH_FOUNDED,
                        level.getGameTime(),
                        memberId,
                        previousHouse.getHouseId()
                ));
            }

            if (!previousHouse.hasHistory(
                    CapitalHouseHistoryType.BRANCH_FOUNDED,
                    memberId,
                    newHouse.getHouseId()
            )) {
                previousHouse.addHistory(new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.BRANCH_FOUNDED,
                        level.getGameTime(),
                        memberId,
                        newHouse.getHouseId()
                ));
            }

            changed = true;
        }

        if (previousHouse.getCurrentMembers().isEmpty()
                && previousHouse.isActive()) {
            previousHouse.setActive(false);
            previousHouse.addHistory(new CapitalHouseHistoryEntry(
                    CapitalHouseHistoryType.BECAME_EXTINCT,
                    level.getGameTime(),
                    memberId,
                    newHouse.getHouseId()
            ));
            changed = true;
        }

        return changed;
    }

    private static CapitalHouseRecord findResolvedHouse(
            ServerLevel level,
            CapitalRecord capital,
            ResolvedHouse resolved
    ) {
        CapitalHouseRecord house =
                CapitalHouseDataAccess.getHouse(
                        level,
                        capital.getCapitalId(),
                        resolved.houseId()
                );

        if (house == null) {
            house = CapitalHouseDataAccess.findHouseByName(
                    level,
                    capital.getCapitalId(),
                    resolved.houseName()
            );
        }

        return house;
    }

    private static CapitalHouseRecord createResolvedHouse(
            ServerLevel level,
            CapitalRecord capital,
            ResolvedHouse resolved
    ) {
        CapitalHouseRecord house =
                CapitalHouseDataAccess.createHouse(
                        level,
                        capital.getCapitalId(),
                        resolved.houseId(),
                        resolved.houseName()
                );

        if (house == null) {
            return null;
        }

        house.setFounderId(resolved.founderId());
        house.setFoundedGameTime(resolved.foundedGameTime());

        if (!house.hasHistory(
                CapitalHouseHistoryType.FOUNDED,
                resolved.founderId()
        )) {
            house.addHistory(new CapitalHouseHistoryEntry(
                    CapitalHouseHistoryType.FOUNDED,
                    resolved.foundedGameTime() > 0L
                            ? resolved.foundedGameTime()
                            : level.getGameTime(),
                    resolved.founderId(),
                    null
            ));
        }

        return house;
    }

    private static boolean makeRoyal(
            ServerLevel level,
            CapitalHouseRecord house,
            UUID sovereignId
    ) {
        boolean changed = false;

        if (house.getTier() != CapitalHouseTier.ROYAL) {
            house.setTier(CapitalHouseTier.ROYAL);
            house.addHistory(new CapitalHouseHistoryEntry(
                    CapitalHouseHistoryType.BECAME_ROYAL,
                    level.getGameTime(),
                    sovereignId,
                    null
            ));
            changed = true;
        }

        if (!Objects.equals(house.getHeadId(), sovereignId)) {
            house.setHeadId(sovereignId);
            house.addHistory(new CapitalHouseHistoryEntry(
                    CapitalHouseHistoryType.HEAD_CHANGED,
                    level.getGameTime(),
                    sovereignId,
                    null
            ));
            changed = true;
        }

        return changed;
    }

    private static boolean normalizeRoyalHouse(
            ServerLevel level,
            CapitalRecord capital
    ) {
        UUID sovereignId = effectiveSovereignId(capital);

        CapitalHouseRecord currentRoyal =
                sovereignId == null
                        ? null
                        : CapitalHouseDataAccess.findHouseForMember(
                                level,
                                capital.getCapitalId(),
                                sovereignId
                        );

        boolean changed = false;

        Collection<CapitalHouseRecord> houses =
                CapitalHouseDataAccess.getHouses(
                        level,
                        capital.getCapitalId()
                );

        for (CapitalHouseRecord house : houses) {
            if (house == null
                    || house.getTier() != CapitalHouseTier.ROYAL
                    || house == currentRoyal) {
                continue;
            }

            house.setTier(
                    hasCurrentDuke(capital, house)
                            ? CapitalHouseTier.GREAT
                            : CapitalHouseTier.NOBLE
            );

            house.addHistory(new CapitalHouseHistoryEntry(
                    CapitalHouseHistoryType.CEASED_ROYAL,
                    level.getGameTime(),
                    sovereignId,
                    currentRoyal == null
                            ? null
                            : currentRoyal.getHouseId()
            ));

            changed = true;
        }

        return changed;
    }

    private static boolean hasCurrentDuke(
            CapitalRecord capital,
            CapitalHouseRecord house
    ) {
        for (UUID memberId : house.getCurrentMembers()) {
            if (capital.isDuke(memberId)) {
                return true;
            }
        }

        return false;
    }

    private static CapitalHouseTier desiredTier(
            CapitalRecord capital,
            UUID memberId
    ) {
        UUID sovereignId = effectiveSovereignId(capital);

        if (memberId.equals(sovereignId)) {
            return CapitalHouseTier.ROYAL;
        }

        if (capital.isDuke(memberId)) {
            return CapitalHouseTier.GREAT;
        }

        return CapitalHouseTier.NOBLE;
    }

    private static UUID effectiveSovereignId(
            CapitalRecord capital
    ) {
        if (capital == null) {
            return null;
        }

        if (capital.isPlayerSovereign()
                && capital.getPlayerSovereignId() != null) {
            return capital.getPlayerSovereignId();
        }

        return capital.getSovereign();
    }

    private static LinkedHashSet<UUID> collectCandidates(
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        LinkedHashSet<UUID> candidates =
                new LinkedHashSet<>();

        add(candidates, effectiveSovereignId(capital));
        add(candidates, capital.getConsort());
        add(candidates, capital.getDowager());
        add(candidates, capital.getHeir());

        candidates.addAll(capital.getRoyalChildren());
        candidates.addAll(capital.getDisinheritedRoyalChildren());
        candidates.addAll(capital.getLegitimizedRoyalChildren());
        candidates.addAll(capital.getRoyalHousehold());
        candidates.addAll(capital.getDukes());
        candidates.addAll(capital.getLords());

        if (residents != null) {
            candidates.addAll(residents);
        }

        return candidates;
    }

    private static void add(
            Set<UUID> values,
            UUID value
    ) {
        if (value != null) {
            values.add(value);
        }
    }

    private static ResolvedHouse resolveHouse(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (level == null
                || capital == null
                || entityId == null) {
            return null;
        }

        Entity villager =
                MCAIntegrationBridge.findLoadedMCAVillagerByUuid(
                        level,
                        entityId
                );

        if (villager != null) {
            VillagerIdentityService.ensureAssigned(
                    level,
                    villager,
                    capital
            );

            VillagerIdentityData identity =
                    VillagerIdentityService.getIdentity(
                            villager
                    );

            if (identity != null
                    && identity.hasFoundedHouse()) {
                UUID houseId = stableHouseId(
                        capital.getCapitalId(),
                        identity.houseFounderId(),
                        identity.houseName()
                );

                return new ResolvedHouse(
                        houseId,
                        identity.houseName(),
                        identity.houseFounderId(),
                        identity.houseFoundedAtGameTime()
                );
            }
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(entityId);

        if (player != null) {
            PlayerHouseRecord playerHouse =
                    PlayerHouseService.get(
                            level,
                            entityId
                    );

            if (playerHouse != null
                    && playerHouse.hasHouseName()) {
                return new ResolvedHouse(
                        stableHouseId(
                                capital.getCapitalId(),
                                entityId,
                                playerHouse.getHouseName()
                        ),
                        playerHouse.getHouseName(),
                        entityId,
                        playerHouse.getHouseNameSetAtGameTime()
                );
            }
        }

        return null;
    }

    private static UUID stableHouseId(
            UUID capitalId,
            UUID founderId,
            String houseName
    ) {
        if (founderId != null) {
            return UUID.nameUUIDFromBytes(
                    ("mcacapitals:house:"
                            + capitalId
                            + ":founder:"
                            + founderId)
                            .getBytes(StandardCharsets.UTF_8)
            );
        }

        return UUID.nameUUIDFromBytes(
                ("mcacapitals:house:"
                        + capitalId
                        + ":name:"
                        + (houseName == null
                        ? ""
                        : houseName.trim()
                                .toLowerCase(Locale.ROOT)))
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private record ResolvedHouse(
            UUID houseId,
            String houseName,
            UUID founderId,
            long foundedGameTime
    ) {
    }
}
