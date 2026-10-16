package com.example.sunrisebox;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

// Profile screen: picture, name and contact details.
// The profile is saved to a text file with FileHelper (file handling demo).
// The picture can be changed with the Camera or Gallery (implicit Intents),
// and all profile + booking data can be downloaded as a .txt file.
public class ProfileActivity extends AppCompatActivity {

    TextView tvProfileName, tvProfileStats, tabExplore, tabBookings;
    EditText etProfileName, etProfilePhone, etProfileEmail;
    Button btnSaveProfile, btnChangePhoto, btnDownloadData;
    ImageView imgProfile;
    DatabaseHelper db;

    // Launchers that start another app and receive its result back
    ActivityResultLauncher<Intent> cameraLauncher, galleryLauncher, exportLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileStats = findViewById(R.id.tvProfileStats);
        etProfileName = findViewById(R.id.etProfileName);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileEmail = findViewById(R.id.etProfileEmail);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnDownloadData = findViewById(R.id.btnDownloadData);
        imgProfile = findViewById(R.id.imgProfile);
        tabExplore = findViewById(R.id.tabExplore);
        tabBookings = findViewById(R.id.tabBookings);
        db = new DatabaseHelper(this);

        // Read the saved profile from the file (null if nothing saved yet)
        String[] profile = FileHelper.readProfile(this);
        if (profile != null) {
            etProfileName.setText(profile[0]);
            etProfilePhone.setText(profile[1]);
            etProfileEmail.setText(profile[2]);
            tvProfileName.setText(profile[0]);
        } else {
            tvProfileName.setText("Guest Player");
        }

        tvProfileStats.setText("Upcoming bookings: " + db.countUpcomingBookings());

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        registerResultLaunchers();

        btnChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoChoiceDialog();
            }
        });

        btnDownloadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExport();
            }
        });

        // Bottom bar: go back to Explore, or switch to My Bookings
        tabExplore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tabBookings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, MyBookingsActivity.class));
                finish();
            }
        });
    }

    private void saveProfile() {
        String name = etProfileName.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();
        String email = etProfileEmail.getText().toString().trim();

        if (name.isEmpty()) {
            etProfileName.setError("Name is required");
            etProfileName.requestFocus();
            return;
        }
        if (phone.length() != 10) {
            etProfilePhone.setError("Enter a 10-digit phone number");
            etProfilePhone.requestFocus();
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etProfileEmail.setError("Enter a valid email");
            etProfileEmail.requestFocus();
            return;
        }

        if (FileHelper.saveProfile(this, name, phone, email)) {
            tvProfileName.setText(name);
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not save profile", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- Result launchers (must be registered in onCreate) ----------

    private void registerResultLaunchers() {
        // Camera: the camera app returns a small thumbnail Bitmap in the "data" extra
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            return; // cancelled - keep the current picture
                        }
                        Bundle extras = result.getData().getExtras();
                        Object photo = null;
                        if (extras != null) {
                            photo = extras.get("data");
                        }
                        if (photo instanceof Bitmap) {
                            imgProfile.setImageBitmap((Bitmap) photo);
                        } else {
                            Toast.makeText(ProfileActivity.this, "Could not get the photo", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Gallery: returns the Uri of the image the user picked
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null
                                || result.getData().getData() == null) {
                            return; // cancelled - keep the current picture
                        }
                        showGalleryImage(result.getData().getData());
                    }
                });

        // Export: returns the Uri of the new .txt file the user created
        exportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null
                                || result.getData().getData() == null) {
                            return; // cancelled - nothing to save
                        }
                        saveExport(result.getData().getData());
                    }
                });
    }

    // ---------- Feature: profile photo from Camera or Gallery ----------

    private void showPhotoChoiceDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle(R.string.btn_change_photo);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    openCamera();
                } else {
                    openGallery();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        try {
            galleryLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
        }
    }

    // Load the picked image at a smaller size, so a very large photo cannot crash the app
    private void showGalleryImage(Uri imageUri) {
        try {
            // First pass: only read the image width and height
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(in, null, options);
            if (in != null) {
                in.close();
            }

            // Shrink by 2, 4, 8... until the image is about 512 px (enough for the round picture)
            int sampleSize = 1;
            while (options.outWidth / (sampleSize * 2) >= 512 && options.outHeight / (sampleSize * 2) >= 512) {
                sampleSize = sampleSize * 2;
            }

            // Second pass: read the actual (smaller) image
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            in = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
            if (in != null) {
                in.close();
            }

            if (bitmap != null) {
                imgProfile.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "Could not open the image", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Could not open the image", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- Feature: download all data as a .txt file ----------

    // Opens the system "save" screen so the user chooses where the .txt file goes
    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "SunriseBox_MyData.txt");
        try {
            exportLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to save the file", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExport(Uri fileUri) {
        String text;
        try {
            text = buildExportText();
        } catch (Exception e) {
            Toast.makeText(this, "Could not read your data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (FileHelper.saveTextToUri(this, fileUri, text)) {
            Toast.makeText(this, "Data downloaded", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not save the file", Toast.LENGTH_SHORT).show();
        }
    }

    // Profile from profile.txt (FileHelper) + every booking from SQLite (DatabaseHelper)
    private String buildExportText() {
        StringBuilder sb = new StringBuilder();
        sb.append("SUNRISE BOX — MY DATA\n\n");

        sb.append("PROFILE\n");
        String[] profile = FileHelper.readProfile(this);
        if (profile == null) {
            sb.append("No profile saved yet.\n");
        } else {
            sb.append("Name: ").append(profile[0]).append("\n");
            sb.append("Phone: ").append(profile[1]).append("\n");
            sb.append("Email: ").append(profile[2].isEmpty() ? "Not provided" : profile[2]).append("\n");
        }

        sb.append("\nMY BOOKINGS\n");
        int count = 0;
        count = appendBookings(sb, db.getUpcomingBookings(), "Upcoming", count);
        count = appendBookings(sb, db.getPastBookings(), "Past", count);
        if (count == 0) {
            sb.append("No bookings found.\n");
        }
        return sb.toString();
    }

    // Adds each booking in the cursor to the text. Returns the new total number of bookings.
    private int appendBookings(StringBuilder sb, Cursor cursor, String status, int count) {
        while (cursor.moveToNext()) {
            count++;
            sb.append("\nBooking ").append(count).append("\n");
            sb.append("Booking ID: ").append(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID))).append("\n");
            sb.append("Venue: ").append(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VENUE_NAME))).append("\n");
            sb.append("Captain / team: ").append(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME))).append("\n");
            sb.append("Phone: ").append(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE))).append("\n");
            sb.append("Date: ").append(BookingActivity.displayDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE)))).append("\n");
            sb.append("Time: ").append(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIME))).append("\n");
            sb.append("Duration: ").append(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION))).append("\n");
            sb.append("Format: ").append(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FORMAT))).append("\n");
            sb.append("Extras: ").append(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXTRAS))).append("\n");
            sb.append("Total price: ₹").append(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRICE))).append("\n");
            sb.append("Status: ").append(status).append("\n");
        }
        cursor.close();
        return count;
    }
}
