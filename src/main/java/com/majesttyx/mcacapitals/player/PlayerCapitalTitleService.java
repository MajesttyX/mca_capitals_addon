package com.majesttyx.mcacapitals.player;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.PlayerCapitalTitleSavedData;
import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerCapitalTitleService {

    private PlayerCapitalTitleService() {
    }

    public static PlayerCapitalTitleRecord get(ServerLevel level, UUID playerId, UUID capitalId) {
        if (level == null || playerId == null || capitalId == null) {
            return null;
        }
        return PlayerCapitalTitleSavedData.get(level).get(playerId, capitalId);
    }

    public static PlayerCapitalTitleRecord getOrCreate(ServerLevel level, UUID playerId, UUID capitalId) {
        if (level == null || playerId == null || capitalId == null) {
            return null;
        }
        return PlayerCapitalTitleSavedData.get(level).getOrCreate(playerId, capitalId);
    }

    public static void clear(ServerLevel level, UUID playerId, UUID capitalId) {
        if (level == null || playerId == null || capitalId == null) {
            return;
        }
        PlayerCapitalTitleSavedData.get(level).remove(playerId, capitalId);
    }

    public static void grantTitle(ServerLevel level, CapitalRecord capital, UUID playerId, NobleTitle title) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return;
        }

        PlayerCapitalTitleRecord record = getOrCreate(level, playerId, capital.getCapitalId());
        if (record == null) {
            return;
        }

        record.setGrantedTitle(title);
        cachePlayerName(level, record, playerId);
        PlayerCapitalTitleSavedData.get(level).setDirty();
    }

    public static void grantMarriageTitle(ServerLevel level, CapitalRecord capital, UUID playerId, UUID spouseId, NobleTitle title) {
        if (level == null || capital == null || playerId == null || spouseId == null || capital.getCapitalId() == null) {
            return;
        }

        PlayerCapitalTitleRecord record = getOrCreate(level, playerId, capital.getCapitalId());
        if (record == null) {
            return;
        }

        record.clearDowagerTitle();
        record.setMarriageTitle(title);
        record.setMarriageSourceSpouseId(spouseId);
        cachePlayerName(level, record, playerId);
        PlayerCapitalTitleSavedData.get(level).setDirty();
    }

    public static NobleTitle getMarriageTitle(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return NobleTitle.COMMONER;
        }

        PlayerCapitalTitleRecord record = get(level, playerId, capital.getCapitalId());
        if (record == null || !record.hasMarriageTitle()) {
            return NobleTitle.COMMONER;
        }

        UUID sourceSpouseId = record.getMarriageSourceSpouseId();
        if (sourceSpouseId == null) {
            record.clearMarriageTitle();
            cleanupRecordIfEmpty(level, record);
            return NobleTitle.COMMONER;
        }

        UUID currentSpouseId = MCAIntegrationBridge.getSpouse(level, playerId);
        if (currentSpouseId == null || !currentSpouseId.equals(sourceSpouseId)) {
            record.clearMarriageTitle();
            cleanupRecordIfEmpty(level, record);
            return NobleTitle.COMMONER;
        }

        NobleTitle marriageTitle = record.getMarriageTitle();

        if ((marriageTitle == NobleTitle.DUKE || marriageTitle == NobleTitle.DUCHESS) && isValidDukeMarriageSource(capital, sourceSpouseId)) {
            return marriageTitle;
        }

        if ((marriageTitle == NobleTitle.LORD || marriageTitle == NobleTitle.LADY) && capital.isLord(sourceSpouseId)) {
            return marriageTitle;
        }

        if ((marriageTitle == NobleTitle.PRINCE || marriageTitle == NobleTitle.PRINCESS) && isValidPrinceMarriageSource(capital, sourceSpouseId)) {
            return marriageTitle;
        }

        record.clearMarriageTitle();
        cleanupRecordIfEmpty(level, record);
        return NobleTitle.COMMONER;
    }

    public static NobleTitle getDowagerBaseTitle(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return NobleTitle.COMMONER;
        }

        PlayerCapitalTitleRecord record = get(level, playerId, capital.getCapitalId());
        if (record == null || !record.hasDowagerTitle()) {
            return NobleTitle.COMMONER;
        }

        return record.getDowagerBaseTitle();
    }

    public static void transitionMarriageToDowager(ServerLevel level, CapitalRecord capital, UUID playerId, UUID deadSpouseId) {
        if (level == null || capital == null || playerId == null || deadSpouseId == null || capital.getCapitalId() == null) {
            return;
        }

        PlayerCapitalTitleRecord record = get(level, playerId, capital.getCapitalId());
        if (record == null || !record.hasMarriageTitle()) {
            return;
        }

        if (!deadSpouseId.equals(record.getMarriageSourceSpouseId())) {
            return;
        }

        NobleTitle marriageTitle = record.getMarriageTitle();
        if (!isDowagerEligibleMarriageTitle(marriageTitle)) {
            record.clearMarriageTitle();
            cleanupRecordIfEmpty(level, record);
            return;
        }

        record.clearMarriageTitle();
        record.setDowagerBaseTitle(marriageTitle);
        record.setDowagerSourceSpouseId(deadSpouseId);
        cachePlayerName(level, record, playerId);
        PlayerCapitalTitleSavedData.get(level).setDirty();
    }

    public static void clearMarriageTitlesFromDeadSpouse(ServerLevel level, UUID deadSpouseId) {
        if (level == null || deadSpouseId == null) {
            return;
        }

        PlayerCapitalTitleSavedData data = PlayerCapitalTitleSavedData.get(level);
        boolean changed = false;
        List<PlayerCapitalTitleRecord> records = new ArrayList<>(data.getRecords().values());

        for (PlayerCapitalTitleRecord record : records) {
            if (record == null || !deadSpouseId.equals(record.getMarriageSourceSpouseId())) {
                continue;
            }

            record.clearMarriageTitle();
            changed = true;
            if (!record.hasAnyCapitalOffice()) {
                data.remove(record.getPlayerId(), record.getCapitalId());
            }
        }

        if (changed) {
            data.setDirty();
        }
    }

    public static void clearAllMarriageDerivedStateForRemarriage(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null) {
            return;
        }

        PlayerCapitalTitleSavedData data = PlayerCapitalTitleSavedData.get(level);
        boolean changed = false;
        List<PlayerCapitalTitleRecord> records = new ArrayList<>(data.getRecords().values());

        for (PlayerCapitalTitleRecord record : records) {
            if (record == null || !playerId.equals(record.getPlayerId())) {
                continue;
            }

            if (record.hasMarriageTitle()) {
                record.clearMarriageTitle();
                changed = true;
            }

            if (record.hasDowagerTitle()) {
                record.clearDowagerTitle();
                changed = true;
            }

            if (!record.hasAnyCapitalOffice()) {
                data.remove(record.getPlayerId(), record.getCapitalId());
            }
        }

        if (changed) {
            data.setDirty();
        }
    }

    public static void grantCommander(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return;
        }

        PlayerCapitalTitleRecord record = getOrCreate(level, playerId, capital.getCapitalId());
        if (record == null) {
            return;
        }

        record.setCommander(true);
        cachePlayerName(level, record, playerId);
        PlayerCapitalTitleSavedData.get(level).setDirty();
    }

    public static void revokeCommander(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return;
        }

        PlayerCapitalTitleRecord record = get(level, playerId, capital.getCapitalId());
        if (record == null) {
            return;
        }

        record.setCommander(false);

        if (!record.hasAnyCapitalOffice()) {
            clear(level, playerId, capital.getCapitalId());
        } else {
            PlayerCapitalTitleSavedData.get(level).setDirty();
        }
    }

    public static UUID getCommanderHolder(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getCapitalId() == null) {
            return null;
        }

        for (PlayerCapitalTitleRecord record : PlayerCapitalTitleSavedData.get(level).getRecords().values()) {
            if (record == null) {
                continue;
            }
            if (!capital.getCapitalId().equals(record.getCapitalId())) {
                continue;
            }
            if (record.isCommander()) {
                return record.getPlayerId();
            }
        }

        return null;
    }

    public static void revokeCommanderForCapital(ServerLevel level, CapitalRecord capital) {
        UUID holder = getCommanderHolder(level, capital);
        if (holder != null) {
            revokeCommander(level, capital, holder);
        }
    }

    public static NobleTitle getGrantedTitle(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return NobleTitle.COMMONER;
        }

        PlayerCapitalTitleRecord record = get(level, playerId, capital.getCapitalId());
        return record == null ? NobleTitle.COMMONER : record.getGrantedTitle();
    }

    public static boolean isCommander(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return false;
        }

        PlayerCapitalTitleRecord record = get(level, playerId, capital.getCapitalId());
        return record != null && record.isCommander();
    }

    public static boolean hasAnyOffice(ServerLevel level, CapitalRecord capital, UUID playerId) {
        if (level == null || capital == null || playerId == null || capital.getCapitalId() == null) {
            return false;
        }

        PlayerCapitalTitleRecord record = get(level, playerId, capital.getCapitalId());
        return record != null && record.hasAnyCapitalOffice();
    }

    public static void cachePlayerName(ServerLevel level, PlayerCapitalTitleRecord record, UUID playerId) {
        if (level == null || record == null || playerId == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            record.setCachedPlayerName(player.getName().getString());
        }
    }

    private static boolean isValidPrinceMarriageSource(CapitalRecord capital, UUID sourceSpouseId) {
        if (capital == null || sourceSpouseId == null) {
            return false;
        }

        if (sourceSpouseId.equals(capital.getHeir())) {
            return true;
        }

        return capital.isRoyalChild(sourceSpouseId) || capital.isLegitimizedRoyalChild(sourceSpouseId);
    }

    private static boolean isValidDukeMarriageSource(CapitalRecord capital, UUID sourceSpouseId) {
        if (capital == null || sourceSpouseId == null) {
            return false;
        }

        return capital.isDuke(sourceSpouseId);
    }

    private static boolean isDowagerEligibleMarriageTitle(NobleTitle title) {
        return title == NobleTitle.PRINCE
                || title == NobleTitle.PRINCESS
                || title == NobleTitle.DUKE
                || title == NobleTitle.DUCHESS;
    }

    private static void cleanupRecordIfEmpty(ServerLevel level, PlayerCapitalTitleRecord record) {
        if (level == null || record == null) {
            return;
        }

        if (!record.hasAnyCapitalOffice()) {
            PlayerCapitalTitleSavedData.get(level).remove(record.getPlayerId(), record.getCapitalId());
        } else {
            PlayerCapitalTitleSavedData.get(level).setDirty();
        }
    }
}