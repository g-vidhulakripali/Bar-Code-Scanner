package com.projects.barcodescanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup permission launcher
        requestCameraPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        launchCamera(); // If permission granted, launch camera
                    } else {
                        Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Prepare a file and URI to receive the camera photo
        imageUri = createImageUri();

        // Setup launcher to take a picture and receive the result
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess) {
                        // Picture was taken successfully, now scan it for a barcode
                        scanBarcodeFromUri(imageUri);
                    } else {
                        Toast.makeText(this, "Photo capture cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });

        Button scanButton = findViewById(R.id.cameraButton);
        scanButton.setText("Take Picture to Scan");
        scanButton.setOnClickListener(v -> checkCameraPermissionAndLaunchCamera());
    }

    private Uri createImageUri() {
        File imagePath = new File(getFilesDir(), "images");
        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }
        File imageFile = new File(imagePath, "barcode_image.jpg");
        return FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider", // Authority must match AndroidManifest
                imageFile
        );
    }

    private void checkCameraPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        if (imageUri != null) {
            takePictureLauncher.launch(imageUri);
        } else {
            Toast.makeText(this, "Could not prepare image file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanBarcodeFromUri(Uri imageUriToScan) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUriToScan);
            BarcodeScanner scanner = BarcodeScanning.getClient();

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.isEmpty()) {
                            Toast.makeText(this, "No barcode found in the image.", Toast.LENGTH_LONG).show();
                        } else {
                            // Get the first barcode found
                            String barcodeValue = barcodes.get(0).getRawValue();
                            Toast.makeText(this, "Scan Successful!", Toast.LENGTH_SHORT).show();
                            openBarcodeResultPage(barcodeValue);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Barcode scanning failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image for scanning.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openBarcodeResultPage(String barcode) {
        Intent intent = new Intent(this, BarcodeResultActivity.class);
        intent.putExtra(BarcodeResultActivity.EXTRA_BARCODE_RESULT, barcode);
        startActivity(intent);
    }
}