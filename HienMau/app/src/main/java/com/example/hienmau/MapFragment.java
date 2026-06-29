package com.example.hienmau;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.DiemCoDinhResponse;
import com.example.hienmau.models.YeuCauMau;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private List<DiemCoDinhResponse> fixedPointsList = new ArrayList<>();
    private List<YeuCauMau> activeRequestsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setPadding(0, 0, 0, 280);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        checkPermissionAndGetLocation();
        loadDataFromServer();

        mMap.setOnMarkerClickListener(marker -> {
            Object data = marker.getTag();
            if (data instanceof YeuCauMau) {
                showSimpleDetailDialog((YeuCauMau) data);
            } else if (data instanceof DiemCoDinhResponse) {
                showFixedPointDialog((DiemCoDinhResponse) data);
            }
            return false;
        });
    }

    private void checkPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        mMap.setMyLocationEnabled(true);

        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f));
            } else {

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(21.0285, 105.8542), 12f));
            }
        });
    }

    private void loadDataFromServer() {
        ApiClient.getApiService().getActiveRequests(null).enqueue(new Callback<List<YeuCauMau>>() {
            @Override
            public void onResponse(Call<List<YeuCauMau>> call, Response<List<YeuCauMau>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeRequestsList = response.body();
                    loadFixedPointsFromServer();
                } else {
                    Toast.makeText(getContext(), "Lỗi tải bản tin bản đồ: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<YeuCauMau>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối máy chủ bản đồ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFixedPointsFromServer() {
        ApiClient.getApiService().getFixedPoints().enqueue(new Callback<List<DiemCoDinhResponse>>() {
            @Override
            public void onResponse(Call<List<DiemCoDinhResponse>> call, Response<List<DiemCoDinhResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fixedPointsList = response.body();
                }
                veTatCaMarkerLenMap();
            }
            @Override
            public void onFailure(Call<List<DiemCoDinhResponse>> call, Throwable t) {
                veTatCaMarkerLenMap();
            }
        });
    }

    private void veTatCaMarkerLenMap() {
        if (mMap == null) return;
        mMap.clear();

        // Danh sách lưu vết các tọa độ đã vẽ để phát hiện trùng lặp
        List<LatLng> printedPositions = new ArrayList<>();

        // 1. Kết xuất các Điểm cố định (Màu Xanh Lá)
        if (fixedPointsList != null) {
            for (DiemCoDinhResponse point : fixedPointsList) {
                if (point.viDo != 0 && point.kinhDo != 0) {
                    LatLng pos = new LatLng(point.viDo, point.kinhDo);
                    printedPositions.add(pos); // Lưu vết vị trí

                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(point.tenDiaDiem)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    if (m != null) m.setTag(point);
                }
            }
        }

        // 2. Kết xuất các bản tin yêu cầu máu (Đỏ / Xanh Dương / Cam B2B)
        if (activeRequestsList != null) {
            for (YeuCauMau item : activeRequestsList) {
                if (item.viDo != 0.0 && item.kinhDo != 0.0) {
                    double lat = item.viDo;
                    double lng = item.kinhDo;

                    LatLng originalPos = new LatLng(lat, lng);
                    for (LatLng existingPos : printedPositions) {
                        // Nếu khoảng cách giữa tọa độ cũ và mới quá nhỏ (trùng lặp bệnh viện)
                        if (Math.abs(existingPos.latitude - lat) < 0.0001 && Math.abs(existingPos.longitude - lng) < 0.0001) {
                            lat += 0.00008; // Dịch nhẹ vĩ độ lên phía trên
                            lng += 0.00008; // Dịch nhẹ kinh độ sang bên phải
                        }
                    }
                    LatLng finalPos = new LatLng(lat, lng);
                    printedPositions.add(finalPos);

                    float markerColor;
                    if (item.loaiTin == 2) {
                        markerColor = BitmapDescriptorFactory.HUE_ORANGE; // Màu Cam liên viện
                    } else if (item.loaiTin == 1) {
                        markerColor = BitmapDescriptorFactory.HUE_AZURE;  // Màu Xanh Dương sự kiện
                    } else {
                        markerColor = BitmapDescriptorFactory.HUE_RED;    // Màu Đỏ cấp cứu
                    }

                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(finalPos) // Vẽ theo tọa độ đã tối ưu dịch lệch
                            .title(item.tenBenhVien != null ? item.tenBenhVien : "Cơ sở y tế")
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                    if (m != null) m.setTag(item);
                }
            }
        }
    }

    private void showSimpleDetailDialog(YeuCauMau item) {
        String title;
        if (item.loaiTin == 2) title = "Điều phối kho máu liên viện";
        else if (item.loaiTin == 1) title = "Sự kiện hiến máu";
        else title = "Cần máu khẩn cấp";

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage("🏥 Đơn vị tiếp nhận: " + item.tenBenhVien + "\n\n📝 Nội dung: " + (item.noiDung != null && !item.noiDung.isEmpty() ? item.noiDung : "Không có lời nhắn bổ sung."))
                .setPositiveButton("Xem chi tiết", (dialog, which) -> {
                    Intent intent = new Intent(getContext(), ChiTietYeuCauActivity.class);

                    // ĐỒNG BỘ TOÀN DIỆN: Truyền gối đầu cả Object lẫn ID để thỏa mãn mọi điều kiện check Intent bên Activity
                    intent.putExtra("DATA_YEU_CAU", item);
                    intent.putExtra("YEU_CAU_ID", item.id);

                    startActivity(intent);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showFixedPointDialog(DiemCoDinhResponse point) {
        new AlertDialog.Builder(requireContext())
                .setTitle(point.tenDiaDiem)
                .setMessage("🏠 Địa chỉ: " + point.diaChi +
                        "\n\n⏰ Giờ làm việc: " + point.thoiGianLamViec +
                        "\n\n📞 Điện thoại: " + point.sdt +
                        "\n\n(Lưu ý: Nghỉ Thứ 2 và các ngày lễ chính thức)")
                .setPositiveButton("Chỉ đường", (d, w) -> {
                    String uri = String.format(java.util.Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", point.viDo, point.kinhDo, point.viDo, point.kinhDo, point.tenDiaDiem);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                })
                .setNeutralButton("Gọi điện", (d, w) -> {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + point.sdt.trim())));
                })
                .setNegativeButton("Đóng", null)
                .show();
    }
}
