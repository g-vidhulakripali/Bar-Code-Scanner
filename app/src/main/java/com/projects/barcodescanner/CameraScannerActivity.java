package com.projects.barcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.projects.barcodescanner.db.SupabaseService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CameraScannerActivity extends AppCompatActivity implements ProductNotFoundBottomSheet.OnScanCompletionListener, ProductFoundBottomSheet.OnScanCompletionListener, SensorEventListener {

    private ExecutorService cameraExecutor;
    private PreviewView cameraPreviewView;
    private BarcodeScanner scanner;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;

    // --- Light Sensor and Torch Control ---
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private boolean isTorchOn = false;
    // Threshold in LUX for considering the environment as "dark"
    private static final float DARK_THRESHOLD = 15.0f;

    private boolean simulateLux = false;
    // --- End Light Sensor ---

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan barcodes.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scanner);

        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        MaterialButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());

        // Initialize Light Sensor
        // Initialize Light Sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null) {
            Toast.makeText(this, "No light sensor found. Auto-torch simulation enabled.", Toast.LENGTH_SHORT).show();
            simulateLux = true; // <-- enable simulation
        }



        cameraExecutor = Executors.newSingleThreadExecutor();
        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        scanner = BarcodeScanning.getClient(options);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e("CameraScannerActivity", "Use case binding failed", e);
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        if (isProcessing.get()) {
            imageProxy.close();
            return;
        }
        isProcessing.set(true);

        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        ContextCompat.getMainExecutor(this).execute(() -> {
                            setTorchState(false); // Turn off torch before stopping camera
                            if (cameraProvider != null) {
                                cameraProvider.unbindAll();
                            }
                        });
                        String barcodeValue = barcodes.get(0).getRawValue();
                        Log.d("ScannerDebug", "Barcode Scanned: " + barcodeValue);
                        checkProductInDatabase(barcodeValue);
                    } else {
                        isProcessing.set(false);
                    }
                })
                .addOnFailureListener(e -> isProcessing.set(false))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void checkProductInDatabase(String barcode) {
        SupabaseService.getProductByBarcode(barcode, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Supabase", "Failed to fetch product by barcode", e);
                runOnUiThread(() -> showProductNotFoundPopup(barcode));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();

                        if (jsonArray.size() > 0) {
                            // --- PRODUCT FOUND ---
                            JsonObject productObject = jsonArray.get(0).getAsJsonObject();

                            String name = "Product Name";
                            if (productObject.has("product_name") && !productObject.get("product_name").isJsonNull()) {
                                name = productObject.get("product_name").getAsString();
                            }
                            String description = "No description available";
                            if (productObject.has("description") && !productObject.get("description").isJsonNull()) {
                                description = productObject.get("description").getAsString();
                            }
                            String imageUrl = null;
                            if (productObject.has("image_url") && !productObject.get("image_url").isJsonNull()) {
                                imageUrl = productObject.get("image_url").getAsString();
                            }

                            final String finalName = name;
                            final String finalDescription = description;
                            final String finalImageUrl = imageUrl;
                            runOnUiThread(() -> showProductFoundPopup(barcode, finalName, finalDescription, finalImageUrl));

                        } else {
                            // --- PRODUCT NOT FOUND ---
                            runOnUiThread(() -> showProductNotFoundPopup(barcode));
                        }
                    } catch (Exception e) {
                        Log.e("Supabase", "Error parsing JSON response", e);
                        runOnUiThread(() -> showProductNotFoundPopup(barcode));
                    }
                } else {
                    Log.e("Supabase", "Unsuccessful response: " + response.code());
                    runOnUiThread(() -> showProductNotFoundPopup(barcode));
                }
            }
        });
    }

    private void showProductNotFoundPopup(String barcode) {
        ProductNotFoundBottomSheet bottomSheet = ProductNotFoundBottomSheet.newInstance(barcode);
        bottomSheet.setOnScanCompletionListener(this);
        bottomSheet.setCancelable(true);
        bottomSheet.show(getSupportFragmentManager(), "ProductNotFoundBottomSheetTag");
    }

    private void showProductFoundPopup(String barcode, String name, String description, String imageUrl) {
        ProductFoundBottomSheet bottomSheet = ProductFoundBottomSheet.newInstance(barcode, name, description, imageUrl);
        bottomSheet.setOnScanCompletionListener(this);
        bottomSheet.setCancelable(true);
        bottomSheet.show(getSupportFragmentManager(), "ProductFoundBottomSheetTag");
    }

    @Override
    public void onScanCompleted() {
        isProcessing.set(false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    private void setTorchState(boolean state) {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(state);
            isTorchOn = state;
        }
    }

    // --- SENSOR EVENT LISTENER IMPLEMENTATION ---

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            // Check if the camera is active before trying to control the torch
            if (camera != null) {
                if (lux < DARK_THRESHOLD && !isTorchOn) {
                    setTorchState(true); // Turn torch ON
                } else if (lux >= DARK_THRESHOLD && isTorchOn) {
                    setTorchState(false); // Turn torch OFF
                }
                Log.d("LightSensor", "Lux value: " + lux);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation.
    }

    // --- LIFECYCLE MANAGEMENT FOR SENSOR ---

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d("LightSensor", "Real light sensor registered");
        } else {
            Log.d("LightSensor", "No light sensor found, starting simulation...");
            simulateLuxValues();
        }
    }


    private void simulateLuxValues() {
        final float[] testLuxValues = {5f, 50f, 10f, 100f}; // dark → bright → dark → bright
        final int delay = 2000; // 2 seconds

        android.os.Handler handler = new android.os.Handler(getMainLooper());

        for (int i = 0; i < testLuxValues.length; i++) {
            float lux = testLuxValues[i];
            handler.postDelayed(() -> {
                Log.d("LightSensor", "Simulated Lux value: " + lux);
                if (camera != null) {
                    if (lux < DARK_THRESHOLD && !isTorchOn) {
                        setTorchState(true);
                    } else if (lux >= DARK_THRESHOLD && isTorchOn) {
                        setTorchState(false);
                    }
                }
            }, i * delay);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener when the activity is paused to save battery
        sensorManager.unregisterListener(this);
        // Also ensure the torch is off when leaving the screen
        setTorchState(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}