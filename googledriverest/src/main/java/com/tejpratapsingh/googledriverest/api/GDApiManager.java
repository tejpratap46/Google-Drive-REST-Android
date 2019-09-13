package com.tejpratapsingh.googledriverest.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.tejpratapsingh.googledriverest.Helper.GDConstants;
import com.tejpratapsingh.googledriverest.Helper.GDException;
import com.tejpratapsingh.googledriverest.Helper.GDFileManager;
import com.tejpratapsingh.googledriverest.auth.GDAuthConfig;
import com.tejpratapsingh.googledriverest.modal.GDAuthResponse;
import com.tejpratapsingh.googledriverest.modal.GDDeleteFileResponse;
import com.tejpratapsingh.googledriverest.modal.GDDownloadFileResponse;
import com.tejpratapsingh.googledriverest.modal.GDUploadFileResponse;
import com.tejpratapsingh.googledriverest.modal.GDUserInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class GDApiManager {
    private static final String TAG = "GDApiManager";

    private OkHttpClient client;

    private static final GDApiManager ourInstance = new GDApiManager();

    public static GDApiManager getInstance() {
        return ourInstance;
    }

    private GDApiManager() {
        this.client = new OkHttpClient();
    }

    public void getAuthFromCodeAsync(final String code, final GDAuthConfig config, final GDAuthResponse.OnAuthResponseListener onAuthResponseListener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    onAuthResponseListener.onSuccess(getInstance().getAuthFromCode(code, config));
                } catch (GDException e) {
                    onAuthResponseListener.onError(e);
                }
            }
        });
    }

    public GDAuthResponse getAuthFromCode(final String code, final GDAuthConfig config) throws GDException {
        String requestBody = String.format("code=%s&client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=authorization_code", code, config.getClientId(), config.getClientSecret(), config.getRedirectURI());

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, requestBody);
        Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        try {
            Response response = getInstance().client.newCall(request).execute();
            try {
                JSONObject responseJSON = new JSONObject(response.body().string());

                GDAuthResponse gdAuthResponse = new GDAuthResponse(
                        responseJSON.getString("access_token"),
                        responseJSON.getString("refresh_token"),
                        (System.currentTimeMillis() / 1000) + responseJSON.getLong("expires_in")
                );

                return gdAuthResponse;

            } catch (JSONException e) {
                e.printStackTrace();
                throw new GDException(e.getMessage());
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new GDException(e.getMessage());
        }
    }

    public void getAuthFromRefreshTokenAsync(final Context context, final GDAuthResponse previousAuthResponse, final GDAuthConfig config, final GDAuthResponse.OnAuthResponseListener onAuthResponseListener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    onAuthResponseListener.onSuccess(getInstance().getAuthFromRefreshToken(context, previousAuthResponse, config));
                } catch (GDException e) {
                    onAuthResponseListener.onError(e);
                }
            }
        });
    }

    public GDAuthResponse getAuthFromRefreshToken(final Context context, final GDAuthResponse previousAuthResponse, final GDAuthConfig config) throws GDException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(
                mediaType, String.format(Locale.getDefault(), "client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=refresh_token&refresh_token=%s", config.getClientId(), config.getClientSecret(), config.getRedirectURI(), previousAuthResponse.getRefreshToken())
        );
        Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .post(body)
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        try {
            Response response = getInstance().client.newCall(request).execute();
            try {
                JSONObject responseJSON = new JSONObject(response.body().string());

                GDAuthResponse gdAuthResponse = new GDAuthResponse(
                        responseJSON.getString("access_token"),
                        previousAuthResponse.getRefreshToken(),
                        (System.currentTimeMillis() / 1000) + responseJSON.getInt("expires_in")
                );

                SharedPreferences.Editor editor = context.getSharedPreferences(GDConstants.GD_PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString(GDConstants.GD_PREFS_ACCESS_TOKEN, gdAuthResponse.getAccessToken());
                editor.putString(GDConstants.GD_PREFS_REFRESH_TOKEN, gdAuthResponse.getRefreshToken());
                editor.putLong(GDConstants.GD_PREFS_TOKEN_EXPIRES_AT, gdAuthResponse.getExpiresAtTimestamp());
                editor.apply();

                return gdAuthResponse;

            } catch (JSONException e) {
                e.printStackTrace();
                throw new GDException(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new GDException(e.getMessage());
        }
    }

    /**
     * Get user information if you asked for SCOPE: EMAIL
     * in BACKGROUND
     *
     * @param gdAuthResponse             Auth credentials
     * @param onUserInfoReceivedListener onComplete event listener
     */
    public void getUserInfoAsync(final Context context, final GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig, final GDUserInfo.OnUserInfoReceivedListener onUserInfoReceivedListener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    onUserInfoReceivedListener.onSuccess(getInstance().getUserInfo(context, gdAuthResponse, authConfig));
                } catch (GDException e) {
                    e.printStackTrace();
                    onUserInfoReceivedListener.onError(e);
                }
            }
        });
    }

    /**
     * Get user information if you asked for SCOPE: EMAIL
     * in CURRENT thread
     *
     * @param gdAuthResponse Auth credentials
     * @return user info
     * @throws GDException if any error occurred
     */
    public GDUserInfo getUserInfo(final Context context, GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig) throws GDException {

        if (gdAuthResponse.isExpired()) {
            // Get access token again from refresh token
            gdAuthResponse = this.getAuthFromRefreshToken(context, gdAuthResponse, authConfig);
        }
        Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + gdAuthResponse.getAccessToken())
                .get()
                .build();

        try {
            Response response = getInstance().client.newCall(request).execute();

            try {
                JSONObject userJSONObject = new JSONObject(response.body().string());

                return new GDUserInfo(userJSONObject.has("name") ? userJSONObject.getString("name") : "",
                        userJSONObject.getString("email"), // Email is not optional
                        userJSONObject.has("profile") ? userJSONObject.getString("profile") : "",
                        userJSONObject.has("picture") ? userJSONObject.getString("picture") : "");

            } catch (JSONException e) {
                e.printStackTrace();
                throw new GDException(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new GDException(e.getMessage());
        }
    }

    /**
     * Upload file to google drive in BACKGROUND thread
     *
     * @param gdAuthResponse     auth credentials from google response
     * @param fileToUpload       file to upload to google drive
     * @param fileMime           mime type of file, can be fetched from GDFileManager.getMimeType
     * @param uploadToAppFolder  true if you want to use app folder in google drive (files won't be visible to user)
     * @param uploadFileListener listener for success or exception
     */
    public void uploadFileAsync(final Context context, final GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig, final File fileToUpload, final String fileMime, final boolean uploadToAppFolder, final GDUploadFileResponse.OnUploadFileCompleteListener uploadFileListener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GDUploadFileResponse gdUploadFileResponse = getInstance().uploadFile(context, gdAuthResponse, authConfig, fileToUpload, fileMime, uploadToAppFolder);
                    uploadFileListener.onSuccess(gdUploadFileResponse);
                } catch (GDException e) {
                    uploadFileListener.onError(e);
                }
            }
        });
    }

    /**
     * Upload file to google drive in CURRENT thread
     *
     * @param gdAuthResponse    auth credentials from google response
     * @param fileToUpload      file to upload to google drive
     * @param fileMime          mime type of file, can be fetched from GDFileManager.getMimeType
     * @param uploadToAppFolder true if you want to use app folder in google drive (files won't be visible to user)
     * @return GDUploadFileResponse object with fileId and name
     * @throws GDException if any error occurred
     */
    public GDUploadFileResponse uploadFile(final Context context, GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig, final File fileToUpload, final String fileMime, final boolean uploadToAppFolder) throws GDException {

        if (gdAuthResponse.isExpired()) {
            // Get access token again from refresh token
            gdAuthResponse = this.getAuthFromRefreshToken(context, gdAuthResponse, authConfig);
        }
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create("{\"name\": \"" + fileToUpload.getName() + "\"}", mediaType);
        if (uploadToAppFolder) {
            body = RequestBody.create("{\"name\": \"" + fileToUpload.getName() + "\", \"parents\":[\"appDataFolder\"]}", mediaType);
        }
        Request fileCreateRequest = new Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files")
                .post(body)
                .addHeader("Authorization", "Bearer " + gdAuthResponse.getAccessToken())
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .build();

        try {
            Response fileCreateResponse = getInstance().client.newCall(fileCreateRequest).execute();

            JSONObject fileCreteResponseJSONObject = new JSONObject(fileCreateResponse.body().string());

            MediaType mediaMimeType = MediaType.parse(fileMime);

            Request fileUploadRequest = new Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files/" + fileCreteResponseJSONObject.getString("id") + "?uploadType=media")
                    .patch(RequestBody.create(mediaMimeType, fileToUpload))
                    .addHeader("Authorization", "Bearer " + gdAuthResponse.getAccessToken())
                    .addHeader("Content-Type", fileMime)
                    .addHeader("Content-Length", "" + fileToUpload.length())
                    .build();

            try {
                Response fileUploadResponse = getInstance().client.newCall(fileUploadRequest).execute();

                JSONObject fileUploadResponseJSONObject = new JSONObject(fileUploadResponse.body().string());

                return new GDUploadFileResponse(
                        fileUploadResponseJSONObject.getString("id"),
                        fileUploadResponseJSONObject.getString("name"),
                        fileUploadResponseJSONObject.getString("mimeType")
                );
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                throw new GDException(e.getMessage());
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            throw new GDException(e.getMessage());
        }
    }


    /**
     * Download file in BACKGROUND thread
     *
     * @param context                      to get local folder of application
     * @param gdAuthResponse               Auth credentials
     * @param gdFileId                     fileId to download
     * @param fileName                     name of saved file
     * @param downloadFileCompleteListener on complete event
     */
    public void downloadFileAsync(final Context context, final GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig, final String gdFileId, final String fileName, final GDDownloadFileResponse.OnDownloadFileCompleteListener downloadFileCompleteListener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadFileCompleteListener.onSuccess(getInstance().downloadFile(context, gdAuthResponse, authConfig, gdFileId, fileName));
                } catch (GDException e) {
                    downloadFileCompleteListener.onError(e);
                }
            }
        });
    }


    /**
     * Download file in CURRENT thread
     *
     * @param context        to get local folder of application
     * @param gdAuthResponse Auth credentials
     * @param gdFileId       fileId to download
     * @param fileName       name of saved file
     * @return saved File
     * @throws GDException if any error occurred
     */
    public File downloadFile(final Context context, GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig, final String gdFileId, final String fileName) throws GDException {

        if (gdAuthResponse.isExpired()) {
            // Get access token again from refresh token
            gdAuthResponse = this.getAuthFromRefreshToken(context, gdAuthResponse, authConfig);
        }
        Request request = new Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/" + gdFileId + "?alt=media")
                .get()
                .addHeader("authorization", "Bearer " + gdAuthResponse.getAccessToken())
                .build();

        try {
            Response response = getInstance().client.newCall(request).execute();
            if (response.isSuccessful()) {
                InputStream fileInputStream = response.body().byteStream();

                File savedFile = GDFileManager.getInstance().saveFileToPrivateStorageFromInputStream(context, fileInputStream, fileName, true);

                return savedFile;
            } else {
                throw new GDException("File not found on Google Drive");
            }
        } catch (IOException | GDException e) {
            e.printStackTrace();
            throw new GDException(e.getMessage());
        }

    }


    /**
     * Download file in BACKGROUND thread
     *
     * @param context                      to get local folder of application
     * @param gdAuthResponse               Auth credentials
     * @param gdFileId                     fileId to delete
     * @param deleteFileCompleteListener on complete event
     */
    public void deleteFileAsync(final Context context, final GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig, final String gdFileId, final GDDeleteFileResponse.OnDeleteFileListener deleteFileCompleteListener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Boolean isFileDeleted = getInstance().deleteFile(context, gdAuthResponse, authConfig, gdFileId);
                    if (isFileDeleted) {
                        deleteFileCompleteListener.onSucess();
                    } else {
                        deleteFileCompleteListener.onError(null);
                    }
                } catch (GDException e) {
                    deleteFileCompleteListener.onError(e);
                }
            }
        });
    }


    /**
     * Download file in CURRENT thread
     *
     * @param context        to get local folder of application
     * @param gdAuthResponse Auth credentials
     * @param gdFileId       fileId to delete
     * @return true if file deleted
     * @throws GDException if any error occurred
     */
    public Boolean deleteFile(final Context context, GDAuthResponse gdAuthResponse, final GDAuthConfig authConfig, final String gdFileId) throws GDException {

        if (gdAuthResponse.isExpired()) {
            // Get access token again from refresh token
            gdAuthResponse = this.getAuthFromRefreshToken(context, gdAuthResponse, authConfig);
        }
        Request request = new Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/" + gdFileId)
                .delete()
                .addHeader("authorization", "Bearer " + gdAuthResponse.getAccessToken())
                .build();

        try {
            Response response = getInstance().client.newCall(request).execute();

            return response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
            throw new GDException(e.getMessage());
        }

    }
}
