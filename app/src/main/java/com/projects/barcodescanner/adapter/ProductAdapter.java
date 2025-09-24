package com.projects.barcodescanner.adapter;

import android.content.Context;
import android.content.Intent; // <-- Import the Intent class
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

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.projects.barcodescanner.ProductDetailActivity; // <-- Import your new Activity
import com.projects.barcodescanner.R;
import com.projects.barcodescanner.model.Product;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList = new ArrayList<>();
    private static final int ITEM_COUNT = 1000; // Use a large but reasonable number

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        if (productList.isEmpty()) {
            return;
        }
        Product product = productList.get(position % productList.size());
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.isEmpty() ? 0 : ITEM_COUNT;
    }

    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    public int getRealCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImageView;
        private TextView productNameTextView;
        private TextView barcodeNumberTextView;
        private ImageView barcodeImageView;
        private View edibleStatusDot;
        private TextView edibleStatusTextView;
        private Button viewMoreButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImageView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            barcodeNumberTextView = itemView.findViewById(R.id.barcodeNumberTextView);
            barcodeImageView = itemView.findViewById(R.id.barcodeImageView);
            edibleStatusDot = itemView.findViewById(R.id.edibleStatusDot);
            edibleStatusTextView = itemView.findViewById(R.id.edibleStatusTextView);
            viewMoreButton = itemView.findViewById(R.id.viewMoreButton);
        }

        public void bind(Product product) {
            Context context = itemView.getContext();

            productNameTextView.setText(product.getProductName());
            barcodeNumberTextView.setText(product.getBarcode());

            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Picasso.get().load(product.getImageUrl()).into(productImageView);
            } else {
                productImageView.setImageResource(R.drawable.product_default);
            }

            if (product.isEdible()) {
                edibleStatusDot.setBackgroundResource(R.drawable.bg_circle_green);
                edibleStatusTextView.setText("EDIBLE");
                edibleStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_secondary_variant));
            } else {
                edibleStatusDot.setBackgroundResource(R.drawable.bg_circle_red);
                edibleStatusTextView.setText("NOT EDIBLE");
                edibleStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.design_default_color_error));
            }

            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(
                        product.getBarcode(),
                        BarcodeFormat.CODE_128,
                        600,
                        150
                );
                barcodeImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                barcodeImageView.setVisibility(View.GONE);
            }

            viewMoreButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProductDetailActivity.class);
                intent.putExtra("PRODUCT_BARCODE", product.getBarcode());
                context.startActivity(intent);
            });
        }
    }
}