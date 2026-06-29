package com.example.hienmau;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.YeuCauMau;
import com.example.hienmau.models.YeuCauMauChiTiet;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateRequestActivity extends AppCompatActivity {

    private Button btnAddBlood, btnPost, btnPickStartDate, btnPickEndDate, btnPickStartTime, btnPickEndTime;
    private EditText etNote, etEventName, etSoNguoiCan;
    private TextView tvLocation, tvBloodSummary, tvBloodHint, tvHeaderTitle;
    private MaterialButtonToggleGroup toggleLoaiTin;
    private LinearLayout layoutLoaiTin, layoutEventDetails;

    private String[] listNhomMau = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
    private String[] listUrgency = {"--", "Ưu tiên", "Khẩn cấp"};

    private Double selectedLat = null;
    private Double selectedLng = null;
    private String selectedAddress = "";
    private String selectedName = "";
    private NguoiDung currentUser;
    private YeuCauMau editPost = null;
    private boolean isEditMode = false;
    private List<YeuCauMauChiTiet> tempChiTiets = new ArrayList<>();
    private String sDate = "", eDate = "", sTime = "", eTime = "";
    private com.google.android.material.textfield.TextInputLayout tilSoNguoiCan;

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("lat", 0);
                    selectedLng = result.getData().getDoubleExtra("lng", 0);
                    selectedAddress = result.getData().getStringExtra("address");
                    selectedName = result.getData().getStringExtra("name");

                    tvLocation.setText(selectedName + "\n" + selectedAddress);
                    tvLocation.setTextColor(Color.BLACK);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_request);
        initViews();
        checkEditMode();
    }

    private void initViews() {
        btnAddBlood = findViewById(R.id.btnAddBloodGroup);
        btnPost = findViewById(R.id.btnPostRequest);
        etNote = findViewById(R.id.etNote);
        etSoNguoiCan = findViewById(R.id.etSoNguoiCan);
        tilSoNguoiCan = findViewById(R.id.tilSoNguoiCan);
        tvLocation = findViewById(R.id.tvSelectedHospitalName);
        tvBloodSummary = findViewById(R.id.tvBloodSummary);
        tvBloodHint = findViewById(R.id.tvBloodHint);
        etEventName = findViewById(R.id.etEventName);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        layoutLoaiTin = findViewById(R.id.layoutLoaiTin);
        layoutEventDetails = findViewById(R.id.layoutEventDetails);
        toggleLoaiTin = findViewById(R.id.toggleLoaiTin);
        btnPickStartDate = findViewById(R.id.btnPickStartDate);
        btnPickEndDate = findViewById(R.id.btnPickEndDate);
        btnPickStartTime = findViewById(R.id.btnPickStartTime);
        btnPickEndTime = findViewById(R.id.btnPickEndTime);
        Button btnPickMap = findViewById(R.id.btnPickHospitalLocation);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUser = new Gson().fromJson(prefs.getString("USER_DATA", null), NguoiDung.class);

        if (currentUser.loaiTaiKhoan == 0) {
            layoutLoaiTin.setVisibility(View.GONE);
            tvHeaderTitle.setText("Yêu cầu cần máu khẩn cấp");
        } else {
            layoutLoaiTin.setVisibility(View.VISIBLE);
            toggleLoaiTin.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btnSuKien) updateUIByLoaiTin(1);
                    else if (checkedId == R.id.btnLienVien) updateUIByLoaiTin(2);
                    else updateUIByLoaiTin(0);
                }
            });
        }

        if (currentUser.loaiTaiKhoan == 1) {
            selectedLat = currentUser.viDo;
            selectedLng = currentUser.kinhDo;
            selectedName = currentUser.hoTen;
            selectedAddress = currentUser.diaChi;
            tvLocation.setText(selectedName + "\n" + selectedAddress);
            btnPickMap.setVisibility(View.GONE);
        }

        btnPickStartDate.setOnClickListener(v -> showDatePicker(true));
        btnPickEndDate.setOnClickListener(v -> showDatePicker(false));
        btnPickStartTime.setOnClickListener(v -> showTimePicker(true));
        btnPickEndTime.setOnClickListener(v -> showTimePicker(false));
        btnAddBlood.setOnClickListener(v -> showAddBloodDialog());
        btnPickMap.setOnClickListener(v -> mapPickerLauncher.launch(new Intent(this, MapsPickerActivity.class)));
        btnPost.setOnClickListener(v -> xuLyDangTin());
    }

    private void updateUIByLoaiTin(int loaiTin) {
        if (loaiTin == 1) {
            layoutEventDetails.setVisibility(View.VISIBLE);
            tvBloodHint.setVisibility(View.VISIBLE);
            tilSoNguoiCan.setVisibility(View.VISIBLE);
            tilSoNguoiCan.setHint("Số người dự kiến (Không bắt buộc)");
            tvHeaderTitle.setText(isEditMode ? "Chỉnh sửa sự kiện" : "Tạo sự kiện vận động");
            btnPost.setText(isEditMode ? "CẬP NHẬT SỰ KIỆN" : "ĐĂNG SỰ KIỆN");
        }
        else if (loaiTin == 2) {
            layoutEventDetails.setVisibility(View.GONE);
            tvBloodHint.setVisibility(View.GONE);
            tilSoNguoiCan.setVisibility(View.VISIBLE);
            tilSoNguoiCan.setHint("Số đơn vị máu cần đối tác hỗ trợ (Ví dụ: 10)");

            tvHeaderTitle.setText(isEditMode ? "Chỉnh sửa tin liên viện" : "Kêu gọi điều phối liên viện");
            btnPost.setText(isEditMode ? "CẬP NHẬT YÊU CẦU" : "ĐĂNG TIN LIÊN VIỆN");
        }
        else {
            layoutEventDetails.setVisibility(View.GONE);
            tvBloodHint.setVisibility(View.GONE);
            tilSoNguoiCan.setVisibility(View.VISIBLE);
            tilSoNguoiCan.setHint("Số người cần hỗ trợ");
            tvHeaderTitle.setText(isEditMode ? "Chỉnh sửa tin cấp cứu" : "Yêu cầu cần máu khẩn cấp");
            btnPost.setText(isEditMode ? "CẬP NHẬT TIN" : "ĐĂNG TIN CẤP CỨU");
        }
        updateSummaryText();
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("EDIT_POST_DATA")) {
            editPost = (YeuCauMau) getIntent().getSerializableExtra("EDIT_POST_DATA");
            isEditMode = true;

            selectedLat = editPost.viDo;
            selectedLng = editPost.kinhDo;
            selectedName = editPost.tenBenhVien;
            selectedAddress = editPost.diaChiBenhVien;
            tvLocation.setText(selectedName + "\n" + selectedAddress);


            if (editPost.loaiTin == 1) {
                if (toggleLoaiTin != null) toggleLoaiTin.check(R.id.btnSuKien);
                updateUIByLoaiTin(1);

                if (editPost.noiDung != null && editPost.noiDung.startsWith("🌟 SỰ KIỆN: ")) {
                    int firstLineEnd = editPost.noiDung.indexOf("\n");
                    if (firstLineEnd != -1) {
                        etEventName.setText(editPost.noiDung.substring(12, firstLineEnd).trim());
                    }
                    if (editPost.noiDung.contains("\n\n")) {
                        etNote.setText(editPost.noiDung.substring(editPost.noiDung.indexOf("\n\n") + 2).trim());
                    }
                } else {
                    etNote.setText(editPost.noiDung);
                }

                sDate = editPost.ngayBatDau; eDate = editPost.ngayKetThuc;
                sTime = editPost.gioBatDau; eTime = editPost.gioKetThuc;
                btnPickStartDate.setText(sDate);
                if (eDate != null && !eDate.isEmpty()) btnPickEndDate.setText(eDate);
                btnPickStartTime.setText(sTime);
                if (eTime != null && !eTime.isEmpty()) btnPickEndTime.setText(eTime);

            }
            else if (editPost.loaiTin == 2) {
                if (toggleLoaiTin != null) toggleLoaiTin.check(R.id.btnLienVien);
                updateUIByLoaiTin(2);
                etNote.setText(editPost.noiDung);
            }
            else {
                if (toggleLoaiTin != null) toggleLoaiTin.check(R.id.btnTinKhanCap);
                updateUIByLoaiTin(0);
                etNote.setText(editPost.noiDung);
            }

            if (editPost.soNguoiCan != null) {
                etSoNguoiCan.setText(String.valueOf(editPost.soNguoiCan));
            }

            if (editPost.chiTiets != null) {
                tempChiTiets.clear();
                tempChiTiets.addAll(editPost.chiTiets);
                updateSummaryText();
            }
        }
    }

    private void showDatePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y);
            if (isStart) { sDate = date; btnPickStartDate.setText(date); }
            else { eDate = date; btnPickEndDate.setText(date); }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dpd.show();
    }
    private void showTimePicker(boolean isStart) {
        // 1. Lấy đối tượng Calendar để lấy giờ hiện tại
        java.util.Calendar c = java.util.Calendar.getInstance();
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = c.get(java.util.Calendar.MINUTE);

        // 2. Truyền hour và minute vào thay cho h và m ở đoạn cuối
        new android.app.TimePickerDialog(this, (view, h, m) -> {
            // h và m ở đây là giờ/phút mà người dùng vừa CHỌN
            String time = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
            if (isStart) {
                sTime = time;
                btnPickStartTime.setText(time);
            } else {
                eTime = time;
                btnPickEndTime.setText(time);
            }
        }, hour, minute, true).show();
    }
    private void showAddBloodDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_blood, null);
        dialog.setContentView(dialogView);
        dialog.getBehavior().setPeekHeight(1600);
        ((View) dialogView.getParent()).setBackgroundColor(Color.TRANSPARENT);

        LinearLayout container = dialogView.findViewById(R.id.dialogBloodContainer);
        Button btnAddMore = dialogView.findViewById(R.id.btnAddMoreInDialog);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDialog);

        List<View> dialogRows = new ArrayList<>();

        final class RowHelper {
            void addRow(YeuCauMauChiTiet oldData) {
                View row = getLayoutInflater().inflate(R.layout.item_blood_type_row, null);
                Spinner spMau = row.findViewById(R.id.spNhomMauRow);
                Spinner spUrgencyRow = row.findViewById(R.id.spUrgencyRow);
                EditText etML = row.findViewById(R.id.etSoLuongRow);
                ImageButton btnDelete = row.findViewById(R.id.btnDeleteRow);

                spMau.setAdapter(new ArrayAdapter<>(CreateRequestActivity.this, android.R.layout.simple_spinner_item, listNhomMau));
                spUrgencyRow.setAdapter(new ArrayAdapter<>(CreateRequestActivity.this, android.R.layout.simple_spinner_item, listUrgency));

                if (oldData != null) {
                    // Ghép lại để so khớp với Spinner (Ví dụ: "A" + "+" = "A+")
                    String fullBlood = oldData.nhomMau + oldData.rh;
                    for (int i = 0; i < listNhomMau.length; i++) {
                        if (listNhomMau[i].equals(fullBlood)) {
                            spMau.setSelection(i);
                            break;
                        }
                    }
                    etML.setText(String.valueOf(oldData.soDonVi));
                    spUrgencyRow.setSelection(oldData.mucDoKhanCap - 1);
                }

                btnDelete.setOnClickListener(v -> {
                    container.removeView(row);
                    dialogRows.remove(row);
                });

                container.addView(row);
                dialogRows.add(row);
            }
        }
        RowHelper helper = new RowHelper();

        if (!tempChiTiets.isEmpty()) {
            for (YeuCauMauChiTiet data : tempChiTiets) {
                helper.addRow(data);
            }
        } else {
            helper.addRow(null);
        }

        btnAddMore.setOnClickListener(v -> helper.addRow(null));

        btnConfirm.setOnClickListener(v -> {
            tempChiTiets.clear();
            for (View row : dialogRows) {
                Spinner spMau = row.findViewById(R.id.spNhomMauRow);
                Spinner spUrgency = row.findViewById(R.id.spUrgencyRow);
                EditText etML = row.findViewById(R.id.etSoLuongRow);

                YeuCauMauChiTiet ct = new YeuCauMauChiTiet();

                // Tách "A+" thành "A" và "+"
                String selected = spMau.getSelectedItem().toString();
                ct.nhomMau = selected.substring(0, selected.length() - 1); // "A"
                ct.rh = selected.substring(selected.length() - 1); // "+"

                ct.mucDoKhanCap = spUrgency.getSelectedItemPosition() + 1;

                String ml = etML.getText().toString().trim();
                ct.soDonVi = ml.isEmpty() ? 0 : Integer.parseInt(ml);

                tempChiTiets.add(ct);
            }
            updateSummaryText();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void updateSummaryText() {
        boolean isSuKien = (currentUser.loaiTaiKhoan == 1 && toggleLoaiTin.getCheckedButtonId() == R.id.btnSuKien);
        if (tempChiTiets.isEmpty()) {
            if (isSuKien) {
                // Hiển thị thông báo thân thiện cho Sự kiện
                tvBloodSummary.setText("Mọi nhóm máu đều có thể tham gia hiến tặng");
                tvBloodSummary.setTextColor(Color.parseColor("#2E7D32")); // Màu xanh lá cho tích cực
            } else {
                tvBloodSummary.setText("Chưa có thông tin nhóm máu");
                tvBloodSummary.setTextColor(Color.parseColor("#757575")); // Màu xám mặc định
            }
            return;
        }

        tvBloodSummary.setTextColor(Color.parseColor("#757575"));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tempChiTiets.size(); i++) {
            YeuCauMauChiTiet ct = tempChiTiets.get(i);
            sb.append("• ").append(ct.nhomMau).append(ct.rh);
            if (ct.soDonVi > 0) sb.append(" (").append(ct.soDonVi).append("ml)");
            if (ct.mucDoKhanCap > 1) sb.append(" - ").append(listUrgency[ct.mucDoKhanCap - 1]);
            if (i < tempChiTiets.size() - 1) sb.append("\n");
        }
        tvBloodSummary.setText(sb.toString());
    }
    private void xuLyDangTin() {
        if (selectedLat == null || selectedLat == 0) {
            Toast.makeText(this, "Vui lòng chọn vị trí bệnh viện!", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentCheckId = toggleLoaiTin.getCheckedButtonId();
        boolean isSuKien = (currentUser.loaiTaiKhoan == 1 && currentCheckId == R.id.btnSuKien);
        boolean isLienVien = (currentUser.loaiTaiKhoan == 1 && currentCheckId == R.id.btnLienVien);

        if (!isSuKien && !isLienVien && tempChiTiets.isEmpty()) {
            Toast.makeText(this, "Tin khẩn cấp cần ít nhất 1 nhóm máu!", Toast.LENGTH_SHORT).show();
            return;
        }

        YeuCauMau request = isEditMode ? editPost : new YeuCauMau();
        request.nguoiDangId = currentUser.id;
        request.tenBenhVien = selectedName;
        request.diaChiBenhVien = selectedAddress;
        request.viDo = selectedLat;
        request.kinhDo = selectedLng;

        if (isSuKien) request.loaiTin = 1;
        else if (isLienVien) request.loaiTin = 2;
        else request.loaiTin = 0;

        request.ngayBatDau = sDate; request.ngayKetThuc = eDate;
        request.gioBatDau = sTime; request.gioKetThuc = eTime;
        request.chiTiets = tempChiTiets;

        String strSoNguoi = etSoNguoiCan.getText().toString().trim();
        if (strSoNguoi.isEmpty()) {
            request.soNguoiCan = null;
        } else {
            request.soNguoiCan = Integer.parseInt(strSoNguoi);
        }

        String note = etNote.getText().toString().trim();
        if (isSuKien) {
            String name = etEventName.getText().toString().trim();
            if (name.isEmpty()) { etEventName.setError("Nhập tên sự kiện"); return; }

            String header = "🌟 SỰ KIỆN: " + name + "\n📅 Từ: " + sDate + (eDate.isEmpty() ? "" : " đến " + eDate) +
                    "\n⏰ Giờ: " + sTime + (eTime.isEmpty() ? "" : " - " + eTime);
            request.noiDung = header + "\n\n" + note;
        } else {
            request.noiDung = note;
        }

        if (isEditMode) updateToApi(request); else postToApi(request);
    }

    private void updateToApi(YeuCauMau request) {
        ApiClient.getApiService().updateYeuCau(request.id, request).enqueue(new Callback<YeuCauMau>() {
            @Override
            public void onResponse(Call<YeuCauMau> call, Response<YeuCauMau> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateRequestActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Cập nhật thất bại!";
                        Toast.makeText(CreateRequestActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(CreateRequestActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<YeuCauMau> call, Throwable t) {
                Toast.makeText(CreateRequestActivity.this, "Lỗi kết nối đến máy chủ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postToApi(YeuCauMau request) {
        ApiClient.getApiService().postYeuCau(request).enqueue(new Callback<YeuCauMau>() {
            @Override
            public void onResponse(Call<YeuCauMau> call, Response<YeuCauMau> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateRequestActivity.this, "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Đăng tin thất bại!";
                        Toast.makeText(CreateRequestActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(CreateRequestActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<YeuCauMau> call, Throwable t) {
                Toast.makeText(CreateRequestActivity.this, "Lỗi kết nối đến máy chủ!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
