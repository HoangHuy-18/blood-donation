package com.example.hienmau;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.RegistrationResponse;
import com.example.hienmau.models.YeuCauMau;
import com.example.hienmau.models.YeuCauMauChiTiet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChiTietYeuCauActivity extends AppCompatActivity {
    private YeuCauMau yeuCau;
    private TextView tvAvatarLarge, tvTenNguoiDang, tvThoiGian, tvTenBenhVien, tvDiaChi, tvNoiDung, tvProgressDetail;
    private ChipGroup chipGroupBlood;
    private MaterialButton btnMainAction;
    private int currentUserId = -1;
    private NguoiDung currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet_yeu_cau);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userJson = prefs.getString("USER_DATA", null);
        if (userJson != null) {
            currentUser = new Gson().fromJson(userJson, NguoiDung.class);
            if (currentUser != null) {
                currentUserId = currentUser.id;
            }
        }

        initViews();

        if (getIntent().hasExtra("DATA_YEU_CAU")) {
            Object data = getIntent().getSerializableExtra("DATA_YEU_CAU");

            if (data instanceof YeuCauMau) {
                yeuCau = (YeuCauMau) data;
                setData();
                setupEvents();
            } else {
                int id = getIntent().getIntExtra("DATA_YEU_CAU", -1);
                if (id != -1) {
                    layDuLieuTuApi(id);
                }
            }
        }

    }
    private void layDuLieuTuApi(int id) {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Đang tải chi tiết...");
        pd.setCancelable(false);
        pd.show();

        ApiClient.getApiService().getChiTietYeuCau(id).enqueue(new retrofit2.Callback<YeuCauMau>() {
            @Override
            public void onResponse(Call<YeuCauMau> call, Response<YeuCauMau> response) {
                pd.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    yeuCau = response.body();
                    setData();
                    setupEvents();
                } else {
                    Toast.makeText(ChiTietYeuCauActivity.this, "Bài đăng này không còn tồn tại!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<YeuCauMau> call, Throwable t) {
                pd.dismiss();
                Toast.makeText(ChiTietYeuCauActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void initViews() {
        tvAvatarLarge = findViewById(R.id.tvAvatarLarge);
        tvTenNguoiDang = findViewById(R.id.tvTenNguoiDang);
        tvThoiGian = findViewById(R.id.tvThoiGian);
        tvTenBenhVien = findViewById(R.id.tvTenBenhVien);
        tvDiaChi = findViewById(R.id.tvDiaChi);
        tvNoiDung = findViewById(R.id.tvNoiDung);
        tvProgressDetail = findViewById(R.id.tvProgressDetail);
        chipGroupBlood = findViewById(R.id.chipGroupBlood);
        btnMainAction = findViewById(R.id.btnMainAction);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupEvents() {
        findViewById(R.id.btnDanDuong).setOnClickListener(v -> {
            if (yeuCau.viDo != 0 && yeuCau.kinhDo != 0) {
                String uri = String.format(java.util.Locale.ENGLISH,
                        "geo:%f,%f?q=%f,%f(%s)",
                        yeuCau.viDo, yeuCau.kinhDo, yeuCau.viDo, yeuCau.kinhDo, yeuCau.tenBenhVien);

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                }
            } else {
                Toast.makeText(this, "Tọa độ bệnh viện không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnGoiDien).setOnClickListener(v -> {
            if (yeuCau != null && yeuCau.nguoiDang != null && yeuCau.nguoiDang.sdt != null) {
                String phone = yeuCau.nguoiDang.sdt.trim();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không tìm thấy số điện thoại người đăng!", Toast.LENGTH_SHORT).show();
            }
        });

        Integer needed = yeuCau.soNguoiCan;
        int confirmed = yeuCau.soNguoiDaXacNhan;

        if (yeuCau.loaiTin == 2) {

            if (yeuCau.nguoiDangId == currentUserId) {
                btnMainAction.setText("XEM DANH SÁCH");
                btnMainAction.setEnabled(true);
                btnMainAction.setBackgroundColor(Color.parseColor("#D32F2F"));
                btnMainAction.setOnClickListener(v -> openListVolunteer());
            }
            else if (currentUser != null && currentUser.loaiTaiKhoan == 1) {
                btnMainAction.setText("CHIA SẺ NGUỒN MÁU");
                btnMainAction.setEnabled(true);
                btnMainAction.setBackgroundColor(Color.parseColor("#2E7D32"));
                btnMainAction.setOnClickListener(v -> hienThiDialogXacNhanChiaSe());
            }
            else {
                btnMainAction.setText("TIN ĐIỀU PHỐI NỘI BỘ LIÊN VIỆN");
                btnMainAction.setEnabled(false);
                btnMainAction.setBackgroundColor(Color.parseColor("#9E9E9E"));
            }
        }
        else {
            if (yeuCau.nguoiDangId == currentUserId) {
                btnMainAction.setText("XEM DANH SÁCH TÌNH NGUYỆN VIÊN");
                btnMainAction.setEnabled(true);
                btnMainAction.setBackgroundColor(Color.parseColor("#D32F2F"));
                btnMainAction.setOnClickListener(v -> openListVolunteer());
            }
            else if (currentUser != null && currentUser.loaiTaiKhoan == 1) {
                btnMainAction.setText("CHỈ DÀNH CHO TÌNH NGUYỆN VIÊN");
                btnMainAction.setEnabled(false);
                btnMainAction.setBackgroundColor(Color.parseColor("#9E9E9E"));
            }
            else {
                if (needed != null && needed > 0 && confirmed >= needed) {
                    btnMainAction.setText("ĐÃ ĐỦ NGƯỜI");
                    btnMainAction.setEnabled(false);
                    btnMainAction.setBackgroundColor(Color.GRAY);
                } else {
                    btnMainAction.setText("TÔI SẼ ĐẾN");
                    btnMainAction.setEnabled(true);
                    btnMainAction.setBackgroundColor(Color.parseColor("#D32F2F"));
                    btnMainAction.setOnClickListener(v -> registerToCome());
                }
            }
        }
    }

    private void hienThiDialogXacNhanChiaSe() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận điều phối liên viện")
                .setMessage("Bạn xác nhận kho lưu trữ của đơn vị mình sẵn sàng san sẻ và vận chuyển nguồn cung cấp máu này cho cơ sở đối tác chứ?")
                .setPositiveButton("XÁC NHẬN CHIA SẺ", (dialog, which) -> goiApiChiaSeLienVien())
                .setNegativeButton("Hủy bỏ", null)
                .show();
    }

    private void goiApiChiaSeLienVien() {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Đang gửi tín hiệu kết nối liên viện...");
        pd.setCancelable(false);
        pd.show();

        ApiClient.getApiService().xacNhanChiaSeMau(yeuCau.id, currentUserId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        pd.dismiss();
                        if (response.isSuccessful()) {
                            Toast.makeText(ChiTietYeuCauActivity.this, "Đã kết nối thành công! Đối tác đã nhận được thông báo hỗ trợ.", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            String errorMsg = "";
                            try {
                                if (response.errorBody() != null) {
                                    String rawJson = response.errorBody().string();

                                    // Kiểm tra xem Server có ném về thực thể đối tượng JSON chứa message cảnh báo trùng hay không
                                    if (rawJson.contains("message")) {
                                        java.util.Map<String, String> map = new Gson().fromJson(rawJson, java.util.Map.class);
                                        errorMsg = map.get("message");
                                    } else {
                                        errorMsg = "Máy chủ gặp sự cố xử lý hệ thống liên viện! (Mã lỗi: " + response.code() + ")";
                                    }
                                } else {
                                    errorMsg = "Kết nối thất bại từ máy chủ trung tâm! (Mã lỗi: " + response.code() + ")";
                                }
                            } catch (Exception e) {
                                errorMsg = "Lỗi bóc tách luồng dữ liệu mạng: " + response.code();
                            }

                            new AlertDialog.Builder(ChiTietYeuCauActivity.this)
                                    .setTitle("Thông báo hệ thống")
                                    .setMessage(errorMsg)
                                    .setPositiveButton("Đã hiểu", null)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        pd.dismiss();
                        Toast.makeText(ChiTietYeuCauActivity.this, "Lỗi kết nối mạng điều phối liên viện!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setData() {
        String name = (yeuCau.nguoiDang != null) ? yeuCau.nguoiDang.hoTen : "Ẩn danh";
        tvTenNguoiDang.setText(name);
        tvAvatarLarge.setText(name.substring(0, 1).toUpperCase());
        tvThoiGian.setText(getRelativeTime(yeuCau.ngayDang));

        String bvName = (yeuCau.benhVien != null && yeuCau.benhVien.thongTinTaiKhoan != null)
                ? yeuCau.benhVien.thongTinTaiKhoan.hoTen : yeuCau.tenBenhVien;
        tvTenBenhVien.setText(bvName);
        tvDiaChi.setText(yeuCau.diaChiBenhVien);
        tvNoiDung.setText((yeuCau.noiDung == null || yeuCau.noiDung.isEmpty()) ? "Không có lời nhắn bổ sung." : yeuCau.noiDung);

        Integer needed = yeuCau.soNguoiCan;
        int confirmed = yeuCau.soNguoiDaXacNhan;

        if (yeuCau.loaiTin == 2) {
            tvProgressDetail.setVisibility(View.GONE);
        } else {
            if (needed == null || needed == 0) {
                tvProgressDetail.setText("Đã có " + confirmed + " người tham gia");
            } else {
                tvProgressDetail.setText("Tiến độ: " + confirmed + "/" + needed + " người đã xác nhận đến");
            }
        }
        if (needed != null && needed > 0 && confirmed >= needed) {
            tvProgressDetail.setTextColor(Color.RED);
        } else {
            tvProgressDetail.setTextColor(Color.parseColor("#757575"));
        }

        chipGroupBlood.removeAllViews();
        if (yeuCau.chiTiets != null) {
            for (YeuCauMauChiTiet ct : yeuCau.chiTiets) {
                addBloodChip(ct);
            }
        }
    }

    private void addBloodChip(YeuCauMauChiTiet ct) {
        Chip chip = new Chip(this);
        String bloodText = ct.nhomMau + ct.rh;
        String urgency = "";
        if (ct.mucDoKhanCap == 3) urgency = " - Khẩn cấp";
        else if (ct.mucDoKhanCap == 2) urgency = " - Ưu tiên";

        chip.setText(bloodText + urgency);
        chip.setChipBackgroundColorResource(R.color.white);
        chip.setChipStrokeWidth(2f);

        if (ct.mucDoKhanCap == 3) {
            chip.setChipStrokeColorResource(android.R.color.holo_red_dark);
            chip.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            chip.setChipStrokeColorResource(android.R.color.darker_gray);
        }

        chipGroupBlood.addView(chip);
    }
    private String getRelativeTime(String dateStr) {
        if (dateStr == null) return "Vừa xong";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateStr);
            long now = System.currentTimeMillis();
            long diff = now - date.getTime();

            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            if (days < 7) return days + " ngày trước";

            return new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "Vừa xong";
        }
    }

    private void registerToCome() {
        // 1. Hiện hộp thoại xác nhận trước khi gọi API
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận sẽ đến")
                .setMessage("Bạn chắc chắn sẽ đến cơ sở y tế này để hỗ trợ chứ?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    thucHienGoiApi();
                })
                .setNegativeButton("Để sau", null)
                .show();
    }
    private void thucHienGoiApi() {
        ApiClient.getApiService().dangKyHienMau(yeuCau.id, currentUserId)
                .enqueue(new Callback<RegistrationResponse>() {
                    @Override
                    public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ChiTietYeuCauActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            // HIỂN THỊ LỖI ĐỘNG TỪ SERVER (12 Tuần hoặc Trùng lặp)
                            try {
                                String errorMsg = response.errorBody().string();

                                new AlertDialog.Builder(ChiTietYeuCauActivity.this)
                                        .setTitle("Không thể đăng ký")
                                        .setMessage(errorMsg)
                                        .setPositiveButton("Đã hiểu", null)
                                        .show();

                            } catch (Exception e) {
                                Toast.makeText(ChiTietYeuCauActivity.this, "Lỗi khi đăng ký!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<RegistrationResponse> call, Throwable t) {
                        Toast.makeText(ChiTietYeuCauActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openListVolunteer() {
        Intent intent = new Intent(this, DanhSachTinhNguyenActivity.class);
        intent.putExtra("YEU_CAU_ID", yeuCau.id);
        intent.putExtra("LOAI_TIN", yeuCau.loaiTin);
        startActivity(intent);
    }
}
