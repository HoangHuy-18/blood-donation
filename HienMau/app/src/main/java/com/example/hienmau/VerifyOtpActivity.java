package com.example.hienmau;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.ResetPasswordRequest;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {
    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private TextView tvDescription, tvCountdown;
    private androidx.appcompat.widget.AppCompatButton btnVerify;
    private ImageButton btnBack;

    private String userEmail = "";
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Nhận email được chuyển giao sang từ màn hình ForgotPasswordActivity trước đó
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        initViews();
        setupOtpNavigation();
        startResendTimer();

        btnVerify.setOnClickListener(v -> handleResetPasswordFlow());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);

        tvDescription = findViewById(R.id.tvOtpDescription);
        tvCountdown = findViewById(R.id.tvResendCountdown);
        btnVerify = findViewById(R.id.btnXacMinhOtp);
        btnBack = findViewById(R.id.btnBackOtp);

        if (userEmail != null && !userEmail.isEmpty()) {
            tvDescription.setText("Chúng tôi đã gửi mã 6 chữ số đến email " + userEmail);
        }
    }

    // THUẬT TOÁN TỰ ĐỘNG DỊCH CHUYỂN TIÊU ĐIỂM Ô NHẬP LIỆU RỜI (Auto-Focus OTP Boxes)
    private void setupOtpNavigation() {
        etOtp1.addTextChangedListener(new GenericTextWatcher(etOtp1, etOtp2));
        etOtp2.addTextChangedListener(new GenericTextWatcher(etOtp2, etOtp3));
        etOtp3.addTextChangedListener(new GenericTextWatcher(etOtp3, etOtp4));
        etOtp4.addTextChangedListener(new GenericTextWatcher(etOtp4, etOtp5));
        etOtp5.addTextChangedListener(new GenericTextWatcher(etOtp5, etOtp6));
        etOtp6.addTextChangedListener(new GenericTextWatcher(etOtp6, null));

        // Tự động nhảy ngược lại ô trước đó nếu người dùng bấm xóa (Backspace)
        etOtp2.setOnKeyListener((v, keyCode, event) -> handleBackspace(etOtp2, etOtp1, keyCode, event));
        etOtp3.setOnKeyListener((v, keyCode, event) -> handleBackspace(etOtp3, etOtp2, keyCode, event));
        etOtp4.setOnKeyListener((v, keyCode, event) -> handleBackspace(etOtp4, etOtp3, keyCode, event));
        etOtp5.setOnKeyListener((v, keyCode, event) -> handleBackspace(etOtp5, etOtp4, keyCode, event));
        etOtp6.setOnKeyListener((v, keyCode, event) -> handleBackspace(etOtp6, etOtp5, keyCode, event));
    }

    private boolean handleBackspace(EditText current, EditText previous, int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            if (current.getText().toString().isEmpty()) {
                previous.setText("");
                previous.requestFocus();
                return true;
            }
        }
        return false;
    }

    private class GenericTextWatcher implements TextWatcher {
        private final View currentView;
        private final View nextView;

        public GenericTextWatcher(View currentView, View nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
    }

    private void startResendTimer() {
        tvCountdown.setEnabled(false);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(String.format(Locale.getDefault(), "Chưa nhận được? Gửi lại sau 0:%02d", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("Gửi lại mã xác nhận ngay");
                tvCountdown.setTextColor(Color.parseColor("#D32F2F"));
                tvCountdown.setEnabled(true);
                tvCountdown.setOnClickListener(v -> reSendOtpCode());
            }
        }.start();
    }

    private void reSendOtpCode() {
        startResendTimer();
        tvCountdown.setTextColor(Color.parseColor("#555555"));
        ApiClient.getApiService().forgotPassword(userEmail).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyOtpActivity.this, "Mã mới đã được gửi vào hòm thư của bạn!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // HỘP THOẠI NHẬP MẬT KHẨU MỚI TẠI CHỖ (Bảo mật lồng biểu mẫu)
    private void handleResetPasswordFlow() {
        StringBuilder sbOtp = new StringBuilder();
        sbOtp.append(etOtp1.getText().toString().trim())
                .append(etOtp2.getText().toString().trim())
                .append(etOtp3.getText().toString().trim())
                .append(etOtp4.getText().toString().trim())
                .append(etOtp5.getText().toString().trim())
                .append(etOtp6.getText().toString().trim());

        String fullOtp = sbOtp.toString();
        if (fullOtp.length() < 6) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ mã OTP 6 số!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo Dialog hiện đại cho y bác sĩ hoặc người dùng điền mật khẩu mới luôn tại chỗ
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_password, null);
        android.widget.EditText etNewPass = dialogView.findViewById(R.id.etNewPassDialog);
        android.widget.EditText etConfirmPass = dialogView.findViewById(R.id.etConfirmNewPassDialog);

        new AlertDialog.Builder(this)
                .setTitle("Đặt lại mật khẩu mới")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("XÁC NHẬN ĐỔI", (dialog, which) -> {
                    String pass = etNewPass.getText().toString().trim();
                    String confirm = etConfirmPass.getText().toString().trim();

                    if (pass.isEmpty() || !pass.equals(confirm)) {
                        Toast.makeText(VerifyOtpActivity.this, "Mật khẩu không khớp hoặc trống!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    thucHienGoiApiReset(fullOtp, pass);
                })
                .setNegativeButton("HỦY", null)
                .show();
    }
    private void thucHienGoiApiReset(String otp, String newPassword) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang cập nhật mật khẩu mới bảo mật...");
        progressDialog.show();

        ResetPasswordRequest request = new ResetPasswordRequest(userEmail, otp, newPassword);

        ApiClient.getApiService().resetPassword(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyOtpActivity.this, "Thay đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();

                    // Xóa sạch ngăn xếp và điều hướng thẳng về LoginActivity
                    Intent intent = new Intent(VerifyOtpActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Mã xác nhận OTP không chính xác hoặc đã hết hạn!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(VerifyOtpActivity.this, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
