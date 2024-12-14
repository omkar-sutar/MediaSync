package com.dubu.MediaSync;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;

public class BackupSession{
    private final PreferenceData preferenceData = PreferenceData.getPreferenceData();
    private FTPClient ftpClient;
    private HTTPBasedFileClient httpBasedFileClient;
    BackupSession() throws Exception {
        httpBasedFileClient = new HTTPBasedFileClient();
        httpBasedFileClient.connect();
    }

    public void StartBackup(String[] filepaths, OnProgressCallback callback) throws Exception {
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
        // First filter out files that are already present on server
        String[] filteredFilepaths = httpBasedFileClient.filterFilesIfExist(filepaths);
        int totalFiles = filteredFilepaths.length;
        if (totalFiles==0)return;

//        AtomicInteger count = new AtomicInteger(0);
//        runAsync(()->callback.onProgress(0,totalFiles));
//
//        for (int i=0;i<totalFiles;i++){
//            String filepath = filteredFilepaths[i];
//            File file = new File(filepath);
//            executorService.execute(()->{
//                try {
//                    httpBasedFileClient.putFile(file);
//                    count.incrementAndGet();
//                    runAsync(()->callback.onProgress(count.get(),totalFiles));
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//        }
//        executorService.shutdown();
//        while (!executorService.isTerminated()){
//            Thread.sleep(500);
//        }

        for (int i=0;i<totalFiles;i++) {
            int currentProgress = i;
            runAsync(()->callback.onProgress(currentProgress,totalFiles));
            String filepath = filteredFilepaths[i];
            File file = new File(filepath);
            httpBasedFileClient.putFile(file);
        }
    }

    public interface OnProgressCallback{
        void onProgress(int progress,int max);
    }
    private void runAsync(Runnable runnable){
        new Thread(runnable).start();
    }
}
