package com.tejpratapsingh.googledriverest.modal;

import com.tejpratapsingh.googledriverest.Helper.GDException;

import java.io.File;

public class GDDownloadFileResponse {
    public interface OnDownloadFileCompleteListener {
        void onSuccess(File downloadedFile);
        void onError(GDException exception);
    }
}
