package com.voice.assistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PICK_AUDIO = 2001;
    private static final int REQ_OVERLAY = 2002;

    private TextView tvRootStatus;
    private TextView tvAudioName;
    private EditText etCommentPath;
    private EditText etImPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvRootStatus = findViewById(R.id.tv_root_status);
        tvAudioName = findViewById(R.id.tv_audio_name);
        etCommentPath = findViewById(R.id.et_comment_path);
        etImPath = findViewById(R.id.et_im_path);
        Button btnPickAudio = findViewById(R.id.btn_pick_audio);
        Button btnSaveStart = findViewById(R.id.btn_save_start);

        etCommentPath.setText(NativeConfig.getCommentPath(this));
        etImPath.setText(NativeConfig.getImPath(this));
        String saved = NativeConfig.getSavedAudioName(this);
        if (saved != null && !saved.isEmpty()) {
            tvAudioName.setText("📄 " + saved);
        }

        btnPickAudio.setOnClickListener(v -> pickAudio());
        btnSaveStart.setOnClickListener(v -> {
            NativeConfig.setCommentPath(this, etCommentPath.getText().toString().trim());
            NativeConfig.setImPath(this, etImPath.getText().toString().trim());
            startService(new Intent(this, FloatWindowService.class));
            tvRootStatus.setText("已保存");
        });
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQ_PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_AUDIO && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String name = queryName(uri);
                NativeConfig.setSavedAudioName(this, name);
                tvAudioName.setText("📄 " + name);
            }
        }
    }

    private String queryName(Uri uri) {
        String result = "audio";
        try (var cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
