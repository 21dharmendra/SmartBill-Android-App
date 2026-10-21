package com.smartbill.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etShopName;
    private EditText etOwnerName;
    private EditText etShopPhone;
    private EditText etShopAddress;
    private EditText etShopUpiId;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etShopName = findViewById(R.id.etShopName);
        etOwnerName = findViewById(R.id.etOwnerName);
        etShopPhone = findViewById(R.id.etShopPhone);
        etShopAddress = findViewById(R.id.etShopAddress);
        etShopUpiId = findViewById(R.id.etShopUpiId);

        Button btnSaveSettings = findViewById(R.id.btnSaveSettings);

        preferences = getSharedPreferences(
                "smartbill_settings",
                MODE_PRIVATE
        );

        loadShopDetails();

        btnSaveSettings.setOnClickListener(v -> saveShopDetails());
    }

    private void loadShopDetails() {
        etShopName.setText(preferences.getString("shop_name", ""));
        etOwnerName.setText(preferences.getString("owner_name", ""));
        etShopPhone.setText(preferences.getString("shop_phone", ""));
        etShopAddress.setText(preferences.getString("shop_address", ""));
        etShopUpiId.setText(preferences.getString("shop_upi_id",""));
    }

    private void saveShopDetails() {
        String shopName = etShopName.getText().toString().trim();
        String ownerName = etOwnerName.getText().toString().trim();
        String shopPhone = etShopPhone.getText().toString().trim();
        String shopAddress = etShopAddress.getText().toString().trim();
        String shopUpiId = etShopUpiId.getText().toString().trim();

        if (shopName.isEmpty()) {
            etShopName.setError("Please enter your shop name");
            etShopName.requestFocus();
            return;
        }

        boolean saved = preferences.edit()
                .putString("shop_name", shopName)
                .putString("owner_name", ownerName)
                .putString("shop_phone", shopPhone)
                .putString("shop_address", shopAddress)
                .putString("shop_upi_id",shopUpiId)
                .commit();

        if (saved) {
            Toast.makeText(
                    this,
                    "Shop details saved successfully",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "Unable to save shop details",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

}
