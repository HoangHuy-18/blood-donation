package com.example.hienmau;


import com.example.hienmau.api.ApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import com.example.hienmau.models.NguoiDung;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private NguoiDung currentUser;

    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private com.google.android.gms.location.LocationRequest locationRequest;
    private com.google.android.gms.location.LocationCallback locationCallback;

    private static final String TAG = "FCM_TOKEN_HUY";

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);

                if (fineLocationGranted) {
                    startLocationUpdates();
                    Toast.makeText(this, "Đã cấp quyền!", Toast.LENGTH_SHORT).show();

                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).getCurrentLocation();
                    }
                }
            });

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }
    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();

        // Quyền thông báo (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Quyền vị trí
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // THÊM: Quyền Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (!permissions.isEmpty()) {
            requestPermissionsLauncher.launch(permissions.toArray(new String[0]));
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Đã hủy quét", Toast.LENGTH_LONG).show();
                } else {
                    String cleanQrCode = result.getContents().trim();
                    xuLyCheckInBangQR(cleanQrCode);
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {

                    giaiMaQRTuUri(uri);
                }
            });
    private void giaiMaQRTuUri(android.net.Uri uri) {
        try {
            // 1. Chuyển Uri thành Bitmap
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            // 2. Chuẩn bị dữ liệu cho ZXing
            com.google.zxing.LuminanceSource source = new com.google.zxing.RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            com.google.zxing.BinaryBitmap binaryBitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));

            // 3. Tiến hành đọc mã QR
            com.google.zxing.Reader reader = new com.google.zxing.MultiFormatReader();
            com.google.zxing.Result result = reader.decode(binaryBitmap);

            // 4. Lấy được chuỗi QR thì gọi hàm Check-in cũ của Huy
            String qrCode = result.getText();
            xuLyCheckInBangQR(qrCode);

        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy mã QR trong ảnh này!", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        locationRequest = com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(30000)
                .setFastestInterval(5000);

        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null && currentUser != null) {
                        guiToaDoLenServer(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        checkAndRequestPermissions();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) return;

                        String token = task.getResult();
                        Log.d(TAG, "Token của tôi: " + token);

                        if (currentUser != null) {
                            capNhatTokenLenServer(currentUser.id, token);
                        }
                    }
                });
        // 1. Ánh xạ Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);

        // 2. Thiết lập Toolbar làm ActionBar
        setSupportActionBar(toolbar);

        // 1. Lấy thông tin người dùng từ máy
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String json = prefs.getString("USER_DATA", null);
        currentUser = new com.google.gson.Gson().fromJson(json, NguoiDung.class);

        // 2. Xử lý nút "+" dựa trên loại tài khoản
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateRequestActivity.class);

            if (currentUser.loaiTaiKhoan == 1) {
            } else {
            }

            startActivity(intent);
        });

        // 3. Xử lý các nút điều hướng khác
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottomNavigationView);
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        nav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            fabAdd.show();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_history) {
                selectedFragment = new HoatDongFragment();
            } else if (id == R.id.nav_map) {
                selectedFragment = new MapFragment();
                fabAdd.hide();
            } else if (id == R.id.nav_scan) {
                String[] options = {"Sử dụng Camera", "Chọn từ Thư viện ảnh", "Nhập mã thủ công"};

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Quét mã QR Check-in")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                // Mở Camera
                                com.journeyapps.barcodescanner.ScanOptions scanOptions = new com.journeyapps.barcodescanner.ScanOptions();
                                scanOptions.setPrompt("Quét mã QR của tình nguyện viên");
                                barcodeLauncher.launch(scanOptions);
                            } else if (which == 1) {
                                // Mở Thư viện ảnh
                                pickImageLauncher.launch("image/*");
                            } else {
                                // 2. Gọi hàm nhập mã thủ công
                                hienThiDialogNhapThuCong();
                            }
                        })
                        .show();
                return false;
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }
    private void hienThiDialogNhapThuCong() {
        final android.widget.EditText etMaQR = new android.widget.EditText(this);
        etMaQR.setHint("Ví dụ: HM-A1B2C3D4");
        etMaQR.setAllCaps(true);

        // Tạo khoảng cách (padding) cho EditText trông đẹp hơn
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50; params.rightMargin = 50; params.topMargin = 20;
        etMaQR.setLayoutParams(params);
        container.addView(etMaQR);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nhập mã định danh")
                .setMessage("Vui lòng nhập mã hiển thị dưới QR code của tình nguyện viên")
                .setView(container)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String manualCode = etMaQR.getText().toString().trim();
                    if (!manualCode.isEmpty()) {
                        // Gọi hàm xử lý Check-in như bình thường
                        xuLyCheckInBangQR(manualCode);
                    } else {
                        Toast.makeText(this, "Vui lòng không để trống mã", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void capNhatTokenLenServer(int userId, String token) {
        ApiClient.getApiService().updateToken(userId, token).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Lỗi cập nhật Token: " + t.getMessage());
            }
        });
    }
    private void guiToaDoLenServer(double lat, double lng) {
        ApiClient.getApiService().updateLocation(currentUser.id, lat, lng).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("LOCATION_MAIN", "Lỗi: " + t.getMessage());
            }
        });
    }
    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn thoát tài khoản không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void xuLyCheckInBangQR(String qrCode) {
        ApiClient.getApiService().xacNhanDenHienBangQR(qrCode).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Check-in thành công!", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 404) {
                    Toast.makeText(MainActivity.this, "Mã QR không tồn tại trên hệ thống", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 400) {
                    Toast.makeText(MainActivity.this, "Người này đã hoàn tất hiến máu rồi", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Lỗi hệ thống: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        if (currentUser == null) return;

        // 2. Gọi API để xóa Token trên Server
        ApiClient.getApiService().logout(currentUser.id).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Dù thành công hay thất bại (do mạng), ta vẫn nên cho người dùng thoát ở local
                processLocalLogout();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Khi mất mạng vẫn cho đăng xuất cục bộ
                processLocalLogout();
            }
        });
    }

    // Hàm xử lý xóa dữ liệu trong máy và chuyển màn hình
    private void processLocalLogout() {
        // Xóa SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Hủy đăng ký Topic nếu có (để chắc chắn không nhận tin chung)
        com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("all_blood_donors");

        // Chuyển về Login
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(MainActivity.this, "Hẹn gặp lại bạn!", Toast.LENGTH_SHORT).show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}