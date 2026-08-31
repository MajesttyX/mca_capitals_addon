package com.majesttyx.mcacapitals.identity;

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

import java.util.Locale;
import java.util.UUID;

public final class DecreeOfTheHouseService {

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
                false,
                getDisplayFirstNameOnly(target),
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

        VillagerIdentityData before = VillagerIdentityService.getIdentity(target);
        boolean establishedHouse = before.hasFoundedHouse();

        if (establishedHouse && !isValidHouseWords(houseWords)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.decree_of_the_house_service.house_words_must_be_2_80_characters_and_cannot_contain_formatting_code"));
            return false;
        }

        if (establishedHouse) {
            applyWholeHouseRevision(level, target, before, currentSurname, houseWords);
        } else {
            VillagerIdentityService.assignCurrentSurname(
                    level,
                    target,
                    currentSurname,
                    SurnameSource.LEGAL_RENAME
            );
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

    private static boolean applyPlayerHouseRevision(
            ServerPlayer player,
            UUID targetId,
            String houseName,
            String houseWords
    ) {
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

        HouseRevisionService.recordAndApply(
                level,
                targetId,
                houseName,
                houseWords
        );

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

    private static void applyWholeHouseRevision(
            ServerLevel level,
            Entity target,
            VillagerIdentityData before,
            String newHouseName,
            String newHouseWords
    ) {
        UUID founderId = before == null ? null : before.houseFounderId();

        if (founderId != null) {
            HouseRevisionService.recordAndApply(
                    level,
                    founderId,
                    newHouseName,
                    newHouseWords
            );
            return;
        }

        if (target != null) {
            VillagerIdentityService.reviseHouse(
                    level,
                    target,
                    newHouseName,
                    newHouseWords
            );
            VillagerIdentitySyncService.syncToNearbyPlayers(level, target);
        }
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
