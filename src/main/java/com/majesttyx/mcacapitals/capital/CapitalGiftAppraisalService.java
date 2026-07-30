package com.majesttyx.mcacapitals.capital;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.interaction.gifts.GiftType;
import net.conczin.mca.resources.data.Analysis;
import net.conczin.mca.resources.data.SerializablePair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CapitalGiftAppraisalService {

    private static final Set<String> ALLOWED_MCA_FACTORS = Set.of(
            "base",
            "age_group",
            "personality",
            "profession",
            "trait"
    );

    private static final Field GIFT_ITEMS_FIELD = findField("items");
    private static final Field GIFT_TAGS_FIELD = findField("tags");

    private CapitalGiftAppraisalService() {
    }

    public static GiftAppraisal appraise(
            ServerPlayer sender,
            CapitalRecord targetCapital,
            List<ItemStack> contents
    ) {
        if (sender == null
                || targetCapital == null
                || contents == null
                || contents.isEmpty()) {
            return new GiftAppraisal(
                    0,
                    "Trivial or confusing",
                    0
            );
        }

        ServerLevel level = sender.serverLevel();

        VillagerEntityMCA receivingSovereign =
                resolveNpcSovereign(
                        level,
                        targetCapital
                );

        int rawValue = 0;

        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            int quantityFactor =
                    quantityFactor(stack.getCount());

            int diplomaticOverride =
                    getDiplomaticOverride(stack);

            if (diplomaticOverride != 0) {
                rawValue += diplomaticOverride
                        * quantityFactor;
                continue;
            }

            int mcaValue = getMcaGiftValue(
                    receivingSovereign,
                    stack,
                    sender
            );

            rawValue += mcaValue * quantityFactor;
        }

        if (rawValue <= -20) {
            return new GiftAppraisal(
                    -25,
                    "Grave insult",
                    rawValue
            );
        }

        if (rawValue <= -8) {
            return new GiftAppraisal(
                    -15,
                    "Offensive or threatening",
                    rawValue
            );
        }

        if (rawValue < 0) {
            return new GiftAppraisal(
                    -5,
                    "Disappointing",
                    rawValue
            );
        }

        if (rawValue < 10) {
            return new GiftAppraisal(
                    0,
                    "Trivial or confusing",
                    rawValue
            );
        }

        if (rawValue < 35) {
            return new GiftAppraisal(
                    5,
                    "Respectable",
                    rawValue
            );
        }

        if (rawValue < 90) {
            return new GiftAppraisal(
                    10,
                    "Generous",
                    rawValue
            );
        }

        return new GiftAppraisal(
                15,
                "Exceptional",
                rawValue
        );
    }

    private static VillagerEntityMCA resolveNpcSovereign(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getSovereign() == null) {
            return null;
        }

        Entity entity =
                level.getEntity(capital.getSovereign());

        return entity instanceof VillagerEntityMCA villager
                ? villager
                : null;
    }

    private static int getMcaGiftValue(
            VillagerEntityMCA receivingSovereign,
            ItemStack stack,
            ServerPlayer sender
    ) {
        int best = 0;

        for (GiftType giftType :
                GiftType.allMatching(stack).toList()) {
            int value = receivingSovereign == null
                    ? getBaseGiftValue(
                    giftType,
                    stack
            )
                    : getAllowedMcaValue(
                    giftType,
                    receivingSovereign,
                    stack,
                    sender
            );

            best = Math.max(best, value);
        }

        return best;
    }

    private static int getAllowedMcaValue(
            GiftType giftType,
            VillagerEntityMCA receivingSovereign,
            ItemStack stack,
            ServerPlayer sender
    ) {
        try {
            Analysis analysis =
                    giftType.getSatisfactionFor(
                            receivingSovereign,
                            stack,
                            sender
                    );

            int total = 0;

            for (SerializablePair<String, Integer> summand :
                    analysis.getSummands()) {
                if (summand == null
                        || summand.left() == null
                        || summand.right() == null
                        || !ALLOWED_MCA_FACTORS.contains(
                        summand.left()
                )) {
                    continue;
                }

                total += summand.right();
            }

            return total;
        } catch (Throwable ignored) {
            return getBaseGiftValue(
                    giftType,
                    stack
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static int getBaseGiftValue(
            GiftType giftType,
            ItemStack stack
    ) {
        if (giftType == null
                || stack == null
                || stack.isEmpty()) {
            return 0;
        }

        try {
            if (GIFT_ITEMS_FIELD != null) {
                Map<Item, Integer> items =
                        (Map<Item, Integer>)
                                GIFT_ITEMS_FIELD.get(
                                        giftType
                                );

                Integer direct =
                        items.get(stack.getItem());

                if (direct != null) {
                    return direct;
                }
            }

            if (GIFT_TAGS_FIELD != null) {
                Map<TagKey<Item>, Integer> tags =
                        (Map<TagKey<Item>, Integer>)
                                GIFT_TAGS_FIELD.get(
                                        giftType
                                );

                for (Map.Entry<
                        TagKey<Item>,
                        Integer
                        > entry : tags.entrySet()) {
                    if (stack.is(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }
        } catch (Throwable ignored) {
            return 0;
        }

        return 0;
    }

    private static int getDiplomaticOverride(
            ItemStack stack
    ) {
        if (stack.is(Items.ROTTEN_FLESH)) {
            return -12;
        }

        if (stack.is(Items.POISONOUS_POTATO)) {
            return -8;
        }

        if (stack.is(Items.SPIDER_EYE)) {
            return -8;
        }

        if (stack.is(Items.TNT)) {
            return -12;
        }

        if (stack.is(Items.FIRE_CHARGE)) {
            return -7;
        }

        if (stack.is(Items.DEAD_BUSH)) {
            return -4;
        }

        if (stack.is(Items.DIRT)) {
            return -2;
        }

        return 0;
    }

    private static int quantityFactor(int count) {
        int safeCount = Math.max(1, count);

        return Math.min(
                6,
                1 + (
                        31
                                - Integer.numberOfLeadingZeros(
                                safeCount
                        )
                )
        );
    }

    private static Field findField(String name) {
        try {
            Field field =
                    GiftType.class.getDeclaredField(name);

            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public record GiftAppraisal(
            int relationshipDelta,
            String description,
            int rawValue
    ) {
    }
}