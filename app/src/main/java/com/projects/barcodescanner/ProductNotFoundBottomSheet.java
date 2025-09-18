package com.projects.barcodescanner;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProductNotFoundBottomSheet extends BottomSheetDialogFragment {

    // 1. Define the listener interface
    public interface OnScanCompletionListener {
        void onScanCompleted();
    }

    private static final String ARG_BARCODE = "barcode_arg";
    private OnScanCompletionListener completionListener;

    public static ProductNotFoundBottomSheet newInstance(String barcode) {
        ProductNotFoundBottomSheet fragment = new ProductNotFoundBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_BARCODE, barcode);
        fragment.setArguments(args);
        return fragment;
    }

    // 2. Add a method to set the listener
    public void setOnScanCompletionListener(OnScanCompletionListener listener) {
        this.completionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_product_not_found, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView barcodeTextView = view.findViewById(R.id.barcodeTextView);
        Button uploadButton = view.findViewById(R.id.uploadButton);

        String barcode = getArguments() != null ? getArguments().getString(ARG_BARCODE) : "N/A";
        barcodeTextView.setText("Barcode: " + barcode);

        uploadButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Upload feature coming soon!", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    // 3. Override onDismiss to trigger the callback
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (completionListener != null) {
            completionListener.onScanCompleted();
        }
    }
}