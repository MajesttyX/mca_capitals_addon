package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenDecreeOfTheHousePacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class DecreeOfTheHouseService {

    private static final double CAPITAL_FALLBACK_RADIUS = 128.0D;

    private DecreeOfTheHouseService() {
    }

    public static OpenDecreeOfTheHousePacket createOpenPacket(ServerLevel level, Entity target) {
        if (level == null || target == null || !MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            return null;
        }

        VillagerIdentityService.ensureAssigned(level, target);
        HouseRevisionService.applyLatestRevision(level, target);
        VillagerIdentityData identity = VillagerIdentityService.getIdentity(target);

        return new OpenDecreeOfTheHousePacket(
                target.getUUID(),
                false,
                getDisplayFirstNameOnly(level, target),
                identity.currentSurname(),
                identity.hasFoundedHouse(),
                identity.houseName(),
                identity.houseWords()
        );
    }

    public static void openPlayerHouseEditor(ServerPlayer player) {
        if (player == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        PlayerHouseRecord record = PlayerHouseService.get(level, player.getUUID());

        ModNetwork.sendToPlayer(player, new OpenDecreeOfTheHousePacket(
                player.getUUID(),
                true,
                player.getName().getString(),
                record == null ? "" : record.getHouseName(),
                true,
                record == null ? "" : record.getHouseName(),
                record == null ? "" : record.getHouseWords()
        ));
    }

    public static boolean applyFromPacket(
            ServerPlayer player,
            UUID targetId,
            boolean playerTarget,
            String ignoredFirstName,
            String currentSurname,
            String houseWords
    ) {
        if (player == null || targetId == null) {
            return false;
        }

        if (playerTarget) {
            return applyPlayerHouseRevision(player, targetId, currentSurname, houseWords);
        }

        ServerLevel level = player.serverLevel();
        Entity target = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, targetId);
        if (target == null || !MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.that_villager_is_no_longer_available"));
            return false;
        }

        ItemStack decree = findHeldDecree(player);
        if (decree.isEmpty()) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.you_must_be_holding_a_decree_of_the_house_to_complete_this_revision"));
            return false;
        }

        currentSurname = normalizeNamePart(currentSurname);
        houseWords = normalizeHouseWords(houseWords);

        if (!isValidNamePart(currentSurname, 2, 40)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.surname_must_be_2_40_characters_using_letters_spaces_hyphens_or_apostr"));
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, target);
        HouseRevisionService.applyLatestRevision(level, target);

        VillagerIdentityData before = VillagerIdentityService.getIdentity(target);
        boolean establishedHouse = before.hasFoundedHouse();

        if (establishedHouse && !isValidHouseWords(houseWords)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.house_words_must_be_2_80_characters_and_cannot_contain_formatting_code"));
            return false;
        }

        if (establishedHouse) {
            boolean surnameChanged = isHouseNameBeingChanged(before, currentSurname);

            if (surnameChanged) {
                applySelectedHouseRevisionOnly(level, target, before, currentSurname, houseWords);
            } else {
                applySharedHouseWordsRevision(level, target, before, houseWords);
            }
        } else {
            VillagerIdentityService.assignCurrentSurname(level, target, currentSurname, SurnameSource.LEGAL_RENAME);
            VillagerIdentitySyncService.syncToNearbyPlayers(level, target);
        }

        damageDecree(decree);

        VillagerIdentityData after = VillagerIdentityService.getIdentity(target);
        player.sendSystemMessage(
                after.hasFoundedHouse()
                        ? Component.translatable(
                                "mcacapitals.system.decree_of_the_house_service.house_records_revised_with_words",
                                after.currentSurname(),
                                after.houseWords()
                        )
                        : Component.translatable(
                                "mcacapitals.system.decree_of_the_house_service.house_records_revised",
                                after.currentSurname()
                        )
        );

        return true;
    }

    public static ItemStack createFreshDecree() {
        return new ItemStack(ModItems.DECREE_OF_THE_HOUSE.get());
    }

    private static boolean applyPlayerHouseRevision(ServerPlayer player, UUID targetId, String houseName, String houseWords) {
        if (player == null || targetId == null || !targetId.equals(player.getUUID())) {
            return false;
        }

        ItemStack decree = findHeldDecree(player);
        if (decree.isEmpty()) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.you_must_be_holding_a_decree_of_the_house_to_revise_your_house"));
            return false;
        }

        ServerLevel level = player.serverLevel();
        houseName = PlayerHouseService.normalizeHouseName(houseName);
        houseWords = PlayerHouseService.normalizeHouseWords(houseWords);

        if (!PlayerHouseService.isValidHouseName(houseName)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.house_name_must_be_2_20_characters_using_letters_spaces_hyphens_or_apo"));
            return false;
        }

        if (!houseWords.isBlank() && !PlayerHouseService.isValidHouseWords(houseWords)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.house_words_must_be_2_80_characters_and_cannot_contain_formatting_code"));
            return false;
        }

        if (!PlayerHouseService.reviseHouse(level, player, houseName, houseWords)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.the_house_records_could_not_be_revised"));
            return false;
        }

        damageDecree(decree);

        player.sendSystemMessage(
                houseWords.isBlank()
                        ? Component.translatable(
                                "mcacapitals.system.decree_of_the_house_service.player_house_records_revised",
                                houseName
                        )
                        : Component.translatable(
                                "mcacapitals.system.decree_of_the_house_service.player_house_records_revised_with_words",
                                houseName,
                                houseWords
                        )
        );

        return true;
    }

    private static boolean isHouseNameBeingChanged(VillagerIdentityData before, String newSurname) {
        if (before == null) {
            return true;
        }

        String oldCurrentSurname = normalizeNullable(before.currentSurname());
        String oldHouseName = normalizeNullable(before.houseName());
        String normalizedNew = normalizeNullable(newSurname);

        if (normalizedNew.isBlank()) {
            return true;
        }

        if (!oldHouseName.isBlank()) {
            return !oldHouseName.equals(normalizedNew);
        }

        return !oldCurrentSurname.equals(normalizedNew);
    }

    private static void applySelectedHouseRevisionOnly(
            ServerLevel level,
            Entity target,
            VillagerIdentityData before,
            String newHouseName,
            String newHouseWords
    ) {
        VillagerIdentityService.assignCurrentSurname(level, target, newHouseName, SurnameSource.LEGAL_RENAME);
        VillagerIdentityService.clearHouse(target);

        VillagerIdentityService.foundHouse(
                level,
                target,
                newHouseName,
                newHouseWords,
                before.houseWordsPersonality(),
                before.houseFounderId(),
                before.houseFounderName(),
                before.houseFoundedInCapitalId(),
                before.houseFoundedInCapitalName()
        );

        HouseRevisionService.recordRevision(
                level,
                VillagerIdentityService.getIdentity(target),
                newHouseWords
        );
        VillagerIdentitySyncService.syncToNearbyPlayers(level, target);
    }

    private static void applySharedHouseWordsRevision(
            ServerLevel level,
            Entity target,
            VillagerIdentityData targetBefore,
            String newHouseWords
    ) {
        String associatedHouseName = targetBefore.houseName();
        String associatedSurname = targetBefore.currentSurname();

        /*
         * Persist the House-level revision first. Loaded relatives are updated
         * immediately below; unloaded members receive the same revision when
         * they next become a scanned resident or open the Decree editor.
         */
        HouseRevisionService.recordRevision(level, targetBefore, newHouseWords);

        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, target.getUUID());
        Set<Entity> affected = collectLoadedAssociatedHouseMembers(
                level,
                capital,
                target,
                associatedHouseName,
                associatedSurname
        );

        for (Entity member : affected) {
            if (member == null || !MCAIntegrationBridge.isMCAVillagerEntity(member)) {
                continue;
            }

            VillagerIdentityService.ensureAssigned(level, member);
            VillagerIdentityData memberIdentity = VillagerIdentityService.getIdentity(member);

            if (!memberIdentity.hasFoundedHouse()) {
                continue;
            }

            if (!isAssociatedHouseMember(memberIdentity, associatedHouseName, associatedSurname)) {
                continue;
            }

            String houseName = memberIdentity.houseName();
            if (houseName == null || houseName.isBlank()) {
                houseName = associatedHouseName;
            }

            HouseRevisionService.recordRevision(level, memberIdentity, newHouseWords);
            VillagerIdentityService.clearHouse(member);

            VillagerIdentityService.foundHouse(
                    level,
                    member,
                    houseName,
                    newHouseWords,
                    memberIdentity.houseWordsPersonality(),
                    memberIdentity.houseFounderId(),
                    memberIdentity.houseFounderName(),
                    memberIdentity.houseFoundedInCapitalId(),
                    memberIdentity.houseFoundedInCapitalName()
            );

            VillagerIdentitySyncService.syncToNearbyPlayers(level, member);
        }
    }

    private static Set<Entity> collectLoadedAssociatedHouseMembers(
            ServerLevel level,
            CapitalRecord capital,
            Entity target,
            String associatedHouseName,
            String associatedSurname
    ) {
        Set<Entity> affected = new LinkedHashSet<>();
        affected.add(target);

        collectCapitalMatchingMembers(level, capital, associatedHouseName, associatedSurname, affected);
        collectNearbyMatchingMembers(level, target, associatedHouseName, associatedSurname, affected);

        return affected;
    }

    private static void collectCapitalMatchingMembers(
            ServerLevel level,
            CapitalRecord capital,
            String associatedHouseName,
            String associatedSurname,
            Set<Entity> affected
    ) {
        if (level == null || capital == null || capital.getCapitalId() == null) {
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        for (UUID residentId : residents) {
            addIfAssociatedHouseMember(level, residentId, associatedHouseName, associatedSurname, affected);
        }
    }

    private static void collectNearbyMatchingMembers(
            ServerLevel level,
            Entity target,
            String associatedHouseName,
            String associatedSurname,
            Set<Entity> affected
    ) {
        if (level == null || target == null) {
            return;
        }

        AABB searchBox = target.getBoundingBox().inflate(CAPITAL_FALLBACK_RADIUS);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox, MCAIntegrationBridge::isMCAVillagerEntity)) {
            addIfAssociatedHouseMember(level, entity.getUUID(), associatedHouseName, associatedSurname, affected);
        }
    }

    private static void addIfAssociatedHouseMember(
            ServerLevel level,
            UUID entityId,
            String associatedHouseName,
            String associatedSurname,
            Set<Entity> affected
    ) {
        if (level == null || entityId == null) {
            return;
        }

        Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, entityId);
        if (entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        VillagerIdentityService.ensureAssigned(level, entity);
        VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);

        if (isAssociatedHouseMember(identity, associatedHouseName, associatedSurname)) {
            affected.add(entity);
        }
    }

    private static boolean isAssociatedHouseMember(
            VillagerIdentityData identity,
            String associatedHouseName,
            String associatedSurname
    ) {
        if (identity == null || !identity.hasFoundedHouse()) {
            return false;
        }

        String normalizedHouseName = normalizeNullable(identity.houseName());
        String normalizedCurrentSurname = normalizeNullable(identity.currentSurname());
        String normalizedAssociatedHouseName = normalizeNullable(associatedHouseName);
        String normalizedAssociatedSurname = normalizeNullable(associatedSurname);

        return (!normalizedAssociatedHouseName.isBlank() && normalizedHouseName.equals(normalizedAssociatedHouseName))
                || (!normalizedAssociatedSurname.isBlank() && normalizedCurrentSurname.equals(normalizedAssociatedSurname));
    }

    private static String getDisplayFirstNameOnly(ServerLevel level, Entity target) {
        if (level == null || target == null) {
            return "";
        }

        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, target.getUUID());
        return CapitalNameService.resolveDisplayName(level, capital, target.getUUID());
    }

    private static ItemStack findHeldDecree(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.DECREE_OF_THE_HOUSE.get())) {
            return main;
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(ModItems.DECREE_OF_THE_HOUSE.get())) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    private static void damageDecree(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        int nextDamage = stack.getDamageValue() + 1;
        if (nextDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
            return;
        }

        stack.setDamageValue(nextDamage);
    }

    private static String normalizeNamePart(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeHouseWords(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private static boolean isValidNamePart(String value, int min, int max) {
        if (value == null) {
            return false;
        }

        String normalized = normalizeNamePart(value);
        if (normalized.length() < min || normalized.length() > max) {
            return false;
        }

        if (normalized.contains("§")) {
            return false;
        }

        return normalized.matches("[A-Za-z][A-Za-z '\\-]*");
    }

    private static boolean isValidHouseWords(String value) {
        if (value == null) {
            return false;
        }

        String normalized = normalizeHouseWords(value);
        if (normalized.length() < 2 || normalized.length() > 80) {
            return false;
        }

        if (normalized.contains("§")) {
            return false;
        }

        return normalized.matches("[A-Za-z][A-Za-z '\\-,]*");
    }
}
