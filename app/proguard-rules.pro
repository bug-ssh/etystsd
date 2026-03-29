# Proguard 混淆规则

# 保留 JNI 接口，不能混淆（否则 native 方法找不到）
-keep class com.voice.assistant.NativeConfig { *; }

# 保留 Activity/Service 名（系统需要）
-keep class com.voice.assistant.MainActivity { *; }
-keep class com.voice.assistant.FloatWindowService { *; }
-keep class com.voice.assistant.AudioPickerActivity { *; }

# 通用
-dontwarn **
