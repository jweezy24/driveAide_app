package com.example.driveaide;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.Vector;

import android.content.res.AssetFileDescriptor;
import android.util.Log;

public class MLModelWrapper {

    private Module pytorchModel;
    private MTCNN tfliteModel;

    private Context context;

    public MLModelWrapper(Context context, AssetManager assetmgr) {
//        pytorchModel = loadPyTorchModel(context, "your_pytorch_model.ptl"); // Replace with your model file name
        this.context = context;

        try{
            tfliteModel = new MTCNN(assetmgr);
        }catch (IOException e){
            tfliteModel = null;
        }
    }

    private Module loadPyTorchModel(Context context, String modelName) {
        Module model = null;
        try {
            model = LiteModuleLoader.load(assetFilePath(context, modelName));
        } catch (IOException e) {
            // Handle exceptions
        }
        return model;
    }

    private Interpreter loadTensorFlowLiteModel(Context context, String modelName) {
        Interpreter model = null;
        try {
            model = new Interpreter(loadModelFile(context, modelName));
        } catch (IOException e) {
            // Handle exceptions
        }
        return model;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private String assetFilePath(Context context, String assetName) throws IOException {
        return context.getFilesDir().getAbsolutePath() + "/" + assetName;
    }

    public float[][][][] preprocessImage(Bitmap image) {
        // Resize the image to the expected input size for FaceNet (e.g., 160x160)
        Bitmap resizedImage = Bitmap.createScaledBitmap(image, 160, 160, true);

        // Initialize an array to hold the preprocessed image data
        float[][][][] preprocessedImage = new float[1][160][160][3];

        // Iterate over each pixel, extract RGB values, normalize them, and store in the array
        for (int y = 0; y < 160; y++) {
            for (int x = 0; x < 160; x++) {
                int pixel = resizedImage.getPixel(x, y);

                // Normalize pixel values to [-1,1] or [0,1] as required by your specific FaceNet model
                // Here's an example normalization to [-1,1]
                preprocessedImage[0][y][x][0] = ((pixel >> 16) & 0xff) / 127.5f - 1; // Red channel
                preprocessedImage[0][y][x][1] = ((pixel >> 8) & 0xff) / 127.5f - 1;  // Green channel
                preprocessedImage[0][y][x][2] = (pixel & 0xff) / 127.5f - 1;        // Blue channel
            }
        }

        return preprocessedImage;
    }

    // Method to preprocess input data, run inference and process the output for PyTorch model
    public void runPyTorchInference(Bitmap inputImage) throws IOException {
        // Load the model (should be done only once, e.g., when the app starts)
        Module model = Module.load(assetFilePath(this.context, "model_0.pt"));

        Log.d("TORCH", "HERE");


        // Preprocess the input image
        Bitmap resizedImage = Bitmap.createScaledBitmap(inputImage, 448, 448, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedImage,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        // Run inference
        final Tensor outputTensor = model.forward(IValue.from(inputTensor)).toTensor();

        // Process and handle the output (example for a classification model)
        final float[] scores = outputTensor.getDataAsFloatArray();
        int maxScoreIdx = -1;
        float maxScore = -Float.MAX_VALUE;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }

        // Handle the result (e.g., display the classification)
//        String className = getClassLabel(maxScoreIdx); // Implement getClassLabel to map indices to class names
//        displayInferenceResult(className); // Implement displayInferenceResult to handle the display of results
    }

    // Method to preprocess input data, run inference and process the output for TensorFlow Lite model
    public Vector<Box> runTensorFlowLiteInference(Bitmap inputImage) {
        // Preprocess inputImage to a format suitable for your TensorFlow Lite model
        // ...

        // Run inference
        // ...
        float[][][][] preprocessedImage = preprocessImage(inputImage);

        // Prepare the model's input structure, if necessary
        // ...

        // Run inference
        float[][] output = new float[1][512]; // Adjust size as per model's output
        Vector<Box> bbox = tfliteModel.detectFaces(inputImage, 128);

        // Process and handle the output
        // ...
        return bbox;
    }
}
