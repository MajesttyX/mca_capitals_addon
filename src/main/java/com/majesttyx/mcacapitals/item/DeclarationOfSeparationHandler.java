package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.data.CapitalHouseDataAccess;
import com.majesttyx.mcacapitals.house.CapitalHouseRecord;
import com.majesttyx.mcacapitals.house.DeclarationOfSeparationService;
import com.majesttyx.mcacapitals.identity.VillagerIdentityData;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenDeclarationOfSeparationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DeclarationOfSeparationHandler {
    private DeclarationOfSeparationHandler(){}
    public static InteractionResult handleEntityInteract(Player player, Entity rawTarget, InteractionHand hand){
        if(player==null||rawTarget==null||hand==null) return InteractionResult.PASS;
        ItemStack held=player.getItemInHand(hand);
        if(!held.is(ModItems.HOUSE_LEDGER.get())||!player.isShiftKeyDown()) return InteractionResult.PASS;
        if(!(rawTarget instanceof LivingEntity target)) return InteractionResult.PASS;
        if(player.level().isClientSide) return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer serverPlayer)||!(player.level() instanceof ServerLevel level)) return InteractionResult.PASS;
        if(!MCAIntegrationBridge.isMCAVillagerEntity(target)){player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_separation.only_mca"));return InteractionResult.SUCCESS;}
        CapitalRecord capital=CapitalTitleResolver.findCapitalForEntity(level,target.getUUID());
        CapitalHouseRecord currentHouse=capital==null||capital.getCapitalId()==null?null:CapitalHouseDataAccess.findHouseForMember(level,capital.getCapitalId(),target.getUUID());
        if(capital==null||!DeclarationOfSeparationService.isEligibleFounder(level,capital,currentHouse,target.getUUID())){player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_separation.not_eligible"));return InteractionResult.SUCCESS;}
        VillagerIdentityService.ensureAssigned(level,target);
        VillagerIdentityData identity=VillagerIdentityService.getIdentity(target);
        if(identity==null||!identity.hasFoundedHouse()){player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_separation.no_current_house"));return InteractionResult.SUCCESS;}
        ModNetwork.sendToPlayer(serverPlayer,new OpenDeclarationOfSeparationPacket(target.getUUID(),target.getName().getString(),identity.houseName(),identity.houseWords()));
        return InteractionResult.SUCCESS;
    }
}
