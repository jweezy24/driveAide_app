package com.example.driveaide;

import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import com.example.driveaide.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class DriveSummaryActivity extends AppCompatActivity {

    private TextView statsTextView;
    private TextView lifetimeStatsTextView;

    // Sample Dates
    String[] dates = new String[]{"2023-11-01", "2023-11-02", "2023-11-03"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_summary);

        // fetch the viewDrive button
        Button viewDriveButton = findViewById(R.id.view_map);

        // Set an onClickListener for the "Start Drive" button
        viewDriveButton.setOnClickListener(v -> {
            // Start the camera activity when the button is clicked
            startActivity(new Intent(this, MapActivity.class));
        });

        // fetch the viewDrive button
        Button exitStats = findViewById(R.id.return_home);

        // Set an onClickListener for the "Start Drive" button
        exitStats.setOnClickListener(v -> {
            // Start the camera activity when the button is clicked
            startActivity(new Intent(this, MainActivity.class));
        });

        BarChart barChart = findViewById(R.id.barChart);

        // Create dataset for the bar chart
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1f, 10f)); // Add entries in the form of new BarEntry(x, y)
        entries.add(new BarEntry(2f, 20f)); // x is the position of the bar, y is the height

        BarDataSet dataSet = new BarDataSet(entries, "Label"); // Add entries to dataset
        dataSet.setColor(Color.RED); // Optional: customize dataset appearance

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate(); // refresh


        Spinner dateSpinner = findViewById(R.id.dateSpinner);
        statsTextView = findViewById(R.id.statsTextView);
        lifetimeStatsTextView = findViewById(R.id.lifetimeStatsTextView);


        // Sort dates in descending order
        Arrays.sort(dates, Comparator.reverseOrder());

        // Implement spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(adapter);

        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Fetch and display statistics for selected date
                String selectedDate = (String) parent.getItemAtPosition(position);
                displayStatsForDate(selectedDate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Display Lifetime Stats
        displayLifetimeStats();

    }

    private void displayStatsForDate(String date) {
        // Fetch statistics based on the date and update statsTextView
        // This is a placeholder for demonstration
        statsTextView.setText("Statistics for " + date);
    }

    private void displayLifetimeStats() {
        // Fetch statistics based on the date and update statsTextView
        // This is a placeholder for demonstration
        lifetimeStatsTextView.setText("Lifetime statistics");
    }
}
