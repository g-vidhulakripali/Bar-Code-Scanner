package com.projects.barcodescanner.db;

import com.projects.barcodescanner.constants.Constants;
import okhttp3.*;
import org.json.JSONObject;

public class SupabaseClient {
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Registers a new user with Supabase Auth, sending along metadata like the username.
     * The database trigger will use this metadata to populate the public.users table.
     */
    public static void registerUser(String email, String password, JSONObject metadata, Callback callback) {
        String url = Constants.SUPABASE_URL + "/auth/v1/signup";

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
            json.put("data", metadata); // Key change: Sending metadata
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

    // You can remove the insertUserData method now as it is no longer used.
}