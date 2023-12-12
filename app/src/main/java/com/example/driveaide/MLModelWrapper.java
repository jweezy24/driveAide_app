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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.res.AssetFileDescriptor;
import android.util.Log;

public class MLModelWrapper {

    private Module pytorchModel;
    private MTCNN tfliteModel;

    private Context context;

    private Tensor[] input_cache = new Tensor[10];
    float[] batchData = new float[10 * 3 * 3 * 448 * 448];

    private int current_index = 0;

    private List<Module> models = new ArrayList<Module>();
    private List<String> modelLabels = new ArrayList<String>();

    public MLModelWrapper(Context context, AssetManager assetmgr) throws IOException {
//        pytorchModel = loadPyTorchModel(context, "your_pytorch_model.ptl"); // Replace with your model file name
        this.context = context;

        loadModelsAndLabels(context);
        try{
            tfliteModel = new MTCNN(assetmgr);
        }catch (IOException e){
            tfliteModel = null;
        }
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
        File file = new File(context.getFilesDir(), assetName);

        try (InputStream is = context.getAssets().open(assetName);
             OutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        }
        return file.getAbsolutePath();
    }



    private Tensor bitmapToTensor(Bitmap bitmap) {
        // Resize the bitmap to the required input size for the model
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 448, 448, true);

        // Normalize the pixels
        float[] mean = new float[]{0.485f, 0.456f, 0.406f};
        float[] std = new float[]{0.229f, 0.224f, 0.225f};

        // Convert the bitmap to a tensor
        return TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, mean, std);
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

    public void add_video_to_video_cache(ArrayList<Bitmap> bitmaps){
        if(inference_ready()){
            return;
        }

        for (int j = 0; j < 3; j++) {
            // Assuming bitmapToTensor converts a single Bitmap to a Tensor of shape [3, 448, 448]
            Bitmap resizedImage = Bitmap.createScaledBitmap(bitmaps.get(j), 448, 448, true);
            Tensor tmp = bitmapToTensor(resizedImage);

            float[] image_data = tmp.getDataAsFloatArray();
            System.arraycopy(image_data,0,batchData,this.current_index,image_data.length);
            this.current_index += image_data.length;
            Log.d("DEBUGGING", String.valueOf(this.current_index));
        }


    }

    public boolean inference_ready(){
        if(this.current_index >= 1806335){
            return true;
        }
        return false;
    }

    public Tensor applySigmoid(Tensor inputTensor) {
        // Apply the sigmoid function to each element of the tensor
        float[] data = inputTensor.getDataAsFloatArray();
        float[] sigmoidData = new float[data.length];

        for (int i = 0; i < data.length; i++) {
            sigmoidData[i] = (float) (1 / (1 + Math.exp(-data[i])));
        }

        // Create a new tensor from the sigmoidData array with the same shape as the input tensor
        return Tensor.fromBlob(sigmoidData, inputTensor.shape());
    }

    public void printTensor(Tensor tensor,String tag) {
        // First, ensure that the tensor is 2D and the second dimension is 1
        if (tensor.shape().length != 2 || tensor.shape()[1] != 1) {
            throw new IllegalArgumentException("The tensor is not of shape [10, 1].");
        }

        // Get the data as a float array
        float[] data = tensor.getDataAsFloatArray();
        long[] shape = tensor.shape();

        // Iterate over the array and print each value
        for (int i = 0; i < shape[0]; i++) { // Rows
            float value = data[i]; // Only one column
            System.out.println(tag + " " +value);
        }
    }

    public void loadModelsAndLabels(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] files = assetManager.list(""); // List all files in the assets folder


        for (String file : files) {
            if (file.startsWith("model_") && file.endsWith(".ptl")) {
                Log.d("LOADING MODELS",file);
                String path = assetFilePath( context, file );
                Module tmp = LiteModuleLoader.load(path);
                if(tmp != null) {
                    models.add(tmp);
                    // Extract label (remove "model_" prefix and ".ptl" suffix)
                    String label = file.substring(6, file.length() - 4);
                    modelLabels.add(label);
                }

            }
        }

    }

    // Method to preprocess input data, run inference and process the output for PyTorch model
    public Map<String, Float> runPyTorchInference() throws InterruptedException, ExecutionException {
        Tensor input = Tensor.fromBlob(this.batchData, new long[]{10, 3, 3, 448, 448});

        ExecutorService executor = Executors.newFixedThreadPool(this.models.size()); // Create a thread pool
        List<Future<Map.Entry<String, Float>>> futures = new ArrayList<>();

        for (int i = 0; i < this.models.size(); i++) {
            final int count = i;
            futures.add(executor.submit(() -> {
                Module model = this.models.get(count);
                Tensor outputTensor = model.forward(IValue.from(input)).toTensor();
                Tensor predictions = applySigmoid(outputTensor);

                final float[] scores = predictions.getDataAsFloatArray();
                int maxScoreIdx = -1;
                float maxScore = -Float.MAX_VALUE;
                for (int j = 0; j < 1; j++) {
                    if (scores[j] > maxScore) {
                        maxScore = scores[j];
                        maxScoreIdx = j;
                    }
                }

                return new HashMap.SimpleEntry<>(this.modelLabels.get(count), scores[maxScoreIdx]);
            }));
        }
        this.current_index=0;
        Map<String, Float> modelValues = new HashMap<>();
        for (Future<Map.Entry<String, Float>> future : futures) {
            Map.Entry<String, Float> entry = future.get(); // This will wait for the thread to complete
            modelValues.put(entry.getKey(), entry.getValue());
        }

        executor.shutdown(); // Shutdown the executor service

        return modelValues;
    }

    // Method to preprocess input data, run inference and process the output for TensorFlow Lite model
    public Vector<Box> runTensorFlowLiteInference(Bitmap inputImage) {

        float[][][][] preprocessedImage = preprocessImage(inputImage);


        // Run inference
        float[][] output = new float[1][512]; // Adjust size as per model's output
        Vector<Box> bbox = tfliteModel.detectFaces(inputImage, 128);

        return bbox;
    }
}
