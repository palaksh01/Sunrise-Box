package com.example.sunrisebox;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

// File handling: saves and reads the player's profile in the app's private storage.
// File location on the phone: /data/data/com.example.sunrisebox/files/profile.txt
// Line 1 = name, line 2 = phone, line 3 = email.
// Bookings are NOT stored here - they are stored in SQLite (DatabaseHelper).
// saveTextToUri() writes the "Download All Data" export to a file the user picks (outside the app).
public class FileHelper {

    public static final String FILE_NAME = "profile.txt";

    // Write the profile to the file (replaces the old one). Returns true if it worked.
    public static boolean saveProfile(Context context, String name, String phone, String email) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            String text = name + "\n" + phone + "\n" + email;
            fos.write(text.getBytes());
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Read the profile back. Returns {name, phone, email}, or null if no profile is saved yet.
    public static String[] readProfile(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String[] profile = new String[]{"", "", ""};
            for (int i = 0; i < 3; i++) {
                String line = reader.readLine();
                if (line != null) {
                    profile[i] = line;
                }
            }
            reader.close();
            return profile;
        } catch (IOException e) {
            return null;
        }
    }

    // Write text to a file the user chose with the system "save" screen (ACTION_CREATE_DOCUMENT).
    // The Uri points outside the app, so we open it through the ContentResolver and
    // write with a FileOutputStream. Returns true if it worked.
    public static boolean saveTextToUri(Context context, Uri uri, String text) {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "w");
            if (pfd == null) {
                return false;
            }
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            fos.write(text.getBytes());
            fos.close();
            pfd.close();
            return true;
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }
}
