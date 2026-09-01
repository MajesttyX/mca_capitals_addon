package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRelationshipBand;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import forge.net.conczin.mca.Config;
import forge.net.conczin.mca.cobalt.network.NetworkHandler;
import forge.net.conczin.mca.entity.ai.Messenger;
import forge.net.conczin.mca.network.s2c.VillagerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalAmbientDialogueHandler {

    private static final long SCAN_INTERVAL_TICKS = 20L * 10L;
    private static final long CAPITAL_COOLDOWN_TICKS = 20L * 15L;
    private static final long VILLAGER_COOLDOWN_TICKS = 20L * 180L;
    private static final long COMBAT_COOLDOWN_TICKS = 20L * 5L;
    private static final int RESPONSE_DELAY_MIN_TICKS = 20;
    private static final int RESPONSE_DELAY_MAX_TICKS = 60;
    private static final int RECENT_CONVERSATION_MEMORY = 4;
    private static final int EVENING_START_TIME = 9000;
    private static final int EVENING_END_TIME = 12000;
    private static final int BELL_SEARCH_RADIUS = 72;
    private static final double BELL_RADIUS = 20.0D;
    private static final double PLAYER_HEAR_RADIUS = 24.0D;
    private static final double PLAYER_NEAR_BELL_RADIUS = 36.0D;

    private final Map<UUID, CapitalConversationState> capitalStates = new HashMap<>();
    private final Map<UUID, ParticipantState> participantStates = new HashMap<>();
    private MinecraftServer activeServer;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        if (activeServer != level.getServer()) {
            activeServer = level.getServer();
            capitalStates.clear();
            participantStates.clear();
        }

        long gameTime = level.getGameTime();
        processPendingResponses(level, gameTime);

        if (gameTime % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        int dayTime = (int) (level.getDayTime() % 24000L);
        if (dayTime < EVENING_START_TIME || dayTime > EVENING_END_TIME) {
            return;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalsSnapshot().values()) {
            tickCapital(level, capital, gameTime);
        }
    }

    private void tickCapital(ServerLevel level, CapitalRecord capital, long gameTime) {
        if (capital == null
                || capital.getState() != CapitalState.ACTIVE
                || capital.getVillageId() == null
                || !CapitalManager.isCapitalInLevel(capital, level)) {
            return;
        }

        UUID capitalId = capital.getCapitalId();
        CapitalConversationState state = capitalStates.computeIfAbsent(
                capitalId,
                ignored -> new CapitalConversationState()
        );

        if (state.pending != null || gameTime < state.nextEligibleTick) {
            return;
        }

        CapitalAmbientConversationDefinitions.Context context =
                CapitalAmbientConversationDefinitions.Context.EVENING;

        List<CapitalAmbientConversationDefinitions.Definition> definitions =
                CapitalAmbientConversationDefinitions.get(level, context);
        if (definitions.isEmpty()) {
            return;
        }

        BlockPos villageCenter = MCAIntegrationBridge.getVillageCenter(
                level,
                capital.getVillageId()
        );
        if (villageCenter == null || villageCenter.equals(BlockPos.ZERO)) {
            return;
        }

        BlockPos meetingPoint = findMeetingPoint(level, villageCenter);
        if (nearestPlayerNearBell(level, meetingPoint) == null) {
            return;
        }

        List<Entity> villagers = eligibleVillagersNearBell(
                level,
                capital,
                meetingPoint,
                gameTime
        );
        if (villagers.size() < 2) {
            return;
        }

        ExchangeCandidate candidate = pickCandidate(
                level,
                capital,
                state,
                context,
                definitions,
                villagers
        );
        if (candidate == null) {
            return;
        }

        Component call = CapitalAmbientConversationRuntime.createCall(
                level,
                capital,
                candidate.foreignCapital,
                candidate.caller,
                candidate.responder,
                context,
                candidate.definition.id()
        );
        if (call == null) {
            return;
        }

        reserveParticipant(
                candidate.caller.getUUID(),
                capitalId,
                candidate.definition.id(),
                gameTime
        );
        reserveParticipant(
                candidate.responder.getUUID(),
                capitalId,
                candidate.definition.id(),
                gameTime
        );

        playSpeech(candidate.caller);
        sendToNearbyPlayers(level, candidate.caller, call);

        rememberConversation(state, candidate.definition.id());
        state.nextEligibleTick = gameTime + CAPITAL_COOLDOWN_TICKS;
        state.pending = new PendingExchange(
                candidate.caller.getUUID(),
                candidate.responder.getUUID(),
                capitalId,
                candidate.foreignCapital == null ? null : candidate.foreignCapital.getCapitalId(),
                candidate.definition.id(),
                context,
                candidate.definition.requiresMourning(),
                gameTime + randomResponseDelay(level),
                level.dimension()
        );
    }

    private void processPendingResponses(ServerLevel level, long gameTime) {
        for (Map.Entry<UUID, CapitalConversationState> entry : capitalStates.entrySet()) {
            CapitalConversationState state = entry.getValue();
            PendingExchange pending = state.pending;
            if (pending == null
                    || !pending.dimension.equals(level.dimension())
                    || gameTime < pending.responseTick) {
                continue;
            }

            CapitalRecord capital = CapitalManager.getCapital(entry.getKey());
            if (!canCompletePending(level, capital, pending)) {
                cancelPending(state, pending);
                continue;
            }

            Entity caller = MCAIntegrationBridge.getEntityByUuid(level, pending.callerId);
            Entity responder = MCAIntegrationBridge.getEntityByUuid(level, pending.responderId);
            if (caller == null || responder == null) {
                cancelPending(state, pending);
                continue;
            }

            BlockPos villageCenter = MCAIntegrationBridge.getVillageCenter(
                    level,
                    capital.getVillageId()
            );
            if (villageCenter == null || villageCenter.equals(BlockPos.ZERO)) {
                cancelPending(state, pending);
                continue;
            }

            BlockPos meetingPoint = findMeetingPoint(level, villageCenter);
            if (nearestPlayerNearBell(level, meetingPoint) == null
                    || !caller.blockPosition().closerThan(meetingPoint, BELL_RADIUS)
                    || !responder.blockPosition().closerThan(meetingPoint, BELL_RADIUS)
                    || caller.distanceToSqr(responder) > BELL_RADIUS * BELL_RADIUS) {
                cancelPending(state, pending);
                continue;
            }

            CapitalRecord foreignCapital = pending.foreignCapitalId == null
                    ? null
                    : CapitalManager.getCapital(pending.foreignCapitalId);

            Component response = CapitalAmbientConversationRuntime.createResponse(
                    level,
                    capital,
                    foreignCapital,
                    responder,
                    caller,
                    pending.context,
                    pending.conversationId
            );
            if (response == null) {
                cancelPending(state, pending);
                continue;
            }

            playSpeech(responder);
            sendToNearbyPlayers(level, responder, response);
            completePending(state, pending);
        }
    }

    private boolean canCompletePending(
            ServerLevel level,
            CapitalRecord capital,
            PendingExchange pending
    ) {
        if (capital == null
                || capital.getState() != CapitalState.ACTIVE
                || capital.getVillageId() == null) {
            return false;
        }

        if (pending.requiresMourning && !capital.isMourningActive()) {
            return false;
        }

        Entity caller = MCAIntegrationBridge.getEntityByUuid(level, pending.callerId);
        Entity responder = MCAIntegrationBridge.getEntityByUuid(level, pending.responderId);
        return isParticipantValid(level, capital, caller)
                && isParticipantValid(level, capital, responder)
                && isReservedFor(pending.callerId, capital.getCapitalId())
                && isReservedFor(pending.responderId, capital.getCapitalId());
    }

    private ExchangeCandidate pickCandidate(
            ServerLevel level,
            CapitalRecord capital,
            CapitalConversationState state,
            CapitalAmbientConversationDefinitions.Context context,
            List<CapitalAmbientConversationDefinitions.Definition> definitions,
            List<Entity> villagers
    ) {
        List<ExchangeCandidate> preferred = buildCandidates(
                level,
                capital,
                state,
                context,
                definitions,
                villagers,
                true
        );
        if (!preferred.isEmpty()) {
            return preferred.get(level.random.nextInt(preferred.size()));
        }

        List<ExchangeCandidate> available = buildCandidates(
                level,
                capital,
                state,
                context,
                definitions,
                villagers,
                false
        );
        if (available.isEmpty()) {
            return null;
        }

        return available.get(level.random.nextInt(available.size()));
    }

    private List<ExchangeCandidate> buildCandidates(
            ServerLevel level,
            CapitalRecord capital,
            CapitalConversationState state,
            CapitalAmbientConversationDefinitions.Context context,
            List<CapitalAmbientConversationDefinitions.Definition> definitions,
            List<Entity> villagers,
            boolean avoidRecent
    ) {
        List<ExchangeCandidate> candidates = new ArrayList<>();

        for (CapitalAmbientConversationDefinitions.Definition definition : definitions) {
            if (definition.requiresMourning() && !capital.isMourningActive()) {
                continue;
            }

            if (avoidRecent && state.recentConversationIds.contains(definition.id())) {
                continue;
            }

            CapitalRecord foreignCapital = pickForeignCapital(
                    level,
                    capital,
                    definition.foreignCapital()
            );
            if (definition.foreignCapital()
                    != CapitalAmbientConversationDefinitions.ForeignCapital.NONE
                    && foreignCapital == null) {
                continue;
            }

            for (Entity caller : villagers) {
                if (avoidRecent && hasRecentConversation(caller.getUUID(), definition.id())) {
                    continue;
                }

                if (!areReferencesValid(
                        level,
                        capital,
                        caller.getUUID(),
                        definition.callReferences()
                )) {
                    continue;
                }

                if (!CapitalAmbientConversationRuntime.hasCall(
                        caller,
                        context,
                        definition.id()
                )) {
                    continue;
                }

                for (Entity responder : villagers) {
                    if (caller.getUUID().equals(responder.getUUID())
                            || caller.distanceToSqr(responder) > BELL_RADIUS * BELL_RADIUS) {
                        continue;
                    }

                    if (avoidRecent && hasRecentConversation(
                            responder.getUUID(),
                            definition.id()
                    )) {
                        continue;
                    }

                    if (!areReferencesValid(
                            level,
                            capital,
                            responder.getUUID(),
                            definition.responseReferences()
                    )) {
                        continue;
                    }

                    if (!CapitalAmbientConversationRuntime.hasResponse(
                            responder,
                            context,
                            definition.id()
                    )) {
                        continue;
                    }

                    candidates.add(
                            new ExchangeCandidate(
                                    caller,
                                    responder,
                                    definition,
                                    foreignCapital
                            )
                    );
                }
            }
        }

        return candidates;
    }

    private List<Entity> eligibleVillagersNearBell(
            ServerLevel level,
            CapitalRecord capital,
            BlockPos center,
            long gameTime
    ) {
        AABB area = new AABB(center).inflate(BELL_RADIUS, 8.0D, BELL_RADIUS);
        return MCAIntegrationBridge.getNearbyMCAVillagers(level, area).stream()
                .filter(villager -> isParticipantValid(level, capital, villager))
                .filter(villager -> isParticipantAvailable(villager.getUUID(), gameTime))
                .toList();
    }

    private boolean isParticipantValid(
            ServerLevel level,
            CapitalRecord capital,
            Entity villager
    ) {
        if (villager == null
                || !MCAIntegrationBridge.isAliveMCAVillagerEntity(villager)
                || !MCAIntegrationBridge.isTeenOrAdultVillager(level, villager.getUUID())
                || isRoyalFamilyMember(capital, villager.getUUID())) {
            return false;
        }

        if (villager instanceof LivingEntity living) {
            if (living.isSleeping()
                    || living.isDeadOrDying()
                    || living.getHealth() <= 0.0F
                    || living.hurtTime > 0) {
                return false;
            }

            if (living.getLastHurtByMobTimestamp() > 0
                    && living.tickCount - living.getLastHurtByMobTimestamp()
                    <= COMBAT_COOLDOWN_TICKS) {
                return false;
            }

            if (living.getLastHurtMobTimestamp() > 0
                    && living.tickCount - living.getLastHurtMobTimestamp()
                    <= COMBAT_COOLDOWN_TICKS) {
                return false;
            }
        }

        return true;
    }

    private boolean isRoyalFamilyMember(CapitalRecord capital, UUID villagerId) {
        if (capital == null || villagerId == null) {
            return false;
        }

        return villagerId.equals(capital.getSovereign())
                || villagerId.equals(capital.getConsort())
                || villagerId.equals(capital.getDowager())
                || villagerId.equals(capital.getHeir())
                || capital.isRoyalChild(villagerId)
                || capital.isLegitimizedRoyalChild(villagerId)
                || capital.isPrinceConsort(villagerId)
                || capital.isDowagerPrince(villagerId)
                || capital.isRoyalHouseholdMember(villagerId);
    }

    private boolean areReferencesValid(
            ServerLevel level,
            CapitalRecord capital,
            UUID speakerId,
            Set<CapitalAmbientConversationDefinitions.Reference> references
    ) {
        if (capital == null || speakerId == null || references == null || references.isEmpty()) {
            return true;
        }

        for (CapitalAmbientConversationDefinitions.Reference reference : references) {
            UUID referencedId = referenceId(level, capital, reference);
            if (reference == CapitalAmbientConversationDefinitions.Reference.SPEAKER
                    || referencedId == null
                    || speakerId.equals(referencedId)) {
                return false;
            }
        }

        return true;
    }

    private UUID referenceId(
            ServerLevel level,
            CapitalRecord capital,
            CapitalAmbientConversationDefinitions.Reference reference
    ) {
        return switch (reference) {
            case SPEAKER -> null;
            case SOVEREIGN -> capital.getPlayerSovereignId() != null
                    ? capital.getPlayerSovereignId()
                    : capital.getSovereign();
            case CONSORT -> capital.getPlayerConsortId() != null
                    ? capital.getPlayerConsortId()
                    : capital.getConsort();
            case DOWAGER -> capital.getDowager();
            case HEIR -> capital.getHeir();
            case HAND -> capital.getHand();
            case COMMANDER -> capital.getCommander();
            case HERALD -> capital.getHerald();
            case GRAND_MAESTER -> capital.getGrandMaester();
            case MASTER_OF_LAWS -> capital.getMasterOfLaws();
            case AMBASSADOR -> CapitalAmbassadorService.getAmbassador(level, capital);
        };
    }

    private CapitalRecord pickForeignCapital(
            ServerLevel level,
            CapitalRecord capital,
            CapitalAmbientConversationDefinitions.ForeignCapital filter
    ) {
        if (filter == null
                || filter == CapitalAmbientConversationDefinitions.ForeignCapital.NONE) {
            return null;
        }

        List<CapitalRecord> candidates = CapitalManager.getAllCapitalRecords().stream()
                .filter(other -> other != null
                        && other.getState() == CapitalState.ACTIVE
                        && other.getCapitalId() != null
                        && !other.getCapitalId().equals(capital.getCapitalId()))
                .filter(other -> matchesForeignCapitalFilter(level, capital, other, filter))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(level.random.nextInt(candidates.size()));
    }

    private boolean matchesForeignCapitalFilter(
            ServerLevel level,
            CapitalRecord capital,
            CapitalRecord foreignCapital,
            CapitalAmbientConversationDefinitions.ForeignCapital filter
    ) {
        if (filter == CapitalAmbientConversationDefinitions.ForeignCapital.ANY) {
            return true;
        }

        int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                capital.getCapitalId(),
                foreignCapital.getCapitalId()
        );
        CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                level,
                capital.getCapitalId(),
                foreignCapital.getCapitalId()
        );

        if (filter == CapitalAmbientConversationDefinitions.ForeignCapital.POSITIVE) {
            return state != CapitalDiplomaticState.WAR && score > 0;
        }

        CapitalRelationshipBand band = CapitalRelationshipBand.fromScore(score);
        return state != CapitalDiplomaticState.WAR
                && (band == CapitalRelationshipBand.FRIENDLY
                || band == CapitalRelationshipBand.EXCELLENT);
    }

    private boolean isParticipantAvailable(UUID villagerId, long gameTime) {
        ParticipantState state = participantStates.get(villagerId);
        return state == null
                || state.reservedCapitalId == null
                && gameTime >= state.nextEligibleTick;
    }

    private boolean isReservedFor(UUID villagerId, UUID capitalId) {
        ParticipantState state = participantStates.get(villagerId);
        return state != null && capitalId.equals(state.reservedCapitalId);
    }

    private boolean hasRecentConversation(UUID villagerId, String conversationId) {
        ParticipantState state = participantStates.get(villagerId);
        return state != null && conversationId.equals(state.lastConversationId);
    }

    private void reserveParticipant(
            UUID villagerId,
            UUID capitalId,
            String conversationId,
            long gameTime
    ) {
        ParticipantState state = participantStates.computeIfAbsent(
                villagerId,
                ignored -> new ParticipantState()
        );
        state.reservedCapitalId = capitalId;
        state.lastConversationId = conversationId;
        state.nextEligibleTick = gameTime + VILLAGER_COOLDOWN_TICKS;
    }

    private void releaseParticipant(UUID villagerId, UUID capitalId) {
        ParticipantState state = participantStates.get(villagerId);
        if (state != null && capitalId.equals(state.reservedCapitalId)) {
            state.reservedCapitalId = null;
        }
    }

    private void rememberConversation(
            CapitalConversationState state,
            String conversationId
    ) {
        state.recentConversationIds.remove(conversationId);
        state.recentConversationIds.addFirst(conversationId);
        while (state.recentConversationIds.size() > RECENT_CONVERSATION_MEMORY) {
            state.recentConversationIds.removeLast();
        }
    }

    private void completePending(
            CapitalConversationState state,
            PendingExchange pending
    ) {
        releaseParticipant(pending.callerId, pending.capitalId);
        releaseParticipant(pending.responderId, pending.capitalId);
        state.pending = null;
    }

    private void cancelPending(
            CapitalConversationState state,
            PendingExchange pending
    ) {
        releaseParticipant(pending.callerId, pending.capitalId);
        releaseParticipant(pending.responderId, pending.capitalId);
        state.pending = null;
    }

    private int randomResponseDelay(ServerLevel level) {
        return RESPONSE_DELAY_MIN_TICKS
                + level.random.nextInt(
                        RESPONSE_DELAY_MAX_TICKS - RESPONSE_DELAY_MIN_TICKS + 1
                );
    }

    private BlockPos findMeetingPoint(ServerLevel level, BlockPos villageCenter) {
        return level.getPoiManager()
                .findClosest(
                        holder -> holder.is(PoiTypes.MEETING),
                        villageCenter,
                        BELL_SEARCH_RADIUS,
                        PoiManager.Occupancy.ANY
                )
                .orElse(villageCenter);
    }

    private ServerPlayer nearestPlayerNearBell(ServerLevel level, BlockPos center) {
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            if (!player.blockPosition().closerThan(center, PLAYER_NEAR_BELL_RADIUS)) {
                continue;
            }

            double distance = player.distanceToSqr(
                    center.getX() + 0.5D,
                    center.getY() + 0.5D,
                    center.getZ() + 0.5D
            );
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private void playSpeech(Entity speaker) {
        if (speaker instanceof Messenger messenger) {
            messenger.playSpeechEffect();
        }
    }

    private void sendToNearbyPlayers(
            ServerLevel level,
            Entity speaker,
            Component line
    ) {
        if (speaker instanceof Messenger messenger) {
            MutableComponent content = line.copy();
            content = messenger.transformMessage(content);

            MutableComponent prefix = Component.literal(Config.getInstance().villagerChatPrefix)
                    .append(speaker.getDisplayName())
                    .append(": ");

            for (ServerPlayer player : level.players()) {
                if (player.distanceTo(speaker) <= PLAYER_HEAR_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new VillagerMessage(prefix.copy(), content.copy(), speaker.getUUID()),
                            player
                    );
                }
            }
            return;
        }

        Component message = CapitalDialogueSpeaker.formatVillagerSpeech(speaker, line);
        for (ServerPlayer player : level.players()) {
            if (player.distanceTo(speaker) <= PLAYER_HEAR_RADIUS) {
                player.sendSystemMessage(message);
            }
        }
    }

    private record ExchangeCandidate(
            Entity caller,
            Entity responder,
            CapitalAmbientConversationDefinitions.Definition definition,
            CapitalRecord foreignCapital
    ) {
    }

    private record PendingExchange(
            UUID callerId,
            UUID responderId,
            UUID capitalId,
            UUID foreignCapitalId,
            String conversationId,
            CapitalAmbientConversationDefinitions.Context context,
            boolean requiresMourning,
            long responseTick,
            ResourceKey<Level> dimension
    ) {
    }

    private static final class CapitalConversationState {
        private long nextEligibleTick;
        private final Deque<String> recentConversationIds = new ArrayDeque<>();
        private PendingExchange pending;
    }

    private static final class ParticipantState {
        private long nextEligibleTick;
        private UUID reservedCapitalId;
        private String lastConversationId = "";
    }
}
