# 🎵 语音助手 - 抖音定制版

> 在抖音评论区 / 私聊中，将录制的语音替换为自定义音频后再发送。

---

## 📋 项目结构

```
DouyinVoiceApp/
├── app/src/main/
│   ├── AndroidManifest.xml          # 权限声明
│   ├── java/com/voice/assistant/
│   │   ├── MainActivity.java        # 主界面
│   │   ├── FloatWindowService.java  # 悬浮窗服务
│   │   ├── RootReplacer.java        # Root 文件替换核心
│   │   └── UriHelper.java          # Uri 工具类
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml    # 主界面布局
│       │   └── layout_float_window.xml  # 悬浮窗布局
│       ├── drawable/                # 图标 & 背景
│       └── values/                  # 颜色/字符串/样式
├── build.gradle
├── settings.gradle
└── preview.html                     # 交互式界面预览
```

---

## 🚀 功能特性

| 功能 | 说明 |
|------|------|
| 模式切换 | 评论区语音 / 私聊语音，对应不同目录 |
| 音频选择 | 从手机本地选择任意音频格式 |
| 悬浮窗 | 覆盖在抖音上方，可拖动定位 |
| 一键替换 | Root 权限替换抖音刚录制的最新语音文件 |
| 状态反馈 | Toast 提示替换成功/失败原因 |

---

## 📂 抖音语音文件目录

| 模式 | 目录路径 |
|------|----------|
| 评论区 | `/data/data/com.ss.android.ugc.aweme/files/comment/audio` |
| 私聊 | `/data/data/com.ss.android.ugc.aweme/files/im` |

---

## 🛠️ 编译要求

- **Android Studio** Hedgehog 及以上
- **JDK 11+**
- **compileSdk 34**，**minSdk 26**（Android 8.0+）

---

## ⚙️ 权限说明

| 权限 | 用途 |
|------|------|
| `SYSTEM_ALERT_WINDOW` | 显示悬浮窗 |
| `READ_EXTERNAL_STORAGE` | 读取用户选择的音频文件 |
| `READ_MEDIA_AUDIO` | Android 13+ 读取音频 |
| `FOREGROUND_SERVICE` | 悬浮窗后台服务保活 |

> ⚠️ **Root 权限**：替换抖音数据目录需要设备已 Root 并授权本 APP 的 `su` 请求。

---

## 📖 使用步骤

1. **安装 APP** → 授予悬浮窗权限
2. **选择模式**：评论区 或 私聊
3. **选择音频**：点击「选择音频文件」从本地选取
4. **启动悬浮窗**：点击「保存并启动悬浮窗」，APP 自动跳转抖音
5. **在抖音录制**：评论区或私聊录制 1-30 秒语音，**不要点发送**
6. **点击替换**：悬浮窗中切换对应模式 → 点击 🔄 替换按钮
7. **发送**：替换成功后，回到发送按钮发出语音

---

## ⚠️ 注意事项

- 设备必须已 **Root**，替换目标目录需 Root 权限
- 抖音不同版本路径可能有差异，可在主界面路径卡片中确认
- 建议使用与抖音录音相同格式的音频（`.amr` 或 `.opus`）
- 本工具仅供学习研究使用

---

## 🎨 UI 预览

打开 `preview.html` 可查看完整的交互式界面预览（主界面 + 悬浮窗 + 使用流程）。
