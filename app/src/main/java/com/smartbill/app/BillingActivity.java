package com.smartbill.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.smartbill.app.billing.BillingManager;
import com.smartbill.app.database.DBHelper;
import com.smartbill.app.model.CartItem;
import com.smartbill.app.model.Product;
import android.widget.EditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BillingActivity extends Activity {

    private DBHelper dbHelper;
    private BillingManager billingManager;

    private ListView listAvailableProducts;
    private ListView listCartItems;
    private TextView txtTotal;
    private EditText etSearchProduct;

    private final List<Product> availableProducts =
            new ArrayList<>();

    private final List<CartItem> cartItems =
            new ArrayList<>();

    private ArrayAdapter<Product> productAdapter;
    private ArrayAdapter<CartItem> cartAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_billing);

        dbHelper = new DBHelper(this);
        billingManager = new BillingManager();

        listAvailableProducts =
                findViewById(R.id.listAvailableProducts);

        listCartItems =
                findViewById(R.id.listCartItems);

        txtTotal = findViewById(R.id.txtTotal);

        etSearchProduct = findViewById(R.id.etSearchProduct);

        Button btnGenerateQR =
                findViewById(R.id.btnGenerateQR);

        Button btnCashPayment =
                findViewById(R.id.btnCashPayment);

        setupAvailableProducts();
        setupCart();

        loadProducts();
        updateCart();

        etSearchProduct.addTextChangedListener(new android.text.TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                productAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        listAvailableProducts.setOnItemClickListener(
                (parent, view, position, id) -> {

                    Product product =
                            availableProducts.get(position);

                    boolean added = billingManager.addProduct(product);

                    if (!added) {
                        showMessage(
                                product.getStockQuantity() <= 0
                                        ? "Out of stock!"
                                        : "Maximum available stock reached!"
                        );
                        return;
                    }

                    updateCart();
                }
        );

        btnGenerateQR.setOnClickListener(v -> {

            if (billingManager.isCartEmpty()) {
                showMessage("Please add products first");
                return;
            }

            android.content.SharedPreferences preferences =
                    getSharedPreferences(
                            "smartbill_settings",
                            MODE_PRIVATE
                    );

            String upiId = preferences
                    .getString("shop_upi_id", "")
                    .trim();

            if (upiId.isEmpty()) {
                showMessage("Please add your Shop UPI ID in Settings");
                return;
            }

            BigDecimal total = billingManager.calculateTotal();

            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                showMessage("Invalid bill amount");
                return;
            }

            long billId = dbHelper.savePendingUpiBill(
                    billingManager.getCartItems(),
                    total
            );

            if (billId == -1) {
                showMessage("Failed to create UPI bill");
                return;
            }

            Intent intent = new Intent(
                    BillingActivity.this,
                    UPIPaymentActivity.class
            );

            intent.putExtra("bill_id", billId);
            intent.putExtra("amount", total.toPlainString());

            startActivity(intent);
        });

        btnCashPayment.setOnClickListener(v -> {

            if (billingManager.isCartEmpty()) {
                showMessage("Please add products first");
                return;
            }

            new android.app.AlertDialog.Builder(
                    BillingActivity.this
            )
                    .setTitle("Confirm Cash Payment")
                    .setMessage(
                            "Total amount: ₹" +
                                    billingManager.calculateTotal().toPlainString() +
                                    "\n\nHas the customer paid the full amount in cash?"
                    )
                    .setPositiveButton("Yes, Received", (dialog, which) -> {

                        saveCashBill();

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupAvailableProducts() {

        productAdapter = new ArrayAdapter<Product>(
                this,
                0,
                availableProducts
        ) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View row = convertView;

                if (row == null) {
                    row = getLayoutInflater().inflate(
                            R.layout.item_product,
                            parent,
                            false
                    );
                }

                Product product = getItem(position);

                TextView txtProductName =
                        row.findViewById(R.id.txtProductName);

                TextView txtProductPrice =
                        row.findViewById(R.id.txtProductPrice);

                TextView txtProductStock =
                        row.findViewById(R.id.txtProductStock);

                if (product != null) {

                    txtProductName.setText(product.getProductName());

                    txtProductPrice.setText(
                            "₹" + product.getPrice().toPlainString()
                    );

                    txtProductStock.setText(
                            "Available stock: " + product.getStockQuantity()
                    );
                }

                return row;
            }
        };

        listAvailableProducts.setAdapter(productAdapter);
    }

    private void setupCart() {

        cartAdapter = new ArrayAdapter<CartItem>(
                this, 0, cartItems) {

            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent) {

                View row = convertView;

                if (row == null) {
                    row = getLayoutInflater().inflate(
                            R.layout.item_cart,
                            parent,
                            false
                    );
                }

                CartItem item = getItem(position);

                TextView txtCartProductName =
                        row.findViewById(R.id.txtCartProductName);

                TextView txtCartQuantity =
                        row.findViewById(R.id.txtCartQuantity);

                TextView txtCartSubtotal =
                        row.findViewById(R.id.txtCartSubtotal);

                Button btnIncreaseQty =
                        row.findViewById(R.id.btnIncreaseQty);

                Button btnDecreaseQty =
                        row.findViewById(R.id.btnDecreaseQty);

                Button btnRemoveCartItem =
                        row.findViewById(R.id.btnRemoveCartItem);

                if (item != null) {

                    txtCartProductName.setText(
                            item.getProduct().getProductName()
                    );

                    txtCartQuantity.setText(
                            String.valueOf(item.getQuantity())
                    );

                    txtCartSubtotal.setText(
                            "Subtotal: ₹" + item.getSubtotal().toPlainString()
                    );

                    // Increase quantity
                    btnIncreaseQty.setOnClickListener(v -> {

                        boolean increased =
                                billingManager.increaseQuantity(item);

                        if (!increased) {
                            showMessage(
                                    "Cannot add more. Available stock: "
                                            + item.getProduct().getStockQuantity()
                            );
                            return;
                        }

                        updateCart();
                    });

                    // Decrease quantity
                    btnDecreaseQty.setOnClickListener(v -> {

                        billingManager.decreaseQuantity(item);

                        updateCart();
                    });

                    // Remove item completely
                    btnRemoveCartItem.setOnClickListener(v -> {

                        billingManager.removeItem(item);

                        updateCart();
                    });
                }

                return row;
            }
        };

        listCartItems.setAdapter(cartAdapter);
    }

    private void loadProducts() {

        availableProducts.clear();

        availableProducts.addAll(
                dbHelper.getAllProducts()
        );

        productAdapter.notifyDataSetChanged();
    }

    private void updateCart() {

        cartItems.clear();

        cartItems.addAll(
                billingManager.getCartItems()
        );

        cartAdapter.notifyDataSetChanged();

        BigDecimal total = billingManager.calculateTotal();

        txtTotal.setText(
                "₹" + total.toPlainString()
        );
    }

    private void saveCashBill() {

        long billId = dbHelper.saveCashBill(
                billingManager.getCartItems(),
                billingManager.calculateTotal()
        );

        if (billId == -1) {

            showMessage("Failed to save bill");

            return;
        }

        Intent intent = new Intent(
                BillingActivity.this,
                ReceiptActivity.class
        );

        intent.putExtra("bill_id", billId);

        startActivity(intent);

        finish();
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbHelper != null) {
            dbHelper.close();
        }

    }
}

