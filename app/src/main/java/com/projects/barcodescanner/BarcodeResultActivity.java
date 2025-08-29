package com.projects.barcodescanner;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BarcodeResultActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE_RESULT = "com.projects.barcodescanner.BARCODE_RESULT";

    private TextView barcodeResultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_barcode_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        barcodeResultTextView = findViewById(R.id.barcodeResultTextView);

        String barcodeResult = getIntent().getStringExtra(EXTRA_BARCODE_RESULT);

        if (barcodeResult != null && !barcodeResult.isEmpty()) {
            barcodeResultTextView.setText(barcodeResult);
        } else {
            barcodeResultTextView.setText("No barcode found.");
        }
    }
}