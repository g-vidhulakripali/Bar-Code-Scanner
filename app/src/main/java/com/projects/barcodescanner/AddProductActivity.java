package com.projects.barcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";

    // UI Components
    private EditText barcodeEditText, productNameEditText, descriptionEditText, categoryEditText,
            priceEditText, ingredientsEditText, manufacturedInEditText, availableStoresEditText,
            currencyEditText, brandEditText, specificationsEditText, healthBenefitsEditText;
    private ImageView productImageView;
    private ProgressBar progressBar;
    private Spinner countrySpinner;
    private ImageButton searchButton;
    private SwitchMaterial isEdibleSwitch;

    // Services & Clients
    private FusedLocationProviderClient fusedLocationClient;
    private OkHttpClient httpClient;
    private Gson gson;

    // State
    private Uri selectedImageUri;
    private String productBarcode;
    private ArrayAdapter<String> countryAdapter;
    private final Map<String, String> countryNameToCodeMap = new HashMap<>();
    private final Map<String, String> countryCodeToNameMap = new HashMap<>(); // NEW: Reverse map for reliable lookup

    // Activity Result Launchers
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onLocationPermissionResult);
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            selectedImageUri = result.getData().getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                productImageView.setImageBitmap(bitmap);
                productImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                productImageView.setPadding(0, 0, 0, 0);
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
        setupCountrySpinner();
        setupEditTextScrolling(); // <<< *** ADDED THIS LINE ***

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)   // longer connection timeout
                .readTimeout(60, TimeUnit.SECONDS)     // allow slow API responses
                .writeTimeout(60, TimeUnit.SECONDS)    // allow time to upload data
                .callTimeout(90, TimeUnit.SECONDS)     // total time for the call
                .build();

        gson = new Gson();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        productBarcode = getIntent().getStringExtra("PRODUCT_BARCODE");
        barcodeEditText.setText(productBarcode);

        checkLocationPermission();

        searchButton.setOnClickListener(v -> onSearchClicked());
        findViewById(R.id.imagePickerCard).setOnClickListener(v -> openImagePicker());
        findViewById(R.id.addProductButton).setOnClickListener(v -> saveProductFlow());
        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        barcodeEditText = findViewById(R.id.barcodeEditText);
        productNameEditText = findViewById(R.id.productNameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        priceEditText = findViewById(R.id.priceEditText);
        currencyEditText = findViewById(R.id.currencyEditText);
        ingredientsEditText = findViewById(R.id.ingredientsEditText);
        manufacturedInEditText = findViewById(R.id.manufacturedInEditText);
        availableStoresEditText = findViewById(R.id.availableStoresEditText);
        productImageView = findViewById(R.id.productImageView);
        progressBar = findViewById(R.id.progressBar);
        countrySpinner = findViewById(R.id.countrySpinner);
        searchButton = findViewById(R.id.searchButton);
        isEdibleSwitch = findViewById(R.id.isEdibleSwitch);
        brandEditText = findViewById(R.id.brandEditText);
        specificationsEditText = findViewById(R.id.specificationsEditText);
        healthBenefitsEditText = findViewById(R.id.healthBenefitsEditText);
    }

    // <<< *** NEW METHOD TO FIX SCROLLING ***
    @SuppressLint("ClickableViewAccessibility")
    private void setupEditTextScrolling() {
        // List of all multi-line EditTexts that could interfere with the ScrollView
        EditText[] scrollableEditTexts = {
                ingredientsEditText,
                healthBenefitsEditText,
                specificationsEditText,
                descriptionEditText
        };

        for (EditText editText : scrollableEditTexts) {
            editText.setOnTouchListener((v, event) -> {
                // We request the parent (ScrollView) to not intercept touch events.
                // This allows the EditText to handle scrolling its own content.
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if ((event.getAction() & MotionEvent.ACTION_UP) != 0) {
                    // When the touch is released, we allow the parent to intercept events again.
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                // We return false to allow the EditText to handle its default touch events (like cursor placement).
                return false;
            });
        }
    }


    private void setupCountrySpinner() {
        String[] isoCountryCodes = Locale.getISOCountries();
        List<String> countryNames = new ArrayList<>();
        for (String countryCode : isoCountryCodes) {
            Locale locale = new Locale("", countryCode);
            String countryName = locale.getDisplayCountry();
            countryNameToCodeMap.put(countryName, countryCode.toLowerCase());
            countryCodeToNameMap.put(countryCode.toUpperCase(), countryName); // NEW: Populate the reverse map
            countryNames.add(countryName);
        }
        Collections.sort(countryNames);
        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countryNames);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(countryAdapter);

        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCountryName = (String) parent.getItemAtPosition(position);
                String countryCode = countryNameToCodeMap.get(selectedCountryName);
                updateCurrencyForCountry(countryCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currencyEditText.setText("");
            }
        });
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

    // MODIFIED: This method is now much more reliable.
    private void setDefaultCountryFromLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String countryCode = addresses.get(0).getCountryCode(); // e.g., "US", "IN"
                        if (countryCode != null) {
                            // Use the reliable code to find the exact display name from our map
                            String countryName = countryCodeToNameMap.get(countryCode.toUpperCase());
                            if (countryName != null) {
                                int position = countryAdapter.getPosition(countryName);
                                if (position >= 0) {
                                    countrySpinner.setSelection(position);
                                    // The onItemSelected listener will automatically update the currency.
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoder failed", e);
                }
            }
        });
    }


    private void updateCurrencyForCountry(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            currencyEditText.setText("");
            return;
        }
        try {
            Locale locale = new Locale("", countryCode.toUpperCase());
            Currency currency = Currency.getInstance(locale);
            if (currency != null) {
                String currencyText = currency.getCurrencyCode() + " (" + currency.getSymbol(locale) + ")";
                currencyEditText.setText(currencyText);
            } else {
                currencyEditText.setText("N/A");
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Could not get currency for country code: " + countryCode, e);
            currencyEditText.setText("N/A");
        }
    }

    // ... The rest of your code remains unchanged ...

    private void onSearchClicked() {
        String productName = productNameEditText.getText().toString().trim();
        if (productName.isEmpty()) {
            Toast.makeText(this, "Please enter a product name to search.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedCountryName = (String) countrySpinner.getSelectedItem();
        String countryCode = countryNameToCodeMap.get(selectedCountryName); // Needed for backup
        if (countryCode == null || selectedCountryName == null) {
            Toast.makeText(this, "Please select a valid country.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Use the new primary API, with SerpAPI as a fallback
        searchProductOnCustomApi(productName, selectedCountryName, countryCode);
    }

    private void searchProductOnCustomApi(String productName, String countryName, String countryCode) {
        showLoading();
        String url = "https://barcode-scanner-api-l0kx.onrender.com/fetch-product-details";
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("productName", productName);
        jsonBody.addProperty("country", countryName);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Custom API search failed, falling back to SerpAPI.", e);
                runOnUiThread(() -> {
                    Toast.makeText(AddProductActivity.this, "Search failed. Trying backup...", Toast.LENGTH_SHORT).show();
                    searchProductOnSerpApi(productName, countryCode);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                        runOnUiThread(() -> {
                            populateUIFromCustomApiResponse(jsonObject);
                            hideLoading();
                        });
                    } catch (JsonSyntaxException e) {
                        Log.e(TAG, "Failed to parse custom API response, falling back to SerpAPI.", e);
                        runOnUiThread(() -> {
                            Toast.makeText(AddProductActivity.this, "Couldn't read search result. Trying backup...", Toast.LENGTH_SHORT).show();
                            searchProductOnSerpApi(productName, countryCode);
                        });
                    }
                } else {
                    Log.w(TAG, "Custom API error: " + response.code() + ", falling back to SerpAPI.");
                    runOnUiThread(() -> {
                        Toast.makeText(AddProductActivity.this, "Search error (" + response.code() + "). Trying backup...", Toast.LENGTH_SHORT).show();
                        searchProductOnSerpApi(productName, countryCode);
                    });
                }
            }
        });
    }

    private void populateUIFromCustomApiResponse(JsonObject data) {
        if (data.has("productName") && !data.get("productName").isJsonNull()) {
            productNameEditText.setText(data.get("productName").getAsString());
        }
        if (data.has("brand") && !data.get("brand").isJsonNull()) {
            brandEditText.setText(data.get("brand").getAsString());
        }
        if (data.has("description") && !data.get("description").isJsonNull()) {
            descriptionEditText.setText(data.get("description").getAsString());
        }
        if (data.has("category") && !data.get("category").isJsonNull()) {
            categoryEditText.setText(data.get("category").getAsString());
        }
        if (data.has("price") && !data.get("price").isJsonNull()) {
            priceEditText.setText(data.get("price").getAsString());
        }
        if (data.has("currency") && !data.get("currency").isJsonNull()) {
            currencyEditText.setText(data.get("currency").getAsString());
        }
        if (data.has("manufacturedIn") && !data.get("manufacturedIn").isJsonNull()) {
            manufacturedInEditText.setText(data.get("manufacturedIn").getAsString());
        }
        if (data.has("isEdible") && !data.get("isEdible").isJsonNull()) {
            isEdibleSwitch.setChecked(data.get("isEdible").getAsBoolean());
        }

        // Handle arrays by converting them to comma-separated strings
        if (data.has("ingredients") && data.get("ingredients").isJsonArray()) {
            ingredientsEditText.setText(jsonArrayToString(data.getAsJsonArray("ingredients")));
        }
        if (data.has("availableStores") && data.get("availableStores").isJsonArray()) {
            availableStoresEditText.setText(jsonArrayToString(data.getAsJsonArray("availableStores")));
        }
        if (data.has("healthBenefits") && data.get("healthBenefits").isJsonArray()) {
            healthBenefitsEditText.setText(jsonArrayToString(data.getAsJsonArray("healthBenefits")));
        }
        if (data.has("specifications") && data.get("specifications").isJsonArray()) {
            specificationsEditText.setText(specificationsJsonArrayToString(data.getAsJsonArray("specifications")));
        }

        Toast.makeText(this, "Product details populated. Please review.", Toast.LENGTH_LONG).show();
    }

    private String jsonArrayToString(JsonArray jsonArray) {
        if (jsonArray == null || jsonArray.size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jsonArray.size(); i++) {
            sb.append(jsonArray.get(i).getAsString());
            if (i < jsonArray.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String specificationsJsonArrayToString(JsonArray jsonArray) {
        if (jsonArray == null || jsonArray.size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject spec = jsonArray.get(i).getAsJsonObject();
            if (spec.has("key") && spec.has("value")) {
                String key = spec.get("key").getAsString();
                String value = spec.get("value").getAsString();
                sb.append(key).append(": ").append(value);
                if (i < jsonArray.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    // Fallback search
    private void searchProductOnSerpApi(String productName, String countryCode) {
        showLoading();
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
                    if (jsonObject.has("organic_results")) {
                        JsonArray results = jsonObject.getAsJsonArray("organic_results");
                        if (results.size() > 0) {
                            JsonObject firstResult = results.get(0).getAsJsonObject();
                            runOnUiThread(() -> populateUIFromSerpApi(firstResult));
                        } else {
                            runOnUiThread(() -> Toast.makeText(AddProductActivity.this, "No details found.", Toast.LENGTH_SHORT).show());
                        }
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(AddProductActivity.this, "API error: " + response.code(), Toast.LENGTH_SHORT).show());
                }
                runOnUiThread(AddProductActivity.this::hideLoading);
            }
        });
    }

    private void populateUIFromSerpApi(JsonObject searchResult) {
        String snippet = searchResult.has("snippet") ? searchResult.get("snippet").getAsString() : "";
        String title = searchResult.has("title") ? searchResult.get("title").getAsString() : "";

        descriptionEditText.setText(snippet);
        //descriptionEditText.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        //descriptionEditText.setVerticalScrollBarEnabled(true);
        manufacturedInEditText.setText((String) countrySpinner.getSelectedItem());

        if (snippet.toLowerCase().contains("category:")) {
            categoryEditText.setText(extractValueAfterKeyword(snippet, "category:"));
        }
        if (snippet.toLowerCase().contains("ingredients:")) {
            ingredientsEditText.setText(extractValueAfterKeyword(snippet, "ingredients:"));
        } else if (title.toLowerCase().contains("ingredients")) {
            ingredientsEditText.setText("Found in title, please verify.");
        }
        if (snippet.toLowerCase().contains("available at")) {
            availableStoresEditText.setText(extractValueAfterKeyword(snippet, "available at"));
        }

        Toast.makeText(this, "Fields auto-filled. Please review and edit.", Toast.LENGTH_LONG).show();
    }

    private String extractValueAfterKeyword(String text, String keyword) {
        try {
            int keywordIndex = text.toLowerCase().indexOf(keyword);
            if (keywordIndex != -1) {
                int startIndex = keywordIndex + keyword.length();
                int endIndex = text.indexOf(".", startIndex);
                if (endIndex == -1) endIndex = text.length();
                return text.substring(startIndex, endIndex).trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing snippet", e);
        }
        return "";
    }

    private void saveProductFlow() {
        if (productNameEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Product name is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading();
        if (selectedImageUri != null) {
            uploadImageAndThenSaveData();
        } else {
            // No image picked → use default placeholder
            String defaultImageUrl = Constants.SUPABASE_URL +
                    "/storage/v1/object/public/product-images/default.png";
            saveProductData(defaultImageUrl);
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
                        String fileName = call.request().url().pathSegments()
                                .get(call.request().url().pathSegments().size() - 1);

                        String publicUrl = Constants.SUPABASE_URL +
                                "/storage/v1/object/public/product-images/" + fileName;

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
        productJson.addProperty("category", categoryEditText.getText().toString());
        productJson.addProperty("manufactured_in", manufacturedInEditText.getText().toString());
        productJson.addProperty("is_edible", isEdibleSwitch.isChecked());
        productJson.addProperty("location", (String) countrySpinner.getSelectedItem());
        productJson.addProperty("brand", brandEditText.getText().toString());

        String currencyText = currencyEditText.getText().toString();
        if (!currencyText.isEmpty() && !currencyText.equalsIgnoreCase("N/A")) {
            String currencyCode = currencyText.split(" ")[0];
            productJson.addProperty("currency", currencyCode);
        }

        if (!priceEditText.getText().toString().isEmpty()) {
            try {
                productJson.addProperty("price", Double.parseDouble(priceEditText.getText().toString()));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
                hideLoading();
                return;
            }
        }

        productJson.add("ingredients", convertStringTojsonArray(ingredientsEditText.getText().toString()));
        productJson.add("available_stores", convertStringTojsonArray(availableStoresEditText.getText().toString()));
        productJson.add("specifications", convertStringTojsonArray(specificationsEditText.getText().toString()));
        productJson.add("health_benefits", convertStringTojsonArray(healthBenefitsEditText.getText().toString()));


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

    private JsonArray convertStringTojsonArray(String text) {
        JsonArray jsonArray = new JsonArray();
        if (text != null && !text.trim().isEmpty()) {
            String[] items = text.split(",");
            for (String item : items) {
                if (!item.trim().isEmpty()) {
                    jsonArray.add(item.trim());
                }
            }
        }
        return jsonArray;
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

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }
}