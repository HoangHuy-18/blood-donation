    package com.example.hienmau;

    import android.app.DatePickerDialog;
    import android.graphics.Color;
    import android.os.Bundle;
    import android.view.View;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.ItemTouchHelper;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

    import com.example.hienmau.Adapter.VolunteerAdapter;
    import com.example.hienmau.api.ApiClient;
    import com.example.hienmau.models.XacNhanHienMau;

    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.List;
    import java.util.Locale;

    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;
    public class DanhSachTinhNguyenActivity extends AppCompatActivity {
        private RecyclerView rv;
        private VolunteerAdapter adapter;
        private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
        private int loaiTin;
        private TextView tvEmpty;
        private android.widget.Button btnKetThuc;
        private int yeuCauId;
        private List<Object> displayList = new ArrayList<>();
        private int trangThaiBaiDangGoc = 0;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_danh_sach_tinh_nguyen);
            yeuCauId = getIntent().getIntExtra("YEU_CAU_ID", -1);
            loaiTin = getIntent().getIntExtra("LOAI_TIN", 0);

            adapter = new VolunteerAdapter(displayList, this, loaiTin, new VolunteerAdapter.OnVolunteerActionListener() {
                @Override
                public void onConfirm(XacNhanHienMau item) {
                    if (loaiTin == 2) {
                        xacNhanXoaDoiTacLienVien(item);
                    } else {
                        xacNhanNguoiHien(item);
                    }
                }

                @Override
                public void onInputResult(XacNhanHienMau item) {
                    // Bước tiếp theo: Bác sĩ nhập kết quả lâm sàng (Chỉ dành cho Sự kiện/Cấp cứu)
                    showDialogNhapKetQua(item);
                }
            });

            // Lấy ID bài đăng từ Intent
            yeuCauId = getIntent().getIntExtra("YEU_CAU_ID", -1);

            initViews();
            setupSwipeToDelete();
            loadVolunteers();
        }
        private void setupSwipeToDelete() {
            ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    Object item = displayList.get(position);
                    if (item instanceof XacNhanHienMau) {
                        xacNhanXoaTinhNguyenVien((XacNhanHienMau) item, position);
                    } else {
                        adapter.notifyItemChanged(position);
                    }
                }

                @Override
                public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                        int actionState, boolean isCurrentlyActive) {

                    // Chỉ vẽ nếu đang vuốt sang trái (dX < 0)
                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                        View itemView = viewHolder.itemView;
                        android.graphics.Paint paint = new android.graphics.Paint();

                        // 1. Vẽ nền màu đỏ
                        paint.setColor(android.graphics.Color.parseColor("#D32F2F"));
                        android.graphics.RectF background = new android.graphics.RectF(
                                (float) itemView.getRight() + dX, (float) itemView.getTop(),
                                (float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background, paint);

                        // 2. Vẽ Icon thùng rác
                        android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(
                                DanhSachTinhNguyenActivity.this, android.R.drawable.ic_menu_delete);
                        if (icon != null) {
                            icon.setTint(android.graphics.Color.WHITE);
                            int itemHeight = itemView.getBottom() - itemView.getTop();
                            int intrinsicWidth = icon.getIntrinsicWidth();
                            int intrinsicHeight = icon.getIntrinsicHeight();

                            int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                            int iconMargin = (itemHeight - intrinsicHeight) / 2;
                            int iconLeft = itemView.getRight() - iconMargin - intrinsicWidth;
                            int iconRight = itemView.getRight() - iconMargin;
                            int iconBottom = iconTop + intrinsicHeight;

                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            };

            new ItemTouchHelper(callback).attachToRecyclerView(rv);
        }
        private void xacNhanNguoiHien(XacNhanHienMau item) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận người hiến")
                    .setMessage("Bạn xác nhận tình nguyện viên này sẽ đến hiến máu chứ?")
                    .setPositiveButton("Xác nhận", (d, w) -> {
                        ApiClient.getApiService().xacNhanDenHien(item.id).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), "Đã chốt người hiến!", Toast.LENGTH_SHORT).show();
                                    loadVolunteers();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {}
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }

        private void xacNhanXoaDoiTacLienVien(XacNhanHienMau item) {
            String tenVienDoiTac = (item.nguoiHien != null) ? item.nguoiHien.hoTen : "Cơ sở y tế đối tác";

            // Bốc chuẩn xác ID giao dịch thực tế lưu trong bản ghi
            final int idGiaoDichLienVien = item.id;

            android.util.Log.d("DEBUG_B2B", "ID thuc te boc tu item: " + idGiaoDichLienVien);

            new AlertDialog.Builder(this)
                    .setTitle("Hủy bỏ điều phối liên viện")
                    .setMessage("Bạn có chắc chắn muốn từ chối và gỡ bỏ " + tenVienDoiTac + " khỏi danh sách viện trợ nguồn máu này không?")
                    .setPositiveButton("XÁC NHẬN HỦY", (dialog, which) -> {

                        int pos = displayList.indexOf(item);
                        if (pos != -1) {
                            if (idGiaoDichLienVien == 0) {
                                Toast.makeText(this, "Lỗi đồng bộ dữ liệu: ID bằng 0. Vui lòng Refresh lại danh sách!", Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Gán ép chắc chắn ID giao dịch thực tế vào thực thể trước khi đẩy qua Retrofit
                            item.id = idGiaoDichLienVien;
                            thucHienXoaApi(item, pos);
                        }
                    })
                    .setNegativeButton("Quay lại", null)
                    .setCancelable(false)
                    .show();
        }

        private void xacNhanXoaTinhNguyenVien(XacNhanHienMau xn, int position) {
            new AlertDialog.Builder(this)
                    .setTitle("Hủy yêu cầu")
                    .setMessage("Bạn có chắc chắn muốn xóa tình nguyện viên này khỏi danh sách không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        // Gọi API hủy đăng ký
                        thucHienXoaApi(xn, position);
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> {
                        // Nếu người dùng bấm Hủy, khôi phục lại item trong RecyclerView
                        adapter.notifyItemChanged(position);
                    })
                    .setCancelable(false)
                    .show();
        }

        @Override
        protected void onResume() {
            super.onResume();
            loadVolunteers();
        }
        private void thucHienXoaApi(XacNhanHienMau xn, int position) {
            ApiClient.getApiService().xoaTinhNguyenVien(xn.id, loaiTin).enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(DanhSachTinhNguyenActivity.this, "Đã hủy bỏ liên kết điều phối thành công!", Toast.LENGTH_SHORT).show();
                        loadVolunteers(); // Gọi lại hàm này để bốc lại danh sách mới từ DB lên
                    } else {
                        adapter.notifyItemChanged(position);
                        try {
                            String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Từ chối xóa";
                            Toast.makeText(DanhSachTinhNguyenActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(DanhSachTinhNguyenActivity.this, "Lỗi phản hồi hệ thống!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    adapter.notifyItemChanged(position);
                    Toast.makeText(DanhSachTinhNguyenActivity.this, "Mất kết nối mạng điều phối!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        private void initViews() {
            rv = findViewById(R.id.rvTinhNguyenVien);
            tvEmpty = findViewById(R.id.tvEmptyDS);
            rv.setLayoutManager(new LinearLayoutManager(this));
            swipeRefresh = findViewById(R.id.swipeRefreshDS);
            btnKetThuc = findViewById(R.id.btnKetThucSuKien);
            rv.setAdapter(adapter);

            swipeRefresh.setColorSchemeColors(Color.parseColor("#D32F2F"));
            swipeRefresh.setOnRefreshListener(this::loadData);

            if (loaiTin == 1) {
                btnKetThuc.setVisibility(View.VISIBLE);
                if (trangThaiBaiDangGoc == 2) {
                    btnKetThuc.setEnabled(false);
                    btnKetThuc.setText("SỰ KIỆN ĐÃ KẾT THÚC");
                    btnKetThuc.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                } else {
                    btnKetThuc.setEnabled(true);
                    btnKetThuc.setOnClickListener(v -> xacNhanKetThucSuKien());
                }
            } else {
                btnKetThuc.setVisibility(View.GONE);
            }

            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarDS);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> finish());
            }
        }
        private void showDialogNhapKetQua(XacNhanHienMau item) {
            com.google.android.material.bottomsheet.BottomSheetDialog sheetDialog =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_checkin_result, null);
            sheetDialog.setContentView(dialogView);

            TextView tvTenNguoiHien = dialogView.findViewById(R.id.tvTenNguoiHienDialog);
            TextView tvSdt = dialogView.findViewById(R.id.tvSdtDialog);
            TextView tvAvatar = dialogView.findViewById(R.id.tvAvatarDialog);
            TextView tvYTeKhuyenNghi = dialogView.findViewById(R.id.tvYTeKhuyenNghiAlert);

            if (item.nguoiHien != null) {
                String hoTen = item.nguoiHien.hoTen;
                tvTenNguoiHien.setText(hoTen);
                tvSdt.setText("SĐT: " + item.nguoiHien.sdt);
                if (hoTen != null && !hoTen.isEmpty()) {
                    tvAvatar.setText(hoTen.substring(0, 1).toUpperCase());
                }
            }

            android.widget.EditText etChieuCao = dialogView.findViewById(R.id.etChieuCaoResult);
            android.widget.EditText etCanNang = dialogView.findViewById(R.id.etCanNangResult);
            android.widget.EditText etHuyetApMax = dialogView.findViewById(R.id.etHuyetApMaxResult);
            android.widget.EditText etHuyetApMin = dialogView.findViewById(R.id.etHuyetApMinResult);

            android.widget.Spinner spNhomMau = dialogView.findViewById(R.id.spNhomMauResult);
            android.widget.Spinner spRh = dialogView.findViewById(R.id.spRhResult);
            android.widget.EditText etLuongMau = dialogView.findViewById(R.id.etLuongMauResult);
            android.widget.Button btnHoanTat = dialogView.findViewById(R.id.btnHoanTatResult);
            android.widget.Button btnKhongHien = dialogView.findViewById(R.id.btnKhongThamGiaResult);
            android.widget.Button btnHuy = dialogView.findViewById(R.id.btnHuyResult);

            String[] nhomMauArr = {"A", "B", "O", "AB"};
            String[] rhArr = {"+", "-"};
            spNhomMau.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nhomMauArr));
            spRh.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rhArr));

            if (item.nguoiHien != null && item.nguoiHien.caNhan != null) {
                if (item.nguoiHien.caNhan.chieuCao != null && item.nguoiHien.caNhan.chieuCao > 0) {
                    etChieuCao.setText(String.valueOf(item.nguoiHien.caNhan.chieuCao));
                }
                if (item.nguoiHien.caNhan.canNang != null && item.nguoiHien.caNhan.canNang > 0) {
                    etCanNang.setText(String.valueOf(item.nguoiHien.caNhan.canNang));
                }
            }

            if (item.trangThaiConfirm >= 2) {
                btnHoanTat.setText("CẬP NHẬT");
                btnHoanTat.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0288D1"))); // Màu xanh Edit

                if (item.huyetApTamThu != null && item.huyetApTamThu > 0) etHuyetApMax.setText(String.valueOf(item.huyetApTamThu));
                if (item.huyetApTamTruong != null && item.huyetApTamTruong > 0) etHuyetApMin.setText(String.valueOf(item.huyetApTamTruong));
                if (item.luongMau > 0) etLuongMau.setText(String.valueOf(item.luongMau));

                if (item.nhomMauXacNhan != null) {
                    for (int i = 0; i < nhomMauArr.length; i++) {
                        if (nhomMauArr[i].equalsIgnoreCase(item.nhomMauXacNhan)) { spNhomMau.setSelection(i); break; }
                    }
                }
                if (item.rhXacNhan != null) {
                    for (int i = 0; i < rhArr.length; i++) {
                        if (rhArr[i].equalsIgnoreCase(item.rhXacNhan)) { spRh.setSelection(i); break; }
                    }
                }
            }

            if (trangThaiBaiDangGoc == 2) {
                btnHoanTat.setEnabled(false);
                btnHoanTat.setText("ĐÃ KHÓA SỔ");
                btnHoanTat.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

                btnKhongHien.setEnabled(false);
                btnKhongHien.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

                etLuongMau.setEnabled(false);
                etChieuCao.setEnabled(false);
                etCanNang.setEnabled(false);
                etHuyetApMax.setEnabled(false);
                etHuyetApMin.setEnabled(false);
                spNhomMau.setEnabled(false);
                spRh.setEnabled(false);
                tvYTeKhuyenNghi.setText("🔒 Chiến dịch đã kết thúc và đóng sổ báo cáo.");
                tvYTeKhuyenNghi.setBackgroundColor(Color.parseColor("#EEEEEE"));
                tvYTeKhuyenNghi.setTextColor(Color.parseColor("#616161"));
            }

            android.text.TextWatcher yTeWatcher = new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    if (trangThaiBaiDangGoc == 2) return;
                    try {
                        String strMax = etHuyetApMax.getText().toString().trim();
                        String strMin = etHuyetApMin.getText().toString().trim();
                        String strCc = etChieuCao.getText().toString().trim();
                        String strCn = etCanNang.getText().toString().trim();

                        if (!strMax.isEmpty() && !strMin.isEmpty()) {
                            int maxHA = Integer.parseInt(strMax);
                            int minHA = Integer.parseInt(strMin);

                            if (maxHA > 140 || maxHA < 90 || minHA > 90 || minHA < 60) {
                                tvYTeKhuyenNghi.setVisibility(View.VISIBLE);
                                tvYTeKhuyenNghi.setText("🚨 CẢNH BÁO LÂM SÀNG: Chỉ số Huyết áp nằm ngoài vùng an toàn!");
                                tvYTeKhuyenNghi.setBackgroundColor(Color.parseColor("#FFEBEE"));
                                tvYTeKhuyenNghi.setTextColor(Color.parseColor("#C62828"));
                                btnHoanTat.setEnabled(false);
                                btnHoanTat.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
                                return;
                            }
                        }

                        btnHoanTat.setEnabled(true);
                        if (item.trangThaiConfirm >= 2) {
                            btnHoanTat.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0288D1")));
                        } else {
                            btnHoanTat.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                        }

                        if (!strCc.isEmpty() && !strCn.isEmpty()) {
                            int chieuCaoCm = Integer.parseInt(strCc);
                            double canNangKg = Double.parseDouble(strCn);

                            double chieuCaoMet = (double) chieuCaoCm / 100;
                            double bmi = canNangKg / (chieuCaoMet * chieuCaoMet);

                            if (bmi < 18.5) {
                                tvYTeKhuyenNghi.setVisibility(View.VISIBLE);
                                tvYTeKhuyenNghi.setText(String.format(Locale.getDefault(), "⚠️ Thể trạng gầy (BMI: %.1f < 18.5). KHUYẾN NGHỊ LÂM SÀNG: Tiếp nhận thể tích máu tối đa an toàn là 250ml.", bmi));
                                tvYTeKhuyenNghi.setBackgroundColor(Color.parseColor("#FFF3E0"));
                                tvYTeKhuyenNghi.setTextColor(Color.parseColor("#E65100"));
                            } else if (bmi >= 18.5 && bmi <= 24.9) {
                                tvYTeKhuyenNghi.setVisibility(View.VISIBLE);
                                tvYTeKhuyenNghi.setText(String.format(Locale.getDefault(), "✅ Thể trạng lý tưởng (BMI: %.1f). Có thể tiếp nhận linh hoạt thể tích máu: 350ml hoặc 450ml dựa trên chỉ tiêu.", bmi));
                                tvYTeKhuyenNghi.setBackgroundColor(Color.parseColor("#E8F5E9"));
                                tvYTeKhuyenNghi.setTextColor(Color.parseColor("#2E7D32"));
                            } else {
                                tvYTeKhuyenNghi.setVisibility(View.VISIBLE);
                                tvYTeKhuyenNghi.setText(String.format(Locale.getDefault(), "ℹ️ Thể trạng thừa cân (BMI: %.1f). Bác sĩ lưu ý kiểm tra thêm các bệnh nền trước khi chốt rút máu.", bmi));
                                tvYTeKhuyenNghi.setBackgroundColor(Color.parseColor("#E3F2FD"));
                                tvYTeKhuyenNghi.setTextColor(Color.parseColor("#1565C0"));
                            }
                        } else {
                            tvYTeKhuyenNghi.setText("Môi trường sẵn sàng. Vui lòng hoàn thành nhập dữ liệu đo đạc sức khỏe.");
                            tvYTeKhuyenNghi.setBackgroundColor(Color.parseColor("#F5F5F5"));
                            tvYTeKhuyenNghi.setTextColor(Color.parseColor("#757575"));
                        }
                    } catch (Exception ignored) {}
                }
            };

            etChieuCao.addTextChangedListener(yTeWatcher);
            etCanNang.addTextChangedListener(yTeWatcher);
            etHuyetApMax.addTextChangedListener(yTeWatcher);
            etHuyetApMin.addTextChangedListener(yTeWatcher);

            final Calendar calendar = Calendar.getInstance();
            Button btnPickDate = dialogView.findViewById(R.id.btnPickNgayHien);

            String ngayHienHienTai = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            btnPickDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));

            btnPickDate.setOnClickListener(vClick -> {

                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                java.util.Date dateMin = null;
                java.util.Date dateMax = null;

                try {
                    if (item.yeuCau != null && item.yeuCau.ngayBatDau != null && item.yeuCau.ngayKetThuc != null) {
                        dateMin = parser.parse(item.yeuCau.ngayBatDau);
                        dateMax = parser.parse(item.yeuCau.ngayKetThuc);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int startYear = calendar.get(Calendar.YEAR);
                int startMonth = calendar.get(Calendar.MONTH);
                int startDay = calendar.get(Calendar.DAY_OF_MONTH);

                if (dateMin != null) {
                    Calendar calMin = Calendar.getInstance();
                    calMin.setTime(dateMin);
                    startYear = calMin.get(Calendar.YEAR);
                    startMonth = calMin.get(Calendar.MONTH);
                    startDay = calMin.get(Calendar.DAY_OF_MONTH);
                }

                DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, day) -> {
                    calendar.set(year, month, day);
                    String selectedDateShow = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    String selectedDateApi = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
                    btnPickDate.setText(selectedDateShow);
                    btnPickDate.setTag(selectedDateApi);
                }, startYear, startMonth, startDay);

                if (dateMin != null && dateMax != null) {
                    datePickerDialog.getDatePicker().setMinDate(dateMin.getTime());

                    datePickerDialog.getDatePicker().setMaxDate(dateMax.getTime());
                }

                datePickerDialog.show();
            });

            btnHuy.setOnClickListener(vClick -> sheetDialog.dismiss());

            btnKhongHien.setOnClickListener(vClick -> {
                new AlertDialog.Builder(this)
                        .setTitle("Đánh dấu không hiến")
                        .setMessage("Bạn xác nhận tình nguyện viên này không tham gia hiến máu thực tế (Vắng mặt hoặc không đủ điều kiện rút máu)?")
                        .setPositiveButton("Xác nhận", (vDialog, vWhich) -> {
                            ApiClient.getApiService().danhDauKhongHien(item.id).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(DanhSachTinhNguyenActivity.this, "Đã cập nhật trạng thái không tham gia!", Toast.LENGTH_SHORT).show();
                                        sheetDialog.dismiss();
                                        loadVolunteers();
                                    }
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(DanhSachTinhNguyenActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Quay lại", null)
                        .show();
            });

            btnHoanTat.setOnClickListener(vClick -> {
                String strCc = etChieuCao.getText().toString().trim();
                String strCn = etCanNang.getText().toString().trim();
                String strMax = etHuyetApMax.getText().toString().trim();
                String strMin = etHuyetApMin.getText().toString().trim();
                String luongStr = etLuongMau.getText().toString().trim();

                if (strCc.isEmpty() || strCn.isEmpty() || strMax.isEmpty() || strMin.isEmpty() || luongStr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ tất cả các chỉ số sức khỏe y tế!", Toast.LENGTH_LONG).show();
                    return;
                }

                int chieuCaoVal, haMaxVal, haMinVal, luongMauVal;
                double canNangVal;

                try {
                    chieuCaoVal = Integer.parseInt(strCc);
                    if (chieuCaoVal <= 0 || chieuCaoVal > 250) {
                        etChieuCao.setError("Chiều cao không hợp lệ (Phải từ 50 - 250 cm)");
                        return;
                    }
                } catch (NumberFormatException e) {
                    etChieuCao.setError("Chiều cao phải là một số nguyên");
                    return;
                }

                try {
                    canNangVal = Double.parseDouble(strCn);
                    if (canNangVal <= 0 || canNangVal > 200) {
                        etCanNang.setError("Cân nặng không hợp lệ (Phải từ 20 - 200 kg)");
                        return;
                    }
                } catch (NumberFormatException e) {
                    etCanNang.setError("Cân nặng phải là một số hợp lệ");
                    return;
                }

                try {
                    haMaxVal = Integer.parseInt(strMax);
                    if (haMaxVal <= 0) {
                        etHuyetApMax.setError("Huyết áp tâm thu phải là số dương");
                        return;
                    }
                } catch (NumberFormatException e) {
                    etHuyetApMax.setError("Yêu cầu nhập số nguyên");
                    return;
                }

                try {
                    haMinVal = Integer.parseInt(strMin);
                    if (haMinVal <= 0) {
                        etHuyetApMin.setError("Huyết áp tâm trương phải là số dương");
                        return;
                    }
                } catch (NumberFormatException e) {
                    etHuyetApMin.setError("Yêu cầu nhập số nguyên");
                    return;
                }

                try {
                    luongMauVal = Integer.parseInt(luongStr);
                    if (luongMauVal != 250 && luongMauVal != 350 && luongMauVal != 450) {
                        etLuongMau.setError("Thể tích máu thực thu quy chuẩn phải là: 250, 350 hoặc 450 ml");
                        return;
                    }
                } catch (NumberFormatException e) {
                    etLuongMau.setError("Lượng máu phải là một số nguyên quy chuẩn");
                    return;
                }

                double chieuCaoMet = (double) chieuCaoVal / 100;
                double bmi = canNangVal / (chieuCaoMet * chieuCaoMet);
                if (bmi < 18.5 && luongMauVal > 250) {
                    etLuongMau.setError("Vượt ngưỡng! Thể trạng gầy (BMI < 18.5) chỉ được phép hiến 250ml.");
                    return;
                }

                com.example.hienmau.models.KetQuaHienMau request = new com.example.hienmau.models.KetQuaHienMau();
                request.xacNhanId = item.id;
                request.ngayHien = btnPickDate.getTag() != null ? btnPickDate.getTag().toString() : ngayHienHienTai;
                request.nhomMau = spNhomMau.getSelectedItem().toString();
                request.heRh = spRh.getSelectedItem().toString();
                request.luongMau = luongMauVal;
                request.chieuCao = chieuCaoVal;
                request.canNang = canNangVal;
                request.huyetApTamThu = haMaxVal;
                request.huyetApTamTruong = haMinVal;

                ApiClient.getApiService().hoanTatHienMau(request).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(DanhSachTinhNguyenActivity.this, "Đã lưu kết quả lâm sàng thành công!", Toast.LENGTH_SHORT).show();
                            sheetDialog.dismiss();
                            loadVolunteers();
                        } else {
                            Toast.makeText(DanhSachTinhNguyenActivity.this, "Lỗi phản hồi từ máy chủ!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(DanhSachTinhNguyenActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            sheetDialog.show();
        }
        private void xacNhanKetThucSuKien() {
            // Đếm xem trong danh sách hiện tại có bao nhiêu người đã Hoàn tất (Status 2)
            int countSuccess = 0;
            for (Object obj : displayList) {
                if (obj instanceof XacNhanHienMau) {
                    if (((XacNhanHienMau) obj).trangThaiConfirm == 2) {
                        countSuccess++;
                    }
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Kết thúc sự kiện")
                    .setMessage("Hiện có " + countSuccess + " tình nguyện viên hiến máu thành công.\n\n" +
                            "Khi kết thúc:\n" +
                            "- Bài đăng sẽ được đóng.\n" +
                            "- Gửi thông báo tri ân đến " + countSuccess + " người này.\n" +
                            "- Những người chưa hiến sẽ không nhận được thông báo.")
                    .setPositiveButton("XÁC NHẬN KẾT THÚC", (dialog, which) -> {
                        ApiClient.getApiService().ketThucSuKien(yeuCauId).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(DanhSachTinhNguyenActivity.this, "Sự kiện đã kết thúc!", Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(DanhSachTinhNguyenActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("HỦY", null)
                    .show();
        }
        private void loadData() {
            // Hiện vòng xoay loading
            swipeRefresh.setRefreshing(true);
            loadVolunteers();
        }
        private void loadVolunteers() {
            if (yeuCauId == -1) return;

            ApiClient.getApiService().getDanhSachNguoiHien(yeuCauId).enqueue(new Callback<List<XacNhanHienMau>>() {
                @Override
                public void onResponse(Call<List<XacNhanHienMau>> call, Response<List<XacNhanHienMau>> response) {
                    swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null) {
                        displayList.clear();
                        List<XacNhanHienMau> fullData = response.body();

                        List<XacNhanHienMau> daXacNhan = new ArrayList<>();
                        List<XacNhanHienMau> dangCho = new ArrayList<>();

                        for (XacNhanHienMau x : fullData) {
                            if (x.trangThaiConfirm >= 1) daXacNhan.add(x);
                            else dangCho.add(x);
                        }

                        if (!daXacNhan.isEmpty()) {
                            displayList.add("Đã xác nhận");
                            displayList.addAll(daXacNhan);
                        }
                        if (!dangCho.isEmpty()) {
                            displayList.add("Danh sách tình nguyện viên");
                            displayList.addAll(dangCho);
                        }

                        if (!fullData.isEmpty() && fullData.get(0).yeuCau != null) {
                            trangThaiBaiDangGoc = fullData.get(0).yeuCau.trangThai;
                            if(trangThaiBaiDangGoc == 2) btnKetThuc.setVisibility(View.GONE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
                @Override
                public void onFailure(Call<List<XacNhanHienMau>> call, Throwable t) {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(DanhSachTinhNguyenActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
