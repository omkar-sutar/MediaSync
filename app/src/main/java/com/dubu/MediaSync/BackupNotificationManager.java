package com.dubu.MediaSync;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_MAX;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_MIN;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.dubu.mediasync.R;

public class BackupNotificationManager {
    private static BackupNotificationManager backupNotificationManager = null;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "BackupServiceChannel";
    public static final int BACKUP_ACTIVE_NOTIFICATION_ID = 1;
    public static final int BACKUP_FINISHED_NOTIFICATION_ID = 2;

    /**
     * Utility class to manage all notification related actions.
     * This is a Singleton class, use {@link BackupNotificationManager#getBackupNotificationManager(Context)}
     * to get the instance.
     * @param context Application context
     */
    private BackupNotificationManager(@NonNull Context context){
        notificationManager  = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    /**
     * @param context: context
     * @return Singleton instance of {@link BackupNotificationManager}
     */
    public static BackupNotificationManager getBackupNotificationManager(Context context){
        if(backupNotificationManager!=null)return backupNotificationManager;
        backupNotificationManager = new BackupNotificationManager(context);
        return backupNotificationManager;
    }
    public void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Backup Service Channel",
                NotificationManager.IMPORTANCE_LOW  // Set to low importance
        );
        serviceChannel.setSound(null, null);  // Disable sound for the channel
        serviceChannel.enableVibration(false);  // Disable vibration for the channel
        notificationManager.createNotificationChannel(serviceChannel);
    }

    public Notification createBackupStartNotification(Context context){
        // Returns a "Backup in progress" notification
        // Create an Intent to open MainActivity when the notification is tapped
        Intent openMainActivityIntent = new Intent(context, MainActivity.class);

        // Create a PendingIntent from the notification Intent
        // OS will use this intent to call MainActivity on your behalf when notification is clicked
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openMainActivityIntent,
                PendingIntent.FLAG_IMMUTABLE // Required for Android 12 and above
        );
        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Backup in Progress")
                .setContentText("Backing up your data...")
                .setOnlyAlertOnce(true)  // Alert only the first time
                .setProgress(0, 0, true)
                .setOngoing(true)  // Make the notification non-dismissible
                .setSmallIcon(R.drawable.cloud_upload)
                .setPriority(IMPORTANCE_MAX)  // Lower priority
                .setContentIntent(pendingIntent);   // Supply the pending intent, pendingIntent will be executed when notification is clicked
        return notificationBuilder.build();
    }
    public void updateNotificationProgress(int progress, int max) {
        notificationBuilder.setProgress(max, progress, false)
                .setPriority(IMPORTANCE_MIN)
                .setOngoing(true)  // Ensure it remains non-dismissable
                .setOnlyAlertOnce(true)
                .setContentText("Backing up: " + progress + "/" + max);

        notificationManager.notify(BACKUP_ACTIVE_NOTIFICATION_ID, notificationBuilder.build());
    }
    public void finishNotification() {
        notificationBuilder.setContentText("Backup completed")
                .setProgress(0, 0, false) // Remove the progress bar
                .setOngoing(false);       // Allow the notification to be dismissed

        notificationManager.notify(BACKUP_ACTIVE_NOTIFICATION_ID, notificationBuilder.build());
    }
    public void notifyBackupFinishedNotification(Context context, int backupResultStatus,@Nullable String backupResultMessage){
        String title,contentText;
        if(backupResultStatus==-1){
            title="Backup failed";
            contentText="The last backup did not complete successfully";
            if(backupResultMessage!=null){
                contentText=backupResultMessage;
            }
        }
        else {
            title="Backup completed";
            contentText="The last backup was completed successfully.";
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.cloud_upload)
                .setContentText(contentText);
        notificationManager.notify(BACKUP_FINISHED_NOTIFICATION_ID,builder.build());
    }
}
