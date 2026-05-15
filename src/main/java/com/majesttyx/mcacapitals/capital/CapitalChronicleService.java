package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

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

    private CapitalChronicleService() {
    }

    public static void addEntry(ServerLevel level, CapitalRecord capital, String text) {
        if (level == null || capital == null || text == null || text.isBlank()) {
            return;
        }

        String trimmed = text.trim();
        if (hasChronicleText(capital, trimmed)) {
            return;
        }

        long day = Math.max(1L, level.getDayTime() / 24000L + 1L);
        capital.addChronicleEntry("Day " + day + ": " + trimmed);

        if (isProclamation(trimmed)) {
            broadcastProclamation(level, capital, trimmed);
        }
    }

    private static boolean hasChronicleText(CapitalRecord capital, String text) {
        if (capital == null || text == null || text.isBlank()) {
            return false;
        }

        for (String entry : capital.getChronicleEntries()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String normalized = stripChroniclePrefix(entry);
            if (text.equals(normalized)) {
                return true;
            }
        }

        return false;
    }

    private static String stripChroniclePrefix(String entry) {
        if (entry == null) {
            return "";
        }

        int colon = entry.indexOf(':');
        if (entry.startsWith("Day ") && colon >= 0 && colon + 1 < entry.length()) {
            return entry.substring(colon + 1).trim();
        }

        return entry.trim();
    }

    private static boolean isProclamation(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("was appointed")
                || normalized.contains("was named")
                || normalized.contains("was raised to ducal rank")
                || normalized.contains("was elevated to the ducal rank")
                || normalized.contains("was married to")
                || normalized.contains("were married in")
                || normalized.contains("died")
                || normalized.contains("inherited the throne")
                || normalized.contains("claimed the throne")
                || normalized.contains("was acclaimed as")
                || normalized.contains("royal child")
                || normalized.contains("entered into the dynastic record")
                || normalized.contains("stands vacant")
                || normalized.contains("rose to capital status");
    }

    private static void broadcastProclamation(ServerLevel level, CapitalRecord capital, String text) {
        String speaker = CapitalHeraldService.resolveHeraldSpeakerName(level, capital);
        String proclamation = buildProclamationLine(level, capital, text);
        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                capital,
                Component.literal(speaker + ": " + proclamation)
        );
    }

    private static String buildProclamationLine(ServerLevel level, CapitalRecord capital, String text) {
        String trimmed = text == null ? "" : text.trim();

        Matcher ducal = DUCAL_APPOINTMENT.matcher(trimmed);
        if (ducal.matches()) {
            return pick(trimmed,
                    "Let all within " + ducal.group(2) + " know: " + ducal.group(1) + " now bears ducal rank.",
                    "By decree of the crown, " + ducal.group(1) + " is this day elevated to ducal dignity in " + ducal.group(2) + ".",
                    ducal.group(1) + " has been granted the honors of a duchy in " + ducal.group(2) + ".",
                    "By command of the crown, " + ducal.group(1) + " is named among the dukes and duchesses of " + ducal.group(2) + ".",
                    ducal.group(1) + " is now counted among the ducal ranks of " + ducal.group(2) + ".");
        }

        Matcher marriage = ROYAL_MARRIAGE.matcher(trimmed);
        if (marriage.matches()) {
            return pick(trimmed,
                    marriage.group(1) + " has entered into marriage with " + marriage.group(2) + ".",
                    "By joyous proclamation, " + marriage.group(1) + " and " + marriage.group(2) + " are joined in marriage.",
                    "Let all the capital know: " + marriage.group(1) + " has taken " + marriage.group(2) + " in marriage.",
                    "A royal marriage is declared this day between " + marriage.group(1) + " and " + marriage.group(2) + ".",
                    marriage.group(1) + " and " + marriage.group(2) + " are now joined in lawful marriage.");
        }

        Matcher capitalMarriage = CAPITAL_MARRIAGE.matcher(trimmed);
        if (capitalMarriage.matches()) {
            return pick(trimmed,
                    "Let all in " + capitalMarriage.group(3) + " know: " + capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " are now married.",
                    capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " have been joined in marriage in " + capitalMarriage.group(3) + ".",
                    "By proclamation of the court, " + capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " are wed in " + capitalMarriage.group(3) + ".",
                    "A marriage is declared in " + capitalMarriage.group(3) + " between " + capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + ".",
                    capitalMarriage.group(1) + " and " + capitalMarriage.group(2) + " now stand joined in marriage within " + capitalMarriage.group(3) + ".");
        }

        Matcher child = ROYAL_CHILD.matcher(trimmed);
        if (child.matches()) {
            return pick(trimmed,
                    child.group(1) + " now stands in the dynastic record of " + child.group(2) + ".",
                    child.group(1) + " has been entered among the royal line of " + child.group(2) + ".",
                    "By proclamation of the court, " + child.group(1) + " is entered into the dynastic record of " + child.group(2) + ".",
                    "The child " + child.group(1) + " has been recorded in the dynasty of " + child.group(2) + ".",
                    child.group(1) + " is this day acknowledged in the dynastic line of " + child.group(2) + ".");
        }

        Matcher hand = HAND_APPOINTMENT.matcher(trimmed);
        if (hand.matches()) {
            return pick(trimmed,
                    "By sovereign decree, " + hand.group(1) + " now serves as " + hand.group(2) + " of " + hand.group(3) + ".",
                    "Let all in " + hand.group(3) + " know: " + hand.group(1) + " has been invested as " + hand.group(2) + ".",
                    hand.group(1) + " is appointed " + hand.group(2) + " of " + hand.group(3) + ".",
                    "The court proclaims " + hand.group(1) + " as " + hand.group(2) + " of " + hand.group(3) + ".",
                    hand.group(1) + " now bears the office of " + hand.group(2) + " in " + hand.group(3) + ".");
        }

        Matcher gm = GRAND_MAESTER_APPOINTMENT.matcher(trimmed);
        if (gm.matches()) {
            return pick(trimmed,
                    "By decree of the court, " + gm.group(1) + " now serves as Grand Maester of " + gm.group(2) + ".",
                    "Let all in " + gm.group(2) + " take note: " + gm.group(1) + " has been raised to Grand Maester.",
                    gm.group(1) + " is named Grand Maester of " + gm.group(2) + ".",
                    "The chains of learning now rest with " + gm.group(1) + ", Grand Maester of " + gm.group(2) + ".",
                    gm.group(1) + " now holds the office of Grand Maester in " + gm.group(2) + ".");
        }

        Matcher herald = HERALD_APPOINTMENT.matcher(trimmed);
        if (herald.matches()) {
            return pick(trimmed,
                    "By decree of the court, " + herald.group(1) + " now bears the office of Court Herald of " + herald.group(2) + ".",
                    "Let all in " + herald.group(2) + " know: " + herald.group(1) + " has been named Court Herald.",
                    herald.group(1) + " now serves as Court Herald of " + herald.group(2) + ".",
                    "The voice of courtly proclamation now belongs to " + herald.group(1) + " in " + herald.group(2) + ".",
                    herald.group(1) + " has taken up the office of Court Herald in " + herald.group(2) + ".");
        }

        Matcher guard = ROYAL_GUARD_APPOINTMENT.matcher(trimmed);
        if (guard.matches()) {
            return pick(trimmed,
                    "By sovereign command, " + guard.group(1) + " now stands among the royal guard of " + guard.group(2) + ".",
                    "Let all in " + guard.group(2) + " know: " + guard.group(1) + " has joined the royal guard.",
                    guard.group(1) + " now serves in the royal guard of " + guard.group(2) + ".",
                    "The crown now counts " + guard.group(1) + " among the royal guard of " + guard.group(2) + ".",
                    guard.group(1) + " has taken up sworn service in the royal guard of " + guard.group(2) + ".");
        }

        Matcher commander = COMMANDER_APPOINTMENT.matcher(trimmed);
        if (commander.matches()) {
            return pick(trimmed,
                    "By decree of the crown, " + commander.group(1) + " now commands the " + commander.group(2) + " of " + commander.group(3) + ".",
                    commander.group(1) + " has been appointed Commander of the " + commander.group(2) + " in " + commander.group(3) + ".",
                    "Let all in " + commander.group(3) + " know: " + commander.group(1) + " now bears command of the " + commander.group(2) + ".",
                    commander.group(1) + " now holds command over the " + commander.group(2) + " of " + commander.group(3) + ".",
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

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ModDataKeys.CAPITAL_ID, capital.getCapitalId().toString());
        tag.putInt(ModDataKeys.VILLAGE_ID, capital.getVillageId() == null ? -1 : capital.getVillageId());
        tag.putString(ModDataKeys.VILLAGE_NAME, MCAIntegrationBridge.getVillageName(level, capital.getVillageId()));
    }

    public static void writeChronicleBook(ServerLevel level, CapitalRecord capital, ItemStack stack) {
        if (level == null || capital == null || stack == null) {
            return;
        }

        List<String> pages = createPages(level, capital);
        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ModDataKeys.BOOK_TITLE, "Chronicle of " + villageName);
        tag.putString(ModDataKeys.BOOK_AUTHOR, "The Royal Chancery");
        tag.putBoolean(ModDataKeys.BOOK_RESOLVED, true);
        tag.putInt(ModDataKeys.BOOK_GENERATION, 0);

        ListTag pageList = new ListTag();
        int count = 0;
        for (String page : pages) {
            if (count >= MAX_PAGES) {
                break;
            }
            String json = Component.Serializer.toJson(Component.literal(page));
            pageList.add(StringTag.valueOf(json));
            count++;
        }

        tag.put(ModDataKeys.BOOK_PAGES, pageList);
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