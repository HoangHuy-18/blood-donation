package com.example.hienmau;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.RegisterPersonal;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterPersonalActivity extends AppCompatActivity {
    private TextInputEditText etHoTen, etSDT, etEmail, etMatKhau, etMatKhauConfirm, etNgaySinh, etDiaChi;
    private TextView tvCoordinates;
    private Button btnPickLocation, btnRegisterSubmit;

    private double selectedLat = 0, selectedLng = 0;

    // Bộ nhận kết quả từ màn hình Bản đồ
    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("lat", 0);
                    selectedLng = result.getData().getDoubleExtra("lng", 0);
                    String address = result.getData().getStringExtra("address");

                    // Điền tự động địa chỉ và hiện tọa độ
                    etDiaChi.setText(address);
                    tvCoordinates.setText(String.format(Locale.getDefault(), "Tọa độ: %.6f, %.6f", selectedLat, selectedLng));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_personal);
        // 1. Ánh xạ các View
        initViews();

        View header = findViewById(R.id.headerStep);
        TextView tvStep = header.findViewById(R.id.tvStepTitle);
        LinearProgressIndicator progress = header.findViewById(R.id.registerProgress);

        tvStep.setText("Đăng ký tài khoản cá nhân");
        progress.setProgress(100);

        // 3. Chọn Ngày sinh
        etNgaySinh.setOnClickListener(v -> showDatePicker());
        // 4. Nút mở bản đồ để chọn vị trí
        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });
        // 5. Nút bấm Đăng ký
        btnRegisterSubmit.setOnClickListener(v -> handleRegister());
    }

    private void initViews() {
        etHoTen = findViewById(R.id.etHoTen);
        etSDT = findViewById(R.id.etSDT);
        etEmail = findViewById(R.id.etEmail);
        etMatKhau = findViewById(R.id.etMatKhau);
        etMatKhauConfirm = findViewById(R.id.etMatKhauConfirm);
        etNgaySinh = findViewById(R.id.etNgaySinh);
        etDiaChi = findViewById(R.id.etDiaChi);
        tvCoordinates = findViewById(R.id.tvCoordinates);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // Định dạng yyyy-MM-dd để khớp với C# DateTime
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            etNgaySinh.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void handleRegister() {
        String pass = etMatKhau.getText().toString().trim();
        String confirmPass = etMatKhauConfirm.getText().toString().trim();
        String hoTen = etHoTen.getText().toString().trim();
        String sdt = etSDT.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String diaChi = etDiaChi.getText().toString().trim();
        String ngaySinh = etNgaySinh.getText().toString().trim();

        // 1. Kiểm tra validate cơ bản
        if (hoTen.isEmpty() || sdt.isEmpty() || email.isEmpty() || pass.isEmpty() || ngaySinh.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLat == 0 || selectedLng == 0) {
            Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ!", Toast.LENGTH_SHORT).show();
            return;
        }

        RegisterPersonal dto = new RegisterPersonal(hoTen, sdt, pass, email, diaChi, ngaySinh, selectedLat, selectedLng);


        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng ký")
                .setMessage("Bạn chắc chắn muốn đăng ký với thông tin này?")
                .setPositiveButton("Đăng ký", (dialog, which) -> {
                    performApiRegister(dto);
                })
                .setNegativeButton("Xem lại", null)
                .show();
    }
    private void performApiRegister(RegisterPersonal dto) {
        // Hiện thông báo đang xử lý
        Toast.makeText(this, "Đang xử lý...", Toast.LENGTH_SHORT).show();

        ApiClient.getApiService().registerPersonal(dto).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterPersonalActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    // Quay về màn hình đăng nhập và xóa stack các màn hình trước
                    Intent intent = new Intent(RegisterPersonalActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        String error = response.errorBody().string();
                        Toast.makeText(RegisterPersonalActivity.this, error, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(RegisterPersonalActivity.this, "Lỗi đăng ký!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(RegisterPersonalActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
