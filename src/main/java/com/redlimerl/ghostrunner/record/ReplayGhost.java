package com.redlimerl.ghostrunner.record;

import com.redlimerl.ghostrunner.GhostRunner;
import com.redlimerl.ghostrunner.config.GhostRunnerProperties;
import com.redlimerl.ghostrunner.data.RunnerOptions;
import com.redlimerl.ghostrunner.entity.GhostEntity;
import com.redlimerl.ghostrunner.record.data.Timeline;
import com.redlimerl.ghostrunner.util.Utils;
import com.redlimerl.speedrunigt.option.SpeedRunOption;
import com.redlimerl.speedrunigt.option.SpeedRunOptions;
import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.InGameTimerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ReplayGhost {

    //고스트 스킨 목록
    private static final HashMap<UUID, Identifier> skins = new HashMap<>();
    private static final HashMap<UUID, String> models = new HashMap<>();
    public static void addPlayerSkin(UUID uuid, Identifier id, String model) {
        skins.put(uuid, id);
        models.put(uuid, model);
    }
    public static Identifier getPlayerSkin(UUID uuid) {
        return skins.getOrDefault(uuid, DefaultSkinHelper.getTexture());
    }
    public static String getPlayerModel(UUID uuid) {
        return models.getOrDefault(uuid, "default");
    }

    //선택한 고스트 목록
    private static final HashMap<Long, ArrayList<UUID>> selectedGhosts = new HashMap<>();
    public static void toSelectGhosts(Long seed, UUID... uuids) {
        ArrayList<UUID> uuidList = new ArrayList<>();
        int c = 0;
        for (UUID uuid : uuids) {
            if (c > 4) break;
            uuidList.add(uuid);
            c++;
        }
        selectedGhosts.put(seed, uuidList);
    }
    public static ArrayList<UUID> getSelectedGhosts(Long seed) {
        return selectedGhosts.getOrDefault(seed, new ArrayList<>());
    }
    public static void removeInSelectedGhosts(Long seed, UUID... uuids) {
        for (UUID uuid : uuids) {
            if (selectedGhosts.containsKey(seed)) {
                selectedGhosts.get(seed).remove(uuid);
            }
        }
    }

    //체크 포인트 메세지
    public static void sendBestCheckPointMessage(Timeline.Moment moment) {
        if (!SpeedRunOption.getOption(RunnerOptions.TOGGLE_CHECKPOINT_MESSAGE)) return;

        long bestTime = 0;
        for (ReplayGhost replayGhost : ghostList) {
            long time = replayGhost.ghostInfo.getCheckPoint(moment);
            if (time != 0 && (bestTime == 0 || bestTime > time)) {
                bestTime = time;
            }
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        long nowTime = InGameTimer.getInstance().getInGameTime();
        if (bestTime == 0) {
            player.sendMessage(
                    new TranslatableText("ghostrunner.message.checkpoint_new")
                            .append(new LiteralText(" [" + InGameTimerUtils.timeToStringFormat(nowTime) + "]")
                                    .formatted(Formatting.YELLOW)), false
            );
        } else {
            boolean isFast = bestTime > nowTime;
            player.sendMessage(
                    new TranslatableText("ghostrunner.message.checkpoint_" + (isFast ? "faster" : "slower") + "_than_ghost")
                            .append(new LiteralText(" [" + InGameTimerUtils.timeToStringFormat(nowTime)).formatted(Formatting.YELLOW))
                            .append(new LiteralText(" (" + (isFast ? "-" : "+") + " " + (InGameTimerUtils.timeToStringFormat(Math.abs(nowTime - bestTime))) + ")")
                                    .formatted(isFast ? Formatting.GREEN : Formatting.RED))
                            .append(new LiteralText("]").formatted(Formatting.YELLOW)), false);
        }
    }

    private static final ArrayList<ReplayGhost> ghostList = new ArrayList<>();
    public static boolean paused;

    public static ArrayList<ReplayGhost> getGhostList() {
        return ghostList;
    }

    public static void insertBrains(Long seed) {
        ghostList.clear();

        if (!selectedGhosts.containsKey(seed) || selectedGhosts.get(seed).isEmpty()) {
            return;
        }

        for (UUID uuid : selectedGhosts.get(seed)) {
            GhostInfo ghostInfo = GhostInfo.fromData(uuid);
            if (ghostInfo.getGhostData().getSeed() == seed && Objects.equals(ghostInfo.getGhostData().getClientVersion(), GhostRunner.CLIENT_VERSION)) {
                ghostList.add(new ReplayGhost(ghostInfo));
                Utils.downloadPlayerSkin(ghostInfo.getGhostData().getGhostUserUuid());
            }
        }
    }

    public static void addGhost(GhostInfo ghostInfo) {
        ghostList.add(new ReplayGhost(ghostInfo));
        Utils.downloadPlayerSkin(ghostInfo.getGhostData().getGhostUserUuid());
    }

    public static void tickGhost() {
        if (ghostList.isEmpty() || paused) return;

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.player.clientWorld == null) return;
        ClientWorld playerWorld = client.player.clientWorld;

        ghostList.sort((o1, o2) -> {
            long time1 = o1.ghostInfo.getGhostData().getInGameTime();
            long time2 = o2.ghostInfo.getGhostData().getInGameTime();
            return (int) (time1 - time2);
        });

        Set<ReplayGhost> toRemove = new HashSet<>();
        int place = 1;
        for (ReplayGhost replayGhost : ghostList) {
            PlayerLog playerLog = replayGhost.ghostInfo.pollPlayerLog();
            if (playerLog == null) {
                replayGhost.remove();
                toRemove.add(replayGhost);
                continue;
            }

            if (playerLog.world != null && replayGhost.lastWorld != playerLog.world) replayGhost.lastWorld = playerLog.world;

            if (replayGhost.ghost == null) {
                if (!Objects.equals(replayGhost.lastWorld.toString(), playerWorld.getRegistryKey().getValue().toString())) { //if the ghost's world is not client world
                    continue;
                }
                replayGhost.summon(playerWorld, playerLog);
            }
            if (replayGhost.ghost != null) {
                if (GhostRunnerProperties.bootsEnabled) {
                    if (ghostList.size() > 1) {
                        replayGhost.ghost.setBoots(place);
                    } else {
                        replayGhost.ghost.clearBoots();
                    }
                }
            }

            if (!Objects.equals(replayGhost.lastWorld.toString(), playerWorld.getRegistryKey().getValue().toString()) //if the ghost's world is not client world
                    || !Objects.equals(replayGhost.ghost.world.getRegistryKey().getValue().toString(), playerWorld.getRegistryKey().getValue().toString())) {
                replayGhost.remove();
                continue;
            }

            replayGhost.ghost.updateTrackedPositionAndAngles(
                    playerLog.x == null ? replayGhost.ghost.getX() : playerLog.x,
                    playerLog.y == null ? replayGhost.ghost.getY() : playerLog.y,
                    playerLog.z == null ? replayGhost.ghost.getZ() : playerLog.z,
                    playerLog.yaw == null ? replayGhost.ghost.yaw : playerLog.yaw,
                    playerLog.pitch == null ? replayGhost.ghost.pitch : playerLog.pitch, 1, true);
            replayGhost.ghost.setHeadYaw(playerLog.yaw == null ? replayGhost.ghost.yaw : playerLog.yaw);
            if (playerLog.pose != null) replayGhost.ghost.setPose(playerLog.pose);

            place++;
        }
        ghostList.removeAll(toRemove);
    }


    /* =================*/

    private GhostEntity ghost = null;
    private Identifier lastWorld = null;
    private final GhostInfo ghostInfo;
    protected ReplayGhost(GhostInfo ghostInfo) {
        this.ghostInfo = ghostInfo;
    }

    public void summon(ClientWorld world, PlayerLog log) {
        GhostEntity entity = new GhostEntity(GhostRunner.GHOST_ENTITY_TYPE, world);
        entity.refreshPositionAndAngles(log.x == null ? 0 : log.x, log.y == null ? 0 : log.y, log.z == null ? 0 : log.z, 0f, 0f);
        entity.setTargetSkinUuid(ghostInfo.getGhostData().getGhostUserUuid());
        entity.model = getPlayerModel(ghostInfo.getGhostData().getGhostUserUuid());
        world.addEntity(entity.getEntityId(), entity);
        ghost = entity;
    }

    public void remove() {
        if (ghost != null) {
            ghost.remove();
            ghost = null;
        }
    }
}
