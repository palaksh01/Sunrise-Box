package com.example.sunrisebox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

// SQLite database helper for Sunrise Box.
// Two tables: venues (synthetic tricity venue data) and bookings.
// All data is stored on this phone in sunrisebox.db (SQLite, not MySQL).
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "sunrisebox.db";
    public static final int DATABASE_VERSION = 2;

    // Venues table
    public static final String TABLE_VENUES = "venues";
    public static final String V_ID = "id";
    public static final String V_NAME = "name";
    public static final String V_AREA = "area";
    public static final String V_CITY = "city";
    public static final String V_RATE = "rate_per_hour";
    public static final String V_LAT = "latitude";
    public static final String V_LNG = "longitude";
    public static final String V_IMAGE = "image_name";

    // Bookings table
    public static final String TABLE_BOOKINGS = "bookings";
    public static final String COL_ID = "id";
    public static final String COL_VENUE_ID = "venue_id";
    public static final String COL_VENUE_NAME = "venue_name";
    public static final String COL_NAME = "name";
    public static final String COL_PHONE = "phone";
    public static final String COL_DATE = "booking_date";   // stored as yyyy-MM-dd
    public static final String COL_TIME = "booking_time";   // stored as hh:mm AM/PM
    public static final String COL_DURATION = "duration";
    public static final String COL_FORMAT = "format";
    public static final String COL_EXTRAS = "extras";
    public static final String COL_PRICE = "total_price";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_VENUES + " ("
                + V_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + V_NAME + " TEXT, "
                + V_AREA + " TEXT, "
                + V_CITY + " TEXT, "
                + V_RATE + " INTEGER, "
                + V_LAT + " REAL, "
                + V_LNG + " REAL, "
                + V_IMAGE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_BOOKINGS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_VENUE_ID + " INTEGER, "
                + COL_VENUE_NAME + " TEXT, "
                + COL_NAME + " TEXT, "
                + COL_PHONE + " TEXT, "
                + COL_DATE + " TEXT, "
                + COL_TIME + " TEXT, "
                + COL_DURATION + " TEXT, "
                + COL_FORMAT + " TEXT, "
                + COL_EXTRAS + " TEXT, "
                + COL_PRICE + " INTEGER)");

        // Synthetic (sample) venue data for Chandigarh, Mohali and Panchkula
        addVenue(db, "Sunrise Box Arena", "Sector 17", "Chandigarh", 1200, 30.7415, 76.7810, "venue1");
        addVenue(db, "Boundary Box", "Sector 35", "Chandigarh", 1000, 30.7225, 76.7570, "venue2");
        addVenue(db, "Night Owl Cricket Box", "Phase 7", "Mohali", 900, 30.7046, 76.7179, "venue3");
        addVenue(db, "Sixer Turf", "Sector 70", "Mohali", 1100, 30.6990, 76.7000, "venue4");
        addVenue(db, "Power Play Box", "Sector 5", "Panchkula", 950, 30.6942, 76.8606, "venue5");
        addVenue(db, "Yorker Zone", "Sector 20", "Panchkula", 850, 30.6600, 76.8500, "venue6");

        // Synthetic bookings: one in the past and one upcoming, so both lists have data to show
        addSampleBooking(db, 3, "Night Owl Cricket Box", "Demo Strikers", -7, "07:00 PM", "2 hours", "6-a-side", "Floodlights", 1800);
        addSampleBooking(db, 1, "Sunrise Box Arena", "Demo Strikers", 3, "06:00 PM", "1 hour", "8-a-side", "None", 1200);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VENUES);
        onCreate(db);
    }

    private void addVenue(SQLiteDatabase db, String name, String area, String city, int rate,
                          double lat, double lng, String image) {
        ContentValues values = new ContentValues();
        values.put(V_NAME, name);
        values.put(V_AREA, area);
        values.put(V_CITY, city);
        values.put(V_RATE, rate);
        values.put(V_LAT, lat);
        values.put(V_LNG, lng);
        values.put(V_IMAGE, image);
        db.insert(TABLE_VENUES, null, values);
    }

    private void addSampleBooking(SQLiteDatabase db, int venueId, String venueName, String name,
                                  int daysFromToday, String time, String duration, String format,
                                  String extras, int price) {
        Calendar day = Calendar.getInstance();
        day.add(Calendar.DAY_OF_MONTH, daysFromToday);
        ContentValues values = new ContentValues();
        values.put(COL_VENUE_ID, venueId);
        values.put(COL_VENUE_NAME, venueName);
        values.put(COL_NAME, name);
        values.put(COL_PHONE, "9000000000");
        values.put(COL_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.getTime()));
        values.put(COL_TIME, time);
        values.put(COL_DURATION, duration);
        values.put(COL_FORMAT, format);
        values.put(COL_EXTRAS, extras);
        values.put(COL_PRICE, price);
        db.insert(TABLE_BOOKINGS, null, values);
    }

    // Today's date in the same yyyy-MM-dd format used in the bookings table
    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
    }

    // ---------- Venues ----------

    public Cursor getAllVenues() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_VENUES + " ORDER BY " + V_ID, null);
    }

    public Cursor getVenue(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_VENUES + " WHERE " + V_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    // ---------- Bookings ----------

    // INSERT - returns the new row id, or -1 if it failed
    public long insertBooking(int venueId, String venueName, String name, String phone, String date,
                              String time, String duration, String format, String extras, int price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_VENUE_ID, venueId);
        values.put(COL_VENUE_NAME, venueName);
        values.put(COL_NAME, name);
        values.put(COL_PHONE, phone);
        values.put(COL_DATE, date);
        values.put(COL_TIME, time);
        values.put(COL_DURATION, duration);
        values.put(COL_FORMAT, format);
        values.put(COL_EXTRAS, extras);
        values.put(COL_PRICE, price);
        long result = db.insert(TABLE_BOOKINGS, null, values);
        db.close();
        return result;
    }

    // SELECT bookings from today onwards (soonest first)
    public Cursor getUpcomingBookings() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BOOKINGS + " WHERE " + COL_DATE + " >= ? ORDER BY "
                + COL_DATE + " ASC", new String[]{today()});
    }

    // SELECT bookings before today (most recent first)
    public Cursor getPastBookings() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BOOKINGS + " WHERE " + COL_DATE + " < ? ORDER BY "
                + COL_DATE + " DESC", new String[]{today()});
    }

    // SELECT one booking by id
    public Cursor getBooking(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BOOKINGS + " WHERE " + COL_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    // Number of upcoming bookings (used on the Profile screen)
    public int countUpcomingBookings() {
        Cursor cursor = getUpcomingBookings();
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // UPDATE - returns the number of rows changed
    public int updateBooking(int id, String name, String phone, String date, String time,
                             String duration, String format, String extras, int price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_PHONE, phone);
        values.put(COL_DATE, date);
        values.put(COL_TIME, time);
        values.put(COL_DURATION, duration);
        values.put(COL_FORMAT, format);
        values.put(COL_EXTRAS, extras);
        values.put(COL_PRICE, price);
        int rows = db.update(TABLE_BOOKINGS, values, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // DELETE - returns the number of rows deleted
    public int deleteBooking(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_BOOKINGS, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }
}
