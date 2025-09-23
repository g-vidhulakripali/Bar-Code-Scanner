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

    /**
     * --- NEW METHOD IMPLEMENTED HERE ---
     * Fetches a single user profile from the 'users' table by their UUID.
     * @param userId The UUID of the user from Supabase Auth.
     * @param callback OkHttp callback to handle the response.
     */
    public static void getUserProfile(String userId, Callback callback) {
        // Query the users table to get the row where the 'id' column matches the userId
        String urlWithQuery = Constants.SUPABASE_URL + "/rest/v1/users" + "?id=eq." + userId + "&select=username";

        Request request = new Request.Builder()
                .url(urlWithQuery)
                .get()
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                // The Bearer token should be the service_role key for this kind of direct table access
                // Using the API Key here assumes it's the service_role key or the table has read access policies.
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(callback);
    }

    // You can remove fetchUserData as it is no longer needed.
}