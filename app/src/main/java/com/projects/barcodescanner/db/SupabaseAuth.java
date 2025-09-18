package com.projects.barcodescanner.db;

import com.projects.barcodescanner.constants.Constants;
import okhttp3.*;
import org.json.JSONObject;

public class SupabaseAuth {
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Logs a user in with Supabase Auth using their email and password.
     */
    public static void loginUser(String email, String password, Callback callback) {
        String url = Constants.SUPABASE_URL + "/auth/v1/token?grant_type=password";

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }

    // You can remove fetchUserData as it is no longer needed.
}