package com.projects.barcodescanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AdminProductBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PRODUCT_NAME = "product_name";
    private static final String ARG_PRODUCT_PRICE = "product_price";
    private static final String ARG_BARCODE = "barcode"; // Key for barcode

    // MODIFIED: newInstance now accepts a barcode
    public static AdminProductBottomSheet newInstance(String productName, double productPrice, String barcode) {
        AdminProductBottomSheet fragment = new AdminProductBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_NAME, productName);
        args.putDouble(ARG_PRODUCT_PRICE, productPrice);
        args.putString(ARG_BARCODE, barcode); // Add barcode to arguments
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_admin_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView productNameTextView = view.findViewById(R.id.adminProductNameTextView);
        TextView productPriceTextView = view.findViewById(R.id.adminProductPriceTextView);
        TextView barcodeTextView = view.findViewById(R.id.adminBarcodeTextView); // Get barcode TextView

        if (getArguments() != null) {
            String productName = getArguments().getString(ARG_PRODUCT_NAME);
            double productPrice = getArguments().getDouble(ARG_PRODUCT_PRICE);
            String barcode = getArguments().getString(ARG_BARCODE); // Get barcode from arguments

            productNameTextView.setText(productName);
            barcodeTextView.setText("Barcode: " + barcode); // Set the barcode text

            // Only show price if it's a valid product
            if (productPrice > 0) {
                productPriceTextView.setText(String.format("Price: $%.2f", productPrice));
                productPriceTextView.setVisibility(View.VISIBLE);
            } else {
                productPriceTextView.setVisibility(View.GONE);
            }
        }
    }
}