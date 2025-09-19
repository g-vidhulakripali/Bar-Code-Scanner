package com.projects.barcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraScannerActivity extends AppCompatActivity implements ProductNotFoundBottomSheet.OnScanCompletionListener {

    private ExecutorService cameraExecutor;
    private PreviewView cameraPreviewView;
    private BarcodeScanner scanner;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;

    // --- CHANGE 1: Variable type is now MaterialButton ---
    private MaterialButton torchButton;
    private boolean isTorchOn = false;
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
        torchButton = findViewById(R.id.torchButton);
        MaterialButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());
        torchButton.setOnClickListener(v -> toggleTorch());

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
            if (camera.getCameraInfo().hasFlashUnit()) {
                torchButton.setVisibility(View.VISIBLE);
                updateTorchIcon(); // Set the initial correct icon
            } else {
                torchButton.setVisibility(View.GONE);
            }
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
                            if (cameraProvider != null) {
                                cameraProvider.unbindAll();
                            }
                            resetTorchUI();
                        });
                        String barcodeValue = barcodes.get(0).getRawValue();
                        Log.d("ScannerDebug", "Barcode Scanned: " + barcodeValue);
                        ContextCompat.getMainExecutor(this).execute(() -> showProductNotFoundPopup(barcodeValue));
                    } else {
                        isProcessing.set(false);
                    }
                })
                .addOnFailureListener(e -> isProcessing.set(false))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void showProductNotFoundPopup(String barcode) {
        ProductNotFoundBottomSheet bottomSheet = ProductNotFoundBottomSheet.newInstance(barcode);
        bottomSheet.setOnScanCompletionListener(this);
        bottomSheet.setCancelable(true);
        bottomSheet.show(getSupportFragmentManager(), "ProductNotFoundBottomSheetTag");
    }

    @Override
    public void onScanCompleted() {
        isProcessing.set(false);
        startCamera();
    }

    private void toggleTorch() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) return;
        isTorchOn = !isTorchOn;
        camera.getCameraControl().enableTorch(isTorchOn);
        updateTorchIcon();
    }

    private void resetTorchUI() {
        if (isTorchOn) {
            isTorchOn = false;
            updateTorchIcon();
        }
    }

    private void updateTorchIcon() {
        if (isTorchOn) {
            torchButton.setIconResource(R.drawable.ic_flashlight_on);
        } else {
            torchButton.setIconResource(R.drawable.ic_flashlight_off);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}