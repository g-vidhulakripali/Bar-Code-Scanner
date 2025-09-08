// Your updated MainActivity.java
package com.projects.barcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

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

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        Button scanButton = findViewById(R.id.scanButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        TextView welcomeTextView = findViewById(R.id.welcomeTextView);

        // Display welcome message
        String username = sharedPreferences.getString("username", "User");
        welcomeTextView.setText("Welcome, " + username + "!");

        scanButton.setOnClickListener(v -> {
            // Launch the camera scanner activity
            Intent intent = new Intent(MainActivity.this, CameraScannerActivity.class);
            cameraScannerLauncher.launch(intent);
        });

        logoutButton.setOnClickListener(v -> logout());
    }

    private void logout() {
        // Clear shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Navigate back to LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Prevent going back to the main activity
    }

    private void openBarcodeResultPage(String barcode) {
        Intent intent = new Intent(this, BarcodeResultActivity.class);
        intent.putExtra(BarcodeResultActivity.EXTRA_BARCODE_RESULT, barcode);
        startActivity(intent);
    }
}