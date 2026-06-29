package com.example.hienmau;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.XacNhanHienMau;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackingMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private XacNhanHienMau data;
    private Marker donorMarker;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateLocationRunnable;
    private int currentUserId = -1;
    private Polyline routeLine;
    private List<XacNhanHienMau> listDonors = new ArrayList<>();
    private java.util.HashMap<Integer, Marker> markerMap = new java.util.HashMap<>();
    private int selectedDonorId = -1;
    private int yeuCauId = -1;
    private RecyclerView rvDonors;
    private DonorMiniAdapter donorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_map);

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String json = prefs.getString("USER_DATA", null);
        if (json != null) {
            currentUserId = new com.google.gson.Gson().fromJson(json, NguoiDung.class).id;
        }

        yeuCauId = getIntent().getIntExtra("YEU_CAU_ID", -1);
        data = (XacNhanHienMau) getIntent().getSerializableExtra("DATA_TRACKING");

        rvDonors = findViewById(R.id.rvDonorTrackingList);
        donorAdapter = new DonorMiniAdapter();
        rvDonors.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        rvDonors.setAdapter(donorAdapter);

        findViewById(R.id.btnBackTracking).setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        MaterialButton btnConfirm = findViewById(R.id.btnConfirmFinalSuccess);
        btnConfirm.setOnClickListener(v -> finishDonation());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null) {
                selectedDonorId = (int) marker.getTag();
                donorAdapter.notifyDataSetChanged();

                for (XacNhanHienMau xn : listDonors) {
                    if (xn.nguoiHienId == selectedDonorId) {
                        ((TextView)findViewById(R.id.tvTrackingDonorName)).setText(xn.nguoiHien.hoTen);
                        updateTrackingView(marker.getPosition());
                        break;
                    }
                }
            }
            return false;
        });

        if (data == null || data.yeuCau == null) {
            Toast.makeText(this, "Dữ liệu không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra và xóa bỏ các vòng lặp cũ nếu có trước khi bắt đầu cái mới
        if (updateLocationRunnable != null) {
            handler.removeCallbacks(updateLocationRunnable);
        }

        // Tọa độ Bệnh viện
        LatLng hospital = new LatLng(data.yeuCau.viDo, data.yeuCau.kinhDo);
        mMap.addMarker(new MarkerOptions().position(hospital).title("Điểm hẹn"));

        // Khởi tạo Polyline
        routeLine = mMap.addPolyline(new PolylineOptions().color(Color.BLUE).width(10f));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hospital, 15f));

        if (data.nguoiHienId == currentUserId) {
            startLocationUpdates();
        }

        startFetchingDonorLocation();
    }

    private void startLocationUpdates() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000); // 10 giây lấy tọa độ 1 lần

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        client.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    guiToaDoLenServer(location.getLatitude(), location.getLongitude());
                }
            }
        }, Looper.getMainLooper());
    }
    private void startFetchingDonorLocation() {
        updateLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (data == null) return;
                ApiClient.getApiService().getDanhSachNguoiHien(yeuCauId).enqueue(new Callback<List<XacNhanHienMau>>() {
                    @Override
                    public void onResponse(Call<List<XacNhanHienMau>> call, Response<List<XacNhanHienMau>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // 1. Lọc danh sách những người đang đến (Status 1)
                            List<XacNhanHienMau> tempActiveDonors = new ArrayList<>();
                            for (XacNhanHienMau x : response.body()) {
                                if (x.trangThaiConfirm == 1) tempActiveDonors.add(x);
                            }

                            listDonors.clear();
                            listDonors.addAll(tempActiveDonors);
                            donorAdapter.notifyDataSetChanged();

                            // 2. Vẽ Marker cho tất cả người đang đến
                            for (XacNhanHienMau xn : listDonors) {
                                if (xn.nguoiHien.viDo != null) {
                                    LatLng pos = new LatLng(xn.nguoiHien.viDo, xn.nguoiHien.kinhDo);

                                    if (!markerMap.containsKey(xn.nguoiHienId)) {
                                        Marker m = mMap.addMarker(new MarkerOptions()
                                                .position(pos)
                                                .title(xn.nguoiHien.hoTen)
                                                .icon(bitmapDescriptorFromVector(TrackingMapActivity.this, R.drawable.directions_bike)));
                                        m.setTag(xn.nguoiHienId);
                                        markerMap.put(xn.nguoiHienId, m);
                                    } else {
                                        markerMap.get(xn.nguoiHienId).setPosition(pos);
                                    }

                                    // 3. Nếu là người đang theo dõi -> Vẽ Polyline
                                    if (xn.nguoiHienId == selectedDonorId) {
                                        updateTrackingView(pos);
                                    }
                                }
                            }

                            // Tự động chọn người đầu tiên nếu chưa chọn ai
                            if (selectedDonorId == -1 && !listDonors.isEmpty()) {
                                selectedDonorId = listDonors.get(0).nguoiHienId;
                                ((TextView)findViewById(R.id.tvTrackingDonorName)).setText(listDonors.get(0).nguoiHien.hoTen);
                            }
                        }
                    }
                    @Override public void onFailure(Call<List<XacNhanHienMau>> call, Throwable t) {}
                });
                handler.postDelayed(this, 10000);
            }
        };
        handler.post(updateLocationRunnable);
    }

    private void updateTrackingView(LatLng donorPos) {
        LatLng hospitalPos = new LatLng(data.yeuCau.viDo, data.yeuCau.kinhDo);

        // Vẽ đường nối duy nhất cho người được chọn
        List<LatLng> points = new ArrayList<>();
        points.add(donorPos);
        points.add(hospitalPos);
        if (routeLine != null) routeLine.setPoints(points);

        // Căn chỉnh camera theo người này và bệnh viện
        updateCameraBounds(donorPos, hospitalPos);
    }

    private void updateCameraBounds(LatLng p1, LatLng p2) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(p1);
        builder.include(p2);
        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
    }

    private void guiToaDoLenServer(double lat, double lng) {
        ApiClient.getApiService().updateLocation(currentUserId, lat, lng).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("LOCATION_UPDATE", "Đã cập nhật: " + lat + "," + lng);
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void finishDonation() {
        ApiClient.getApiService().medicalConfirm(data.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TrackingMapActivity.this, "Xác nhận thành công!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacks(updateLocationRunnable);
    }
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private class DonorMiniAdapter extends RecyclerView.Adapter<DonorMiniAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donor_mini_tracking, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            XacNhanHienMau xn = listDonors.get(position);
            if (xn.nguoiHien == null) return;

            holder.tvName.setText(xn.nguoiHien.hoTen);
            // Hiển thị chữ cái đầu làm avatar
            holder.tvAvatar.setText(xn.nguoiHien.hoTen.substring(0, 1).toUpperCase());

            // Highlight (Làm nổi bật) nếu người này đang được chọn theo dõi
            if (xn.nguoiHienId == selectedDonorId) {
                holder.itemView.setBackgroundResource(R.drawable.bg_status_pending); // Màu xanh nhạt bạn đã có
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_outline_gray); // Màu xám mặc định
            }

            holder.itemView.setOnClickListener(v -> {
                selectedDonorId = xn.nguoiHienId;
                notifyDataSetChanged(); // Cập nhật lại giao diện danh sách

                // Cập nhật tên ở Card dưới cùng
                ((TextView)findViewById(R.id.tvTrackingDonorName)).setText(xn.nguoiHien.hoTen);

                // Nếu Map đã có marker của người này, thực hiện zoom camera ngay
                if (markerMap.containsKey(selectedDonorId)) {
                    LatLng pos = markerMap.get(selectedDonorId).getPosition();
                    updateTrackingView(pos);
                }
            });
        }

        @Override
        public int getItemCount() { return listDonors.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName;
            ViewHolder(View iv) {
                super(iv);
                tvAvatar = iv.findViewById(R.id.tvMiniAvatar);
                tvName = iv.findViewById(R.id.tvMiniName);
            }
        }
    }
}
