package com.example.driveaide;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    // ActivityMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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
        this.drawRoute(this.getCoords());
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

    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    // a list of saved locations that represents a dummy route from Northbrook, IL to Madison, WI
    // LOCATIONS[x][0] is the latitude, LOCATIONS[x][1] is the longitude
    private static final double[][] LOCATIONS = {
            {42.137479927960655, -87.87257662808176},
            {42.13834293583288, -87.87283375049964},
            {42.138565793145936, -87.88124847920076},
            {42.1093252673987, -87.87650972974107},
            {42.10714820296511, -87.86972204392961},
            {42.104872799691776, -87.86838255341085},
            {42.09125849542489, -87.86799699365237},
            {42.05126508390043, -87.86914253714325},
            {41.990353463070115, -87.86779202986773},
            {41.99655213424321, -87.87526170774856},
            {42.02123744415552, -87.95401579041508},
            {42.05639502493344, -88.01876167674727},
            {42.066876211177295, -88.10657755261506},
            {42.06778431179494, -88.29242193656657},
            {42.234248178506185, -88.78680670235335},
            {42.23551915164242, -88.91177618482908},
            {42.24898989255836, -88.9608713367126},
            {42.27668483002652, -88.96499121138837},
            {42.36223350279965, -88.96464788872866},
            {42.47401965178804, -88.99457396723494},
            {42.71765643031303, -88.98496092943894},
            {42.740353980483015, -89.00521697167711},
            {42.80235157034594, -89.00624694038314},
            {42.90956644494773, -89.08589781681235},
            {42.96209909976571, -89.09928740612627},
            {43.00730661379683, -89.21739043497776},
            {43.037928358580665, -89.26991881516767},
            {43.04746872532756, -89.29566742096804},
            {43.04339183549875, -89.36927157323849},
            {43.06144189426256, -89.38693278812676},
            {43.06725048843446, -89.38584034212026},
            {43.06712116428289, -89.38566377218068},
            {43.06604024007413, -89.38869511029729},
            {43.067292528613635, -89.39051752189229},
            {43.06983657282658, -89.38696291715107},
            {43.073949001325026, -89.39232188988062},
            {43.07373776301536, -89.39315483224783},
            {43.0735259847934, -89.39303749836566}
    };

    private List<LatLng> getCoords() {
        List<LatLng> pointsList = new ArrayList<>();
        for (double[] LOCATION : LOCATIONS) {
            pointsList.add(new LatLng(LOCATION[0], LOCATION[1]));
        }
        return pointsList;
    }
}
