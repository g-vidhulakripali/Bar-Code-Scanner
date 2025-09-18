package com.projects.barcodescanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    // The ActivityResultLauncher is no longer needed because CameraScannerActivity
    // now handles its own UI (the popup) and doesn't return a result.
    // private final ActivityResultLauncher<Intent> cameraScannerLauncher = ...

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

        // Display welcome message (this remains the same)
        String username = sharedPreferences.getString("username", "User");
        welcomeTextView.setText("Welcome, " + username + "!");

        scanButton.setOnClickListener(v -> {
            // SIMPLIFIED: Simply launch the camera scanner activity.
            // We no longer need to wait for a result.
            Intent intent = new Intent(MainActivity.this, CameraScannerActivity.class);
            startActivity(intent);
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

    // The method to open the result page is no longer needed, as the popup
    // is now part of CameraScannerActivity's flow.
    // private void openBarcodeResultPage(String barcode) { ... }
}