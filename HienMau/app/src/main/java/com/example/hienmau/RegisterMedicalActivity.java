package com.example.hienmau;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.RegisterMedical;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterMedicalActivity extends AppCompatActivity {
    private TextInputEditText etMaSoThue, etTenCoSo, etSDT, etMatKhauConfirm, etEmail, etMatKhau, etDiaChi;
    private TextView tvCoordinatesMedical;

    private Button btnPickLocationMedical, btnRegisterMedicalSubmit;

    private double selectedLat = 0, selectedLng = 0;

    // Bộ nhận kết quả từ MapsPickerActivity
    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("lat", 0);
                    selectedLng = result.getData().getDoubleExtra("lng", 0);
                    String address = result.getData().getStringExtra("address");

                    // Tự động điền địa chỉ do Google trả về và hiển thị tọa độ
                    etDiaChi.setText(address);
                    tvCoordinatesMedical.setText(String.format(Locale.getDefault(), "Tọa độ: %.6f, %.6f", selectedLat, selectedLng));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_medical);

        // 1. Ánh xạ các View từ XML
        initViews();

        // 2. Nút mở bản đồ chọn vị trí
        btnPickLocationMedical.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        View header = findViewById(R.id.headerStep);
        TextView tvStep = header.findViewById(R.id.tvStepTitle);
        LinearProgressIndicator progress = header.findViewById(R.id.registerProgress);

        tvStep.setText("Bước 1: Thông tin cơ bản");
        progress.setProgress(50); // Mới xong một nửa
        // 3. Nút gửi đăng ký
        btnRegisterMedicalSubmit.setOnClickListener(v -> handleRegisterMedical());
    }

    private void initViews() {
        etMaSoThue = findViewById(R.id.etMaSoThue);
        etTenCoSo = findViewById(R.id.etTenCoSo);
        etSDT = findViewById(R.id.etSDT);
        etEmail = findViewById(R.id.etEmail);
        etMatKhau = findViewById(R.id.etMatKhau);
        etMatKhauConfirm = findViewById(R.id.etMatKhauConfirm);
        etDiaChi = findViewById(R.id.etDiaChi);

        tvCoordinatesMedical = findViewById(R.id.tvCoordinates); // Hoặc ID tương ứng bạn đặt
        btnPickLocationMedical = findViewById(R.id.btnPickLocation); // Nút chọn từ bản đồ
        btnRegisterMedicalSubmit = findViewById(R.id.btnRegisterMedicalSubmit);
    }

    private void handleRegisterMedical() {

        String pass = etMatKhau.getText().toString().trim();
        String confirmPass = etMatKhauConfirm.getText().toString().trim();

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp!", Toast.LENGTH_SHORT).show();
            etMatKhauConfirm.requestFocus();
            return;
        }

        // Kiểm tra dữ liệu cơ bản
        if (selectedLat == 0 || selectedLng == 0) {
            Toast.makeText(this, "Vui lòng xác vị trí trên bản đồ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo DTO gửi lên Server
        RegisterMedical dto = new RegisterMedical(
                etMaSoThue.getText().toString().trim(),
                etTenCoSo.getText().toString().trim(),
                etSDT.getText().toString().trim(),
                etMatKhau.getText().toString().trim(),
                etEmail.getText().toString().trim(),
                etDiaChi.getText().toString().trim(),
                selectedLat,
                selectedLng
        );

        Intent intent = new Intent(this, RegisterMedicalImagesActivity.class);
        intent.putExtra("MEDICAL_DATA", dto);
        startActivity(intent);

    }
}
