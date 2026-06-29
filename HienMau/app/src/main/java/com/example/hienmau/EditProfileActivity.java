package com.example.hienmau;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.UpdateProfileRequest;
import com.example.hienmau.models.UserProfileResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {
    private TextInputLayout tilName;
    private TextInputEditText etName, etSdt, etDiaChi, etBlood;
    private MaterialButton btnSave;
    private NguoiDung currentUser;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    currentLat = result.getData().getDoubleExtra("lat", 0.0);
                    currentLng = result.getData().getDoubleExtra("lng", 0.0);
                    String address = result.getData().getStringExtra("address");

                    // Điền chuỗi địa chỉ định vị an toàn vào ô nhập liệu
                    etDiaChi.setText(address);
                    etDiaChi.setError(null); // Xóa cảnh báo lỗi nếu có
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Lấy dữ liệu phiên đăng nhập hiện tại
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userJson = prefs.getString("USER_DATA", null);
        currentUser = new Gson().fromJson(userJson, NguoiDung.class);

        if (currentUser != null) {
            currentLat = currentUser.viDo;
            currentLng = currentUser.kinhDo;
        }

        initViews();
        setupToolbar();
        prefillData();

        // Gắn bộ lắng nghe: Khi bấm vào ô địa chỉ hoặc biểu tượng bản đồ đều mở MapsPickerActivity
        etDiaChi.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        tilName = findViewById(R.id.tilName);
        etName = findViewById(R.id.etEditName);
        etSdt = findViewById(R.id.etEditSdt);
        etDiaChi = findViewById(R.id.etEditDiaChi);
        etBlood = findViewById(R.id.etEditBlood);
        btnSave = findViewById(R.id.btnSaveProfile);

        if (currentUser.loaiTaiKhoan == 1) {
            tilName.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarEdit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void prefillData() {
        etName.setText(currentUser.hoTen);
        etSdt.setText(currentUser.sdt);
        etDiaChi.setText(currentUser.diaChi);
        etBlood.setText("Dữ liệu bảo mật hệ thống");
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String sdt = etSdt.getText().toString().trim();
        String diaChi = etDiaChi.getText().toString().trim();

        if (sdt.isEmpty() || diaChi.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin số điện thoại và địa chỉ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // RÀNG BUỘC CỨNG: Ép buộc phải có tọa độ thực thi (tránh trường hợp chuỗi địa chỉ bị xóa trắng)
        if (currentLat == 0.0 || currentLng == 0.0) {
            Toast.makeText(this, "Vui lòng click chọn lại vị trí hợp lệ trên bản đồ định vị!", Toast.LENGTH_LONG).show();
            return;
        }

        // Khởi tạo và đóng gói dữ liệu JSON an toàn
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUserId(currentUser.id);
        request.setSdt(sdt);
        request.setDiaChi(diaChi);
        request.setViDo(currentLat);
        request.setKinhDo(currentLng);

        if (currentUser.loaiTaiKhoan == 0) {
            request.setHoTen(name);
        }

        btnSave.setEnabled(false);

        ApiClient.getApiService().updateProfile(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    currentUser.hoTen = name;
                    currentUser.sdt = sdt;
                    currentUser.diaChi = diaChi;
                    currentUser.viDo = currentLat;
                    currentUser.kinhDo = currentLng;

                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                            .putString("USER_DATA", new Gson().toJson(currentUser)).apply();

                    Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ định vị thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thất bại từ máy chủ!", Toast.LENGTH_SHORT).show();
                }
                btnSave.setEnabled(true);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối mạng đến máy chủ trung tâm!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
