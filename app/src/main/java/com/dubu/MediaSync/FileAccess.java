package com.dubu.MediaSync;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.ZoneId;
import java.util.ArrayList;

// TODO: set /camera path in preferenceData and use it here
public class FileAccess {

    public PreferenceData preferenceData;
    ;

    public FileAccess(PreferenceData preferenceData) {
        this.preferenceData = preferenceData;
    }

    public void setPreferenceData(PreferenceData preferenceData) {
        this.preferenceData = preferenceData;
    }

    public ArrayList<String> getRecentImages(Context context, PreferenceData preferenceData) {
        // Returns list of paths of images sorted by descending order according to date taken
        ArrayList<String> imagePaths = new ArrayList<>();
        for (int i = 0; i < preferenceData.Directories.length; i++) {
            String directory = preferenceData.Directories[i];
            Cursor cursor = getCursorV2(context, directory);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        // Retrieve the image path
                        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                        imagePaths.add(imagePath);
                    }
                } catch (Exception e) {
                    Log.e("Error while querying: ", e.toString());
                } finally {
                    cursor.close();
                }
            }
        }

        return imagePaths;
    }

    @Nullable
    /**
     * returns cursor to images
     */
    private Cursor getCursor(@NonNull Context context, String directory) {
        String selection = MediaStore.Images.Media.DATA + " like ?";
        String[] selectionArgs = {directory};
        // Columns to retrieve from the query
        String[] projection = {MediaStore.Images.Media.DATA};

        // Sort order: descending by date taken
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        // Limiting the result to the most recent images
        //String limit = String.valueOf(100);

        // Querying the MediaStore content provider
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Returns cursor to files of all types (image, video and others matching path)
     * @param context Application context
     * @param directory Directory to list files from.
     * @return Cursor
     */
    private Cursor getCursorV2(Context context, String directory) {
        String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_TAKEN,
                MediaStore.Files.FileColumns.DATE_MODIFIED
        };

        // Return only image files matching path and date threshold
        String selection = MediaStore.Files.FileColumns.DATA + " LIKE ? AND (" +
                MediaStore.Files.FileColumns.DATE_TAKEN + " >= ?" + " OR " + MediaStore.Files.FileColumns.DATE_MODIFIED + ">= ?)";

        // Create selection args array with the path
        String[] selectionArgs = new String[]{directory,
                String.valueOf(preferenceData.getDateThreshold()
                        .atStartOfDay()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli())
        };

        Uri queryUri = MediaStore.Files.getContentUri("external");

        ContentResolver contentResolver = context.getContentResolver();


        return contentResolver.query(queryUri,
                projection,
                selection,
                selectionArgs, // Selection args (none).
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

    }
}
