package com.dubu.MediaSync;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;

import com.dubu.mediasync.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.switchmaterial.SwitchMaterial;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> manageStorageLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ServiceConnection serviceConnection;
    private boolean isServiceBound = false;
    private BroadcastReceiver backupCompleteReceiver;
    private BackupNotificationManager backupNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceData preferenceData = PreferenceData.getPreferenceData();
        //preferenceData.Directories = new PreferenceData.Directory[]{{}}
        setContentView(R.layout.activity_main);
        configureBtnEventListeners(preferenceData);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                isServiceBound = true;
                setBackupButtonState(false);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isServiceBound = false;
                // Service crashed or was killed
                setBackupButtonState(true);
                showToast("Backup service disconnected unexpectedly");
            }
        };
        // Configure backup button state
        //updateBackupButtonState();
        Intent intent = new Intent(this, BackupService.class);
        bindService(intent, serviceConnection, 0);
        initFieldValues(preferenceData);

        setupManageStorageLauncher();
        setupNotificationPermissionLauncher();
        backupNotificationManager=BackupNotificationManager.getBackupNotificationManager(this);
        requestAllPermissions(this);

        createAndRegisterBroadcastReceiver();


        FileAccess files = new FileAccess(preferenceData);
        ArrayList<String> images = files.getRecentImages(this, preferenceData);
        Log.d("ImageArrSize:", Integer.toString(images.size()));
    }

    private void setupManageStorageLauncher() {
        manageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Environment.isExternalStorageManager()) {
                        showToast("Permission granted");
                    } else {
                        showToast("Failed to get permission");
                        finish();
                    }
                }
        );
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (!result) {
                        showToast("Notification permission is required!");
                        finish();
                    } else {
                        showToast("Notification permission granted");
                        alertAndAccessStoragePermissions(this);
                    }
                }
        );
    }

    private void requestAllPermissions(Context context) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle("Alert")
                .setMessage("Notification permission is required to show notifications")
                .setCancelable(false)
                .setPositiveButton("Ok", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                });

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            alertAndAccessStoragePermissions(context);
        } else {
            builder.show();
        }
    }

    private void alertAndAccessStoragePermissions(Context context) {
        if (Environment.isExternalStorageManager()) {
            return;
        }
        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_Material3);
        TextView textView = new TextView(context);
        textView.setText("Please provide storage permission on the next screen to continue");
        textView.setPadding(50, 50, 50, 50);
        alert.setTitle("Permissions needed")
                .setView(textView)
                .setPositiveButton("Ok", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    manageStorageLauncher.launch(intent);
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    MainActivity.this.finish();
                })
                .setCancelable(false);
        alert.show();
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void initFieldValues(PreferenceData preferenceData) {
        preferenceData.ReadPreferenceData(this);
        SwitchMaterial switchMaterial = findViewById(R.id.backupStateBtn);
        switchMaterial.setChecked(preferenceData.getBackupState());
        TextView ipAddr, port, username, password, dateThreshold,lastBackup;
        ipAddr = findViewById(R.id.ipaddrTextView);
        port = findViewById(R.id.portTextView);
        username = findViewById(R.id.usernameTextView);
        password = findViewById(R.id.passwordTextView);
        dateThreshold = findViewById(R.id.dateThresholdTextView);
        ipAddr.setText(preferenceData.getIPAddr());
        port.setText(preferenceData.getPort());
        username.setText(preferenceData.getUsername());
        password.setText(preferenceData.getPassword());
        String date = preferenceData.getDateThreshold().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        dateThreshold.setText(date);
        lastBackup = findViewById(R.id.lastBackupTextView);
        lastBackup.setText(preferenceData.getLastBackup());
    }

//    private void AlertAndAccessStoragePermissions(Context context,ActivityResultLauncher<Intent> manageStorageLauncher) {
//        if (Environment.isExternalStorageManager()){
//            return;
//        }
//        AlertDialog.Builder alert= new AlertDialog.Builder(context);
//        TextView textView = new TextView(context);
//        textView.setText("Please provide the permissions on the next screen to continue");
//        textView.setPadding(50,50,50,50);
//        alert.setTitle("Permissions needed").setView(textView)
//        .setPositiveButton("Ok",(dialog,which)->{
//            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//            intent.setData(Uri.parse("package:" + getPackageName()));
//            manageStorageLauncher.launch(intent);
//        }).setNegativeButton("Exit",(dialog,which)->{
//            MainActivity.this.finish();
//        }).setCancelable(false);
//        alert.show();
//    }

    //    private void RequestAllPermissions(Context context,ActivityResultLauncher<Intent> manageStorageLauncher){
//
//        ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),result-> {
//
//            if (!result){
//                Toast.makeText(this,"Notification permission is required!: "+ result.toString(),Toast.LENGTH_LONG).show();
//                finish();
//            }
//            Toast.makeText(this,"Notification permission granted",Toast.LENGTH_LONG).show();
//            // Now get MANAGE_EXTERNAL_STORAGE permission
//            AlertAndAccessStoragePermissions(context,manageStorageLauncher);
//        });
//        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_Material3)
//                .setTitle("Alert")
//                .setMessage("Notification permission is required, to show notification")
//                .setPositiveButton("Ok" ,(dialog,which)-> {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
//                    }
//                });
//
//        // If notification permission is already present, show MANAGE_EXTERNAL_STORAGE permission dialog
//        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
//            AlertAndAccessStoragePermissions(context,manageStorageLauncher);
//            return;
//        }
//        builder.show();
//    }
    private void configureBtnEventListeners(PreferenceData preferenceData) {
        TextView dummyTextView = new TextView(this);
        Hashtable<Integer, TextView> editBtnVsField = new Hashtable<>();
        editBtnVsField.put(R.id.ipaddrEditBtn, (TextView) findViewById(R.id.ipaddrTextView));
        editBtnVsField.put(R.id.portEditBtn, (TextView) findViewById(R.id.portTextView));
        editBtnVsField.put(R.id.usernameEditBtn, (TextView) findViewById(R.id.usernameTextView));
        editBtnVsField.put(R.id.passwordEditBtn, (TextView) findViewById(R.id.passwordTextView));
        editBtnVsField.put(R.id.dateThresholdEditBtn,(TextView) findViewById(R.id.dateThresholdTextView));
        editBtnVsField.put(R.id.foldersEditBtn,dummyTextView);
        editBtnVsField.forEach((id, textView) -> {
            findViewById(id).setOnClickListener(view -> {
                showEditValueDialog(id, textView, preferenceData);
            });
        });
        SwitchMaterial switchMaterial = findViewById(R.id.backupStateBtn);
        switchMaterial.setOnCheckedChangeListener((btnView, isChecked) -> {
            preferenceData.setBackupState(isChecked);
            preferenceData.WritePreferenceData(this);
        });
        Button pingBtn = findViewById(R.id.pingServerBtn);
        pingBtn.setOnClickListener(view -> {
            // Use Executors for better thread management in Android
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                //FTPConnectionV2 ftpConnection = new FTPConnectionV2(preferenceData);
                HTTPBasedFileClient httpBasedFileClient = new HTTPBasedFileClient();
                try {
                    //ftpConnection.ping();
                    httpBasedFileClient.ping();
                    Utils.runOnUiThread(() -> Utils.showToast(this, "Ping successful!"));
                    Log.d("FTP", "Connected and disconnected successfully");
                } catch (Exception e) {
                    // Handle UI updates on the main thread
                    Utils.runOnUiThread(() -> Utils.showToast(MainActivity.this, e.getMessage()));
                    Log.e("HTTPError", "Error: " + e); // Use Log.e for errors
                }
            });
            executor.shutdown(); // Good practice to shut down the executor when done
        });

        Button backupBtn = findViewById(R.id.backupNowBtn);
        backupBtn.setOnClickListener((view) -> {


            Intent intent = new Intent(this, BackupService.class);
            this.startForegroundService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            //updateBackupButtonState();
        });

        Hashtable<Integer, TextView> copyBtnVsField = new Hashtable<>();
        copyBtnVsField.put(R.id.copyIPImg, (TextView) findViewById(R.id.ipaddrTextView));
        copyBtnVsField.put(R.id.copyPortImg, (TextView) findViewById(R.id.portTextView));
        copyBtnVsField.put(R.id.copyUsernameImg, (TextView) findViewById(R.id.usernameTextView));
        copyBtnVsField.put(R.id.copyPasswordImg, (TextView) findViewById(R.id.passwordTextView));

        copyBtnVsField.forEach((id, textView) -> {
            findViewById(id).setOnClickListener(view -> {
                CharSequence value = textView.getText();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied text", value);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (id == R.id.copyPasswordImg) {
                        PersistableBundle extras = new PersistableBundle();
                        extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                        clip.getDescription().setExtras(extras);
                    }
                } else {
                    Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show();
                }
                clipboard.setPrimaryClip(clip);
            });
        });
    }

    // Used to show the edit dialog whenever edit button with id sourceWidgetId is clicked
    // Updates the new value to the targetTextView if it is not null
    private void showEditValueDialog(int sourceWidgetId, TextView targetTextView, PreferenceData preferenceData) {
        // Create an EditText for the input
        final EditText input = new EditText(this);
        // Fill the input with whatever the original value
        if(sourceWidgetId == R.id.ipaddrEditBtn){
            input.setText(preferenceData.getIPAddr());
        } else if (sourceWidgetId == R.id.portEditBtn)  {
            input.setText(preferenceData.getPort());
        }
        else if (sourceWidgetId == R.id.usernameEditBtn)  {
            input.setText(preferenceData.getUsername());
        }
        else if (sourceWidgetId == R.id.passwordEditBtn)  {
            input.setText(preferenceData.getPassword());
        }
        else if (sourceWidgetId == R.id.dateThresholdEditBtn)  {
            input.setText(preferenceData.getDateThreshold().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        else if (sourceWidgetId == R.id.foldersEditBtn)  {
            StringBuilder placeholder = new StringBuilder();
            for (int i = 0; i < preferenceData.Directories.length; i++) {
                if (i > 0) {
                    placeholder.append(", ");
                }
                placeholder.append(preferenceData.Directories[i]);
            }
            input.setText(placeholder.toString());
        }
        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter new value")
                .setView(input)
                .setPositiveButton("OK", (dialog, whichButton) -> {
                    // Set the input text to the TextView
                    String value = input.getText().toString();
                    if (targetTextView != null) {
                        targetTextView.setText(value);
                    }
                    if (sourceWidgetId == R.id.ipaddrEditBtn) {
                        preferenceData.setIPAddr(value);
                    } else if (sourceWidgetId == R.id.portEditBtn) {
                        preferenceData.setPort(value);
                    } else if (sourceWidgetId == R.id.usernameEditBtn) {
                        preferenceData.setUsername(value);
                    } else if (sourceWidgetId == R.id.passwordEditBtn) {
                        preferenceData.setPassword(value);
                    } else if (sourceWidgetId == R.id.dateThresholdEditBtn) {
                        LocalDate dateThreshold = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        preferenceData.setDateThreshold(dateThreshold);
                    } else if (sourceWidgetId == R.id.foldersEditBtn) {
                        String[] folders = value.split(",");
                        for (int i = 0; i < folders.length; i++) {
                            folders[i] = folders[i].trim();
                        }
                        preferenceData.setDirectories(folders);
                    }
                    preferenceData.WritePreferenceData(this);
                })
                .setNegativeButton("Cancel", (dialog, whichButton) -> {
                    // Dismiss the dialog
                    dialog.dismiss();
                })
                .show();
    }

    private boolean isBackupServiceRunning() {
        return isServiceBound;
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (BackupService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
    }

    private void setBackupButtonState(boolean enabled) {
        Button backupBtn = findViewById(R.id.backupNowBtn);
        int btnColor = ContextCompat.getColor(this, R.color.backupbtn_enabled_color);
        String btnText = getResources().getString(R.string.backupbtn_enabled_text);
        if (!enabled) {
            btnColor = ContextCompat.getColor(this, R.color.backupbtn_disabled_color);
            btnText = getResources().getString(R.string.backupbtn_disabled_text);
        }
        // Disable the button first
        backupBtn.setEnabled(enabled);
        // change color
        backupBtn.setBackgroundTintList(ColorStateList.valueOf(btnColor));
        // Change the button text
        backupBtn.setText(btnText);
    }

    private void updateBackupButtonState() {
        setBackupButtonState(!isBackupServiceRunning());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(backupCompleteReceiver);
    }

    private void onBackupComplete(Intent intent) {
        int backupResultStatus = intent.getIntExtra("backup_result_status_key", -1);
        String backupResultMessage = intent.getStringExtra("backup_result_message_key");
        backupNotificationManager.notifyBackupFinishedNotification(this,backupResultStatus,backupResultMessage);
        // Use the extracted integer value
        if (backupResultStatus == 0) {
            // Backup was successful
            // TODO Update last backup info
            showToast("Backup completed successfully");
            TextView lastBackup = findViewById(R.id.lastBackupTextView);
            PreferenceData preferenceData = PreferenceData.getPreferenceData();
            preferenceData.setLastBackup("",true);
            preferenceData.WritePreferenceData(this);
            lastBackup.setText(preferenceData.getLastBackup());
        } else {
            // Backup failed or encountered an error
            // ... handle the error
            showToast("Backup failed");
        }
    }

    private void createAndRegisterBroadcastReceiver() {
        backupCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Unbind the service, enable backup button
                setBackupButtonState(true);
                if (isServiceBound) {
                    unbindService(serviceConnection);
                    isServiceBound = false;
                }
                onBackupComplete(intent);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                backupCompleteReceiver,
                new IntentFilter("BACKUP_COMPLETED")
        );
    }
}