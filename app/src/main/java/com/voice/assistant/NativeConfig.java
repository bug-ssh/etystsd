package com.voice.assistant;

/**
 * JNI 桥：所有核心配置从 libvoiceconfig.so 读取
 * Java 层无任何明文路径字符串
 */
public final class NativeConfig {

    static {
        System.loadLibrary("voiceconfig");
    }

    // 从 .so 获取评论语音目录
    public static native String getCommentPath();

    // 从 .so 获取私聊语音目录
    public static native String getImPath();

    // 从 .so 获取抖音包名
    public static native String getDouyinPackage();

    // native 层执行文件替换（Root）
    // 返回 0 = 成功
    public static native int nativeReplaceFile(String srcPath, String targetPath);

    // native 层扫描目录，返回最新文件完整路径
    public static native String findLatestFile(String dir);

    private NativeConfig() {}
}
