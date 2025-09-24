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
    public static void getUserProfile(String userId, String accessToken, Callback callback) {
        String urlWithQuery = Constants.SUPABASE_URL +
                "/rest/v1/users?id=eq." + userId + "&select=username,role";

        Request request = new Request.Builder()
                .url(urlWithQuery)
                .get()
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                // --- CRITICAL FIX: Use the user's personal token for Authorization ---
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(callback);
    }


    // You can remove fetchUserData as it is no longer needed.
}