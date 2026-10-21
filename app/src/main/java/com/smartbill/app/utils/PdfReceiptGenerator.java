package com.smartbill.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.smartbill.app.database.DBHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfReceiptGenerator {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int LEFT = 40;
    private static final int RIGHT = 555;
    private static final int BOTTOM = 785;

    public static File generateReceipt(
            Context context,
            long billId
    ) throws IOException {

        DBHelper dbHelper = new DBHelper(context);
        PdfDocument document = new PdfDocument();

        File pdfFile = new File(
                new File(context.getFilesDir(), "receipts"),
                "SmartBill_SB" + billId + ".pdf"
        );

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            SharedPreferences settings =
                    context.getSharedPreferences(
                            "smartbill_settings",
                            Context.MODE_PRIVATE
                    );

            String shopName = settings.getString(
                    "shop_name", "Your Shop");

            String ownerName = settings.getString(
                    "owner_name", "");

            String phone = settings.getString(
                    "shop_phone", "");

            String address = settings.getString(
                    "shop_address", "");

            String upiId = settings.getString(
                    "shop_upi_id", "");

            String total;
            String paymentMethod;
            String paymentStatus;
            String createdAt;

            try (Cursor c = db.query(
                    "bills",
                    null,
                    "bill_id = ?",
                    new String[]{String.valueOf(billId)},
                    null,
                    null,
                    null
            )) {
                if (!c.moveToFirst()) {
                    throw new IOException("Bill not found: " + billId);
                }

                total = c.getString(
                        c.getColumnIndexOrThrow("total_amount"));

                paymentMethod = c.getString(
                        c.getColumnIndexOrThrow("payment_method"));

                paymentStatus = c.getString(
                        c.getColumnIndexOrThrow("payment_status"));

                createdAt = c.getString(
                        c.getColumnIndexOrThrow("created_at"));
            }

            PdfWriter writer = new PdfWriter(document);

            writer.startPage();
            writer.drawHeader(shopName);

            int y = 112;

            writer.centered(
                    "INVOICE / RECEIPT",
                    y,
                    16,
                    true,
                    0xFF173F73
            );

            y += 28;

            writer.text("Invoice No: SB" + billId,
                    LEFT, y, 11, true, 0xFF222222);

            writer.rightText("Date: " + formatDate(createdAt),
                    RIGHT, y, 10, false, 0xFF333333);

            y += 25;

            if (!empty(ownerName)) {
                writer.text("Owner: " + ownerName,
                        LEFT, y, 10, false, 0xFF333333);
                y += 17;
            }

            if (!empty(phone)) {
                writer.text("Phone: " + phone,
                        LEFT, y, 10, false, 0xFF333333);
                y += 17;
            }

            if (!empty(address)) {
                for (String line : wrap(address, 70)) {
                    writer.text("Address: " + line,
                            LEFT, y, 10, false, 0xFF333333);
                    y += 17;
                }
            }

            if (!empty(upiId)) {
                writer.text("UPI ID: " + upiId,
                        LEFT, y, 10, false, 0xFF333333);
                y += 17;
            }

            y += 12;
            writer.line(LEFT, y, RIGHT, y);
            y += 25;

            writer.tableHeader(y);
            y += 30;

            try (Cursor items = db.query(
                    "bill_items",
                    null,
                    "bill_id = ?",
                    new String[]{String.valueOf(billId)},
                    null,
                    null,
                    "bill_item_id ASC"
            )) {
                while (items.moveToNext()) {

                    String name = items.getString(
                            items.getColumnIndexOrThrow("product_name"));

                    int quantity = items.getInt(
                            items.getColumnIndexOrThrow("quantity"));

                    String price = items.getString(
                            items.getColumnIndexOrThrow("unit_price"));

                    String subtotal = items.getString(
                            items.getColumnIndexOrThrow("subtotal"));

                    // Reserve space for totals on the final page.
                    if (y > BOTTOM - 145) {
                        writer.startPage();
                        writer.tableHeader(65);
                        y = 98;
                    }

                    writer.productRow(
                            name,
                            quantity,
                            price,
                            subtotal,
                            y
                    );

                    y += 29;
                }
            }

            if (y > BOTTOM - 120) {
                writer.startPage();
                y = 100;
            }

            writer.line(LEFT, y, RIGHT, y);
            y += 32;

            writer.text(
                    "TOTAL AMOUNT",
                    LEFT,
                    y,
                    15,
                    true,
                    0xFF173F73
            );

            writer.rightText(
                    "Rs. " + money(total),
                    RIGHT,
                    y,
                    15,
                    true,
                    0xFF173F73
            );

            y += 32;

            writer.text(
                    "Payment Method: " + display(paymentMethod),
                    LEFT,
                    y,
                    11,
                    false,
                    0xFF333333
            );

            y += 22;

            int statusColor = "PAID".equalsIgnoreCase(paymentStatus)
                    ? 0xFF15803D
                    : 0xFFB45309;

            writer.text(
                    "Payment Status: " + display(paymentStatus),
                    LEFT,
                    y,
                    11,
                    true,
                    statusColor
            );

            y += 42;

            writer.centered(
                    "Thank you for shopping with us!",
                    y,
                    13,
                    true,
                    0xFF173F73
            );

            writer.centered(
                    "Generated by SmartBill",
                    y + 20,
                    9,
                    false,
                    0xFF718096
            );

            File directory = pdfFile.getParentFile();

            if (directory == null ||
                    (!directory.exists() && !directory.mkdirs())) {
                throw new IOException(
                        "Could not create receipt directory.");
            }

            // Finish the active page before writing the document.
            writer.finish();

            try (FileOutputStream output =
                         new FileOutputStream(pdfFile)) {
                document.writeTo(output);
                output.flush();
            }

            return pdfFile;

        } finally {
            document.close();
            dbHelper.close();
        }
    }

    private static class PdfWriter {

        private final PdfDocument document;
        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber = 0;

        PdfWriter(PdfDocument document) {
            this.document = document;
        }

        void startPage() {
            if (page != null) {
                document.finishPage(page);
            }

            pageNumber++;

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(
                            PAGE_WIDTH,
                            PAGE_HEIGHT,
                            pageNumber
                    ).create();

            page = document.startPage(info);
            canvas = page.getCanvas();
        }

        void finish() {
            if (page != null) {
                document.finishPage(page);
                page = null;
            }
        }

        void drawHeader(String shopName) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xFF173F73);

            canvas.drawRect(
                    0, 0, PAGE_WIDTH, 82, p);

            centered(
                    display(shopName),
                    42,
                    23,
                    true,
                    0xFFFFFFFF
            );

            centered(
                    "SMARTBILL",
                    66,
                    10,
                    true,
                    0xFFDCE8F8
            );
        }

        void tableHeader(int y) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xFFE6EEF9);

            canvas.drawRect(
                    LEFT, y - 18, RIGHT, y + 8, p);

            text("PRODUCT", LEFT + 5, y,
                    9, true, 0xFF173F73);

            rightText("QTY", 365, y,
                    9, true, 0xFF173F73);

            rightText("PRICE", 450, y,
                    9, true, 0xFF173F73);

            rightText("AMOUNT", RIGHT - 5, y,
                    9, true, 0xFF173F73);
        }

        void productRow(
                String name,
                int quantity,
                String price,
                String subtotal,
                int y
        ) {
            text(shorten(name, 27), LEFT + 5, y,
                    9, false, 0xFF333333);

            rightText(String.valueOf(quantity), 365, y,
                    9, false, 0xFF333333);

            rightText(money(price), 450, y,
                    9, false, 0xFF333333);

            rightText(money(subtotal), RIGHT - 5, y,
                    9, false, 0xFF333333);

            line(LEFT, y + 10, RIGHT, y + 10);
        }

        void text(
                String value,
                float x,
                float y,
                float size,
                boolean bold,
                int color
        ) {
            Paint p = createPaint(size, bold, color);
            canvas.drawText(
                    value == null ? "" : value,
                    x,
                    y,
                    p
            );
        }

        void rightText(
                String value,
                float rightX,
                float y,
                float size,
                boolean bold,
                int color
        ) {
            Paint p = createPaint(size, bold, color);
            String safe = value == null ? "" : value;

            canvas.drawText(
                    safe,
                    rightX - p.measureText(safe),
                    y,
                    p
            );
        }

        void centered(
                String value,
                float y,
                float size,
                boolean bold,
                int color
        ) {
            Paint p = createPaint(size, bold, color);
            String safe = value == null ? "" : value;

            canvas.drawText(
                    safe,
                    (PAGE_WIDTH - p.measureText(safe)) / 2f,
                    y,
                    p
            );
        }

        void line(
                float x1,
                float y1,
                float x2,
                float y2
        ) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xFFDCE4EF);
            p.setStrokeWidth(1);

            canvas.drawLine(x1, y1, x2, y2, p);
        }

        private Paint createPaint(
                float size,
                boolean bold,
                int color
        ) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            p.setTextSize(size);
            p.setColor(color);
            p.setTypeface(Typeface.create(
                    Typeface.DEFAULT,
                    bold ? Typeface.BOLD : Typeface.NORMAL
            ));

            return p;
        }

        private String shorten(String value, int maxLength) {
            if (value == null) {
                return "";
            }

            if (value.length() <= maxLength) {
                return value;
            }

            return value.substring(0, maxLength - 3) + "...";
        }
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String display(String value) {
        return empty(value) ? "-" : value.trim();
    }

    private static String money(String value) {
        try {
            return new BigDecimal(value).toPlainString();
        } catch (Exception e) {
            return "0.00";
        }
    }

    private static String formatDate(String value) {
        if (empty(value)) {
            return "-";
        }

        try {
            SimpleDateFormat input = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
            );

            Date date = input.parse(value);

            if (date == null) {
                return value;
            }

            SimpleDateFormat output = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            );

            return output.format(date);

        } catch (Exception e) {
            return value;
        }
    }

    private static String[] wrap(String text, int maxLength) {
        String value = text == null ? "" : text.trim();

        if (value.isEmpty()) {
            return new String[0];
        }

        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : value.split("\\s+")) {
            if (current.length() > 0 &&
                    current.length() + word.length() + 1 > maxLength) {
                lines.add(current.toString());
                current.setLength(0);
            }

            if (current.length() > 0) {
                current.append(' ');
            }

            current.append(word);
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines.toArray(new String[0]);
    }
}