package com.example.mcacapitals.data;

import com.example.mcacapitals.capital.CapitalRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class CapitalSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_data";

    static final String KEY_CAPITALS = "Capitals";

    static final String KEY_CAPITAL_ID = "CapitalId";
    static final String KEY_VILLAGE_ID = "VillageId";
    static final String KEY_STATE = "State";

    static final String KEY_SOVEREIGN = "Sovereign";
    static final String KEY_SOVEREIGN_FEMALE = "SovereignFemale";

    static final String KEY_CONSORT = "Consort";
    static final String KEY_CONSORT_FEMALE = "ConsortFemale";

    static final String KEY_DOWAGER = "Dowager";
    static final String KEY_DOWAGER_FEMALE = "DowagerFemale";

    static final String KEY_HEIR = "Heir";
    static final String KEY_HEIR_FEMALE = "HeirFemale";
    static final String KEY_HEIR_MODE = "HeirMode";

    static final String KEY_PLAYER_SOVEREIGN = "PlayerSovereign";
    static final String KEY_PLAYER_SOVEREIGN_ID = "PlayerSovereignId";
    static final String KEY_PLAYER_SOVEREIGN_NAME = "PlayerSovereignName";

    static final String KEY_PLAYER_CONSORT = "PlayerConsort";
    static final String KEY_PLAYER_CONSORT_ID = "PlayerConsortId";
    static final String KEY_PLAYER_CONSORT_NAME = "PlayerConsortName";

    static final String KEY_MONARCHY_REJECTED = "MonarchyRejected";

    static final String KEY_MOURNING_ACTIVE = "MourningActive";
    static final String KEY_MOURNING_END_DAY = "MourningEndDay";
    static final String KEY_MOURNING_ORIGINAL_CLOTHES = "MourningOriginalClothes";

    static final String KEY_ROYAL_CHILDREN = "RoyalChildren";
    static final String KEY_ROYAL_CHILD_FEMALE = "RoyalChildFemale";
    static final String KEY_DISINHERITED_ROYAL_CHILDREN = "DisinheritedRoyalChildren";
    static final String KEY_LEGITIMIZED_ROYAL_CHILDREN = "LegitimizedRoyalChildren";
    static final String KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE = "LegitimizedRoyalChildFemale";
    static final String KEY_ROYAL_SUCCESSION_ORDER = "RoyalSuccessionOrder";

    static final String KEY_DUKES = "Dukes";
    static final String KEY_DUKE_FEMALE = "DukeFemale";

    static final String KEY_LORDS = "Lords";
    static final String KEY_LORD_FEMALE = "LordFemale";

    static final String KEY_KNIGHTS = "Knights";
    static final String KEY_KNIGHT_FEMALE = "KnightFemale";

    static final String KEY_CHRONICLE_ENTRIES = "ChronicleEntries";

    static final String KEY_COMMANDER = "Commander";
    static final String KEY_COMMANDER_FEMALE = "CommanderFemale";
    static final String KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME = "LastCommanderRaidBlessingGameTime";
    static final String KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY = "LastCommanderRandomBlessingDay";

    static final String KEY_ROYAL_GUARDS = "RoyalGuards";
    static final String KEY_ROYAL_GUARD_FEMALE = "RoyalGuardFemale";
    static final String KEY_DISGRACED_ROYAL_GUARDS = "DisgracedRoyalGuards";
    static final String KEY_ROYAL_GUARD_LIEGE = "RoyalGuardLiege";
    static final String KEY_ROYAL_GUARD_PATROLLING = "RoyalGuardPatrolling";
    static final String KEY_ROYAL_GUARD_PATROL_ANCHORS = "RoyalGuardPatrolAnchors";
    static final String KEY_ROYAL_GUARD_DUTY_MODES = "RoyalGuardDutyModes";
    static final String KEY_LAST_ROYAL_GUARD_PROMPT_DAY = "LastRoyalGuardPromptDay";
    static final String KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER = "PendingPlayerGuardSelectionRequester";

    static final String KEY_ENTITY_ID = "EntityId";
    static final String KEY_CLOTHES = "Clothes";

    static final String KEY_GUARD_ID = "GuardId";
    static final String KEY_X = "X";
    static final String KEY_Y = "Y";
    static final String KEY_Z = "Z";
    static final String KEY_MODE = "Mode";

    static final String KEY_ID = "Id";
    static final String KEY_FLAG = "Flag";

    private final List<CapitalRecord> capitals = new ArrayList<>();

    public List<CapitalRecord> getCapitals() {
        return capitals;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        return CapitalSavedDataWriter.saveCapitals(tag, capitals);
    }

    public static CapitalSavedData load(CompoundTag tag) {
        return CapitalSavedDataReader.loadCapitals(tag);
    }
}