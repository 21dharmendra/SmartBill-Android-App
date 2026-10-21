package com.smartbill.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.smartbill.app.database.DBHelper;
import com.smartbill.app.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends Activity {

    private EditText etProductName;
    private EditText etProductPrice;
    private EditText etProductStock;
    private EditText etLowStockThreshold;

    private ListView listProducts;

    private DBHelper dbHelper;

    private final List<Product> products = new ArrayList<>();

    private ArrayAdapter<Product> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_products);

        etProductName = findViewById(R.id.etProductName);
        etProductPrice = findViewById(R.id.etProductPrice);
        etProductStock = findViewById(R.id.etProductStock);
        etLowStockThreshold =
                findViewById(R.id.etLowStockThreshold);

        Button btnAddProduct = findViewById(R.id.btnAddProduct);

        listProducts = findViewById(R.id.listProducts);

        dbHelper = new DBHelper(this);

        setupProductList();

        btnAddProduct.setOnClickListener(v -> addProduct());

        listProducts.setOnItemClickListener(
                (parent, view, position, id) ->
                        showRestockDialog(products.get(position))
        );

        listProducts.setOnItemLongClickListener(
                (parent, view, position, id) -> {

                    Product product = products.get(position);

                    new AlertDialog.Builder(this)
                            .setTitle("Delete Product")
                            .setMessage(
                                    "Delete " +
                                            product.getProductName() +
                                            "?"
                            )
                            .setPositiveButton(
                                    "Delete",
                                    (dialog, which) -> {

                                        boolean deleted =
                                                dbHelper.deleteProduct(
                                                        product.getProductId()
                                                );

                                        if (deleted) {
                                            showMessage("Product deleted");
                                            loadProducts();
                                        } else {
                                            showMessage(
                                                    "Could not delete product"
                                            );
                                        }
                                    }
                            )
                            .setNegativeButton("Cancel", null)
                            .show();

                    return true;
                }
        );

        loadProducts();
    }

    private void setupProductList() {

        adapter = new ArrayAdapter<Product>(
                this,
                android.R.layout.simple_list_item_1,
                products
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {

                TextView textView = (TextView) super.getView(
                        position,
                        convertView,
                        parent
                );

                Product product = getItem(position);

                if (product != null) {

                    String stockStatus = product.isLowStock()
                            ? "LOW STOCK"
                            : "IN STOCK";

                    textView.setText(
                            product.getProductName()
                                    + "\nPrice: ₹"
                                    + product.getPrice().toPlainString()
                                    + "\nAvailable: "
                                    + product.getStockQuantity()
                                    + " units"
                                    + "\n"
                                    + stockStatus
                                    + "\nTap to restock | Long press to delete"
                    );

                    textView.setTextColor(
                            product.isLowStock()
                                    ? 0xFFB45309
                                    : 0xFF263449
                    );
                }

                textView.setPadding(16, 20, 16, 20);

                return textView;
            }
        };

        listProducts.setAdapter(adapter);
    }

    private void addProduct() {

        String name = etProductName.getText()
                .toString()
                .trim();

        String priceText = etProductPrice.getText()
                .toString()
                .trim();

        String stockText = etProductStock.getText()
                .toString()
                .trim();

        String thresholdText = etLowStockThreshold.getText()
                .toString()
                .trim();

        if (name.isEmpty()) {
            etProductName.setError("Enter product name");
            return;
        }

        if (priceText.isEmpty()) {
            etProductPrice.setError("Enter product price");
            return;
        }

        if (stockText.isEmpty()) {
            etProductStock.setError("Enter opening stock");
            return;
        }

        if (thresholdText.isEmpty()) {
            etLowStockThreshold.setError(
                    "Enter low-stock threshold"
            );
            return;
        }

        BigDecimal price;

        int stock;
        int threshold;

        try {
            price = new BigDecimal(priceText);

            stock = Integer.parseInt(stockText);

            threshold = Integer.parseInt(thresholdText);

        } catch (NumberFormatException e) {
            showMessage("Enter valid numeric values");
            return;
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            etProductPrice.setError(
                    "Price must be greater than zero"
            );
            return;
        }

        if (stock < 0) {
            etProductStock.setError(
                    "Stock cannot be negative"
            );
            return;
        }

        if (threshold < 0) {
            etLowStockThreshold.setError(
                    "Threshold cannot be negative"
            );
            return;
        }

        long result = dbHelper.addProduct(
                name,
                price,
                stock,
                threshold
        );

        if (result != -1) {

            showMessage("Product added successfully");

            etProductName.setText("");
            etProductPrice.setText("");
            etProductStock.setText("");
            etLowStockThreshold.setText("");

            loadProducts();

        } else {
            showMessage("Failed to add product");
        }
    }

    private void showRestockDialog(Product product) {

        EditText input = new EditText(this);

        input.setHint("Quantity to add");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(48, 16, 48, 8);
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Restock " + product.getProductName())
                .setMessage(
                        "Current stock: "
                                + product.getStockQuantity()
                                + "\nEnter the quantity to add:"
                )
                .setView(layout)
                .setPositiveButton(
                        "Add Stock",
                        (dialog, which) -> {

                            String value = input.getText()
                                    .toString()
                                    .trim();

                            if (value.isEmpty()) {
                                showMessage(
                                        "Enter a quantity"
                                );
                                return;
                            }

                            int quantity;

                            try {
                                quantity = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                showMessage(
                                        "Enter a valid quantity"
                                );
                                return;
                            }

                            if (quantity <= 0) {
                                showMessage(
                                        "Quantity must be greater than zero"
                                );
                                return;
                            }

                            boolean updated = dbHelper.addStock(
                                    product.getProductId(),
                                    quantity
                            );

                            if (updated) {
                                showMessage("Stock updated successfully");
                                loadProducts();
                            } else {
                                showMessage("Could not update stock");
                            }
                        }
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProducts() {

        products.clear();

        products.addAll(dbHelper.getAllProducts());

        adapter.notifyDataSetChanged();
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