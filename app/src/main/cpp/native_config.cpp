#include <jni.h>
#include <string>

static const char* DEFAULT_COMMENT_AUDIO_DIR = "/data/data/com.ss.android.ugc.aweme/files/comment/audio";
static const char* DEFAULT_IM_AUDIO_DIR = "/data/data/com.ss.android.ugc.aweme/files/im";
static const char* TARGET_PACKAGE = "com.ss.android.ugc.aweme";
static const char* ROOT_CHECK_CMD = "su -c 'id'";
static const char* ROOT_COPY_CMD_TEMPLATE = "su -c 'cp \"%s\" \"%s\" && chmod 666 \"%s\"'";
static const char* ROOT_LIST_CMD_TEMPLATE = "su -c 'ls -la \"%s\"'";
static const int MIN_AUDIO_DURATION_SEC = 1;
static const int MAX_AUDIO_DURATION_SEC = 30;
static const int FLOAT_WINDOW_SIZE_DP = 56;
static const float FLOAT_WINDOW_ALPHA = 0.92f;

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getCommentAudioDir(JNIEnv* env, jclass) {
    return env->NewStringUTF(DEFAULT_COMMENT_AUDIO_DIR);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getImAudioDir(JNIEnv* env, jclass) {
    return env->NewStringUTF(DEFAULT_IM_AUDIO_DIR);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getTargetPackage(JNIEnv* env, jclass) {
    return env->NewStringUTF(TARGET_PACKAGE);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getRootCheckCommand(JNIEnv* env, jclass) {
    return env->NewStringUTF(ROOT_CHECK_CMD);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getRootCopyCommandTemplate(JNIEnv* env, jclass) {
    return env->NewStringUTF(ROOT_COPY_CMD_TEMPLATE);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_getRootListCommandTemplate(JNIEnv* env, jclass) {
    return env->NewStringUTF(ROOT_LIST_CMD_TEMPLATE);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_voice_assistant_NativeConfig_getMinAudioDuration(JNIEnv*, jclass) {
    return MIN_AUDIO_DURATION_SEC;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_voice_assistant_NativeConfig_getMaxAudioDuration(JNIEnv*, jclass) {
    return MAX_AUDIO_DURATION_SEC;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_voice_assistant_NativeConfig_getFloatWindowSizeDp(JNIEnv*, jclass) {
    return FLOAT_WINDOW_SIZE_DP;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_voice_assistant_NativeConfig_getFloatWindowAlpha(JNIEnv*, jclass) {
    return FLOAT_WINDOW_ALPHA;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_voice_assistant_NativeConfig_isPathAllowed(JNIEnv* env, jclass, jstring path) {
    if (path == nullptr) return JNI_FALSE;
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    bool ok = cpath && (std::string(cpath).find(TARGET_PACKAGE) != std::string::npos);
    if (cpath) env->ReleaseStringUTFChars(path, cpath);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_buildCopyCommand(JNIEnv* env, jclass, jstring sourcePath, jstring targetPath) {
    const char* src = sourcePath ? env->GetStringUTFChars(sourcePath, nullptr) : nullptr;
    const char* dst = targetPath ? env->GetStringUTFChars(targetPath, nullptr) : nullptr;
    std::string cmd = "";
    if (src && dst) {
        cmd = "su -c 'cp \"" + std::string(src) + "\" \"" + std::string(dst) + "\" && chmod 666 \"" + std::string(dst) + "\"'";
    }
    if (src) env->ReleaseStringUTFChars(sourcePath, src);
    if (dst) env->ReleaseStringUTFChars(targetPath, dst);
    return env->NewStringUTF(cmd.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_voice_assistant_NativeConfig_buildListCommand(JNIEnv* env, jclass, jstring dirPath) {
    const char* dir = dirPath ? env->GetStringUTFChars(dirPath, nullptr) : nullptr;
    std::string cmd = "";
    if (dir) {
        cmd = "su -c 'ls -la \"" + std::string(dir) + "\"'";
    }
    if (dir) env->ReleaseStringUTFChars(dirPath, dir);
    return env->NewStringUTF(cmd.c_str());
}
