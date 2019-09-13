package com.tejpratapsingh.googledriverest.auth;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tejpratapsingh.googledriverest.Helper.GDConstants;
import com.tejpratapsingh.googledriverest.Helper.GDException;
import com.tejpratapsingh.googledriverest.Helper.GDUtilities;
import com.tejpratapsingh.googledriverest.api.GDApiManager;
import com.tejpratapsingh.googledriverest.modal.GDAuthResponse;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import static android.content.Context.MODE_PRIVATE;

public class GDAuthManager {

    private static final String TAG = "GDAuthManager";

    public interface OnGoogleAuthCompleteListener {
        // Web browser is loading page
        void onLoadingStart();
        // Web browser finished loading page
        void onLoadingFinish();
        // Finished with success
        void onSuccess(GDAuthResponse gdAuthResponse);
        // Finished with error
        void onError(GDException exception);
    }

    private static final GDAuthManager ourInstance = new GDAuthManager();

    public static GDAuthManager getInstance() {
        return ourInstance;
    }

    private GDAuthManager() {
    }

    public void startGoogleDriveAuth(final Activity activity, final WebView webView, final GDAuthConfig config, final OnGoogleAuthCompleteListener onGoogleAuthCompleteListener) {

        try {
            GDAuthResponse gdAuthResponse = getAuthData(activity.getApplicationContext());
            if (gdAuthResponse.isExpired() == false) {
                onGoogleAuthCompleteListener.onSuccess(gdAuthResponse);
                Log.d(TAG, "startGoogleDriveAuth: OLD AUTH IS NOT EXPIRED, USE it");
                return;
            }
        } catch (GDException e) {
            e.printStackTrace();
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        String userAgent = String.format("%s", webView.getSettings().getUserAgentString().replace("; wv)", ")"));
        Log.d(TAG, "onCreateWindow: userAgent: " + userAgent);
        webView.getSettings().setUserAgentString(userAgent);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.setWebChromeClient(new MyWebChromeClient());
        enableHTML5AppCache(webView);

        webView.loadUrl(config.getAuthURL());
        onGoogleAuthCompleteListener.onLoadingStart();
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                onGoogleAuthCompleteListener.onLoadingFinish();
                // do your stuff here
                Log.d(TAG, "onPageFinished: Current URL: " + url);
                Log.d(TAG, "onPageFinished: url.matched: " + url.toLowerCase().startsWith(config.getRedirectURI().toLowerCase()));
                if (url.toLowerCase().startsWith(config.getRedirectURI().toLowerCase())) {
                    // DO Auth API CALLS
                    try {
                        URL urlObject = new URL(url);
                        try {
                            String code = GDUtilities.splitQuery(urlObject).get("code");
                            Log.d(TAG, "onPageFinished: Auth Finished with code: " + code);
                            Log.d(TAG, "onPageFinished: Getting access code from request code");

                            GDApiManager gdApiManager = GDApiManager.getInstance();
                            gdApiManager.getAuthFromCodeAsync(code, config, new GDAuthResponse.OnAuthResponseListener() {
                                @Override
                                public void onSuccess(final GDAuthResponse gdAuthResponse) {
                                    Log.d(TAG, "onSuccess: Auth Finished: " + gdAuthResponse.toString());
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SharedPreferences.Editor editor = activity.getSharedPreferences(GDConstants.GD_PREFS_NAME, MODE_PRIVATE).edit();
                                            editor.putString(GDConstants.GD_PREFS_ACCESS_TOKEN, gdAuthResponse.getAccessToken());
                                            editor.putString(GDConstants.GD_PREFS_REFRESH_TOKEN, gdAuthResponse.getRefreshToken());
                                            editor.putLong(GDConstants.GD_PREFS_TOKEN_EXPIRES_AT, gdAuthResponse.getExpiresAtTimestamp());
                                            if (editor.commit()) {
                                                onGoogleAuthCompleteListener.onSuccess(gdAuthResponse);
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onError(GDException exception) {
                                    onGoogleAuthCompleteListener.onError(exception);
                                }
                            });
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            onGoogleAuthCompleteListener.onError(new GDException(e.getMessage()));
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        onGoogleAuthCompleteListener.onError(new GDException(e.getMessage()));
                    }

                }
            }
        });
    }

    public GDAuthResponse getAuthData(Context context) throws GDException {
        SharedPreferences preferences = context.getSharedPreferences(GDConstants.GD_PREFS_NAME, MODE_PRIVATE);

        if (preferences.contains(GDConstants.GD_PREFS_ACCESS_TOKEN) == false
                || preferences.contains(GDConstants.GD_PREFS_REFRESH_TOKEN) == false
                || preferences.contains(GDConstants.GD_PREFS_TOKEN_EXPIRES_AT) == false) {
            throw new GDException("You did not use GDAuthResponse.getInstance().setAuthData() [to persist auth data.]");
        }

        return new GDAuthResponse(
                preferences.getString(GDConstants.GD_PREFS_ACCESS_TOKEN, null),
                preferences.getString(GDConstants.GD_PREFS_REFRESH_TOKEN, null),
                preferences.getLong(GDConstants.GD_PREFS_TOKEN_EXPIRES_AT, 0)
        );
    }

    public Boolean setAuthData(Activity activity, GDAuthResponse authData) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(GDConstants.GD_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(GDConstants.GD_PREFS_ACCESS_TOKEN, authData.getAccessToken());
        editor.putString(GDConstants.GD_PREFS_REFRESH_TOKEN, authData.getRefreshToken());
        editor.putLong(GDConstants.GD_PREFS_TOKEN_EXPIRES_AT, authData.getExpiresAtTimestamp());
        return editor.commit();
    }

    public void clearCachedAuthData(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(GDConstants.GD_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(GDConstants.GD_PREFS_ACCESS_TOKEN);
        editor.remove(GDConstants.GD_PREFS_REFRESH_TOKEN);
        editor.remove(GDConstants.GD_PREFS_TOKEN_EXPIRES_AT);
        editor.commit();
    }

    private void enableHTML5AppCache(WebView mWebView) {

        mWebView.getSettings().setDomStorageEnabled(true);

        // Set cache size to 8 mb by default. should be more than enough
        mWebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);

        // This next one is crazy. It's the DEFAULT location for your app's cache
        // But it didn't work for me without this line
        mWebView.getSettings().setAppCachePath("/data/data/" + mWebView.getContext().getPackageName() + "/cache");
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
    }

    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            WebView newWebView = new WebView(view.getContext());

            newWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            newWebView.getSettings().setLoadWithOverviewMode(true);
            newWebView.getSettings().setAllowContentAccess(true);
            newWebView.getSettings().setDatabaseEnabled(true);
            newWebView.getSettings().setLoadsImagesAutomatically(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                newWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            enableHTML5AppCache(newWebView);
            newWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onCloseWindow(WebView window) {
                }
            });
            view.addView(newWebView);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
        }
    }
}
