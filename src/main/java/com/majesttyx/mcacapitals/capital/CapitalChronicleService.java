package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CapitalChronicleService {

    private static final int CHARS_PER_PAGE = 220;
    private static final int MAX_PAGES = 100;

    private static final Map<UUID, HeraldAnnouncement> LAST_HERALD_ANNOUNCEMENTS = new HashMap<>();

    private CapitalChronicleService() {
    }

    public static void addEvent(
            ServerLevel level,
            CapitalRecord capital,
            CapitalChronicleEventId eventId,
            Object... arguments
    ) {
        addEventInternal(level, capital, eventId, true, arguments);
    }

    public static void addEventWithoutHerald(
            ServerLevel level,
            CapitalRecord capital,
            CapitalChronicleEventId eventId,
            Object... arguments
    ) {
        addEventInternal(level, capital, eventId, false, arguments);
    }

    public static CapitalChronicleEntry.Argument literal(Object value) {
        return CapitalChronicleEntry.Argument.literal(value);
    }

    public static CapitalChronicleEntry.Argument translatable(String translationKey) {
        return CapitalChronicleEntry.Argument.translatable(translationKey);
    }

    public static CapitalChronicleEntry.Argument translatableSnapshot(
            String translationKey,
            Object... literalArguments
    ) {
        return CapitalChronicleEntry.Argument.translatableSnapshot(
                translationKey,
                literalArguments
        );
    }

    public static CapitalChronicleEntry.Argument itemList(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return CapitalChronicleEntry.Argument.itemList("");
        }

        StringBuilder encoded = new StringBuilder();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (!encoded.isEmpty()) {
                encoded.append(';');
            }
            encoded.append(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .append('=')
                    .append(stack.getCount());
        }
        return CapitalChronicleEntry.Argument.itemList(encoded.toString());
    }

    public static void addEntry(
            ServerLevel level,
            CapitalRecord capital,
            String entry
    ) {
        if (level == null || capital == null || entry == null || entry.isBlank()) {
            return;
        }

        String normalized = entry.trim();
        if (hasRawChronicleEntry(capital, normalized)) {
            return;
        }

        capital.addChronicleEntry(normalized);
        announceLegacyThroughHerald(level, capital, normalized);
    }

    public static void addEntryWithoutHerald(CapitalRecord capital, String entry) {
        if (capital == null || entry == null || entry.isBlank()) {
            return;
        }

        String normalized = entry.trim();
        if (hasRawChronicleEntry(capital, normalized)) {
            return;
        }

        capital.addChronicleEntry(normalized);
    }

    public static Component renderStoredEntry(String storedEntry) {
        CapitalChronicleEntry semantic = CapitalChronicleEntry.decode(storedEntry);
        if (semantic != null) {
            return semantic.renderWithDay();
        }
        return Component.literal(storedEntry == null ? "" : storedEntry);
    }

    public static CapitalChronicleEntry decodeSemanticEntry(String storedEntry) {
        return CapitalChronicleEntry.decode(storedEntry);
    }

    public static boolean hasMarriageEvent(
            CapitalRecord capital,
            CapitalChronicleEventId eventId,
            String firstName,
            String secondName,
            String capitalName
    ) {
        if (capital == null
                || (eventId != CapitalChronicleEventId.ROYAL_MARRIAGE
                && eventId != CapitalChronicleEventId.CAPITAL_MARRIAGE)) {
            return false;
        }

        List<CapitalChronicleEntry.Argument> arguments = new ArrayList<>();
        arguments.add(CapitalChronicleEntry.Argument.literal(firstName));
        arguments.add(CapitalChronicleEntry.Argument.literal(secondName));
        if (eventId == CapitalChronicleEventId.CAPITAL_MARRIAGE) {
            arguments.add(CapitalChronicleEntry.Argument.literal(capitalName));
        }

        if (hasSemanticChronicleEntry(capital, semanticDedupeKey(eventId, arguments))) {
            return true;
        }

        String legacyKey = canonicalMarriage(
                eventId == CapitalChronicleEventId.ROYAL_MARRIAGE
                        ? "royal_marriage"
                        : "capital_marriage",
                firstName,
                secondName,
                eventId == CapitalChronicleEventId.CAPITAL_MARRIAGE ? capitalName : null
        );

        for (String stored : capital.getChronicleEntries()) {
            if (stored == null || CapitalChronicleEntry.decode(stored) != null) {
                continue;
            }
            if (legacyKey.equals(canonicalLegacyChronicleEntry(stored))) {
                return true;
            }
        }

        return false;
    }

    private static void addEventInternal(
            ServerLevel level,
            CapitalRecord capital,
            CapitalChronicleEventId eventId,
            boolean announceHerald,
            Object... rawArguments
    ) {
        if (level == null || capital == null || eventId == null) {
            return;
        }

        List<CapitalChronicleEntry.Argument> arguments = normalizeArguments(rawArguments);
        String dedupeKey = semanticDedupeKey(eventId, arguments);
        CapitalChronicleEntry entry = new CapitalChronicleEntry(
                currentDay(level),
                eventId.type(),
                eventId.chronicleKey(),
                announceHerald ? eventId.heraldKey() : "",
                dedupeKey,
                arguments
        );

        if (hasSemanticChronicleEntry(capital, dedupeKey)) {
            return;
        }

        capital.addChronicleEntry(entry.encode());
        if (announceHerald) {
            announceSemanticThroughHerald(level, capital, entry);
        }
    }

    private static List<CapitalChronicleEntry.Argument> normalizeArguments(Object[] rawArguments) {
        if (rawArguments == null || rawArguments.length == 0) {
            return List.of();
        }

        List<CapitalChronicleEntry.Argument> arguments = new ArrayList<>(rawArguments.length);
        for (Object rawArgument : rawArguments) {
            if (rawArgument instanceof CapitalChronicleEntry.Argument argument) {
                arguments.add(argument);
            } else if (rawArgument instanceof Number number) {
                arguments.add(CapitalChronicleEntry.Argument.literal(number.toString()));
            } else if (rawArgument instanceof Boolean value) {
                arguments.add(CapitalChronicleEntry.Argument.literal(Boolean.toString(value)));
            } else if (rawArgument == null) {
                arguments.add(CapitalChronicleEntry.Argument.literal(""));
            } else {
                arguments.add(CapitalChronicleEntry.Argument.literal(rawArgument));
            }
        }
        return List.copyOf(arguments);
    }

    private static boolean hasSemanticChronicleEntry(CapitalRecord capital, String dedupeKey) {
        if (capital == null || dedupeKey == null || dedupeKey.isBlank()) {
            return false;
        }

        for (String stored : capital.getChronicleEntries()) {
            CapitalChronicleEntry decoded = CapitalChronicleEntry.decode(stored);
            if (decoded != null && dedupeKey.equals(decoded.dedupeKey())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRawChronicleEntry(CapitalRecord capital, String normalized) {
        if (capital == null || normalized == null || normalized.isBlank()) {
            return false;
        }

        String canonical = canonicalLegacyChronicleEntry(normalized);
        for (String existing : capital.getChronicleEntries()) {
            if (existing == null || CapitalChronicleEntry.decode(existing) != null) {
                continue;
            }

            String existingNormalized = existing.trim();
            if (existingNormalized.equals(normalized)) {
                return true;
            }

            if (canonical != null && canonical.equals(canonicalLegacyChronicleEntry(existingNormalized))) {
                return true;
            }
        }
        return false;
    }

    private static String semanticDedupeKey(
            CapitalChronicleEventId eventId,
            List<CapitalChronicleEntry.Argument> arguments
    ) {
        if ((eventId == CapitalChronicleEventId.ROYAL_MARRIAGE
                || eventId == CapitalChronicleEventId.CAPITAL_MARRIAGE)
                && arguments.size() >= 2) {
            String first = canonicalText(arguments.get(0).value());
            String second = canonicalText(arguments.get(1).value());
            String left = first.compareTo(second) <= 0 ? first : second;
            String right = first.compareTo(second) <= 0 ? second : first;
            StringBuilder marriage = new StringBuilder(eventId.path())
                    .append(':')
                    .append(left)
                    .append(':')
                    .append(right);
            for (int i = 2; i < arguments.size(); i++) {
                marriage.append(':').append(canonicalText(arguments.get(i).value()));
            }
            return marriage.toString();
        }

        StringBuilder key = new StringBuilder(eventId.path());
        for (CapitalChronicleEntry.Argument argument : arguments) {
            key.append(':')
                    .append(argument.dedupeKindName())
                    .append(':')
                    .append(canonicalText(argument.dedupeValue()));
        }
        return key.toString();
    }

    private static String canonicalLegacyChronicleEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }

        String trimmed = stripLegacyDayPrefix(entry.trim());
        String royalMarker = " was married to ";
        int royalIndex = trimmed.indexOf(royalMarker);
        if (royalIndex > 0 && trimmed.endsWith(".")) {
            String first = trimmed.substring(0, royalIndex);
            String second = trimmed.substring(royalIndex + royalMarker.length(), trimmed.length() - 1);
            return canonicalMarriage("royal_marriage", first, second, null);
        }

        String capitalMarker = " and ";
        String marriedMarker = " were married in ";
        int firstJoin = trimmed.indexOf(capitalMarker);
        int marriedIndex = trimmed.indexOf(marriedMarker);
        if (firstJoin > 0 && marriedIndex > firstJoin && trimmed.endsWith(".")) {
            String first = trimmed.substring(0, firstJoin);
            String second = trimmed.substring(firstJoin + capitalMarker.length(), marriedIndex);
            String capital = trimmed.substring(marriedIndex + marriedMarker.length(), trimmed.length() - 1);
            return canonicalMarriage("capital_marriage", first, second, capital);
        }

        return null;
    }

    private static String canonicalMarriage(String prefix, String firstName, String secondName, String capitalName) {
        String first = canonicalText(CapitalNameService.normalizeBaseName(firstName));
        String second = canonicalText(CapitalNameService.normalizeBaseName(secondName));
        String left = first.compareTo(second) <= 0 ? first : second;
        String right = first.compareTo(second) <= 0 ? second : first;
        if (capitalName == null || capitalName.isBlank()) {
            return prefix + ':' + left + ':' + right;
        }
        return prefix + ':' + left + ':' + right + ':' + canonicalText(capitalName);
    }

    private static String stripLegacyDayPrefix(String value) {
        if (value == null || !value.startsWith("Day ")) {
            return value;
        }

        int separator = value.indexOf(": ");
        if (separator <= 4) {
            return value;
        }

        String day = value.substring(4, separator);
        for (int i = 0; i < day.length(); i++) {
            if (!Character.isDigit(day.charAt(i))) {
                return value;
            }
        }

        return value.substring(separator + 2).trim();
    }

    private static String canonicalText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static void announceSemanticThroughHerald(
            ServerLevel level,
            CapitalRecord capital,
            CapitalChronicleEntry entry
    ) {
        if (level == null || capital == null || entry == null || capital.getHerald() == null) {
            return;
        }

        String signature = entry.heraldKey() + ':' + entry.dedupeKey();
        if (!reserveHeraldAnnouncement(level, capital, signature)) {
            return;
        }

        Component announcement = entry.renderHeraldVariant(1 + level.random.nextInt(3));
        if (announcement == null) {
            return;
        }

        Component speakerName = CapitalHeraldService.resolveHeraldSpeakerName(level, capital);
        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                capital,
                Component.translatable(
                        "mcacapitals.herald.spoken",
                        speakerName,
                        announcement
                )
        );
    }

    private static void announceLegacyThroughHerald(
            ServerLevel level,
            CapitalRecord capital,
            String entry
    ) {
        if (level == null || capital == null || entry == null || entry.isBlank() || capital.getHerald() == null) {
            return;
        }

        String signature = "legacy:" + entry.trim();
        if (!reserveHeraldAnnouncement(level, capital, signature)) {
            return;
        }

        Component speakerName = CapitalHeraldService.resolveHeraldSpeakerName(level, capital);
        CapitalPlayerNotificationService.notifyPlayersInCapital(
                level,
                capital,
                Component.translatable(
                        "mcacapitals.herald.spoken",
                        speakerName,
                        Component.literal(entry.trim())
                )
        );
    }

    private static boolean reserveHeraldAnnouncement(
            ServerLevel level,
            CapitalRecord capital,
            String signature
    ) {
        UUID capitalId = capital.getCapitalId();
        if (capitalId == null) {
            return true;
        }

        long gameTime = level.getGameTime();
        HeraldAnnouncement previous = LAST_HERALD_ANNOUNCEMENTS.get(capitalId);
        if (previous != null
                && previous.gameTime() == gameTime
                && previous.signature().equals(signature)) {
            return false;
        }

        LAST_HERALD_ANNOUNCEMENTS.put(
                capitalId,
                new HeraldAnnouncement(gameTime, signature)
        );
        return true;
    }

    public static void bindChronicleItem(ServerLevel level, CapitalRecord capital, ItemStack stack) {
        if (level == null || capital == null || stack == null) {
            return;
        }

        String capitalName = resolveChronicleCapitalName(level, capital);
        Component capitalNameComponent = chronicleCapitalNameComponent(capitalName);

        ModItemStackData.updateCustomData(stack, tag -> {
            tag.putString(ModDataKeys.CAPITAL_ID, capital.getCapitalId().toString());
            tag.putInt(ModDataKeys.VILLAGE_ID, capital.getVillageId() == null ? -1 : capital.getVillageId());
            tag.putString(ModDataKeys.VILLAGE_NAME, capitalName);
        });
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.translatable(
                        "mcacapitals.chronicle.item.bound_name",
                        capitalNameComponent
                )
        );
    }

    public static void writeChronicleBook(ServerLevel level, CapitalRecord capital, ItemStack stack) {
        if (level == null || capital == null || stack == null) {
            return;
        }

        List<Component> pages = createPages(level, capital);
        String capitalName = resolveChronicleCapitalName(level, capital);

        List<Filterable<Component>> writtenPages = new ArrayList<>();
        ListTag pageList = new ListTag();
        int count = 0;
        for (Component page : pages) {
            if (count >= MAX_PAGES) {
                break;
            }
            writtenPages.add(Filterable.passThrough(page));
            pageList.add(StringTag.valueOf(Component.Serializer.toJson(page, level.registryAccess())));
            count++;
        }

        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(capitalName),
                "",
                0,
                writtenPages,
                true
        ));

        ModItemStackData.updateCustomData(stack, tag -> {
            tag.putString(ModDataKeys.BOOK_TITLE, "mcacapitals.chronicle.book.title");
            tag.putString(ModDataKeys.BOOK_AUTHOR, "mcacapitals.chronicle.book.author");
            tag.putBoolean(ModDataKeys.BOOK_RESOLVED, true);
            tag.putInt(ModDataKeys.BOOK_GENERATION, 0);
            tag.put(ModDataKeys.BOOK_PAGES, pageList);
        });

        bindChronicleItem(level, capital, stack);
    }

    private static List<Component> createPages(ServerLevel level, CapitalRecord capital) {
        List<Component> pages = new ArrayList<>();
        Component capitalName = chronicleCapitalNameComponent(resolveChronicleCapitalName(level, capital));

        pages.add(Component.translatable(
                "mcacapitals.chronicle.book.introduction",
                capitalName
        ));

        Component current = Component.empty();
        int currentLength = 0;
        for (String storedEntry : capital.getChronicleEntries()) {
            Component line = renderStoredEntry(storedEntry);
            int lineLength = line.getString().length() + 2;
            if (currentLength > 0 && currentLength + lineLength > CHARS_PER_PAGE) {
                pages.add(current);
                current = Component.empty();
                currentLength = 0;
            }

            if (currentLength > 0) {
                current = current.copy().append("\n\n");
            }
            current = current.copy().append(line);
            currentLength += lineLength;
        }

        if (currentLength > 0) {
            pages.add(current);
        }

        return pages;
    }

    private static String resolveChronicleCapitalName(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return "";
        }

        String capitalName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        if (capitalName == null
                || capitalName.isBlank()
                || "Unknown Village".equals(capitalName)
                || "Unknown Capital".equals(capitalName)) {
            return "";
        }
        return capitalName;
    }

    private static Component chronicleCapitalNameComponent(String capitalName) {
        return capitalName == null || capitalName.isBlank()
                ? Component.translatable("mcacapitals.chronicle.unknown_capital")
                : Component.literal(capitalName);
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }

    private record HeraldAnnouncement(long gameTime, String signature) {
    }

    public static void clearRuntimeState() {
        LAST_HERALD_ANNOUNCEMENTS.clear();
    }

}
