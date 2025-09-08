package com.projects.barcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Launcher for the camera scanner activity
    private final ActivityResultLauncher<Intent> cameraScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Get the scanned barcode from the result
                    String barcode = result.getData().getStringExtra(CameraScannerActivity.EXTRA_SCANNED_BARCODE);
                    if (barcode != null) {
                        // Launch the result activity
                        openBarcodeResultPage(barcode);
                    }
                }
            });

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

        Button scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(v -> {
            // Launch the camera scanner activity
            Intent intent = new Intent(MainActivity.this, CameraScannerActivity.class);
            cameraScannerLauncher.launch(intent);
        });
    }

    private void openBarcodeResultPage(String barcode) {
        Intent intent = new Intent(this, BarcodeResultActivity.class);
        intent.putExtra(BarcodeResultActivity.EXTRA_BARCODE_RESULT, barcode);
        startActivity(intent);
    }
}