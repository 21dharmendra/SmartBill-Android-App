package com.smartbill.app.billing;

import com.smartbill.app.model.CartItem;
import com.smartbill.app.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BillingManager {

    private final List<CartItem> cart = new ArrayList<>();

    // Add a product only when stock is available.
    public boolean addProduct(Product product) {

        if (product == null) {
            return false;
        }

        int availableStock = product.getStockQuantity();

        if (availableStock <= 0) {
            return false;
        }

        for (CartItem item : cart) {

            if (item.getProduct().getProductId()
                    == product.getProductId()) {

                if (item.getQuantity() >= availableStock) {
                    return false;
                }

                item.setQuantity(item.getQuantity() + 1);
                return true;
            }
        }

        cart.add(new CartItem(product, 1));
        return true;
    }

    // Increase quantity only if the available stock permits it.
    public boolean increaseQuantity(CartItem item) {

        if (item == null || !cart.contains(item)) {
            return false;
        }

        int availableStock = item.getProduct().getStockQuantity();

        if (item.getQuantity() >= availableStock) {
            return false;
        }

        item.setQuantity(item.getQuantity() + 1);
        return true;
    }

    // Decrease quantity or remove the item.
    public void decreaseQuantity(CartItem item) {

        if (item == null || !cart.contains(item)) {
            return;
        }

        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
        } else {
            cart.remove(item);
        }
    }

    public void removeItem(CartItem item) {
        if (item == null) {
            return;
        }

        cart.remove(item);
    }

    public BigDecimal calculateTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart) {
            total = total.add(item.getSubtotal());
        }

        return total;
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cart);
    }

    public boolean isCartEmpty() {
        return cart.isEmpty();
    }

    public void clearCart() {
        cart.clear();
    }
}