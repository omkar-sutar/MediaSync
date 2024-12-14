package com.dubu.MediaSync;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPConnector;

public class FTPConnectionV2 {
    private final PreferenceData preferenceData;
    private final FTPClient ftpClient;
    public FTPConnectionV2(PreferenceData preferenceData){
        this.preferenceData = preferenceData;
        ftpClient = new FTPClient();
    }

    public  void connect() throws Exception,CustomExceptions.UserUnauthorizedException {
        ftpClient.setConnector(new CustomFTPConnector(5000,40000));
        ftpClient.connect(preferenceData.getIPAddr(), Integer.parseInt(preferenceData.getPort()));

        // Login to the server
        try{
            ftpClient.login(preferenceData.getUsername(), preferenceData.getPassword());
        }catch (FTPException e){
            throw new CustomExceptions.UserUnauthorizedException(e.getMessage());
        }

    }
    public void disconnect() throws Exception{
        ftpClient.disconnect(true);
    }

    public void ping() throws Exception{
        connect();
        ftpClient.listNames();
        disconnect();
    }

    public void putFile(File file, String remoteFilePath) throws Exception {
        //if(ftpClient.list(remoteFilePath).length!=0)return;
        try (FileInputStream fis = new FileInputStream(file)) {
            ftpClient.upload(file);
        }
    }
    public String[] filterFilesIfExist(String[] localFilePaths) throws Exception {
        FTPFile[] filesOnServer = ftpClient.list();
        HashSet<String> set = new HashSet<>();
        for(FTPFile file:filesOnServer) set.add(file.getName());
        ArrayList<String> uniqueFiles=new ArrayList<>();
        for(String localFilePath:localFilePaths){
            String localFilename=localFilePath.substring(localFilePath.lastIndexOf("/")+1);
            if(set.contains(localFilename))continue;
            uniqueFiles.add(localFilePath);
        }
        return uniqueFiles.toArray(new String[0]);
    }

    /**
     * Used to set custom connection and read timeouts
     */
    public class CustomFTPConnector extends FTPConnector {

        private final int connectTimeout;
        private final int readTimeout;

        public CustomFTPConnector(int connectTimeout, int readTimeout) {
            this.connectTimeout = connectTimeout;
            this.readTimeout = readTimeout;
        }

        @Override
        public Socket connectForCommunicationChannel(String host, int port) throws IOException {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setSoTimeout(readTimeout);
            return socket;
        }

        @Override
        public Socket connectForDataTransferChannel(String host, int port) throws IOException {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setSoTimeout(readTimeout);
            return socket;
        }
    }

}
