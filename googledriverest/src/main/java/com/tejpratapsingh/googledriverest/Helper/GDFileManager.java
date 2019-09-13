package com.tejpratapsingh.googledriverest.Helper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Date;

public class GDFileManager {
    private static final GDFileManager ourInstance = new GDFileManager();

    public static GDFileManager getInstance() {
        return ourInstance;
    }

    private GDFileManager() {
    }

    private final String tempDirectoryName = "temp";
    private final String tempDuplicateFileNameSuffix = "dup";



    /**
     * Write file from InputStream, file will be overwritten if exist
     *
     * @param context          context
     * @param inputStream      input stream reader of data (file)
     * @param fileName         name of file which has to saved
     * @param overWriteIfExist if true, file will be overwritten if exist
     * @return saved file
     * @throws IOException
     */
    public File saveFileToPrivateStorageFromInputStream(Context context, InputStream inputStream, String fileName, boolean overWriteIfExist) throws GDException, IOException {
        // Start referencing a new file
        File fileToSave = new File(context.getExternalFilesDir(null), fileName);
        if (overWriteIfExist == false) {
            // Check if file already exist or not, return if exist
            boolean isFileAlreadyExist = hasExternalStoragePrivateFile(context, fileName);
            if (isFileAlreadyExist) {
                throw new GDException("File Already Exists, make it overWritable to replace.");
            }
        }
        FileOutputStream fileOutput = new FileOutputStream(fileToSave);

        byte[] buffer = new byte[1024];
        int bufferLength = 0;

        while ((bufferLength = inputStream.read(buffer)) > 0) {
            fileOutput.write(buffer, 0, bufferLength);
        }
        fileOutput.flush();
        fileOutput.close();
        inputStream.close();
        return fileToSave;
    }

    /**
     * Check if file is available in private storage
     *
     * @param context  context
     * @param fileName Name of file in external private storage to be checked
     * @return boolean is file exist or not
     */
    public boolean hasExternalStoragePrivateFile(Context context, String fileName) {
        // Get path for the file on external storage.
        // If external storage is not currently mounted this will fail.
        File file = new File(context.getExternalFilesDir(null), fileName);
        if (file != null) {
            return file.exists();
        }
        return false;
    }

    private boolean makeDirectoryInPrivateStorage(Context context, String directoryName) {
        File randomDirectory = new File(context.getExternalFilesDir(null) + File.separator + directoryName);
        if (!randomDirectory.exists()) {
            System.out.println("creating directory: " + directoryName);
            randomDirectory.mkdir();
        }
        return true;
    }

    /**
     * Save a string in file
     *
     * @param fileToWrite
     * @param dataToWrite
     * @return
     * @throws GDException
     */
    public File saveStringToFile(File fileToWrite, String dataToWrite) throws GDException {
        try {
            FileWriter fw = new FileWriter(fileToWrite);
            fw.write(dataToWrite);
            fw.close();
            return fileToWrite;
        } catch (IOException e) {
            e.printStackTrace();
            throw new GDException(e.getMessage());
        }
    }

    /**
     * Get Temp folder from private storage (create one if not exist)
     *
     * @param context current context
     * @return temp folder location (path)
     */
    public String getTempFolder(Context context) {
        File tempDirectory = new File(context.getExternalFilesDir(null) + File.separator + tempDirectoryName);
        if (!tempDirectory.exists()) {
            System.out.println("creating directory: temp");
            tempDirectory.mkdir();
        }

        return tempDirectory.getAbsolutePath();
    }

    /**
     * Create a temporary file in temp folder
     *
     * @param context       current context
     * @param withExtension specify file extension
     * @param withDuplicate create a duplicate of temp file (used in case of image processing), DUPLICATE FILES ARE JUST A EMPTY FILE PATH, ALL FILE MANAGEMENT HAS TO BE DONE BY YOU.
     * @return created temp file
     */
    public File createTempFile(Context context, String withExtension, boolean withDuplicate) {
        // Actual temp file
        String tempFileName = Long.toString(new Date().getTime());
        if (withExtension != null && withExtension.isEmpty() == false) {
            tempFileName = tempFileName + "." + withExtension;
        }
        File tempFile = new File(getTempFolder(context), tempFileName);
        if (withDuplicate) {
            // Duplicate of temp file
            File tempDuplicateFile = new File(getTempFolder(context), tempFileName + tempDuplicateFileNameSuffix);
        }

        return tempFile;
    }

    /**
     * Create a temporary file in temp folder with user given name, file will be overwritten if name is collapsed
     *
     * @param context       current context
     * @param withDuplicate create a duplicate of temp file (used in case of image processing), DUPLICATE FILES ARE JUST A EMPTY FILE PATH, ALL FILE MANAGEMENT HAS TO BE DONE BY YOU.
     * @return created temp file
     */
    public File createTempFileWithName(Context context, String tempFileName, boolean withDuplicate) {
        // Actual temp file
        File tempFile = new File(getTempFolder(context), tempFileName);
        if (withDuplicate) {
            // Duplicate of temp file
            File tempDuplicateFile = new File(getTempFolder(context), tempFileName + tempDuplicateFileNameSuffix);
        }

        return tempFile;
    }

    /**
     * Get Mime type from a file
     *
     * @param file file whose mime type has to find
     * @return mime type of string
     */
    public String getMimeType(Context context, File file) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (type == null) {
            try {
                type = file.toURI().toURL().openConnection().getContentType();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (type == null) {
            type = context.getContentResolver().getType(Uri.fromFile(file));
        }
        if (type == null) {
            type = URLConnection.guessContentTypeFromName(file.getName());
        }
        if (type == null) {
            // If nothing worked, just set mime type to empty string
            type = "";
        }
        return type;
    }

    public File getFileFromURI(Context context, Uri contentUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
            File fileToSave = createTempFile(context, getFileExtension(new File(getFileName(context, contentUri))), false);
            FileOutputStream fileOutput = new FileOutputStream(fileToSave);

            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
            }
            fileOutput.flush();
            fileOutput.close();
            inputStream.close();
            return fileToSave;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gte file's extension without "."
     *
     * @param sourceFile whole extension has to be found
     * @return file extension without "."
     */
    public String getFileExtension(File sourceFile) {
        if (sourceFile == null || sourceFile.getName().lastIndexOf(".") <= 0) {
            return null;
        }
        String[] fileNameParts = sourceFile.getName().split("\\.");
        return fileNameParts[fileNameParts.length - 1];
    }

    public String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
