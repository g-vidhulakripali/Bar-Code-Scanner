package com.projects.barcodescanner.model;

import java.util.List;

public class Product {
    private String productName;
    private String brand;
    private String description;
    private String category;
    private String price;
    private String currency;
    private List<String> specifications;
    private String barcode;
    private boolean isEdible;
    private List<String> healthBenefits;
    private List<String> ingredients;
    private String manufacturedIn;
    private List<String> availableStores;
    private String imageUrl;
    private String location;

    // Getters and Setters for all fields
    // You can generate these automatically in Android Studio (Right-click -> Generate -> Getters and Setters)

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<String> getSpecifications() { return specifications; }
    public void setSpecifications(List<String> specifications) { this.specifications = specifications; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public boolean isEdible() { return isEdible; }
    public void setEdible(boolean edible) { isEdible = edible; }
    public List<String> getHealthBenefits() { return healthBenefits; }
    public void setHealthBenefits(List<String> healthBenefits) { this.healthBenefits = healthBenefits; }
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    public String getManufacturedIn() { return manufacturedIn; }
    public void setManufacturedIn(String manufacturedIn) { this.manufacturedIn = manufacturedIn; }
    public List<String> getAvailableStores() { return availableStores; }
    public void setAvailableStores(List<String> availableStores) { this.availableStores = availableStores; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}