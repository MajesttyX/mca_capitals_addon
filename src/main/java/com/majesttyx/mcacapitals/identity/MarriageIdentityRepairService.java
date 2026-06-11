package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class MarriageIdentityRepairService {

    private MarriageIdentityRepairService() {
    }

    public static boolean repairPlayerVillagerMarriage(ServerLevel level, ServerPlayer player, Entity spouse, CapitalRecord capital) {
        if (level == null || player == null || spouse == null || capital == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(spouse)) {
            return false;
        }

        boolean changed = false;

        changed |= applyPlayerHouseToSpouse(level, player, spouse);
        changed |= applyPlayerTitleToVillagerSpouse(level, player, spouse, capital);

        if (changed) {
            refreshAfterMarriageRepair(level, capital, spouse);
        }

        return changed;
    }

    public static boolean repairVillagerVillagerMarriage(ServerLevel level, Entity first, Entity second) {
        if (level == null || first == null || second == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(first) || !MCAIntegrationBridge.isMCAVillagerEntity(second)) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, first);
        VillagerIdentityService.ensureAssigned(level, second);

        MarriageHousehold household = resolveMarriageHousehold(level, first, second);
        if (household == null || household.source() == null || household.target() == null) {
            return false;
        }

        VillagerIdentityData sourceIdentity = VillagerIdentityService.getIdentity(household.source());
        if (sourceIdentity == null) {
            return false;
        }

        String sourceSurname = VillagerIdentityService.getCurrentSurname(household.source());
        if (sourceSurname == null || sourceSurname.isBlank()) {
            return false;
        }

        boolean changed = false;

        changed |= VillagerIdentityService.assignCurrentSurname(
                level,
                household.target(),
                sourceSurname,
                SurnameSource.MARRIAGE
        );

        if (sourceIdentity.hasFoundedHouse()) {
            changed |= VillagerIdentityService.foundHouse(
                    level,
                    household.target(),
                    sourceIdentity.houseName(),
                    sourceIdentity.houseWords(),
                    sourceIdentity.houseWordsPersonality(),
                    sourceIdentity.houseFounderId(),
                    sourceIdentity.houseFounderName(),
                    sourceIdentity.houseFoundedInCapitalId(),
                    sourceIdentity.houseFoundedInCapitalName()
            );
        }

        if (changed) {
            CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, household.source().getUUID());
            if (capital == null) {
                capital = CapitalTitleResolver.findCapitalForEntity(level, household.target().getUUID());
            }

            if (capital != null) {
                refreshAfterMarriageRepair(level, capital, household.source());
                refreshAfterMarriageRepair(level, capital, household.target());
            } else {
                VillagerIdentitySyncService.syncToNearbyPlayers(level, household.source());
                VillagerIdentitySyncService.syncToNearbyPlayers(level, household.target());
            }
        }

        return changed;
    }

    private static boolean applyPlayerHouseToSpouse(ServerLevel level, ServerPlayer player, Entity spouse) {
        PlayerHouseRecord record = PlayerHouseService.get(level, player.getUUID());
        if (record == null || !record.hasHouseName()) {
            return false;
        }

        return PlayerHouseIdentityService.applyPlayerHouseIdentityToVillager(
                level,
                spouse,
                player.getUUID(),
                record,
                SurnameSource.MARRIAGE,
                false
        );
    }

    private static boolean applyPlayerTitleToVillagerSpouse(ServerLevel level, ServerPlayer player, Entity spouse, CapitalRecord capital) {
        UUID playerId = player.getUUID();
        UUID spouseId = spouse.getUUID();

        if (playerId.equals(capital.getPlayerSovereignId())) {
            boolean changed = false;

            if (!spouseId.equals(capital.getConsort())) {
                capital.setConsort(spouseId);
                changed = true;
            }

            boolean spouseFemale = MCAIntegrationBridge.isFemale(level, spouseId);
            if (capital.isConsortFemale() != spouseFemale) {
                capital.setConsortFemale(spouseFemale);
                changed = true;
            }

            return changed;
        }

        NobleTitle playerTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, playerId);
        if (playerTitle == null || playerTitle == NobleTitle.COMMONER) {
            return false;
        }

        int playerRank = rankValue(playerTitle.name());
        int spouseRank = getVillagerRank(level, spouseId);

        if (spouseRank <= playerRank) {
            return false;
        }

        boolean spouseFemale = MCAIntegrationBridge.isFemale(level, spouseId);

        if (playerTitle == NobleTitle.DUKE || playerTitle == NobleTitle.DUCHESS) {
            if (!capital.isDuke(spouseId) || capital.isDukeFemale(spouseId) != spouseFemale) {
                capital.addDuke(spouseId, spouseFemale);
                return true;
            }
            return false;
        }

        if (playerTitle == NobleTitle.LORD || playerTitle == NobleTitle.LADY) {
            if (!capital.isLord(spouseId) || capital.isLordFemale(spouseId) != spouseFemale) {
                capital.addLord(spouseId, spouseFemale);
                return true;
            }
            return false;
        }

        return false;
    }

    private static MarriageHousehold resolveMarriageHousehold(ServerLevel level, Entity first, Entity second) {
        int firstRank = getVillagerRank(level, first.getUUID());
        int secondRank = getVillagerRank(level, second.getUUID());

        if (firstRank < secondRank) {
            return new MarriageHousehold(first, second);
        }

        if (secondRank < firstRank) {
            return new MarriageHousehold(second, first);
        }

        VillagerIdentityData firstIdentity = VillagerIdentityService.getIdentity(first);
        VillagerIdentityData secondIdentity = VillagerIdentityService.getIdentity(second);

        boolean firstHouse = firstIdentity != null && firstIdentity.hasFoundedHouse();
        boolean secondHouse = secondIdentity != null && secondIdentity.hasFoundedHouse();

        if (firstHouse && !secondHouse) {
            return new MarriageHousehold(first, second);
        }

        if (secondHouse && !firstHouse) {
            return new MarriageHousehold(second, first);
        }

        String firstSurname = VillagerIdentityService.getCurrentSurname(first);
        String secondSurname = VillagerIdentityService.getCurrentSurname(second);

        if (firstSurname != null && !firstSurname.isBlank() && (secondSurname == null || secondSurname.isBlank())) {
            return new MarriageHousehold(first, second);
        }

        if (secondSurname != null && !secondSurname.isBlank() && (firstSurname == null || firstSurname.isBlank())) {
            return new MarriageHousehold(second, first);
        }

        return null;
    }

    private static void refreshAfterMarriageRepair(ServerLevel level, CapitalRecord capital, Entity changedEntity) {
        if (level == null || capital == null) {
            return;
        }

        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        CapitalFoundationService.refreshCourt(level, capital);
        CapitalNameService.refreshCapitalNames(level, capital, residents);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        if (changedEntity != null) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, changedEntity);
        }
    }

    private static int getVillagerRank(ServerLevel level, UUID villagerId) {
        String title = CapitalTitleResolver.getDisplayTitleForEntity(level, villagerId);
        return rankValue(title);
    }

    private static int rankValue(String title) {
        if (title == null || title.isBlank()) {
            return 900;
        }

        String normalized = title.trim().toLowerCase(Locale.ROOT).replace('_', ' ');

        return switch (normalized) {
            case "high queen", "high king" -> 20;
            case "queen", "king" -> 30;
            case "queen consort", "king consort" -> 40;
            case "dowager queen", "dowager king" -> 50;
            case "heir apparent" -> 60;
            case "crown princess", "crown prince" -> 70;
            case "princess", "prince" -> 80;
            case "princess consort", "prince consort" -> 90;
            case "dowager princess", "dowager prince" -> 100;
            case "hand of the queen", "hand of the king" -> 110;
            case "grand maester" -> 115;
            case "duchess", "duke" -> 120;
            case "dowager duchess", "dowager duke" -> 130;
            case "maester" -> 140;
            case "lord commander" -> 150;
            case "lady", "lord" -> 170;
            case "dame", "sir" -> 180;
            default -> 900;
        };
    }

    private record MarriageHousehold(Entity source, Entity target) {
    }
}