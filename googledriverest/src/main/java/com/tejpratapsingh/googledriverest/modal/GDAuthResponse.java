package com.tejpratapsingh.googledriverest.modal;

import android.util.Log;

import com.tejpratapsingh.googledriverest.Helper.GDException;

import java.util.Locale;

public class GDAuthResponse {

    public interface OnAuthResponseListener {
        void onSuccess(GDAuthResponse gdAuthResponse);

        void onError(GDException exception);
    }

    private String accessToken, refreshToken;
    private long expiresAtTimestamp;

    public GDAuthResponse(String accessToken, String refreshToken, long expiresAtTimestamp) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAtTimestamp = expiresAtTimestamp;

        Log.d("", "GDAuthResponse: " + this.toString());
        System.out.println("isExpired: " + this.isExpired());
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public long getExpiresAtTimestamp() {
        return this.expiresAtTimestamp;
    }

    public boolean isExpired() {
        if ((System.currentTimeMillis() / 1000) > this.expiresAtTimestamp - 90) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "accessToken: %s,\nrefreshToken: %s,\nexpiresAtTimestamp: %d,\ntimeRemaining: %d",
                this.accessToken, this.refreshToken, this.expiresAtTimestamp, this.expiresAtTimestamp - (System.currentTimeMillis() / 1000));
    }
}
