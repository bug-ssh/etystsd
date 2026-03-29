package com.voice.assistant;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_PICK_AUDIO = 2001;
    private static final int REQ_OVERLAY    = 2002;

    // Root 状态
    private View     rootBadge;
    private TextView tvRootStatus;

    // 音频选择
    private TextView tvAudioName;
    private Button   btnPickAudio;

    // 路径设置
    private EditText etCommentPath;
    private EditText etImPath;

    // 启动
    private Button btnSaveStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootBadge     = findViewById(R.id.root_badge);
        tvRootStatus  = findViewById(R.id.tv_root_status);
        tvAudioName   = findViewById(R.id.tv_audio_name);
        btnPickAudio  = findViewById(R.id.btn_pick_audio);
        etCommentPath = findViewById(R.id.et_comment_path);
        etImPath      = findViewById(R.id.et_im_path);
        btnSaveStart  = findViewById(R.id.btn_save_start);

        // 初始化路径（优先 SharedPreferences，其次 .so 默认值）
        etCommentPath.setText(NativeConfig.getCommentPath(this));
        etImPath.setText(NativeConfig.getImPath(this));

        // 恢复已选音频名
        String savedName = NativeConfig.getSavedAudioName(this);
        if (!TextUtils.isEmpty(savedName)) {
            tvAudioName.setText("📄 " + savedName);
            tvAudioName.setVisibility(View.VISIBLE);
        }

        // 异步检测 Root
        detectRoot();

        btnPickAudio.setOnClickListener(v -> openFilePicker());
        btnSaveStart.setOnClickListener(v -> onSaveStart());
    }

    // ── Root 检测 ────────────────────────────────────────

    private void detectRoot() {
        tvRootStatus.setText("Root：检测中…");
        rootBadge.setBackgroundResource(R.drawable.bg_badge_gray);

        new Thread(() -> {
            final boolean rooted = NativeConfig.checkRoot();
            runOnUiThread(() -> {
                if (rooted) {
                    tvRootStatus.setText("✓  Root 已获取");
                    rootBadge.setBackgroundResource(R.drawable.bg_badge_green);
                } else {
                    tvRootStatus.setText("✗  未获取 Root");
                    rootBadge.setBackgroundResource(R.drawable.bg_badge_red);
                }
            });
        }).start();
    }

    // ── 文件选择 ─────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择替换音频"), REQ_PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_AUDIO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String name = queryFileName(uri);
                if (TextUtils.isEmpty(name)) name = "selected_audio";

                // 持久化 URI 权限
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignore) {}

                // 拷贝到缓存，保证后台服务可读
                if (copyToCache(uri, name)) {
                    NativeConfig.saveAudioInfo(this, uri.toString(), name);
                    tvAudioName.setText("📄 " + name);
                    tvAudioName.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "音频已选择 ✓", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "文件读取失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startFloatService();
            } else {
                Toast.makeText(this, "悬浮窗权限未授予，无法启动", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ── 保存并启动 ────────────────────────────────────────

    private void onSaveStart() {
        String commentPath = etCommentPath.getText().toString().trim();
        String imPath      = etImPath.getText().toString().trim();

        if (TextUtils.isEmpty(commentPath) || TextUtils.isEmpty(imPath)) {
            Toast.makeText(this, "路径不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (NativeConfig.getSavedAudioName(this) == null) {
            Toast.makeText(this, "请先选择替换音频文件", Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存自定义路径
        NativeConfig.saveCustomPaths(this, commentPath, imPath);

        // 申请悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
            Toast.makeText(this, "请授予悬浮窗权限后返回", Toast.LENGTH_LONG).show();
            return;
        }

        startFloatService();
    }

    private void startFloatService() {
        startService(new Intent(this, FloatWindowService.class));
        Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show();

        // 尝试跳转抖音
        try {
            String pkg = NativeConfig.getDouyinPackage();
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) startActivity(launch);
        } catch (Exception ignore) {}
    }

    // ── 工具方法 ─────────────────────────────────────────

    /** 查询文件名 */
    private String queryFileName(Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(
                    uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) return c.getString(idx);
                }
            } catch (Exception ignore) {}
        }
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut >= 0) return path.substring(cut + 1);
        }
        return null;
    }

    /** 将选中的音频拷贝至私有缓存（供 FloatWindowService 读取）*/
    private boolean copyToCache(Uri uri, String name) {
        // 清除旧缓存
        File cacheDir = getCacheDir();
        File[] old = cacheDir.listFiles(f -> f.getName().startsWith("replace_audio"));
        if (old != null) for (File f : old) f.delete();

        // 保留扩展名
        String ext = ".amr";
        String lower = name.toLowerCase();
        if (lower.endsWith(".opus")) ext = ".opus";
        else if (lower.endsWith(".aac")) ext = ".aac";
        else if (lower.endsWith(".mp3")) ext = ".mp3";
        else if (lower.endsWith(".wav")) ext = ".wav";
        else if (lower.endsWith(".m4a")) ext = ".m4a";
        else if (lower.endsWith(".ogg")) ext = ".ogg";
        else if (lower.endsWith(".amr")) ext = ".amr";

        File dest = new File(cacheDir, "replace_audio" + ext);
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) return false;
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
