package com.smartbill.app.model;

import java.math.BigDecimal;

public class Product {

    private int productId;
    private String productName;
    private BigDecimal price;

    private int stockQuantity;
    private int lowStockThreshold;

    // Existing constructor — keeps current code compatible.
    public Product(
            int productId,
            String productName,
            BigDecimal price
    ) {
        this(
                productId,
                productName,
                price,
                0,
                5
        );
    }

    // New constructor with inventory information.
    public Product(
            int productId,
            String productName,
            BigDecimal price,
            int stockQuantity,
            int lowStockThreshold
    ) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.lowStockThreshold = lowStockThreshold;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold;
    }

    @Override
    public String toString() {
        return productName
                + " - ₹" + price.toPlainString()
                + " | Stock: " + stockQuantity;
    }
}