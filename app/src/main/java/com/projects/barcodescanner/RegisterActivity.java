package com.projects.barcodescanner;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projects.barcodescanner.databinding.ActivityRegisterBinding;
import com.projects.barcodescanner.db.SupabaseClient;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- NEW LOGIC: Disable button initially and set up listeners ---
        binding.btnRegister.setEnabled(false); // Button is disabled by default

        // Create a single TextWatcher for all fields
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Check form validity after any text change
                validateForm();
            }
        };

        // Attach the watcher to all EditText fields
        binding.etUsername.addTextChangedListener(textWatcher);
        binding.etEmail.addTextChangedListener(textWatcher);
        binding.etPassword.addTextChangedListener(textWatcher);
        // --- END OF NEW LOGIC ---

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLoginNow.setOnClickListener(v -> finish());
    }

    // --- NEW METHOD: To check if all fields are filled ---
    private void validateForm() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Enable the button only if all fields are non-empty
        boolean isFormValid = !TextUtils.isEmpty(username) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password);
        binding.btnRegister.setEnabled(isFormValid);
    }

    private void registerUser() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // The validation is already handled by the button's enabled state,
        // but it's good practice to keep this check as a safeguard.
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a metadata object to send the username
        JSONObject metadata = new JSONObject();
        try {
            metadata.put("username", username);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating user data.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call the registerUser method that includes metadata
        SupabaseClient.registerUser(email, password, metadata, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String body = response.body().string();
                Log.d("SupabaseRegister", "Response Code: " + response.code());
                Log.d("SupabaseRegister", "Response Body: " + body);

                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.has("session") && !json.isNull("session")) {
                            runOnUiThread(() -> {
                                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(RegisterActivity.this, "Registration successful! Please confirm your email to log in.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("SupabaseRegister", "JSON Parsing Error on Success", e);
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error parsing registration response.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    String errorMessage = "Registration failed.";
                    try {
                        JSONObject errorJson = new JSONObject(body);
                        errorMessage = errorJson.optString("msg", errorJson.optString("message", "An unknown error occurred."));
                    } catch (Exception e) {
                        Log.e("SupabaseRegister", "JSON Parsing Error on Failure", e);
                    }
                    final String finalErrorMessage = errorMessage;
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show());
                }
            }
        });
    }
}