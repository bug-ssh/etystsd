# Proguard rules

# Keep JNI interface (native methods must not be obfuscated)
-keep class com.voice.assistant.NativeConfig { *; }

# Keep Activity/Service names (system needs them)
-keep class com.voice.assistant.MainActivity { *; }
-keep class com.voice.assistant.FloatWindowService { *; }
-keep class com.voice.assistant.AudioPickerActivity { *; }
-keep class com.voice.assistant.FileBrowserActivity { *; }
-keep class com.voice.assistant.RootReplacer { *; }

# General
-dontwarn **
