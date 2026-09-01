package com.majesttyx.mcacapitals.house;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalChronicleEventType;
import com.majesttyx.mcacapitals.capital.CapitalChronicleEntry;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.capital.CourtAssignmentService;
import com.majesttyx.mcacapitals.data.CapitalHouseDataAccess;
import com.majesttyx.mcacapitals.identity.SurnameSource;
import com.majesttyx.mcacapitals.identity.VillagerIdentityData;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DeclarationOfSeparationService {

    private DeclarationOfSeparationService() {
    }

    public static boolean foundNewHouse(
            ServerPlayer player,
            UUID targetId,
            String newHouseName,
            String houseWords
    ) {
        if (player == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        Entity target = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, targetId);

        if (target == null || !MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.target_missing")
            );
            return false;
        }

        ItemStack ledger = findHeldLedger(player);
        if (ledger.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.house_ledger.must_hold")
            );
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, target);
        VillagerIdentityData before = VillagerIdentityService.getIdentity(target);

        if (before == null || !before.hasFoundedHouse()) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.no_current_house")
            );
            return false;
        }

        CapitalRecord capital =
                CapitalTitleResolver.findCapitalForEntity(level, targetId);

        if (capital == null || capital.getCapitalId() == null) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.no_capital")
            );
            return false;
        }

        CapitalHouseRecord currentHouse =
                CapitalHouseDataAccess.findHouseForMember(
                        level,
                        capital.getCapitalId(),
                        targetId
                );

        UUID separatedFromHouseId =
                currentHouse == null
                        ? null
                        : currentHouse.getHouseId();

        String separatedFromHouseName =
                currentHouse == null
                        ? ""
                        : currentHouse.getHouseName();

        if (!isEligibleFounder(
                level,
                capital,
                currentHouse,
                targetId
        )) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.not_eligible")
            );
            return false;
        }

        newHouseName = PlayerHouseService.normalizeHouseName(newHouseName);
        houseWords = PlayerHouseService.normalizeHouseWords(houseWords);

        if (!PlayerHouseService.isValidHouseName(newHouseName)) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.invalid_name")
            );
            return false;
        }

        if (!houseWords.isBlank() && !PlayerHouseService.isValidHouseWords(houseWords)) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.invalid_words")
            );
            return false;
        }

        if (newHouseName.equalsIgnoreCase(before.houseName())) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.same_house")
            );
            return false;
        }

        CapitalHouseRecord existing =
                CapitalHouseDataAccess.findHouseByName(
                        level,
                        capital.getCapitalId(),
                        newHouseName
                );

        if (existing != null) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.declaration_of_separation.name_exists")
            );
            return false;
        }

        grantFounderLordshipIfNeeded(
                level,
                capital,
                targetId
        );

        String capitalName = capital.getVillageId() == null
                ? ""
                : MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        VillagerIdentityService.assignCurrentSurname(
                level,
                target,
                newHouseName,
                SurnameSource.LEGAL_RENAME
        );

        VillagerIdentityService.clearHouse(target);

        VillagerIdentityService.foundHouse(
                level,
                target,
                newHouseName,
                houseWords,
                before.houseWordsPersonality(),
                targetId,
                target.getName() == null ? "" : target.getName().getString(),
                capital.getCapitalId(),
                capitalName
        );

        VillagerIdentitySyncService.syncToNearbyPlayers(level, target);

        Set<UUID> residents =
                CapitalResidentScanner.scanResidents(
                        level,
                        capital.getCapitalId()
                );

        CapitalHouseRegistryService.synchronize(
                level,
                capital,
                residents
        );

        recordSeparationDisinheritance(
                level,
                capital,
                separatedFromHouseId,
                targetId
        );

        recordSeparationChronicleEntry(
                level,
                capital,
                target,
                targetId,
                separatedFromHouseId,
                separatedFromHouseName,
                newHouseName
        );


        player.sendSystemMessage(
                Component.translatable("mcacapitals.system.declaration_of_separation.founded", target.getName(),
                        newHouseName
                )
        );

        return true;
    }

    public static boolean isEligibleFounder(
            ServerLevel level,
            CapitalRecord capital,
            CapitalHouseRecord currentHouse,
            UUID targetId
    ) {
        if (level == null
                || capital == null
                || targetId == null) {
            return false;
        }

        if (targetId.equals(capital.getSovereign())
                || targetId.equals(capital.getPlayerSovereignId())
                || targetId.equals(capital.getHeir())
                || capital.isRoyalChild(targetId)
                || capital.isLegitimizedRoyalChild(targetId)
                || capital.isDisinheritedRoyalChild(targetId)
                || capital.isDuke(targetId)
                || capital.isLord(targetId)) {
            return true;
        }

        return isUntitledNobleBloodMember(
                level,
                currentHouse,
                targetId
        );
    }

    private static boolean isUntitledNobleBloodMember(
            ServerLevel level,
            CapitalHouseRecord house,
            UUID targetId
    ) {
        if (level == null
                || house == null
                || targetId == null
                || !house.isCurrentMember(targetId)
                || house.getFounderId() == null) {
            return false;
        }

        CapitalHouseTier tier = house.getTier();

        if (tier != CapitalHouseTier.NOBLE
                && tier != CapitalHouseTier.GREAT
                && tier != CapitalHouseTier.ROYAL) {
            return false;
        }

        return targetId.equals(house.getFounderId())
                || isDescendantOf(
                        level,
                        targetId,
                        house.getFounderId(),
                        new HashSet<>()
                );
    }

    private static boolean isDescendantOf(
            ServerLevel level,
            UUID candidate,
            UUID ancestor,
            Set<UUID> visited
    ) {
        if (candidate == null
                || ancestor == null
                || !visited.add(candidate)) {
            return false;
        }

        for (UUID parent :
                MCAIntegrationBridge.getParents(
                        level,
                        candidate
                )) {
            if (ancestor.equals(parent)) {
                return true;
            }

            if (isDescendantOf(
                    level,
                    parent,
                    ancestor,
                    visited
            )) {
                return true;
            }
        }

        return false;
    }

    private static void grantFounderLordshipIfNeeded(
            ServerLevel level,
            CapitalRecord capital,
            UUID targetId
    ) {
        if (level == null
                || capital == null
                || targetId == null) {
            return;
        }

        if (capital.isDuke(targetId)
                || capital.isLord(targetId)
                || targetId.equals(capital.getSovereign())
                || targetId.equals(capital.getPlayerSovereignId())
                || targetId.equals(capital.getHeir())) {
            return;
        }

        boolean female =
                MCAIntegrationBridge.isFemale(
                        level,
                        targetId
                );

        CourtAssignmentService.assignLord(
                capital,
                targetId,
                female
        );
    }

    private static void recordSeparationDisinheritance(
            ServerLevel level,
            CapitalRecord capital,
            UUID oldHouseId,
            UUID founderId
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || oldHouseId == null
                || founderId == null) {
            return;
        }

        CapitalHouseRecord oldHouse =
                CapitalHouseDataAccess.getHouse(
                        level,
                        capital.getCapitalId(),
                        oldHouseId
                );

        if (oldHouse == null
                || oldHouse.hasHistory(
                        CapitalHouseHistoryType.MEMBER_DISINHERITED,
                        founderId
                )) {
            return;
        }

        oldHouse.addHistory(
                new CapitalHouseHistoryEntry(
                        CapitalHouseHistoryType.MEMBER_DISINHERITED,
                        level.getGameTime(),
                        founderId,
                        null
                )
        );

        CapitalHouseDataAccess.markDirty(level);
    }

    private static void recordSeparationChronicleEntry(
            ServerLevel level,
            CapitalRecord capital,
            Entity founder,
            UUID founderId,
            UUID oldHouseId,
            String oldHouseName,
            String newHouseName
    ) {
        if (level == null
                || capital == null
                || founderId == null
                || oldHouseId == null
                || oldHouseName == null
                || oldHouseName.isBlank()
                || newHouseName == null
                || newHouseName.isBlank()) {
            return;
        }

        long day =
                Math.max(
                        1L,
                        level.getDayTime() / 24000L + 1L
                );

        String founderName =
                founder == null
                        || founder.getName() == null
                        ? founderId.toString()
                        : founder.getName().getString();

        String dedupeKey =
                "house_separation:"
                        + oldHouseId
                        + ":"
                        + founderId
                        + ":"
                        + newHouseName.toLowerCase(
                                java.util.Locale.ROOT
                        );

        CapitalChronicleEntry entry =
                new CapitalChronicleEntry(
                        day,
                        CapitalChronicleEventType.GENERIC_NOTABLE,
                        "mcacapitals.chronicle.event.house_separation",
                        "",
                        dedupeKey,
                        java.util.List.of(
                                CapitalChronicleEntry.Argument.literal(
                                        founderName
                                ),
                                CapitalChronicleEntry.Argument.literal(
                                        oldHouseName
                                ),
                                CapitalChronicleEntry.Argument.literal(
                                        newHouseName
                                )
                        )
                );

        for (String stored :
                capital.getChronicleEntries()) {
            CapitalChronicleEntry existing =
                    CapitalChronicleEntry.decode(
                            stored
                    );

            if (existing != null
                    && dedupeKey.equals(
                    existing.dedupeKey()
            )) {
                return;
            }
        }

        capital.addChronicleEntry(
                entry.encode()
        );
    }

    private static ItemStack findHeldLedger(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.HOUSE_LEDGER.get())) {
            return main;
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(ModItems.HOUSE_LEDGER.get())) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }

}
