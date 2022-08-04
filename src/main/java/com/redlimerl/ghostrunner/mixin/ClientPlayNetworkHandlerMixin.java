package com.redlimerl.ghostrunner.mixin;

import com.redlimerl.ghostrunner.GhostRunner;
import com.redlimerl.ghostrunner.record.GhostInfo;
import com.redlimerl.ghostrunner.record.ReplayGhost;
import com.redlimerl.ghostrunner.record.data.GhostData;
import com.redlimerl.ghostrunner.record.data.GhostType;
import com.redlimerl.ghostrunner.util.Utils;
import com.redlimerl.speedrunigt.SpeedRunIGT;
import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.TimerStatus;
import com.redlimerl.speedrunigt.timer.running.RunType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.Difficulty;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    @Shadow private MinecraftClient client;
    @Shadow private ClientWorld world;
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(method="onPlaySoundId", at=@At("TAIL"))
    private void onPlaySoundId(PlaySoundIdS2CPacket packet, CallbackInfo ci) {
        if (packet.getVolume() == 0f) {
            switch (packet.getSoundId().toString()) {
                case "minecraft:ambient.basalt_deltas.additions":
                    startRecording();
                    break;
                case "minecraft:ambient.basalt_deltas.loop":
                    stopRecording(packet.getCategory().getName(), packet.getPitch());
                    break;
                case "minecraft:ambient.basalt_deltas.mood":
                    startReplaying(packet.getCategory().getName(), packet.getPitch());
                    break;
                case "minecraft:ambient.cave":
                    killGhosts();
                    break;
                case "minecraft:ambient.crimson_forest.additions":
                    break;
                case "ghostrunner:a":
                    System.out.println("yay");
                    break;
                //pause
                //import
                //copy
                //TODO import into slot, pause/resume, copy
            }
        }
    }

    private void killGhosts() {
        LOGGER.info("GhostRunner is now stopping replays");
        ReplayGhost.getGhostList().clear();
    }

    private void startReplaying(String name, float pitch) {
        LOGGER.info("GhostRunner is now replaying slot " + name + pitch);
        GhostInfo ghostInfo;
        try {
            ghostInfo = GhostInfo.fromData(name, pitch);
            ReplayGhost.addGhost(ghostInfo);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void stopRecording(String category, float slot) {
        LOGGER.info("GhostRunner is now completing and saving in slot " + category + slot);
//        InGameTimer.complete();
        GhostInfo.INSTANCE.savePractice(category, slot);
        if (client.player != null) {
            client.player.sendChatMessage("/trigger mod_gr_rta set " + InGameTimer.getInstance().getRealTimeAttack());
            client.player.sendChatMessage("/trigger mod_gr_igt set " + InGameTimer.getInstance().getInGameTime());
        }
    }

    private void startRecording() {
        LOGGER.info("GhostRunner is now recording!");
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
        IntegratedServer server = client.getServer();
        if (server != null) {
            String folderName = server.getIconFile().getParentFile().getName();
            System.out.println(folderName);
            InGameTimer.start(folderName, RunType.SET_SEED);
        }
    }
}
