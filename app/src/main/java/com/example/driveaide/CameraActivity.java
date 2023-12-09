package com.example.driveaide;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.LinearLayout;
import android.content.Intent;
import android.widget.EditText;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

private TextureView textureView;
    private CameraDevice cameraDevice;
    private Size previewSize;
    private String cameraId;
    private MLModelWrapper mlModelWrapper;
    private int count = 0;
    private ImageView iv= null;
    //Thread lock
    private Object lock = new Object();
    private float[] global_bbox = null;
    private RecyclerView confidenceView;        // the recycler view being displayed under the camera
    private CustomRecyclerViewAdapter mAdapter; // the adapter for the recycler view
    private HashMap<String, Double> mDataMap;      // a map of confidence values for each category
    private List<ItemData> mDataList;               // a list of all distractions
    private final Handler recyclerViewHandler = new Handler(Looper.getMainLooper());
    private final int UPDATE_INTERVAL_MS = 10*1000; // 10 seconds x 1000 ms/1 second
    private final double DISTRACTION_THRESHOLD = 0.5;       // the threshold to determine distraction

    private Map<String, Float> model_results = null;

    private ArrayList<Bitmap> frame_cache = new ArrayList<>(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create TextureView
        textureView = new TextureView(this);
//
//        // Set layout parameters for the TextureView
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);  // Using weight to occupy two-thirds of the screen
        textureView.setLayoutParams(params);
//
//        // Create the main layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(textureView);


        textureView.setSurfaceTextureListener(surfaceTextureListener);

        iv = new ImageView(this);
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.5f);
        iv.setLayoutParams(ivParams);
        layout.addView(iv);



        mDataMap = new HashMap<>();
        mDataList = new ArrayList<>();

        // set up the recycler view
        // confidenceView = findViewById(R.id.recyclerView);   // initialize recyclerView
        RecyclerView rView = new RecyclerView(this);
        confidenceView = rView;
        mAdapter = new CustomRecyclerViewAdapter(mDataList, this);  // initialize adapter
        confidenceView.setAdapter(mAdapter);    // link adapter
        confidenceView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHandler.postDelayed(updateTextViewRunnable, UPDATE_INTERVAL_MS);

        layout.addView(confidenceView);



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            setUpCamera();
        }

        setContentView(layout);
        AssetManager asstmgr = this.getAssets();
        try {
            mlModelWrapper = new MLModelWrapper(this,asstmgr);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            if(count%10 == 0) {
                synchronized (lock){
                    processAndDisplayImage();
                }
            }
            count+=1;
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

    private Bitmap getBitmapFromTextureView(TextureView textureView) {
        Bitmap bitmap = null;
        if (textureView.isAvailable()) {
            bitmap = textureView.getBitmap();
        }
        return bitmap;
    }

    private void processImage() {
        Bitmap bitmap = getBitmapFromTextureView(textureView);
        if (bitmap != null) {
            // Run the inference in a background thread
            new Thread(() -> {
                // Replace 'runTensorFlowLiteInference' with the appropriate method name
                mlModelWrapper.runTensorFlowLiteInference(bitmap);
                // Handle the result, e.g., display the processed bitmap or bounding boxes
                // Remember to switch back to the main thread if updating UI
            }).start();
        }
    }

    private Bitmap drawBoundingBoxes(Bitmap bitmap, float[] boundingBoxes) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);


        // Assuming the box contains [top, left, bottom, right] coordinates
        canvas.drawRect(boundingBoxes[1], boundingBoxes[0], boundingBoxes[3], boundingBoxes[2], paint);
        runOnUiThread(() ->{
            iv.setImageBitmap(mutableBitmap);
        });
        return mutableBitmap;
    }

    private void drawBoundingBoxesOnTextureView(float[] boundingBoxes) {
        runOnUiThread(() -> {

            // Create a transparent bitmap
            Bitmap overlayBitmap = Bitmap.createBitmap(textureView.getWidth(), textureView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(overlayBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);

            // Draw bounding box
            canvas.drawRect(boundingBoxes[1], boundingBoxes[0], boundingBoxes[3], boundingBoxes[2], paint);

            // Set the bitmap as the image for the overlay
            runOnUiThread (() -> {
                iv.draw(canvas);
            });
        });
    }

    private void logBoundingBoxes(float[] boundingBoxes) {
        StringBuilder sb = new StringBuilder();

        sb.append("Box ").append(": ");
        sb.append("[");
        for (int j = 0; j < boundingBoxes.length; j++) {
            sb.append(boundingBoxes[j]);
            if (j < boundingBoxes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]\n");

        Log.d("BoundingBoxes", sb.toString());
    }

    private void processAndDisplayImage() {
        Bitmap bitmap = getBitmapFromTextureView(textureView);
        if (bitmap != null) {
            new Thread(() -> {
                synchronized (lock){
                Vector<Box> bb = mlModelWrapper.runTensorFlowLiteInference(bitmap);
                if (bb.size() > 0) {
                    float[] boundingBoxes = mlModelWrapper.runTensorFlowLiteInference(bitmap).firstElement().getBbr();
                    global_bbox = boundingBoxes;

                    int x = Math.max((int) boundingBoxes[1], 0);
                    int y = Math.max((int) boundingBoxes[0], 0);
                    int width = Math.min((int) (boundingBoxes[3] - boundingBoxes[1]), bitmap.getWidth() - x);
                    int height = Math.min((int) (boundingBoxes[2] - boundingBoxes[0]), bitmap.getHeight() - y);

                    // Ensure the width and height are positive
                    width = Math.max(width, 0);
                    height = Math.max(height, 0);

                    // Check if the bounding box is within the bitmap dimensions
                    if (x + width <= bitmap.getWidth() && y + height <= bitmap.getHeight()) {
                        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height);

                        if (this.frame_cache.size() < 3){
                            this.frame_cache.add(croppedBitmap);
                        }else if(this.frame_cache.size() == 3){
                            mlModelWrapper.add_video_to_video_cache(this.frame_cache);
                            this.frame_cache.clear();
                        }

                        if(mlModelWrapper.inference_ready()) {
                            try {

                                Map<String, Float> reses =  mlModelWrapper.runPyTorchInference();
                                Log.d("Results",reses.toString());
                                this.model_results = reses;
                            }catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        logBoundingBoxes(boundingBoxes);
                        drawBoundingBoxes(bitmap, boundingBoxes);

                        // Now, you can use croppedBitmap for further processing or display
                    }
                }
                }//thread
            }).start();
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
            // update the list used in the recyclerview
            CameraActivity.this.updateList();

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
        if(model_results != null){
            for (String model : model_results.keySet()) {
                // access confidence value
                float val = model_results.get(model);

                // update the value in the list
                if (val >= DISTRACTION_THRESHOLD) {
                    mDataList.add(new ItemData(model, String.format("%.6f", val)));
                }
            }
        }
    }


}

