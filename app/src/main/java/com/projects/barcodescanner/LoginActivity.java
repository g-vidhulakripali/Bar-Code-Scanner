// Create new Activity: com.projects.barcodescanner.LoginActivity
package com.projects.barcodescanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.projects.barcodescanner.databinding.ActivityLoginBinding;
import com.projects.barcodescanner.db.AppDatabase;
import com.projects.barcodescanner.db.User;
import com.projects.barcodescanner.db.UserDao;

import org.mindrot.jbcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private UserDao userDao;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            navigateToMain();
            return; // Skip the rest of onCreate
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        userDao = db.userDao();

        // **CORRECTED ID**: Changed from loginButton to btnLogin
        binding.btnLogin.setOnClickListener(v -> loginUser());

        // **CORRECTED ID and VIEW TYPE**: Changed from registerButton to tvRegister (TextView)
        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // (Optional) Add listener for Forgot Password
        binding.tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
            // Implement forgot password logic here
        });
    }

    private void loginUser() {
        // **CORRECTED ID**: Changed from usernameEditText to etUsername
        String username = binding.etUsername.getText().toString().trim();
        // **CORRECTED ID**: Changed from passwordEditText to etPassword
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Username and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            User user = userDao.findByUsername(username);

            if (user != null && BCrypt.checkpw(password, user.passwordHash)) {
                // Login successful
                // Save login state
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putString("username", user.username);
                editor.apply();

                // Navigate to MainActivity
                runOnUiThread(this::navigateToMain);
            } else {
                // Login failed
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Prevent going back to the login screen
    }
}