package com.majesttyx.mcacapitals.client;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CapitalNameTagHandler {

    private static final List<String> KNOWN_TITLES = List.of(
            "Hand of the Queen",
            "Hand of the King",
            "Lord Commander",
            "Princess Consort",
            "Prince Consort",
            "Dowager Duchess",
            "Dowager Princess",
            "Dowager Prince",
            "Dowager Queen",
            "Dowager Duke",
            "Dowager King",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Grand Maester",
            "Court Herald",
            "Crown Princess",
            "Crown Prince",
            "High Queen",
            "High King",
            "Princess",
            "Prince",
            "Duchess",
            "Maester",
            "Queen",
            "Duke",
            "King",
            "Lady",
            "Lord",
            "Dame",
            "Sir"
    );

    private CapitalNameTagHandler() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        UUID villagerId = entity.getUUID();
        VillagerIdentityClientCache.ClientVillagerIdentity identity = VillagerIdentityClientCache.get(villagerId);
        if (identity == null) {
            return;
        }

        List<Component> lines = buildLines(event.getOriginalContent().getString(), identity);
        if (lines.isEmpty()) {
            return;
        }

        event.setResult(Event.Result.DENY);
        renderLayeredNameTag(event, lines);
    }

    private static List<Component> buildLines(String originalName, VillagerIdentityClientCache.ClientVillagerIdentity identity) {
        String cleanedOriginalName = normalizeSpaces(originalName);

        String statusLine = detectMcaStatusLine(cleanedOriginalName);
        if (!statusLine.isBlank()) {
            cleanedOriginalName = removeMcaStatusLine(cleanedOriginalName);
        }

        String orderLine = normalizeRoyalGuardOrderLine(identity.royalGuardOrderLine());
        if (orderLine.isBlank()) {
            orderLine = detectRoyalGuardOrderLine(cleanedOriginalName);
        }

        if (!orderLine.isBlank()) {
            cleanedOriginalName = removeRoyalGuardOrderLine(cleanedOriginalName);
        }

        String title = identity.displayTitle() == null ? "" : identity.displayTitle().trim();
        if (title.isBlank() || "None".equals(title) || "Commoner".equals(title)) {
            title = detectTitleFromOriginalName(cleanedOriginalName);
        }

        String baseName = stripTitle(cleanedOriginalName, title);
        String surname = identity.currentSurname() == null ? "" : identity.currentSurname().trim();
        String fullName = appendSurname(baseName, surname);

        List<Component> lines = new ArrayList<>();

        if (!orderLine.isBlank()) {
            String guardTitle = title;
            if (!"Sir".equals(guardTitle) && !"Dame".equals(guardTitle)) {
                guardTitle = detectTitleFromOriginalName(cleanedOriginalName);
            }

            String guardName = stripTitle(cleanedOriginalName, guardTitle);
            guardName = appendSurname(guardName, surname);

            if ("Sir".equals(guardTitle) || "Dame".equals(guardTitle)) {
                lines.add(Component.literal(guardTitle + " " + guardName));
            } else {
                lines.add(Component.literal(guardName));
            }

            lines.add(Component.literal(orderLine));
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if (title.isBlank() || "None".equals(title) || "Commoner".equals(title)) {
            if (!fullName.isBlank()) {
                lines.add(Component.literal(fullName));
            }
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if ("Lady".equals(title) || "Lord".equals(title) || "Dame".equals(title) || "Sir".equals(title)) {
            lines.add(Component.literal(title + " " + fullName));
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if (isCourtOfficeNameFirst(title)) {
            lines.add(Component.literal(fullName));
            lines.add(Component.literal(title));
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        if (isInlineTitleName(title)) {
            lines.add(Component.literal(title + " " + fullName));
            addStatusLineIfPresent(lines, statusLine);
            return lines;
        }

        lines.add(Component.literal(title));
        lines.add(Component.literal(fullName));
        addStatusLineIfPresent(lines, statusLine);
        return lines;
    }

    private static void addStatusLineIfPresent(List<Component> lines, String statusLine) {
        if (statusLine != null && !statusLine.isBlank()) {
            lines.add(Component.literal(statusLine));
        }
    }

    private static boolean isCourtOfficeNameFirst(String title) {
        return "Heir Apparent".equals(title)
                || "Hand of the Queen".equals(title)
                || "Hand of the King".equals(title);
    }

    private static boolean isInlineTitleName(String title) {
        return "Duke".equals(title)
                || "Duchess".equals(title)
                || "Prince".equals(title)
                || "Princess".equals(title)
                || "King".equals(title)
                || "Queen".equals(title);
    }

    private static String detectTitleFromOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }

        for (String title : KNOWN_TITLES) {
            if (originalName.equals(title) || originalName.startsWith(title + " ")) {
                return title;
            }
        }

        return "";
    }

    private static String detectMcaStatusLine(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }

        String normalized = normalizeSpaces(originalName);
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.contains("(staying)")) {
            return "Staying";
        }

        if (lower.contains("(following)")) {
            return "Following";
        }

        if (lower.endsWith(" staying")) {
            return "Staying";
        }

        if (lower.endsWith(" following")) {
            return "Following";
        }

        return "";
    }

    private static String removeMcaStatusLine(String originalName) {
        String normalized = normalizeSpaces(originalName);
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.contains("(staying)")) {
            normalized = normalized.replaceAll("(?i)\\s*\\(\\s*staying\\s*\\)", "");
            return normalizeSpaces(normalized);
        }

        if (lower.contains("(following)")) {
            normalized = normalized.replaceAll("(?i)\\s*\\(\\s*following\\s*\\)", "");
            return normalizeSpaces(normalized);
        }

        if (lower.endsWith(" staying")) {
            return normalized.substring(0, normalized.length() - " staying".length()).trim();
        }

        if (lower.endsWith(" following")) {
            return normalized.substring(0, normalized.length() - " following".length()).trim();
        }

        return normalized;
    }

    private static String normalizeRoyalGuardOrderLine(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lower = normalizeSpaces(value).toLowerCase(Locale.ROOT);

        if (lower.contains("queensguard")) {
            return "Of the Queensguard";
        }

        if (lower.contains("kingsguard")) {
            return "Of the Kingsguard";
        }

        return "";
    }

    private static String detectRoyalGuardOrderLine(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }

        String lower = normalizeSpaces(originalName).toLowerCase(Locale.ROOT);

        if (lower.contains("of the queensguard")) {
            return "Of the Queensguard";
        }

        if (lower.contains("of the kingsguard")) {
            return "Of the Kingsguard";
        }

        return "";
    }

    private static String removeRoyalGuardOrderLine(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }

        String normalized = normalizeSpaces(originalName);
        normalized = normalized.replaceAll("(?i)\\bof\\s+the\\s+queensguard\\b", "");
        normalized = normalized.replaceAll("(?i)\\bof\\s+the\\s+kingsguard\\b", "");
        return normalizeSpaces(normalized);
    }

    private static String stripTitle(String originalName, String title) {
        String name = originalName == null ? "" : originalName.trim();
        if (name.isBlank() || title == null || title.isBlank()) {
            return name;
        }

        if (name.equals(title)) {
            return "";
        }

        if (name.startsWith(title + " ")) {
            return name.substring(title.length()).trim();
        }

        return name;
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

    private static void renderLayeredNameTag(RenderNameTagEvent event, List<Component> lines) {
        Entity entity = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.getEntityRenderDispatcher() == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        Font font = event.getEntityRenderer().getFont();
        boolean normalRender = !entity.isDiscrete();

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.5D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

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
                    normalRender ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
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