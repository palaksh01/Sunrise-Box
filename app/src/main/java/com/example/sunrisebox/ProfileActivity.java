package com.example.sunrisebox;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Profile screen: picture, name and contact details.
// The profile is saved to a text file with FileHelper (file handling demo).
public class ProfileActivity extends AppCompatActivity {

    TextView tvProfileName, tvProfileStats, tabExplore, tabBookings;
    EditText etProfileName, etProfilePhone, etProfileEmail;
    Button btnSaveProfile;
    DatabaseHelper db;

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
}
