package com.redlimerl.ghostrunner.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.redlimerl.ghostrunner.GhostRunner;
import com.redlimerl.ghostrunner.data.RunnerOptions;
import com.redlimerl.ghostrunner.data.UpdateStatus;
import com.redlimerl.ghostrunner.gui.GenericToast;
import com.redlimerl.ghostrunner.record.ReplayGhost;
import com.redlimerl.ghostrunner.record.data.GhostData;
import com.redlimerl.speedrunigt.option.SpeedRunOption;
import com.redlimerl.speedrunigt.option.SpeedRunOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class Utils {

    public static int getUpdateButtonOffset() {
        return GhostRunner.UPDATE_STATUS.getStatus() == UpdateStatus.Status.OUTDATED && SpeedRunOption.getOption(RunnerOptions.UPDATE_NOTIFICATION) ? 20 : 0;
    }

    public static UUID UUIDFromString(String string) {
        if (string.contains("-")) {
            return UUID.fromString(string);
        } else {
            Pattern pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
            String uuid = pattern.matcher(string).replaceAll("$1-$2-$3-$4-$5");
            return UUID.fromString(uuid);
        }
    }

    public static void downloadPlayerSkin(UUID uuid) {
        new Thread(() -> {
            try {
                MinecraftSessionService sessionService = MinecraftClient.getInstance().getSessionService();
                PlayerSkinProvider skinProvider = MinecraftClient.getInstance().getSkinProvider();

                GameProfile profile = sessionService.fillProfileProperties(new GameProfile(uuid, null), false);

                if (profile != null) {
                    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> minecraftProfileTexture = skinProvider.getTextures(profile);

                    if (minecraftProfileTexture != null) {
                        MinecraftProfileTexture profileTexture = minecraftProfileTexture.get(MinecraftProfileTexture.Type.SKIN);
                        Identifier skin = skinProvider.loadSkin(profileTexture, MinecraftProfileTexture.Type.SKIN);
                        String model = profileTexture.getMetadata("model");
                        if (model == null) {
                            model = "default";
                        }

                        ReplayGhost.addPlayerSkin(uuid, skin, model);
                    }
                }
            } catch (Exception e) {
                GhostRunner.debug("Failed load ghost's skin.");
            }
        }).start();
    }

    public static boolean isUrl(String text) {
        return text.matches("^(http|https|ftp)://.*$");
    }

    /**
     * @param left compare version A
     * @param right compare version B
     * @return if left > right = 1 / if left == right = 0 / if left < right = -1
     */
    public static int compareVersion(@NotNull String left, @NotNull String right) {
        if (left.equals(right)) {
            return 0;
        }
        int leftStart = 0, rightStart = 0, result;
        do {
            int leftEnd = left.indexOf('.', leftStart);
            int rightEnd = right.indexOf('.', rightStart);
            Integer leftValue = Integer.parseInt(leftEnd < 0
                    ? left.substring(leftStart)
                    : left.substring(leftStart, leftEnd));
            Integer rightValue = Integer.parseInt(rightEnd < 0
                    ? right.substring(rightStart)
                    : right.substring(rightStart, rightEnd));
            result = leftValue.compareTo(rightValue);
            leftStart = leftEnd + 1;
            rightStart = rightEnd + 1;
        } while (result == 0 && leftStart > 0 && rightStart > 0);
        if (result == 0) {
            if (leftStart > rightStart) {
                return containsNonZeroValue(left, leftStart) ? 1 : 0;
            }
            if (leftStart < rightStart) {
                return containsNonZeroValue(right, rightStart) ? -1 : 0;
            }
        }
        return result;
    }

    private static boolean containsNonZeroValue(String str, int beginIndex) {
        for (int i = beginIndex; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c != '0' && c != '.') {
                return true;
            }
        }
        return false;
    }

    public static void promptImportGhost(Path dest) {
        MemoryStack stack = MemoryStack.stackPush();
        PointerBuffer filters = stack.mallocPointer(1);
        filters.put(stack.UTF8("*.mcg"));
        filters.flip();
        String string = TinyFileDialogs.tinyfd_openFileDialog("Import Ghost", GhostRunner.GHOST_SHARE_PATH.toString(),
                filters, "Minecraft Ghost (*.mcg)", false);

        ArrayList<String> successList = new ArrayList<>();
        ArrayList<String> failList = new ArrayList<>();

        if (string != null) {
            for (String s : string.split("\\|")) {
                Path path = Paths.get(s);
                String name = path.getFileName().toString().substring(0, path.getFileName().toString().length() - 4);

                try {
                    Path tempPath = GhostRunner.GHOST_TEMP_PATH.resolve(name);
                    tempPath.toFile().mkdirs();
                    TarGzUtil.decompressTarGzipFile(path, tempPath);

                    if (dest.toFile().exists()) {
                        dest.toFile().delete();
                    }
                    GhostData ghostData = GhostData.loadData(tempPath);
                    if (tempPath.toFile().renameTo(dest.toFile())) {
                        successList.add(ghostData.getGhostName());
                    } else {
                        failList.add(ghostData.getGhostName());
                    }
                } catch (Exception e) {
                    failList.add(name);
                }
            }
        }

        for (String success : successList) {
            MinecraftClient.getInstance().getToastManager().add(
                    new GenericToast(I18n.translate("ghostrunner.message.import_success"), ": "+success, new ItemStack(Items.CHEST)));
        }

        for (String fail : failList) {
            MinecraftClient.getInstance().getToastManager().add(
                    new GenericToast(I18n.translate("ghostrunner.message.import_fail"), ": "+fail, new ItemStack(Items.BEDROCK)));
        }

        stack.pop();
    }
}
