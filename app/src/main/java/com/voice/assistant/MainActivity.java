package com.voice.assistant;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_PICK_AUDIO   = 2001;
    private static final int REQ_OVERLAY      = 2002;
    private static final int REQ_BROWSE_FILE  = 2003;
    private static final int REQ_PICK_BG      = 2004;

    // Root status
    private View     rootBadge;
    private TextView tvRootStatus;

    // Audio selection
    private TextView tvAudioName;
    private Button   btnPickAudio;

    // Path settings
    private EditText etCommentPath;
    private EditText etImPath;

    // Target file
    private TextView tvTargetFile;
    private Button   btnBrowseFile;

    // Launch
    private Button btnSaveStart;

    // Settings
    private Button btnSetBg;
    private Button btnFontSmall;
    private Button btnFontMedium;
    private Button btnFontLarge;

    // Root layout for background
    private ScrollView scrollRoot;

    // Font size multipliers
    private static final float[] FONT_SCALES = {0.85f, 1.0f, 1.2f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollRoot    = findViewById(R.id.scroll_root);
        rootBadge     = findViewById(R.id.root_badge);
        tvRootStatus  = findViewById(R.id.tv_root_status);
        tvAudioName   = findViewById(R.id.tv_audio_name);
        btnPickAudio  = findViewById(R.id.btn_pick_audio);
        etCommentPath = findViewById(R.id.et_comment_path);
        etImPath      = findViewById(R.id.et_im_path);
        tvTargetFile  = findViewById(R.id.tv_target_file);
        btnBrowseFile = findViewById(R.id.btn_browse_file);
        btnSaveStart  = findViewById(R.id.btn_save_start);
        btnSetBg      = findViewById(R.id.btn_set_bg);
        btnFontSmall  = findViewById(R.id.btn_font_small);
        btnFontMedium = findViewById(R.id.btn_font_medium);
        btnFontLarge  = findViewById(R.id.btn_font_large);

        // Initialize paths
        etCommentPath.setText(NativeConfig.getCommentPath(this));
        etImPath.setText(NativeConfig.getImPath(this));

        // Restore selected audio name
        String savedName = NativeConfig.getSavedAudioName(this);
        if (!TextUtils.isEmpty(savedName)) {
            tvAudioName.setText(savedName);
            tvAudioName.setVisibility(View.VISIBLE);
        }

        // Restore target file
        String targetFile = NativeConfig.getTargetFile(this);
        if (!TextUtils.isEmpty(targetFile)) {
            tvTargetFile.setText(targetFile);
            tvTargetFile.setVisibility(View.VISIBLE);
        }

        // Load custom background
        loadCustomBackground();

        // Apply saved font size
        applyFontSize(NativeConfig.getFontSize(this));

        // Detect Root async
        detectRoot();

        // Listeners
        btnPickAudio.setOnClickListener(v -> openFilePicker());
        btnSaveStart.setOnClickListener(v -> onSaveStart());
        btnBrowseFile.setOnClickListener(v -> openRootFileBrowser());
        btnSetBg.setOnClickListener(v -> pickBackground());

        btnFontSmall.setOnClickListener(v -> {
            NativeConfig.saveFontSize(this, NativeConfig.FONT_SMALL);
            applyFontSize(NativeConfig.FONT_SMALL);
            toast("Font: Small");
        });
        btnFontMedium.setOnClickListener(v -> {
            NativeConfig.saveFontSize(this, NativeConfig.FONT_MEDIUM);
            applyFontSize(NativeConfig.FONT_MEDIUM);
            toast("Font: Medium");
        });
        btnFontLarge.setOnClickListener(v -> {
            NativeConfig.saveFontSize(this, NativeConfig.FONT_LARGE);
            applyFontSize(NativeConfig.FONT_LARGE);
            toast("Font: Large");
        });
    }

    // ── Root Detection ──────────────────────────────────

    private void detectRoot() {
        tvRootStatus.setText("Root: detecting...");
        rootBadge.setBackgroundResource(R.drawable.bg_badge_gray);

        new Thread(() -> {
            final boolean rooted = NativeConfig.checkRoot();
            runOnUiThread(() -> {
                if (rooted) {
                    tvRootStatus.setText("Root: granted");
                    rootBadge.setBackgroundResource(R.drawable.bg_badge_green);
                } else {
                    tvRootStatus.setText("Root: not available");
                    rootBadge.setBackgroundResource(R.drawable.bg_badge_red);
                }
            });
        }).start();
    }

    // ── Audio File Picker ───────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select replacement audio"), REQ_PICK_AUDIO);
    }

    // ── Root File Browser ───────────────────────────────

    private void openRootFileBrowser() {
        String dir = etCommentPath.getText().toString().trim();
        if (TextUtils.isEmpty(dir)) {
            dir = NativeConfig.getDefaultCommentPath();
        }
        Intent intent = new Intent(this, FileBrowserActivity.class);
        intent.putExtra(FileBrowserActivity.EXTRA_INITIAL_DIR, dir);
        startActivityForResult(intent, REQ_BROWSE_FILE);
    }

    // ── Background Picker ───────────────────────────────

    private void pickBackground() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select background image"), REQ_PICK_BG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_AUDIO && resultCode == RESULT_OK && data != null) {
            handleAudioPicked(data);
        }

        if (requestCode == REQ_BROWSE_FILE && resultCode == RESULT_OK && data != null) {
            String path = data.getStringExtra(FileBrowserActivity.EXTRA_SELECTED_PATH);
            if (!TextUtils.isEmpty(path)) {
                NativeConfig.saveTargetFile(this, path);
                tvTargetFile.setText(path);
                tvTargetFile.setVisibility(View.VISIBLE);
                toast("Target file set");
            }
        }

        if (requestCode == REQ_PICK_BG && resultCode == RESULT_OK && data != null) {
            handleBackgroundPicked(data);
        }

        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startFloatService();
            } else {
                toast("Overlay permission not granted");
            }
        }
    }

    private void handleAudioPicked(Intent data) {
        Uri uri = data.getData();
        if (uri == null) return;

        String name = queryFileName(uri);
        if (TextUtils.isEmpty(name)) name = "selected_audio";

        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignore) {}

        if (copyToCache(uri, name)) {
            NativeConfig.saveAudioInfo(this, uri.toString(), name);
            tvAudioName.setText(name);
            tvAudioName.setVisibility(View.VISIBLE);
            toast("Audio selected");
        } else {
            toast("Failed to read file, please retry");
        }
    }

    private void handleBackgroundPicked(Intent data) {
        Uri uri = data.getData();
        if (uri == null) return;

        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignore) {}

        // Copy background to cache
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) { toast("Failed to read image"); return; }
            File bgFile = new File(getCacheDir(), "custom_bg.jpg");
            try (OutputStream out = new FileOutputStream(bgFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            }
            NativeConfig.saveBackgroundUri(this, bgFile.getAbsolutePath());
            loadCustomBackground();
            toast("Background set");
        } catch (IOException e) {
            toast("Failed to set background");
        }
    }

    // ── Save & Start ────────────────────────────────────

    private void onSaveStart() {
        String commentPath = etCommentPath.getText().toString().trim();
        String imPath      = etImPath.getText().toString().trim();

        if (TextUtils.isEmpty(commentPath) || TextUtils.isEmpty(imPath)) {
            toast("Paths cannot be empty");
            return;
        }

        if (NativeConfig.getSavedAudioName(this) == null) {
            toast("Please select replacement audio first");
            return;
        }

        // Save custom paths
        NativeConfig.saveCustomPaths(this, commentPath, imPath);

        // Request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
            toast("Please grant overlay permission");
            return;
        }

        startFloatService();
    }

    private void startFloatService() {
        startService(new Intent(this, FloatWindowService.class));
        toast("Float window started");

        // Try to launch Douyin
        try {
            String pkg = NativeConfig.getDouyinPackage();
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) startActivity(launch);
        } catch (Exception ignore) {}
    }

    // ── Font Size ───────────────────────────────────────

    private void applyFontSize(int sizePreset) {
        float scale = FONT_SCALES[Math.min(sizePreset, FONT_SCALES.length - 1)];
        applyFontScaleRecursive(scrollRoot, scale);

        // Update button states
        int activeColor = 0xFFFF3B5C;
        int inactiveColor = 0xFF3A3A3C;
        if (btnFontSmall != null) btnFontSmall.setBackgroundColor(sizePreset == 0 ? activeColor : inactiveColor);
        if (btnFontMedium != null) btnFontMedium.setBackgroundColor(sizePreset == 1 ? activeColor : inactiveColor);
        if (btnFontLarge != null) btnFontLarge.setBackgroundColor(sizePreset == 2 ? activeColor : inactiveColor);
    }

    private void applyFontScaleRecursive(View v, float scale) {
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFontScaleRecursive(vg.getChildAt(i), scale);
            }
        }
        if (v instanceof TextView && !(v instanceof EditText) && !(v instanceof Button)) {
            TextView tv = (TextView) v;
            // Store original size in tag on first call
            Object tag = tv.getTag(R.id.scroll_root);
            float baseSize;
            if (tag instanceof Float) {
                baseSize = (Float) tag;
            } else {
                baseSize = tv.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                tv.setTag(R.id.scroll_root, baseSize);
            }
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize * scale);
        }
    }

    // ── Custom Background ───────────────────────────────

    private void loadCustomBackground() {
        String bgPath = NativeConfig.getBackgroundUri(this);
        if (!TextUtils.isEmpty(bgPath)) {
            File bgFile = new File(bgPath);
            if (bgFile.exists()) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(bgPath);
                    if (bmp != null && scrollRoot != null) {
                        BitmapDrawable drawable = new BitmapDrawable(getResources(), bmp);
                        drawable.setAlpha(60); // Semi-transparent overlay
                        scrollRoot.setBackground(drawable);
                        return;
                    }
                } catch (Exception ignore) {}
            }
        }
        // Default background
        if (scrollRoot != null) {
            scrollRoot.setBackgroundColor(0xFF0D0D0F);
        }
    }

    // ── Utility Methods ─────────────────────────────────

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

    private boolean copyToCache(Uri uri, String name) {
        File cacheDir = getCacheDir();
        File[] old = cacheDir.listFiles(f -> f.getName().startsWith("replace_audio"));
        if (old != null) for (File f : old) f.delete();

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

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
