package com.projects.barcodescanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.projects.barcodescanner.constants.Constants;
import com.projects.barcodescanner.db.SupabaseService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";

    // UI Components
    private TextInputEditText barcodeEditText, productNameEditText, descriptionEditText, otherDetailsEditText, priceEditText;
    private ImageView productImageView;
    private ProgressBar progressBar;
    private Spinner countrySpinner;
    private ImageButton searchButton;

    // Services & Clients
    private FusedLocationProviderClient fusedLocationClient;
    private OkHttpClient httpClient;
    private Gson gson;

    // State
    private Uri selectedImageUri;
    private String productBarcode;
    private ArrayAdapter<String> countryAdapter;
    private final Map<String, String> countryNameToCodeMap = new HashMap<>();

    // Activity Result Launchers
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onLocationPermissionResult);
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            selectedImageUri = result.getData().getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                productImageView.setImageBitmap(bitmap);
                productImageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Adjust scale type after loading
                productImageView.setPadding(0, 0, 0, 0); // Remove padding
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        initializeViews();
        setupToolbar();
        setupCountrySpinner();

        httpClient = new OkHttpClient();
        gson = new Gson();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        productBarcode = getIntent().getStringExtra("PRODUCT_BARCODE");
        barcodeEditText.setText(productBarcode);

        // Set default country based on location
        checkLocationPermission();

        // Set click listeners
        searchButton.setOnClickListener(v -> onSearchClicked());
        findViewById(R.id.imagePickerCard).setOnClickListener(v -> openImagePicker());
        findViewById(R.id.addProductButton).setOnClickListener(v -> saveProductFlow());
        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        barcodeEditText = findViewById(R.id.barcodeEditText);
        productNameEditText = findViewById(R.id.productNameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        otherDetailsEditText = findViewById(R.id.otherDetailsEditText);
        priceEditText = findViewById(R.id.priceEditText);
        productImageView = findViewById(R.id.productImageView);
        progressBar = findViewById(R.id.progressBar);
        countrySpinner = findViewById(R.id.countrySpinner);
        searchButton = findViewById(R.id.searchButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Product");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCountrySpinner() {
        // Populate map and list for spinner
        String[] isoCountryCodes = Locale.getISOCountries();
        List<String> countryNames = new ArrayList<>();
        for (String countryCode : isoCountryCodes) {
            Locale locale = new Locale("", countryCode);
            String countryName = locale.getDisplayCountry();
            countryNameToCodeMap.put(countryName, countryCode.toLowerCase());
            countryNames.add(countryName);
        }
        Collections.sort(countryNames);

        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countryNames);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(countryAdapter);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            setDefaultCountryFromLocation();
        }
    }

    private void onLocationPermissionResult(boolean isGranted) {
        if (isGranted) {
            setDefaultCountryFromLocation();
        } else {
            Toast.makeText(this, "Location permission denied. Cannot set default country.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setDefaultCountryFromLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Should not happen due to prior checks
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String countryName = addresses.get(0).getCountryName();
                        int position = countryAdapter.getPosition(countryName);
                        if (position >= 0) {
                            countrySpinner.setSelection(position);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoder failed", e);
                }
            }
        });
    }

    private void onSearchClicked() {
        String productName = productNameEditText.getText().toString().trim();
        if (productName.isEmpty()) {
            Toast.makeText(this, "Please enter a product name to search.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedCountryName = (String) countrySpinner.getSelectedItem();
        String countryCode = countryNameToCodeMap.get(selectedCountryName);

        if (countryCode == null) {
            Toast.makeText(this, "Please select a valid country.", Toast.LENGTH_SHORT).show();
            return;
        }

        searchProductOnSerpApi(productName, countryCode);
    }

    private void searchProductOnSerpApi(String productName, String countryCode) {
        showLoading();
        // Use standard Google search engine for better text matching
        String url = "https://serpapi.com/search.json?engine=google&q=" + productName + "&gl=" + countryCode + "&api_key=" + Constants.SERPAPI_KEY;

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddProductActivity.this, "API search failed.", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonObject = gson.fromJson(response.body().string(), JsonObject.class);
                    // Standard Google search returns 'organic_results'
                    if (jsonObject.has("organic_results")) {
                        JsonArray results = jsonObject.getAsJsonArray("organic_results");
                        if (results.size() > 0) {
                            // Take the first result
                            JsonObject firstResult = results.get(0).getAsJsonObject();
                            runOnUiThread(() -> populateUI(firstResult));
                        } else {
                            runOnUiThread(() -> Toast.makeText(AddProductActivity.this, "No details found for this product.", Toast.LENGTH_SHORT).show());
                        }
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(AddProductActivity.this, "API error: " + response.code(), Toast.LENGTH_SHORT).show());
                }
                runOnUiThread(AddProductActivity.this::hideLoading);
            }
        });
    }

    private void populateUI(JsonObject searchResult) {
        // We already have the product name, so we don't need to set it again.
        // Let's populate the description and other details.
        if (searchResult.has("snippet")) {
            descriptionEditText.setText(searchResult.get("snippet").getAsString());
        }

        // Try to extract brand or other info from the title or link
        if(searchResult.has("title")) {
            String title = searchResult.get("title").getAsString();
            // A simple heuristic to find a brand name
            String[] commonBrands = {"Amazon", "Walmart", "Target", "Best Buy"};
            for(String brand : commonBrands){
                if(title.contains(brand)){
                    otherDetailsEditText.setText(brand);
                    break;
                }
            }
        }
        Toast.makeText(this, "Fields auto-filled. Please review and edit.", Toast.LENGTH_LONG).show();
    }

    // --- The rest of the file (saveProductFlow, uploadImage, saveProductData, etc.) remains the same ---
    private void saveProductFlow() {
        if (productNameEditText.getText() == null || productNameEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Product name is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading();
        if (selectedImageUri != null) {
            uploadImageAndThenSaveData();
        } else {
            saveProductData(null);
        }
    }

    private void uploadImageAndThenSaveData() {
        try {
            File tempFile = createTempFileFromUri(selectedImageUri);
            if (tempFile == null) {
                hideLoading();
                return;
            }
            SupabaseService.uploadProductImage(tempFile, getContentResolver(), selectedImageUri, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddProductActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                        hideLoading();
                    });
                }
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (response.isSuccessful()) {
                        String fileName = call.request().url().pathSegments().get(call.request().url().pathSize() - 1);
                        String publicUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/product-images/" + fileName;
                        saveProductData(publicUrl);
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(AddProductActivity.this, "Image upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                            hideLoading();
                        });
                    }
                }
            });
        } catch (IOException e) {
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show();
            hideLoading();
        }
    }

    private void saveProductData(String imageUrl) {
        JsonObject productJson = new JsonObject();
        productJson.addProperty("product_name", productNameEditText.getText().toString());
        productJson.addProperty("barcode", productBarcode);
        productJson.addProperty("description", descriptionEditText.getText().toString());
        productJson.addProperty("brand", otherDetailsEditText.getText().toString());
        if (priceEditText.getText() != null && !priceEditText.getText().toString().isEmpty()) {
            productJson.addProperty("price", Double.parseDouble(priceEditText.getText().toString()));
        }
        String selectedCountryName = (String) countrySpinner.getSelectedItem();
        productJson.addProperty("manufactured_in", selectedCountryName);
        if (imageUrl != null) {
            productJson.addProperty("image_url", imageUrl);
        }
        SupabaseService.addProduct(productJson, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddProductActivity.this, "Failed to save product.", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddProductActivity.this, "Product uploaded successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddProductActivity.this, "Failed to save product: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                    hideLoading();
                });
            }
        });
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            Toast.makeText(this, "Cannot open image stream.", Toast.LENGTH_SHORT).show();
            return null;
        }
        File tempFile = File.createTempFile("upload", ".tmp", getCacheDir());
        tempFile.deleteOnExit();
        try (OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        inputStream.close();
        return tempFile;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void showLoading() { progressBar.setVisibility(View.VISIBLE); }
    private void hideLoading() { progressBar.setVisibility(View.GONE); }
}