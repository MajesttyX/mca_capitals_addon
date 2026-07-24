package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalAmbassadorService;
import com.majesttyx.mcacapitals.capital.CapitalBuildingService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.item.DiplomaticPackageItem;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Pseudo
@Mixin(
        targets = "net.conczin.mca.resources.data.dialogue.Question",
        remap = false
)
public abstract class DialogueCrownAuthorityAnswerMixin {

    private static final String PETITION_ANSWER =
            "mcacapitals_petition";

    private static final String REQUEST_ANSWER =
            "mcacapitals_request";

    private static final String SEIZE_THRONE_ANSWER =
            "mcacapitals_seize_throne";

    private static final String ACCUSE_ENEMY_ANSWER =
            "mcacapitals_accuse_enemy";

    private static final String REQUEST_ROYAL_PARDON_ANSWER =
            "mcacapitals_request_royal_pardon";

    private static final String SEND_GIFT_ANSWER =
            "mcacapitals_send_gift";

    private static final String MANAGE_DIPLOMACY_ANSWER =
            "mcacapitals_manage_diplomacy";

    @Inject(
            method = "getValidAnswers(Lnet/minecraft/server/level/ServerPlayer;Lnet/conczin/mca/entity/VillagerEntityMCA;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void mcacapitals$filterPetitionAnswersByCrownAuthority(
            ServerPlayer player,
            VillagerEntityMCA villager,
            CallbackInfoReturnable<List<String>> cir
    ) {
        List<String> currentAnswers = cir.getReturnValue();

        if (currentAnswers == null || currentAnswers.isEmpty()) {
            return;
        }

        boolean hasAnyManagedAnswer =
                currentAnswers.contains(PETITION_ANSWER)
                        || currentAnswers.contains(REQUEST_ANSWER)
                        || currentAnswers.contains(SEIZE_THRONE_ANSWER)
                        || currentAnswers.contains(ACCUSE_ENEMY_ANSWER)
                        || currentAnswers.contains(REQUEST_ROYAL_PARDON_ANSWER)
                        || currentAnswers.contains(SEND_GIFT_ANSWER)
                        || currentAnswers.contains(MANAGE_DIPLOMACY_ANSWER);

        if (!hasAnyManagedAnswer) {
            return;
        }

        if (player == null || villager == null) {
            cir.setReturnValue(removeAllManagedAnswers(currentAnswers));
            return;
        }

        List<String> filtered = new ArrayList<>(currentAnswers);

        if (!hasCrownAuthority(player.serverLevel(), villager.getUUID())) {
            filtered.remove(PETITION_ANSWER);
            filtered.remove(REQUEST_ANSWER);
            filtered.remove(SEIZE_THRONE_ANSWER);
        }

        if (!isMasterOfLaws(player.serverLevel(), villager.getUUID())) {
            filtered.remove(ACCUSE_ENEMY_ANSWER);
        }

        if (!canGrantRoyalPardon(player.serverLevel(), villager.getUUID())) {
            filtered.remove(REQUEST_ROYAL_PARDON_ANSWER);
        }

        if (!canSendGift(player, villager)) {
            filtered.remove(SEND_GIFT_ANSWER);
        }

        if (!CapitalDiplomaticAgreementService.canShowDialogueAnswer(
                player,
                villager
        )) {
            filtered.remove(MANAGE_DIPLOMACY_ANSWER);
        }

        cir.setReturnValue(filtered);
    }

    private static List<String> removeAllManagedAnswers(
            List<String> currentAnswers
    ) {
        List<String> filtered = new ArrayList<>(currentAnswers);

        filtered.remove(PETITION_ANSWER);
        filtered.remove(REQUEST_ANSWER);
        filtered.remove(SEIZE_THRONE_ANSWER);
        filtered.remove(ACCUSE_ENEMY_ANSWER);
        filtered.remove(REQUEST_ROYAL_PARDON_ANSWER);
        filtered.remove(SEND_GIFT_ANSWER);
        filtered.remove(MANAGE_DIPLOMACY_ANSWER);

        return filtered;
    }

    private static boolean hasCrownAuthority(
            ServerLevel level,
            UUID villagerId
    ) {
        if (level == null || villagerId == null) {
            return false;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        return villagerId.equals(capital.getSovereign())
                || villagerId.equals(capital.getHand());
    }

    private static boolean isMasterOfLaws(
            ServerLevel level,
            UUID villagerId
    ) {
        if (level == null || villagerId == null) {
            return false;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        return capital != null
                && capital.getState() == CapitalState.ACTIVE
                && villagerId.equals(capital.getMasterOfLaws());
    }

    private static boolean canGrantRoyalPardon(
            ServerLevel level,
            UUID villagerId
    ) {
        if (level == null || villagerId == null) {
            return false;
        }

        CapitalRecord capital = resolveCapital(level, villagerId);

        return capital != null
                && capital.getState() == CapitalState.ACTIVE
                && (villagerId.equals(capital.getSovereign())
                || villagerId.equals(capital.getHand())
                || villagerId.equals(capital.getMasterOfLaws()));
    }

    private static boolean canSendGift(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        if (player == null || villager == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();

        CapitalRecord capital = resolveAmbassadorCapital(
                level,
                villager.getUUID()
        );

        if (capital == null
                || capital.getState() != CapitalState.ACTIVE
                || !CapitalAmbassadorService.isAmbassador(
                level,
                capital,
                villager.getUUID()
        )) {
            return false;
        }

        UUID playerId = player.getUUID();

        if (!playerId.equals(capital.getPlayerSovereignId())
                && !playerId.equals(capital.getSovereign())) {
            return false;
        }

        if (!CapitalBuildingService.hasAmbassadorBuildings(
                level,
                capital
        )) {
            return false;
        }

        return hasFilledPackage(player.getMainHandItem())
                || hasFilledPackage(player.getOffhandItem());
    }

    private static boolean hasFilledPackage(ItemStack stack) {
        if (stack == null
                || stack.isEmpty()
                || !stack.is(ModItems.DIPLOMATIC_PACKAGE.get())) {
            return false;
        }

        ItemContainerContents contents = stack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );

        int count = 0;

        for (ItemStack stored : contents.nonEmptyItems()) {
            if (!DiplomaticPackageItem.mayStore(stored)) {
                return false;
            }

            count++;

            if (count > DiplomaticPackageItem.SLOT_COUNT) {
                return false;
            }
        }

        return count > 0;
    }

    private static CapitalRecord resolveAmbassadorCapital(
            ServerLevel level,
            UUID villagerId
    ) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && CapitalAmbassadorService.isAmbassador(
                    level,
                    capital,
                    villagerId
            )) {
                return capital;
            }
        }

        return null;
    }

    private static CapitalRecord resolveCapital(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(
                level,
                villagerId
        );

        if (capital == null) {
            Integer villageId = MCAIntegrationBridge.getVillageIdForResident(
                    level,
                    villagerId
            );

            capital = CapitalManager.getCapitalByVillageId(villageId);
        }

        return capital;
    }
}