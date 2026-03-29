package com.voice.assistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 透明 Activity，仅用于文件选择器
 * 选完自动存入 getCacheDir()/replace_audio.*
 */
public class AudioPickerActivity extends Activity {

    private static final int REQ_PICK = 3001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择替换音频"), REQ_PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null && copyToCache(uri)) {
                Toast.makeText(this, "音频已选择 ✓", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "文件读取失败", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private boolean copyToCache(Uri uri) {
        // 先删除旧缓存
        File cacheDir = getCacheDir();
        File[] old = cacheDir.listFiles(f -> f.getName().startsWith("replace_audio"));
        if (old != null) for (File f : old) f.delete();

        // 判断扩展名
        String uriStr = uri.toString().toLowerCase();
        String ext = ".amr";
        if (uriStr.contains(".opus")) ext = ".opus";
        else if (uriStr.contains(".aac")) ext = ".aac";
        else if (uriStr.contains(".mp3")) ext = ".mp3";
        else if (uriStr.contains(".wav")) ext = ".wav";
        else if (uriStr.contains(".m4a")) ext = ".m4a";
        else if (uriStr.contains(".ogg")) ext = ".ogg";

        File dest = new File(cacheDir, "replace_audio" + ext);
        try (InputStream in  = getContentResolver().openInputStream(uri);
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
