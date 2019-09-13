package com.tejpratapsingh.googledriverest.modal;

import com.tejpratapsingh.googledriverest.Helper.GDException;

public class GDUserInfo {

    public interface OnUserInfoReceivedListener {
        void onSuccess(GDUserInfo userInfo);
        void onError(GDException exception);
    }

    private String name, email, profile, picture;

    public GDUserInfo(String name, String email, String profile, String picture) {
        this.name = name;
        this.email = email;
        this.profile = profile;
        this.picture = picture;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfile() {
        return profile;
    }

    public String getPicture() {
        return picture;
    }
}
