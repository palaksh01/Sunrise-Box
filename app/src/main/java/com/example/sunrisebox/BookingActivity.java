package com.example.sunrisebox;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    TextView tvBookVenueName, tvBookVenueLocation, tvBookVenueRate;
    EditText etName, etPhone;
    Spinner spDuration;
    RadioGroup rgFormat;
    RadioButton rbSix, rbEight, rbNets;
    CheckBox cbKit, cbLights, cbUmpire;
    DatePicker dpDate;
    TimePicker tpTime;
    Button btnSubmit;

    DatabaseHelper db;

    String[] durations = {"1 hour", "2 hours", "3 hours"};

    // -1 means a new booking. Any other value means we are updating that booking.
    int bookingId = -1;

    // The venue being booked
    int venueId = -1;
    String venueName = "";
    int ratePerHour = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        tvBookVenueName = findViewById(R.id.tvBookVenueName);
        tvBookVenueLocation = findViewById(R.id.tvBookVenueLocation);
        tvBookVenueRate = findViewById(R.id.tvBookVenueRate);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        spDuration = findViewById(R.id.spDuration);
        rgFormat = findViewById(R.id.rgFormat);
        rbSix = findViewById(R.id.rbSix);
        rbEight = findViewById(R.id.rbEight);
        rbNets = findViewById(R.id.rbNets);
        cbKit = findViewById(R.id.cbKit);
        cbLights = findViewById(R.id.cbLights);
        cbUmpire = findViewById(R.id.cbUmpire);
        dpDate = findViewById(R.id.dpDate);
        tpTime = findViewById(R.id.tpTime);
        btnSubmit = findViewById(R.id.btnSubmit);

        db = new DatabaseHelper(this);

        // Spinner with an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, durations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDuration.setAdapter(adapter);

        // TimePicker in 12-hour (AM/PM) mode
        tpTime.setIs24HourView(false);

        bookingId = getIntent().getIntExtra("booking_id", -1);
        if (bookingId != -1) {
            // Opened from My Bookings: load the booking (this also loads its venue)
            setTitle("Update Booking");
            btnSubmit.setText(R.string.btn_update);
            loadBooking(bookingId);
        } else {
            // Opened from an Explore card: load the venue and fill name/phone from the profile file
            venueId = getIntent().getIntExtra("venue_id", -1);
            loadVenue(venueId);
            String[] profile = FileHelper.readProfile(this);
            if (profile != null) {
                etName.setText(profile[0]);
                etPhone.setText(profile[1]);
            }
        }

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateForm()) {
                    showConfirmDialog();
                }
            }
        });
    }

    // Show the venue name, location and rate at the top of the form
    private void loadVenue(int id) {
        Cursor cursor = db.getVenue(id);
        if (cursor.moveToFirst()) {
            venueName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_NAME));
            ratePerHour = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.V_RATE));
            String area = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_AREA));
            String city = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_CITY));
            tvBookVenueName.setText(venueName);
            tvBookVenueLocation.setText("📍 " + area + ", " + city);
            tvBookVenueRate.setText("₹" + ratePerHour + " / hour");
            cursor.close();
        } else {
            cursor.close();
            Toast.makeText(this, "Venue not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean validateForm() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }
        if (phone.length() != 10) {
            etPhone.setError("Enter a 10-digit phone number");
            etPhone.requestFocus();
            return false;
        }
        if (rgFormat.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please choose a match format", Toast.LENGTH_SHORT).show();
            return false;
        }

        // The booking date must not be in the past
        Calendar selected = Calendar.getInstance();
        selected.set(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth(), 0, 0, 0);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        if (selected.before(today)) {
            Toast.makeText(this, "Booking date cannot be in the past", Toast.LENGTH_SHORT).show();
            return false;
        }

        // For a booking today, the start time must still be ahead of the current time
        Calendar now = Calendar.getInstance();
        if (selected.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && selected.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            int chosenMinutes = tpTime.getHour() * 60 + tpTime.getMinute();
            int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            if (chosenMinutes <= nowMinutes) {
                Toast.makeText(this, "That start time has already passed today", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // Venues are open 6:00 AM to 11:00 PM, so the slot must fit inside those hours
        int startHour = tpTime.getHour();
        int hours = getSelectedHours();
        if (startHour < 6 || startHour + hours > 23
                || (startHour + hours == 23 && tpTime.getMinute() > 0)) {
            Toast.makeText(this, "Slot must be between 6:00 AM and 11:00 PM", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    // Spinner position 0 = 1 hour, 1 = 2 hours, 2 = 3 hours
    private int getSelectedHours() {
        return spDuration.getSelectedItemPosition() + 1;
    }

    private String getSelectedFormat() {
        int checkedId = rgFormat.getCheckedRadioButtonId();
        RadioButton selected = findViewById(checkedId);
        return selected.getText().toString();
    }

    private String getSelectedExtras() {
        String extras = "";
        if (cbKit.isChecked()) {
            extras = extras + cbKit.getText().toString() + ", ";
        }
        if (cbLights.isChecked()) {
            extras = extras + cbLights.getText().toString() + ", ";
        }
        if (cbUmpire.isChecked()) {
            extras = extras + cbUmpire.getText().toString() + ", ";
        }
        if (extras.isEmpty()) {
            return "None";
        }
        return extras.substring(0, extras.length() - 2); // remove last ", "
    }

    // Date saved in the database as yyyy-MM-dd (so it can be sorted and compared)
    private String getSelectedDate() {
        int day = dpDate.getDayOfMonth();
        int month = dpDate.getMonth() + 1; // months start at 0
        int year = dpDate.getYear();
        return String.format(Locale.US, "%d-%02d-%02d", year, month, day);
    }

    private String getSelectedTime() {
        int hour = tpTime.getHour();
        int minute = tpTime.getMinute();
        String amPm = (hour >= 12) ? "PM" : "AM";
        int hour12 = hour % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm);
    }

    // Turns yyyy-MM-dd into dd/MM/yyyy for showing to the user
    public static String displayDate(String dbDate) {
        String[] parts = dbDate.split("-");
        return parts[2] + "/" + parts[1] + "/" + parts[0];
    }

    private void showConfirmDialog() {
        final String name = etName.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();
        final String date = getSelectedDate();
        final String time = getSelectedTime();
        final String duration = spDuration.getSelectedItem().toString();
        final String format = getSelectedFormat();
        final String extras = getSelectedExtras();
        final int totalPrice = ratePerHour * getSelectedHours();

        String summary = "Venue: " + venueName
                + "\nName: " + name
                + "\nPhone: " + phone
                + "\nDate: " + displayDate(date)
                + "\nTime: " + time
                + "\nDuration: " + duration
                + "\nFormat: " + format
                + "\nExtras: " + extras
                + "\n\nTotal: ₹" + totalPrice + " (pay at venue)";

        AlertDialog.Builder builder = new AlertDialog.Builder(BookingActivity.this);
        builder.setTitle(bookingId == -1 ? "Confirm Booking" : "Confirm Update");
        builder.setMessage(summary);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (bookingId == -1) {
                    long newId = db.insertBooking(venueId, venueName, name, phone, date, time,
                            duration, format, extras, totalPrice);
                    if (newId != -1) {
                        Toast.makeText(BookingActivity.this, "Booking request saved", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(BookingActivity.this, "Error: booking was not saved", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    int rows = db.updateBooking(bookingId, name, phone, date, time, duration,
                            format, extras, totalPrice);
                    if (rows > 0) {
                        Toast.makeText(BookingActivity.this, "Booking updated", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(BookingActivity.this, "Error: booking was not updated", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.setNegativeButton("Edit", null);
        builder.show();
    }

    // Fill the form with an existing booking (used when updating)
    private void loadBooking(int id) {
        Cursor cursor = db.getBooking(id);
        if (!cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        venueId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VENUE_ID));
        loadVenue(venueId);

        etName.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)));
        etPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE)));

        String duration = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION));
        for (int i = 0; i < durations.length; i++) {
            if (durations[i].equals(duration)) {
                spDuration.setSelection(i);
            }
        }

        String format = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FORMAT));
        if (format.equals(rbSix.getText().toString())) {
            rbSix.setChecked(true);
        } else if (format.equals(rbEight.getText().toString())) {
            rbEight.setChecked(true);
        } else if (format.equals(rbNets.getText().toString())) {
            rbNets.setChecked(true);
        }

        String extras = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXTRAS));
        cbKit.setChecked(extras.contains(cbKit.getText().toString()));
        cbLights.setChecked(extras.contains(cbLights.getText().toString()));
        cbUmpire.setChecked(extras.contains(cbUmpire.getText().toString()));

        // Date is stored as yyyy-MM-dd
        String[] dateParts = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE)).split("-");
        dpDate.updateDate(Integer.parseInt(dateParts[0]),
                Integer.parseInt(dateParts[1]) - 1,
                Integer.parseInt(dateParts[2]));

        // Time is stored as hh:mm AM/PM
        String time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIME));
        int hour = Integer.parseInt(time.substring(0, 2));
        int minute = Integer.parseInt(time.substring(3, 5));
        boolean pm = time.endsWith("PM");
        if (hour == 12) {
            hour = 0;
        }
        if (pm) {
            hour = hour + 12;
        }
        tpTime.setHour(hour);
        tpTime.setMinute(minute);

        cursor.close();
    }
}
