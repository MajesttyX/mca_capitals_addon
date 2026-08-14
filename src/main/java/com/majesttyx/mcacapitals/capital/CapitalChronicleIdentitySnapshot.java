package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.dialogue.CapitalDialogueGenderResolver;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleRecord;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalChronicleIdentitySnapshot {

    private CapitalChronicleIdentitySnapshot() {
    }


    public static String name(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (level == null || entityId == null) {
            return "Unknown";
        }

        if (capital != null) {
            if (entityId.equals(capital.getPlayerSovereignId())
                    && capital.getPlayerSovereignName() != null
                    && !capital.getPlayerSovereignName().isBlank()) {
                return capital.getPlayerSovereignName().trim();
            }

            if (entityId.equals(capital.getPlayerConsortId())
                    && capital.getPlayerConsortName() != null
                    && !capital.getPlayerConsortName().isBlank()) {
                return capital.getPlayerConsortName().trim();
            }
        }

        if (level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entityId);
            if (player != null) {
                return MCAIntegrationBridge.getPlayerDialogueName(player);
            }
        }

        if (capital != null && capital.getCapitalId() != null) {
            PlayerCapitalTitleRecord record =
                    PlayerCapitalTitleService.get(level, entityId, capital.getCapitalId());
            if (record != null
                    && record.getCachedPlayerName() != null
                    && !record.getCachedPlayerName().isBlank()) {
                return record.getCachedPlayerName().trim();
            }
        }

        return CapitalNameService.resolveDisplayName(level, capital, entityId);
    }

    public static CapitalChronicleEntry.Argument title(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        CapitalTitleOfficeIdentityResolver.TitleIdentity title =
                CapitalTitleOfficeIdentityResolver.resolveTitle(level, capital, entityId);
        CapitalDialogueGenderResolver.ResolvedGender gender =
                CapitalDialogueGenderResolver.resolve(level, capital, entityId);

        return CapitalChronicleEntry.Argument.translatable(
                switch (title) {
                    case HIGH_SOVEREIGN -> gendered("high_sovereign", gender);
                    case SOVEREIGN -> gendered("sovereign", gender);
                    case SOVEREIGN_CONSORT -> gendered("sovereign_consort", gender);
                    case SOVEREIGN_DOWAGER -> gendered("sovereign_dowager", gender);
                    case HEIR_APPARENT -> "mcacapitals.dynamic.title.heir_apparent";
                    case CROWN_HEIR -> gendered("crown_heir", gender);
                    case ROYAL_CHILD -> gendered("royal_child", gender);
                    case PRINCE_CONSORT -> gendered("prince_consort", gender);
                    case DOWAGER_PRINCE -> gendered("dowager_prince", gender);
                    case DUKE -> gendered("duke", gender);
                    case DOWAGER_DUKE -> gendered("dowager_duke", gender);
                    case LORD -> gendered("lord", gender);
                    case KNIGHT -> gendered("knight", gender);
                    case COMMONER -> "mcacapitals.dynamic.title.commoner";
                }
        );
    }

    public static CapitalChronicleEntry.Argument office(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        CapitalTitleOfficeIdentityResolver.OfficeIdentity office =
                CapitalTitleOfficeIdentityResolver.resolveOffice(level, capital, entityId);

        return CapitalChronicleEntry.Argument.translatable(
                switch (office) {
                    case NONE -> "mcacapitals.dynamic.office.none";
                    case HAND -> handOfficeKey(level, capital);
                    case GRAND_MAESTER -> "mcacapitals.dynamic.office.grand_maester";
                    case COURT_HERALD -> "mcacapitals.dynamic.office.court_herald";
                    case LORD_COMMANDER -> "mcacapitals.dynamic.office.lord_commander";
                    case MAESTER -> "mcacapitals.dynamic.office.maester";
                    case MASTER_OF_LAWS -> "mcacapitals.dynamic.office.master_of_laws";
                    case ROYAL_GUARD -> "mcacapitals.dynamic.office.royal_guard";
                    case AMBASSADOR -> "mcacapitals.dynamic.office.ambassador";
                }
        );
    }

    public static CapitalChronicleEntry.Argument handOffice(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return CapitalChronicleEntry.Argument.translatable(handOfficeKey(level, capital));
    }

    public static CapitalChronicleEntry.Argument genderedTitle(
            String base,
            CapitalDialogueGenderResolver.ResolvedGender gender
    ) {
        return CapitalChronicleEntry.Argument.translatable(gendered(base, gender));
    }

    private static String handOfficeKey(ServerLevel level, CapitalRecord capital) {
        UUID sovereignId = capital == null
                ? null
                : capital.getPlayerSovereignId() != null
                ? capital.getPlayerSovereignId()
                : capital.getSovereign();
        CapitalDialogueGenderResolver.ResolvedGender gender =
                CapitalDialogueGenderResolver.resolve(level, capital, sovereignId);
        return "mcacapitals.dynamic.office.hand." + genderPath(gender);
    }

    private static String gendered(
            String base,
            CapitalDialogueGenderResolver.ResolvedGender gender
    ) {
        return "mcacapitals.dynamic.title." + base + "." + genderPath(gender);
    }

    private static String genderPath(CapitalDialogueGenderResolver.ResolvedGender gender) {
        return switch (gender) {
            case FEMALE -> "female";
            case MALE -> "male";
            case NEUTRAL -> "neutral";
        };
    }
}
