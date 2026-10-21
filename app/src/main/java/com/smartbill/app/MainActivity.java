package com.smartbill.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.smartbill.app.database.DBHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MainActivity extends AppCompatActivity {

    private DBHelper dbHelper;

    private TextView txtTodaySales;
    private TextView txtTodayBills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        txtTodaySales = findViewById(R.id.txtTodaySales);
        txtTodayBills = findViewById(R.id.txtTodayBills);

        // New Bill
        Button btnNewBill = findViewById(R.id.btnNewBill);
        btnNewBill.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        BillingActivity.class
                ))
        );

        // Manage Products
        Button btnManageProducts =
                findViewById(R.id.btnManageProducts);

        btnManageProducts.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        ProductActivity.class
                ))
        );

        // Bill History
        Button btnBillHistory =
                findViewById(R.id.btnBillHistory);

        btnBillHistory.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        BillHistoryActivity.class
                ))
        );

        // Pending Payments
        Button btnPendingPayments =
                findViewById(R.id.btnPendingPayments);

        btnPendingPayments.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        PendingPaymentActivity.class
                ))
        );

        // Settings
        Button btnSettings = findViewById(R.id.btnSettings);

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this,
                        SettingsActivity.class
                ))
        );

        // Sales Dashboard
        Button btnSalesDashboard =
                findViewById(R.id.btnSalesDashboard);

        btnSalesDashboard.setOnClickListener(v ->
                openSalesDashboard()
        );

        // View sales button
        Button btnOpenDashboard =
                findViewById(R.id.btnOpenDashboard);

        btnOpenDashboard.setOnClickListener(v ->
                openSalesDashboard()
        );

        loadTodaySummary();
    }

    private void openSalesDashboard() {
        startActivity(new Intent(
                MainActivity.this,
                SalesDashboardActivity.class
        ));
    }

    private void loadTodaySummary() {

        BigDecimal todaySales = dbHelper.getTodaySales();

        txtTodaySales.setText(
                "₹" + todaySales
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString()
        );

        int todayBills = dbHelper.getTodayBillCount();

        txtTodayBills.setText(
                todayBills + (todayBills == 1 ? " bill" : " bills")
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dbHelper != null
                && txtTodaySales != null
                && txtTodayBills != null) {
            loadTodaySummary();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}