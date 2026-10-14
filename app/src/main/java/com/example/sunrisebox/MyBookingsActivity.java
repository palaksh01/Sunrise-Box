package com.example.sunrisebox;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MyBookingsActivity extends AppCompatActivity {

    ListView lvBookings;
    TextView tvEmpty, tvTapHint, tabExplore, tabProfile;
    Button btnUpcoming, btnPast;
    DatabaseHelper db;

    // true = showing upcoming bookings, false = showing past bookings
    boolean showingUpcoming = true;

    // bookingIds.get(i) is the database id of the row shown at position i
    ArrayList<Integer> bookingIds = new ArrayList<>();
    ArrayList<String> bookingTexts = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        lvBookings = findViewById(R.id.lvBookings);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTapHint = findViewById(R.id.tvTapHint);
        btnUpcoming = findViewById(R.id.btnUpcoming);
        btnPast = findViewById(R.id.btnPast);
        tabExplore = findViewById(R.id.tabExplore);
        tabProfile = findViewById(R.id.tabProfile);
        db = new DatabaseHelper(this);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bookingTexts);
        lvBookings.setAdapter(adapter);

        btnUpcoming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingUpcoming = true;
                loadBookings();
            }
        });

        btnPast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingUpcoming = false;
                loadBookings();
            }
        });

        lvBookings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (showingUpcoming) {
                    showUpcomingDialog(bookingIds.get(position), bookingTexts.get(position));
                } else {
                    showPastDialog(bookingIds.get(position), bookingTexts.get(position));
                }
            }
        });

        // Bottom bar: go back to Explore, or switch to Profile
        tabExplore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tabProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MyBookingsActivity.this, ProfileActivity.class));
                finish();
            }
        });
    }

    // Reload every time the screen is shown, so updates appear straight away
    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    private void loadBookings() {
        bookingIds.clear();
        bookingTexts.clear();

        Cursor cursor;
        if (showingUpcoming) {
            cursor = db.getUpcomingBookings();
        } else {
            cursor = db.getPastBookings();
        }
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
            String text = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VENUE_NAME))
                    + "\n📅 " + BookingActivity.displayDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE)))
                    + "  ⏰ " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIME))
                    + " (" + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION)) + ")"
                    + "\n👤 " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME))
                    + "  •  " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FORMAT))
                    + "\nExtras: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXTRAS))
                    + "\nTotal: ₹" + cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRICE));
            bookingIds.add(id);
            bookingTexts.add(text);
        }
        cursor.close();
        adapter.notifyDataSetChanged();

        updateToggleColours();

        // Empty state
        if (bookingTexts.isEmpty()) {
            tvEmpty.setText(showingUpcoming ? R.string.no_upcoming : R.string.no_past);
            tvEmpty.setVisibility(View.VISIBLE);
            tvTapHint.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            tvTapHint.setText(showingUpcoming ? R.string.tap_hint_upcoming : R.string.tap_hint_past);
            tvTapHint.setVisibility(View.VISIBLE);
        }
    }

    // Selected toggle = gold, other toggle = light purple
    private void updateToggleColours() {
        int gold = getResources().getColor(R.color.gold_accent, getTheme());
        int light = getResources().getColor(R.color.purple_light, getTheme());
        int black = getResources().getColor(R.color.black, getTheme());
        btnUpcoming.setBackgroundTintList(ColorStateList.valueOf(showingUpcoming ? gold : light));
        btnPast.setBackgroundTintList(ColorStateList.valueOf(showingUpcoming ? light : gold));
        btnUpcoming.setTextColor(black);
        btnPast.setTextColor(black);
    }

    private void showUpcomingDialog(final int bookingId, String details) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyBookingsActivity.this);
        builder.setTitle("Booking #" + bookingId);
        builder.setMessage(details);
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MyBookingsActivity.this, BookingActivity.class);
                intent.putExtra("booking_id", bookingId);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel Booking", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                confirmDelete(bookingId, "Cancel booking?", "Booking cancelled");
            }
        });
        builder.setNeutralButton("Close", null);
        builder.show();
    }

    // Past bookings cannot be updated, only viewed or removed from history
    private void showPastDialog(final int bookingId, String details) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyBookingsActivity.this);
        builder.setTitle("Past booking #" + bookingId);
        builder.setMessage(details);
        builder.setPositiveButton("Close", null);
        builder.setNegativeButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                confirmDelete(bookingId, "Remove from history?", "Booking removed");
            }
        });
        builder.show();
    }

    private void confirmDelete(final int bookingId, String title, final String doneMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyBookingsActivity.this);
        builder.setTitle(title);
        builder.setMessage("Booking #" + bookingId + " will be deleted.");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (db.deleteBooking(bookingId) > 0) {
                    Toast.makeText(MyBookingsActivity.this, doneMessage, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MyBookingsActivity.this, "Error: booking was not deleted", Toast.LENGTH_SHORT).show();
                }
                loadBookings();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }
}
