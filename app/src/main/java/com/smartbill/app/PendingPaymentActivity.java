package com.smartbill.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.smartbill.app.database.DBHelper;
import com.smartbill.app.model.BillSummary;

import java.math.BigDecimal;
import java.util.List;

public class PendingPaymentActivity extends Activity {

    private DBHelper dbHelper;
    private LinearLayout pendingBillsContainer;
    private TextView txtEmpty;
    private TextView txtPendingTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLayout();

        dbHelper = new DBHelper(this);

        loadPendingPayments();
    }

    private void createLayout() {

        // Main screen
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 16);
        root.setBackgroundColor(0xFFF5F7FA);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(20, 22, 20, 22);
        header.setBackgroundColor(0xFF173F73);

        TextView title = new TextView(this);
        title.setText("Pending Payments");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(0xFFFFFFFF);

        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Manage and verify your UPI transactions");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFFE0EAF5);
        subtitle.setPadding(0, 6, 0, 0);

        header.addView(subtitle);

        LinearLayout.LayoutParams headerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        headerParams.setMargins(0, 0, 0, 18);
        root.addView(header, headerParams);

        // Pending amount summary card
        LinearLayout summaryCard = new LinearLayout(this);
        summaryCard.setOrientation(LinearLayout.VERTICAL);
        summaryCard.setPadding(18, 18, 18, 18);
        summaryCard.setBackgroundColor(0xFFFFFFFF);

        TextView summaryLabel = new TextView(this);
        summaryLabel.setText("TOTAL AWAITING CONFIRMATION");
        summaryLabel.setTextSize(12);
        summaryLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        summaryLabel.setTextColor(0xFF667085);

        summaryCard.addView(summaryLabel);

        txtPendingTotal = new TextView(this);
        txtPendingTotal.setTextSize(23);
        txtPendingTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        txtPendingTotal.setTextColor(0xFFF59E0B);
        txtPendingTotal.setPadding(0, 8, 0, 0);

        summaryCard.addView(txtPendingTotal);

        LinearLayout.LayoutParams summaryParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        summaryParams.setMargins(0, 0, 0, 18);
        root.addView(summaryCard, summaryParams);

        // Scrollable pending bills
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);

        pendingBillsContainer = new LinearLayout(this);
        pendingBillsContainer.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(pendingBillsContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        // Empty state
        txtEmpty = new TextView(this);
        txtEmpty.setText("No pending payments.\nYou're all caught up!");
        txtEmpty.setTextSize(16);
        txtEmpty.setTextColor(0xFF667085);
        txtEmpty.setGravity(Gravity.CENTER);
        txtEmpty.setPadding(12, 16, 12, 16);
        txtEmpty.setVisibility(View.GONE);

        root.addView(
                txtEmpty,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        // Refresh button
        Button btnRefresh = new Button(this);
        btnRefresh.setText("Refresh Payments");
        btnRefresh.setTextColor(0xFFFFFFFF);
        btnRefresh.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF173F73)
        );
        btnRefresh.setOnClickListener(v -> loadPendingPayments());

        LinearLayout.LayoutParams refreshParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        refreshParams.setMargins(0, 10, 0, 4);
        root.addView(btnRefresh, refreshParams);

        // Back button
        Button btnBack = new Button(this);
        btnBack.setText("Back to Home");
        btnBack.setTextColor(0xFF173F73);
        btnBack.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFE5EDF7)
        );
        btnBack.setOnClickListener(v -> finish());

        root.addView(
                btnBack,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dbHelper != null) {
            loadPendingPayments();
        }
    }

    private void loadPendingPayments() {

        if (dbHelper == null) {
            return;
        }

        pendingBillsContainer.removeAllViews();

        List<BillSummary> bills = dbHelper.getPendingUpiBills();

        BigDecimal totalPending = BigDecimal.ZERO;

        for (BillSummary bill : bills) {
            totalPending = totalPending.add(
                    new BigDecimal(bill.getTotalAmount())
            );

            addPendingBillCard(bill);
        }

        txtPendingTotal.setText(
                "Total awaiting confirmation: ₹" +
                        totalPending.toPlainString()
        );

        txtEmpty.setVisibility(
                bills.isEmpty() ? View.VISIBLE : View.GONE
        );
    }

    private void addPendingBillCard(BillSummary bill) {

        // Main bill card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setElevation(3);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);

        // Bill number
        TextView billTitle = new TextView(this);
        billTitle.setText("Bill No: SB" + bill.getBillId());
        billTitle.setTextSize(19);
        billTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        billTitle.setTextColor(0xFF173F73);
        card.addView(billTitle);

        // Amount
        TextView amount = new TextView(this);
        amount.setText("₹" + bill.getTotalAmount());
        amount.setTextSize(25);
        amount.setTypeface(null, android.graphics.Typeface.BOLD);
        amount.setTextColor(0xFF16803C);

        LinearLayout.LayoutParams amountParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        amountParams.setMargins(0, 12, 0, 6);
        card.addView(amount, amountParams);

        // Bill date
        TextView date = new TextView(this);
        date.setText("Date: " + bill.getCreatedAt());
        date.setTextSize(13);
        date.setTextColor(0xFF667085);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        dateParams.setMargins(0, 0, 0, 12);
        card.addView(date, dateParams);

        // Pending status badge
        TextView status = new TextView(this);
        status.setText("● PENDING");
        status.setTextSize(12);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setTextColor(0xFFB45309);
        status.setBackgroundTintList(null);
        status.setPadding(12, 8, 12, 8);
        status.setBackgroundColor(0xFFFFF4DB);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(0, 0, 0, 16);
        card.addView(status, statusParams);

        // View Bill button
        Button btnView = new Button(this);
        btnView.setText("View Bill");
        btnView.setTextColor(0xFF173F73);
        btnView.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFE5EDF7)
        );

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PendingPaymentActivity.this,
                    ReceiptActivity.class
            );

            intent.putExtra("bill_id", bill.getBillId());
            startActivity(intent);
        });

        card.addView(btnView);

        // Confirm Payment button
        Button btnConfirm = new Button(this);
        btnConfirm.setText("Confirm Payment");
        btnConfirm.setTextColor(0xFFFFFFFF);
        btnConfirm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF16803C)
        );

        btnConfirm.setOnClickListener(v -> {

            new android.app.AlertDialog.Builder(
                    PendingPaymentActivity.this
            )
                    .setTitle("Confirm UPI Payment")
                    .setMessage(
                            "Have you verified that ₹" +
                                    bill.getTotalAmount() +
                                    " was received in your bank account or UPI app?"
                    )
                    .setNegativeButton("Not Yet", null)
                    .setPositiveButton("Confirm Payment", (dialog, which) -> {

                        boolean success = dbHelper.confirmUpiPayment(
                                bill.getBillId()
                        );

                        if (success) {
                            Toast.makeText(
                                    PendingPaymentActivity.this,
                                    "Payment confirmed successfully!",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {
                            Toast.makeText(
                                    PendingPaymentActivity.this,
                                    "Payment could not be confirmed. Refresh and try again.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        loadPendingPayments();
                    })
                    .show();
        });

        card.addView(btnConfirm);

        // Cancel Bill button
        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel Bill");
        btnCancel.setTextColor(0xFFFFFFFF);
        btnCancel.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFDC2626)
        );

        btnCancel.setOnClickListener(v -> {

            new android.app.AlertDialog.Builder(
                    PendingPaymentActivity.this
            )
                    .setTitle("Cancel Pending Bill")
                    .setMessage(
                            "Are you sure you want to cancel bill SB" +
                                    bill.getBillId() +
                                    "? The reserved stock will be restored."
                    )
                    .setNegativeButton("Keep Bill", null)
                    .setPositiveButton("Cancel Bill", (dialog, which) -> {

                        boolean success = dbHelper.cancelPendingUpiBill(
                                bill.getBillId()
                        );

                        if (success) {
                            Toast.makeText(
                                    PendingPaymentActivity.this,
                                    "Bill cancelled and stock restored.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {
                            Toast.makeText(
                                    PendingPaymentActivity.this,
                                    "Could not cancel this bill.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        loadPendingPayments();
                    })
                    .show();
        });

        card.addView(btnCancel);

        // Add completed card to the screen
        pendingBillsContainer.addView(card);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}