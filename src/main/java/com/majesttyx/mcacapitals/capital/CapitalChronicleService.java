package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CapitalChronicleService {

    private static final int CHARS_PER_PAGE = 220;
    private static final int MAX_PAGES = 100;

    private static final Pattern DUCAL_APPOINTMENT = Pattern.compile("^(.*) was elevated to the ducal rank in (.*)\\.$");
    private static final Pattern ROYAL_MARRIAGE = Pattern.compile("^(.*) was married to (.*)\\.$");
    private static final Pattern CAPITAL_MARRIAGE = Pattern.compile("^(.*) and (.*) were married in (.*)\\.$");
    private static final Pattern ROYAL_CHILD = Pattern.compile("^A royal child, (.*), was entered into the dynastic record of (.*)\\.$");
    private static final Pattern HAND_APPOINTMENT = Pattern.compile("^(.*) was appointed (Hand of the (?:Queen|King)) of (.*)\\.$");
    private static final Pattern GRAND_MAESTER_APPOINTMENT = Pattern.compile("^(.*) was appointed Grand Maester of (.*)\\.$");
    private static final Pattern HERALD_APPOINTMENT = Pattern.compile("^(.*) was appointed Court Herald of (.*)\\.$");
    private static final Pattern ROYAL_GUARD_APPOINTMENT = Pattern.compile("^(.*) was named to the royal guard of (.*)\\.$");
    private static final Pattern COMMANDER_APPOINTMENT = Pattern.compile("^(.*) was appointed Commander of the (Royal Guard|Army) of (.*)\\.$");
    private static final Pattern VACANCY = Pattern.compile("^The office of (.*) stands vacant in (.*)\\.$");
    private static final Pattern CAPITAL_CREATION = Pattern.compile("^(.*) rose to capital status\\.$");
    private static final Pattern ACCLAIMED_SOVEREIGN = Pattern.compile("^(.*) was acclaimed as (King|Queen) of (.*)\\.$");
    private static final Pattern CLAIMED_THRONE = Pattern.compile("^(.*) claimed the throne as (King|Queen) of (.*)\\.$");

    private static final String[] KNOWN_TITLE_PREFIXES = new String[] {
            "High Queen",
            "High King",
            "Dowager Queen",
            "Dowager King",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Crown Princess",
            "Crown Prince",
            "Dowager Princess",
            "Dowager Prince",
            "Princess Consort",
            "Prince Consort",
            "Hand of the Queen",
            "Hand of the King",
            "Grand Maester",
            "Maester",
            "Court Herald",
            "Princess",
            "Prince",
            "Lord Commander",
            "Dowager Duchess",
            "Dowager Duke",
            "Duchess",
            "Duke",
            "Lady",
            "Lord",
            "Dame",
            "Sir",
            "Queen",
            "King"
    };

    private CapitalChronicleService() {
    }

    public static void addEntry(ServerLevel level, CapitalRecord capital, String entry) {
        if (level == null || capital == null || entry == null || entry.isBlank()) {
            return;
        }

        String normalized = entry.trim();
        if (hasChronicleEntry(capital, normalized)) {
            return;
        }

        capital.addChronicleEntry(normalized);
        announceThroughHerald(level, capital, normalized);
    }

    public static void addEntryWithoutHerald(CapitalRecord capital, String entry) {
        if (capital == null || entry == null || entry.isBlank()) {
            return;
        }

        String normalized = entry.trim();
        if (hasChronicleEntry(capital, normalized)) {
            return;
        }

        capital.addChronicleEntry(normalized);
    }

    private static boolean hasChronicleEntry(CapitalRecord capital, String normalized) {
        if (capital == null || normalized == null || normalized.isBlank()) {
            return false;
        }

        String canonical = canonicalChronicleEntry(normalized);

        for (String existing : capital.getChronicleEntries()) {
            if (existing == null) {
                continue;
            }

            String existingNormalized = existing.trim();
            if (existingNormalized.equals(normalized)) {
                return true;
            }

            if (canonical != null && canonical.equals(canonicalChronicleEntry(existingNormalized))) {
                return true;
            }
        }

        return false;
    }

    private static String canonicalChronicleEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }

        String trimmed = entry.trim();

        Matcher royalMarriage = ROYAL_MARRIAGE.matcher(trimmed);
        if (royalMarriage.matches()) {
            return canonicalMarriageEntry(
                    "royal_marriage",
                    royalMarriage.group(1),
                    royalMarriage.group(2),
                    null
            );
        }

        Matcher capitalMarriage = CAPITAL_MARRIAGE.matcher(trimmed);
        if (capitalMarriage.matches()) {
            return canonicalMarriageEntry(
                    "capital_marriage",
                    capitalMarriage.group(1),
                    capitalMarriage.group(2),
                    capitalMarriage.group(3)
            );
        }

        return null;
    }

    private static String canonicalMarriageEntry(String prefix, String firstName, String secondName, String villageName) {
        String first = canonicalPersonName(firstName);
        String second = canonicalPersonName(secondName);

        String left = first.compareTo(second) <= 0 ? first : second;
        String right = first.compareTo(second) <= 0 ? second : first;

        if (villageName == null || villageName.isBlank()) {
            return prefix + ":" + left + ":" + right;
        }

        return prefix + ":" + left + ":" + right + ":" + canonicalText(villageName);
    }

    private static String canonicalPersonName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String result = name.trim();

        if (result.endsWith(" of the Kingsguard")) {
            result = result.substring(0, result.length() - " of the Kingsguard".length()).trim();
        }
        if (result.endsWith(" of the Queensguard")) {
            result = result.substring(0, result.length() - " of the Queensguard".length()).trim();
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String title : KNOWN_TITLE_PREFIXES) {
                String prefix = title + " ";
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }

        return canonicalText(result);
    }

    private static String canonicalText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static void announceThroughHerald(ServerLevel level, CapitalRecord capital, String entry) {
        if (level == null || capital == null || entry == null || entry.isBlank()) {
            return;
        }

        if (capital.getHerald() == null) {
            return;
        }

        String news = toHeraldNews(entry);
        if (news == null || news.isBlank()) {
            return;
        }

        String speakerName = CapitalHeraldService.resolveHeraldSpeakerName(level, capital);
        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                capital,
                Component.literal(speakerName + ": " + news.trim())
        );
    }

    public static String toHeraldNews(String entry) {
        if (entry == null || entry.isBlank()) {
            return "";
        }

        String trimmed = entry.trim();

        Matcher ducal = DUCAL_APPOINTMENT.matcher(trimmed);
        if (ducal.matches()) {
            return pick(trimmed,
                    "Let all in " + ducal.group(2) + " know: " + ducal.group(1) + " is raised to ducal rank.",
                    "By decree of the crown, " + ducal.group(1) + " now holds ducal rank in " + ducal.group(2) + ".",
                    ducal.group(1) + " has been elevated among the high nobility of " + ducal.group(2) + ".",
                    "The court proclaims " + ducal.group(1) + " a duke of " + ducal.group(2) + ".",
                    "A new ducal title is granted in " + ducal.group(2) + ": " + ducal.group(1) + ".");
        }

        Matcher royalMarriage = ROYAL_MARRIAGE.matcher(trimmed);
        if (royalMarriage.matches()) {
            return pick(trimmed,
                    "Hear this joyous proclamation: " + royalMarriage.group(1) + " is wed to " + royalMarriage.group(2) + ".",
                    "The court rejoices in the marriage of " + royalMarriage.group(1) + " and " + royalMarriage.group(2) + ".",
                    royalMarriage.group(1) + " and " + royalMarriage.group(2) + " are joined in royal marriage.",
                    "Let bells ring for the union of " + royalMarriage.group(1) + " and " + royalMarriage.group(2) + ".",
                    "A royal marriage is proclaimed: " + royalMarriage.group(1) + " and " + royalMarriage.group(2) + ".");
        }

        Matcher capitalMarriage = CAPITAL_MARRIAGE.matcher(trimmed);
        if (capitalMarriage.matches()) {
            return pick(trimmed,
                    capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " were wed in " + capitalMarriage.group(3) + ".",
                    "Joy comes to " + capitalMarriage.group(3) + ": " + capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " are married.",
                    "The court records the marriage of " + capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " in " + capitalMarriage.group(3) + ".",
                    "Let all in " + capitalMarriage.group(3) + " celebrate the union of " + capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + ".",
                    capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " have joined houses in " + capitalMarriage.group(3) + ".");
        }

        Matcher child = ROYAL_CHILD.matcher(trimmed);
        if (child.matches()) {
            return pick(trimmed,
                    "A royal child is entered into the dynastic record: " + child.group(1) + " of " + child.group(2) + ".",
                    "The dynasty of " + child.group(2) + " welcomes " + child.group(1) + ".",
                    "Let the court record the royal child " + child.group(1) + " of " + child.group(2) + ".",
                    child.group(1) + " is recognized in the royal line of " + child.group(2) + ".",
                    "The bloodline of " + child.group(2) + " is strengthened by " + child.group(1) + ".");
        }

        Matcher hand = HAND_APPOINTMENT.matcher(trimmed);
        if (hand.matches()) {
            return pick(trimmed,
                    hand.group(1) + " is appointed " + hand.group(2) + " of " + hand.group(3) + ".",
                    "The crown names " + hand.group(1) + " as " + hand.group(2) + ".",
                    "Let all know: " + hand.group(1) + " now serves as " + hand.group(2) + " of " + hand.group(3) + ".",
                    hand.group(1) + " takes up the office of " + hand.group(2) + ".",
                    "The court confirms " + hand.group(1) + " as " + hand.group(2) + " of " + hand.group(3) + ".");
        }

        Matcher maester = GRAND_MAESTER_APPOINTMENT.matcher(trimmed);
        if (maester.matches()) {
            return pick(trimmed,
                    maester.group(1) + " is appointed Grand Maester of " + maester.group(2) + ".",
                    "The court names " + maester.group(1) + " Grand Maester of " + maester.group(2) + ".",
                    "Wisdom is called to court: " + maester.group(1) + " becomes Grand Maester.",
                    "Let all know that " + maester.group(1) + " now serves as Grand Maester of " + maester.group(2) + ".",
                    maester.group(1) + " takes up the chain and duties of Grand Maester in " + maester.group(2) + ".");
        }

        Matcher herald = HERALD_APPOINTMENT.matcher(trimmed);
        if (herald.matches()) {
            return pick(trimmed,
                    herald.group(1) + " is appointed Court Herald of " + herald.group(2) + ".",
                    "Let all know: " + herald.group(1) + " will speak the court's proclamations.",
                    "The court names " + herald.group(1) + " as Herald of " + herald.group(2) + ".",
                    herald.group(1) + " now bears the voice of the court.",
                    "Proclamations of " + herald.group(2) + " shall be carried by " + herald.group(1) + ".");
        }

        Matcher guard = ROYAL_GUARD_APPOINTMENT.matcher(trimmed);
        if (guard.matches()) {
            return pick(trimmed,
                    guard.group(1) + " is named to the royal guard of " + guard.group(2) + ".",
                    "The crown's shield grows stronger: " + guard.group(1) + " joins the royal guard.",
                    "Let all know: " + guard.group(1) + " now stands among the royal guard of " + guard.group(2) + ".",
                    guard.group(1) + " takes the oath of the royal guard.",
                    "The court names " + guard.group(1) + " to guard the crown of " + guard.group(2) + ".");
        }

        Matcher commander = COMMANDER_APPOINTMENT.matcher(trimmed);
        if (commander.matches()) {
            return pick(trimmed,
                    commander.group(1) + " is appointed Commander of the " + commander.group(2) + " of " + commander.group(3) + ".",
                    "The crown names " + commander.group(1) + " to command the " + commander.group(2) + ".",
                    "Let all know: " + commander.group(1) + " now commands the " + commander.group(2) + " of " + commander.group(3) + ".",
                    commander.group(1) + " takes command in service of " + commander.group(3) + ".",
                    "The court proclaims " + commander.group(1) + " Commander of the " + commander.group(2) + " of " + commander.group(3) + ".");
        }

        Matcher vacancy = VACANCY.matcher(trimmed);
        if (vacancy.matches()) {
            return pick(trimmed,
                    "Let all in " + vacancy.group(2) + " know: the office of " + vacancy.group(1) + " stands vacant.",
                    "The court declares the office of " + vacancy.group(1) + " vacant in " + vacancy.group(2) + ".",
                    "Until further decree, no holder stands in the office of " + vacancy.group(1) + " in " + vacancy.group(2) + ".",
                    "The office of " + vacancy.group(1) + " now lies vacant in " + vacancy.group(2) + ".",
                    "No appointment currently fills the office of " + vacancy.group(1) + " in " + vacancy.group(2) + ".");
        }

        Matcher creation = CAPITAL_CREATION.matcher(trimmed);
        if (creation.matches()) {
            return pick(trimmed,
                    creation.group(1) + " has risen to capital status.",
                    "Let all know: " + creation.group(1) + " now stands as a capital.",
                    "By proclamation of the court, " + creation.group(1) + " is raised to capital standing.",
                    creation.group(1) + " is this day declared a capital.",
                    "The village of " + creation.group(1) + " now holds capital status.");
        }

        Matcher acclaimed = ACCLAIMED_SOVEREIGN.matcher(trimmed);
        if (acclaimed.matches()) {
            return pick(trimmed,
                    "Let all in " + acclaimed.group(3) + " know: " + acclaimed.group(1) + " is acclaimed as " + acclaimed.group(2) + ".",
                    "By proclamation of the court, " + acclaimed.group(1) + " has been hailed as " + acclaimed.group(2) + " of " + acclaimed.group(3) + ".",
                    acclaimed.group(1) + " is this day acclaimed " + acclaimed.group(2) + " of " + acclaimed.group(3) + ".",
                    "The crown of " + acclaimed.group(3) + " now rests upon " + acclaimed.group(1) + ", acclaimed as " + acclaimed.group(2) + ".",
                    acclaimed.group(1) + " now holds the crown of " + acclaimed.group(3) + " as " + acclaimed.group(2) + ".");
        }

        Matcher claimed = CLAIMED_THRONE.matcher(trimmed);
        if (claimed.matches()) {
            return pick(trimmed,
                    "Let all in " + claimed.group(3) + " know: " + claimed.group(1) + " now reigns as " + claimed.group(2) + ".",
                    "By bold claim and sovereign right, " + claimed.group(1) + " takes the throne as " + claimed.group(2) + " of " + claimed.group(3) + ".",
                    "The throne of " + claimed.group(3) + " is now held by " + claimed.group(1) + ", who claims it as " + claimed.group(2) + ".",
                    claimed.group(1) + " has taken the throne of " + claimed.group(3) + " as " + claimed.group(2) + ".",
                    claimed.group(1) + " now sits the throne of " + claimed.group(3) + " as " + claimed.group(2) + ".");
        }

        return trimmed;
    }

    private static String pick(String seed, String... options) {
        if (options == null || options.length == 0) {
            return seed;
        }
        int index = Math.floorMod(seed.hashCode(), options.length);
        return options[index];
    }

    public static void bindChronicleItem(ServerLevel level, CapitalRecord capital, ItemStack stack) {
        if (level == null || capital == null || stack == null) {
            return;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        if (villageName == null || villageName.isBlank()) {
            villageName = "Unknown Capital";
        }

        String resolvedVillageName = villageName;
        ModItemStackData.updateCustomData(stack, tag -> {
            tag.putString(ModDataKeys.CAPITAL_ID, capital.getCapitalId().toString());
            tag.putInt(ModDataKeys.VILLAGE_ID, capital.getVillageId() == null ? -1 : capital.getVillageId());
            tag.putString(ModDataKeys.VILLAGE_NAME, resolvedVillageName);
        });
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(
                        "Chronicle of " + resolvedVillageName
                )
        );
    }

    public static void writeChronicleBook(ServerLevel level, CapitalRecord capital, ItemStack stack) {
        if (level == null || capital == null || stack == null) {
            return;
        }

        List<String> pages = createPages(level, capital);
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        String title = "Chronicle of " + villageName;
        String author = "The Royal Chancery";

        List<Filterable<Component>> writtenPages = new ArrayList<>();
        ListTag pageList = new ListTag();
        int count = 0;
        for (String page : pages) {
            if (count >= MAX_PAGES) {
                break;
            }
            writtenPages.add(Filterable.passThrough(Component.literal(page)));
            pageList.add(StringTag.valueOf(page));
            count++;
        }

        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(title),
                author,
                0,
                writtenPages,
                true
        ));

        ModItemStackData.updateCustomData(stack, tag -> {
            tag.putString(ModDataKeys.BOOK_TITLE, title);
            tag.putString(ModDataKeys.BOOK_AUTHOR, author);
            tag.putBoolean(ModDataKeys.BOOK_RESOLVED, true);
            tag.putInt(ModDataKeys.BOOK_GENERATION, 0);
            tag.put(ModDataKeys.BOOK_PAGES, pageList);
        });

        bindChronicleItem(level, capital, stack);
    }

    private static List<String> createPages(ServerLevel level, CapitalRecord capital) {
        List<String> pages = new ArrayList<>();
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        pages.add("Chronicle of " + villageName + "\n\nA record of the crown, the court, and the great events of the capital.");

        StringBuilder current = new StringBuilder();
        for (String entry : capital.getChronicleEntries()) {
            String line = entry + "\n\n";
            if (current.length() + line.length() > CHARS_PER_PAGE) {
                pages.add(current.toString());
                current = new StringBuilder();
            }
            current.append(line);
        }

        if (!current.isEmpty()) {
            pages.add(current.toString());
        }

        if (pages.isEmpty()) {
            pages.add("No entries have yet been recorded.");
        }

        return pages;
    }
}