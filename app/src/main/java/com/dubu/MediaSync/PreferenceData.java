package com.dubu.MediaSync;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PreferenceData{
    private static PreferenceData preferenceData=null;

//    public static class Directory{
//        public String path;
//        final public String PATHKEY = "path";
//        public Directory(String path) {
//            this.path = path;
//        }
//        public Directory(){}
//    }
    final private String IPADDRKEY = "IPADDR";
    final private String PORTKEY = "PORT";
    final private String USERNAMEKEY = "USERNAME";
    final private String PASSWORDKEY = "PASSWORD";
    final private String BACKUPSTATEKEY = "BACKUPSTATE";
    final private String DATETHRESHOLD = "DATETHRESHOLD";
    final private String DirectoriesKey = "Directories";
    private String IPAddr = "192.168.1.1";
    private String Port = "8080";
    private String Username = "admin";
    private String Password = "admin";

    private LocalDate DateThreshold;
    private Boolean BackupState = false;

    String[] Directories = new String[]{"%DCIM/Camera%"};

    public String[] getDirectories() {
        return Directories;
    }

    public void setDirectories(String[] directories) {
        Directories = directories;
    }

    public void setIPAddr(String IPAddr) {
        this.IPAddr = IPAddr;
    }

    public void setPort(String port) {
        Port = port;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public void setBackupState(Boolean backupState) {
        BackupState = backupState;
    }

    public String getIPAddr() {
        return IPAddr;
    }

    public String getPort() {
        return Port;
    }

    public String getUsername() {
        return Username;
    }

    public String getPassword() {
        return Password;
    }
    public Boolean getBackupState(){
        return BackupState;
    }
    public LocalDate getDateThreshold() {
        return DateThreshold;
    }

    public void setDateThreshold(LocalDate dateThreshold) {
        DateThreshold = dateThreshold;
    }
    
    public void ReadPreferenceData(Activity activity){
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        //SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        IPAddr = sharedPreferences.getString(IPADDRKEY,"192.168.1.1");
        Port = sharedPreferences.getString(PORTKEY,"8080");
        Username = sharedPreferences.getString(USERNAMEKEY,"admin");
        Password = sharedPreferences.getString(PASSWORDKEY,"admin");
        BackupState = sharedPreferences.getBoolean(BACKUPSTATEKEY,false);
        String dateThreshold = sharedPreferences.getString(DATETHRESHOLD,"01/12/2001");
        DateThreshold = LocalDate.parse(dateThreshold, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String directoriesJSONData = sharedPreferences.getString(DirectoriesKey,"");
        if (!directoriesJSONData.isEmpty()){
            try {
                JSONArray jsonArray = new JSONArray(directoriesJSONData);
                String[] directories = new String[jsonArray.length()];
                for(int i=0;i<jsonArray.length();i++){
                    String directory = jsonArray.getString(i);
                    directories[i] = directory;
                }
                 Directories = directories;
            } catch (Exception e) {
                Toast.makeText(activity.getApplicationContext(),"Failed to convert json to obj: "+e.toString(), Toast.LENGTH_LONG).show();
            }

        }
    }
    public void WritePreferenceData(Activity activity){
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(IPADDRKEY,IPAddr);
        editor.putString(PORTKEY,Port);
        editor.putString(USERNAMEKEY,Username);
        editor.putString(PASSWORDKEY,Password);
        editor.putBoolean(BACKUPSTATEKEY,BackupState);
        editor.putString(DATETHRESHOLD,DateThreshold.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        JSONArray jsonArray = new JSONArray();

        try {
            for (String directory : Directories) {
                jsonArray.put(directory);
            }
        } catch (Exception e) {
            Toast.makeText(activity.getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }

        editor.putString(DirectoriesKey,jsonArray.toString());
        editor.apply();
    }
    private PreferenceData(){}

    /**
     * Provides access to the singleton instance of {@link PreferenceData}.
     *
     * This method ensures that only one instance of {@link PreferenceData} exists throughout the
     * application's lifecycle. If the instance hasn't been created yet, it initializes a new one.
     * Make sure ReadPreferenceData is called atleast once before accessing any data.
     *
     * @return The singleton instance of {@link PreferenceData}.
     */
    public static PreferenceData getPreferenceData() {
        if(preferenceData==null){
            preferenceData = new PreferenceData();
        }
        return preferenceData;
    }
}
