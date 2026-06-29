package com.example.hienmau;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.UserProfileResponse;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private TextView tvAvatar, tvName, tvDanhHieu, tvNhomMau, tvSoLanHien, tvNgayGanNhat, tvMaSoThue;
    private View cardDonor, cardMedical;
    private Button btnLogout, btnEdit, btnLichSuHienMau, btnVerifyBlood;
    private NguoiDung currentUser;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {

                    loadProfileData();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        // Lấy thông tin User hiện tại từ SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userJson = prefs.getString("USER_DATA", null);
        if (userJson != null) {
            currentUser = new Gson().fromJson(userJson, NguoiDung.class);
            loadProfileData();
        }

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            editProfileLauncher.launch(intent);
        });

        btnLichSuHienMau.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent intent = new Intent(getContext(), LichSuHienMauActivity.class);
                intent.putExtra("USER_ID", currentUser.id);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void initViews(View view) {

        tvAvatar = view.findViewById(R.id.tvAvatarProfile);
        tvName = view.findViewById(R.id.tvProfileName);
        tvDanhHieu = view.findViewById(R.id.tvDanhHieu);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEdit = view.findViewById(R.id.btnEditProfile);


        cardDonor = view.findViewById(R.id.cardStatsDonor);
        tvNhomMau = view.findViewById(R.id.tvProfileNhomMau);
        tvSoLanHien = view.findViewById(R.id.tvSoLanHien);
        tvNgayGanNhat = view.findViewById(R.id.tvNgayHienGanNhat);
        btnLichSuHienMau = view.findViewById(R.id.btnLichSuHienMau);
        btnVerifyBlood = view.findViewById(R.id.btnVerifyBlood);

        cardDonor = view.findViewById(R.id.cardStatsDonor);
        cardMedical = view.findViewById(R.id.cardStatsMedical);
        tvMaSoThue = view.findViewById(R.id.tvMaSoThue);
    }

    private void loadProfileData() {
        if (currentUser == null) return;


        ApiClient.getApiService().getUserProfile(currentUser.id).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse data = response.body();

                    tvName.setText(data.hoTen != null ? data.hoTen.toUpperCase() : "NGƯỜI DÙNG");
                    if (data.hoTen != null && !data.hoTen.isEmpty()) {
                        tvAvatar.setText(data.hoTen.substring(0, 1).toUpperCase());
                    }

                    tvDanhHieu.setText(data.danhHieu != null ? data.danhHieu : "Người gieo hy vọng");

                    // 2. LOGIC PHÂN LOẠI HIỂN THỊ
                    if (data.loaiTaiKhoan == 1) {
                        // TRƯỜNG HỢP: BỆNH VIỆN
                        cardDonor.setVisibility(View.GONE);
                        cardMedical.setVisibility(View.VISIBLE);
                        btnVerifyBlood.setVisibility(View.GONE); // Bệnh viện không cần xác minh nhóm máu
                        btnLichSuHienMau.setVisibility(View.GONE);
                    } else {
                        cardMedical.setVisibility(View.GONE);
                        cardDonor.setVisibility(View.VISIBLE);
                        tvNgayGanNhat.setVisibility(View.VISIBLE);

                        // 3. HIỆN NÚT XÁC MINH VÀ SET SỰ KIỆN CLICK
                        btnVerifyBlood.setVisibility(View.VISIBLE);

                        if (data.trangThaiXacMinh == 1) {
                            btnVerifyBlood.setOnClickListener(v ->
                                    Toast.makeText(getContext(), "Yêu cầu của bạn đang chờ xử lý", Toast.LENGTH_SHORT).show()
                            );
                        } else if (data.trangThaiXacMinh == 2) {
                            btnVerifyBlood.setOnClickListener(v ->
                                    Toast.makeText(getContext(), "Nhóm máu của bạn đã được xác minh", Toast.LENGTH_SHORT).show()
                            );
                        } else {
                            btnVerifyBlood.setOnClickListener(v -> {
                                Intent intent = new Intent(getContext(), VerifyBloodActivity.class);
                                startActivity(intent);
                            });
                        }

                        String fullBloodType = "Chưa rõ";
                        if (data.nhomMau != null && !data.nhomMau.equals("Chưa rõ")) {
                            fullBloodType = data.nhomMau + (data.heRh != null ? data.heRh : "");
                        }
                        tvNhomMau.setText(fullBloodType);

                        tvSoLanHien.setText(String.format(java.util.Locale.getDefault(), "%02d", data.soLanHien));

                        // Hiển thị ngày hiến gần nhất
                        if (data.ngayHienGanNhat != null && !data.ngayHienGanNhat.isEmpty()) {
                            tvNgayGanNhat.setText("Ngày hiến gần nhất: " + formatDate(data.ngayHienGanNhat));
                            tvNgayGanNhat.setTextColor(Color.parseColor("#444444"));
                        } else {
                            tvNgayGanNhat.setText("Bạn chưa có dữ liệu hiến máu");
                            tvNgayGanNhat.setTextColor(Color.GRAY);
                        }
                        btnLichSuHienMau.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void performLogout() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return "Chưa xác định";
        try {

            String ymd = dateStr.substring(0, 10);
            String[] parts = ymd.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0]; // Trả về dd/MM/yyyy
            }
        } catch (Exception e) {
            return "Ngày không hợp lệ";
        }
        return dateStr;
    }
}
