package com.tejpratapsingh.googledriverest.modal;

public class GDDeleteFileResponse {
    public interface OnDeleteFileListener {
        void onSucess();
        void onError(Exception e);
    }
}
