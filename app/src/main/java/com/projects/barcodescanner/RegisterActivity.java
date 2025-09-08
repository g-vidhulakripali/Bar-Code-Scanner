// Create new Activity: com.projects.barcodescanner.RegisterActivity
package com.projects.barcodescanner;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.projects.barcodescanner.databinding.ActivityRegisterBinding;
import com.projects.barcodescanner.db.AppDatabase;
import com.projects.barcodescanner.db.User;
import com.projects.barcodescanner.db.UserDao;

import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        userDao = db.userDao();

        binding.registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String username = binding.usernameEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String role = binding.roleEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(role)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Run database operations on a background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Check if username already exists
            if (userDao.findByUsername(username) != null) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show());
                return;
            }

            // Hash the password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            User user = new User();
            user.username = username;
            user.passwordHash = hashedPassword;
            user.role = role;

            userDao.insert(user);

            runOnUiThread(() -> {
                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                finish(); // Go back to Login screen
            });
        });
    }
}