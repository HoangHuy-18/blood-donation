package com.example.hienmau;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.LoginRequest;

import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmailLogin, etMatKhau;
    private Button btnLogin;
    private TextView tvRegister , tvForgotPassword;

    private void saveUserToPrefs(NguoiDung user) {
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();

        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(user);

        editor.putString("USER_DATA", json);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (prefs.contains("USER_DATA")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // 1. Ánh xạ các thành phần giao diện
        etEmailLogin = findViewById(R.id.etEmailLogin);
        etMatKhau = findViewById(R.id.etMatKhau);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // 2. Xử lý sự kiện nút Đăng nhập
        btnLogin.setOnClickListener(view -> {
            String email = etEmailLogin.getText().toString().trim();
            String pass = etMatKhau.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            } else {
                tienHanhDangNhap(email, pass);
            }
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // 3. Chuyển sang trang Chọn loại đăng ký (Cá nhân / Tổ chức)
        tvRegister.setOnClickListener(view -> {
            // Thay vì hiện Toast, giờ chúng ta mở RegisterTypeActivity
            Intent intent = new Intent(LoginActivity.this, RegisterTypeActivity.class);
            startActivity(intent);
        });
    }

    private void tienHanhDangNhap(String email, String pass) {
        LoginRequest request = new LoginRequest(email, pass);

        ApiClient.getApiService().login(request).enqueue(new Callback<NguoiDung>() {
            @Override
            public void onResponse(Call<NguoiDung> call, Response<NguoiDung> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // TRƯỜNG HỢP 1: ĐĂNG NHẬP THÀNH CÔNG (Cá nhân hoặc Tổ chức đã duyệt)
                    NguoiDung user = response.body();
                    saveUserToPrefs(user);

                    Toast.makeText(LoginActivity.this, "Xin chào " + user.hoTen, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                    capNhatTokenHauDangNhap(user.id);
                } else {
                    // TRƯỜNG HỢP 2: THẤT BẠI (Sai pass HOẶC Đang chờ phê duyệt)
                    String errorMsg = "Sai Email hoặc mật khẩu!"; // Mặc định
                    try {
                        // Lấy tin nhắn lỗi từ Server (ví dụ: "Tài khoản tổ chức của bạn đang chờ phê duyệt!")
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("LOGIN_ERROR", e.getMessage());
                    }

                    // Hiển thị chính xác thông báo từ API trả về
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<NguoiDung> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage());
                Toast.makeText(LoginActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void capNhatTokenHauDangNhap(int userId) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        guiTokenLenServer(userId, task.getResult());
                    }
                });
    }
    private void guiTokenLenServer(int userId, String token) {
        ApiClient.getApiService().updateToken(userId, token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("FCM_UPDATE", "Cập nhật Token thành công");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("FCM_UPDATE", "Lỗi cập nhật Token: " + t.getMessage());
            }
        });
    }
}
