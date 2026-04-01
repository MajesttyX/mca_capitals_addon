package com.example.mcacapitals.dialogue;

import java.util.List;
import java.util.Map;

final class CapitalDialogueLibraryNews {

    private CapitalDialogueLibraryNews() {
    }

    static void register(Map<CapitalDialogueKey, List<String>> lines) {
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
    }
}