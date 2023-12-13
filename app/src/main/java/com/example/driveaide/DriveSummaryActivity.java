package com.example.driveaide;

import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DriveSummaryActivity extends AppCompatActivity {

    private final double EVENT_THRESHOLD = 0.5;

    String[] drives;

    Spinner dateSpinner;

    private ArrayList<HashMap<String, Object>> driveCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_summary);

        Intent localIntent = getIntent();



        ArrayList<HashMap<String, Object>> arrayList = (ArrayList<HashMap<String, Object>>) localIntent.getSerializableExtra("localDriveData");

        driveCache = arrayList;

        Log.d("DRIVESUMMARY", arrayList.toString());


//        fetchDriveNumbers();

        populateSpinnerLocal();

        // fetch the viewMap button
        Button viewDriveButton = findViewById(R.id.view_map);

        // Set an onClickListener for the "View Map" button
        viewDriveButton.setOnClickListener(v -> {
            // Start the camera activity when the button is clicked
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("driveNumber", "0");
            intent.putExtra("localDriveCache", arrayList);
            startActivity(intent);
        });

        // fetch the Return Home button
        Button exitStats = findViewById(R.id.return_home);

        // Set an onClickListener for the "Return Home" button
        exitStats.setOnClickListener(v -> {
            // Start the camera activity when the button is clicked
            startActivity(new Intent(this, MainActivity.class));
        });

    }

    private void fetchDriveNumbers() {
        DatabaseReference drivesRef = FirebaseDatabase.getInstance().getReference("drives");
        drivesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> driveNumbers = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String driveNumber = snapshot.getKey();
                    if (!"0".equals(driveNumber)) { // Exclude drive number 0
                        driveNumbers.add(driveNumber);
                    }
                }
                populateSpinner(driveNumbers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DriveSummaryActivity", "Error fetching drive numbers: " + databaseError.getMessage());
            }
        });
    }


    private void populateSpinnerLocal() {
        String[] driveNumbers = {"0"}; // Converted to String array

        dateSpinner = findViewById(R.id.dateSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, driveNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(adapter);
        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDriveNumber = (String) parent.getItemAtPosition(position);
                calculateDriveDurationLocal();
                calculateAndDisplayAverageEvents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void populateSpinner(List<String> driveNumbers) {
        dateSpinner = findViewById(R.id.dateSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, driveNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(adapter);
        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDriveNumber = (String) parent.getItemAtPosition(position);
                calculateDriveDuration(selectedDriveNumber);
                fetchDriveData(selectedDriveNumber);


                calculateDriveDurationLocal();
                fetchDriveData(selectedDriveNumber);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }


    private void fetchDataFromLocalCache(int driveNumber) {
        // Implement this method to retrieve data based on the drive number from your local cache
        // For example, if using a HashMap<String, DriveData> as a cache:
        // DriveData driveData = cacheMap.get(driveNumber);
        // Update your UI or logic based on driveData

    }

    private void fetchDriveData(String driveNumber) {
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference("drives").child(driveNumber);
        driveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Process and display the drive data
                // Example: statsTextView.setText("Data for drive " + driveNumber);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DriveSummaryActivity", "Error fetching drive data: " + databaseError.getMessage());
            }
        });
    }

    private void calculateDriveDuration(String driveNumber) {
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference("drives").child(driveNumber);
        driveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() < 2) {
                    // Handle case where there are not enough data points
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                Date firstLogDate = null;
                Date lastLogDate = null;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Date logDate = sdf.parse(snapshot.getKey());
                        if (logDate != null) {
                            if (firstLogDate == null || logDate.before(firstLogDate)) {
                                firstLogDate = logDate;
                            }
                            if (lastLogDate == null || logDate.after(lastLogDate)) {
                                lastLogDate = logDate;
                            }
                        }
                    } catch (ParseException e) {
                        Log.e("DriveSummaryActivity", "Error parsing timestamp: " + e.getMessage());
                    }
                }

                if (firstLogDate != null && lastLogDate != null) {
                    long durationMillis = lastLogDate.getTime() - firstLogDate.getTime();
                    displayDriveDuration(durationMillis);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DriveSummaryActivity", "Error fetching drive data: " + databaseError.getMessage());
            }
        });
    }



    private void calculateDriveDurationLocal() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date firstLogDate = null;
        Date lastLogDate = null;

        // Iterate through the local cache to find the drive data
        for (HashMap<String, Object> driveData : driveCache) {

                // Assuming each entry in the HashMap is a timestamp -> data mapping
                for (Map.Entry<String, Object> entry : driveData.entrySet()) {
                    try {
                        Log.d("ENRTY",entry.getKey());
                        if (entry.getKey().equals( "dateTime") ) {
                            String val = String.valueOf(entry.getValue());
                            Log.d("STRING", val);
                            Date logDate = sdf.parse(val);


                            if (logDate != null) {
                                if (firstLogDate == null || logDate.before(firstLogDate)) {
                                    firstLogDate = logDate;
                                }
                                if (lastLogDate == null || logDate.after(lastLogDate)) {
                                    lastLogDate = logDate;
                                }
                            }

                        }
                    } catch (ParseException e) {
                        Log.e("DriveSummaryActivity", "Error parsing timestamp: " + e.getMessage());
                    }

                }

        }

        if (firstLogDate != null && lastLogDate != null) {
            long durationMillis = lastLogDate.getTime() - firstLogDate.getTime();
            displayDriveDuration(durationMillis);
            Log.d("TIME", String.valueOf(durationMillis));
        } else {
            // Handle case where there are not enough data points or the drive data is not found
            Log.d("TIME", "IN ELSE");
        }
    }


    private void displayDriveDuration(long durationMillis) {
        // Convert duration from milliseconds to a readable format (e.g., hours, minutes, seconds)
        String durationString = convertMillisToReadableDuration(durationMillis);

        // Update the UI with the drive duration
        TextView driveDurationTextView = findViewById(R.id.driveDurationTextView);
        driveDurationTextView.setText("Drive Duration: " + durationString);
    }

    private String convertMillisToReadableDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void fetchAndProcessDrivingEventData(String driveNumber) {
        DatabaseReference driveRef = FirebaseDatabase.getInstance().getReference("drives").child(driveNumber);
        driveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Integer> eventCounts = new HashMap<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot eventSnapshot : snapshot.child("drivingEvents").getChildren()) {
                        String eventName = eventSnapshot.getKey();
                        Double eventValue = eventSnapshot.getValue(Double.class);
                        if (eventValue != null && eventValue > EVENT_THRESHOLD) {
                            eventCounts.put(eventName, eventCounts.getOrDefault(eventName, 0) + 1);
                        }
                    }
                }
                //updateBarChart(eventCounts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DriveSummaryActivity", "Error fetching event data: " + databaseError.getMessage());
            }
        });
    }

    private void calculateAndDisplayAverageEvents() {

        Map<String, Float> totalEventCounts = new HashMap<>();
        Map<String, Integer> eventOccurrences = new HashMap<>();

        // Process each dataMap in the driveCache
        for (HashMap<String, Object> dataMap : driveCache) {
            Object drivingEventsObj = dataMap.get("drivingEvents");

            // Check if drivingEventsObj is of the expected type
            if (drivingEventsObj instanceof Map) {
                Map<String, Float> drivingEvents = (Map<String, Float>) drivingEventsObj;
                for (Map.Entry<String, Float> entry : drivingEvents.entrySet()) {
                    String event = entry.getKey();
                    Float value = entry.getValue();
                    totalEventCounts.put(event, totalEventCounts.getOrDefault(event, 0.0f) + value);
                    eventOccurrences.put(event, eventOccurrences.getOrDefault(event, 0) + 1);
                }
            } else {
                Log.e("DataProcessing", "Unexpected data type for drivingEvents");
            }
        }

        Map<String, Float> averageEventCounts = new HashMap<>();
        for (String event : totalEventCounts.keySet()) {
            float average = totalEventCounts.get(event) / eventOccurrences.get(event);
            averageEventCounts.put(event, average);
        }

        Log.d("Data", averageEventCounts.toString());
        updateBarChart(averageEventCounts);
    }

    private void updateBarChart(Map<String, Float> eventCounts) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> eventLabels = new ArrayList<>(eventCounts.keySet());
        Collections.sort(eventLabels); // Sort event labels if needed

        for (int i = 0; i < eventLabels.size(); i++) {
            String event = eventLabels.get(i);
            entries.add(new BarEntry(i, eventCounts.get(event)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Driving Events");
        dataSet.setColor(Color.RED);

        BarData barData = new BarData(dataSet);
        BarChart barChart = (BarChart) findViewById(R.id.barChart);
        barChart.setData(barData);
        barChart.invalidate(); // Refresh the chart
    }


}
