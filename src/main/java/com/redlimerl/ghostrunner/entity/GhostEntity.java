package com.redlimerl.ghostrunner.entity;

import com.redlimerl.ghostrunner.record.ReplayGhost;
import com.redlimerl.ghostrunner.render.GhostRenderFix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class GhostEntity extends LivingEntity {

    public static HashMap<UUID, UUID> ghostSkins = new HashMap<>();

    public GhostEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    public static class Model extends PlayerEntityModel<GhostEntity> {
        public Model() {
            super(0, false);
        }
    }

    public static class Renderer extends LivingEntityRenderer<GhostEntity, Model> {
        public Renderer(EntityRenderDispatcher dispatcher) {
            super(dispatcher, new Model(), 0);
        }

        @Override
        public Identifier getTexture(GhostEntity entity) {
            if (ghostSkins.containsKey(entity.uuid)) {
                return ReplayGhost.getPlayerSkin(ghostSkins.get(entity.uuid));
            }
            return DefaultSkinHelper.getTexture();
        }

        @Override
        protected void renderLabelIfPresent(GhostEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {

        }

        @Nullable
        @Override
        protected RenderLayer getRenderLayer(GhostEntity entity, boolean showBody, boolean translucent, boolean bl) {
            Identifier texture = this.getTexture(entity);
            return GhostRenderFix.getRenderLayer(texture);
        }

        @Override
        public void render(GhostEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
            GhostRenderFix.isRender = true;
            super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
            GhostRenderFix.isRender = false;
        }
        /*
        java.lang.NullPointerException: Cannot invoke "net.minecraft.client.gl.Framebuffer.beginWrite(boolean)" because the return value of "net.minecraft.client.render.WorldRenderer.getEntityFramebuffer()" is null
	at net.minecraft.client.render.RenderPhase.method_29703(RenderPhase.java:196)
	at net.minecraft.client.render.RenderPhase.startDrawing(RenderPhase.java:214)
	at com.google.common.collect.ImmutableList.forEach(ImmutableList.java:408)
	at net.minecraft.client.render.RenderLayer$MultiPhase.method_23596(RenderLayer.java:754)
	at net.minecraft.client.render.RenderPhase.startDrawing(RenderPhase.java:214)
	at net.minecraft.client.render.RenderLayer.draw(RenderLayer.java:686)
	at net.minecraft.client.render.VertexConsumerProvider$Immediate.draw(VertexConsumerProvider.java:82)
	at net.minecraft.client.render.VertexConsumerProvider$Immediate.getBuffer(VertexConsumerProvider.java:44)
	at net.minecraft.client.render.entity.LivingEntityRenderer.render(LivingEntityRenderer.java:116)
	at com.redlimerl.ghostrunner.entity.GhostEntity$Renderer.render(GhostEntity.java:70)
	at com.redlimerl.ghostrunner.entity.GhostEntity$Renderer.render(GhostEntity.java:42)
         */
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return new ArrayList<>();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {

    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        noClip = true;
    }

    @Override
    public boolean isSpectator() {
        return true;
    }

    @Override
    public boolean isInsideWall() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public void setTargetSkinUuid(UUID uuid) {
        ghostSkins.put(this.uuid, uuid);
    }
}
