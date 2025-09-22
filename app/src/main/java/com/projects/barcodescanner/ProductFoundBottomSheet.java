package com.projects.barcodescanner;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.squareup.picasso.Picasso;

public class ProductFoundBottomSheet extends BottomSheetDialogFragment {

    public interface OnScanCompletionListener {
        void onScanCompleted();
    }

    // --- NEW: Added ARG_NAME constant ---
    private static final String ARG_NAME = "name_arg";
    private static final String ARG_BARCODE = "barcode_arg";
    private static final String ARG_DESCRIPTION = "description_arg";
    private static final String ARG_IMAGE_URL = "image_url_arg";

    private OnScanCompletionListener completionListener;

    // --- UPDATED: newInstance now accepts a 'name' parameter ---
    public static ProductFoundBottomSheet newInstance(String barcode, String name, String description, String imageUrl) {
        ProductFoundBottomSheet fragment = new ProductFoundBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_BARCODE, barcode);
        args.putString(ARG_NAME, name); // Add name to the bundle
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnScanCompletionListener(OnScanCompletionListener listener) {
        this.completionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_product_found, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- UPDATED: Find both TextViews by their new/correct IDs ---
        ImageView productImageView = view.findViewById(R.id.productImageView);
        TextView productNameTextView = view.findViewById(R.id.productNameTextView);
        TextView descriptionTextView = view.findViewById(R.id.descriptionTextView);
        TextView barcodeTextView = view.findViewById(R.id.barcodeTextView);
        Button viewAllButton = view.findViewById(R.id.viewAllButton);

        if (getArguments() != null) {
            String barcode = getArguments().getString(ARG_BARCODE, "N/A");
            // --- UPDATED: Retrieve name and description from the bundle ---
            String name = getArguments().getString(ARG_NAME, "Product Name");
            String description = getArguments().getString(ARG_DESCRIPTION, "No description found.");
            String imageUrl = getArguments().getString(ARG_IMAGE_URL);

            barcodeTextView.setText("Barcode: " + barcode);
            // --- UPDATED: Set text for both fields ---
            productNameTextView.setText(name);
            descriptionTextView.setText(description);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.no_product)
                        .error(R.drawable.no_product)
                        .into(productImageView);
            } else {
                productImageView.setImageResource(R.drawable.no_product);
            }
        }

        viewAllButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View All Details clicked!", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (completionListener != null) {
            completionListener.onScanCompleted();
        }
    }
}