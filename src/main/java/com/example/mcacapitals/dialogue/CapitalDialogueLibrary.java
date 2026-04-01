package com.example.mcacapitals.dialogue;

import net.minecraft.util.RandomSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CapitalDialogueLibrary {
    private static final Map<CapitalDialogueKey, List<String>> LINES = buildLines();

    private CapitalDialogueLibrary() {
    }

    public static String getRandomLine(CapitalDialogueKey key, RandomSource random, Object... args) {
        List<String> options = LINES.get(key);
        if (options == null || options.isEmpty()) {
            return "";
        }

        RandomSource actualRandom = random != null ? random : RandomSource.create();
        String template = options.get(actualRandom.nextInt(options.size()));
        if (template == null) {
            return "";
        }

        return args == null || args.length == 0 ? template : template.formatted(args);
    }

    public static int getLineCount(CapitalDialogueKey key) {
        List<String> options = LINES.get(key);
        return options == null ? 0 : options.size();
    }

    public static String getIndexedLine(CapitalDialogueKey key, int index, Object... args) {
        List<String> options = LINES.get(key);
        if (options == null || options.isEmpty()) {
            return "";
        }

        int safeIndex = Math.floorMod(index, options.size());
        String template = options.get(safeIndex);
        if (template == null) {
            return "";
        }

        return args == null || args.length == 0 ? template : template.formatted(args);
    }

    private static Map<CapitalDialogueKey, List<String>> buildLines() {
        Map<CapitalDialogueKey, List<String>> lines = new EnumMap<>(CapitalDialogueKey.class);

        lines.put(
                CapitalDialogueKey.PETITION_SOVEREIGN_ONLY,
                List.of(
                        "Only a reigning sovereign can hear this petition.",
                        "If you would make a petition, bring it before the reigning sovereign.",
                        "Such matters are heard only by the one who sits the throne.",
                        "Take that petition to the reigning sovereign, not to me."
                )
        );

        lines.put(
                CapitalDialogueKey.PETITION_MISSING_VILLAGE,
                List.of(
                        "Something is amiss with the records of this capital.",
                        "The records of this capital are not in proper order.",
                        "There is a fault in the village record. This matter cannot be heard now.",
                        "The capital's records are in disarray. Ask again later."
                )
        );

        lines.put(
                CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED,
                List.of(
                        "You must stand before the sovereign to present this petition.",
                        "If you mean to make your case, then stand before the sovereign and say it plainly.",
                        "Such petitions are heard face to face before the throne.",
                        "Come before the sovereign properly if you wish to be heard."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_NO_SOVEREIGN,
                List.of(
                        "This capital has no reigning sovereign.",
                        "There is no crowned ruler here to answer such a petition.",
                        "No sovereign holds this throne at present.",
                        "There is no reigning sovereign to hear your claim."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_NOT_REIGNING,
                List.of(
                        "That villager is no longer the reigning sovereign.",
                        "You are speaking to the wrong ruler. That one no longer holds the throne.",
                        "That crown no longer belongs to this villager.",
                        "That villager no longer reigns here."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_PLAYER_HELD,
                List.of(
                        "That throne is already held by a player sovereign.",
                        "A player already sits that throne.",
                        "That crown has already passed into player hands.",
                        "This capital is already ruled by a player sovereign."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_POPULATION_TOO_LOW,
                List.of(
                        "I will not surrender the throne until %s stands stronger. Let the capital reach a population of %s first.",
                        "The throne will not change hands by petition until %s has grown to a population of %s.",
                        "This capital is not yet strong enough for such a transfer. Let it reach a population of %s first.",
                        "When %s reaches a population of %s, then such a petition may be heard."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_LOW_STANDING,
                List.of(
                        "You do not yet have the standing to claim the throne of %s.",
                        "The people of %s do not favor your claim strongly enough.",
                        "You have not yet earned the standing needed to rule %s.",
                        "The throne of %s is not given to one without stronger standing."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_ALREADY_RULES_OTHER,
                List.of(
                        "You already rule another capital and cannot petition for this throne.",
                        "One ruler cannot peacefully claim two crowns in this way.",
                        "You already sit another throne. This petition cannot be granted.",
                        "You cannot petition for this crown while ruling another capital."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_SUCCESS,
                List.of(
                        "Your petition is accepted. The throne of %s passes peacefully to you.",
                        "So be it. By petition, the throne of %s passes into your hands.",
                        "The matter is settled. You shall rule %s in peace.",
                        "Your claim is accepted. The crown of %s is yours now."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NO_SOVEREIGN,
                List.of(
                        "You would challenge the throne? Then speak to the one who wears the crown.",
                        "Why are you asking me? If it is the throne you want, ask the king or queen.",
                        "That is not for me to hear. Bring such words before the reigning sovereign.",
                        "If you mean to contest the crown, you must stand before the sovereign."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_MISSING_VILLAGE,
                List.of(
                        "Something is amiss with the records of this capital. I cannot hear this matter now.",
                        "The capital's records are in disarray. This petition cannot be answered.",
                        "There is a problem with the village record. Ask again when the court is in order.",
                        "The records of this capital are not in proper order. I cannot judge this request."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NOT_IN_AUDIENCE,
                List.of(
                        "If you would speak of taking the throne, stand before the sovereign and say it plainly.",
                        "Do not mutter treason from afar. Stand before the sovereign if you dare.",
                        "If you mean to seize the crown, come before the throne and speak plainly.",
                        "Such words belong in the royal presence, not shouted from a distance."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_LOW_REPUTATION,
                List.of(
                        "You would take the throne? The capital does not love you enough for that.",
                        "The people would never follow you in this. Mind your station.",
                        "You have not earned the standing to make a bid for the crown.",
                        "The capital turns from your claim. You have not won its favor."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NO_ADVANCEMENT,
                List.of(
                        "Bold words alone do not make a ruler. You have not yet proven your worth.",
                        "You speak of taking the throne, yet you have not shown the might such a deed demands.",
                        "The crown is not won by talk. Prove your strength before you make such a claim.",
                        "A usurper must inspire fear or awe. You have done neither."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NO_COMMANDER_SUPPORT,
                List.of(
                        "The Commander and the army do not stand with you. Without steel at your back, this ends here.",
                        "You would seize the throne without the army's support? That is folly.",
                        "The Commander has not declared for you, and the guard will not move at your word.",
                        "Without the Commander or the army behind you, your claim dies where it stands."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_SUCCESS,
                List.of(
                        "So be it. The throne of %s is yours now.",
                        "Then let it be known: the crown of %s passes to you by force.",
                        "The matter is decided. You have taken the throne of %s.",
                        "By strength and boldness, you have claimed the throne of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_POPULATION_TOO_LOW,
                List.of(
                        "The office of Commander of the Royal Army will not be granted until this capital reaches a population of %s.",
                        "This capital must grow to a population of %s before a Commander of the Royal Army may be named.",
                        "The army will not take a Commander until the capital reaches a population of %s.",
                        "When the capital reaches a population of %s, then this office may be granted."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_LOW_STANDING,
                List.of(
                        "You do not yet have the standing to be entrusted with command in %s.",
                        "The people of %s do not yet trust you with command.",
                        "You have not yet earned the standing required to lead the royal army of %s.",
                        "Command in %s is not given without stronger standing."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_ALREADY_GRANTED,
                List.of(
                        "The office of Commander of the Royal Army has already been granted and cannot be taken from another sworn player.",
                        "That office is already held by another sworn commander.",
                        "Another player has already been entrusted with command of the royal army.",
                        "The office has already been granted elsewhere and cannot be stripped by petition."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_ALREADY_HELD,
                List.of(
                        "You already hold the office of Commander of the Royal Army.",
                        "You are already my Commander of the Royal Army.",
                        "That office is already yours.",
                        "You already command the royal army."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_REASSIGN_FAILED,
                List.of(
                        "The office of Commander of the Royal Army could not be reassigned.",
                        "Something prevented the command from being granted.",
                        "This appointment could not be completed.",
                        "The office could not be transferred into your hands."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_SUCCESS,
                List.of(
                        "Your petition is accepted. You are now Commander of the Royal Army of %s.",
                        "So be it. You shall command the royal army of %s.",
                        "The office is yours. You are now Commander of the Royal Army of %s.",
                        "I grant it. You are now Commander of the Royal Army of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_ALREADY_HIGHER,
                List.of(
                        "You already hold a higher noble dignity than Lord or Lady.",
                        "You already stand above the lesser nobility.",
                        "Why would I lower your rank? You already hold a higher dignity.",
                        "You already possess a greater noble title than Lord or Lady."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_ALREADY_HELD,
                List.of(
                        "You already hold the dignity of Lord or Lady.",
                        "You are already one of my lesser nobles.",
                        "That title is already yours.",
                        "You already stand as Lord or Lady of this capital."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_LOW_STANDING,
                List.of(
                        "You have not yet earned the standing required to be raised to the lesser nobility of %s.",
                        "The lesser nobility of %s is not granted without stronger standing.",
                        "You have not yet earned enough favor to be named among the lesser nobles of %s.",
                        "Your standing in %s is not yet high enough for such a title."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_NOT_ENOUGH_MASTERS,
                List.of(
                        "I will not grant this petition until at least %s master villagers strengthen the standing of the capital.",
                        "This capital must have at least %s master villagers before I grant such a title.",
                        "When at least %s master villagers serve this capital, then this petition may be heard.",
                        "The capital is not yet ready. It must first have at least %s master villagers."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_SUCCESS,
                List.of(
                        "Your petition is accepted. You are now %s of %s.",
                        "So be it. You are now %s of %s.",
                        "I grant it. You shall be known as %s of %s.",
                        "The matter is settled. You are now %s of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_ALREADY_HELD,
                List.of(
                        "You already hold the dignity of Duke or Duchess.",
                        "You already stand among my highest nobles.",
                        "That ducal dignity is already yours.",
                        "You already hold ducal rank."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_POPULATION_TOO_LOW,
                List.of(
                        "I will not grant ducal rank until this capital reaches a population of %s.",
                        "This capital must grow to a population of %s before ducal rank may be granted.",
                        "Ducal rank will be granted only when the capital reaches a population of %s.",
                        "When this capital reaches a population of %s, then such a title may be granted."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_LOW_STANDING,
                List.of(
                        "You have not yet earned the standing required to be raised to ducal rank in %s.",
                        "Ducal rank in %s is not granted without stronger standing.",
                        "Your standing in %s is not yet high enough for ducal rank.",
                        "You have not yet earned enough favor to be raised as a duke or duchess of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_SUCCESS,
                List.of(
                        "Your petition is accepted. You are now %s of %s.",
                        "So be it. You are now %s of %s.",
                        "I grant it. You shall be known as %s of %s.",
                        "The matter is settled. You are now %s of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_NO_ELIGIBLE_MATCH,
                List.of(
                        "There is no eligible match in this capital who may presently be named in a betrothal petition.",
                        "No suitable match in this capital may presently be named in such a petition.",
                        "There is no one here who may rightly be named in a betrothal petition at this time.",
                        "No proper match is available in this capital for such a petition."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_MISSING_VILLAGE,
                List.of(
                        "Something is amiss with the records of this capital.",
                        "The capital's records are in disarray.",
                        "There is a fault in the village record. This matter cannot be decided now.",
                        "The records of this capital are not in proper order."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_INVALID_TARGET,
                List.of(
                        "Only an eligible teen or adult noble of this capital may be chosen for betrothal.",
                        "You may only name an eligible teen or adult noble of this capital in such a petition.",
                        "That choice is not valid. Only an eligible teen or adult noble of this capital may be chosen.",
                        "Only a proper noble of age within this capital may be named in this betrothal."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_FAILED,
                List.of(
                        "This betrothal cannot be granted: %s",
                        "That petition fails: %s",
                        "I cannot grant that match: %s",
                        "This matter cannot proceed: %s"
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_SUCCESS,
                List.of(
                        "Your petition is accepted. %s is now betrothed to you.",
                        "So be it. %s is now promised to you.",
                        "The matter is settled. %s is now betrothed to you.",
                        "I grant it. %s is now pledged to you."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_RECOMMEND_INVALID_TARGET,
                List.of(
                        "Only residents of this capital may be named in a betrothal recommendation.",
                        "You may only name residents of this capital in such a recommendation.",
                        "That recommendation is not valid. Only residents of this capital may be named.",
                        "Only those who belong to this capital may be named in this matter."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_RECOMMEND_FAILED,
                List.of(
                        "This recommendation cannot be granted: %s",
                        "That recommendation fails: %s",
                        "I cannot approve that match: %s",
                        "This matter cannot proceed: %s"
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_RECOMMEND_SUCCESS,
                List.of(
                        "Your recommendation is accepted. %s and %s are now betrothed.",
                        "So be it. %s and %s are now promised to one another.",
                        "The matter is settled. %s and %s are now betrothed.",
                        "I grant it. %s and %s are now pledged to one another."
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_MARRIAGE,
                List.of(
                        "Have you heard the happy news? %s",
                        "The whole capital has been speaking of the wedding. %s",
                        "There has been much celebration of late. %s",
                        "Word around the capital is joyful for once. %s",
                        "People can talk of little else just now. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_DEATH_OR_SUCCESSION,
                List.of(
                        "Word travels fast here. %s",
                        "The whole realm has been unsettled of late. %s",
                        "There has been grave talk throughout the capital. %s",
                        "Everyone has been whispering of it. %s",
                        "No one in the capital can ignore it. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_MOURNING_DECLARED,
                List.of(
                        "A hush has fallen over the capital. %s",
                        "You can feel the sorrow in every street. %s",
                        "The whole court is dressed in mourning now. %s",
                        "There has been sombre talk all through the capital. %s",
                        "No one has been in good spirits since it happened. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_MOURNING_ENDED,
                List.of(
                        "It seems the capital is beginning to breathe again. %s",
                        "Folk say the mourning has finally passed. %s",
                        "The mood in the capital has begun to lift. %s",
                        "There has been quieter talk now that the mourning is over. %s",
                        "At last, the capital has stepped out from its mourning. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_HEIR_NAMED,
                List.of(
                        "The court has been speaking of succession again. %s",
                        "Everyone seems to know who stands next in line now. %s",
                        "There has been much talk over the naming of an heir. %s",
                        "The capital is full of whispers about the future of the crown. %s",
                        "That bit of royal news has travelled very quickly. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_DISINHERITED,
                List.of(
                        "The court has been restless ever since. %s",
                        "There has been scandal enough for a month. %s",
                        "No shortage of whispers after that decree. %s",
                        "The realm does love its gossip when succession is involved. %s",
                        "That news spread through the capital like wildfire. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_LEGITIMIZED,
                List.of(
                        "The court has been full of talk about bloodlines and claims. %s",
                        "That royal decree has given everyone something to chatter over. %s",
                        "There has been no end of whispering since the proclamation. %s",
                        "People are already debating what it means for the realm. %s",
                        "The capital has taken keen interest in that decision. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_THRONE_CHANGE,
                List.of(
                        "The capital has had no shortage of talk about the crown. %s",
                        "The whole realm seems to be discussing the throne. %s",
                        "That change at court has everyone murmuring. %s",
                        "No one here has stopped talking about the crown since it happened. %s",
                        "All eyes have been on the royal court of late. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_CAPITAL_FOUNDED,
                List.of(
                        "This place has risen in standing. %s",
                        "There has been proud talk all around the capital. %s",
                        "Folk here have been speaking boldly since the news. %s",
                        "You can tell the people here stand a little taller now. %s",
                        "The whole settlement has taken pride in that news. %s"
                )
        );

        lines.put(
                CapitalDialogueKey.NEWS_GENERIC_NOTABLE,
                List.of(
                        "Have you heard? %s",
                        "Word travels fast here. %s",
                        "There has been much talk of late. %s",
                        "The whole capital is speaking of it. %s",
                        "That has certainly given people something to talk about. %s"
                )
        );

        return lines;
    }
}