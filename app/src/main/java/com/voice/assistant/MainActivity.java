package com.voice.assistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;

    private TextView tvCommentPath;
    private TextView tvImPath;
    private Button   btnSaveStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCommentPath = findViewById(R.id.tv_comment_path);
        tvImPath      = findViewById(R.id.tv_im_path);
        btnSaveStart  = findViewById(R.id.btn_save_start);

        // ── 从 .so 读取路径（Java 层无明文字符串）──
        try {
            String commentPath = NativeConfig.getCommentPath();
            String imPath      = NativeConfig.getImPath();
            tvCommentPath.setText(commentPath);
            tvImPath.setText(imPath);
        } catch (UnsatisfiedLinkError e) {
            // .so 未加载时降级显示（调试阶段）
            tvCommentPath.setText("/data/data/com.ss.android.ugc.aweme/files/comment/audio");
            tvImPath.setText("/data/data/com.ss.android.ugc.aweme/files/im");
        }

        btnSaveStart.setOnClickListener(v -> onSaveStart());
    }

    private void onSaveStart() {
        if (!Settings.canDrawOverlays(this)) {
            // 申请悬浮窗权限
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
            Toast.makeText(this, "请授予悬浮窗权限后返回", Toast.LENGTH_LONG).show();
            return;
        }
        launchFloatAndDouyin();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                launchFloatAndDouyin();
            } else {
                Toast.makeText(this, "悬浮窗权限未授予，无法继续", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void launchFloatAndDouyin() {
        // 启动悬浮窗服务
        startService(new Intent(this, FloatWindowService.class));
        Toast.makeText(this, "悬浮窗已启动，正在打开抖音...", Toast.LENGTH_SHORT).show();

        // 自动跳转抖音
        try {
            String pkg = NativeConfig.getDouyinPackage();
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                startActivity(launch);
            } else {
                Toast.makeText(this, "未检测到抖音，请手动打开", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "打开抖音失败", Toast.LENGTH_SHORT).show();
        }
    }
}
