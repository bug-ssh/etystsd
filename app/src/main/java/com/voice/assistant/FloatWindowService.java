package com.voice.assistant;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatWindowService extends Service {
    private WindowManager wm;
    private View floatView;
    private WindowManager.LayoutParams params;
    private int mode = 0;
    private final float alpha = NativeConfig.getFloatWindowAlpha();

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatWindow();
    }

    private void createFloatWindow() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
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
        floatView.setAlpha(alpha);

        TextView tvMode = floatView.findViewById(R.id.tv_float_mode);
        TextView btnReplace = floatView.findViewById(R.id.btn_float_replace);
        TextView btnSwitch = floatView.findViewById(R.id.btn_float_switch);
        TextView btnClose = floatView.findViewById(R.id.btn_float_close);

        updateModeText(tvMode);

        btnSwitch.setOnClickListener(v -> {
            mode = 1 - mode;
            updateModeText(tvMode);
        });

        btnClose.setOnClickListener(v -> stopSelf());

        btnReplace.setOnClickListener(v -> {
            tvMode.setText(mode == 0 ? "评论中" : "私聊中");
        });

        floatView.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startY;
            private float touchX;
            private float touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        params.x = (int) (startX + event.getRawX() - touchX);
                        params.y = (int) (startY + event.getRawY() - touchY);
                        wm.updateViewLayout(floatView, params);
                        return false;
                }
                return false;
            }
        });
    }

    private void updateModeText(TextView tvMode) {
        tvMode.setText(mode == 0 ? "评论" : "私聊");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wm != null && floatView != null) {
            wm.removeView(floatView);
        }
        floatView = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
