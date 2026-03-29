#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "VoiceConfig"

// ── XOR 解码（key = 0x5A）── //
static std::string xorDecode(const unsigned char* data, int len) {
    const unsigned char KEY = 0x5A;
    std::string result;
    result.reserve(len);
    for (int i = 0; i < len; i++) {
        result += static_cast<char>(data[i] ^ KEY);
    }
    return result;
}

// /data/data/com.ss.android.ugc.aweme/files/comment/audio
static const unsigned char COMMENT_PATH[] = {
    0x75, 0x3E, 0x3B, 0x2E, 0x3B, 0x75, 0x3E, 0x3B, 0x2E, 0x3B, 0x75,
    0x39, 0x35, 0x37, 0x74, 0x29, 0x29, 0x74, 0x3B, 0x34, 0x3E, 0x28,
    0x35, 0x33, 0x3E, 0x74, 0x2F, 0x3D, 0x39, 0x74, 0x3B, 0x2D, 0x3F,
    0x37, 0x3F, 0x75, 0x3C, 0x33, 0x36, 0x3F, 0x29, 0x75, 0x39, 0x35,
    0x37, 0x37, 0x3F, 0x34, 0x2E, 0x75, 0x3B, 0x2F, 0x3E, 0x33, 0x35
};

// /data/data/com.ss.android.ugc.aweme/files/im
static const unsigned char IM_PATH[] = {
    0x75, 0x3E, 0x3B, 0x2E, 0x3B, 0x75, 0x3E, 0x3B, 0x2E, 0x3B, 0x75,
    0x39, 0x35, 0x37, 0x74, 0x29, 0x29, 0x74, 0x3B, 0x34, 0x3E, 0x28,
    0x35, 0x33, 0x3E, 0x74, 0x2F, 0x3D, 0x39, 0x74, 0x3B, 0x2D, 0x3F,
    0x37, 0x3F, 0x75, 0x3C, 0x33, 0x36, 0x3F, 0x29, 0x75, 0x33, 0x37
};

// com.ss.android.ugc.aweme
static const unsigned char PKG_PATH[] = {
    0x39, 0x35, 0x37, 0x74, 0x29, 0x29, 0x74, 0x3B, 0x34, 0x3E, 0x28,
    0x35, 0x33, 0x3E, 0x74, 0x2F, 0x3D, 0x39, 0x74, 0x3B, 0x2D, 0x3F,
    0x37, 0x3F
};

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_nativeGetCommentPath(JNIEnv *env, jclass) {
    std::string path = xorDecode(COMMENT_PATH, sizeof(COMMENT_PATH));
    return env->NewStringUTF(path.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_nativeGetImPath(JNIEnv *env, jclass) {
    std::string path = xorDecode(IM_PATH, sizeof(IM_PATH));
    return env->NewStringUTF(path.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_nativeGetDouyinPackage(JNIEnv *env, jclass) {
    std::string pkg = xorDecode(PKG_PATH, sizeof(PKG_PATH));
    return env->NewStringUTF(pkg.c_str());
}

JNIEXPORT jint JNICALL
Java_com_voice_assistant_NativeConfig_nativeReplaceFile(
        JNIEnv *env, jclass,
        jstring jSrcPath, jstring jDstPath) {
    const char* src = env->GetStringUTFChars(jSrcPath, nullptr);
    const char* dst = env->GetStringUTFChars(jDstPath, nullptr);

    // 构造 cp + chmod 命令，通过 popen 在 root shell 执行
    std::string cmd = std::string("su -c \"cp -f '") + src + "' '" + dst +
                      "' && chmod 644 '" + dst + "'\" 2>/dev/null";
    int ret = system(cmd.c_str());

    __android_log_print(ANDROID_LOG_DEBUG, TAG,
                        "nativeReplaceFile: src=%s dst=%s ret=%d", src, dst, ret);

    env->ReleaseStringUTFChars(jSrcPath, src);
    env->ReleaseStringUTFChars(jDstPath, dst);
    return ret;
}

} // extern "C"
