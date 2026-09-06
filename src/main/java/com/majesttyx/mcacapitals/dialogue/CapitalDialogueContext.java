package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorService;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.identity.VillagerIdentityData;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleRecord;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class CapitalDialogueContext {

    private final List<Component> arguments;

    private CapitalDialogueContext(List<Component> arguments) {
        this.arguments = List.copyOf(arguments);
    }

    static CapitalDialogueContext create(
            ServerLevel level,
            CapitalRecord capital,
            ServerPlayer player,
            Entity speaker
    ) {
        return createInternal(level, capital, null, player, speaker, player);
    }

    static CapitalDialogueContext createAmbient(
            ServerLevel level,
            CapitalRecord capital,
            Entity speaker,
            Entity listener
    ) {
        return createInternal(level, capital, null, null, speaker, listener);
    }

    static CapitalDialogueContext createAmbient(
            ServerLevel level,
            CapitalRecord capital,
            CapitalRecord foreignCapital,
            Entity speaker,
            Entity listener
    ) {
        return createInternal(level, capital, foreignCapital, null, speaker, listener);
    }

    private static CapitalDialogueContext createInternal(
            ServerLevel level,
            CapitalRecord capital,
            CapitalRecord foreignCapital,
            ServerPlayer player,
            Entity speaker,
            Entity listener
    ) {
        CapitalDialogueEventModels.ChronicleEvent latestEvent =
                CapitalDialogueChronicleLogic.findLatestNotableEvent(level, capital);

        UUID playerId = player == null ? null : player.getUUID();
        UUID speakerId = speaker == null ? null : speaker.getUUID();
        UUID listenerId = listener == null ? null : listener.getUUID();
        UUID sovereignId = effectiveSovereignId(capital);
        UUID consortId = effectiveConsortId(capital);
        UUID dowagerId = capital.getDowager();
        UUID heirId = capital.getHeir();
        UUID handId = effectiveHandId(level, capital);
        UUID commanderId = effectiveCommanderId(level, capital);
        UUID heraldId = capital.getHerald();
        UUID grandMaesterId = capital.getGrandMaester();
        UUID masterOfLawsId = capital.getMasterOfLaws();
        UUID ambassadorId = CapitalAmbassadorService.getAmbassador(level, capital);

        Component playerName = player == null
                ? Component.translatable("mcacapitals.dynamic.player")
                : literalOrFallback(
                        MCAIntegrationBridge.getPlayerDialogueName(player),
                        "mcacapitals.dynamic.player"
                );
        Component speakerName = resolveEntityName(level, capital, speakerId, "mcacapitals.dynamic.someone");
        Component listenerName = listenerId == null
                ? playerName
                : resolveEntityName(level, capital, listenerId, "mcacapitals.dynamic.someone");
        Component sovereignName = resolveEntityName(level, capital, sovereignId, "mcacapitals.dynamic.sovereign");
        Component consortName = resolveEntityName(level, capital, consortId, "mcacapitals.dynamic.consort");
        Component dowagerName = resolveEntityName(level, capital, dowagerId, "mcacapitals.dynamic.dowager");
        Component heirName = resolveEntityName(level, capital, heirId, "mcacapitals.dynamic.heir");
        Component handName = resolveEntityName(level, capital, handId, "mcacapitals.dynamic.hand");
        Component commanderName = resolveEntityName(level, capital, commanderId, "mcacapitals.dynamic.commander");
        Component heraldName = resolveEntityName(level, capital, heraldId, "mcacapitals.dynamic.herald");
        Component grandMaesterName = resolveEntityName(level, capital, grandMaesterId, "mcacapitals.dynamic.grand_maester");
        Component masterOfLawsName = resolveEntityName(level, capital, masterOfLawsId, "mcacapitals.dynamic.master_of_laws");
        Component ambassadorName = resolveEntityName(level, capital, ambassadorId, "mcacapitals.dynamic.ambassador");

        CapitalDialogueIdentityResolver.Identity playerIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, playerId, playerName);
        CapitalDialogueIdentityResolver.Identity speakerIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, speakerId, speakerName);
        CapitalDialogueIdentityResolver.Identity listenerIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, listenerId, listenerName);
        CapitalDialogueIdentityResolver.Identity sovereignIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, sovereignId, sovereignName);
        CapitalDialogueIdentityResolver.Identity consortIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, consortId, consortName);
        CapitalDialogueIdentityResolver.Identity dowagerIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, dowagerId, dowagerName);
        CapitalDialogueIdentityResolver.Identity heirIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, heirId, heirName);
        CapitalDialogueIdentityResolver.Identity handIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, handId, handName);
        CapitalDialogueIdentityResolver.Identity commanderIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, commanderId, commanderName);
        CapitalDialogueIdentityResolver.Identity heraldIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, heraldId, heraldName);
        CapitalDialogueIdentityResolver.Identity grandMaesterIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, grandMaesterId, grandMaesterName);
        CapitalDialogueIdentityResolver.Identity masterOfLawsIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, masterOfLawsId, masterOfLawsName);
        CapitalDialogueIdentityResolver.Identity ambassadorIdentity =
                CapitalDialogueIdentityResolver.resolve(level, capital, ambassadorId, ambassadorName);

        List<Component> arguments = new ArrayList<>(155);
        arguments.add(playerName);
        arguments.add(resolveCapitalName(level, capital));
        arguments.add(Component.literal(Integer.toString(safePopulation(level, capital))));
        arguments.add(Component.translatable(
                capital.isMourningActive()
                        ? "mcacapitals.dynamic.mourning.active"
                        : "mcacapitals.dynamic.mourning.ended"
        ));
        arguments.add(speakerName);
        arguments.add(speakerIdentity.title());
        arguments.add(sovereignName);
        arguments.add(sovereignIdentity.title());
        arguments.add(consortName);
        arguments.add(consortIdentity.title());
        arguments.add(heirName);
        arguments.add(heirIdentity.title());
        arguments.add(handName);
        arguments.add(handIdentity.title());
        arguments.add(commanderName);
        arguments.add(commanderIdentity.title());
        arguments.add(heraldName);
        arguments.add(grandMaesterName);
        arguments.add(Component.literal(Integer.toString(capital.getRoyalChildren().size())));
        arguments.add(Component.literal(Integer.toString(capital.getRoyalHousehold().size())));
        arguments.add(
                latestEvent == null
                        ? Component.translatable("mcacapitals.dynamic.recent_court_business")
                        : latestEvent.text()
        );
        arguments.add(resolveEventType(latestEvent));
        arguments.add(Component.literal(
                latestEvent == null
                        ? "0"
                        : Long.toString(Math.max(0L, currentDay(level) - latestEvent.day()))
        ));

        arguments.add(speakerIdentity.office());
        arguments.add(speakerIdentity.style());
        arguments.add(speakerIdentity.address());
        arguments.add(listenerName);
        arguments.add(listenerIdentity.title());
        arguments.add(listenerIdentity.office());
        arguments.add(listenerIdentity.style());
        arguments.add(listenerIdentity.address());
        arguments.add(playerIdentity.title());
        arguments.add(playerIdentity.office());
        arguments.add(playerIdentity.style());
        arguments.add(playerIdentity.address());
        arguments.add(sovereignIdentity.office());
        arguments.add(sovereignIdentity.style());
        arguments.add(sovereignIdentity.address());
        arguments.add(consortIdentity.office());
        arguments.add(consortIdentity.style());
        arguments.add(consortIdentity.address());
        arguments.add(dowagerName);
        arguments.add(dowagerIdentity.title());
        arguments.add(dowagerIdentity.office());
        arguments.add(dowagerIdentity.style());
        arguments.add(dowagerIdentity.address());
        arguments.add(heirIdentity.office());
        arguments.add(heirIdentity.style());
        arguments.add(heirIdentity.address());
        arguments.add(handIdentity.office());
        arguments.add(handIdentity.style());
        arguments.add(handIdentity.address());
        arguments.add(commanderIdentity.office());
        arguments.add(commanderIdentity.style());
        arguments.add(commanderIdentity.address());
        arguments.add(heraldIdentity.title());
        arguments.add(heraldIdentity.office());
        arguments.add(heraldIdentity.style());
        arguments.add(heraldIdentity.address());
        arguments.add(grandMaesterIdentity.title());
        arguments.add(grandMaesterIdentity.office());
        arguments.add(grandMaesterIdentity.style());
        arguments.add(grandMaesterIdentity.address());
        arguments.add(masterOfLawsName);
        arguments.add(masterOfLawsIdentity.title());
        arguments.add(masterOfLawsIdentity.office());
        arguments.add(masterOfLawsIdentity.style());
        arguments.add(masterOfLawsIdentity.address());
        arguments.add(ambassadorName);
        arguments.add(ambassadorIdentity.title());
        arguments.add(ambassadorIdentity.office());
        arguments.add(ambassadorIdentity.style());
        arguments.add(ambassadorIdentity.address());

        addPronouns(arguments, speakerIdentity);
        addPronouns(arguments, listenerIdentity);
        addPronouns(arguments, playerIdentity);
        addPronouns(arguments, sovereignIdentity);
        addPronouns(arguments, consortIdentity);
        addPronouns(arguments, dowagerIdentity);
        addPronouns(arguments, heirIdentity);

        arguments.add(resolveSurname(level, speakerId));
        arguments.add(resolveHouseName(level, speakerId));
        arguments.add(resolveSurname(level, listenerId));
        arguments.add(resolveHouseName(level, listenerId));
        arguments.add(resolveHouseName(level, sovereignId));
        arguments.add(resolveSpouseName(level, capital, speakerId));
        arguments.add(resolveSpouseName(level, capital, listenerId));
        arguments.add(Component.literal(Integer.toString(safeChildCount(level, speakerId))));
        arguments.add(Component.literal(Integer.toString(safeChildCount(level, listenerId))));
        arguments.add(Component.literal(Integer.toString(capital.getRoyalGuards().size())));
        arguments.add(Component.literal(Integer.toString(capital.getDukes().size())));
        arguments.add(Component.literal(Integer.toString(capital.getLords().size())));
        arguments.add(Component.literal(Integer.toString(capital.getKnights().size())));
        arguments.add(Component.literal(Long.toString(mourningDaysRemaining(level, capital))));
        arguments.add(resolveHouseName(level, playerId));
        arguments.add(resolveSurname(level, playerId));

        addPronouns(arguments, handIdentity);
        addPronouns(arguments, commanderIdentity);
        addPronouns(arguments, heraldIdentity);
        addPronouns(arguments, grandMaesterIdentity);
        addPronouns(arguments, masterOfLawsIdentity);
        addPronouns(arguments, ambassadorIdentity);
        arguments.add(resolveForeignCapitalName(level, foreignCapital));

        return new CapitalDialogueContext(arguments);
    }

    Object[] arguments() {
        return arguments.toArray(Object[]::new);
    }

    private static void addPronouns(
            List<Component> arguments,
            CapitalDialogueIdentityResolver.Identity identity
    ) {
        arguments.add(identity.subjectPronoun());
        arguments.add(identity.objectPronoun());
        arguments.add(identity.possessiveAdjective());
        arguments.add(identity.possessivePronoun());
        arguments.add(identity.reflexivePronoun());
    }

    private static Component resolveEventType(CapitalDialogueEventModels.ChronicleEvent event) {
        if (event == null) {
            return Component.translatable("mcacapitals.dynamic.event_type.none");
        }

        return Component.translatable(
                "mcacapitals.dynamic.event_type."
                        + event.type().name().toLowerCase(Locale.ROOT)
        );
    }

    private static Component resolveCapitalName(ServerLevel level, CapitalRecord capital) {
        Integer villageId = capital.getVillageId();
        return villageId == null
                ? Component.translatable("mcacapitals.dynamic.capital")
                : MCAIntegrationBridge.getVillageNameComponent(level, villageId);
    }

    private static Component resolveForeignCapitalName(
            ServerLevel level,
            CapitalRecord foreignCapital
    ) {
        if (foreignCapital == null || foreignCapital.getVillageId() == null) {
            return Component.translatable("mcacapitals.dynamic.foreign_capital");
        }

        return MCAIntegrationBridge.getVillageNameComponent(
                level,
                foreignCapital.getVillageId()
        );
    }

    private static Component resolveEntityName(
            ServerLevel level,
            CapitalRecord capital,
            UUID id,
            String fallbackKey
    ) {
        if (id == null) {
            return Component.translatable(fallbackKey);
        }

        if (capital != null
                && capital.isPlayerSovereign()
                && id.equals(capital.getPlayerSovereignId())
                && capital.getPlayerSovereignName() != null
                && !capital.getPlayerSovereignName().isBlank()) {
            return Component.literal(capital.getPlayerSovereignName().trim());
        }

        if (level.getServer() != null) {
            ServerPlayer onlinePlayer = level.getServer().getPlayerList().getPlayer(id);
            if (onlinePlayer != null) {
                return literalOrFallback(
                        MCAIntegrationBridge.getPlayerDialogueName(onlinePlayer),
                        fallbackKey
                );
            }
        }

        if (capital != null && capital.getCapitalId() != null) {
            PlayerCapitalTitleRecord playerRecord =
                    PlayerCapitalTitleService.get(level, id, capital.getCapitalId());
            if (playerRecord != null
                    && playerRecord.getCachedPlayerName() != null
                    && !playerRecord.getCachedPlayerName().isBlank()) {
                return Component.literal(playerRecord.getCachedPlayerName().trim());
            }
        }

        String savedName = CapitalNameService.resolveDisplayName(level, capital, id);
        if (savedName != null
                && !savedName.isBlank()
                && !savedName.equals(id.toString())) {
            return Component.literal(savedName.trim());
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        if (entity != null && entity.getName() != null) {
            return Component.literal(entity.getName().getString());
        }

        return Component.translatable(fallbackKey);
    }

    private static Component resolveSurname(ServerLevel level, UUID id) {
        if (level == null || id == null) {
            return Component.translatable("mcacapitals.dynamic.surname.unknown");
        }

        PlayerHouseRecord playerHouse = PlayerHouseService.get(level, id);
        if (playerHouse != null && playerHouse.hasHouseName()) {
            return Component.literal(playerHouse.getHouseName());
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        if (entity != null && MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            String surname = VillagerIdentityService.getCurrentSurname(entity);
            if (surname != null && !surname.isBlank()) {
                return Component.literal(surname.trim());
            }
        }

        return Component.translatable("mcacapitals.dynamic.surname.unknown");
    }

    private static Component resolveHouseName(ServerLevel level, UUID id) {
        if (level == null || id == null) {
            return Component.translatable("mcacapitals.dynamic.house.unknown");
        }

        PlayerHouseRecord playerHouse = PlayerHouseService.get(level, id);
        if (playerHouse != null && playerHouse.hasHouseName()) {
            return Component.literal(playerHouse.getHouseName());
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, id);
        if (entity != null && MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);
            if (identity != null && identity.hasFoundedHouse()) {
                return Component.literal(identity.houseName().trim());
            }
        }

        return Component.translatable("mcacapitals.dynamic.house.unknown");
    }

    private static Component resolveSpouseName(
            ServerLevel level,
            CapitalRecord capital,
            UUID id
    ) {
        if (id == null) {
            return Component.translatable("mcacapitals.dynamic.spouse");
        }

        UUID spouseId = MCAIntegrationBridge.getSpouse(level, id);
        return resolveEntityName(level, capital, spouseId, "mcacapitals.dynamic.spouse");
    }

    private static UUID effectiveSovereignId(CapitalRecord capital) {
        return capital.getPlayerSovereignId() != null
                ? capital.getPlayerSovereignId()
                : capital.getSovereign();
    }

    private static UUID effectiveConsortId(CapitalRecord capital) {
        return capital.getPlayerConsortId() != null
                ? capital.getPlayerConsortId()
                : capital.getConsort();
    }

    private static UUID effectiveHandId(ServerLevel level, CapitalRecord capital) {
        UUID playerHand = PlayerCapitalTitleService.getHandHolder(level, capital);
        return playerHand != null ? playerHand : capital.getHand();
    }

    private static UUID effectiveCommanderId(ServerLevel level, CapitalRecord capital) {
        UUID playerCommander = PlayerCapitalTitleService.getCommanderHolder(level, capital);
        return playerCommander != null ? playerCommander : capital.getCommander();
    }

    private static Component literalOrFallback(String value, String fallbackKey) {
        return value == null || value.isBlank()
                ? Component.translatable(fallbackKey)
                : Component.literal(value.trim());
    }

    private static int safePopulation(ServerLevel level, CapitalRecord capital) {
        Integer villageId = capital.getVillageId();
        return villageId == null
                ? 0
                : Math.max(0, MCAIntegrationBridge.getVillagePopulation(level, villageId));
    }

    private static int safeChildCount(ServerLevel level, UUID id) {
        return id == null ? 0 : MCAIntegrationBridge.getChildren(level, id).size();
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }

    private static long mourningDaysRemaining(ServerLevel level, CapitalRecord capital) {
        if (!capital.isMourningActive()) {
            return 0L;
        }
        return Math.max(0L, capital.getMourningEndDay() - currentDay(level));
    }
}
