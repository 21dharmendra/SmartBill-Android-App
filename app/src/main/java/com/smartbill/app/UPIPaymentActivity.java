package com.smartbill.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.math.BigDecimal;

import com.smartbill.app.database.DBHelper;

public class UPIPaymentActivity extends AppCompatActivity {

    private TextView txtUpiBillNumber;
    private TextView txtUpiAmount;
    private TextView txtUpiId;
    private ImageView imgUpiQrCode;

    private Button btnOpenUpiApp;
    private Button btnConfirmUpiPayment;

    private Button btnCancelUpiBill;

    private String upiId;
    private BigDecimal amount;
    private long billId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upi_payment);

        txtUpiBillNumber = findViewById(R.id.txtUpiBillNumber);
        txtUpiAmount = findViewById(R.id.txtUpiAmount);
        txtUpiId = findViewById(R.id.txtUpiId);
        imgUpiQrCode = findViewById(R.id.imgUpiQrCode);

        btnOpenUpiApp = findViewById(R.id.btnOpenUpiApp);
        btnConfirmUpiPayment = findViewById(R.id.btnConfirmUpiPayment);
        btnCancelUpiBill = findViewById(R.id.btnCancelUpiBill);

        // Get bill details from the previous screen.
        billId = getIntent().getLongExtra("bill_id", -1);

        String amountString = getIntent().getStringExtra("amount");

        if (billId <= 0 || amountString == null) {
            Toast.makeText(
                    this,
                    "Missing bill information",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        try {
            amount = new BigDecimal(amountString);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("Invalid amount");
            }
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Invalid payment amount",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        SharedPreferences preferences =
                getSharedPreferences("smartbill_settings", MODE_PRIVATE);

        upiId = preferences.getString("shop_upi_id", "").trim();

        if (upiId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please save your Shop UPI ID in Settings first",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        txtUpiBillNumber.setText("Bill No: SB" + billId);
        txtUpiAmount.setText("Amount: ₹" + amount.toPlainString());
        txtUpiId.setText("Pay to: " + upiId);

        generateQrCode();

        btnOpenUpiApp.setOnClickListener(v -> openUpiApp());

        btnConfirmUpiPayment.setOnClickListener(
                v -> confirmPaymentManually()
        );
        btnCancelUpiBill.setOnClickListener(
                v -> confirmCancellation()
        );
    }

    private String buildUpiUri() {

        return "upi://pay"
                + "?pa=" + Uri.encode(upiId)
                + "&pn=" + Uri.encode(
                getSharedPreferences(
                        "smartbill_settings",
                        MODE_PRIVATE
                ).getString("shop_name", "Shop"))
                + "&am=" + Uri.encode(amount.toPlainString())
                + "&cu=INR"
                + "&tn=" + Uri.encode("SmartBill SB" + billId);
    }

    private void generateQrCode() {

        try {
            String upiUri = buildUpiUri();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            com.google.zxing.common.BitMatrix bitMatrix =
                    qrCodeWriter.encode(
                            upiUri,
                            BarcodeFormat.QR_CODE,
                            600,
                            600
                    );

            Bitmap bitmap = Bitmap.createBitmap(
                    600,
                    600,
                    Bitmap.Config.ARGB_8888
            );

            for (int x = 0; x < 600; x++) {
                for (int y = 0; y < 600; y++) {
                    bitmap.setPixel(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? android.graphics.Color.BLACK
                                    : android.graphics.Color.WHITE
                    );
                }
            }

            imgUpiQrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            Toast.makeText(
                    this,
                    "Could not generate QR code",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openUpiApp() {

        Uri upiUri = Uri.parse(buildUpiUri());

        Intent intent = new Intent(Intent.ACTION_VIEW, upiUri);

        try {
            startActivity(Intent.createChooser(
                    intent,
                    "Choose UPI Payment App"
            ));
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "No compatible UPI app was found",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void confirmPaymentManually() {

        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setMessage(
                        "Have you checked your bank or merchant app " +
                                "and confirmed that ₹" + amount.toPlainString() +
                                " was actually received?\n\n" +
                                "Only confirm if the payment is visible in your account."
                )
                .setNegativeButton("Not Yet", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Yes, Received", (dialog, which) -> {

                    DBHelper dbHelper = new DBHelper(this);

                    try {
                        boolean updated = dbHelper.confirmUpiPayment(billId);

                        if (!updated) {
                            Toast.makeText(
                                    this,
                                    "Payment could not be confirmed. Please check the bill status.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        Toast.makeText(
                                this,
                                "Payment marked as confirmed",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                UPIPaymentActivity.this,
                                ReceiptActivity.class
                        );

                        intent.putExtra("bill_id", billId);

                        startActivity(intent);
                        finish();

                    } finally {
                        dbHelper.close();
                    }
                })
                .show();

    }

    private void confirmCancellation() {

        new AlertDialog.Builder(this)
                .setTitle("Cancel UPI Bill?")
                .setMessage(
                        "Are you sure you want to cancel bill SB"
                                + billId
                                + "?\n\nThe reserved product stock will be restored."
                )
                .setNegativeButton("Keep Bill", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Cancel Bill", (dialog, which) -> {

                    DBHelper dbHelper = new DBHelper(this);

                    try {
                        boolean cancelled =
                                dbHelper.cancelPendingUpiBill(billId);

                        if (!cancelled) {
                            Toast.makeText(
                                    this,
                                    "Could not cancel bill. It may already be processed.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        Toast.makeText(
                                this,
                                "Bill cancelled and stock restored",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();

                    } finally {
                        dbHelper.close();
                    }
                })
                .show();
    }


}
