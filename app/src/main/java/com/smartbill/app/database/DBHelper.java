package com.smartbill.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.smartbill.app.model.CartItem;
import com.smartbill.app.model.Product;
import com.smartbill.app.model.BillSummary;
import com.smartbill.app.model.DailySales;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.smartbill.app.model.BillSummary;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smartbill.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_PRODUCTS = "products";

    private static final String COL_ID = "product_id";
    private static final String COL_NAME = "product_name";
    private static final String COL_PRICE = "price";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createProductsTable =
                "CREATE TABLE products (" +
                        "product_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "product_name TEXT NOT NULL, " +
                        "price TEXT NOT NULL, " +
                        "stock_quantity INTEGER NOT NULL DEFAULT 0, " +
                        "low_stock_threshold INTEGER NOT NULL DEFAULT 5" +
                        ")";

        db.execSQL(createProductsTable);

        createBillTables(db);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        if (oldVersion < 2) {
            createBillTables(db);
        }

        if (oldVersion < 3) {
            db.execSQL(
                    "ALTER TABLE products ADD COLUMN " +
                            "stock_quantity INTEGER NOT NULL DEFAULT 0"
            );

            db.execSQL(
                    "ALTER TABLE products ADD COLUMN " +
                            "low_stock_threshold INTEGER NOT NULL DEFAULT 5"
            );
        }
    }

    public long addProduct(
            String name,
            BigDecimal price,
            int stockQuantity,
            int lowStockThreshold
    ) {
        if (name == null || name.trim().isEmpty()
                || price == null
                || price.compareTo(BigDecimal.ZERO) <= 0
                || stockQuantity < 0
                || lowStockThreshold < 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_NAME, name.trim());
        values.put(COL_PRICE, price.toPlainString());
        values.put("stock_quantity", stockQuantity);
        values.put("low_stock_threshold", lowStockThreshold);

        return db.insert(TABLE_PRODUCTS, null, values);
    }
    public long addProduct(String name, BigDecimal price) {
        return addProduct(name, price, 0, 5);
    }

    public List<Product> getAllProducts() {

        List<Product> products = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_PRODUCTS,
                null,
                null,
                null,
                null,
                null,
                COL_ID + " DESC"
        )) {

            int idIndex = cursor.getColumnIndexOrThrow(COL_ID);
            int nameIndex = cursor.getColumnIndexOrThrow(COL_NAME);
            int priceIndex = cursor.getColumnIndexOrThrow(COL_PRICE);

            int stockIndex =
                    cursor.getColumnIndexOrThrow("stock_quantity");

            int thresholdIndex =
                    cursor.getColumnIndexOrThrow("low_stock_threshold");

            while (cursor.moveToNext()) {

                int id = cursor.getInt(idIndex);
                String name = cursor.getString(nameIndex);

                BigDecimal price = new BigDecimal(
                        cursor.getString(priceIndex)
                );

                int stockQuantity = cursor.getInt(stockIndex);
                int lowStockThreshold = cursor.getInt(thresholdIndex);

                products.add(
                        new Product(
                                id,
                                name,
                                price,
                                stockQuantity,
                                lowStockThreshold
                        )
                );
            }
        }

        return products;
    }
    public boolean deleteProduct(int productId) {

        SQLiteDatabase db = getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_PRODUCTS,
                COL_ID + " = ?",
                new String[]{String.valueOf(productId)}
        );

        return rowsDeleted > 0;
    }

    private void createBillTables(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS bills (" +
                        "bill_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "total_amount TEXT NOT NULL, " +
                        "payment_method TEXT NOT NULL, " +
                        "payment_status TEXT NOT NULL, " +
                        "created_at TEXT NOT NULL)"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS bill_items (" +
                        "bill_item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "bill_id INTEGER NOT NULL, " +
                        "product_name TEXT NOT NULL, " +
                        "quantity INTEGER NOT NULL, " +
                        "unit_price TEXT NOT NULL, " +
                        "subtotal TEXT NOT NULL, " +
                        "FOREIGN KEY(bill_id) REFERENCES bills(bill_id))"
        );
    }

    public long saveCashBill(
            List<CartItem> cartItems,
            BigDecimal totalAmount) {

        if (cartItems == null || cartItems.isEmpty()
                || totalAmount == null
                || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        long billId = -1;

        db.beginTransaction();

        try {
            // Step 1: Validate and deduct stock.
            // Conditional updates prevent selling unavailable stock.
            for (CartItem item : cartItems) {

                if (item == null
                        || item.getProduct() == null
                        || item.getQuantity() <= 0) {
                    throw new android.database.SQLException(
                            "Invalid cart item"
                    );
                }

                int quantity = item.getQuantity();
                int productId = item.getProduct().getProductId();

                android.database.sqlite.SQLiteStatement statement =
                        db.compileStatement(
                                "UPDATE products "
                                        + "SET stock_quantity = stock_quantity - ? "
                                        + "WHERE product_id = ? AND stock_quantity >= ?"
                        );

                statement.bindLong(1, quantity);
                statement.bindLong(2, productId);
                statement.bindLong(3, quantity);

                int updated = statement.executeUpdateDelete();
                statement.close();

                if (updated != 1) {
                    throw new android.database.SQLException(
                            "Insufficient stock for "
                                    + item.getProduct().getProductName()
                    );
                }
            }

            // Step 2: Save the bill.
            ContentValues billValues = new ContentValues();

            billValues.put("total_amount", totalAmount.toPlainString());
            billValues.put("payment_method", "CASH");
            billValues.put("payment_status", "PAID");

            String dateTime = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date());

            billValues.put("created_at", dateTime);

            billId = db.insert("bills", null, billValues);

            if (billId == -1) {
                throw new android.database.SQLException(
                        "Could not save bill"
                );
            }

            // Step 3: Save bill items.
            for (CartItem item : cartItems) {

                ContentValues itemValues = new ContentValues();

                itemValues.put("bill_id", billId);
                itemValues.put(
                        "product_name",
                        item.getProduct().getProductName()
                );
                itemValues.put("quantity", item.getQuantity());
                itemValues.put(
                        "unit_price",
                        item.getProduct().getPrice().toPlainString()
                );
                itemValues.put(
                        "subtotal",
                        item.getSubtotal().toPlainString()
                );

                long itemId = db.insert(
                        "bill_items",
                        null,
                        itemValues
                );

                if (itemId == -1) {
                    throw new android.database.SQLException(
                            "Could not save bill item"
                    );
                }
            }

            db.setTransactionSuccessful();

        } catch (android.database.SQLException e) {
            android.util.Log.e(
                    "SmartBill",
                    "Cash bill transaction failed",
                    e
            );
            billId = -1;

        } finally {
            db.endTransaction();
        }

        return billId;
    }

    public long savePendingUpiBill(
            List<CartItem> cartItems,
            BigDecimal totalAmount) {

        if (cartItems == null || cartItems.isEmpty()
                || totalAmount == null
                || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        long billId = -1;

        db.beginTransaction();

        try {
            // Step 1: Reserve stock for every product.
            // If any product has insufficient stock, roll back everything.
            for (CartItem item : cartItems) {

                if (item == null
                        || item.getProduct() == null
                        || item.getQuantity() <= 0) {
                    throw new android.database.SQLException(
                            "Invalid cart item"
                    );
                }

                int productId = item.getProduct().getProductId();
                int quantity = item.getQuantity();

                android.database.sqlite.SQLiteStatement statement =
                        db.compileStatement(
                                "UPDATE products "
                                        + "SET stock_quantity = stock_quantity - ? "
                                        + "WHERE product_id = ? "
                                        + "AND stock_quantity >= ?"
                        );

                statement.bindLong(1, quantity);
                statement.bindLong(2, productId);
                statement.bindLong(3, quantity);

                int updated;

                try {
                    updated = statement.executeUpdateDelete();
                } finally {
                    statement.close();
                }

                if (updated != 1) {
                    throw new android.database.SQLException(
                            "Insufficient stock for "
                                    + item.getProduct().getProductName()
                    );
                }
            }

            // Step 2: Create the pending UPI bill.
            ContentValues billValues = new ContentValues();

            billValues.put(
                    "total_amount",
                    totalAmount.toPlainString()
            );

            billValues.put("payment_method", "UPI");
            billValues.put("payment_status", "PENDING");

            String dateTime = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date());

            billValues.put("created_at", dateTime);

            billId = db.insert("bills", null, billValues);

            if (billId == -1) {
                throw new android.database.SQLException(
                        "Could not save UPI bill"
                );
            }

            // Step 3: Save bill items.
            for (CartItem item : cartItems) {

                ContentValues itemValues = new ContentValues();

                itemValues.put("bill_id", billId);

                itemValues.put(
                        "product_name",
                        item.getProduct().getProductName()
                );

                itemValues.put(
                        "quantity",
                        item.getQuantity()
                );

                itemValues.put(
                        "unit_price",
                        item.getProduct()
                                .getPrice()
                                .toPlainString()
                );

                itemValues.put(
                        "subtotal",
                        item.getSubtotal().toPlainString()
                );

                long itemId = db.insert(
                        "bill_items",
                        null,
                        itemValues
                );

                if (itemId == -1) {
                    throw new android.database.SQLException(
                            "Could not save UPI bill item"
                    );
                }
            }

            db.setTransactionSuccessful();

        } catch (android.database.SQLException e) {

            android.util.Log.e(
                    "SmartBill",
                    "Could not create pending UPI bill",
                    e
            );

            billId = -1;

        } finally {
            db.endTransaction();
        }

        return billId;
    }

    public boolean confirmUpiPayment(long billId) {

        if (billId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("payment_status", "PAID");

        int rowsUpdated = db.update(
                "bills",
                values,
                "bill_id = ? "
                        + "AND payment_method = ? "
                        + "AND payment_status = ?",
                new String[]{
                        String.valueOf(billId),
                        "UPI",
                        "PENDING"
                }
        );

        return rowsUpdated == 1;
    }

    public String getReceiptText(long billId) {

        SQLiteDatabase db = getReadableDatabase();

        StringBuilder receipt = new StringBuilder();

// Load shop details saved in Settings
        SharedPreferences preferences =
                context.getSharedPreferences(
                        "smartbill_settings",
                        Context.MODE_PRIVATE
                );

        String shopName = preferences.getString("shop_name", "My Shop");
        String ownerName = preferences.getString("owner_name", "");
        String shopPhone = preferences.getString("shop_phone", "");
        String shopAddress = preferences.getString("shop_address", "");

        try (Cursor billCursor = db.query(
                "bills",
                null,
                "bill_id = ?",
                new String[]{String.valueOf(billId)},
                null,
                null,
                null
        )) {

            if (!billCursor.moveToFirst()) {
                return null;
            }

            String total = billCursor.getString(
                    billCursor.getColumnIndexOrThrow("total_amount")
            );

            String paymentMethod = billCursor.getString(
                    billCursor.getColumnIndexOrThrow("payment_method")
            );

            String paymentStatus = billCursor.getString(
                    billCursor.getColumnIndexOrThrow("payment_status")
            );

            String createdAt = billCursor.getString(
                    billCursor.getColumnIndexOrThrow("created_at")
            );

            // Shop header
            receipt.append("================================\n");
            receipt.append(shopName).append("\n");

            if (!ownerName.trim().isEmpty()) {
                receipt.append("Owner: ")
                        .append(ownerName)
                        .append("\n");
            }

            if (!shopAddress.trim().isEmpty()) {
                receipt.append(shopAddress).append("\n");
            }

            if (!shopPhone.trim().isEmpty()) {
                receipt.append("Phone: ")
                        .append(shopPhone)
                        .append("\n");
            }

            receipt.append("================================\n\n");

            // Bill information
            receipt.append("           BILL RECEIPT\n\n");

            receipt.append("Bill No: SB")
                    .append(billId)
                    .append("\n");

            receipt.append("Date: ")
                    .append(createdAt)
                    .append("\n\n");

            receipt.append("--------------------------------\n");

            // Bill items
            try (Cursor itemCursor = db.query(
                    "bill_items",
                    null,
                    "bill_id = ?",
                    new String[]{String.valueOf(billId)},
                    null,
                    null,
                    "bill_item_id ASC"
            )) {

                while (itemCursor.moveToNext()) {

                    String name = itemCursor.getString(
                            itemCursor.getColumnIndexOrThrow("product_name")
                    );

                    int quantity = itemCursor.getInt(
                            itemCursor.getColumnIndexOrThrow("quantity")
                    );

                    String price = itemCursor.getString(
                            itemCursor.getColumnIndexOrThrow("unit_price")
                    );

                    String subtotal = itemCursor.getString(
                            itemCursor.getColumnIndexOrThrow("subtotal")
                    );

                    receipt.append(name).append("\n");

                    receipt.append("Qty: ")
                            .append(quantity)
                            .append(" | Unit price: ₹")
                            .append(price)
                            .append(" | Total: ₹")
                            .append(subtotal)
                            .append("\n\n");
                }
            }

            receipt.append("--------------------------------\n");

            receipt.append("TOTAL AMOUNT: ₹")
                    .append(total)
                    .append("\n\n");

            receipt.append("Payment Method: ")
                    .append(paymentMethod)
                    .append("\n");

            receipt.append("Payment Status: ")
                    .append(paymentStatus)
                    .append("\n\n");

            receipt.append("Thank you for shopping!\n");
            receipt.append("================================");

        }

        return receipt.toString();
    }
    private final Context context;

    // Get today's date in yyyy-MM-dd format.
    private String getTodayDate() {
        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());
    }

    // Get the total value of bills marked PAID today.
    public BigDecimal getTodaySales() {

        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT COALESCE(SUM(CAST(total_amount AS REAL)), 0) " +
                        "FROM bills " +
                        "WHERE payment_status = 'PAID' " +
                        "AND date(created_at) = date(?)";

        try (Cursor cursor = db.rawQuery(
                sql,
                new String[]{getTodayDate()}
        )) {
            if (cursor.moveToFirst()) {
                return BigDecimal.valueOf(cursor.getDouble(0));
            }
        }

        return BigDecimal.ZERO;
    }

    // Count bills created today.
    public int getTodayBillCount() {

        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT COUNT(*) FROM bills " +
                        "WHERE date(created_at) = date(?)";

        try (Cursor cursor = db.rawQuery(
                sql,
                new String[]{getTodayDate()}
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    // Get today's paid cash sales.
    public BigDecimal getTodayCashSales() {

        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT COALESCE(SUM(CAST(total_amount AS REAL)), 0) " +
                        "FROM bills " +
                        "WHERE payment_method = 'CASH' " +
                        "AND payment_status = 'PAID' " +
                        "AND date(created_at) = date(?)";

        try (Cursor cursor = db.rawQuery(
                sql,
                new String[]{getTodayDate()}
        )) {
            if (cursor.moveToFirst()) {
                return BigDecimal.valueOf(cursor.getDouble(0));
            }
        }

        return BigDecimal.ZERO;
    }

    // Get today's paid UPI sales.
    public BigDecimal getTodayUpiSales() {

        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT COALESCE(SUM(CAST(total_amount AS REAL)), 0) " +
                        "FROM bills " +
                        "WHERE payment_method = 'UPI' " +
                        "AND payment_status = 'PAID' " +
                        "AND date(created_at) = date(?)";

        try (Cursor cursor = db.rawQuery(
                sql,
                new String[]{getTodayDate()}
        )) {
            if (cursor.moveToFirst()) {
                return BigDecimal.valueOf(cursor.getDouble(0));
            }
        }

        return BigDecimal.ZERO;
    }

    // Count UPI bills that are still pending.
    public int getPendingUpiBillCount() {

        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT COUNT(*) FROM bills " +
                        "WHERE payment_method = 'UPI' " +
                        "AND payment_status = 'PENDING'";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    // Count all products.
    public int getTotalProductCount() {

        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT COUNT(*) FROM products";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    // Get the current stock quantity for a product.
    public int getStockQuantity(int productId) {

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_PRODUCTS,
                new String[]{"stock_quantity"},
                COL_ID + " = ?",
                new String[]{String.valueOf(productId)},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return -1;
    }


    // Set stock to an exact quantity.
    public boolean setStockQuantity(
            int productId,
            int quantity
    ) {
        if (quantity < 0) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("stock_quantity", quantity);

        SQLiteDatabase db = getWritableDatabase();

        return db.update(
                TABLE_PRODUCTS,
                values,
                COL_ID + " = ?",
                new String[]{String.valueOf(productId)}
        ) == 1;
    }


    // Add newly received stock.
    public boolean addStock(
            int productId,
            int quantityToAdd
    ) {
        if (quantityToAdd <= 0) {
            return false;
        }

        int currentStock = getStockQuantity(productId);

        if (currentStock < 0) {
            return false;
        }

        if (currentStock > Integer.MAX_VALUE - quantityToAdd) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("stock_quantity", currentStock + quantityToAdd);

        SQLiteDatabase db = getWritableDatabase();

        return db.update(
                TABLE_PRODUCTS,
                values,
                COL_ID + " = ?",
                new String[]{String.valueOf(productId)}
        ) == 1;
    }


    // Return products that have reached their low-stock threshold.
    public List<Product> getLowStockProducts() {

        List<Product> lowStockProducts = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_PRODUCTS,
                null,
                "stock_quantity <= low_stock_threshold",
                null,
                null,
                null,
                COL_NAME + " ASC"
        )) {

            int idIndex = cursor.getColumnIndexOrThrow(COL_ID);
            int nameIndex = cursor.getColumnIndexOrThrow(COL_NAME);
            int priceIndex = cursor.getColumnIndexOrThrow(COL_PRICE);

            int stockIndex =
                    cursor.getColumnIndexOrThrow("stock_quantity");

            int thresholdIndex =
                    cursor.getColumnIndexOrThrow("low_stock_threshold");

            while (cursor.moveToNext()) {

                lowStockProducts.add(
                        new Product(
                                cursor.getInt(idIndex),
                                cursor.getString(nameIndex),
                                new BigDecimal(cursor.getString(priceIndex)),
                                cursor.getInt(stockIndex),
                                cursor.getInt(thresholdIndex)
                        )
                );
            }
        }

        return lowStockProducts;
    }

    public List<BillSummary> getAllBills() {

        List<BillSummary> bills = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT bill_id, total_amount, payment_method, " +
                        "payment_status, created_at " +
                        "FROM bills ORDER BY bill_id DESC",
                null
        );

        try {
            while (cursor.moveToNext()) {

                long billId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("bill_id")
                );

                String totalAmount = cursor.getString(
                        cursor.getColumnIndexOrThrow("total_amount")
                );

                String paymentMethod = cursor.getString(
                        cursor.getColumnIndexOrThrow("payment_method")
                );

                String paymentStatus = cursor.getString(
                        cursor.getColumnIndexOrThrow("payment_status")
                );

                String createdAt = cursor.getString(
                        cursor.getColumnIndexOrThrow("created_at")
                );

                BillSummary bill = new BillSummary(
                        billId,
                        totalAmount,
                        paymentMethod,
                        paymentStatus,
                        createdAt
                );

                bills.add(bill);
            }
        } finally {
            cursor.close();
        }

        return bills;
    }

    // Retrieve all UPI bills awaiting payment confirmation.
    public List<BillSummary> getPendingUpiBills() {

        List<BillSummary> pendingBills = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String query =
                "SELECT bill_id, total_amount, payment_method, " +
                        "payment_status, created_at " +
                        "FROM bills " +
                        "WHERE payment_method = 'UPI' " +
                        "AND payment_status = 'PENDING' " +
                        "ORDER BY bill_id DESC";

        try (Cursor cursor = db.rawQuery(query, null)) {

            while (cursor.moveToNext()) {

                long billId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("bill_id")
                );

                String totalAmount = cursor.getString(
                        cursor.getColumnIndexOrThrow("total_amount")
                );

                String paymentMethod = cursor.getString(
                        cursor.getColumnIndexOrThrow("payment_method")
                );

                String paymentStatus = cursor.getString(
                        cursor.getColumnIndexOrThrow("payment_status")
                );

                String createdAt = cursor.getString(
                        cursor.getColumnIndexOrThrow("created_at")
                );

                BillSummary bill = new BillSummary(
                        billId,
                        totalAmount,
                        paymentMethod,
                        paymentStatus,
                        createdAt
                );

                pendingBills.add(bill);
            }
        }

        return pendingBills;
    }

    public boolean cancelPendingUpiBill(long billId) {

        if (billId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();

        try {
            // Only cancel an existing pending UPI bill.
            Cursor billCursor = db.rawQuery(
                    "SELECT bill_id FROM bills " +
                            "WHERE bill_id = ? " +
                            "AND payment_method = 'UPI' " +
                            "AND payment_status = 'PENDING'",
                    new String[]{String.valueOf(billId)}
            );

            boolean pendingBillExists;

            try {
                pendingBillExists = billCursor.moveToFirst();
            } finally {
                billCursor.close();
            }

            if (!pendingBillExists) {
                return false;
            }

            // Read purchased quantities for this bill.
            Cursor itemCursor = db.rawQuery(
                    "SELECT product_name, quantity " +
                            "FROM bill_items WHERE bill_id = ?",
                    new String[]{String.valueOf(billId)}
            );

            try {
                while (itemCursor.moveToNext()) {

                    String productName =
                            itemCursor.getString(
                                    itemCursor.getColumnIndexOrThrow(
                                            "product_name"
                                    )
                            );

                    int quantity =
                            itemCursor.getInt(
                                    itemCursor.getColumnIndexOrThrow(
                                            "quantity"
                                    )
                            );

                    if (quantity <= 0) {
                        throw new android.database.SQLException(
                                "Invalid quantity in bill"
                        );
                    }

                    // Restore stock using the product name.
                    // This assumes product names are unique.
                    ContentValues stockValues = new ContentValues();

                    db.execSQL(
                            "UPDATE products " +
                                    "SET stock_quantity = stock_quantity + ? " +
                                    "WHERE product_name = ?",
                            new Object[]{quantity, productName}
                    );
                }
            } finally {
                itemCursor.close();
            }

            // Mark the bill cancelled only if it is still pending.
            ContentValues billValues = new ContentValues();
            billValues.put("payment_status", "CANCELLED");

            int updated = db.update(
                    "bills",
                    billValues,
                    "bill_id = ? AND payment_method = ? AND payment_status = ?",
                    new String[]{
                            String.valueOf(billId),
                            "UPI",
                            "PENDING"
                    }
            );

            if (updated != 1) {
                throw new android.database.SQLException(
                        "Could not cancel UPI bill"
                );
            }

            db.setTransactionSuccessful();
            return true;

        } catch (Exception e) {

            android.util.Log.e(
                    "SmartBill",
                    "Could not cancel pending UPI bill",
                    e
            );

            return false;

        } finally {
            db.endTransaction();
        }
    }

    public BigDecimal getWeeklySales() {

        SQLiteDatabase db = getReadableDatabase();

        String query =
                "SELECT COALESCE(SUM(CAST(total_amount AS REAL)), 0) " +
                        "FROM bills " +
                        "WHERE payment_status = 'PAID' " +
                        "AND date(created_at) >= date('now', 'localtime', '-6 days') " +
                        "AND date(created_at) <= date('now', 'localtime')";

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                return BigDecimal.valueOf(cursor.getDouble(0));
            }
        } finally {
            cursor.close();
        }

        return BigDecimal.ZERO;
    }

    public BigDecimal getMonthlySales() {

        SQLiteDatabase db = getReadableDatabase();

        String query =
                "SELECT COALESCE(SUM(CAST(total_amount AS REAL)), 0) " +
                        "FROM bills " +
                        "WHERE payment_status = 'PAID' " +
                        "AND strftime('%Y-%m', created_at) = " +
                        "strftime('%Y-%m', 'now', 'localtime')";

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                return BigDecimal.valueOf(cursor.getDouble(0));
            }
        } finally {
            cursor.close();
        }

        return BigDecimal.ZERO;
    }

    public List<DailySales> getLastSevenDaysSales() {

        List<DailySales> dailySalesList = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "WITH RECURSIVE dates(day, remaining) AS (" +
                        "SELECT date('now', 'localtime'), 6 " +
                        "UNION ALL " +
                        "SELECT date(day, '-1 day'), remaining - 1 " +
                        "FROM dates WHERE remaining > 0" +
                        ") " +
                        "SELECT dates.day, " +
                        "COALESCE(SUM(CAST(bills.total_amount AS REAL)), 0) " +
                        "FROM dates " +
                        "LEFT JOIN bills ON date(bills.created_at) = dates.day " +
                        "AND bills.payment_status = 'PAID' " +
                        "GROUP BY dates.day " +
                        "ORDER BY dates.day ASC",
                null
        );

        try {
            while (cursor.moveToNext()) {

                String date = cursor.getString(0);

                BigDecimal totalSales = BigDecimal.valueOf(
                        cursor.getDouble(1)
                ).setScale(2, java.math.RoundingMode.HALF_UP);

                dailySalesList.add(
                        new DailySales(date, totalSales)
                );
            }
        } finally {
            cursor.close();
        }

        return dailySalesList;
    }
}

