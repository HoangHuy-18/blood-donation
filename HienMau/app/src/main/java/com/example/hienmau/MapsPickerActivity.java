package com.example.hienmau;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private SearchView searchViewMap;
    private LatLng selectedLatLng;
    private String selectedAddress = "";
    private String selectedName = "";
    private TextView tvCurrentAddress;
    private SimpleCursorAdapter suggestionAdapter;
    private final android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_picker);

        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapPicker);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> {
            if (selectedLatLng != null) {
                hideKeyboard();

                Intent intent = new Intent();
                intent.putExtra("lat", selectedLatLng.latitude);
                intent.putExtra("lng", selectedLatLng.longitude);
                intent.putExtra("address", selectedAddress);
                intent.putExtra("name", selectedName);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this, "Vui lòng chọn một điểm!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 1. Cấu hình map: Nút + -
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // 2. Vị trí mặc định: Hà Nội
        LatLng haNoi = new LatLng(21.0285712, 105.817639);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(haNoi, 15f));

        // 3. Khởi tạo tìm kiếm và định vị
        setupSearch();
        enableMyLocation();

        mMap.setOnPoiClickListener(poi -> {
            selectedLatLng = poi.latLng;
            selectedName = poi.name; // ĐÂY CHÍNH LÀ TÊN BỆNH VIỆN

            // Lấy thêm địa chỉ từ tọa độ của POI đó
            selectedAddress = getAddressFromLatLng(poi.latLng);

            // Cập nhật Marker và UI
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(poi.latLng).title(poi.name));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(poi.latLng));
        });

        mMap.setOnMapClickListener(latLng -> updateMarker(latLng));
    }
    private void setupSearch() {
        searchViewMap = findViewById(R.id.searchViewMap);

        // Khởi tạo Adapter cho gợi ý
        suggestionAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                null,
                new String[]{"address"},
                new int[]{android.R.id.text1},
                0
        );
        searchViewMap.setSuggestionsAdapter(suggestionAdapter);

        searchViewMap.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                hideKeyboard();
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 3) {
                    showSuggestions(newText);
                } else if (newText.isEmpty()) {
                    suggestionAdapter.changeCursor(null);
                }
                return true;
            }
        });

        searchViewMap.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) { return true; }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = (Cursor) suggestionAdapter.getItem(position);
                int index = cursor.getColumnIndex("address");
                String selectedAddr = cursor.getString(index);
                searchViewMap.setQuery(selectedAddr, true); // Submit luôn khi chọn
                return true;
            }
        });
    }

    private void showSuggestions(String query) {
        // Kiểm tra query trống như Huy nói
        if (query == null || query.trim().isEmpty()) {
            suggestionAdapter.changeCursor(null);
            return;
        }

        // Hủy yêu cầu tìm kiếm trước đó nếu người dùng vẫn đang gõ
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        // Tạo yêu cầu mới: Đợi 500ms sau khi ngừng gõ mới chạy Geocoder
        searchRunnable = () -> {
            MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID, "address"});
            Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
            try {
                // Tìm kiếm kèm context Hà Nội
                List<Address> list = geocoder.getFromLocationName(query + ", Hà Nội", 5);
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        cursor.addRow(new Object[]{i, list.get(i).getAddressLine(0)});
                    }
                }
                // Chạy trên luồng chính để cập nhật giao diện
                runOnUiThread(() -> suggestionAdapter.changeCursor(cursor));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        searchHandler.postDelayed(searchRunnable, 500); // Đợi 500ms
    }

    private void searchLocation(String location) {
        String fullQuery = location + ", Hà Nội, Việt Nam";
        Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
        try {
            List<Address> addressList = geocoder.getFromLocationName(fullQuery, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                updateMarker(latLng);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15f));
                        updateMarker(current);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }
    private void updateMarker(LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        selectedLatLng = latLng;
        selectedAddress = getAddressFromLatLng(latLng);
        updateAddressAndName(latLng);
        tvCurrentAddress.setText(selectedName + "\n" + selectedAddress);
        tvCurrentAddress.setText(selectedAddress);
    }

    private void updateAddressAndName(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);

                // Lấy địa chỉ đầy đủ
                selectedAddress = addr.getAddressLine(0);

                // Lấy tên địa điểm (Ví dụ: "Bệnh viện Bạch Mai")
                selectedName = addr.getFeatureName();

                // Nếu Feature Name chỉ là số nhà hoặc trùng với địa chỉ,
                // ta có thể gán mặc định nếu muốn
                if (selectedName == null) {
                    selectedName = "Địa điểm đã chọn";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) { e.printStackTrace(); }
        return "Không tìm thấy địa chỉ";
    }

    private void hideKeyboard() {
        android.view.View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
