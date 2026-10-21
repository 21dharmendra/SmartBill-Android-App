package com.smartbill.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.smartbill.app.database.DBHelper;
import com.smartbill.app.model.BillSummary;

import android.graphics.Color;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.List;

public class BillHistoryActivity extends Activity {

    private DBHelper dbHelper;

    private ListView listBillHistory;
    private TextView txtBillCount;
    private TextView txtEmptyHistory;

    private final List<BillSummary> bills =
            new ArrayList<>();

    private ArrayAdapter<BillSummary> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bill_history);

        listBillHistory = findViewById(
                R.id.listBillHistory
        );

        txtBillCount = findViewById(
                R.id.txtBillCount
        );

        txtEmptyHistory = findViewById(
                R.id.txtEmptyHistory
        );

        dbHelper = new DBHelper(this);

        setupAdapter();

        listBillHistory.setAdapter(adapter);

        listBillHistory.setOnItemClickListener(
                (parent, view, position, id) -> {

                    BillSummary bill = bills.get(position);

                    Intent intent = new Intent(
                            BillHistoryActivity.this,
                            ReceiptActivity.class
                    );

                    intent.putExtra(
                            "bill_id",
                            bill.getBillId()
                    );

                    startActivity(intent);
                }
        );

        loadBills();
    }

    private void setupAdapter() {

        adapter = new ArrayAdapter<BillSummary>(
                this,
                0,
                bills
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent) {

                View itemView = convertView;

                if (itemView == null) {
                    itemView = getLayoutInflater().inflate(
                            R.layout.item_bill_history,
                            parent,
                            false
                    );
                }

                BillSummary bill = getItem(position);

                if (bill != null) {

                    TextView txtBillNumber =
                            itemView.findViewById(R.id.txtHistoryBillNumber);

                    TextView txtAmount =
                            itemView.findViewById(R.id.txtHistoryAmount);

                    TextView txtDate =
                            itemView.findViewById(R.id.txtHistoryDate);

                    TextView txtPaymentMethod =
                            itemView.findViewById(R.id.txtHistoryPaymentMethod);

                    TextView txtPaymentStatus =
                            itemView.findViewById(R.id.txtHistoryPaymentStatus);

                    txtBillNumber.setText(
                            "Bill No: SB" + bill.getBillId()
                    );

                    txtAmount.setText(
                            "₹" + bill.getTotalAmount()
                    );

                    txtDate.setText(
                            "Date: " + bill.getCreatedAt()
                    );

                    txtPaymentMethod.setText(
                            "Payment: " + bill.getPaymentMethod()
                    );

                    String status = bill.getPaymentStatus();

                    if (status == null) {
                        status = "UNKNOWN";
                    }

                    status = status.toUpperCase(java.util.Locale.ROOT);

                    txtPaymentStatus.setText(status);

                    if ("PAID".equals(status)) {

                        txtPaymentStatus.setTextColor(
                                Color.parseColor("#2E7D32")
                        );

                        txtPaymentStatus.setBackgroundColor(
                                Color.parseColor("#E8F5E9")
                        );

                    } else if ("PENDING".equals(status)) {

                        txtPaymentStatus.setTextColor(
                                Color.parseColor("#E65100")
                        );

                        txtPaymentStatus.setBackgroundColor(
                                Color.parseColor("#FFF3E0")
                        );

                    } else {

                        txtPaymentStatus.setTextColor(Color.DKGRAY);

                        txtPaymentStatus.setBackgroundColor(
                                Color.parseColor("#EEEEEE")
                        );
                    }
                }

                return itemView;
            }
        };
    }

    private void loadBills() {

        bills.clear();

        bills.addAll(
                dbHelper.getAllBills()
        );

        adapter.notifyDataSetChanged();

        txtBillCount.setText(
                "Total Bills: " + bills.size()
        );

        if (bills.isEmpty()) {

            txtEmptyHistory.setVisibility(
                    View.VISIBLE
            );

            listBillHistory.setVisibility(
                    View.GONE
            );

        } else {

            txtEmptyHistory.setVisibility(
                    View.GONE
            );

            listBillHistory.setVisibility(
                    View.VISIBLE
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dbHelper != null && adapter != null) {
            loadBills();
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
