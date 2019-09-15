package com.tejpratapsingh.googledriverestandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.tejpratapsingh.googledriverest.Helper.GDException;
import com.tejpratapsingh.googledriverest.Helper.GDFileManager;
import com.tejpratapsingh.googledriverest.api.GDApiManager;
import com.tejpratapsingh.googledriverest.auth.GDAuthConfig;
import com.tejpratapsingh.googledriverest.auth.GDAuthManager;
import com.tejpratapsingh.googledriverest.modal.GDAuthResponse;
import com.tejpratapsingh.googledriverest.modal.GDDownloadFileResponse;
import com.tejpratapsingh.googledriverest.modal.GDUploadFileResponse;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private GDAuthConfig gdAuthConfig;

    public static final int REQUEST_CODE_GOOGLE_SIGN_IN = 1;

    private final String CLIENT_ID = "CLIENT_ID";
    private final String CLIENT_SECRET = "CLIENT_SECRET";
    private final String REDIRECT_URI = "https://httpbin1.appspot.com/get";

    ArrayList<GDAuthConfig.SCOPES> scopes = new ArrayList<>();

    ProgressDialog loadingDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scopes.add(GDAuthConfig.SCOPES.EMAIL);
        scopes.add(GDAuthConfig.SCOPES.DRIVE);
        scopes.add(GDAuthConfig.SCOPES.APP_FOLDER);

        showAuthOptions();
    }

    private void showAuthOptions() {
        String[] authOptionList = {"Auth Using Web View", "Auth Using Google Sign In"};
        ArrayAdapter<String> authListAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, authOptionList);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Choose Auth Method")
                .setAdapter(authListAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                startGoogleWithWebView();
                                break;
                            case 1:
                                startGoogleSignInAuth();
                                break;
                        }
                    }
                })
                .create()
                .show();
    }

    private void startGoogleWithWebView() {
        WebView webViewGoogleDrive = (WebView) findViewById(R.id.webViewGoogleDrive);

        GDAuthManager gdAuthManager = GDAuthManager.getInstance();

        try {

            this.gdAuthConfig = new GDAuthConfig("https://httpbin1.appspot.com/get",
                    CLIENT_ID,
                    CLIENT_SECRET,
                    scopes);

            loadingDialog = new ProgressDialog(this); // this = YourActivity
            loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadingDialog.setTitle("Loading");
            loadingDialog.setMessage("Loading. Please wait...");
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCanceledOnTouchOutside(false);

            gdAuthManager.startGoogleDriveAuth(MainActivity.this, webViewGoogleDrive, this.gdAuthConfig, new GDAuthManager.OnGoogleAuthCompleteListener() {
                @Override
                public void onLoadingStart() {
                    // Show loading alert
                    loadingDialog.show();
                }

                @Override
                public void onLoadingFinish() {
                    loadingDialog.dismiss();
                }

                @Override
                public void onSuccess(final GDAuthResponse gdAuthResponse) {
                    // Upload a file
                    showToast("Google Drive Authenticated");

                    testGoogleDriveUpload(gdAuthResponse);
                }

                @Override
                public void onError(GDException exception) {
                    exception.printStackTrace();
                    showToast("Error: " + exception.getMessage());
                }
            });
        } catch (GDException e) {
            e.printStackTrace();
            showToast("Error: " + e.getMessage());
        }
    }

    private void signOutGoogleSignInUser() {
        if (GoogleSignIn.getLastSignedInAccount(getApplicationContext()) != null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(new Scope(GDAuthConfig.SCOPES.APP_FOLDER.getStringValue()))
                    .build();

            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            mGoogleSignInClient.signOut();
        }
    }

    private void startGoogleSignInAuth() {
        signOutGoogleSignInUser();
        final GDAuthManager gdAuthManager = GDAuthManager.getInstance();
        gdAuthManager.clearCachedAuthData(getApplicationContext());
        try {
            final GDAuthConfig gdAuthConfig = new GDAuthConfig(REDIRECT_URI, CLIENT_ID, CLIENT_SECRET, scopes);

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestServerAuthCode(gdAuthConfig.getClientId(), true)
                    .requestEmail()
                    .requestScopes(new Scope(GDAuthConfig.SCOPES.DRIVE.getStringValue()))
                    .requestScopes(new Scope(GDAuthConfig.SCOPES.APP_FOLDER.getStringValue()))
                    .build();

            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_GOOGLE_SIGN_IN);
        } catch (GDException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: ");
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            final GDAuthConfig gdAuthConfig = new GDAuthConfig(REDIRECT_URI, CLIENT_ID, CLIENT_SECRET, scopes);
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Get Auth from access code
            GDApiManager.getInstance().getAuthFromCodeAsync(account.getServerAuthCode(), gdAuthConfig, new GDAuthResponse.OnAuthResponseListener() {
                @Override
                public void onSuccess(final GDAuthResponse gdAuthResponse) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Save auth data to preferences, call: GDAuthManager.getInstance().getAuthData(MainActivity.this) to read it anytime
                            boolean isAuthDataSaved = GDAuthManager.getInstance().setAuthData(MainActivity.this, gdAuthResponse);
                            if (isAuthDataSaved == false) {
                                return;
                            }

                            testGoogleDriveUpload(gdAuthResponse);
                        }
                    });
                }

                @Override
                public void onError(GDException exception) {

                }
            });
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            e.printStackTrace();
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            showToast("Error while connecting to Google");
        } catch (GDException e) {
            e.printStackTrace();
        }
    }

    private void testGoogleDriveUpload(final GDAuthResponse gdAuthResponse) {
        File tempFile = GDFileManager.getInstance().createTempFile(getApplicationContext(), "txt", false);
        try {
            // Create a temp Text file
            GDFileManager.getInstance().saveStringToFile(tempFile, "This is a test file Created by Google_Drive_REST_Android Library");

            // Upload this file to google drive.
            GDApiManager.getInstance().uploadFileAsync(getApplicationContext(), gdAuthResponse, MainActivity.this.gdAuthConfig, tempFile, GDFileManager.getInstance().getMimeType(getApplicationContext(), tempFile), false, new GDUploadFileResponse.OnUploadFileCompleteListener() {
                @Override
                public void onSuccess(GDUploadFileResponse uploadFileResponse) {
                    showToast("File Uploaded Successfully");

                    // Download just uploaded file
                    GDApiManager.getInstance().downloadFileAsync(getApplicationContext(), gdAuthResponse, MainActivity.this.gdAuthConfig, uploadFileResponse.getId(), "downloaded_file.txt", new GDDownloadFileResponse.OnDownloadFileCompleteListener() {
                        @Override
                        public void onSuccess(File downloadedFile) {
                            // Check for a download file in your private files
                            // In here: Internal Storage > Android > data > com.tejpratapsingh.com > files
                            showToast("File Downloaded Successfully");
                            showAlert("Sample Text file is successfully Uploaded and Downloaded from Google Drive. Check you Google Drive.");
                        }

                        @Override
                        public void onError(GDException exception) {
                            showToast("Error: " + exception.getMessage());
                            showAlert("Sample Text file is successfully Uploaded to Google Drive. But failed to Download it.");
                        }
                    });
                }

                @Override
                public void onError(GDException exception) {
                    showToast("Error: " + exception.getMessage());
                    showAlert("Sample Text file is Failed to Upload to Google Drive.");
                }
            });
        } catch (GDException e) {
            e.printStackTrace();
            showToast("Error: " + e.getMessage());
        }
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlert(final String text) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(text)
                        .setPositiveButton(R.string.text_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showAuthOptions();
                            }
                        })
                        .setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNeutralButton(R.string.text_icon8, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String url = "http://www.example.com";
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            }
                        })
                        .create()
                        .show();
            }
        });
    }
}