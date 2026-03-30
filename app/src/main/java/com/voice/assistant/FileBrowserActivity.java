package com.voice.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Root file browser for selecting target voice files.
 * Uses su to list files in protected directories.
 */
public class FileBrowserActivity extends Activity {

    public static final String EXTRA_INITIAL_DIR = "initial_dir";
    public static final String EXTRA_SELECTED_PATH = "selected_path";

    private String currentDir;
    private String[] entries = new String[0];
    private TextView tvPath;
    private ListView listView;
    private FileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentDir = getIntent().getStringExtra(EXTRA_INITIAL_DIR);
        if (currentDir == null || currentDir.isEmpty()) {
            currentDir = "/data/data/com.ss.android.ugc.aweme/files";
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D0D0F);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(0xFF1C1C1E);
        topBar.setPadding(dp(16), dp(48), dp(16), dp(14));
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnBack = new TextView(this);
        btnBack.setText("<");
        btnBack.setTextColor(0xFFFF3B5C);
        btnBack.setTextSize(22);
        btnBack.setPadding(dp(8), dp(4), dp(16), dp(4));
        btnBack.setOnClickListener(v -> navigateUp());
        topBar.addView(btnBack);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Select File");
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(18);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topBar.addView(tvTitle);

        TextView btnClose = new TextView(this);
        btnClose.setText("Cancel");
        btnClose.setTextColor(0xFF8E8E93);
        btnClose.setTextSize(15);
        btnClose.setPadding(dp(16), dp(4), dp(8), dp(4));
        btnClose.setOnClickListener(v -> { setResult(RESULT_CANCELED); finish(); });
        topBar.addView(btnClose);

        root.addView(topBar);

        tvPath = new TextView(this);
        tvPath.setTextColor(0xFF8E8E93);
        tvPath.setTextSize(12);
        tvPath.setPadding(dp(16), dp(10), dp(16), dp(10));
        tvPath.setBackgroundColor(0xFF1C1C1E);
        tvPath.setSingleLine(true);
        root.addView(tvPath);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF3A3A3C);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        root.addView(divider);

        listView = new ListView(this);
        listView.setBackgroundColor(0xFF0D0D0F);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        adapter = new FileAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String name = entries[position];
            onItemSelected(name);
        });

        root.addView(listView);
        setContentView(root);
        loadDir(currentDir);
    }

    private void loadDir(String dir) {
        currentDir = dir;
        tvPath.setText(dir);

        new Thread(() -> {
            final String[] files = NativeConfig.listDir(dir);
            runOnUiThread(() -> {
                entries = files != null ? files : new String[0];
                adapter.notifyDataSetChanged();
                if (entries.length == 0) {
                    Toast.makeText(this, "Empty or access denied", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void onItemSelected(String name) {
        String fullPath = currentDir + "/" + name;

        new Thread(() -> {
            String[] sub = NativeConfig.listDir(fullPath);
            runOnUiThread(() -> {
                if (sub != null && sub.length > 0) {
                    loadDir(fullPath);
                } else {
                    if (!name.contains(".")) {
                        loadDir(fullPath);
                    } else {
                        selectFile(fullPath);
                    }
                }
            });
        }).start();
    }

    private void selectFile(String path) {
        Intent result = new Intent();
        result.putExtra(EXTRA_SELECTED_PATH, path);
        setResult(RESULT_OK, result);
        finish();
    }

    private void navigateUp() {
        if (currentDir.equals("/")) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        int lastSlash = currentDir.lastIndexOf('/');
        if (lastSlash > 0) {
            loadDir(currentDir.substring(0, lastSlash));
        } else {
            loadDir("/");
        }
    }

    @Override
    public void onBackPressed() {
        navigateUp();
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private class FileAdapter extends BaseAdapter {
        @Override public int getCount() { return entries.length; }
        @Override public Object getItem(int position) { return entries[position]; }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
            } else {
                row = new LinearLayout(FileBrowserActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setGravity(Gravity.CENTER_VERTICAL);

                TextView icon = new TextView(FileBrowserActivity.this);
                icon.setId(android.R.id.icon);
                icon.setTextSize(18);
                icon.setPadding(0, 0, dp(12), 0);
                row.addView(icon);

                TextView text = new TextView(FileBrowserActivity.this);
                text.setId(android.R.id.text1);
                text.setTextColor(0xFFE5E5EA);
                text.setTextSize(14);
                text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(text);

                TextView arrow = new TextView(FileBrowserActivity.this);
                arrow.setId(android.R.id.text2);
                arrow.setTextColor(0xFF636366);
                arrow.setTextSize(14);
                row.addView(arrow);
            }

            String name = entries[position];
            TextView icon = row.findViewById(android.R.id.icon);
            TextView text = row.findViewById(android.R.id.text1);
            TextView arrow = row.findViewById(android.R.id.text2);

            boolean isFile = name.contains(".");
            icon.setText(isFile ? "\uD83C\uDFB5" : "\uD83D\uDCC1");
            text.setText(name);
            arrow.setText(isFile ? "" : ">");

            row.setBackgroundColor(position % 2 == 0 ? 0xFF0D0D0F : 0xFF1A1A1C);
            return row;
        }
    }
}
