package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig;
import javazoom.jl.player.Player;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundUtils {

    private static ExecutorService sSountExecutorService = Executors.newSingleThreadExecutor();

    public static void say(String content) {
        if (!CodeLocatorUserConfig.loadConfig().isEnableVoice()) {
            return;
        }
        try {
            InputStream resourceAsStream = null;
            if (ResUtils.getString("voice_installing_apk").equals(content)) {
                Mob.mob(Mob.Action.EXEC, content);
                resourceAsStream = SoundUtils.class.getResourceAsStream("/sounds/lzl_installing.mp3");
            } else if (ResUtils.getString("voice_install_apk_failed").equals(content)) {
                Mob.mob(Mob.Action.EXEC, content);
                resourceAsStream = SoundUtils.class.getResourceAsStream("/sounds/lzl_installFailed.mp3");
            } else if (ResUtils.getString("voice_install_apk_success").equals(content)) {
                Mob.mob(Mob.Action.EXEC, content);
                resourceAsStream = SoundUtils.class.getResourceAsStream("/sounds/lzl_installFinish.mp3");
            } else if (ResUtils.getString("voice_start_apk").equals(content)) {
                Mob.mob(Mob.Action.EXEC, content);
                resourceAsStream = SoundUtils.class.getResourceAsStream("/sounds/lzl_startApp.mp3");
            }
            InputStream finalResourceAsStream = resourceAsStream;
            sSountExecutorService.submit(() -> {
                try {
                    final Player player = new Player(finalResourceAsStream);
                    player.play();
                } catch (Throwable t) {
                    OSHelper.getInstance().say(content);
                }
            });
        } catch (Throwable t) {
            Log.e("播放失败", t);
            try {
                OSHelper.getInstance().say(content);
            } catch (Throwable ignore) {
            }
        }
    }
}
