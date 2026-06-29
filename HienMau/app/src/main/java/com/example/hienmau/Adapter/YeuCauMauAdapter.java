package com.example.hienmau.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hienmau.R;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.YeuCauMau;
import com.example.hienmau.models.YeuCauMauChiTiet;
import com.google.gson.Gson;

import java.util.List;

public class YeuCauMauAdapter extends RecyclerView.Adapter<YeuCauMauAdapter.ViewHolder>{
    private List<YeuCauMau> list;
    private Context context;
    private OnItemClickListener listener;

    private double currentUserLat = 0;
    private double currentUserLng = 0;

    public void setCurrentLocation(double lat, double lng) {
        this.currentUserLat = lat;
        this.currentUserLng = lng;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onActionClick(YeuCauMau item); // Cho nút "Tôi sẽ đến"
        void onDeleteClick(YeuCauMau item); // Cho nút Xóa
        void onEditClick(YeuCauMau item);
        void onReportClick(YeuCauMau item);
    }
    public YeuCauMauAdapter(List<YeuCauMau> list, Context context, OnItemClickListener listener) {
        this.list = list;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_yeu_cau_mau, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YeuCauMau item = list.get(position);

        // 1. Lấy UserID hiện tại
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userJson = prefs.getString("USER_DATA", null);

        int tempUserId = -1;
        if (userJson != null) {
            tempUserId = new Gson().fromJson(userJson, NguoiDung.class).id;
        }
        final int currentUserId = tempUserId;

        // 2. Hiển thị Người đăng & Badge Verified
        String posterName = (item.nguoiDang != null) ? item.nguoiDang.hoTen : "Người dùng ẩn danh";
        holder.tvTenNguoiDang.setText(posterName);
        holder.tvAvatarLetter.setText(posterName.substring(0, 1).toUpperCase());

        if (item.nguoiDang != null && item.nguoiDang.loaiTaiKhoan == 1) {
            holder.imgVerified.setVisibility(View.VISIBLE);
        } else {
            holder.imgVerified.setVisibility(View.GONE);
        }

        // 3. Hiển thị Bệnh viện & Địa chỉ
        String bvName = (item.benhVien != null && item.benhVien.thongTinTaiKhoan != null)
                ? item.benhVien.thongTinTaiKhoan.hoTen : item.tenBenhVien;
        holder.tvTenBenhVien.setText(android.text.Html.fromHtml("<b>Bệnh viện:</b> " + bvName));
        holder.tvDiaChi.setText(android.text.Html.fromHtml("<b>Địa chỉ:</b> " + item.diaChiBenhVien));

        // 4. Xử lý hiển thị nội dung & Thời gian
        if (item.noiDung == null || item.noiDung.trim().isEmpty()) {
            holder.layoutNoiDung.setVisibility(View.GONE);
        } else {
            holder.layoutNoiDung.setVisibility(View.VISIBLE);
            holder.tvNoiDung.setText(item.noiDung);
        }
        holder.tvThoiGian.setText(getRelativeTime(item.ngayDang));

        // 5. Hiển thị khoảng cách
        if (currentUserLat != 0 && currentUserLng != 0 && item.viDo != 0 && item.kinhDo != 0) {
            holder.tvKhoangCach.setText(calculateDistance(currentUserLat, currentUserLng, item.viDo, item.kinhDo));
            holder.tvKhoangCach.setVisibility(View.VISIBLE);
        } else {
            holder.tvKhoangCach.setVisibility(View.GONE);
        }

        // 6. XỬ LÝ CHIP NHÓM MÁU
        holder.layoutChipsContainer.removeAllViews();
        if (item.chiTiets != null) {
            for (YeuCauMauChiTiet ct : item.chiTiets) {
                addBloodChip(holder.layoutChipsContainer, ct);
            }
        }

        // 7. XỬ LÝ TIẾN ĐỘ TÌNH NGUYỆN VIÊN (LOGIC MỚI)
        Integer needed = item.soNguoiCan;
        int confirmed = item.soNguoiDaXacNhan;

        if (item.soNguoiDaXacNhan == 0 && needed != null) {
            confirmed = item.soNguoiDaXacNhan;
        }

        boolean isFull = (needed != null && needed > 0 && confirmed >= needed);

        if (item.loaiTin == 2) {
            holder.layoutVolunteerStatus.setVisibility(View.GONE);
        } else {
            holder.layoutVolunteerStatus.setVisibility(View.VISIBLE);
            if (needed == null || needed == 0) {
                holder.tvVolunteerProgress.setText("Đã có " + confirmed + " người tham gia");
                holder.layoutVolunteerStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                holder.tvVolunteerProgress.setTextColor(Color.parseColor("#1976D2"));
            } else {
                holder.tvVolunteerProgress.setText("Đã có: " + confirmed + "/" + needed + " người đến");
                if (confirmed >= needed) {
                    holder.tvVolunteerProgress.setTextColor(Color.parseColor("#757575"));
                    holder.layoutVolunteerStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
                } else {
                    holder.tvVolunteerProgress.setTextColor(Color.parseColor("#1976D2"));
                    holder.layoutVolunteerStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                }
            }
        }

        // 8. Logic nút Menu (3 chấm)
        holder.btnMenu.setVisibility(View.VISIBLE); // Luôn luôn hiển thị nút 3 chấm cho tất cả bài viết
        holder.btnMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(context, holder.btnMenu);

            if (item.nguoiDangId == currentUserId) {
                // Nếu là bài đăng của chính mình thì hiện Chỉnh sửa / Xóa
                popup.getMenu().add(0, 1, 0, "Chỉnh sửa bài đăng");
                popup.getMenu().add(0, 2, 1, "Xóa bài đăng");
            } else {
                // Nếu là bài đăng của người khác thì hiện Báo cáo
                popup.getMenu().add(0, 3, 0, "Báo cáo bài viết");
            }

            popup.setOnMenuItemClickListener(menuItem -> {
                if (listener != null) {
                    if (menuItem.getItemId() == 1) listener.onEditClick(item);
                    else if (menuItem.getItemId() == 2) listener.onDeleteClick(item);
                    else if (menuItem.getItemId() == 3) listener.onReportClick(item); // Kích hoạt hiển thị dialog báo cáo
                }
                return true;
            });
            popup.show();
        });

        // 9. Logic nút hành động & Trạng thái đủ người
        if (item.loaiTin == 2) {
            if (item.nguoiDangId == currentUserId) {
                holder.btnToiSeDen.setText("XEM DANH SÁCH");
                holder.btnToiSeDen.setEnabled(true);
                holder.btnToiSeDen.setTextColor(Color.parseColor("#D32F2F"));
            } else {
                holder.btnToiSeDen.setText("CHIA SẺ MÁU");
                holder.btnToiSeDen.setEnabled(true);
                holder.btnToiSeDen.setTextColor(Color.parseColor("#2E7D32"));
            }
        } else {
            // Logic Cấp cứu & Sự kiện cũ của Huy
            if (item.nguoiDangId == currentUserId) {
                holder.btnToiSeDen.setText("XEM DANH SÁCH");
                holder.btnToiSeDen.setEnabled(true);
                holder.btnToiSeDen.setTextColor(Color.parseColor("#D32F2F"));
            } else {
                if (isFull) {
                    holder.btnToiSeDen.setText("ĐÃ ĐỦ NGƯỜI");
                    holder.btnToiSeDen.setEnabled(false);
                    holder.btnToiSeDen.setTextColor(Color.GRAY);
                } else {
                    holder.btnToiSeDen.setText("TÔI SẼ ĐẾN");
                    holder.btnToiSeDen.setEnabled(true);
                    holder.btnToiSeDen.setTextColor(Color.parseColor("#D32F2F"));
                }
            }
        }

        View.OnClickListener actionClick = v -> {
            if (listener != null) {
                if (item.nguoiDangId == currentUserId) {
                    Intent intent = new Intent(context, com.example.hienmau.DanhSachTinhNguyenActivity.class);
                    intent.putExtra("YEU_CAU_ID", item.id);
                    intent.putExtra("LOAI_TIN", item.loaiTin);
                    context.startActivity(intent);
                } else {
                    listener.onActionClick(item);
                }
            }
        };

        holder.itemView.setOnClickListener(actionClick);
        holder.btnToiSeDen.setOnClickListener(actionClick);
    }

    // Hàm tạo Chip động đưa vào LinearLayout
    private void addBloodChip(LinearLayout container, YeuCauMauChiTiet ct) {
        View chipView = LayoutInflater.from(context).inflate(R.layout.item_blood_chip, container, false);
        TextView tvChip = chipView.findViewById(R.id.tvChipText);

        // 1. Ghép Nhóm máu và Rh (Ví dụ: "A" + "+" = "A+")
        String bloodTitle = ct.nhomMau + ct.rh;

        // 2. Chỉ hiện số lượng nếu > 0
        String volume = (ct.soDonVi > 0) ? " (" + ct.soDonVi + "ml)" : "";

        // 3. Logic mức độ: Ẩn chữ nếu là mức 1 (Bình thường)
        String urgencyText = "";
        if (ct.mucDoKhanCap == 3) urgencyText = " - Khẩn cấp";
        else if (ct.mucDoKhanCap == 2) urgencyText = " - Ưu tiên";

        tvChip.setText(bloodTitle + volume + urgencyText);

        // 4. Đổi màu chữ theo mức độ
        if (ct.mucDoKhanCap == 3) {
            tvChip.setTextColor(Color.RED);
        } else if (ct.mucDoKhanCap == 2) {
            tvChip.setTextColor(Color.parseColor("#FF9800")); // Màu cam
        } else {
            tvChip.setTextColor(Color.parseColor("#757575")); // Màu xám cho bình thường
        }

        container.addView(chipView);
    }
    private String getRelativeTime(String dateStr) {
        if (dateStr == null) return "Vừa xong";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateStr);
            long now = System.currentTimeMillis();
            long diff = now - date.getTime();

            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            if (days < 7) return days + " ngày trước";

            return new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "Vừa xong";
        }
    }
    private String calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        float distanceInMeters = results[0];
        if (distanceInMeters < 1000) {
            return (int)distanceInMeters + " m";
        } else {
            return String.format("%.1f km", distanceInMeters / 1000);
        }
    }
    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarLetter, tvTenNguoiDang, tvTenBenhVien, tvThoiGian, tvNoiDung, tvDiaChi, tvKhoangCach;
        TextView tvVolunteerProgress, btnToiSeDen;
        ImageButton btnMenu;
        ImageView imgVerified;
        LinearLayout layoutChipsContainer, layoutNoiDung, layoutVolunteerStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
            tvTenNguoiDang = itemView.findViewById(R.id.tvTenNguoiDang);
            imgVerified = itemView.findViewById(R.id.imgVerified);
            tvTenBenhVien = itemView.findViewById(R.id.tvTenBenhVien);
            tvThoiGian = itemView.findViewById(R.id.tvThoiGian);
            tvNoiDung = itemView.findViewById(R.id.tvNoiDung);
            tvDiaChi = itemView.findViewById(R.id.tvDiaChi);
            tvKhoangCach = itemView.findViewById(R.id.tvKhoangCach);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            btnToiSeDen = itemView.findViewById(R.id.btnToiSeDen);
            layoutChipsContainer = itemView.findViewById(R.id.layoutChipsContainer);

            tvVolunteerProgress = itemView.findViewById(R.id.tvVolunteerProgress);
            layoutNoiDung = itemView.findViewById(R.id.layoutNoiDung);
            layoutVolunteerStatus = itemView.findViewById(R.id.layoutVolunteerStatus);
        }
    }
}
