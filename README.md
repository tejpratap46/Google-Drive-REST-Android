# Google-Drive-REST-Android
Google Drive REST API for android

After Google Depricated Google Drive Android Library, this is a simple wrapper of there REST API using OKHTTP.

## Auth
Before you can do any request, you need to authenticate your app with google drive, Best way is to use [Google Sign In For Android](https://developers.google.com/identity/sign-in/android/) with [Offline Access](https://developers.google.com/identity/sign-in/android/offline-access)

### Auth Steps
1. Goto [Google Console Credentials](https://console.developers.google.com/apis/credentials). Make sure Your Google Drive API is turned on for your Google Project.
2. Create One Auth for Android App (For Google Sign In)
3. Create One Auth for Web browser App (For Google Drive Rest API)

