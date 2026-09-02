package com.majesttyx.mcacapitals.house;

import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.data.CapitalHouseDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BookOfHousesService {

    private static final int APPROX_CHARS_PER_PAGE = 600;
    private static final int MAX_PAGES = 100;
    private static final String BOOK_KIND_KEY =
            "mcacapitals_book_kind";
    private static final String BOOK_KIND_HOUSES =
            "houses";

    private BookOfHousesService() {
    }

    public static void bindBookItem(
            ServerLevel level,
            CapitalRecord capital,
            ItemStack stack
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || stack == null) {
            return;
        }

        String capitalName = resolveCapitalName(level, capital);

        ModItemStackData.updateCustomData(stack, tag -> {
            tag.putString(
                    ModDataKeys.CAPITAL_ID,
                    capital.getCapitalId().toString()
            );
            tag.putInt(
                    ModDataKeys.VILLAGE_ID,
                    capital.getVillageId() == null
                            ? -1
                            : capital.getVillageId()
            );
            tag.putString(
                    ModDataKeys.VILLAGE_NAME,
                    capitalName
            );
            tag.putString(
                    BOOK_KIND_KEY,
                    BOOK_KIND_HOUSES
            );
        });

        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.translatable(
                        "mcacapitals.book_of_houses.item.bound_name",
                        Component.literal(capitalName)
                )
        );
    }

    public static void writeBook(
            ServerLevel level,
            CapitalRecord capital,
            ItemStack stack
    ) {
        if (level == null
                || capital == null
                || stack == null) {
            return;
        }

        Set<UUID> residents =
                CapitalResidentScanner.scanResidents(
                        level,
                        capital.getCapitalId()
                );

        CapitalHouseRegistryService.synchronize(
                level,
                capital,
                residents
        );

        List<Component> pages = createPages(level, capital);
        List<Filterable<Component>> writtenPages = new ArrayList<>();

        int count = 0;
        for (Component page : pages) {
            if (count >= MAX_PAGES) {
                break;
            }
            writtenPages.add(Filterable.passThrough(page));
            count++;
        }

        String localizedTitle = Component.translatable(
                "item.mcacapitals.book_of_houses"
        ).getString();
        String title = localizedTitle.length() <= 32
                ? localizedTitle
                : localizedTitle.substring(0, 32);

        stack.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(title),
                        "",
                        0,
                        writtenPages,
                        true
                )
        );

        bindBookItem(level, capital, stack);
    }

    private static List<Component> createPages(
            ServerLevel level,
            CapitalRecord capital
    ) {
        List<Component> pages = new ArrayList<>();

        String capitalName = resolveCapitalName(level, capital);

        Component introduction = Component.empty()
                .append(
                        Component.translatable("mcacapitals.book_of_houses.book.title")
                )
                .append("\n\n")
                .append(Component.literal(capitalName))
                .append("\n\n")
                .append(
                        Component.translatable("mcacapitals.book_of_houses.book.introduction")
                );

        pages.add(introduction);
        pages.add(Component.literal(" "));

        List<CapitalHouseRecord> houses =
                new ArrayList<>(
                        CapitalHouseDataAccess.getHouses(
                                level,
                                capital.getCapitalId()
                        )
                );

        houses.sort(
                Comparator
                        .comparingInt(
                                (CapitalHouseRecord house) ->
                                        tierSort(house.getTier())
                        )
                        .thenComparing(
                                CapitalHouseRecord::getHouseName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        if (houses.isEmpty()) {
            pages.add(
                    Component.translatable("mcacapitals.book_of_houses.book.none")
            );
            return pages;
        }

        for (int houseIndex = 0;
             houseIndex < houses.size();
             houseIndex++) {
            if (houseIndex > 0) {
                pages.add(
                        Component.literal("────────────")
                );
                pages.add(
                        Component.literal(" ")
                );
            }

            CapitalHouseRecord house =
                    houses.get(houseIndex);

            pages.addAll(
                    createHousePages(
                            level,
                            capital,
                            house
                    )
            );
        }

        return pages;
    }

    private static List<Component> createHousePages(
            ServerLevel level,
            CapitalRecord capital,
            CapitalHouseRecord house
    ) {
        List<Component> pages = new ArrayList<>();
        List<Component> lines = new ArrayList<>();

        lines.add(
                Component.translatable("mcacapitals.book_of_houses.house.name", house.getHouseName())
                        
        );
        lines.add(tierComponent(house.getTier()));

        if (!house.isActive()) {
            lines.add(
                    Component.translatable("mcacapitals.book_of_houses.house.extinct")
            );
        }

        if (house.getParentHouseId() != null) {
            CapitalHouseRecord parent =
                    CapitalHouseDataAccess.getHouse(
                            level,
                            capital.getCapitalId(),
                            house.getParentHouseId()
                    );
            if (parent != null) {
                lines.add(
                        Component.translatable("mcacapitals.book_of_houses.house.branch_of", parent.getHouseName()
                        )
                );
            }
        }

        if (house.getFounderId() != null) {
            lines.add(
                    Component.translatable("mcacapitals.book_of_houses.house.founded_by", resolveName(
                                    level,
                                    capital,
                                    house.getFounderId()
                            )
                    )
            );
        }

        if (house.getHeadId() != null) {
            lines.add(
                    Component.translatable("mcacapitals.book_of_houses.house.head", resolveName(
                                    level,
                                    capital,
                                    house.getHeadId()
                            )
                    )
            );
        }

        if (!house.getCurrentMembers().isEmpty()) {
            lines.add(Component.empty());
            lines.add(
                    Component.translatable("mcacapitals.book_of_houses.house.current_members")
            );
            appendMembers(
                    lines,
                    level,
                    capital,
                    house.getCurrentMembers()
            );
        }

        if (!house.getFormerMembers().isEmpty()) {
            lines.add(Component.empty());
            lines.add(
                    Component.translatable("mcacapitals.book_of_houses.house.former_members")
            );
            appendMembers(
                    lines,
                    level,
                    capital,
                    house.getFormerMembers()
            );
        }

        List<CapitalHouseHistoryEntry> importantHistory =
                house.getHistory()
                        .stream()
                        .filter(BookOfHousesService::isVisibleHistory)
                        .toList();

        if (!importantHistory.isEmpty()) {
            lines.add(Component.empty());
            lines.add(
                    Component.translatable("mcacapitals.book_of_houses.house.history")
            );

            for (CapitalHouseHistoryEntry entry
                    : importantHistory) {
                Component rendered =
                        renderHistoryEntry(
                                level,
                                capital,
                                entry
                        );
                if (rendered != null) {
                    lines.add(rendered);
                }
            }
        }

        lines.add(Component.literal(" "));

        paginate(lines, pages);
        return pages;
    }

    private static void appendMembers(
            List<Component> lines,
            ServerLevel level,
            CapitalRecord capital,
            Collection<UUID> members
    ) {
        List<String> names = new ArrayList<>();
        for (UUID memberId : members) {
            names.add(resolveName(level, capital, memberId));
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);

        for (String name : names) {
            lines.add(Component.literal("• " + name));
        }
    }

    private static boolean isVisibleHistory(
            CapitalHouseHistoryEntry entry
    ) {
        if (entry == null || entry.type() == null) {
            return false;
        }

        return switch (entry.type()) {
            case FOUNDED,
                    MEMBER_ELEVATED,
                    MEMBER_DISINHERITED,
                    HEAD_CHANGED,
                    BRANCH_FOUNDED,
                    BECAME_ROYAL,
                    CEASED_ROYAL,
                    BECAME_EXTINCT,
                    RESTORED -> true;
            case MEMBER_JOINED,
                    MEMBER_LEFT -> false;
        };
    }

    private static Component renderHistoryEntry(
            ServerLevel level,
            CapitalRecord capital,
            CapitalHouseHistoryEntry entry
    ) {
        String subject = entry.subjectId() == null
                ? ""
                : resolveName(
                        level,
                        capital,
                        entry.subjectId()
                );

        return switch (entry.type()) {
            case FOUNDED ->
                    Component.translatable("mcacapitals.book_of_houses.history.founded", subject);

            case MEMBER_ELEVATED ->
                    Component.translatable("mcacapitals.book_of_houses.history.member_elevated", subject);

            case MEMBER_DISINHERITED ->
                    Component.translatable("mcacapitals.book_of_houses.history.member_disinherited", subject);

            case HEAD_CHANGED ->
                    Component.translatable("mcacapitals.book_of_houses.history.head_changed", subject);

            case BRANCH_FOUNDED ->
                    Component.translatable("mcacapitals.book_of_houses.history.branch_founded", subject);

            case BECAME_ROYAL ->
                    Component.translatable("mcacapitals.book_of_houses.history.became_royal", subject);

            case CEASED_ROYAL ->
                    Component.translatable("mcacapitals.book_of_houses.history.ceased_royal");

            case BECAME_EXTINCT ->
                    Component.translatable("mcacapitals.book_of_houses.history.became_extinct");

            case RESTORED ->
                    Component.translatable("mcacapitals.book_of_houses.history.restored");

            case MEMBER_JOINED,
                    MEMBER_LEFT -> null;
        };
    }

    private static Component tierComponent(
            CapitalHouseTier tier
    ) {
        if (tier == null) {
            tier = CapitalHouseTier.NOBLE;
        }

        return switch (tier) {
            case ROYAL ->
                    Component.translatable("mcacapitals.book_of_houses.tier.royal");

            case GREAT ->
                    Component.translatable("mcacapitals.book_of_houses.tier.great");

            case NOBLE ->
                    Component.translatable("mcacapitals.book_of_houses.tier.noble");
        };
    }

    private static int tierSort(CapitalHouseTier tier) {
        if (tier == CapitalHouseTier.ROYAL) {
            return 0;
        }
        if (tier == CapitalHouseTier.GREAT) {
            return 1;
        }
        return 2;
    }

    private static void paginate(
            List<Component> lines,
            List<Component> pages
    ) {
        Component current = Component.empty();
        int currentLength = 0;

        for (Component line : lines) {
            String plain = line == null
                    ? ""
                    : line.getString();

            int additional =
                    plain.length()
                            + (currentLength == 0 ? 0 : 1);

            if (currentLength > 0
                    && currentLength + additional
                    > APPROX_CHARS_PER_PAGE) {
                pages.add(current);
                current = Component.empty();
                currentLength = 0;
            }

            if (currentLength > 0) {
                current = current.copy().append("\n");
                currentLength++;
            }

            if (line != null) {
                current = current.copy().append(line);
            }
            currentLength += plain.length();
        }

        if (currentLength > 0) {
            pages.add(current);
        }
    }

    private static String resolveName(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (entityId == null) {
            return Component.translatable("mcacapitals.system.common.unknown").getString();
        }

        ServerPlayer player = level.getServer()
                .getPlayerList()
                .getPlayer(entityId);

        if (player != null) {
            return player.getName().getString();
        }

        return CapitalNameService.resolveDisplayName(
                level,
                capital,
                entityId
        );
    }

    private static String resolveCapitalName(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (capital.getVillageId() == null) {
            return Component.translatable("mcacapitals.system.common.unknown").getString();
        }

        String name =
                MCAIntegrationBridge.getVillageName(
                        level,
                        capital.getVillageId()
                );

        return name == null || name.isBlank()
                ? Component.translatable("mcacapitals.system.common.unnamed").getString()
                : name;
    }
}
