package com.example.driveaide;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.widget.LinearLayout;
import android.content.Intent;
import android.widget.EditText;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button startDriveButton = findViewById(R.id.startDriveButton);

        Button viewDriveSummaryButton = findViewById(R.id.viewDriveSummaryButton);

        // Set an onClickListener for the "Start Drive" button
        startDriveButton.setOnClickListener(v -> {
            // Start the camera activity when the button is clicked
            startActivity(new Intent(this, CameraActivity.class));
        });

        viewDriveSummaryButton.setOnClickListener(v -> {
            // Start the camera activity when the button is clicked
            startActivity(new Intent(this, DriveSummaryActivity.class));
        });

    }
}
