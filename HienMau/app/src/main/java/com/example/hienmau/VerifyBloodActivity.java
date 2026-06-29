package com.example.hienmau;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hienmau.Adapter.SelectedImageAdapter;
import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyBloodActivity extends AppCompatActivity {
    private RecyclerView rvSelectedImages;
    private SelectedImageAdapter adapter;
    private List<Uri> uriList = new ArrayList<>();
    private int currentUserId = -1;
    private Spinner spnNhomMau, spnHeRh;

    // 1. Công cụ chọn nhiều ảnh (Photo Picker)
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                if (!uris.isEmpty()) {
                    uriList.addAll(uris);
                    adapter.notifyDataSetChanged();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_blood);

        // Lấy ID người dùng hiện tại
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userJson = prefs.getString("USER_DATA", null);
        if (userJson != null) {
            currentUserId = new Gson().fromJson(userJson, NguoiDung.class).id;
        }

        initViews();
        setupSpinners();
    }

    private void setupSpinners() {
        // Dữ liệu cho Nhóm máu
        String[] bloodGroups = {"A", "B", "O", "AB"};
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnNhomMau.setAdapter(bloodAdapter);

        // Dữ liệu cho Hệ Rh
        String[] rhFactors = {"+", "-"};
        ArrayAdapter<String> rhAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rhFactors);
        rhAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnHeRh.setAdapter(rhAdapter);
    }

    private void initViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarVerify);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        spnNhomMau = findViewById(R.id.spnVerifyNhomMau);
        spnHeRh = findViewById(R.id.spnVerifyHeRh);

        rvSelectedImages = findViewById(R.id.rvSelectedImages);
        rvSelectedImages.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new SelectedImageAdapter(uriList, position -> {
            uriList.remove(position);
            adapter.notifyItemRemoved(position);
        });
        rvSelectedImages.setAdapter(adapter);

        findViewById(R.id.btnAddMoreImages).setOnClickListener(v -> {
            pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        findViewById(R.id.btnSendVerify).setOnClickListener(v -> uploadImages());
    }

    private void uploadImages() {
        android.util.Log.d("CHECK_ID", "UserId gửi đi: " + currentUserId);

        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (uriList.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh bằng chứng", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Đang tải dữ liệu lên...");
        pd.setCancelable(false);
        pd.show();

        // 1. Lấy giá trị từ Spinner
        String nhomMau = spnNhomMau.getSelectedItem().toString();
        String heRh = spnHeRh.getSelectedItem().toString();

        // 2. Chuẩn bị các thành phần RequestBody
        RequestBody userIdPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUserId));
        RequestBody nhomMauPart = RequestBody.create(MediaType.parse("text/plain"), nhomMau);
        RequestBody heRhPart = RequestBody.create(MediaType.parse("text/plain"), heRh);

        // 3. Chuẩn bị danh sách ảnh
        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (int i = 0; i < uriList.size(); i++) {
            MultipartBody.Part part = prepareFilePart("images", uriList.get(i));
            if (part != null) imageParts.add(part);
        }

        // 4. Gọi API (Lưu ý: ApiService cần cập nhật thêm 2 tham số nhomMau và heRh)
        ApiClient.getApiService().uploadVerifyBlood(userIdPart, nhomMauPart, heRhPart, imageParts)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        pd.dismiss();
                        if (response.isSuccessful()) {
                            Toast.makeText(VerifyBloodActivity.this, "Gửi bằng chứng thành công! Vui lòng chờ duyệt.", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(VerifyBloodActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        pd.dismiss();
                        Toast.makeText(VerifyBloodActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), bytes);
            return MultipartBody.Part.createFormData(partName, "verify_" + System.currentTimeMillis() + ".jpg", requestFile);
        } catch (Exception e) {
            return null;
        }
    }
}
