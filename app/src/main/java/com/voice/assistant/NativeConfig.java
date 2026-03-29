package com.voice.assistant;

import android.util.Base64;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;

/**
 * 核心配置：路径以 Base64 编码存储，Java 层无明文
 * 文件替换操作通过 Root Shell 执行
 */
public final class NativeConfig {

    // Base64 编码后的路径（可自定义混淆）
    private static final String COMMENT_PATH_B64 = "L2RhdGEvZGF0YS9jb20uc3MuYW5kcm9qc2lkLnVnYy5hd2VtZS9maWxlcy9jb21tZW50L2F1ZGlv";
    private static final String IM_PATH_B64 = "L2RhdGEvZGF0YS9jb20uc3MuYW5kcm9pZC51Z2MuYXdlbWUvZmlsZXMvaW0=";
    private static final String DOUYIN_PKG_B64 = "Y29tLnNzdGFiLmFuZHJvaWQudWdjLmF3ZW1l";

    /**
     * 获取评论语音目录（Base64 解码）
     */
    public static String getCommentPath() {
        return new String(Base64.decode(COMMENT_PATH_B64, Base64.DEFAULT));
    }

    /**
     * 获取私聊语音目录（Base64 解码）
     */
    public static String getImPath() {
        return new String(Base64.decode(IM_PATH_B64, Base64.DEFAULT));
    }

    /**
     * 获取抖音包名（Base64 解码）
     */
    public static String getDouyinPackage() {
        return new String(Base64.decode(DOUYIN_PKG_B64, Base64.DEFAULT));
    }

    /**
     * 扫描目录，返回最新修改的文件完整路径
     */
    public static String findLatestFile(String dir) {
        try {
            File directory = new File(dir);
            if (!directory.exists() || !directory.isDirectory()) {
                return null;
            }

            File[] files = directory.listFiles();
            if (files == null || files.length == 0) {
                return null;
            }

            File latest = null;
            long latestTime = 0;

            for (File f : files) {
                if (f.isFile() && f.lastModified() > latestTime) {
                    latestTime = f.lastModified();
                    latest = f;
                }
            }

            return latest != null ? latest.getAbsolutePath() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行文件替换（Root Shell）
     * 返回 0 = 成功，其他 = 失败
     */
    public static int nativeReplaceFile(String srcPath, String targetPath) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            // 复制文件 + 修复权限
            os.writeBytes("cp -f " + srcPath + " " + targetPath + "\n");
            os.writeBytes("chmod 644 " + targetPath + "\n");
            os.writeBytes("exit\n");
            os.flush();

            int result = process.waitFor();
            process.destroy();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 执行 Shell 命令，返回输出
     */
    public static String execShell(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            os.writeBytes(command + "\nexit\n");
            os.flush();

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            process.waitFor();
            process.destroy();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private NativeConfig() {}
}
