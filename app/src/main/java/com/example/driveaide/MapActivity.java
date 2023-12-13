package com.example.driveaide;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    // ActivityMapBinding binding;

    private List<LatLng> locations = new ArrayList<>(); // Dynamic list for locations
    private boolean isMapReady = false; // Flag to track if the map is ready

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        String driveNumber = getIntent().getStringExtra("driveNumber");
        if (driveNumber != null) {
            fetchLocations(driveNumber);
        }

        // fetch the viewDrive button
        Button exitButton = findViewById(R.id.exit_button);

        // Set an onClickListener for the "Start Drive" button
        exitButton.setOnClickListener(v -> {
            // Start the camera activity when the button is clicked
            startActivity(new Intent(this, DriveSummaryActivity.class));
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // replace getCoords() with coordinates from database
        mMap = googleMap;
        isMapReady = true; // Set the flag to true as the map is ready

        if (!locations.isEmpty()) {
            drawRoute(locations); // Draw the route if locations are already fetched
        }
    }

    public void drawRoute(List<LatLng> points) {
        // draw a polyline using the list of points
        PolylineOptions route = new PolylineOptions()
                .addAll(points)
                .color(Color.BLUE) // Set color as per your preference
                .width(5); // Set width of the polyline

        mMap.addPolyline(route);

        // zoom the map to the route
        this.zoomMap(points);
    }

    public void zoomMap(List<LatLng> points) {
        // Calculate the bounds for LatLng points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();

        // Adjust the camera to show all the points within the bounds with padding
        int padding = 100; // Padding around the points (in pixels)
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        // Set the camera position on the map
        mMap.animateCamera(cu);
    }


    private void fetchLocations(String driveNumber) {
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference("drives").child(driveNumber);
        driveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                locations.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String latitudeStr = snapshot.child("latitude").getValue(String.class);
                    String longitudeStr = snapshot.child("longitude").getValue(String.class);

                    if (latitudeStr != null && longitudeStr != null && !latitudeStr.equals("N/A") && !longitudeStr.equals("N/A")) {
                        try {
                            double latitude = Double.parseDouble(latitudeStr);
                            double longitude = Double.parseDouble(longitudeStr);
                            locations.add(new LatLng(latitude, longitude));
                        } catch (NumberFormatException e) {
                            Log.e("MapActivity", "Error parsing coordinates: " + e.getMessage());
                        }
                    }
                }
                if (isMapReady && !locations.isEmpty()) {
                    drawRoute(locations);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MapActivity", "Error fetching locations: " + databaseError.getMessage());
            }
        });
    }




}
