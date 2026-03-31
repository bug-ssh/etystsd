package com.voice.assistant;

import android.content.Context;
import android.content.SharedPreferences;

public final class NativeConfig {
    private static final String PREFS = "voice_assistant_prefs";
    private static final String KEY_COMMENT_PATH = "comment_path";
    private static final String KEY_IM_PATH = "im_path";
    private static final String KEY_SAVED_AUDIO_NAME = "saved_audio_name";

    static {
        System.loadLibrary("voice_config");
    }

    private NativeConfig() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getCommentPath(Context context) {
        String path = prefs(context).getString(KEY_COMMENT_PATH, null);
        return path != null ? path : getCommentAudioDir();
    }

    public static String getImPath(Context context) {
        String path = prefs(context).getString(KEY_IM_PATH, null);
        return path != null ? path : getImAudioDir();
    }

    public static String getSavedAudioName(Context context) {
        return prefs(context).getString(KEY_SAVED_AUDIO_NAME, "");
    }

    public static void setCommentPath(Context context, String path) {
        prefs(context).edit().putString(KEY_COMMENT_PATH, path).apply();
    }

    public static void setImPath(Context context, String path) {
        prefs(context).edit().putString(KEY_IM_PATH, path).apply();
    }

    public static void setSavedAudioName(Context context, String name) {
        prefs(context).edit().putString(KEY_SAVED_AUDIO_NAME, name).apply();
    }

    public static native String getCommentAudioDir();
    public static native String getImAudioDir();
    public static native String getTargetPackage();
    public static native String getRootCheckCommand();
    public static native String getRootCopyCommandTemplate();
    public static native String getRootListCommandTemplate();
    public static native int getMinAudioDuration();
    public static native int getMaxAudioDuration();
    public static native int getFloatWindowSizeDp();
    public static native float getFloatWindowAlpha();
    public static native boolean isPathAllowed(String path);
    public static native String buildCopyCommand(String sourcePath, String targetPath);
    public static native String buildListCommand(String dirPath);
}
