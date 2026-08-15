package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import forge.net.mca.entity.VillagerEntityMCA;
import forge.net.mca.entity.ai.MoveState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CapitalNameTagHandler {

    private static final double
            MAX_NAME_TAG_DISTANCE_SQR =
            4096.0D;


    private CapitalNameTagHandler() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        if (!shouldRenderMcaNameTags(entity)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (renderCustomNameTag(
                entity,
                event.getOriginalContent(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                0.0F
        )) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean shouldRenderMcaNameTags(Entity entity) {
        try {
            Class<?> configClass = Class.forName("forge.net.mca.Config");
            Method getInstance = configClass.getMethod("getInstance");
            Object config = getInstance.invoke(null);
            if (config == null) {
                return true;
            }

            Field showNameTags = config.getClass().getField("showNameTags");
            Object showNameTagsValue = showNameTags.get(config);
            if (showNameTagsValue instanceof Boolean booleanValue && !booleanValue) {
                return false;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return true;
            }

            Field nameTagDistance = config.getClass().getField("nameTagDistance");
            Object nameTagDistanceValue = nameTagDistance.get(config);
            if (!(nameTagDistanceValue instanceof Number numberValue)) {
                return true;
            }

            double distance = numberValue.doubleValue();
            return minecraft.player.distanceToSqr(entity) < distance * distance;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean shouldUseCustomNameTag(
            Entity entity
    ) {
        if (entity == null
                || !MCAIntegrationBridge
                .isMCAVillagerEntity(entity)) {
            return false;
        }

        VillagerIdentityClientCache
                .ClientVillagerIdentity identity =
                VillagerIdentityClientCache
                        .get(
                                entity.getUUID()
                        );

        if (identity == null) {
            return false;
        }

        String originalName = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getName().getString();

        return !buildLines(
                originalName,
                identity,
                resolveMcaStatus(entity)
        ).isEmpty();
    }

    public static boolean renderCustomNameTag(
            Entity entity,
            Component originalContent,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick
    ) {
        if (entity == null
                || originalContent == null
                || poseStack == null
                || bufferSource == null) {
            return false;
        }

        if (!MCAIntegrationBridge
                .isMCAVillagerEntity(entity)) {
            return false;
        }

        if (!isWithinRenderDistance(
                entity
        )) {
            return false;
        }

        if (!canPlayerSee(
                entity
        )) {
            return false;
        }

        UUID villagerId =
                entity.getUUID();

        VillagerIdentityClientCache
                .ClientVillagerIdentity identity =
                VillagerIdentityClientCache
                        .get(
                                villagerId
                        );

        if (identity == null) {
            return false;
        }

        String originalName = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getName().getString();

        List<Component> lines =
                buildLines(
                        originalName,
                        identity,
                        resolveMcaStatus(entity)
                );

        if (lines.isEmpty()) {
            return false;
        }

        renderLayeredNameTag(
                entity,
                poseStack,
                bufferSource,
                packedLight,
                partialTick,
                lines
        );

        return true;
    }

    private static boolean isWithinRenderDistance(
            Entity entity
    ) {
        return shouldRenderMcaNameTags(entity);
    }

    private static boolean canPlayerSee(
            Entity entity
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft == null
                || minecraft.player == null) {
            return true;
        }

        return !entity.isInvisibleTo(
                minecraft.player
        );
    }

    private static List<Component> buildLines(
            String originalName,
            VillagerIdentityClientCache.ClientVillagerIdentity identity,
            McaStatus statusLine
    ) {
        String cleanedOriginalName = normalizeSpaces(originalName);

        CapitalTitleResolver.ResolvedTitleId titleId = identity.resolvedTitleId();
        Component title = identity.displayTitle() == null
                ? Component.empty()
                : identity.displayTitle();
        Component orderLine = identity.royalGuardOrderLine() == null
                ? Component.empty()
                : identity.royalGuardOrderLine();
        Component courtOfficeLine = visibleCourtOfficeLine(identity);

        String baseName = identity.baseName() == null
                ? ""
                : identity.baseName().trim();
        if (baseName.isBlank()) {
            baseName = cleanedOriginalName;
        }

        String surname = identity.currentSurname() == null
                ? ""
                : identity.currentSurname().trim();
        String fullNameText = appendSurname(baseName, surname);
        Component fullName = Component.literal(fullNameText);

        List<Component> lines = new ArrayList<>();

        if (!orderLine.getString().isBlank()) {
            if (titleId == CapitalTitleResolver.ResolvedTitleId.ROYAL_GUARD) {
                lines.add(titledName(title, fullName));
            } else if (!fullNameText.isBlank()) {
                lines.add(fullName);
            }

            lines.add(orderLine);
            addCourtOfficeLineIfPresent(lines, courtOfficeLine);
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if (!identity.hasTitle()) {
            if (!fullNameText.isBlank()) {
                lines.add(fullName);
            }

            addCourtOfficeLineIfPresent(lines, courtOfficeLine);
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if (titleId == CapitalTitleResolver.ResolvedTitleId.LORD
                || titleId == CapitalTitleResolver.ResolvedTitleId.KNIGHT
                || titleId == CapitalTitleResolver.ResolvedTitleId.ROYAL_GUARD) {
            lines.add(titledName(title, fullName));
            addCourtOfficeLineIfPresent(lines, courtOfficeLine);
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if (isCourtOfficeNameFirst(titleId)) {
            if (!fullNameText.isBlank()) {
                lines.add(fullName);
            }
            lines.add(title);
            addCourtOfficeLineIfPresent(lines, courtOfficeLine);
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if (isInlineTitleName(titleId)) {
            lines.add(titledName(title, fullName));
            addCourtOfficeLineIfPresent(lines, courtOfficeLine);
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        lines.add(title);
        if (!fullNameText.isBlank()) {
            lines.add(fullName);
        }
        addCourtOfficeLineIfPresent(lines, courtOfficeLine);
        addStatusLineIfPresent(lines, statusLine);
        return lines;
    }

    private static Component titledName(Component title, Component name) {
        return Component.translatable(
                "mcacapitals.dynamic.name.titled",
                title,
                name
        );
    }

    private static Component visibleCourtOfficeLine(
            VillagerIdentityClientCache.ClientVillagerIdentity identity
    ) {
        if (identity.resolvedCourtOfficeId()
                != CapitalTitleResolver.SecondaryOfficeId.MASTER_OF_LAWS) {
            return Component.empty();
        }

        return identity.courtOfficeLine() == null
                ? Component.empty()
                : identity.courtOfficeLine();
    }

    private static void addCourtOfficeLineIfPresent(List<Component> lines, Component courtOfficeLine) {
        if (courtOfficeLine != null && !courtOfficeLine.getString().isBlank()) {
            lines.add(courtOfficeLine);
        }
    }

    private static void addStatusLineIfPresent(List<Component> lines, McaStatus statusLine) {
        if (statusLine == McaStatus.STAYING) {
            lines.add(Component.translatable("mcacapitals.system.name_tag.staying"));
        } else if (statusLine == McaStatus.FOLLOWING) {
            lines.add(Component.translatable("mcacapitals.system.name_tag.following"));
        }
    }

    private static boolean isCourtOfficeNameFirst(CapitalTitleResolver.ResolvedTitleId titleId) {
        return titleId == CapitalTitleResolver.ResolvedTitleId.HEIR_APPARENT
                || titleId == CapitalTitleResolver.ResolvedTitleId.HAND;
    }

    private static boolean isInlineTitleName(CapitalTitleResolver.ResolvedTitleId titleId) {
        return titleId == CapitalTitleResolver.ResolvedTitleId.DUKE
                || titleId == CapitalTitleResolver.ResolvedTitleId.ROYAL_CHILD
                || titleId == CapitalTitleResolver.ResolvedTitleId.SOVEREIGN;
    }

    private static McaStatus resolveMcaStatus(Entity entity) {
        if (!(entity instanceof VillagerEntityMCA villager)
                || villager.getVillagerBrain() == null) {
            return McaStatus.NONE;
        }

        MoveState moveState = villager.getVillagerBrain().getMoveState();
        if (moveState == MoveState.STAY) {
            return McaStatus.STAYING;
        }
        if (moveState == MoveState.FOLLOW) {
            return McaStatus.FOLLOWING;
        }
        return McaStatus.NONE;
    }

    private static String appendSurname(String baseName, String surname) {
        String name = baseName == null ? "" : baseName.trim();
        String lastName = surname == null ? "" : surname.trim();

        if (name.isBlank()) {
            return lastName;
        }

        if (lastName.isBlank()) {
            return name;
        }

        if (name.endsWith(" " + lastName) || name.equals(lastName)) {
            return name;
        }

        return name + " " + lastName;
    }

    private static String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private enum McaStatus {
        NONE,
        STAYING,
        FOLLOWING
    }

    private static void renderLayeredNameTag(
            Entity entity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick,
            List<Component> lines
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft == null
                || minecraft
                .getEntityRenderDispatcher()
                == null
                || minecraft.font == null) {
            return;
        }

        Font font =
                minecraft.font;

        boolean normalRender =
                !entity.isDiscrete();

        poseStack.pushPose();

        // Use the Minecraft 1.20.1 vanilla name-tag anchor at the entity bounding-box height.
        poseStack.translate(
                0.0D,
                entity.getBbHeight() + 0.5D,
                0.0D
        );

        poseStack.mulPose(
                minecraft
                        .getEntityRenderDispatcher()
                        .cameraOrientation()
        );

        poseStack.scale(
                -0.025F,
                -0.025F,
                0.025F
        );

        Matrix4f matrix =
                poseStack.last()
                        .pose();

        float opacity =
                minecraft.options
                        .getBackgroundOpacity(
                                0.25F
                        );

        int backgroundColor =
                (int) (
                        opacity
                                * 255.0F
                ) << 24;

        int totalHeight =
                (lines.size() - 1)
                        * 10;

        int startY =
                -totalHeight;

        for (int i = 0;
             i < lines.size();
             i++) {

            Component line =
                    lines.get(i);

            int y =
                    startY
                            + (i * 10);

            float x =
                    -font.width(line)
                            / 2.0F;

            font.drawInBatch(
                    line,
                    x,
                    y,
                    553648127,
                    false,
                    matrix,
                    bufferSource,
                    normalRender
                            ? Font.DisplayMode.SEE_THROUGH
                            : Font.DisplayMode.NORMAL,
                    backgroundColor,
                    packedLight
            );

            if (normalRender) {
                font.drawInBatch(
                        line,
                        x,
                        y,
                        -1,
                        false,
                        matrix,
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        packedLight
                );
            }
        }

        poseStack.popPose();
    }
}