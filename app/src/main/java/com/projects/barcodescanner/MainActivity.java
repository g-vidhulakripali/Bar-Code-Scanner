package com.projects.barcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ExecutorService cameraExecutor;
    private PreviewView cameraPreviewView;
    private BarcodeScanner scanner;
    private boolean isScanningPaused = false; // Flag to prevent multiple scans of the same barcode

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan barcodes.", Toast.LENGTH_SHORT).show();
                    finish(); // Close the app if permission is denied
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Check for camera permission at startup
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
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
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

        // Configure the scanner for all barcode formats
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        scanner = BarcodeScanning.getClient(options);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null || isScanningPaused) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        isScanningPaused = true; // Pause scanning
                        String barcodeValue = barcodes.get(0).getRawValue();
                        Log.d("BarcodeScanner", "Barcode detected: " + barcodeValue);

                        // Launch result activity on the main thread
                        runOnUiThread(() -> openBarcodeResultPage(barcodeValue));
                    }
                })
                .addOnFailureListener(e -> Log.e("BarcodeScanner", "Barcode scanning failed", e))
                .addOnCompleteListener(task -> imageProxy.close()); // Always close the imageProxy
    }

    private void openBarcodeResultPage(String barcode) {
        // Stop the camera by unbinding all use cases
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            Log.e("BarcodeScanner", "Failed to unbind camera use cases", e);
        }

        Intent intent = new Intent(this, BarcodeResultActivity.class);
        intent.putExtra(BarcodeResultActivity.EXTRA_BARCODE_RESULT, barcode);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume scanning when the activity comes back into view
        isScanningPaused = false;
        checkCameraPermission(); // Re-check permission and start camera if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}