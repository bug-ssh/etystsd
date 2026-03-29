package com.voice.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 核心配置：路径以 XOR 编码存储在 libvoiceconfig.so 中，Java 层无明文。
 * 自定义路径通过 SharedPreferences 覆盖默认值。
 */
public final class NativeConfig {

    private static final String TAG  = "NativeConfig";
    private static final String PREF = "voice_prefs";

    public static final String KEY_COMMENT_PATH  = "custom_comment_path";
    public static final String KEY_IM_PATH       = "custom_im_path";
    public static final String KEY_AUDIO_URI     = "selected_audio_uri";
    public static final String KEY_AUDIO_NAME    = "selected_audio_name";

    private static boolean soLoaded = false;

    static {
        try {
            System.loadLibrary("voiceconfig");
            soLoaded = true;
            Log.i(TAG, "libvoiceconfig.so 加载成功");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "libvoiceconfig.so 加载失败，使用内置默认值");
        }
    }

    // ── Native 方法声明 ──────────────────────────────────
    private static native String nativeGetCommentPath();
    private static native String nativeGetImPath();
    private static native String nativeGetDouyinPackage();
    public  static native int    nativeReplaceFile(String srcPath, String dstPath);

    // ── 带 SharedPreferences 覆盖的公开接口 ────────────────

    /** 获取评论语音目录（优先用户自定义 > so内置 > 硬编码降级）*/
    public static String getCommentPath(Context ctx) {
        String custom = prefs(ctx).getString(KEY_COMMENT_PATH, null);
        if (custom != null && !custom.trim().isEmpty()) return custom.trim();
        return getDefaultCommentPath();
    }

    /** 获取私聊语音目录 */
    public static String getImPath(Context ctx) {
        String custom = prefs(ctx).getString(KEY_IM_PATH, null);
        if (custom != null && !custom.trim().isEmpty()) return custom.trim();
        return getDefaultImPath();
    }

    /** 从 .so 读取评论默认路径 */
    public static String getDefaultCommentPath() {
        if (soLoaded) {
            try { return nativeGetCommentPath(); } catch (Throwable ignore) {}
        }
        return "/data/data/com.ss.android.ugc.aweme/files/comment/audio";
    }

    /** 从 .so 读取私聊默认路径 */
    public static String getDefaultImPath() {
        if (soLoaded) {
            try { return nativeGetImPath(); } catch (Throwable ignore) {}
        }
        return "/data/data/com.ss.android.ugc.aweme/files/im";
    }

    /** 获取抖音包名 */
    public static String getDouyinPackage() {
        if (soLoaded) {
            try { return nativeGetDouyinPackage(); } catch (Throwable ignore) {}
        }
        return "com.ss.android.ugc.aweme";
    }

    /** 保存自定义路径 */
    public static void saveCustomPaths(Context ctx, String commentPath, String imPath) {
        prefs(ctx).edit()
                .putString(KEY_COMMENT_PATH, commentPath)
                .putString(KEY_IM_PATH, imPath)
                .apply();
    }

    /** 保存已选音频信息 */
    public static void saveAudioInfo(Context ctx, String uri, String name) {
        prefs(ctx).edit()
                .putString(KEY_AUDIO_URI, uri)
                .putString(KEY_AUDIO_NAME, name)
                .apply();
    }

    /** 获取已选音频 URI 字符串 */
    public static String getSavedAudioUri(Context ctx) {
        return prefs(ctx).getString(KEY_AUDIO_URI, null);
    }

    /** 获取已选音频文件名 */
    public static String getSavedAudioName(Context ctx) {
        return prefs(ctx).getString(KEY_AUDIO_NAME, null);
    }

    /** 检测 Root（同步，请在子线程调用）*/
    public static boolean checkRoot() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            p.destroy();
            return line != null && line.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private NativeConfig() {}
}
