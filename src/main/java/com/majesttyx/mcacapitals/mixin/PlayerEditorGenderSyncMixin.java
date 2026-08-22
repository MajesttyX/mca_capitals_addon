package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import forge.net.conczin.mca.entity.ai.Genetics;
import forge.net.conczin.mca.entity.ai.relationship.Gender;
import forge.net.conczin.mca.server.world.data.FamilyTree;
import forge.net.conczin.mca.server.world.data.FamilyTreeNode;
import forge.net.conczin.mca.server.world.data.PlayerSaveData;
import forge.net.conczin.mca.network.NbtDataMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Pseudo
@Mixin(targets = "forge.net.conczin.mca.network.c2s.VillagerEditorSyncRequest", remap = false)
public abstract class PlayerEditorGenderSyncMixin {

    @Shadow(remap = false)
    @Final
    private String command;

    @Shadow(remap = false)
    @Final
    private UUID uuid;

    @Inject(
            method = "receive(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void mcacapitals$persistPlayerGenderSelection(ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }

        if (uuid == null || !uuid.equals(player.getUUID())) {
            return;
        }

        if (!"gender".equals(command) && !"sync".equals(command)) {
            return;
        }

        CompoundTag editorData = ((NbtDataMessage) (Object) this).getData();
        Gender gender = resolveGenderFromEditorData(editorData);
        if (gender == Gender.UNASSIGNED) {
            return;
        }

        try {
            PlayerSaveData saveData = PlayerSaveData.get(player);
            CompoundTag entityData = saveData.getEntityData();
            CompoundTag normalized = entityData == null
                    ? new CompoundTag()
                    : entityData.copy();
            CompoundTag mcaData = normalized.contains(
                    ModDataKeys.MCA_DATA_KEY,
                    Tag.TAG_COMPOUND
            )
                    ? normalized.getCompound(ModDataKeys.MCA_DATA_KEY)
                    : new CompoundTag();

            Genetics.writeGender(mcaData, gender);
            normalized.put(ModDataKeys.MCA_DATA_KEY, mcaData);

            saveData.setEntityData(normalized);
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

        CompoundTag mcaData = data.contains(
                ModDataKeys.MCA_DATA_KEY,
                Tag.TAG_COMPOUND
        )
                ? data.getCompound(ModDataKeys.MCA_DATA_KEY)
                : data;

        Gender gender = Genetics.readGender(mcaData);
        if (gender != Gender.UNASSIGNED || mcaData == data) {
            return gender;
        }

        return Genetics.readGender(data);
    }
}