package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleOfficeIdentityResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

final class CapitalDialogueIdentityResolver {

    record Identity(
            Component title,
            Component office,
            Component style,
            Component address,
            Component subjectPronoun,
            Component objectPronoun,
            Component possessiveAdjective,
            Component possessivePronoun,
            Component reflexivePronoun
    ) {
    }

    private CapitalDialogueIdentityResolver() {
    }

    static Identity resolve(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId,
            Component fallbackName
    ) {
        CapitalTitleOfficeIdentityResolver.TitleIdentity titleIdentity =
                CapitalTitleOfficeIdentityResolver.resolveTitle(level, capital, entityId);
        CapitalTitleOfficeIdentityResolver.OfficeIdentity officeIdentity =
                CapitalTitleOfficeIdentityResolver.resolveOffice(level, capital, entityId);
        CapitalDialogueGenderResolver.ResolvedGender gender =
                CapitalDialogueGenderResolver.resolve(level, capital, entityId);

        Component title = titleComponent(titleIdentity, gender);
        Component office = officeComponent(level, capital, officeIdentity);
        Component style = styleComponent(titleIdentity, officeIdentity, gender, title, office);
        Component address = addressComponent(titleIdentity, officeIdentity, title, office, fallbackName);

        return new Identity(
                title,
                office,
                style,
                address,
                pronoun("subject", gender),
                pronoun("object", gender),
                pronoun("possessive_adjective", gender),
                pronoun("possessive", gender),
                pronoun("reflexive", gender)
        );
    }

    private static Component titleComponent(
            CapitalTitleOfficeIdentityResolver.TitleIdentity title,
            CapitalDialogueGenderResolver.ResolvedGender gender
    ) {
        return switch (title) {
            case HIGH_SOVEREIGN -> genderedTitle("high_sovereign", gender);
            case SOVEREIGN -> genderedTitle("sovereign", gender);
            case SOVEREIGN_CONSORT -> genderedTitle("sovereign_consort", gender);
            case SOVEREIGN_DOWAGER -> genderedTitle("sovereign_dowager", gender);
            case HEIR_APPARENT -> Component.translatable("mcacapitals.dynamic.title.heir_apparent");
            case CROWN_HEIR -> genderedTitle("crown_heir", gender);
            case ROYAL_CHILD -> genderedTitle("royal_child", gender);
            case PRINCE_CONSORT -> genderedTitle("prince_consort", gender);
            case DOWAGER_PRINCE -> genderedTitle("dowager_prince", gender);
            case DUKE -> genderedTitle("duke", gender);
            case DOWAGER_DUKE -> genderedTitle("dowager_duke", gender);
            case LORD -> genderedTitle("lord", gender);
            case KNIGHT -> genderedTitle("knight", gender);
            case COMMONER -> Component.translatable("mcacapitals.dynamic.title.commoner");
        };
    }

    private static Component officeComponent(
            ServerLevel level,
            CapitalRecord capital,
            CapitalTitleOfficeIdentityResolver.OfficeIdentity office
    ) {
        return switch (office) {
            case NONE -> Component.translatable("mcacapitals.dynamic.office.none");
            case HAND -> handOffice(level, capital);
            case GRAND_MAESTER -> Component.translatable("mcacapitals.dynamic.office.grand_maester");
            case COURT_HERALD -> Component.translatable("mcacapitals.dynamic.office.court_herald");
            case LORD_COMMANDER -> Component.translatable("mcacapitals.dynamic.office.lord_commander");
            case MAESTER -> Component.translatable("mcacapitals.dynamic.office.maester");
            case MASTER_OF_LAWS -> Component.translatable("mcacapitals.dynamic.office.master_of_laws");
            case ROYAL_GUARD -> Component.translatable("mcacapitals.dynamic.office.royal_guard");
            case AMBASSADOR -> Component.translatable("mcacapitals.dynamic.office.ambassador");
        };
    }

    private static Component handOffice(ServerLevel level, CapitalRecord capital) {
        UUID sovereignId = capital == null
                ? null
                : capital.getPlayerSovereignId() != null
                ? capital.getPlayerSovereignId()
                : capital.getSovereign();

        CapitalDialogueGenderResolver.ResolvedGender sovereignGender =
                CapitalDialogueGenderResolver.resolve(level, capital, sovereignId);

        return Component.translatable(
                "mcacapitals.dynamic.office.hand."
                        + genderPath(sovereignGender)
        );
    }

    private static Component styleComponent(
            CapitalTitleOfficeIdentityResolver.TitleIdentity title,
            CapitalTitleOfficeIdentityResolver.OfficeIdentity office,
            CapitalDialogueGenderResolver.ResolvedGender gender,
            Component fallbackTitle,
            Component fallbackOffice
    ) {
        String key = switch (title) {
            case HIGH_SOVEREIGN, SOVEREIGN, SOVEREIGN_CONSORT, SOVEREIGN_DOWAGER ->
                    "mcacapitals.dynamic.style.majesty." + genderPath(gender);
            case CROWN_HEIR ->
                    "mcacapitals.dynamic.style.royal_highness." + genderPath(gender);
            case ROYAL_CHILD, PRINCE_CONSORT, DOWAGER_PRINCE ->
                    "mcacapitals.dynamic.style.highness." + genderPath(gender);
            case DUKE, DOWAGER_DUKE ->
                    "mcacapitals.dynamic.style.grace." + genderPath(gender);
            default -> null;
        };

        if (key != null) {
            return Component.translatable(key);
        }
        if (title == CapitalTitleOfficeIdentityResolver.TitleIdentity.COMMONER
                && office != CapitalTitleOfficeIdentityResolver.OfficeIdentity.NONE) {
            return fallbackOffice;
        }
        return fallbackTitle;
    }

    private static Component addressComponent(
            CapitalTitleOfficeIdentityResolver.TitleIdentity title,
            CapitalTitleOfficeIdentityResolver.OfficeIdentity office,
            Component fallbackTitle,
            Component fallbackOffice,
            Component fallbackName
    ) {
        return switch (title) {
            case HIGH_SOVEREIGN, SOVEREIGN, SOVEREIGN_CONSORT, SOVEREIGN_DOWAGER ->
                    Component.translatable("mcacapitals.dynamic.address.majesty");
            case CROWN_HEIR ->
                    Component.translatable("mcacapitals.dynamic.address.royal_highness");
            case ROYAL_CHILD, PRINCE_CONSORT, DOWAGER_PRINCE ->
                    Component.translatable("mcacapitals.dynamic.address.highness");
            case DUKE, DOWAGER_DUKE ->
                    Component.translatable("mcacapitals.dynamic.address.grace");
            case COMMONER -> office != CapitalTitleOfficeIdentityResolver.OfficeIdentity.NONE
                    ? fallbackOffice
                    : fallbackName == null
                    ? Component.translatable("mcacapitals.dynamic.someone")
                    : fallbackName;
            default -> fallbackTitle;
        };
    }

    private static Component genderedTitle(
            String base,
            CapitalDialogueGenderResolver.ResolvedGender gender
    ) {
        return Component.translatable(
                "mcacapitals.dynamic.title."
                        + base
                        + "."
                        + genderPath(gender)
        );
    }

    private static Component pronoun(
            String type,
            CapitalDialogueGenderResolver.ResolvedGender gender
    ) {
        return Component.translatable(
                "mcacapitals.dynamic.pronoun."
                        + type
                        + "."
                        + genderPath(gender)
        );
    }

    private static String genderPath(CapitalDialogueGenderResolver.ResolvedGender gender) {
        return switch (gender) {
            case FEMALE -> "female";
            case MALE -> "male";
            case NEUTRAL -> "neutral";
        };
    }
}
