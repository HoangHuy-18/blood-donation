package com.example.hienmau;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hienmau.Adapter.HoatDongAdapter;
import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.XacNhanHienMau;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HoatDongFragment extends Fragment {
    private TextView tabDiChuyen, tabSuKien, tabDaDi, tabBaiDang;
    private RecyclerView rvHoatDong;
    private View layoutEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private List<XacNhanHienMau> fullList = new ArrayList<>();
    private List<XacNhanHienMau> filterList = new ArrayList<>();
    private List<com.example.hienmau.models.YeuCauMau> myRawPosts = new ArrayList<>();
    private HoatDongAdapter adapter;
    private int currentUserId = -1;
    private int selectedStatus = 0; // 0: Cấp cứu, 1: Sự kiện, 2: Đã đi, 3: Bài đăng

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hoat_dong, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View
        tabDiChuyen = view.findViewById(R.id.tabDiChuyen);
        tabSuKien = view.findViewById(R.id.tabSuKien);
        tabDaDi = view.findViewById(R.id.tabDaDi);
        tabBaiDang = view.findViewById(R.id.tabBaiDang);
        rvHoatDong = view.findViewById(R.id.rvHoatDong);
        layoutEmpty = view.findViewById(R.id.layoutEmptyHoatDong);
        swipeRefresh = view.findViewById(R.id.swipeRefreshHoatDong);

        if (savedInstanceState != null) {
            selectedStatus = savedInstanceState.getInt("CURRENT_TAB", 0);
        }

        // 2. Lấy dữ liệu User
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("USER_DATA", null);
        if (json != null) {
            currentUserId = new Gson().fromJson(json, NguoiDung.class).id;
        }

        rvHoatDong.setLayoutManager(new LinearLayoutManager(getContext()));
        updateTabUI(selectedStatus);

        tabDiChuyen.setOnClickListener(v -> updateTabUI(0));
        tabSuKien.setOnClickListener(v -> updateTabUI(1));
        tabDaDi.setOnClickListener(v -> updateTabUI(2));
        tabBaiDang.setOnClickListener(v -> updateTabUI(3));

        // 1. Cấu hình chức năng vuốt (Chỉ vuốt trái/phải, không kéo thả lên xuống)
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Chỉ cho phép vuốt đối với các tab đang chờ di chuyển / chờ xử lý (Tab 0 và Tab 1)
                if (selectedStatus == 2 || selectedStatus == 3) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                XacNhanHienMau item = filterList.get(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Hủy lịch hẹn hiến máu")
                        .setMessage("Bạn chắc chắn hủy ca này nữa?")
                        .setCancelable(false)
                        .setPositiveButton("Xác nhận hủy", (dialog, which) -> {
                            thucHienHuyTrenServer(item, position); // Truyền thêm position vào để xử lý khôi phục UI nếu lỗi
                        })
                        .setNegativeButton("Quay lại", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // Khôi phục lại UI ban đầu nếu bấm Quay lại
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                // ĐỒNG BỘ ĐỒ HỌA: Vẽ nền đỏ và Icon thùng rác y hệt như màn hình danh sách tinh nguyện
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;
                    android.graphics.Paint paint = new android.graphics.Paint();

                    // Vẽ nền màu đỏ tươi (#D32F2F)
                    paint.setColor(android.graphics.Color.parseColor("#D32F2F"));
                    android.graphics.RectF background = new android.graphics.RectF(
                            (float) itemView.getRight() + dX, (float) itemView.getTop(),
                            (float) itemView.getRight(), (float) itemView.getBottom());
                    c.drawRect(background, paint);

                    // Vẽ Icon Delete từ hệ thống thư viện Google Android
                    android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(
                            requireContext(), android.R.drawable.ic_menu_delete);
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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(rvHoatDong);

        loadData();
        swipeRefresh.setOnRefreshListener(this::loadData);
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("CURRENT_TAB", selectedStatus);
    }

    private void setupAdapter() {
        adapter = new HoatDongAdapter(filterList, getContext(), currentUserId, selectedStatus, new HoatDongAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(XacNhanHienMau item) {
                if (item.yeuCau == null) return;

                boolean isOwner = (item.yeuCau.nguoiDangId == currentUserId);
                boolean isDonor = (item.nguoiHienId == currentUserId);

                if (isOwner) {

                    if (selectedStatus == 3) {

                        Intent intent = new Intent(getContext(), DanhSachTinhNguyenActivity.class);
                        intent.putExtra("YEU_CAU_ID", item.yeuCauId);
                        intent.putExtra("LOAI_TIN", item.yeuCau.loaiTin);
                        startActivity(intent);

                    } else if (selectedStatus == 0) {

                        Intent intent = new Intent(getContext(), TrackingMapActivity.class);
                        intent.putExtra("DATA_TRACKING", item);
                        intent.putExtra("YEU_CAU_ID", item.yeuCauId);
                        startActivity(intent);

                    } else {

                        Intent intent = new Intent(getContext(), DanhSachTinhNguyenActivity.class);
                        intent.putExtra("YEU_CAU_ID", item.yeuCauId);
                        intent.putExtra("LOAI_TIN", item.yeuCau.loaiTin);
                        startActivity(intent);
                    }
                }
                else if (isDonor) {

                    if (item.yeuCau.loaiTin == 1) {

                        Intent intent = new Intent(getContext(), CheckInEventActivity.class);
                        intent.putExtra("DATA_XN", item);
                        startActivity(intent);
                    } else {

                        moGoogleMapsDanDuong(item);
                    }
                }
            }

            @Override
            public void onCancelClick(XacNhanHienMau item) {
                int position = filterList.indexOf(item);
                if (position != -1) {
                    thucHienHuyTrenServer(item, position);
                }
            }
            @Override
            public void onDeletePostClick(com.example.hienmau.models.YeuCauMau post) {
                xacNhanXoaBaiDang(post);
            }

            @Override
            public void onEditPostClick(com.example.hienmau.models.YeuCauMau post) {
                Intent intent = new Intent(getContext(), CreateRequestActivity.class);
                intent.putExtra("EDIT_POST_DATA", post);
                startActivity(intent);
            }
        });
        rvHoatDong.setAdapter(adapter);
    }

    private void xacNhanXoaBaiDang(com.example.hienmau.models.YeuCauMau post) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa bài đăng này không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    thucHienXoaBaiTrenServer(post.id);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void thucHienXoaBaiTrenServer(int postId) {
        ApiClient.getApiService().xoaYeuCauMau(postId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã xóa bài đăng thành công", Toast.LENGTH_SHORT).show();
                    loadData(); // Tải lại dữ liệu để cập nhật danh sách
                } else {
                    Toast.makeText(getContext(), "Không thể xóa bài đăng lúc này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void thucHienHuyTrenServer(XacNhanHienMau item, int position) {
        if (currentUserId == -1 || item.yeuCauId == 0) {
            adapter.notifyItemChanged(position);
            return;
        }

        if (item.trangThaiConfirm == 2) {
            adapter.notifyItemChanged(position);
            Toast.makeText(getContext(), "🔒 Không thể hủy lịch hẹn!", Toast.LENGTH_LONG).show();
            return;
        }

        ApiClient.getApiService().nguoiHienHuyDen(item.yeuCauId, currentUserId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã hủy lịch hẹn ca cấp cứu thành công!", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    adapter.notifyItemChanged(position);

                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Không thể thực hiện lệnh hủy lúc này!";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi phản hồi hệ thống: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                adapter.notifyItemChanged(position);
                Toast.makeText(getContext(), "Lỗi kết nối mạng đến máy chủ trung tâm!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateTabUI(int status) {
        selectedStatus = status;

        // Đổi màu tab
        tabDiChuyen.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabSuKien.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabDaDi.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabBaiDang.setBackgroundResource(R.drawable.bg_tab_unselected);

        tabDiChuyen.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
        tabSuKien.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
        tabDaDi.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
        tabBaiDang.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));

        if (status == 0) {
            tabDiChuyen.setBackgroundResource(R.drawable.bg_tab_selected);
            tabDiChuyen.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        }
        else if (status == 1) {
            tabSuKien.setBackgroundResource(R.drawable.bg_tab_selected);
            tabSuKien.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        }
        else if (status == 2) {
            tabDaDi.setBackgroundResource(R.drawable.bg_tab_selected);
            tabDaDi.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        }
        else {
            tabBaiDang.setBackgroundResource(R.drawable.bg_tab_selected);
            tabBaiDang.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        }

        setupAdapter();
        filterData();
    }

    private void loadData() {
        if (currentUserId == -1) return;
        swipeRefresh.setRefreshing(true);
        fullList.clear();

        ApiClient.getApiService().getLichSuHien(currentUserId).enqueue(new Callback<List<XacNhanHienMau>>() {
            @Override
            public void onResponse(Call<List<XacNhanHienMau>> call, Response<List<XacNhanHienMau>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullList.addAll(response.body());
                    loadMyPostsRegistration();
                }
            }
            @Override
            public void onFailure(Call<List<XacNhanHienMau>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadMyPostsRegistration() {
        ApiClient.getApiService().BaiVietCuaToi(currentUserId).enqueue(new Callback<List<XacNhanHienMau>>() {
            @Override
            public void onResponse(Call<List<XacNhanHienMau>> call, Response<List<XacNhanHienMau>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    fullList.addAll(response.body());
                    loadAllMyRawPosts();
                } else {
                    loadAllMyRawPosts();
                }
            }
            @Override
            public void onFailure(Call<List<XacNhanHienMau>> call, Throwable t) {
                loadAllMyRawPosts();
            }
        });
    }

    private void loadAllMyRawPosts() {

        ApiClient.getApiService().getActiveRequests(null).enqueue(new Callback<List<com.example.hienmau.models.YeuCauMau>>() {
            @Override
            public void onResponse(Call<List<com.example.hienmau.models.YeuCauMau>> call, Response<List<com.example.hienmau.models.YeuCauMau>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {


                    for (com.example.hienmau.models.YeuCauMau post : response.body()) {
                        if (post.nguoiDangId == currentUserId) {


                            boolean isDuplicate = false;
                            for (XacNhanHienMau exist : fullList) {
                                if (exist.yeuCauId == post.id) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            // Nếu là bài đăng sự kiện/cấp cứu mới tinh, tự động bọc vào thực thể ảo
                            if (!isDuplicate) {
                                XacNhanHienMau fakeXn = new XacNhanHienMau();
                                fakeXn.yeuCauId = post.id;
                                fakeXn.yeuCau = post;
                                fakeXn.trangThaiConfirm = 0;
                                fakeXn.nguoiHienId = 0;
                                fakeXn.tempCount = 0;
                                fullList.add(fakeXn);
                            }
                        }
                    }

                    filterData();
                }
            }

            @Override
            public void onFailure(Call<List<com.example.hienmau.models.YeuCauMau>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                filterData();
            }
        });
    }

    private void moGoogleMapsDanDuong(XacNhanHienMau item) {
        if (item.yeuCau != null && item.yeuCau.viDo != 0 && item.yeuCau.kinhDo != 0) {
            String uri = String.format(java.util.Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f&travelmode=motorcycle",
                    item.yeuCau.viDo, item.yeuCau.kinhDo);

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");


            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {

                String webUri = "http://maps.google.com/maps?daddr=" + item.yeuCau.viDo + "," + item.yeuCau.kinhDo;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
            }
        } else {
            Toast.makeText(getContext(), "Tọa độ bệnh viện không hợp lệ!", Toast.LENGTH_SHORT).show();
        }
    }
    private void filterData() {
        filterList.clear();

        java.util.HashMap<Integer, Integer> countMap = new java.util.HashMap<>();
        for (XacNhanHienMau item : fullList) {
            if (item.yeuCau != null && item.yeuCau.nguoiDangId == currentUserId) {
                if (item.nguoiHienId > 0 && item.trangThaiConfirm >= 1) {
                    int id = item.yeuCauId;
                    countMap.put(id, countMap.getOrDefault(id, 0) + 1);
                }
            }
        }

        for (XacNhanHienMau item : fullList) {
            if (item.yeuCau != null && item.yeuCau.nguoiDangId == currentUserId) {
                int realCount = countMap.getOrDefault(item.yeuCauId, 0);
                item.tempCount = realCount;
                item.yeuCau.soNguoiDaXacNhan = realCount;
            }
        }

        // TRƯỜNG HỢP 1: Tab BÀI ĐĂNG (Của tôi)
        if (selectedStatus == 3) {
            java.util.HashSet<Integer> seenIds = new java.util.HashSet<>();
            for (XacNhanHienMau item : fullList) {
                if (item.yeuCau != null && item.yeuCau.nguoiDangId == currentUserId) {
                    if (!seenIds.contains(item.yeuCauId)) {
                        filterList.add(item);
                        seenIds.add(item.yeuCauId);
                    }
                }
            }
        }
        // TRƯỜNG HỢP 2: Tab CẤP CỨU (Active & loaiTin == 0)
        else if (selectedStatus == 0) {
            for (XacNhanHienMau item : fullList) {
                if (item.yeuCau != null && item.yeuCau.loaiTin == 0) {

                    // TRƯỜNG HỢP A: Bạn là TÌNH NGUYỆN VIÊN thực tế đi hiến ca này
                    // Kiểm tra item.nguoiHienId hoặc nếu item lồng dữ liệu cá nhân của currentUserId
                    if (item.nguoiHienId == currentUserId || (item.nguoiHien != null && item.nguoiHien.id == currentUserId)) {
                        if (item.trangThaiConfirm == 1) {
                            filterList.add(item);
                        }
                    }

                    // TRƯỜNG HỢP B: Bạn là NGƯỜI ĐĂNG TIN (Chủ bài đăng cấp cứu)
                    else if (item.yeuCau.nguoiDangId == currentUserId) {
                        if (item.trangThaiConfirm == 1) {
                            filterList.add(item);
                        }
                    }

                }
            }
        }
        // TRƯỜNG HỢP 3: Tab SỰ KIỆN (loaiTin == 1 & Chưa hoàn thành)
        else if (selectedStatus == 1) {
            java.util.HashSet<Integer> seenIds = new java.util.HashSet<>();
            for (XacNhanHienMau item : fullList) {
                if (item.yeuCau != null && item.yeuCau.loaiTin == 1 && item.yeuCau.trangThai != 2) {
                    if (item.yeuCau.nguoiDangId == currentUserId) {
                        if (!seenIds.contains(item.yeuCauId)) {
                            filterList.add(item);
                            seenIds.add(item.yeuCauId);
                        }
                    } else if (item.nguoiHienId == currentUserId) {
                        filterList.add(item);
                    }
                }
            }
        }
        // TRƯỜNG HỢP 4: Tab ĐÃ ĐI (Hoàn thành)
        else if (selectedStatus == 2) {
            for (XacNhanHienMau item : fullList) {
                if (item.trangThaiConfirm == 2 && item.nguoiHienId == currentUserId) {
                    filterList.add(item);
                }
            }
        }

        // Cập nhật hiển thị Empty Layout
        if (filterList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvHoatDong.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvHoatDong.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }
}
