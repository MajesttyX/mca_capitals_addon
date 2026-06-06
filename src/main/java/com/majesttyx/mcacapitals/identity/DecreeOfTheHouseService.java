package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.network.OpenDecreeOfTheHousePacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.Locale;
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
        VillagerIdentityData identity = VillagerIdentityService.getIdentity(target);

        return new OpenDecreeOfTheHousePacket(
                target.getUUID(),
                getDisplayFirstNameOnly(target),
                identity.currentSurname(),
                identity.hasFoundedHouse(),
                identity.houseName(),
                identity.houseWords()
        );
    }

    public static boolean applyFromPacket(
            ServerPlayer player,
            UUID targetId,
            String ignoredFirstName,
            String currentSurname,
            String houseWords
    ) {
        if (player == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        Entity target = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, targetId);
        if (target == null || !MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            player.sendSystemMessage(Component.literal("That villager is no longer available."));
            return false;
        }

        ItemStack decree = findHeldDecree(player);
        if (decree.isEmpty()) {
            player.sendSystemMessage(Component.literal("You must be holding a Decree of the House to complete this revision."));
            return false;
        }

        currentSurname = normalizeNamePart(currentSurname);
        houseWords = normalizeHouseWords(houseWords);

        if (!isValidNamePart(currentSurname, 2, 40)) {
            player.sendSystemMessage(Component.literal("Surname must be 2-40 characters using letters, spaces, hyphens, or apostrophes."));
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, target);

        VillagerIdentityData before = VillagerIdentityService.getIdentity(target);
        boolean establishedHouse = before.hasFoundedHouse();

        if (establishedHouse && !isValidHouseWords(houseWords)) {
            player.sendSystemMessage(Component.literal("House Words must be 2-80 characters and cannot contain formatting codes."));
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
        player.sendSystemMessage(Component.literal(
                "The House records have been revised: "
                        + after.currentSurname()
                        + (after.hasFoundedHouse() ? " — " + after.houseWords() : "")
        ));

        return true;
    }

    public static ItemStack createFreshDecree() {
        return new ItemStack(ModItems.DECREE_OF_THE_HOUSE.get());
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

    private static String getDisplayFirstNameOnly(Entity target) {
        if (target == null || target.getName() == null) {
            return "";
        }

        return stripFormattingAndKnownTitlePrefixes(target.getName().getString());
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

    private static String stripFormattingAndKnownTitlePrefixes(String value) {
        String normalized = normalizeNamePart(value);
        if (normalized.isBlank()) {
            return "";
        }

        String lower = normalized.toLowerCase(Locale.ROOT);

        String[] prefixes = new String[] {
                "high queen ",
                "high king ",
                "dowager queen ",
                "dowager king ",
                "queen consort ",
                "king consort ",
                "crown princess ",
                "crown prince ",
                "princess consort ",
                "prince consort ",
                "dowager princess ",
                "dowager prince ",
                "hand of the queen ",
                "hand of the king ",
                "grand maester ",
                "court herald ",
                "lord commander ",
                "princess ",
                "prince ",
                "duchess ",
                "duke ",
                "lady ",
                "lord ",
                "dame ",
                "sir ",
                "queen ",
                "king "
        };

        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                return normalized.substring(prefix.length()).trim();
            }
        }

        return normalized;
    }
}