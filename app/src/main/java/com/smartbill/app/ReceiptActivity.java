package com.smartbill.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import com.smartbill.app.utils.PdfReceiptGenerator;

import java.io.File;
import java.io.IOException;

import android.content.ActivityNotFoundException;

import com.smartbill.app.database.DBHelper;

import java.math.BigDecimal;

public class ReceiptActivity extends Activity {

    private DBHelper dbHelper;
    private TextView txtReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_receipt);
        Button btnSharePdf = findViewById(R.id.btnSharePdf);

        btnSharePdf.setOnClickListener(v -> {

            long billId = getIntent().getLongExtra(
                    "bill_id",
                    -1
            );

            if (billId == -1) {

                Toast.makeText(
                        this,
                        "Invalid bill",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            sharePdfViaWhatsApp(billId);
        });

        Button btnShareWhatsApp = findViewById(R.id.btnShareWhatsApp);
        btnShareWhatsApp.setOnClickListener(v -> {

            long billId = getIntent().getLongExtra("bill_id", -1);

            if (billId <= 0) {
                Toast.makeText(
                        this,
                        "Invalid bill",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            sharePdfViaWhatsApp(billId);
        });

        txtReceipt = findViewById(R.id.txtReceipt);

        Button btnNewBill = findViewById(R.id.btnNewBill);

        Button btnGeneratePdf = findViewById(R.id.btnGeneratePdf);

        btnGeneratePdf.setOnClickListener(v -> {

            long billId = getIntent().getLongExtra(
                    "bill_id",
                    -1
            );

            if (billId == -1) {
                Toast.makeText(
                        this,
                        "Invalid bill",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            try {

                File pdfFile = PdfReceiptGenerator.generateReceipt(
                        this,
                        billId
                );

                Toast.makeText(
                        this,
                        "PDF generated: " + pdfFile.getName(),
                        Toast.LENGTH_LONG
                ).show();

            } catch (IOException e) {

                Toast.makeText(
                        this,
                        "Failed to generate PDF: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        dbHelper = new DBHelper(this);

        long billId = getIntent().getLongExtra(
                "bill_id",
                -1
        );

        if (billId == -1) {

            Toast.makeText(
                    this,
                    "Invalid bill",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }

        loadReceipt(billId);

        btnNewBill.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ReceiptActivity.this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(intent);

            finish();
        });
    }

    private void loadReceipt(long billId) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder receipt = new StringBuilder();

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

                txtReceipt.setText("Bill not found.");

                return;
            }

            String total = billCursor.getString(
                    billCursor.getColumnIndexOrThrow(
                            "total_amount"
                    )
            );

            String paymentMethod = billCursor.getString(
                    billCursor.getColumnIndexOrThrow(
                            "payment_method"
                    )
            );

            String paymentStatus = billCursor.getString(
                    billCursor.getColumnIndexOrThrow(
                            "payment_status"
                    )
            );

            String createdAt = billCursor.getString(
                    billCursor.getColumnIndexOrThrow(
                            "created_at"
                    )
            );

            receipt.append("Bill No: SB")
                    .append(billId)
                    .append("\n");

            receipt.append("Date: ")
                    .append(createdAt)
                    .append("\n");

            receipt.append("--------------------------------\n");

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
                            itemCursor.getColumnIndexOrThrow(
                                    "product_name"
                            )
                    );

                    int quantity = itemCursor.getInt(
                            itemCursor.getColumnIndexOrThrow(
                                    "quantity"
                            )
                    );

                    String price = itemCursor.getString(
                            itemCursor.getColumnIndexOrThrow(
                                    "unit_price"
                            )
                    );

                    String subtotal = itemCursor.getString(
                            itemCursor.getColumnIndexOrThrow(
                                    "subtotal"
                            )
                    );

                    receipt.append(name)
                            .append("\n");

                    receipt.append("Qty: ")
                            .append(quantity)
                            .append(" x ₹")
                            .append(price)
                            .append(" = ₹")
                            .append(subtotal)
                            .append("\n\n");
                }
            }

            receipt.append("--------------------------------\n");

            receipt.append("TOTAL: ₹")
                    .append(new BigDecimal(total).toPlainString())
                    .append("\n\n");

            receipt.append("Payment: ")
                    .append(paymentMethod)
                    .append("\n");

            receipt.append("Status: ")
                    .append(paymentStatus)
                    .append("\n\n");

            receipt.append("Thank you for shopping!");

            txtReceipt.setText(receipt.toString());
        }
    }
    private void sharePdfViaWhatsApp(long billId) {
        try {
            File pdfFile =
                    PdfReceiptGenerator.generateReceipt(this, billId);

            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");

            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "SmartBill Receipt - SB" + billId
            );
            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Thank you for shopping with us! " +
                            "Please find your receipt attached. " +
                            "Bill No: SB" + billId
            );

            shareIntent.setPackage("com.whatsapp");
            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(shareIntent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    this,
                    "WhatsApp is not installed",
                    Toast.LENGTH_LONG
            ).show();

        } catch (IOException e) {
            Toast.makeText(
                    this,
                    "Unable to generate PDF receipt",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Unable to share receipt: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
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