package com.voice.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Core config: paths stored with XOR encoding in libvoiceconfig.so.
 * Custom paths override defaults via SharedPreferences.
 */
public final class NativeConfig {

    private static final String TAG  = "NativeConfig";
    private static final String PREF = "voice_prefs";

    public static final String KEY_COMMENT_PATH   = "custom_comment_path";
    public static final String KEY_IM_PATH        = "custom_im_path";
    public static final String KEY_AUDIO_URI      = "selected_audio_uri";
    public static final String KEY_AUDIO_NAME     = "selected_audio_name";
    public static final String KEY_TARGET_FILE    = "target_file_path";
    public static final String KEY_BG_URI         = "custom_bg_uri";
    public static final String KEY_FONT_SIZE      = "font_size";

    // Font size presets
    public static final int FONT_SMALL  = 0;
    public static final int FONT_MEDIUM = 1;
    public static final int FONT_LARGE  = 2;

    private static boolean soLoaded = false;

    static {
        try {
            System.loadLibrary("voiceconfig");
            soLoaded = true;
            Log.i(TAG, "libvoiceconfig.so loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "libvoiceconfig.so load failed, using built-in defaults");
        }
    }

    // ── Native method declarations ──────────────────────
    private static native String nativeGetCommentPath();
    private static native String nativeGetImPath();
    private static native String nativeGetDouyinPackage();
    public  static native int    nativeReplaceFile(String srcPath, String dstPath);
    public  static native boolean nativeCheckRoot();
    public  static native String[] nativeListDir(String dir);

    // ── Public API with SharedPreferences override ──────

    /** Comment voice directory (custom > so default > hardcoded fallback) */
    public static String getCommentPath(Context ctx) {
        String custom = prefs(ctx).getString(KEY_COMMENT_PATH, null);
        if (custom != null && !custom.trim().isEmpty()) return custom.trim();
        return getDefaultCommentPath();
    }

    /** IM voice directory */
    public static String getImPath(Context ctx) {
        String custom = prefs(ctx).getString(KEY_IM_PATH, null);
        if (custom != null && !custom.trim().isEmpty()) return custom.trim();
        return getDefaultImPath();
    }

    /** Default comment path from .so */
    public static String getDefaultCommentPath() {
        if (soLoaded) {
            try { return nativeGetCommentPath(); } catch (Throwable ignore) {}
        }
        return "/data/data/com.ss.android.ugc.aweme/files/comment/audio";
    }

    /** Default IM path from .so */
    public static String getDefaultImPath() {
        if (soLoaded) {
            try { return nativeGetImPath(); } catch (Throwable ignore) {}
        }
        return "/data/data/com.ss.android.ugc.aweme/files/im";
    }

    /** Douyin package name */
    public static String getDouyinPackage() {
        if (soLoaded) {
            try { return nativeGetDouyinPackage(); } catch (Throwable ignore) {}
        }
        return "com.ss.android.ugc.aweme";
    }

    /** Check Root via native (runs su -c id, checks uid=0) */
    public static boolean checkRoot() {
        if (soLoaded) {
            try { return nativeCheckRoot(); } catch (Throwable ignore) {}
        }
        // Fallback Java check
        return checkRootJava();
    }

    /** List files in directory via Root shell */
    public static String[] listDir(String dir) {
        if (soLoaded) {
            try { return nativeListDir(dir); } catch (Throwable ignore) {}
        }
        // Fallback
        return listDirJava(dir);
    }

    private static boolean checkRootJava() {
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

    private static String[] listDirJava(String dir) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                    "ls -t \"" + dir + "\" 2>/dev/null"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            java.util.List<String> list = new java.util.ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) list.add(line);
            }
            p.waitFor();
            p.destroy();
            return list.toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }

    /** Save custom paths */
    public static void saveCustomPaths(Context ctx, String commentPath, String imPath) {
        prefs(ctx).edit()
                .putString(KEY_COMMENT_PATH, commentPath)
                .putString(KEY_IM_PATH, imPath)
                .apply();
    }

    /** Save selected audio info */
    public static void saveAudioInfo(Context ctx, String uri, String name) {
        prefs(ctx).edit()
                .putString(KEY_AUDIO_URI, uri)
                .putString(KEY_AUDIO_NAME, name)
                .apply();
    }

    /** Save target file path for replacement */
    public static void saveTargetFile(Context ctx, String path) {
        prefs(ctx).edit().putString(KEY_TARGET_FILE, path).apply();
    }

    public static String getTargetFile(Context ctx) {
        return prefs(ctx).getString(KEY_TARGET_FILE, null);
    }

    public static String getSavedAudioUri(Context ctx) {
        return prefs(ctx).getString(KEY_AUDIO_URI, null);
    }

    public static String getSavedAudioName(Context ctx) {
        return prefs(ctx).getString(KEY_AUDIO_NAME, null);
    }

    /** Save custom background URI */
    public static void saveBackgroundUri(Context ctx, String uri) {
        prefs(ctx).edit().putString(KEY_BG_URI, uri).apply();
    }

    public static String getBackgroundUri(Context ctx) {
        return prefs(ctx).getString(KEY_BG_URI, null);
    }

    /** Save font size preset (0=small, 1=medium, 2=large) */
    public static void saveFontSize(Context ctx, int size) {
        prefs(ctx).edit().putInt(KEY_FONT_SIZE, size).apply();
    }

    public static int getFontSize(Context ctx) {
        return prefs(ctx).getInt(KEY_FONT_SIZE, FONT_MEDIUM);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private NativeConfig() {}
}
