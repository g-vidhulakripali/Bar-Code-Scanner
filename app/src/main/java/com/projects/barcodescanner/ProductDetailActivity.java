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

    private ImageView productImageView;
    private TextView productNameTextView, brandTextView, priceTextView, availabilityTextView, countryFlagTextView;
    private Button updateButton;
    private ProgressBar progressBar;

    private View descriptionCard, healthBenefitsCard, additionalDetailsCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        initializeViews();

        updateButton.setOnClickListener(v -> Toast.makeText(this, "Update Clicked!", Toast.LENGTH_SHORT).show());

        String barcode = getIntent().getStringExtra("PRODUCT_BARCODE");
        Log.d(TAG, "onCreate: Received barcode from Intent: " + barcode);

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
        availabilityTextView = findViewById(R.id.availabilityTextView);
        countryFlagTextView = findViewById(R.id.countryFlagTextView);
        updateButton = findViewById(R.id.updateButton);
        progressBar = findViewById(R.id.progressBar);
        descriptionCard = findViewById(R.id.descriptionCard);
        healthBenefitsCard = findViewById(R.id.healthBenefitsCard);
        additionalDetailsCard = findViewById(R.id.additionalDetailsCard);
    }

    private void fetchProductDetails(String barcode) {
        Log.d(TAG, "fetchProductDetails: Fetching details for barcode: " + barcode);
        progressBar.setVisibility(View.VISIBLE);

        SupabaseService.getProductByBarcode(barcode, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "onFailure: Failed to fetch product details from Supabase.", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProductDetailActivity.this, "Network Error: Failed to load details.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "onResponse: Received response with code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    final String responseBody = response.body().string();
                    Log.d(TAG, "onResponse: Raw JSON from Supabase: " + responseBody);

                    try {
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                        if (jsonArray.size() > 0) {
                            Log.d(TAG, "onResponse: Product found in JSON. Parsing...");
                            JsonObject productObject = jsonArray.get(0).getAsJsonObject();
                            final Product product = parseProduct(productObject);
                            runOnUiThread(() -> populateUi(product));
                        } else {
                            Log.w(TAG, "onResponse: JSON array is empty. Product not found by Supabase for this query.");
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(ProductDetailActivity.this, "Product data not found in database.", Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse: Error parsing product JSON", e);
                        runOnUiThread(() -> Toast.makeText(ProductDetailActivity.this, "Error reading product data.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e(TAG, "onResponse: API call was not successful. Code: " + response.code());
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
        String jsonString = getString(obj, key);
        if (jsonString == null || jsonString.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // First parse string into a JsonElement
            JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
            Type listType = new TypeToken<List<String>>() {}.getType();
            return gson.fromJson(jsonArray, listType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse string to list for key: '" + key + "'. Value was: " + jsonString, e);
            return Collections.emptyList();
        }
    }


    private void populateUi(Product product) {
        Log.d(TAG, "populateUi: Populating UI with product: " + product.getProductName());
        progressBar.setVisibility(View.GONE);
        updateButton.setVisibility(View.VISIBLE);

        productNameTextView.setText(product.getProductName());
        brandTextView.setText("by " + product.getBrand());
        priceTextView.setText(product.getCurrency() + " " + product.getPrice()); // Added space for better formatting
        countryFlagTextView.setText(getFlagEmoji(product.getManufacturedIn()));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Picasso.get().load(product.getImageUrl()).placeholder(R.drawable.no_product).into(productImageView);
        }

        setupExpandableCard(descriptionCard, "Description", product.getDescription());
        setupExpandableCard(healthBenefitsCard, "Health Benefits", formatList(product.getHealthBenefits()));
        String additionalDetails = "SPECIFICATIONS:\n" + formatList(product.getSpecifications()) +
                "\n\nINGREDIENTS:\n" + formatList(product.getIngredients());
        setupExpandableCard(additionalDetailsCard, "Additional Details", additionalDetails);

        if (product.getAvailableStores() != null && !product.getAvailableStores().isEmpty()) {
            availabilityTextView.setText("Available at: " + String.join(", ", product.getAvailableStores()));
            availabilityTextView.setVisibility(View.VISIBLE);
        } else {
            availabilityTextView.setVisibility(View.GONE);
        }
    }

    private void setupExpandableCard(View cardView, String title, String content) {
        TextView titleView = cardView.findViewById(R.id.cardTitleTextView);
        TextView contentView = cardView.findViewById(R.id.cardContentTextView);
        ImageView arrowView = cardView.findViewById(R.id.arrowImageView);
        LinearLayout header = cardView.findViewById(R.id.headerLayout);

        // Hide the entire card if content is not available
        if (content == null || content.isEmpty() || content.equals("N/A")) {
            cardView.setVisibility(View.GONE);
            return;
        } else {
            cardView.setVisibility(View.VISIBLE);
        }

        titleView.setText(title);
        contentView.setText(content);

        header.setOnClickListener(v -> {
            boolean isVisible = contentView.getVisibility() == View.VISIBLE;
            contentView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            arrowView.animate().rotation(isVisible ? 0 : 180).setDuration(300).start();
        });
    }

    private String formatList(List<String> list) {
        if (list == null || list.isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            sb.append("• ").append(item).append("\n");
        }
        return sb.toString().trim();
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
}