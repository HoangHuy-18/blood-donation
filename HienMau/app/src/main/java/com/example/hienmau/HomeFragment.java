package com.example.hienmau;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hienmau.Adapter.YeuCauMauAdapter;
import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.YeuCauMau;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private RecyclerView rvYeuCauMau;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private YeuCauMauAdapter adapter;
    private List<YeuCauMau> listYeuCau = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private com.google.android.material.tabs.TabLayout tabLayoutHome;
    private int currentLoaiTin = 0;
    private NguoiDung currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvYeuCauMau = view.findViewById(R.id.rvYeuCauMau);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tabLayoutHome = view.findViewById(R.id.tabLayoutHome);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userJson = prefs.getString("USER_DATA", null);
        if (userJson != null) {
            currentUser = new Gson().fromJson(userJson, NguoiDung.class);
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        rvYeuCauMau.setLayoutManager(new LinearLayoutManager(getContext()));


        adapter = new YeuCauMauAdapter(listYeuCau, getContext(), new YeuCauMauAdapter.OnItemClickListener() {
            @Override
            public void onActionClick(YeuCauMau item) {

                Intent intent = new Intent(getContext(), ChiTietYeuCauActivity.class);
                intent.putExtra("DATA_YEU_CAU", item);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(YeuCauMau item) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Xác nhận xóa bài")
                        .setMessage("Bạn có chắc chắn muốn xóa bài điều phối đăng tải này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            thucHienXoa(item);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onEditClick(YeuCauMau item) {
                Intent intent = new Intent(getContext(), CreateRequestActivity.class);
                intent.putExtra("EDIT_POST_DATA", item);
                startActivity(intent);
            }

            @Override
            public void onReportClick(YeuCauMau item) {
                showReportDialog(item.id);
            }
        });
        rvYeuCauMau.setAdapter(adapter);

        tabLayoutHome.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                currentLoaiTin = tab.getPosition();
                loadData();
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        loadData();
        swipeRefresh.setOnRefreshListener(this::loadData);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getCurrentLocation();
    }
    public void getCurrentLocation() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    updateAdapterLocation(location);
                } else {
                    requestFreshLocation();
                }
            });
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
    private void thucHienXoa(YeuCauMau item) {
        ApiClient.getApiService().xoaYeuCauMau(item.id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã xóa bài đăng thành công!", Toast.LENGTH_SHORT).show();
                    listYeuCau.remove(item);
                    adapter.notifyDataSetChanged();
                    updateUI();
                } else {
                    String errorMsg = "Lỗi từ chối xóa từ máy chủ!";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Thất bại kết nối hệ thống: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateAdapterLocation(location);
                    }
                });
    }
    private void updateAdapterLocation(android.location.Location location) {
        if (adapter != null) {
            adapter.setCurrentLocation(location.getLatitude(), location.getLongitude());
        }
    }
    private void loadData() {
        swipeRefresh.setRefreshing(true);
        ApiClient.getApiService().getActiveRequests(currentLoaiTin).enqueue(new Callback<List<YeuCauMau>>() {
            @Override
            public void onResponse(Call<List<YeuCauMau>> call, Response<List<YeuCauMau>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    listYeuCau.clear();
                    listYeuCau.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateUI();
                }
            }

            @Override
            public void onFailure(Call<List<YeuCauMau>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                listYeuCau.clear();
                adapter.notifyDataSetChanged();
                updateUI();
                Toast.makeText(getContext(), "Lỗi kết nối đến máy chủ trung tâm!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReportDialog(int yeuCauId) {
        if (getContext() == null) return;

        // 1. Khởi tạo Dialog với layout custom đã chuẩn bị từ trước
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_post, null);
        builder.setView(dialogView);

        android.widget.RadioGroup rgReasons = dialogView.findViewById(R.id.rgReportReasons);
        android.widget.EditText edtDetails = dialogView.findViewById(R.id.edtReportDetails);

        builder.setPositiveButton("GỬI BÁO CÁO", (dialog, which) -> {
            // Xác định văn bản lý do dựa trên RadioButton được lựa chọn
            String lyDo = "Tin giả / Sai sự thật"; // Lý do mặc định
            int checkedId = rgReasons.getCheckedRadioButtonId();
            if (checkedId == R.id.rbSpam) {
                lyDo = "Nội dung trùng lặp / Spam";
            } else if (checkedId == R.id.rbInappropriate) {
                lyDo = "Ngôn từ không phù hợp";
            }

            String chiTiet = edtDetails.getText().toString().trim();

            executeSendReport(yeuCauId, lyDo, chiTiet);
        });

        builder.setNegativeButton("HỦY BỎ", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D32F2F"));
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
    }

    private void executeSendReport(int yeuCauId, String lyDo, String chiTiet) {
        int currentUserId = -1;
        if (currentUser != null) {
            currentUserId = currentUser.id;
        }

        if (currentUserId == -1) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để thực hiện tính năng này", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.hienmau.models.CreateBaoCaoRequest request =
                new com.example.hienmau.models.CreateBaoCaoRequest(yeuCauId, currentUserId, lyDo, chiTiet);

        ApiClient.getApiService().guiBaoCaoBaiViet(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Báo cáo thành công! Ban quản trị sẽ rà soát tin tức.", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Gửi báo cáo thất bại";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi xử lý yêu cầu báo cáo", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Mất kết nối máy chủ trung tâm!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (listYeuCau.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvYeuCauMau.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvYeuCauMau.setVisibility(View.VISIBLE);
        }
    }
}
