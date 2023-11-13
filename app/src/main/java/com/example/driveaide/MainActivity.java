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
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private Size previewSize;
    private String cameraId;
    private RecyclerView confidenceView;        // the recycler view being displayed under the camera
    private CustomRecyclerViewAdapter mAdapter; // the adapter for the recycler view
    private HashMap<String, Double> mDataMap;      // a map of confidence values for each category
    private List<ItemData> mDataList;               // a list of all distractions
    private final Handler recyclerViewHandler = new Handler(Looper.getMainLooper());
    private final int UPDATE_INTERVAL_MS = 10*1000; // 10 seconds x 1000 ms/1 second
    private final double DISTRACTION_THRESHOLD = 0.5;       // the threshold to determine distraction

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);

        mDataMap = new HashMap<>();
        mDataList = new ArrayList<>();

        // set up the recycler view
        confidenceView = findViewById(R.id.recyclerView);   // initialize recyclerView
        mAdapter = new CustomRecyclerViewAdapter(mDataList, this);  // initialize adapter
        confidenceView.setAdapter(mAdapter);    // link adapter
        confidenceView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHandler.postDelayed(updateTextViewRunnable, UPDATE_INTERVAL_MS);

        // Request camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            setUpCamera();
        }

    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    private void setUpCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    this.cameraId = cameraId;
                    previewSize = new Size(textureView.getWidth(), textureView.getHeight());
                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }

                    try {
                        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(previewSurface);

                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // Handle configuration failure
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Runnable to update the RecyclerView at a specified interval.
    private final Runnable updateTextViewRunnable = new Runnable() {
        @Override
        public void run() {
            // Returning early if mDataList is null.
            if (mDataMap == null) {
                return;
            }

            // Update list values shown
            mDataMap = MainActivity.this.fetchData();

            // update the list used in the recyclerview
            MainActivity.this.updateList();

            // Notifying the adapter that the data has changed, prompting it to update the RecyclerView.
            mAdapter.notifyDataSetChanged();

            // Rescheduling this Runnable to run again after the specified interval.
            recyclerViewHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    // searches through a HashMap and returns a list of all pairs with values greater than a certain
    // threshold.
    private void updateList() {
        mDataList.clear();
        for (String model : MODEL_LIST) {
            // access confidence value
            double val = mDataMap.get(model);

            // update the value in the list
            if (val >= DISTRACTION_THRESHOLD) {
                mDataList.add(new ItemData(model, String.format("%.6f", val)));
            }
        }
    }


    //*******************************************************************************************
    // for testing purposes; true hashmap will be provided by models in final version
    // Randomizes confidence levels for each model
    private HashMap<String, Double> fetchData() {
        HashMap<String, Double> map = new HashMap<String, Double>();
        for (String model : MODEL_LIST) {
            map.put(model, Math.random());
        }
        return map;
    }

    // for testing purposes; the true model list will be provided by models
    private final String[] MODEL_LIST = {
            "model A",
            "model B",
            "model C",
            "model D",
            "model E",
            "model F",
            "model G"
    };
    //*******************************************************************************************

}
