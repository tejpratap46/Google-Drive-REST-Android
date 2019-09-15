# Google-Drive-REST-Android
Google Drive REST API for android
![Cover](https://raw.githubusercontent.com/tejpratap46/Google-Drive-REST-Android/master/cover_big.png)
This is a simple wrapper of around Google Drive REST API using OKHTTP.

## Auth
Before you can do any request, you need to authenticate your app with google drive, Best way is to use [Google Sign In For Android](https://developers.google.com/identity/sign-in/android/) with [Offline Access](https://developers.google.com/identity/sign-in/android/offline-access)

### Auth Steps
1. Goto [Google Console Credentials](https://console.developers.google.com/apis/credentials). Make sure Your Google Drive API is turned on for your Google Project.
2. Create first Auth for Android App (For Google Sign In)
3. Create another Auth for Web browser App (For Google Drive Rest API)

In Your Android App code, Auth using offline access, code:
```java
String serverClientId = "CLIENT_ID_OF_WEB_BROWSER_API";
GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
        .requestServerAuthCode(serverClientId)
        .requestEmail()
        .build();
        
GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_GOOGLE_SIGN_IN);
```
4. In your ```onActivityResult(Intent data)```
```java
Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
try {
    GoogleSignInAccount account = task.getResult(ApiException.class);
    String authCode = account.getServerAuthCode();
    
    ArrayList<GDAuthConfig.SCOPES> scopes = new ArrayList<>();
    scopes.add(GDAuthConfig.SCOPES.EMAIL);
    scopes.add(GDAuthConfig.SCOPES.DRIVE);
    scopes.add(GDAuthConfig.SCOPES.APP_FOLDER);
    
    final GDAuthConfig gdAuthConfig = new GDAuthConfig(REDIRECT_URI, CLIENT_ID, CLIENT_SECRET, scopes);
    
    // Use auth code to get AccessToken
    GDApiManager.getInstance().getAuthFromCodeAsync(authCode, gdAuthConfig, new GDAuthResponse.OnAuthResponseListener() {
        @Override
        public void onSuccess(final GDAuthResponse gdAuthResponse) {
            boolean isAuthDataSaved = GDAuthManager.getInstance().setAuthData(MainActivity.this, gdAuthResponse);
        }

        @Override
        public void onError(GDException exception) {

        }
    });
} catch (ApiException e) {
    Log.w(TAG, "Sign-in failed", e);
    updateUI(null);
}
```

## Available API
1. ```GDAuthResponse gdAuthResponse = getAuthFromCode(authCode, gdAuthConfig);```
2. ```GDAuthResponse gdAuthResponse = getAuthFromRefreshToken(context, authCode, previousAuthResponse);```
3. ```GDUserInfo gdUserInfo = getUserInfo(context, gdAuthResponse, gdAuthConfig);```
4. ```GDUploadFileResponse fileUploadResponse = uploadFile(context, gdAuthResponse, gdAuthConfig, fileToUpload, fileMime, uploadToAppFolder);```
5. ```File downloadedFile = downloadFile(context, gdAuthResponse, gdAuthConfig, gdResourceId, fileName)```
6. ```boolnea isFileDeleted = deleteFile(context, gdAuthResponse, gdAuthConfig, gdResourceId)```

### All API's are abailable as Async Requests.

Fo example, look into [MainActivity](https://github.com/tejpratap46/Google-Drive-REST-Android/blob/master/app/src/main/java/com/tejpratapsingh/googledriverestandroid/MainActivity.java) of app module
