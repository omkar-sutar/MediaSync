package com.dubu.MediaSync;

import static java.nio.file.Files.getLastModifiedTime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.json.JSONArray;
import org.json.JSONObject;

public class HTTPBasedFileClient {
    private final PreferenceData preferenceData = PreferenceData.getPreferenceData();
    private String FileUploadPath = "http://%s:%s/api/media/upload";
    private String PingPath = "http://%s:%s/ping";
    private String FilesListPath = "http://%s:%s/api/media";
    private String LoginPath = "http://%s:%s/login";
    private final String JWTHeaderKey = "x-access-token";
    private String jwt;

    // Create response handler
    private final HttpClientResponseHandler<HTTPResponse> responseHandler = response -> {
        int status = response.getCode();
        String responseBody = EntityUtils.toString(response.getEntity());
        return new HTTPResponse(status,responseBody);
    };

    // Set up the timeout
    private final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(10, TimeUnit.SECONDS))
            .build();

    private static class HTTPResponse{
        int status;
        String body;
        public HTTPResponse(int status,String body){
            this.status=status;
            this.body=body;
        }
    }

    public void putFile(File file) throws Exception {
        String url = String.format(FileUploadPath,preferenceData.getIPAddr(),preferenceData.getPort());

        // Create MultipartEntityBuilder
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addPart("files",new FileBody(file, ContentType.DEFAULT_BINARY));
        builder.addPart("lastModifiedTime",new StringBody(String.valueOf(file.lastModified()), ContentType.TEXT_PLAIN));

        // Build the multipart entity
        HttpEntity multipart = builder.build();

        // Create HttpPost request
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(multipart);
        httpPost.addHeader(JWTHeaderKey, jwt);


        // Create HttpClient with custom configuration
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                //.setDefaultRequestConfig(requestConfig)
                .build()) {

            // Send request and get response
            HTTPResponse httpResponse = httpClient.execute(httpPost, responseHandler);
            Exception httpError = httpErrorToException(httpResponse);
            if (httpError!=null)throw httpError;
        }

    }

    public void connect() throws Exception{
        String url = String.format(LoginPath, preferenceData.getIPAddr(), preferenceData.getPort());
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type","application/json");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username",preferenceData.getUsername());
        jsonObject.put("password",preferenceData.getPassword());
        httpPost.setEntity(new StringEntity(jsonObject.toString()));

        // Create HttpClient with custom configuration
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            // Send request and get response
            HTTPResponse httpResponse = httpClient.execute(httpPost, responseHandler);
            Exception httpError = httpErrorToException(httpResponse);
            if (httpError != null) throw httpError;

            JSONObject jsonResponse = new JSONObject(httpResponse.body);
            jwt = jsonResponse.getString("token");
        }
    }

    public String[] getFiles() throws Exception{
        String url = String.format(FilesListPath, preferenceData.getIPAddr(), preferenceData.getPort());
        String[] filenames;
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(JWTHeaderKey,jwt);

        // Create HttpClient with custom configuration
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            // Send request and get response
            HTTPResponse httpResponse = httpClient.execute(httpGet, responseHandler);
            Exception httpError = httpErrorToException(httpResponse);
            if (httpError != null) throw httpError;

            JSONObject jsonObject = new JSONObject(httpResponse.body);
            JSONArray files = jsonObject.getJSONArray("files");
            filenames = new String[files.length()];
            for (int i = 0; i < files.length(); i++) {
                JSONObject singleFile=files.getJSONObject(i);
                filenames[i] = singleFile.getString("filename");
            }
        }
        return filenames;
    }

    public String[] filterFilesIfExist(String[] localFilePaths) throws Exception {
        String[] remoteFiles = getFiles();
        HashSet<String> set = new HashSet<>(Arrays.asList(remoteFiles));
        ArrayList<String> uniqueFiles=new ArrayList<>();
        for (String localFilePath : localFilePaths) {
            String localFilename=localFilePath.substring(localFilePath.lastIndexOf("/")+1);
            if (set.contains(localFilename))continue;
            uniqueFiles.add(localFilePath);
        }
        return uniqueFiles.toArray(new String[0]);
    }
    public void ping() throws Exception {

        connect();

        String url = String.format(PingPath,preferenceData.getIPAddr(),preferenceData.getPort());
        //String serverUrl = "http://" + PreferenceData.getPreferenceData().getIPAddr() + ":" +  + "/upload";

        // Create MultipartEntityBuilder
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        // Build the multipart entity
        HttpEntity multipart = builder.build();

        // Create HttpPost request
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(JWTHeaderKey,jwt);

        // Set up the timeout
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
                .setResponseTimeout(Timeout.of(10, TimeUnit.SECONDS))
                .build();


        // Create HttpClient with custom configuration
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            // Send request and get response
            HTTPResponse httpResponse = httpClient.execute(httpGet, responseHandler);
            Exception httpError = httpErrorToException(httpResponse);
            if (httpError!=null)throw httpError;
        }
    }
    private Exception httpErrorToException(HTTPResponse httpResponse){
        switch (httpResponse.status){
            case 200:
                return null;
            case 401:
                return new CustomExceptions.UserUnauthorizedException(httpResponse.body);
            default:
                return new Exception(httpResponse.body);
        }
    }
}