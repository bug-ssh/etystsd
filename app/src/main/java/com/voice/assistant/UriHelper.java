package com.voice.assistant;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Utility for resolving content URIs to file paths and copying files.
 */
public class UriHelper {
    private static final String TAG = "UriHelper";

    /**
     * Get a real file path from a URI. Falls back to copying to cache if needed.
     */
    public static String getPath(Context context, Uri uri) {
        if (uri == null) return null;

        // Try DocumentsContract first (API 19+)
        if (DocumentsContract.isDocumentUri(context, uri)) {
            return getDocumentPath(context, uri);
        }

        // Content URI
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getContentPath(context, uri);
        }

        // File URI
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        // Fallback: copy to cache
        return copyToCache(context, uri);
    }

    private static String getDocumentPath(Context context, Uri uri) {
        String docId = DocumentsContract.getDocumentId(uri);

        // External storage
        if (isExternalStorageDocument(uri)) {
            String[] split = docId.split(":");
            String type = split[0];
            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
        }

        // Downloads
        if (isDownloadsDocument(uri)) {
            try {
                long id = Long.parseLong(docId);
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), id);
                String path = queryColumn(context, contentUri, MediaStore.MediaColumns.DATA);
                if (path != null) return path;
            } catch (NumberFormatException e) {
                // Some devices return path directly in docId
                if (docId.startsWith("raw:")) {
                    return docId.substring(4);
                }
            }
        }

        // Media documents
        if (isMediaDocument(uri)) {
            String[] split = docId.split(":");
            String type = split[0];
            Uri contentUri = null;
            if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }
            if (contentUri != null) {
                String path = queryColumn(context, contentUri, MediaStore.MediaColumns.DATA,
                        "_id=?", new String[]{split[1]});
                if (path != null) return path;
            }
        }

        return copyToCache(context, uri);
    }

    private static String getContentPath(Context context, Uri uri) {
        String path = queryColumn(context, uri, MediaStore.MediaColumns.DATA);
        if (path != null) return path;
        return copyToCache(context, uri);
    }

    private static String queryColumn(Context context, Uri uri, String column) {
        return queryColumn(context, uri, column, null, null);
    }

    private static String queryColumn(Context context, Uri uri, String column,
                                       String selection, String[] selectionArgs) {
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{column}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(column);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Query failed for " + uri, e);
        }
        return null;
    }

    /**
     * Copy URI content to cache directory and return the path.
     */
    public static String copyToCache(Context context, Uri uri) {
        try {
            String fileName = "voice_" + System.currentTimeMillis() + ".audio";

            // Try to get original filename
            String displayName = queryColumn(context, uri, MediaStore.MediaColumns.DISPLAY_NAME);
            if (displayName != null && !displayName.isEmpty()) {
                fileName = displayName;
            }

            File cacheFile = new File(context.getCacheDir(), fileName);
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            FileOutputStream fos = new FileOutputStream(cacheFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();

            return cacheFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy URI to cache", e);
            return null;
        }
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
