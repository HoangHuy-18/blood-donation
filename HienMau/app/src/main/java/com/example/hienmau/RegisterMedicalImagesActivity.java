package com.example.hienmau;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.RegisterMedical;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterMedicalImagesActivity extends AppCompatActivity {
    private ImageView ivGiayPhep, ivQuyetDinh;
    private String strGiayPhep = "", strQuyetDinh = "";
    private RegisterMedical medicalData; // Chứa dữ liệu từ trang trước


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_medical_images);


        medicalData = (RegisterMedical) getIntent().getSerializableExtra("MEDICAL_DATA");

        ivGiayPhep = findViewById(R.id.ivGiayPhep);
        ivQuyetDinh = findViewById(R.id.ivQuyetDinh);

        View header = findViewById(R.id.headerStep);
        TextView tvStep = header.findViewById(R.id.tvStepTitle);
        LinearProgressIndicator progress = header.findViewById(R.id.registerProgress);

        tvStep.setText("Bước 2: Xác thực hồ sơ pháp lý");
        progress.setProgress(100);


        ivGiayPhep.setOnClickListener(v -> pickImage(1));
        ivQuyetDinh.setOnClickListener(v -> pickImage(2));

        findViewById(R.id.btnFinalRegister).setOnClickListener(v -> uploadAndRegister());
    }


    // Hàm chọn ảnh từ máy
    private void pickImage(int type) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        if (type == 1) launcherGiayPhep.launch(intent);
        else launcherQuyetDinh.launch(intent);
    }

    // Nhận kết quả chọn ảnh và chuyển sang Base64
    private final ActivityResultLauncher<Intent> launcherGiayPhep = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    ivGiayPhep.setImageURI(uri);
                    strGiayPhep = encodeImageToBase64(uri);
                }
            });

    private final ActivityResultLauncher<Intent> launcherQuyetDinh = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    ivQuyetDinh.setImageURI(uri);
                    strQuyetDinh = encodeImageToBase64(uri);
                }
            });

    private String encodeImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream); // Nén 70% để đỡ nặng
            byte[] bytes = outputStream.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) { return ""; }
    }

    private void uploadAndRegister() {
        if (strGiayPhep.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ảnh Giấy phép!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gán ảnh vào DTO
        medicalData.setAnhGiayPhep(strGiayPhep);
        medicalData.setAnhQuyetDinh(strQuyetDinh);

        // HIỆN BẢNG KIỂM TRA LẠI THÔNG TIN
        String reviewInfo = "Tên: " + medicalData.hoTen + "\n" +
                "MST: " + medicalData.maSoThue + "\n" +
                "Email: " + medicalData.email + "\n" +
                "SĐT: " + medicalData.sdt + "\n" +
                "Địa chỉ: " + medicalData.diaChi;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng ký")
                .setMessage("Vui lòng kiểm tra lại thông tin:\n\n" + reviewInfo)
                .setPositiveButton("ĐĂNG KÝ NGAY", (dialog, which) -> {
                    // Chạy code gọi API của Huy ở đây
                    tienHanhGoiApi();
                })
                .setNegativeButton("CHỈNH SỬA LẠI", null)
                .show();
    }
    private void tienHanhGoiApi() {
        // Gọi API Đăng ký
        ApiClient.getApiService().registerMedical(medicalData).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterMedicalImagesActivity.this, "Đăng ký thành công! Vui lòng chờ duyệt.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterMedicalImagesActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    try {
                        // Hiện lỗi nếu Email hoặc SDT đã tồn tại từ Server
                        String error = response.errorBody().string();
                        Toast.makeText(RegisterMedicalImagesActivity.this, error, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(RegisterMedicalImagesActivity.this, "Lỗi đăng ký!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(RegisterMedicalImagesActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
