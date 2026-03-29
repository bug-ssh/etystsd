package com.voice.assistant;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class FloatWindowService extends Service {

    private WindowManager              wm;
    private View                       floatView;
    private WindowManager.LayoutParams params;

    /** 0 = 评论模式，1 = 私聊模式 */
    private int mode = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatWindow();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatView != null && wm != null) {
            wm.removeView(floatView);
            floatView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ── 创建悬浮窗 ────────────────────────────────────────

    private void createFloatWindow() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.layout_float_window, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 16;
        params.y = 320;

        wm.addView(floatView, params);

        TextView tvMode     = floatView.findViewById(R.id.tv_float_mode);
        TextView btnReplace = floatView.findViewById(R.id.btn_float_replace);
        TextView btnSwitch  = floatView.findViewById(R.id.btn_float_switch);
        TextView btnClose   = floatView.findViewById(R.id.btn_float_close);

        refreshModeUI(tvMode);

        btnSwitch.setOnClickListener(v -> {
            mode = (mode == 0) ? 1 : 0;
            refreshModeUI(tvMode);
            toast("已切换为：" + (mode == 0 ? "评论模式" : "私聊模式"));
        });

        btnReplace.setOnClickListener(v -> doReplace());
        btnClose.setOnClickListener(v -> stopSelf());

        setupDrag(floatView);
    }

    private void refreshModeUI(TextView tvMode) {
        if (mode == 0) {
            tvMode.setText("评论");
            tvMode.setBackgroundResource(R.drawable.bg_mode_comment);
        } else {
            tvMode.setText("私聊");
            tvMode.setBackgroundResource(R.drawable.bg_mode_im);
        }
    }

    // ── 核心替换逻辑 ──────────────────────────────────────

    private void doReplace() {
        new Thread(() -> {
            try {
                // 1. 读取目标目录（从 SharedPreferences 或 .so 默认值）
                String targetDir = (mode == 0)
                        ? NativeConfig.getCommentPath(FloatWindowService.this)
                        : NativeConfig.getImPath(FloatWindowService.this);

                // 2. Root 扫描目录，找最新语音文件
                String latestFile = findLatestFileRoot(targetDir);
                if (latestFile == null || latestFile.isEmpty()) {
                    post("未找到语音文件，请先在抖音录制语音（不要发送）");
                    return;
                }

                // 3. 获取缓存中的替换音频
                File srcFile = getCachedAudio();
                if (srcFile == null || !srcFile.exists()) {
                    post("请先在主界面选择替换音频文件");
                    return;
                }

                // 4. 调用 native .so 执行 Root 替换
                int ret = NativeConfig.nativeReplaceFile(
                        srcFile.getAbsolutePath(), latestFile);

                if (ret == 0) {
                    post("✓ 替换成功！现在可以发送语音");
                } else {
                    post("替换失败（" + ret + "），请确认已授予 Root 权限");
                }

            } catch (Exception e) {
                post("替换异常：" + e.getMessage());
            }
        }).start();
    }

    /** 用 Root Shell 找目录中最新文件 */
    private String findLatestFileRoot(String dir) {
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"su", "-c",
                            "ls -t \"" + dir + "\" 2>/dev/null | head -1"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            String name = br.readLine();
            p.waitFor();
            p.destroy();
            if (name == null || name.trim().isEmpty()) return null;
            return dir + "/" + name.trim();
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取 MainActivity 存入缓存的替换音频 */
    private File getCachedAudio() {
        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles(
                f -> f.getName().startsWith("replace_audio"));
        if (files != null && files.length > 0) return files[0];
        return null;
    }

    // ── 拖动 ─────────────────────────────────────────────

    private void setupDrag(View v) {
        final float[] lastRaw = {0, 0};
        final int[]   initPos = {0, 0};
        final boolean[] moved = {false};

        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastRaw[0] = event.getRawX();
                    lastRaw[1] = event.getRawY();
                    initPos[0] = params.x;
                    initPos[1] = params.y;
                    moved[0]   = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastRaw[0];
                    float dy = event.getRawY() - lastRaw[1];
                    if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                        moved[0]  = true;
                        params.x  = (int)(initPos[0] + dx);
                        params.y  = (int)(initPos[1] + dy);
                        wm.updateViewLayout(floatView, params);
                    }
                    return moved[0];
                default:
                    return false;
            }
        });
    }

    // ── 工具 ─────────────────────────────────────────────

    private void post(String msg) {
        mainHandler.post(() -> toast(msg));
    }

    private void toast(String msg) {
        Toast.makeText(FloatWindowService.this, msg, Toast.LENGTH_SHORT).show();
    }
}
