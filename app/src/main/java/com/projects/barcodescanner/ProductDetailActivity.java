package com.projects.barcodescanner;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.projects.barcodescanner.db.SupabaseService;
import com.projects.barcodescanner.model.Product;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";

    // UI Views
    private ImageView productImageView;
    private TextView productNameTextView, brandTextView, priceTextView, countryFlagTextView,
            barcodeTextView, categoryTextView, edibleStatusTextView, descriptionTextView,
            ingredientsTextView, availabilityTextView, specificationsTextView, bestSuitedTextView,
            healthBenefitsTextView; // <-- ADDED
    private ProgressBar progressBar;
    private LinearLayout descriptionSection, ingredientsSection, availabilitySection, specificationsSection,
            healthBenefitsSection; // <-- ADDED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        initializeViews();

        String barcode = getIntent().getStringExtra("PRODUCT_BARCODE");
        if (barcode != null && !barcode.isEmpty()) {
            fetchProductDetails(barcode);
        } else {
            Toast.makeText(this, "Error: Barcode not found.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        productImageView = findViewById(R.id.productImageView);
        productNameTextView = findViewById(R.id.productNameTextView);
        brandTextView = findViewById(R.id.brandTextView);
        priceTextView = findViewById(R.id.priceTextView);
        countryFlagTextView = findViewById(R.id.countryFlagTextView);
        barcodeTextView = findViewById(R.id.barcodeTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        edibleStatusTextView = findViewById(R.id.edibleStatusTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        ingredientsTextView = findViewById(R.id.ingredientsTextView);
        availabilityTextView = findViewById(R.id.availabilityTextView);
        specificationsTextView = findViewById(R.id.specificationsTextView);
        bestSuitedTextView = findViewById(R.id.bestSuitedTextView);
        healthBenefitsTextView = findViewById(R.id.healthBenefitsTextView); // <-- ADDED

        // Section Layouts
        descriptionSection = findViewById(R.id.descriptionSection);
        ingredientsSection = findViewById(R.id.ingredientsSection);
        availabilitySection = findViewById(R.id.availabilitySection);
        specificationsSection = findViewById(R.id.specificationsSection);
        healthBenefitsSection = findViewById(R.id.healthBenefitsSection); // <-- ADDED

        progressBar = findViewById(R.id.progressBar);
    }

    private void fetchProductDetails(String barcode) {
        progressBar.setVisibility(View.VISIBLE);
        SupabaseService.getProductByBarcode(barcode, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: Failed to fetch product details.", e);
                handleNetworkError();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                        if (jsonArray.size() > 0) {
                            JsonObject productObject = jsonArray.get(0).getAsJsonObject();
                            final Product product = parseProduct(productObject);
                            runOnUiThread(() -> populateUi(product));
                        } else {
                            handleProductNotFound();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse: Error parsing product JSON", e);
                        handleDataError();
                    }
                } else {
                    Log.e(TAG, "onResponse: API call failed. Code: " + response.code());
                    handleDataError();
                }
            }
        });
    }

    private Product parseProduct(JsonObject jsonObject) {
        Product product = new Product();
        Gson gson = new Gson();

        product.setProductName(getString(jsonObject, "product_name"));
        product.setBrand(getString(jsonObject, "brand"));
        product.setDescription(getString(jsonObject, "description"));
        product.setCategory(getString(jsonObject, "category"));
        product.setPrice(getString(jsonObject, "price"));
        product.setCurrency(getString(jsonObject, "currency"));
        product.setBarcode(getString(jsonObject, "barcode"));
        product.setEdible(getBool(jsonObject, "is_edible"));
        product.setManufacturedIn(getString(jsonObject, "manufactured_in"));
        product.setImageUrl(getString(jsonObject, "image_url"));
        product.setLocation(getString(jsonObject, "location"));

        product.setSpecifications(parseStringList(gson, jsonObject, "specifications"));
        product.setHealthBenefits(parseStringList(gson, jsonObject, "health_benefits"));
        product.setIngredients(parseStringList(gson, jsonObject, "ingredients"));
        product.setAvailableStores(parseStringList(gson, jsonObject, "available_stores"));

        return product;
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private boolean getBool(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean();
    }

    private List<String> parseStringList(Gson gson, JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return Collections.emptyList();
        }

        try {
            // Case 1: Supabase returns a proper JSON array (ideal case)
            if (obj.get(key).isJsonArray()) {
                Type listType = new TypeToken<List<String>>() {}.getType();
                return gson.fromJson(obj.get(key).getAsJsonArray(), listType);
            }

            // Case 2: Supabase stored array as a string -> parse string into JSON
            String jsonString = obj.get(key).getAsString();
            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();

            Type listType = new TypeToken<List<String>>() {}.getType();
            return gson.fromJson(jsonArray, listType);

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse list for key: " + key + " | Value: " + obj.get(key), e);
            return Collections.emptyList();
        }
    }



    private void populateUi(Product product) {
        progressBar.setVisibility(View.GONE);

        // --- Populate Main Info ---
        productNameTextView.setText(product.getProductName());
        brandTextView.setText(product.getBrand());
        barcodeTextView.setText(product.getBarcode());
        categoryTextView.setText(product.getCategory());
        priceTextView.setText(String.format("%s%s", product.getCurrency(), product.getPrice()));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Picasso.get().load(product.getImageUrl()).placeholder(R.drawable.product_default).into(productImageView);
        }

        // --- Populate Info Box ---
        setupEdibleStatus(product.isEdible());
        String manufacturedText = "Manufactured: " + product.getManufacturedIn();
        countryFlagTextView.setText(manufacturedText);
        populateInfoLine(bestSuitedTextView, product.getLocation(), "Best suited for: ");

        // --- Populate Collapsible Sections ---
        populateSection(descriptionSection, descriptionTextView, product.getDescription());
        populateSection(ingredientsSection, ingredientsTextView, formatList(product.getIngredients()));
        populateSection(healthBenefitsSection, healthBenefitsTextView, formatList(product.getHealthBenefits())); // <-- ADDED
        populateSection(availabilitySection, availabilityTextView, formatList(product.getAvailableStores()));
        populateSection(specificationsSection, specificationsTextView, formatList(product.getSpecifications()));
    }

    private void setupEdibleStatus(boolean isEdible) {
        if (isEdible) {
            edibleStatusTextView.setText("Edible");
            edibleStatusTextView.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(this, R.drawable.dot_green), null, null, null);
        } else {
            edibleStatusTextView.setText("Not Edible");
            edibleStatusTextView.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(this, R.drawable.dot_red), null, null, null);
        }
    }

    private void populateInfoLine(TextView textView, String data, String prefix) {
        if (data != null && !data.isEmpty()) {
            textView.setText(prefix + data);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }
    private String formatList(List<String> list) {
        if (list == null || list.isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            sb.append("• ").append(item).append("\n");
        }
        return sb.toString().trim();
    }

    private void populateSection(View sectionView, TextView textView, String content) {
        if (content != null && !content.isEmpty()) {
            textView.setText(content);
            sectionView.setVisibility(View.VISIBLE);
        } else {
            sectionView.setVisibility(View.GONE);
        }
    }

    private String getFlagEmoji(String countryName) {
        if (countryName == null || countryName.isEmpty()) return "🏳️";
        switch (countryName.toLowerCase(Locale.ROOT)) {
            case "india": return "🇮🇳";
            case "usa":
            case "united states": return "🇺🇸";
            case "germany": return "🇩🇪";
            default: return "🏳️";
        }
    }

    private void handleNetworkError() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(ProductDetailActivity.this, "Network Error: Failed to load details.", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleProductNotFound() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(ProductDetailActivity.this, "Product data not found in database.", Toast.LENGTH_LONG).show();
        });
    }

    private void handleDataError() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(ProductDetailActivity.this, "Error reading product data.", Toast.LENGTH_SHORT).show();
        });
    }
}