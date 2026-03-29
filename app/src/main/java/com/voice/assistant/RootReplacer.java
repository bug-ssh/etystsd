package com.voice.assistant;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Root 权限语音文件替换工具
 * 流程：
 *  1. 将用户选择的音频拷贝到应用私有目录（/data/data/com.voice.assistant/cache/replace_audio.amr）
 *  2. 扫描目标目录，找到最新的语音文件（抖音录制后会生成 .amr 或 .opus 文件）
 *  3. 通过 Root Shell 把我们的文件覆盖进去
 *  4. 修复权限，让抖音可以正常读取
 */
public class RootReplacer {

    private static final String TAG = "RootReplacer";
    private static final String CACHE_NAME = "replace_audio";

    public static boolean replaceLatestVoice(Context ctx, Uri sourceUri, String targetDir) {
        try {
            // 1. 拷贝选择的音频到缓存
            File cacheFile = copyToCacheDir(ctx, sourceUri);
            if (cacheFile == null) {
                Log.e(TAG, "缓存文件复制失败");
                return false;
            }

            // 2. 扫描目标目录，找最新文件
            String latestFile = findLatestFileInDir(targetDir);
            if (latestFile == null) {
                Log.e(TAG, "目标目录无语音文件: " + targetDir);
                return false;
            }

            String srcPath = cacheFile.getAbsolutePath();
            Log.d(TAG, "替换目标: " + latestFile);
            Log.d(TAG, "来源文件: " + srcPath);

            // 3. Root 覆盖命令
            String[] commands = {
                    "mount -o rw,remount /data || true",
                    "cp -f \"" + srcPath + "\" \"" + latestFile + "\"",
                    "chmod 644 \"" + latestFile + "\"",
                    "chown u0_a$(stat -c %U /data/data/com.ss.android.ugc.aweme | sed 's/[^0-9]//g') \"" + latestFile + "\" || true"
            };

            return execRootCommands(commands);

        } catch (Exception e) {
            Log.e(TAG, "替换异常: " + e.getMessage(), e);
            return false;
        }
    }

    /** 将 Uri 对应的文件拷贝到私有缓存，返回缓存文件 */
    private static File copyToCacheDir(Context ctx, Uri uri) {
        try {
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;

            File cacheDir = ctx.getCacheDir();
            // 保留扩展名，抖音文件通常是 .amr/.opus
            String ext = ".amr";
            String uriStr = uri.toString().toLowerCase();
            if (uriStr.contains(".opus")) ext = ".opus";
            else if (uriStr.contains(".aac"))  ext = ".aac";
            else if (uriStr.contains(".mp3"))  ext = ".mp3";
            else if (uriStr.contains(".wav"))  ext = ".wav";
            else if (uriStr.contains(".m4a"))  ext = ".m4a";

            File dest = new File(cacheDir, CACHE_NAME + ext);
            OutputStream out = new FileOutputStream(dest);

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();

            return dest;
        } catch (IOException e) {
            Log.e(TAG, "copyToCacheDir error: " + e.getMessage());
            return null;
        }
    }

    /** 通过 Root Shell 列出目录中最新的文件 */
    private static String findLatestFileInDir(String dir) {
        try {
            // 使用 ls -t 列出最新文件
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                    "ls -t \"" + dir + "\" 2>/dev/null | head -1"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String fileName = br.readLine();
            br.close();
            p.waitFor();

            if (fileName == null || fileName.trim().isEmpty()) {
                return null;
            }
            return dir + "/" + fileName.trim();
        } catch (Exception e) {
            Log.e(TAG, "findLatestFileInDir error: " + e.getMessage());
            return null;
        }
    }

    /** 以 Root 权限执行一组 Shell 命令 */
    private static boolean execRootCommands(String[] commands) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            OutputStream os = process.getOutputStream();
            for (String cmd : commands) {
                os.write((cmd + "\n").getBytes());
            }
            os.write("exit\n".getBytes());
            os.flush();
            os.close();

            // 读取错误输出
            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            StringBuilder err = new StringBuilder();
            String line;
            while ((line = errReader.readLine()) != null) {
                err.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            Log.d(TAG, "Root 命令执行完毕，退出码: " + exitCode);
            if (err.length() > 0) {
                Log.w(TAG, "Stderr: " + err);
            }
            return exitCode == 0;
        } catch (Exception e) {
            Log.e(TAG, "execRootCommands error: " + e.getMessage(), e);
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
