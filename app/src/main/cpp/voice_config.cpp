#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>

#define TAG "VoiceNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ============================================================
//  核心配置 — 存储在 .so 中，Java层无明文字符串
//  路径通过简单的字节偏移拼接，防止直接 strings 扫描
// ============================================================

// 将字节数组还原为路径字符串（防止明文扫描）
static std::string decodeBytes(const unsigned char* data, int len) {
    std::string result;
    result.reserve(len);
    for (int i = 0; i < len; i++) {
        result += (char)(data[i] ^ 0x00); // XOR 0 = 原值，可换成其他key
    }
    return result;
}

// 评论语音目录（字节编码存储）
// /data/data/com.ss.android.ugc.aweme/files/comment/audio
static const unsigned char PATH_COMMENT_BYTES[] = {
    0x2f,0x64,0x61,0x74,0x61,0x2f,0x64,0x61,0x74,0x61,0x2f,
    0x63,0x6f,0x6d,0x2e,0x73,0x73,0x2e,0x61,0x6e,0x64,0x72,
    0x6f,0x69,0x64,0x2e,0x75,0x67,0x63,0x2e,0x61,0x77,0x65,
    0x6d,0x65,0x2f,0x66,0x69,0x6c,0x65,0x73,0x2f,0x63,0x6f,
    0x6d,0x6d,0x65,0x6e,0x74,0x2f,0x61,0x75,0x64,0x69,0x6f
};

// 私聊语音目录（字节编码存储）
// /data/data/com.ss.android.ugc.aweme/files/im
static const unsigned char PATH_IM_BYTES[] = {
    0x2f,0x64,0x61,0x74,0x61,0x2f,0x64,0x61,0x74,0x61,0x2f,
    0x63,0x6f,0x6d,0x2e,0x73,0x73,0x2e,0x61,0x6e,0x64,0x72,
    0x6f,0x69,0x64,0x2e,0x75,0x67,0x63,0x2e,0x61,0x77,0x65,
    0x6d,0x65,0x2f,0x66,0x69,0x6c,0x65,0x73,0x2f,0x69,0x6d
};

// 包名（字节编码）
// com.ss.android.ugc.aweme
static const unsigned char PKG_BYTES[] = {
    0x63,0x6f,0x6d,0x2e,0x73,0x73,0x2e,0x61,0x6e,0x64,0x72,
    0x6f,0x69,0x64,0x2e,0x75,0x67,0x63,0x2e,0x61,0x77,0x65,0x6d,0x65
};

// ============================================================
//  JNI 接口
// ============================================================

extern "C" {

// 获取评论语音目录
JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getCommentPath(
        JNIEnv* env, jclass /*clazz*/) {
    std::string path = decodeBytes(PATH_COMMENT_BYTES, sizeof(PATH_COMMENT_BYTES));
    LOGI("getCommentPath: %s", path.c_str());
    return env->NewStringUTF(path.c_str());
}

// 获取私聊语音目录
JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getImPath(
        JNIEnv* env, jclass /*clazz*/) {
    std::string path = decodeBytes(PATH_IM_BYTES, sizeof(PATH_IM_BYTES));
    LOGI("getImPath: %s", path.c_str());
    return env->NewStringUTF(path.c_str());
}

// 获取抖音包名
JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getDouyinPackage(
        JNIEnv* env, jclass /*clazz*/) {
    std::string pkg = decodeBytes(PKG_BYTES, sizeof(PKG_BYTES));
    return env->NewStringUTF(pkg.c_str());
}

// 执行 Root Shell 替换命令（全程在 native 层，不经 Java）
// srcPath: 源文件路径 (APP cache)
// targetPath: 目标文件路径 (抖音语音文件)
// 返回: 0=成功, <0=失败
JNIEXPORT jint JNICALL
Java_com_voice_assistant_NativeConfig_nativeReplaceFile(
        JNIEnv* env, jclass /*clazz*/,
        jstring jSrcPath, jstring jTargetPath) {

    const char* src    = env->GetStringUTFChars(jSrcPath,    nullptr);
    const char* target = env->GetStringUTFChars(jTargetPath, nullptr);

    if (!src || !target) {
        LOGE("nativeReplaceFile: null path");
        return -1;
    }

    // 构造 shell 命令
    // 格式: su -c "cp -f 'src' 'target' && chmod 644 'target'"
    std::string cmd = "su -c \"cp -f '";
    cmd += src;
    cmd += "' '";
    cmd += target;
    cmd += "' && chmod 644 '";
    cmd += target;
    cmd += "'\"";

    LOGI("nativeReplaceFile cmd: %s", cmd.c_str());
    int ret = system(cmd.c_str());
    LOGI("nativeReplaceFile result: %d", ret);

    env->ReleaseStringUTFChars(jSrcPath,    src);
    env->ReleaseStringUTFChars(jTargetPath, target);

    return ret;
}

// 扫描目录，获取最新文件的完整路径（native层执行，结果回传Java）
JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_findLatestFile(
        JNIEnv* env, jclass /*clazz*/, jstring jDir) {

    const char* dir = env->GetStringUTFChars(jDir, nullptr);
    if (!dir) return nullptr;

    // 用 shell ls -t | head -1 获取最新文件名
    std::string cmd = "su -c \"ls -t '";
    cmd += dir;
    cmd += "' 2>/dev/null | head -1\"";

    FILE* pipe = popen(cmd.c_str(), "r");
    if (!pipe) {
        env->ReleaseStringUTFChars(jDir, dir);
        return nullptr;
    }

    char buf[512] = {0};
    if (fgets(buf, sizeof(buf), pipe) == nullptr) {
        pclose(pipe);
        env->ReleaseStringUTFChars(jDir, dir);
        return nullptr;
    }
    pclose(pipe);

    // 去掉末尾换行
    size_t len = strlen(buf);
    while (len > 0 && (buf[len-1] == '\n' || buf[len-1] == '\r' || buf[len-1] == ' '))
        buf[--len] = '\0';

    if (len == 0) {
        env->ReleaseStringUTFChars(jDir, dir);
        return nullptr;
    }

    // 拼接完整路径
    std::string fullPath = std::string(dir) + "/" + buf;
    LOGI("findLatestFile: %s", fullPath.c_str());

    env->ReleaseStringUTFChars(jDir, dir);
    return env->NewStringUTF(fullPath.c_str());
}

} // extern "C"
