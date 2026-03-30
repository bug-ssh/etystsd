package com.voice.assistant;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class FloatWindowService extends Service {

    private static final String CHANNEL_ID = "float_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager              wm;
    private View                       floatView;
    private WindowManager.LayoutParams params;

    /** 0 = comment mode, 1 = IM mode */
    private int mode = 0;

    /** Whether the panel is expanded */
    private boolean expanded = true;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int screenWidth;
    private int screenHeight;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        createFloatWindow();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatView != null && wm != null) {
            try { wm.removeView(floatView); } catch (Exception ignore) {}
            floatView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ── Notification (required for foreground service) ──

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Voice Assistant", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Float window service");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setContentTitle("Voice Assistant Running")
                .setContentText("Float window is active")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    // ── Create Float Window ─────────────────────────────

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

        // Switch mode on tap
        btnSwitch.setOnClickListener(v -> {
            mode = (mode == 0) ? 1 : 0;
            refreshModeUI(tvMode);
            vibrate();
            toast("Mode: " + (mode == 0 ? "Comment" : "IM"));
        });

        // Replace on tap
        btnReplace.setOnClickListener(v -> doReplace());

        // Long press replace -> open audio picker
        btnReplace.setOnLongClickListener(v -> {
            vibrate();
            Intent intent = new Intent(FloatWindowService.this, AudioPickerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            toast("Select replacement audio...");
            return true;
        });

        // Close
        btnClose.setOnClickListener(v -> stopSelf());

        // Long press close -> collapse/expand
        btnClose.setOnLongClickListener(v -> {
            toggleExpand(tvMode, btnReplace, btnSwitch);
            return true;
        });

        setupSmoothDrag(floatView);
    }

    private void refreshModeUI(TextView tvMode) {
        if (mode == 0) {
            tvMode.setText("Comment");
            tvMode.setBackgroundResource(R.drawable.bg_mode_comment);
        } else {
            tvMode.setText("IM");
            tvMode.setBackgroundResource(R.drawable.bg_mode_im);
        }
    }

    private void toggleExpand(TextView tvMode, TextView btnReplace, TextView btnSwitch) {
        expanded = !expanded;
        int visibility = expanded ? View.VISIBLE : View.GONE;
        tvMode.setVisibility(visibility);
        btnReplace.setVisibility(visibility);
        btnSwitch.setVisibility(visibility);
        vibrate();
    }

    // ── Core Replacement Logic ──────────────────────────

    private void doReplace() {
        // Visual feedback
        TextView btnReplace = floatView.findViewById(R.id.btn_float_replace);
        btnReplace.setAlpha(0.5f);
        vibrate();

        new Thread(() -> {
            try {
                // 1. Check for a specific target file first
                String targetFile = NativeConfig.getTargetFile(FloatWindowService.this);

                if (targetFile == null || targetFile.isEmpty()) {
                    // 2. Fall back to scanning directory for latest file
                    String targetDir = (mode == 0)
                            ? NativeConfig.getCommentPath(FloatWindowService.this)
                            : NativeConfig.getImPath(FloatWindowService.this);

                    targetFile = findLatestFileRoot(targetDir);
                    if (targetFile == null || targetFile.isEmpty()) {
                        post("No voice file found. Record voice in Douyin first (don't send)");
                        resetButton(btnReplace);
                        return;
                    }
                }

                // 3. Get cached replacement audio
                File srcFile = getCachedAudio();
                if (srcFile == null || !srcFile.exists()) {
                    post("Please select replacement audio first");
                    resetButton(btnReplace);
                    return;
                }

                // 4. Execute Root replacement via native
                int ret = NativeConfig.nativeReplaceFile(
                        srcFile.getAbsolutePath(), targetFile);

                if (ret == 0) {
                    post("Replaced! You can now send the voice.");
                } else {
                    post("Replace failed (" + ret + "). Check Root permission.");
                }

            } catch (Exception e) {
                post("Error: " + e.getMessage());
            }
            resetButton(btnReplace);
        }).start();
    }

    private void resetButton(TextView btn) {
        mainHandler.post(() -> btn.setAlpha(1.0f));
    }

    /** Find latest file in directory via Root shell */
    private String findLatestFileRoot(String dir) {
        try {
            String[] files = NativeConfig.listDir(dir);
            if (files != null && files.length > 0) {
                return dir + "/" + files[0];
            }
        } catch (Exception ignore) {}
        return null;
    }

    /** Get cached replacement audio from MainActivity */
    private File getCachedAudio() {
        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles(
                f -> f.getName().startsWith("replace_audio"));
        if (files != null && files.length > 0) return files[0];
        return null;
    }

    // ── Smooth Drag with Velocity & Edge Snap ───────────

    private void setupSmoothDrag(View v) {
        final float[] lastRaw = {0, 0};
        final int[]   initPos = {0, 0};
        final boolean[] moved = {false};
        final long[] downTime = {0};

        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastRaw[0] = event.getRawX();
                    lastRaw[1] = event.getRawY();
                    initPos[0] = params.x;
                    initPos[1] = params.y;
                    moved[0]   = false;
                    downTime[0] = System.currentTimeMillis();

                    // Scale up on touch
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
                    return false;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastRaw[0];
                    float dy = event.getRawY() - lastRaw[1];
                    if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                        moved[0]  = true;
                        params.x  = (int)(initPos[0] + dx);
                        params.y  = (int)(initPos[1] + dy);
                        // Clamp to screen bounds
                        params.x = Math.max(0, Math.min(params.x, screenWidth - v.getWidth()));
                        params.y = Math.max(0, Math.min(params.y, screenHeight - v.getHeight()));
                        try { wm.updateViewLayout(floatView, params); } catch (Exception ignore) {}
                    }
                    return moved[0];

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Scale back
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();

                    if (moved[0]) {
                        // Snap to nearest edge with smooth animation
                        snapToEdge();
                    }
                    return moved[0];

                default:
                    return false;
            }
        });
    }

    /** Smoothly animate the float window to the nearest horizontal edge */
    private void snapToEdge() {
        int center = params.x + (floatView.getWidth() / 2);
        final int targetX = (center < screenWidth / 2) ? 8 : screenWidth - floatView.getWidth() - 8;

        ValueAnimator animator = ValueAnimator.ofInt(params.x, targetX);
        animator.setDuration(250);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            params.x = (int) anim.getAnimatedValue();
            try { wm.updateViewLayout(floatView, params); } catch (Exception ignore) {}
        });
        animator.start();
    }

    // ── Utility ─────────────────────────────────────────

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(30);
                }
            }
        } catch (Exception ignore) {}
    }

    private void post(String msg) {
        mainHandler.post(() -> toast(msg));
    }

    private void toast(String msg) {
        Toast.makeText(FloatWindowService.this, msg, Toast.LENGTH_SHORT).show();
    }
}
