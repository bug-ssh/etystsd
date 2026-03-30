#include <jni.h>
#include <string>
#include <cstdio>
#include <cstdlib>
#include <android/log.h>

#define TAG "VoiceConfig"

// ── XOR obfuscation (key = 0x5A) ── //
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

// XOR-encoded "su" (0x29='s'^0x5A, 0x2F='u'^0x5A)
static const unsigned char SU_BIN[] = { 0x29, 0x2F };

// XOR-encoded "/system/bin/su"
static const unsigned char SU_PATH1[] = {
    0x75, 0x29, 0x2D, 0x29, 0x2E, 0x3F, 0x37, 0x75, 0x38, 0x33, 0x34, 0x75, 0x29, 0x2F
};

// XOR-encoded "/system/xbin/su"
static const unsigned char SU_PATH2[] = {
    0x75, 0x29, 0x2D, 0x29, 0x2E, 0x3F, 0x37, 0x75, 0x22, 0x38, 0x33, 0x34, 0x75, 0x29, 0x2F
};

// XOR-encoded "/sbin/su"
static const unsigned char SU_PATH3[] = {
    0x75, 0x29, 0x38, 0x33, 0x34, 0x75, 0x29, 0x2F
};

// XOR-encoded "/vendor/bin/su"
static const unsigned char SU_PATH4[] = {
    0x75, 0x2C, 0x3F, 0x34, 0x3E, 0x35, 0x28, 0x75, 0x38, 0x33, 0x34, 0x75, 0x29, 0x2F
};

// XOR-encoded "uid=0"
static const unsigned char UID_ROOT[] = { 0x2F, 0x33, 0x3E, 0x7B, 0x6A };

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

JNIEXPORT jboolean JNICALL
Java_com_voice_assistant_NativeConfig_nativeCheckRoot(JNIEnv *env, jclass) {
    // Check multiple su binary locations
    const unsigned char* paths[] = { SU_PATH1, SU_PATH2, SU_PATH3, SU_PATH4 };
    const int sizes[] = {
        (int)sizeof(SU_PATH1), (int)sizeof(SU_PATH2),
        (int)sizeof(SU_PATH3), (int)sizeof(SU_PATH4)
    };

    for (int i = 0; i < 4; i++) {
        std::string p = xorDecode(paths[i], sizes[i]);
        FILE* f = fopen(p.c_str(), "r");
        if (f) {
            fclose(f);
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Found su at: %s", p.c_str());
        }
    }

    // Try executing su -c id and check for uid=0
    std::string su = xorDecode(SU_BIN, sizeof(SU_BIN));
    std::string cmd = su + " -c id";
    FILE* pipe = popen(cmd.c_str(), "r");
    if (pipe) {
        char buf[256] = {0};
        if (fgets(buf, sizeof(buf), pipe) != nullptr) {
            pclose(pipe);
            std::string output(buf);
            std::string uid0 = xorDecode(UID_ROOT, sizeof(UID_ROOT));
            if (output.find(uid0) != std::string::npos) {
                return JNI_TRUE;
            }
        } else {
            pclose(pipe);
        }
    }
    return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_voice_assistant_NativeConfig_nativeReplaceFile(
        JNIEnv *env, jclass,
        jstring jSrcPath, jstring jDstPath) {
    const char* src = env->GetStringUTFChars(jSrcPath, nullptr);
    const char* dst = env->GetStringUTFChars(jDstPath, nullptr);

    std::string su = xorDecode(SU_BIN, sizeof(SU_BIN));

    // Build command: su -c "cp -f 'src' 'dst' && chmod 644 'dst' && chown $(stat ... 'dst') 'dst'"
    std::string cmd = su + " -c \"cp -f '" + src + "' '" + dst +
                      "' && chmod 644 '" + dst + "'\" 2>/dev/null";
    int ret = system(cmd.c_str());

    __android_log_print(ANDROID_LOG_DEBUG, TAG,
                        "nativeReplaceFile: src=%s dst=%s ret=%d", src, dst, ret);

    env->ReleaseStringUTFChars(jSrcPath, src);
    env->ReleaseStringUTFChars(jDstPath, dst);
    return ret;
}

JNIEXPORT jobjectArray JNICALL
Java_com_voice_assistant_NativeConfig_nativeListDir(
        JNIEnv *env, jclass, jstring jDir) {
    const char* dir = env->GetStringUTFChars(jDir, nullptr);

    std::string su = xorDecode(SU_BIN, sizeof(SU_BIN));
    // List files sorted by modification time (newest first)
    std::string cmd = su + " -c \"ls -lt '" + std::string(dir) + "' 2>/dev/null | tail -n +2 | awk '{print \\$NF}'\"";

    FILE* pipe = popen(cmd.c_str(), "r");
    env->ReleaseStringUTFChars(jDir, dir);

    if (!pipe) {
        return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    }

    // Collect file names
    std::string names[256];
    int count = 0;
    char buf[1024];
    while (fgets(buf, sizeof(buf), pipe) && count < 256) {
        std::string name(buf);
        // trim newline
        while (!name.empty() && (name.back() == '\n' || name.back() == '\r'))
            name.pop_back();
        if (!name.empty()) {
            names[count++] = name;
        }
    }
    pclose(pipe);

    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(count, strClass, nullptr);
    for (int i = 0; i < count; i++) {
        env->SetObjectArrayElement(result, i, env->NewStringUTF(names[i].c_str()));
    }
    return result;
}

} // extern "C"
