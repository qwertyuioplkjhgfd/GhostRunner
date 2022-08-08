package com.redlimerl.ghostrunner.mixin;

import com.redlimerl.ghostrunner.GhostRunner;
import com.redlimerl.ghostrunner.entity.GhostEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method="getRenderer", at=@At("HEAD"), cancellable = true)
    public <T extends Entity> void onGetRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (entity instanceof GhostEntity) {
            GhostEntity entity1 = (GhostEntity) entity;
            if (entity1.getModel().equals("slim")) {
                System.out.println("hello???");
                cir.setReturnValue(GhostRunner.slimRenderer);
                return;
            }
            cir.setReturnValue(GhostRunner.defaultRenderer);
        }
    }
}
