package com.redlimerl.ghostrunner.mixin;

import com.google.common.io.Files;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.redlimerl.ghostrunner.GhostRunner;
import com.redlimerl.ghostrunner.record.GhostInfo;
import com.redlimerl.ghostrunner.record.ReplayGhost;
import com.redlimerl.ghostrunner.record.data.GhostData;
import com.redlimerl.ghostrunner.record.data.GhostType;
import com.redlimerl.ghostrunner.util.Utils;
import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.running.RunType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.Difficulty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static com.redlimerl.ghostrunner.GhostRunner.CLIENT_VERSION;
import static com.redlimerl.ghostrunner.GhostRunner.GHOST_VERSION;
import static com.redlimerl.ghostrunner.GhostRunner.MOD_VERSION;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow private MinecraftClient client;
    @Shadow private ClientWorld world;

    private static final Logger LOGGER = LogManager.getLogger("GhostRunner");

    private String clipboard;

    @Inject(method="onPlaySoundId", at=@At("TAIL"))
    private void onPlaySoundId(PlaySoundIdS2CPacket packet, CallbackInfo ci) {
        switch (packet.getSoundId().toString()) {
            case "ghostrunner:start_recording":
                startRecording();
                break;
            case "ghostrunner:stop_recording":
                stopRecording();
                break;
            case "ghostrunner:save_recording":
                saveRecording(packet.getCategory().getName(), packet.getPitch());
                break;
            case "ghostrunner:replay_recording":
                startReplaying(packet.getCategory().getName(), packet.getPitch());
                break;
            case "ghostrunner:stop_playback":
                killGhosts();
                break;
            case "ghostrunner:pause_playback":
                pausePlayback();
                break;
            case "ghostrunner:resume_playback":
                resumePlayback();
                break;
            case "ghostrunner:toggle_playback":
                togglePlayback();
                break;
            case "ghostrunner:import_to":
                importTo(packet.getCategory().getName(), packet.getPitch());
                break;
            case "ghostrunner:copy_recording":
                copy(packet.getCategory().getName(), packet.getPitch());
                break;
            case "ghostrunner:paste_recording":
                paste(packet.getCategory().getName(), packet.getPitch());
                break;
            case "ghostrunner:ping":
                client.player.sendChatMessage("/trigger mod_gr_ping set 1");
                break;
                //TODO implement
        }
    }

    private void paste(String name, float pitch) {
        if (clipboard == null) {
            LOGGER.warn("Tried pasting empty clipboard");
            return;
        }
        LOGGER.info("Pasting slot " + clipboard + " into " + name + pitch);
        try {
            File srcFile = GhostRunner.GHOSTS_PATH.resolve(clipboard).toFile();
            File destFile = GhostRunner.GHOSTS_PATH.resolve(name + pitch).toFile();
            FileUtils.copyDirectory(srcFile, destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copy(String name, float pitch) {
        LOGGER.info("Copying slot " + name + pitch);
        clipboard = name + pitch;
        if (!GhostRunner.GHOSTS_PATH.resolve(clipboard).toFile().exists()) {
            LOGGER.warn("Ghost " + clipboard + " not found in ghosts folder.");
        }
    }

    private void stopRecording() {
        LOGGER.info("GhostRunner is now stopping recording");
        InGameTimer.complete();
        GhostRunner.recording = false;
    }

    private void importTo(String category, float slot) {
        LOGGER.info("GhostRunner is now prompting ghost import into slot " + category + slot);
        Utils.promptImportGhost(GhostRunner.GHOSTS_PATH.resolve(category + slot));
    }

    private void togglePlayback() {
        if (ReplayGhost.paused) {
            resumePlayback();
        } else {
            pausePlayback();
        }
    }

    private void resumePlayback() {
        LOGGER.info("GhostRunner is now resuming playback");
        ReplayGhost.paused = false;
    }

    private void pausePlayback() {
        LOGGER.info("GhostRunner is now pausing playback");
        ReplayGhost.paused = true;
    }

    private void killGhosts() {
        LOGGER.info("GhostRunner is now stopping replays");
        for (ReplayGhost replayGhost : ReplayGhost.getGhostList()) {
            replayGhost.remove();
        }
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

    private void saveRecording(String category, float slot) {
        LOGGER.info("GhostRunner is now completing and saving in slot " + category + slot);
        if (!GhostRunner.recording) {
            LOGGER.error("Tried to save a ghost when not recording");
            return;
        }
//        InGameTimer.complete();
        GhostInfo.INSTANCE.savePractice(category, slot);
        if (client.player != null) {
            CommandContextBuilder<CommandSource> builder = new CommandContextBuilder<>(null, null, null, 0);

            client.getNetworkHandler().getCommandSource().getCompletions(builder.build("/trigger "), null).thenAccept(suggestions -> {
                for (Suggestion suggestion : suggestions.getList()) {
                    switch (suggestion.getText()) {
                        case "mod_gr_rta":
                            client.player.sendChatMessage("/trigger mod_gr_rta set " + InGameTimer.getInstance().getRealTimeAttack());
                            break;
                        case "mod_gr_igt":
                            client.player.sendChatMessage("/trigger mod_gr_igt set " + InGameTimer.getInstance().getInGameTime());
                            break;
                    }
                }
            });
        }
    }

    private void startRecording() {
        LOGGER.info("GhostRunner is now recording!");
        GhostRunner.recording = true;
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
            InGameTimer.start(folderName, RunType.SET_SEED);
        }
    }
}
