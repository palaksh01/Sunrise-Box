package com.example.sunrisebox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// Explore screen: shows every venue from the database as a card.
public class MainActivity extends AppCompatActivity {

    // static = keeps its value while the app is running, so the welcome dialog shows only once
    private static boolean welcomeShown = false;

    LinearLayout venueContainer;
    TextView tabBookings, tabProfile;
    DatabaseHelper db;

    // BroadcastReceiver: Android sends ACTION_AIRPLANE_MODE_CHANGED when Airplane Mode is switched.
    // The "state" extra is true when it was turned ON and false when it was turned OFF.
    BroadcastReceiver airplaneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isOn = intent.getBooleanExtra("state", false);
            if (isOn) {
                Toast.makeText(context, "Airplane Mode ON", Toast.LENGTH_SHORT).show();
                // Short vibration through the view (no VIBRATE permission needed)
                venueContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } else {
                Toast.makeText(context, "Airplane Mode OFF", Toast.LENGTH_SHORT).show();
            }
        }
    };
    boolean receiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0); // blend the action bar into the purple header
        }

        venueContainer = findViewById(R.id.venueContainer);
        tabBookings = findViewById(R.id.tabBookings);
        tabProfile = findViewById(R.id.tabProfile);
        db = new DatabaseHelper(this);

        loadVenues();

        if (!welcomeShown) {
            welcomeShown = true;
            showWelcomeDialog();
        }

        // Bottom bar (Explore is this screen, so it needs no listener)
        tabBookings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MyBookingsActivity.class));
            }
        });

        tabProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        });
    }

    // Listen for Airplane Mode only while this screen is visible
    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            ContextCompat.registerReceiver(this, airplaneReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
    }

    // Stop listening when the screen is hidden (avoids leaks and duplicate registrations)
    @Override
    protected void onStop() {
        super.onStop();
        if (receiverRegistered) {
            unregisterReceiver(airplaneReceiver);
            receiverRegistered = false;
        }
    }

    // Read the venues table and add one card (item_venue.xml) per venue
    private void loadVenues() {
        LayoutInflater inflater = LayoutInflater.from(this);
        Cursor cursor = db.getAllVenues();
        while (cursor.moveToNext()) {
            final int venueId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.V_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_NAME));
            String area = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_AREA));
            String city = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_CITY));
            int rate = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.V_RATE));
            String imageName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_IMAGE));

            View card = inflater.inflate(R.layout.item_venue, venueContainer, false);
            ImageView imgVenue = card.findViewById(R.id.imgVenue);
            TextView tvName = card.findViewById(R.id.tvVenueName);
            TextView tvLocation = card.findViewById(R.id.tvVenueLocation);
            TextView tvRate = card.findViewById(R.id.tvVenueRate);
            Button btnBook = card.findViewById(R.id.btnVenueBook);
            Button btnMap = card.findViewById(R.id.btnVenueMap);

            tvName.setText(name);
            tvLocation.setText("📍 " + area + ", " + city);
            tvRate.setText("₹" + rate + " / hour");

            // Use the venue photo if it has been added to res/drawable, otherwise keep the placeholder
            int imageId = getResources().getIdentifier(imageName, "drawable", getPackageName());
            if (imageId != 0) {
                imgVenue.setImageResource(imageId);
            }

            btnBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, BookingActivity.class);
                    intent.putExtra("venue_id", venueId);
                    startActivity(intent);
                }
            });

            btnMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, LocationActivity.class);
                    intent.putExtra("venue_id", venueId);
                    startActivity(intent);
                }
            });

            venueContainer.addView(card);
        }
        cursor.close();
    }

    private void showWelcomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.welcome_title);
        builder.setMessage(R.string.welcome_message);
        builder.setPositiveButton("Let's Play", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Pick a venue to book a slot", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // Options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_all_venues_map) {
            // No venue_id extra, so LocationActivity shows every venue
            startActivity(new Intent(MainActivity.this, LocationActivity.class));
            return true;
        } else if (id == R.id.menu_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.about_title);
            builder.setMessage(R.string.about_message);
            builder.setPositiveButton("OK", null);
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
