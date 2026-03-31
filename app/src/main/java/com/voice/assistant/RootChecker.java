package com.voice.assistant;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Root permission checker and file operations via su.
 */
public class RootChecker {
    private static final String TAG = "RootChecker";

    public interface RootCallback {
        void onResult(boolean hasRoot, String message);
    }

    /**
     * Check if device has root access.
     */
    public static boolean checkRoot() {
        // Method 1: Check for su binary
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su",
                "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/data/adb/ksu/bin/su",
                "/data/adb/ap/bin/su"};
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }

        // Method 2: Try running su command
        try {
            Process process = Runtime.getRuntime().exec(NativeConfig.getRootCheckCommand());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            reader.close();
            if (line != null && line.contains("uid=0")) {
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Root check via su failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check root access asynchronously.
     */
    public static void checkRootAsync(RootCallback callback) {
        new Thread(() -> {
            boolean hasRoot = checkRoot();
            String msg = hasRoot ? "Root权限已获取" : "未检测到Root权限";
            if (callback != null) {
                callback.onResult(hasRoot, msg);
            }
        }).start();
    }

    /**
     * Execute a root command and return the output.
     */
    public static CommandResult executeRootCommand(String command) {
        CommandResult result = new CommandResult();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }

            result.exitCode = process.waitFor();
            result.output = output.toString().trim();
            result.error = error.toString().trim();
            result.success = (result.exitCode == 0);

            reader.close();
            errorReader.close();
            os.close();
        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
            Log.e(TAG, "Root command failed: " + command, e);
        }
        return result;
    }

    public static class CommandResult {
        public boolean success = false;
        public int exitCode = -1;
        public String output = "";
        public String error = "";
    }
}
