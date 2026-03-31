package com.voice.assistant;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Handles the actual voice file replacement via root commands.
 */
public class RootReplacer {
    private static final String TAG = "RootReplacer";
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ReplaceCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public RootReplacer(Context context) {
        this.context = context;
    }

    /**
     * Replace a target file with the source file using root access.
     * @param sourcePath Path to the audio file to use as replacement
     * @param targetPath Path to the target file to be replaced
     * @param callback Callback for result
     */
    public void replaceFile(String sourcePath, String targetPath, ReplaceCallback callback) {
        new Thread(() -> {
            try {
                // Validate paths
                if (sourcePath == null || sourcePath.isEmpty()) {
                    postFailure(callback, "源文件路径为空");
                    return;
                }
                if (targetPath == null || targetPath.isEmpty()) {
                    postFailure(callback, "目标文件路径为空");
                    return;
                }

                // Check source file exists (might be app-local or sdcard)
                File sourceFile = new File(sourcePath);
                if (!sourceFile.exists()) {
                    postFailure(callback, "源文件不存在: " + sourcePath);
                    return;
                }

                // Validate target path via native check
                if (!NativeConfig.isPathAllowed(targetPath)) {
                    postFailure(callback, "目标路径不在允许范围内: " + targetPath);
                    return;
                }

                // Check root
                if (!RootChecker.checkRoot()) {
                    postFailure(callback, "未获取Root权限，无法替换文件");
                    return;
                }

                // Build and execute copy command via native
                String copyCmd = NativeConfig.buildCopyCommand(sourcePath, targetPath);
                Log.d(TAG, "Executing: " + copyCmd);

                RootChecker.CommandResult result = RootChecker.executeRootCommand(copyCmd);

                if (result.success) {
                    postSuccess(callback, "替换成功！\n" + targetPath);
                } else {
                    postFailure(callback, "替换失败: " + result.error);
                }

            } catch (Exception e) {
                Log.e(TAG, "Replace error", e);
                postFailure(callback, "替换出错: " + e.getMessage());
            }
        }).start();
    }

    /**
     * List files in a directory via root.
     */
    public void listFiles(String dirPath, ReplaceCallback callback) {
        new Thread(() -> {
            try {
                if (!RootChecker.checkRoot()) {
                    postFailure(callback, "未获取Root权限");
                    return;
                }

                String listCmd = NativeConfig.buildListCommand(dirPath);
                RootChecker.CommandResult result = RootChecker.executeRootCommand(listCmd);

                if (result.success) {
                    postSuccess(callback, result.output);
                } else {
                    postFailure(callback, "无法列出文件: " + result.error);
                }
            } catch (Exception e) {
                postFailure(callback, "出错: " + e.getMessage());
            }
        }).start();
    }

    private void postSuccess(ReplaceCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(message));
        }
    }

    private void postFailure(ReplaceCallback callback, String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onFailure(error));
        }
    }
}
