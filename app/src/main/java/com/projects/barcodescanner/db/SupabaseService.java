package com.projects.barcodescanner.db;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.projects.barcodescanner.constants.Constants;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SupabaseService {

    // --- CHANGE: Create a custom client with a longer timeout ---
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Time to establish a connection
            .readTimeout(30, TimeUnit.SECONDS)    // Time to wait for data to be read
            .writeTimeout(30, TimeUnit.SECONDS)   // Time to wait for data to be written
            .build();
    // --- END CHANGE ---

    private static final String PRODUCTS_ENDPOINT = Constants.SUPABASE_URL + "/rest/v1/products";

    /**
     * Inserts a new product into the Supabase 'products' table.
     * @param productJson A JsonObject containing the product data.
     * @param callback OkHttp callback to handle the response.
     */
    public static void addProduct(JsonObject productJson, Callback callback) {
        RequestBody body = RequestBody.create(
                productJson.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(PRODUCTS_ENDPOINT)
                .post(body)
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal") // To avoid getting the whole object back
                .build();

        client.newCall(request).enqueue(callback);
    }

    /**
     * Uploads an image file to Supabase Storage.
     * @param file The image file to upload.
     * @param contentResolver To determine the MIME type of the file.
     * @param imageUri The URI of the image.
     * @param callback OkHttp callback to handle the response.
     */
    public static void uploadProductImage(File file, ContentResolver contentResolver, Uri imageUri, Callback callback) {
        String fileName = "product_" + System.currentTimeMillis();
        String uploadUrl = Constants.SUPABASE_URL + "/storage/v1/object/product-images/" + fileName;

        RequestBody requestBody = RequestBody.create(file, MediaType.parse(contentResolver.getType(imageUri)));

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_API_KEY)
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                .addHeader("Content-Type", contentResolver.getType(imageUri))
                .build();

        client.newCall(request).enqueue(callback);
    }

    /**
     * Fetches a single product by its barcode.
     * @param barcode The product's barcode.
     * @param callback OkHttp callback to handle the response.
     */
    public static void getProductByBarcode(String barcode, Callback callback) {
        // The URL needs to be encoded properly, OkHttp handles this
        String urlWithQuery = PRODUCTS_ENDPOINT + "?barcode=eq." + barcode + "&select=*";

        Request request = new Request.Builder()
                .url(urlWithQuery)
                .get()
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(callback);
    }

    /**
     * Updates an existing product identified by its barcode.
     * @param barcode The barcode of the product to update.
     * @param updatedDataJson A JsonObject with the fields to update.
     * @param callback OkHttp callback to handle the response.
     */
    public static void updateProduct(String barcode, JsonObject updatedDataJson, Callback callback) {
        String urlWithQuery = PRODUCTS_ENDPOINT + "?barcode=eq." + barcode;

        RequestBody body = RequestBody.create(
                updatedDataJson.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(urlWithQuery)
                .patch(body) // PATCH is used for partial updates
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(callback);
    }

    /**
     * Fetches a list of products based on a location identifier.
     * @param location The location to filter products by.
     * @param callback OkHttp callback to handle the response.
     */
    public static void getProductsByLocation(String location, Callback callback) {
        String urlWithQuery = PRODUCTS_ENDPOINT + "?location=eq." + location + "&select=*";

        Request request = new Request.Builder()
                .url(urlWithQuery)
                .get()
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(callback);
    }

    /**
     * NEW METHOD: Fetches a distinct list of all locations (countries) from the products table.
     * This requires a PostgreSQL function to be created in the Supabase SQL Editor.
     * @param callback OkHttp callback to handle the response.
     */
    public static void getDistinctLocations(Callback callback) {
        // This URL points to a Remote Procedure Call (RPC)
        String url = Constants.SUPABASE_URL + "/rest/v1/rpc/get_distinct_locations";

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(new byte[0])) // RPCs are POST requests
                .addHeader("apikey", Constants.SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(callback);
    }

}