package com.projects.barcodescanner.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat; // Import ZXing class
import com.journeyapps.barcodescanner.BarcodeEncoder; // Import BarcodeEncoder
import com.projects.barcodescanner.R;
import com.projects.barcodescanner.model.Product;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList = new ArrayList<>();

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImageView;
        private TextView productNameTextView;
        private TextView barcodeNumberTextView;
        private ImageView barcodeImageView; // The ImageView for the barcode
        private View edibleStatusDot;
        private TextView edibleStatusTextView;
        private Button viewMoreButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImageView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            barcodeNumberTextView = itemView.findViewById(R.id.barcodeNumberTextView);
            barcodeImageView = itemView.findViewById(R.id.barcodeImageView); // Initialize the barcode ImageView
            edibleStatusDot = itemView.findViewById(R.id.edibleStatusDot);
            edibleStatusTextView = itemView.findViewById(R.id.edibleStatusTextView);
            viewMoreButton = itemView.findViewById(R.id.viewMoreButton);
        }

        public void bind(Product product) {
            Context context = itemView.getContext();

            // Bind product name and barcode number
            productNameTextView.setText(product.getProductName().replace(" ", "\n"));
            barcodeNumberTextView.setText(product.getBarcode());

            // Load product image using Picasso
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Picasso.get().load(product.getImageUrl()).into(productImageView);
            } else {
                productImageView.setImageResource(R.drawable.product_default);
            }

            // Set edible status indicator
            if (product.isEdible()) {
                edibleStatusDot.setBackgroundResource(R.drawable.bg_circle_green);
                edibleStatusTextView.setText("EDIBLE");
                edibleStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_secondary_variant));
            } else {
                edibleStatusDot.setBackgroundResource(R.drawable.bg_circle_red);
                edibleStatusTextView.setText("NOT EDIBLE");
                edibleStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_error));
            }

            // --- BARCODE GENERATION LOGIC ---
            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                // CODE_128 is a common format that looks like your example.
                // You can also use other formats like UPC_A or EAN_13 if they fit your data.
                // The dimensions (e.g., 600x150) can be adjusted to fit your layout.
                Bitmap bitmap = barcodeEncoder.encodeBitmap(
                        product.getBarcode(),
                        BarcodeFormat.CODE_128,
                        600,
                        150
                );
                barcodeImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                // Optionally, hide the ImageView or set a placeholder if generation fails
                barcodeImageView.setVisibility(View.GONE);
            }


            viewMoreButton.setOnClickListener(v -> {
                Toast.makeText(context, "Viewing more about " + product.getProductName(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}