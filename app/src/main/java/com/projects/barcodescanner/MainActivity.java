package com.projects.barcodescanner;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.projects.barcodescanner.adapter.ProductAdapter;
import com.projects.barcodescanner.db.SupabaseAuth;
import com.projects.barcodescanner.db.SupabaseService;
import com.projects.barcodescanner.model.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // View Components
    private ViewPager2 productsViewPager;
    private TextView welcomeTextView;
    private AutoCompleteTextView locationAutoCompleteTextView;
    private TextView emptyStateTextView;
    private ProgressBar loadingProgressBar;
    private TabLayout pageIndicator;
    private ImageButton prevButton, nextButton;

    // Adapters & Data
    private ProductAdapter productAdapter;
    private SharedPreferences sharedPreferences;

    // Location Services
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();
        setupViewPager();

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fetchAndSetUsername();
        initializePermissionLauncher();

        // --- MODIFIED LOGIC ---
        // 1. Populate the dropdown with all countries immediately from a local source.
        List<String> allCountries = getAllCountries();
        setupLocationSpinner(allCountries);

        // 2. Then, proceed with trying to auto-detect the user's location.
        checkLocationPermissionAndFetchData();
    }

    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        locationAutoCompleteTextView = findViewById(R.id.locationAutoCompleteTextView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        productsViewPager = findViewById(R.id.productsViewPager);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        pageIndicator = findViewById(R.id.pageIndicator);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
    }

    private void setupListeners() {
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
        findViewById(R.id.scanButton).setOnClickListener(v -> openScanner());
        prevButton.setOnClickListener(v -> navigateViewPager(false));
        nextButton.setOnClickListener(v -> navigateViewPager(true));
    }

    private void setupViewPager() {
        productAdapter = new ProductAdapter();
        productsViewPager.setAdapter(productAdapter);
        new TabLayoutMediator(pageIndicator, productsViewPager, (tab, position) -> {}).attach();
    }

    private void initializePermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                getCurrentLocationAndFetchProducts();
            } else {
                Toast.makeText(this, "Permission denied. Please select a location manually.", Toast.LENGTH_LONG).show();
                showLoading(false);
                updateProductViewVisibility(false);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void updateProductViewVisibility(boolean hasProducts) {
        productsViewPager.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
        pageIndicator.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
        prevButton.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
        nextButton.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
        emptyStateTextView.setVisibility(hasProducts ? View.GONE : View.VISIBLE);
    }

    /**
     * NEW METHOD: Generates a sorted list of all country names using Android's Locale services.
     * This is efficient and does not require a network call.
     * @return A sorted List of country names.
     */
    private List<String> getAllCountries() {
        List<String> countries = new ArrayList<>();
        String[] isoCountryCodes = Locale.getISOCountries();
        for (String countryCode : isoCountryCodes) {
            Locale locale = new Locale("", countryCode);
            String countryName = locale.getDisplayCountry();
            if (countryName != null && !countryName.isEmpty()) {
                countries.add(countryName);
            }
        }
        Collections.sort(countries);
        return countries;
    }

    private void setupLocationSpinner(List<String> locations) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, locations);
        locationAutoCompleteTextView.setAdapter(adapter);
        locationAutoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = (String) parent.getItemAtPosition(position);
            fetchProductsFromSupabase(selectedLocation);
        });
    }

    private void checkLocationPermissionAndFetchData() {
        showLoading(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndFetchProducts();
        } else {
            showLoading(false); // Hide loader while permission dialog is shown
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentLocationAndFetchProducts() {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            String country = getCountryFromLocation(location);
                            locationAutoCompleteTextView.setText(country, false);
                            fetchProductsFromSupabase(country);
                        } else {
                            Toast.makeText(this, "Could not auto-detect location. Please select one.", Toast.LENGTH_LONG).show();
                            showLoading(false);
                            updateProductViewVisibility(false);
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "Failed to get current location.", e);
                        Toast.makeText(this, "Location disabled. Please select one.", Toast.LENGTH_LONG).show();
                        showLoading(false);
                        updateProductViewVisibility(false);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location Security Exception", e);
            showLoading(false);
            updateProductViewVisibility(false);
        }
    }

    private void fetchProductsFromSupabase(String country) {
        showLoading(true);
        if (country == null || country.isEmpty() || country.equalsIgnoreCase("Unknown")) {
            showLoading(false);
            updateProductViewVisibility(false);
            return;
        }

        SupabaseService.getProductsByLocation(country, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Error fetching products.", Toast.LENGTH_SHORT).show();
                    updateProductViewVisibility(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<Product> productList = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            Product p = new Product();
                            p.setProductName(obj.optString("product_name"));
                            p.setBarcode(obj.optString("barcode"));
                            p.setImageUrl(obj.optString("image_url"));
                            p.setEdible(obj.optBoolean("is_edible"));
                            productList.add(p);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing products JSON", e);
                    }
                }

                runOnUiThread(() -> {
                    showLoading(false);
                    productAdapter.setProducts(productList);
                    updateProductViewVisibility(!productList.isEmpty());
                });
            }
        });
    }

    // --- Unchanged Helper & User Action Methods ---

    private void fetchAndSetUsername() {
        String cachedUsername = sharedPreferences.getString("username", "User");
        welcomeTextView.setText("Hello, " + cachedUsername);
        String userId = sharedPreferences.getString("userId", null);
        if (userId == null) { return; }
        SupabaseAuth.getUserProfile(userId, new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {Log.e(TAG, "Username fetch failed", e);}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        if (jsonArray.length() > 0) {
                            String realUsername = jsonArray.getJSONObject(0).getString("username");
                            runOnUiThread(() -> welcomeTextView.setText("Hello, " + realUsername));
                            sharedPreferences.edit().putString("username", realUsername).apply();
                        }
                    } catch (Exception e) { Log.e(TAG, "Username parse error", e); }
                }
            }
        });
    }

    private String getCountryFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String country = addresses.get(0).getCountryName();
                if (country != null) return country;
            }
        } catch (IOException e) { Log.e(TAG, "Geocoder failed", e); }
        return "Unknown";
    }

    private void navigateViewPager(boolean forward) {
        int currentItem = productsViewPager.getCurrentItem();
        if (productAdapter.getItemCount() > 0) {
            productsViewPager.setCurrentItem(forward ? currentItem + 1 : currentItem - 1, true);
        }
    }

    private void openScanner() {
        startActivity(new Intent(MainActivity.this, CameraScannerActivity.class));
    }

    private void logout() {
        sharedPreferences.edit().clear().apply();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}