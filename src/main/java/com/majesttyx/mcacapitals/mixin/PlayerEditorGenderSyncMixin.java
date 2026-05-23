package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.MCACapitals;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.FamilyTree;
import net.conczin.mca.server.world.data.FamilyTreeNode;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Pseudo
@Mixin(targets = "net.conczin.mca.network.c2s.VillagerEditorSyncRequest", remap = false)
public abstract class PlayerEditorGenderSyncMixin {

    @Shadow(remap = false)
    public abstract String command();

    @Shadow(remap = false)
    public abstract UUID uuid();

    @Shadow(remap = false)
    public abstract CompoundTag data();

    @Inject(
            method = "handleServer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void mcacapitals$persistPlayerGenderSelection(ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }

        UUID targetId = uuid();
        if (targetId == null || !targetId.equals(player.getUUID())) {
            return;
        }

        String command = command();
        if (!"gender".equals(command) && !"sync".equals(command)) {
            return;
        }

        CompoundTag data = data();
        Gender gender = resolveGenderFromEditorData(data);
        if (gender == Gender.UNASSIGNED) {
            return;
        }

        try {
            PlayerSaveData saveData = PlayerSaveData.get(player);
            CompoundTag entityData = saveData.getEntityData();
            if (entityData == null) {
                entityData = new CompoundTag();
            }

            entityData.putInt("gender", gender.getId());
            entityData.putInt("Gender", gender.getId());

            saveData.setEntityData(entityData);
            saveData.setEntityDataSet(true);
            saveData.setDirty();

            FamilyTree familyTree = FamilyTree.get(player.serverLevel());
            FamilyTreeNode node = familyTree.getOrCreate(player);
            node.setGender(gender);
            familyTree.setDirty();

            MCACapitals.LOGGER.info(
                    "[MCACapitals] Synced MCA player editor gender. player='{}', command='{}', gender='{}', dataName='{}', genderId={}",
                    player.getGameProfile().getName(),
                    command,
                    gender.name(),
                    gender.getDataName(),
                    gender.getId()
            );
        } catch (Throwable t) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Failed to sync MCA player editor gender for player='{}' ({})",
                    player.getGameProfile().getName(),
                    t.toString()
            );
        }
    }

    private Gender resolveGenderFromEditorData(CompoundTag data) {
        if (data == null) {
            return Gender.UNASSIGNED;
        }

        if (data.contains("Gender")) {
            return Gender.byId(data.getInt("Gender"));
        }

        if (data.contains("gender")) {
            return Gender.byId(data.getInt("gender"));
        }

        return Gender.UNASSIGNED;
    }
}