package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalCrownJusticeService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticAgreementService;
import com.majesttyx.mcacapitals.capital.CapitalDiplomaticGiftService;
import com.majesttyx.mcacapitals.capital.CapitalForeignAffairsService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalPlayerWarrantDialogueService;
import com.majesttyx.mcacapitals.capital.CapitalSovereignDeclarationPromptService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private static final String REVIEW_CROWN_JUSTICE_ANSWER =
            "mcacapitals_review_crown_justice";

    private static final String ASK_FOREIGN_AFFAIRS_ANSWER =
            "mcacapitals_ask_foreign_affairs";

    private static final String SEND_GIFT_ANSWER =
            "mcacapitals_send_gift";

    private static final String MANAGE_DIPLOMACY_ANSWER =
            "mcacapitals_manage_diplomacy";

    private static final String DECLARE_FOR_CAPITAL_ANSWER =
            "mcacapitals_declare_for_capital";

    private static final String PAY_WARRANT_FINE_ANSWER =
            "mcacapitals_pay_warrant_fine";

    private static final String SURRENDER_WARRANT_ANSWER =
            "mcacapitals_surrender_warrant";

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
                        || currentAnswers.contains(REVIEW_CROWN_JUSTICE_ANSWER)
                        || currentAnswers.contains(ASK_FOREIGN_AFFAIRS_ANSWER)
                        || currentAnswers.contains(SEND_GIFT_ANSWER)
                        || currentAnswers.contains(MANAGE_DIPLOMACY_ANSWER)
                        || currentAnswers.contains(DECLARE_FOR_CAPITAL_ANSWER)
                        || currentAnswers.contains(PAY_WARRANT_FINE_ANSWER)
                        || currentAnswers.contains(SURRENDER_WARRANT_ANSWER);

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

        if (!CapitalCrownJusticeService.canShowDialogueAnswer(player, villager)) {
            filtered.remove(REVIEW_CROWN_JUSTICE_ANSWER);
        }

        if (!CapitalForeignAffairsService.canShowDialogueAnswer(
                player,
                villager
        )) {
            filtered.remove(ASK_FOREIGN_AFFAIRS_ANSWER);
        }

        if (!CapitalDiplomaticGiftService.canShowDialogueAnswer(
                player,
                villager
        )) {
            filtered.remove(SEND_GIFT_ANSWER);
        }

        if (!CapitalDiplomaticAgreementService.canShowDialogueAnswer(
                player,
                villager
        )) {
            filtered.remove(MANAGE_DIPLOMACY_ANSWER);
        }

        if (!CapitalSovereignDeclarationPromptService.canShowDeclarationAnswer(
                player,
                villager
        )) {
            filtered.remove(DECLARE_FOR_CAPITAL_ANSWER);
        }

        if (!CapitalPlayerWarrantDialogueService.canShowPayFine(player, villager)) {
            filtered.remove(PAY_WARRANT_FINE_ANSWER);
        }

        if (!CapitalPlayerWarrantDialogueService.canShowSurrender(player, villager)) {
            filtered.remove(SURRENDER_WARRANT_ANSWER);
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
        filtered.remove(REVIEW_CROWN_JUSTICE_ANSWER);
        filtered.remove(ASK_FOREIGN_AFFAIRS_ANSWER);
        filtered.remove(SEND_GIFT_ANSWER);
        filtered.remove(MANAGE_DIPLOMACY_ANSWER);
        filtered.remove(DECLARE_FOR_CAPITAL_ANSWER);
        filtered.remove(PAY_WARRANT_FINE_ANSWER);
        filtered.remove(SURRENDER_WARRANT_ANSWER);

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