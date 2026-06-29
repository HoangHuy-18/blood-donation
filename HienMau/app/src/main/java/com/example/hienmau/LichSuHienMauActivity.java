package com.example.hienmau;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hienmau.Adapter.VolunteerAdapter;
import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.XacNhanHienMau;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LichSuHienMauActivity extends AppCompatActivity {
    private RecyclerView rvLichSu;
    private TextView tvEmpty;
    private VolunteerAdapter adapter;
    private List<Object> lichSuList = new ArrayList<>();
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lich_su_hien_mau);

        userId = getIntent().getIntExtra("USER_ID", -1);

        initViews();
        loadLichSu();
    }

    private void initViews() {
        rvLichSu = findViewById(R.id.rvLichSu);
        tvEmpty = findViewById(R.id.tvEmptyLichSu);
        rvLichSu.setLayoutManager(new LinearLayoutManager(this));

        // Dùng lại VolunteerAdapter. Truyền loaiTin = -1 để Adapter biết đây là chế độ "Chỉ xem"
        adapter = new VolunteerAdapter(lichSuList, this, -1, null);
        rvLichSu.setAdapter(adapter);

        Toolbar toolbar = findViewById(R.id.toolbarLichSu);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadLichSu() {
        if (userId == -1) return;

        // Gọi API lấy danh sách bài đăng mà user đã tham gia (Bạn cần viết thêm API này ở Backend nếu chưa có)
        ApiClient.getApiService().getLichSuHienMau(userId).enqueue(new Callback<List<XacNhanHienMau>>() {
            @Override
            public void onResponse(Call<List<XacNhanHienMau>> call, Response<List<XacNhanHienMau>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lichSuList.clear();
                    List<XacNhanHienMau> data = response.body();

                    if (data.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvLichSu.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvLichSu.setVisibility(View.VISIBLE);

                        lichSuList.add("Hành trình của bạn");
                        lichSuList.addAll(data);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<XacNhanHienMau>> call, Throwable t) {
                Toast.makeText(LichSuHienMauActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
