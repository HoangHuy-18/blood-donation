package com.example.hienmau;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.ResetPasswordRequest;
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputEditText etEmail;
    private Button btnSendOtp;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Ánh xạ các thành phần giao diện phục vụ luồng gửi yêu cầu
        etEmail = findViewById(R.id.etEmailForgot);
        btnSendOtp = findViewById(R.id.btnSendOtp);

        btnSendOtp.setOnClickListener(v -> handleSendOtp());
    }

    private void handleSendOtp() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError("Nhập email!");
            return;
        }

        // Hiện vòng quay xử lý để y bác sĩ hoặc người dùng biết hệ thống đang kết nối Server
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi yêu cầu mã OTP...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        startCountDown();


        ApiClient.getApiService().forgotPassword(email).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Mã OTP đã được gửi đi thành công!", Toast.LENGTH_SHORT).show();


                    Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                    intent.putExtra("USER_EMAIL", email);
                    startActivity(intent);


                    finish();
                } else {

                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Email không tồn tại!";
                        Toast.makeText(ForgotPasswordActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this, "Email không tồn tại trên hệ thống!", Toast.LENGTH_SHORT).show();
                    }

                    if (countDownTimer != null) countDownTimer.cancel();
                    btnSendOtp.setEnabled(true);
                    btnSendOtp.setText("GỬI LẠI MÃ MỚI");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
                if (countDownTimer != null) countDownTimer.cancel();
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("GỬI LẠI MÃ MỚI");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối mạng đến máy chủ trung tâm!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCountDown() {
        btnSendOtp.setEnabled(false);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnSendOtp.setText("Gửi lại sau (" + millisUntilFinished / 1000 + "s)");
            }

            @Override
            public void onFinish() {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("GỬI LẠI MÃ MỚI");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
