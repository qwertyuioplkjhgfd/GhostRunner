package com.redlimerl.ghostrunner.mixin;

import com.redlimerl.ghostrunner.GhostRunner;
import com.redlimerl.ghostrunner.record.GhostInfo;
import com.redlimerl.ghostrunner.record.data.GhostData;
import com.redlimerl.ghostrunner.record.data.GhostType;
import com.redlimerl.ghostrunner.util.Utils;
import com.redlimerl.speedrunigt.timer.InGameTimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.world.Difficulty;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.UUID;

import static com.redlimerl.ghostrunner.GhostRunner.CLIENT_VERSION;
import static com.redlimerl.ghostrunner.GhostRunner.GHOST_VERSION;
import static com.redlimerl.ghostrunner.GhostRunner.MOD_VERSION;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(method="onPlaySoundId", at=@At("TAIL"))
    private void onPlaySoundId(PlaySoundIdS2CPacket packet, CallbackInfo ci) {
        if (packet.getVolume() == 0f) {
            switch (packet.getSoundId().toString()) {
                case "minecraft:ambient.basalt_deltas.additions":
                    LOGGER.info("Ghost is now recording!");
                    startRecording();
                    break;
                case "minecraft:ambient.basalt_deltas.loop":
                    LOGGER.info("Ghost is now saving in slot " + packet.getPitch());
                    stopRecording(packet.getPitch());
                    break;
            }
        }
    }

    private void stopRecording(float slot) {
        
    }

    private void startRecording() {
        GhostInfo recInfo = GhostInfo.INSTANCE;
        recInfo.clear();
        recInfo.setGhostData(new GhostData(
                UUID.randomUUID(),
                MOD_VERSION,
                CLIENT_VERSION,
                GHOST_VERSION,
                "temp",
                Utils.UUIDFromString(MinecraftClient.getInstance().getSession().getUuid()),
                MinecraftClient.getInstance().getSession().getUsername(),
                GhostType.SET_SEED.getId(),
                69420,
                0,
                0,
                Instant.now().toString(),
                RandomStringUtils.randomAlphanumeric(32),
                false,
                false,
                Difficulty.EASY.getName(),
                "",
                InGameTimer.getInstance().getCategory().getID(),
                true, //kek
                false
        ));
        InGameTimer.reset();
    }
}
