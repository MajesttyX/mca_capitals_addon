package com.example.mcacapitals.player;

import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.data.PlayerCapitalTitleSavedData;
import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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
            return NobleTitle.COMMONER;
        }

        UUID currentSpouseId = MCAIntegrationBridge.getSpouse(level, playerId);
        if (currentSpouseId == null || !currentSpouseId.equals(sourceSpouseId)) {
            return NobleTitle.COMMONER;
        }

        NobleTitle marriageTitle = record.getMarriageTitle();

        if ((marriageTitle == NobleTitle.DUKE || marriageTitle == NobleTitle.DUCHESS) && capital.isDuke(sourceSpouseId)) {
            return marriageTitle;
        }

        if ((marriageTitle == NobleTitle.LORD || marriageTitle == NobleTitle.LADY) && capital.isLord(sourceSpouseId)) {
            return marriageTitle;
        }

        return NobleTitle.COMMONER;
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
}