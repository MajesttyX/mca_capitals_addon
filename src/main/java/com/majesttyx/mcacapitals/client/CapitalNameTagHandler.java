package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.MCACapitals;
import net.conczin.mca.Config;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MoveState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = MCACapitals.MODID, value = Dist.CLIENT)
public final class CapitalNameTagHandler {

    private CapitalNameTagHandler() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        if (!shouldRenderMcaNameTags(entity)) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        UUID villagerId = entity.getUUID();
        VillagerIdentityClientCache.ClientVillagerIdentity identity = VillagerIdentityClientCache.get(villagerId);
        if (identity == null) {
            return;
        }

        String originalName = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getName().getString();
        List<Component> lines = buildLines(originalName, identity, resolveMcaStatus(entity));
        if (lines.isEmpty()) {
            return;
        }

        event.setCanRender(TriState.FALSE);
        renderLayeredNameTag(event, lines);
    }

    private static boolean shouldRenderMcaNameTags(Entity entity) {
        Config config = Config.getInstance();
        if (!config.showNameTags) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return true;
        }

        double distance = config.nameTagDistance;
        return minecraft.player.distanceToSqr(entity) < distance * distance;
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

    private static void renderLayeredNameTag(RenderNameTagEvent event, List<Component> lines) {
        Entity entity = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.getEntityRenderDispatcher() == null) {
            return;
        }

        double distance = minecraft.getEntityRenderDispatcher().distanceToSqr(entity);
        if (!ClientHooks.isNameplateInRenderDistance(entity, distance)) {
            return;
        }

        Vec3 attachment = entity.getAttachments().getNullable(
                EntityAttachment.NAME_TAG,
                0,
                entity.getViewYRot(event.getPartialTick())
        );

        if (attachment == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        Font font = event.getEntityRenderer().getFont();
        boolean normalRender = !entity.isDiscrete();

        poseStack.pushPose();
        poseStack.translate(attachment.x, attachment.y + 0.5D, attachment.z);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        float opacity = minecraft.options.getBackgroundOpacity(0.25F);
        int backgroundColor = (int) (opacity * 255.0F) << 24;

        int totalHeight = (lines.size() - 1) * 10;
        int startY = -totalHeight;

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int y = startY + (i * 10);
            float x = -font.width(line) / 2.0F;

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
                    event.getPackedLight()
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
                        event.getPackedLight()
                );
            }
        }

        poseStack.popPose();
    }
}