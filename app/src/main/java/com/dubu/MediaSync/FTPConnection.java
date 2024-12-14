package com.dubu.MediaSync;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class FTPConnection {
    private final PreferenceData preferenceData;
    private FTPClient ftpClient;
    public FTPConnection(PreferenceData preferenceData){
        this.preferenceData = preferenceData;
    }
    public FTPClient connect() throws IOException, CustomExceptions.UserUnauthorizedException {
        ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(5000);
        ftpClient.connect(preferenceData.getIPAddr(),Integer.parseInt(preferenceData.getPort()));
        boolean success = ftpClient.login(preferenceData.getUsername(),preferenceData.getPassword());
        if(!success){
            throw new CustomExceptions.UserUnauthorizedException();
        }
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        return ftpClient;
    }
    public void putFile(String localFilePath, String remoteFilePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(localFilePath)) {
            ftpClient.storeFile(remoteFilePath,fis);
        }
    }
    public void putFile(File file, String remoteFilePath) throws IOException {
        if(ftpClient.listFiles(remoteFilePath).length!=0)return;
        try (FileInputStream fis = new FileInputStream(file)) {
            ftpClient.storeFile(remoteFilePath,fis);
        }
    }
    public String[] filterFilesIfExist(String[] localFilePaths) throws IOException {
        String[] filesOnServer = ftpClient.listNames();
        HashSet<String> set = new HashSet<>();
        for(String filename:filesOnServer) set.add(filename);
        ArrayList<String> uniqueFiles=new ArrayList<>();
        for(String localFilePath:localFilePaths){
            String localFilename=localFilePath.substring(localFilePath.lastIndexOf("/")+1);
            if(set.contains(localFilename))continue;
            uniqueFiles.add(localFilePath);
        }
        return uniqueFiles.toArray(new String[0]);
    }
}
