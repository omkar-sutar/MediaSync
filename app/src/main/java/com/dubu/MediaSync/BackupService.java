package com.dubu.MediaSync;


import static com.dubu.MediaSync.BackupNotificationManager.BACKUP_ACTIVE_NOTIFICATION_ID;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class BackupService extends Service {


    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final PreferenceData preferenceData = PreferenceData.getPreferenceData();
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BackupService getService() {
            return BackupService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager  = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        BackupNotificationManager backupNotificationManager = BackupNotificationManager.getBackupNotificationManager(this);
        startForeground(BACKUP_ACTIVE_NOTIFICATION_ID, backupNotificationManager.createBackupStartNotification(this));
        // Start your backup logic in a background thread
        new Thread(this::performBackup).start();
        Toast.makeText(this,"Backup service started",Toast.LENGTH_LONG).show();
        return START_NOT_STICKY;    // Tells OS if this service is killed due to e.g. low memory then whether to  restart it whenever sufficient resources are available.
    }
    private void performBackup() {
        int backupResultStatus = 0;
        String backupResultMessage = null;
        try{
            BackupSession backupSession = new BackupSession();
            FileAccess fileAccess = new FileAccess(preferenceData);
            ArrayList<String> files =  fileAccess.getRecentImages(this,preferenceData);
            backupSession.StartBackup(files.toArray(new String[0]),(progress,max)->
                    BackupNotificationManager.getBackupNotificationManager(this).updateNotificationProgress(progress,max));

        } catch (Exception e) {

            Log.d("exception", e.toString());
            Utils.runOnUiThread(() -> showToast(e.getMessage()));
            backupResultStatus = -1;
            backupResultMessage = e.toString();
        }
        finally {
            sendBackupCompleteBroadcast(backupResultStatus,backupResultMessage);
            stopThisService();
        }
    }
    private void stopThisService() {
        // Make sure all clients have unbinded, only then this service will exit
        // Calling this method will abort the service unless all clients have unbinded
        stopForeground(true);
        stopSelf();
    }

    private void showToast(String message) {
        Utils.showToast(getApplicationContext(),message);
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // Don't stop service when unbind as we want to continue this service even when main app exits
        // stopThisService()
        return super.onUnbind(intent);
    }


    private void sendBackupCompleteBroadcast(int exitCode,@Nullable String optMessage) {
        Intent intent = new Intent("BACKUP_COMPLETED");
        intent.putExtra("backup_result_status_key",exitCode);
        if(optMessage!=null){
            intent.putExtra("backup_result_message_key",optMessage);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}