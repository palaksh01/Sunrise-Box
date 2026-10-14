package com.example.sunrisebox;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// Shows one venue (when a venue_id is passed) or every venue on Google Maps.
public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Centre of the tricity, used when showing all venues
    private static final LatLng TRICITY_CENTRE = new LatLng(30.7100, 76.7900);

    TextView tvVenueAddress;
    DatabaseHelper db;
    int venueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        tvVenueAddress = findViewById(R.id.tvVenueAddress);
        db = new DatabaseHelper(this);
        venueId = getIntent().getIntExtra("venue_id", -1);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        Cursor cursor;
        if (venueId != -1) {
            cursor = db.getVenue(venueId);
        } else {
            cursor = db.getAllVenues();
        }

        LatLng lastVenue = null;
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_NAME));
            String area = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_AREA));
            String city = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.V_CITY));
            int rate = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.V_RATE));
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.V_LAT));
            double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.V_LNG));

            lastVenue = new LatLng(lat, lng);
            googleMap.addMarker(new MarkerOptions()
                    .position(lastVenue)
                    .title(name)
                    .snippet(area + ", " + city + " • ₹" + rate + "/hr"));

            if (venueId != -1) {
                tvVenueAddress.setText(name + "\n📍 " + area + ", " + city + " (sample location)");
            }
        }
        cursor.close();

        if (venueId != -1 && lastVenue != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastVenue, 15));
        } else {
            tvVenueAddress.setText("All Sunrise Box venues in Chandigarh, Mohali & Panchkula (sample locations)");
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TRICITY_CENTRE, 11));
        }
    }
}
