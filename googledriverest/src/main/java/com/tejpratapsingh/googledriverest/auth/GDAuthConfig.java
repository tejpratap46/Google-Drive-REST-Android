package com.tejpratapsingh.googledriverest.auth;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

import com.tejpratapsingh.googledriverest.Helper.GDException;

public class GDAuthConfig {

    private String redirectURI, clientId, clientSecret;
    private ArrayList<SCOPES> scopes;

    public enum SCOPES {

        EMAIL("email"),
        APP_FOLDER("https://www.googleapis.com/auth/drive.appfolder"),
        DRIVE("https://www.googleapis.com/auth/drive.file"),
        DRIVE_FULL("https://www.googleapis.com/auth/drive"),
        READ_ONLY("https://www.googleapis.com/auth/drive.readonly"),
        MATADATA("https://www.googleapis.com/auth/drive.metadata"),
        APP_SCRIPT("https://www.googleapis.com/auth/drive.scripts");

        private String stringValue;

        SCOPES(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }
    }

    public GDAuthConfig(String redirectURI, String clientId, String clientSecret, ArrayList<SCOPES> scopes) throws GDException {

        if (redirectURI == null || redirectURI.isEmpty()) {
            throw new GDException("redirectURI cannot be null");
        } else if (clientId == null || clientId.isEmpty()) {
            throw new GDException("clientId cannot be null");
        } else if (scopes == null || scopes.size() == 0) {
            throw new GDException("scopes cannot be empty");
        }

        this.redirectURI = redirectURI;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAuthURL() {

        ArrayList<String> scopesStringArray = new ArrayList<>();
        for (SCOPES scope : this.scopes) {
            scopesStringArray.add(scope.getStringValue());
        }

        String scopesCSV = TextUtils.join("%20", scopesStringArray);

        return String.format(Locale.getDefault(), "https://accounts.google.com/o/oauth2/v2/auth?scope=%s&access_type=offline&prompt=consent&include_granted_scopes=true&state=state_parameter_passthrough_value&redirect_uri=%s&response_type=code&client_id=%s",
                scopesCSV, this.redirectURI, this.clientId);
    }
}
