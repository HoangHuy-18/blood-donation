package com.example.hienmau;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.models.XacNhanHienMau;

public class CheckInEventActivity extends AppCompatActivity {
    private XacNhanHienMau data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_event);

        data = (XacNhanHienMau) getIntent().getSerializableExtra("DATA_XN");

        initViews();
        generateQrCode(data.qrCode);
    }

    private void initViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarCheckIn);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvTen = findViewById(R.id.tvTenSuKien);
        TextView tvDiaChi = findViewById(R.id.tvDiaChiSuKien);
        TextView tvMa = findViewById(R.id.tvMaDinhDanh);
        Button btnMap = findViewById(R.id.btnMoBanDo);

        if (data.yeuCau != null) {
            tvTen.setText(data.yeuCau.tenBenhVien);
            tvDiaChi.setText("Địa chỉ: " + data.yeuCau.diaChiBenhVien);
            tvMa.setText("Mã định danh: " + data.qrCode);

            btnMap.setOnClickListener(v -> {
                // Mở Google Maps dẫn đường
                String uri = "google.navigation:q=" + data.yeuCau.viDo + "," + data.yeuCau.kinhDo;
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            });
        }
    }

    private void generateQrCode(String content) {
        try {
            com.journeyapps.barcodescanner.BarcodeEncoder barcodeEncoder = new com.journeyapps.barcodescanner.BarcodeEncoder();
            android.graphics.Bitmap bitmap = barcodeEncoder.encodeBitmap(content, com.google.zxing.BarcodeFormat.QR_CODE, 500, 500);
            ImageView imageView = findViewById(R.id.imgQrCheckIn);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
