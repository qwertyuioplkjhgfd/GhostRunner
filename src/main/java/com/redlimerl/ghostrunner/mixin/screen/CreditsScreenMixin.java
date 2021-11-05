package com.redlimerl.ghostrunner.mixin.screen;


import com.redlimerl.ghostrunner.GhostRunner;
import com.redlimerl.ghostrunner.record.GhostInfo;
import net.minecraft.client.gui.screen.CreditsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreditsScreen.class)
public class CreditsScreenMixin {
    @Inject(method = "close", at = @At("TAIL"))
    private void closeMixin(CallbackInfo ci) {
        if (GhostRunner.IS_COMPLETED) {
            GhostRunner.IS_COMPLETED = false;
            GhostInfo.INSTANCE.save();
        }
    }
}
