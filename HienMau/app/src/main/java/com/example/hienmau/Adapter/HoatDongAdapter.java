package com.example.hienmau.Adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hienmau.R;
import com.example.hienmau.models.XacNhanHienMau;
import com.example.hienmau.models.YeuCauMau;
import com.example.hienmau.models.YeuCauMauChiTiet;

import java.util.List;

public class HoatDongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HOAT_DONG = 0;
    private static final int TYPE_BAI_DANG = 1;

    private List<XacNhanHienMau> list;
    private Context context;
    private int currentUserId;
    private int selectedTab;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(XacNhanHienMau item);
        void onCancelClick(XacNhanHienMau item);
        void onDeletePostClick(YeuCauMau item);
        void onEditPostClick(YeuCauMau post);
    }
    public HoatDongAdapter(List<XacNhanHienMau> list, Context context, int currentUserId, int selectedTab, OnItemClickListener listener) {
        this.list = list;
        this.context = context;
        this.currentUserId = currentUserId;
        this.selectedTab = selectedTab;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (selectedTab == 3) ? TYPE_BAI_DANG : TYPE_HOAT_DONG;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_BAI_DANG) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_yeu_cau_mau, parent, false);
            return new PostViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_hoat_dong, parent, false);
            return new ActivityViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        XacNhanHienMau item = list.get(position);
        if (item == null || item.yeuCau == null) return;

        if (holder instanceof ActivityViewHolder) {
            ActivityViewHolder h = (ActivityViewHolder) holder;
            YeuCauMau yeuCau = item.yeuCau;
            boolean isEvent = (yeuCau.loaiTin == 1); // 1 là Sự kiện, 0 là Cấp cứu
            boolean isCreator = (yeuCau.nguoiDangId == currentUserId);

            if (isCreator) {
                if (isEvent) {
                    h.tvTitle.setText(yeuCau.tenBenhVien);
                    h.tvSubTitle.setText("Sự kiện bạn đang tổ chức");
                    h.tvAvatar.setText("🏥");
                    h.tvAvatar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                    h.tvAvatar.setTextColor(Color.parseColor("#2E7D32"));
                } else {
                    if (item.tempCount > 1) {
                        h.tvTitle.setText(item.tempCount + " người đang đến giúp");
                        h.tvSubTitle.setText("Hỗ trợ tại: " + yeuCau.tenBenhVien);

                        h.tvAvatar.setText(String.valueOf(item.tempCount));
                        h.tvAvatar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                        h.tvAvatar.setTextColor(Color.parseColor("#1976D2"));
                    } else {
                        String tenNguoiHien = (item.nguoiHien != null) ? item.nguoiHien.hoTen : "Tình nguyện viên";
                        h.tvTitle.setText(tenNguoiHien);
                        h.tvSubTitle.setText("Đang đến: " + yeuCau.tenBenhVien);

                        // Hiển thị chữ cái đầu của tên
                        if (!tenNguoiHien.isEmpty()) {
                            h.tvAvatar.setText(tenNguoiHien.substring(0, 1).toUpperCase());
                        }
                        h.tvAvatar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                        h.tvAvatar.setTextColor(Color.parseColor("#1976D2"));
                    }

                }
            } else {
                h.tvTitle.setText(yeuCau.tenBenhVien);
                if (isEvent) {
                    String gio = (yeuCau.gioBatDau != null) ? yeuCau.gioBatDau : "";
                    String ngay = (yeuCau.ngayBatDau != null) ? yeuCau.ngayBatDau : "";
                    h.tvSubTitle.setText("Bạn tham gia sự kiện này\n" +
                                        "Thời gian: " + gio + " - " + ngay);
                    h.tvAvatar.setText("📅");
                    h.tvAvatar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F8E9")));
                    h.tvAvatar.setTextColor(Color.parseColor("#558B2F"));
                } else {
                    h.tvSubTitle.setText("Bạn đang đi hỗ trợ");
                    h.tvAvatar.setText("🆘");
                    h.tvAvatar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                    h.tvAvatar.setTextColor(Color.parseColor("#E65100"));
                }
            }

            updateStatusUI(h.tvTrangThai, item.trangThaiConfirm, isEvent, isCreator, item.tempCount);
        } else if (holder instanceof PostViewHolder) {
            PostViewHolder h = (PostViewHolder) holder;
            YeuCauMau post = item.yeuCau;

            h.tvTenNguoiDang.setText("Bài đăng của bạn");
            h.tvAvatarLetter.setText("B");
            h.tvThoiGian.setText(getRelativeTime(post.ngayDang));

            // Bệnh viện & Địa chỉ
            h.tvTenBenhVien.setText(android.text.Html.fromHtml("<b>Bệnh viện:</b> " + post.tenBenhVien));
            h.tvDiaChi.setText(android.text.Html.fromHtml("<b>Địa chỉ:</b> " + post.diaChiBenhVien));

            // Khoảng cách - Bỏ hiển thị theo yêu cầu
            h.tvKhoangCach.setVisibility(View.GONE);

            // Nội dung
            if (post.noiDung == null || post.noiDung.trim().isEmpty()) {
                h.layoutNoiDung.setVisibility(View.GONE);
            } else {
                h.layoutNoiDung.setVisibility(View.VISIBLE);
                h.tvNoiDung.setText(post.noiDung);
            }

            // Chip Nhóm máu
            h.layoutChipsContainer.removeAllViews();
            if (post.chiTiets != null) {
                for (YeuCauMauChiTiet ct : post.chiTiets) {
                    addBloodChip(h.layoutChipsContainer, ct);
                }
            }

            if (post.loaiTin == 2) {
                h.layoutVolunteerStatus.setVisibility(View.GONE);
                h.btnToiSeDen.setText("XEM DANH SÁCH");
            } else {
                h.layoutVolunteerStatus.setVisibility(View.VISIBLE);
                Integer needed = post.soNguoiCan;
                int confirmed = item.tempCount;

                if (needed == null || needed == 0) {
                    h.tvVolunteerProgress.setText("Đã có " + confirmed + " người tham gia");
                    h.layoutVolunteerStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                    h.tvVolunteerProgress.setTextColor(Color.parseColor("#1976D2"));
                } else {
                    h.tvVolunteerProgress.setText("Đã có: " + confirmed + "/" + needed + " người đến");
                    if (confirmed >= needed) {
                        h.tvVolunteerProgress.setTextColor(Color.parseColor("#757575"));
                        h.layoutVolunteerStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
                    } else {
                        h.tvVolunteerProgress.setTextColor(Color.parseColor("#1976D2"));
                        h.layoutVolunteerStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                    }
                }
                h.btnToiSeDen.setText("XEM DANH SÁCH");
            }


            // Menu 3 chấm
            h.btnMenu.setVisibility(View.VISIBLE);
            h.btnMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(context, h.btnMenu);
                popup.getMenu().add(0, 1, 0, "Chỉnh sửa bài đăng");
                popup.getMenu().add(0, 2, 1, "Xóa bài đăng");

                popup.setOnMenuItemClickListener(menuItem -> {
                    if (listener != null) {
                        if (menuItem.getItemId() == 1) listener.onEditPostClick(post);
                        else if (menuItem.getItemId() == 2) listener.onDeletePostClick(post);
                    }
                    return true;
                });
                popup.show();
            });
        }
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }
    private void addBloodChip(LinearLayout container, YeuCauMauChiTiet ct) {
        View chipView = LayoutInflater.from(context).inflate(R.layout.item_blood_chip, container, false);
        TextView tvChip = chipView.findViewById(R.id.tvChipText);

        String bloodTitle = ct.nhomMau + ct.rh;

        String volume = (ct.soDonVi > 0) ? " (" + ct.soDonVi + "ml)" : "";

        String urgencyText = "";
        if (ct.mucDoKhanCap == 3) {
            urgencyText = " - Khẩn cấp";
            tvChip.setTextColor(Color.RED);
        } else if (ct.mucDoKhanCap == 2) {
            urgencyText = " - Ưu tiên";
            tvChip.setTextColor(Color.parseColor("#FF9800")); // Màu cam cho ưu tiên
        } else {
            tvChip.setTextColor(Color.parseColor("#757575")); // Màu xám cho bình thường
        }

        // 4. Gán chuỗi hoàn chỉnh vào TextView
        tvChip.setText(bloodTitle + volume + urgencyText);

        container.addView(chipView);
    }
    private String getRelativeTime(String dateStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateStr);
            long diff = System.currentTimeMillis() - date.getTime();
            long minutes = diff / (60 * 1000);
            if (minutes < 60) return minutes + " phút trước";
            long hours = minutes / 60;
            if (hours < 24) return hours + " giờ trước";
            return new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
        } catch (Exception e) { return "Vừa xong"; }
    }
    private void updateStatusUI(TextView tv, int status, boolean isEvent, boolean isCreator, int count) {
        if (isEvent) {
            if (isCreator) {
                tv.setText(count + " người tham gia");
                tv.setTextColor(Color.parseColor("#E65100"));
                tv.setBackgroundResource(R.drawable.bg_status_urgent);
            } else {
                if (status == 2) {
                    tv.setText("Đã đến điểm hiến");
                    tv.setTextColor(Color.parseColor("#2E7D32"));
                    tv.setBackgroundResource(R.drawable.bg_status_completed);
                } else {
                    tv.setText("Đã đăng ký");
                    tv.setTextColor(Color.parseColor("#1976D2"));
                    tv.setBackgroundResource(R.drawable.bg_status_pending);
                }
            }
        } else {
            if (status == 1) {
                tv.setText("Đang đến");
                tv.setTextColor(Color.parseColor("#1976D2"));
                tv.setBackgroundResource(R.drawable.bg_status_pending);
            } else if (status == 2) {
                tv.setText("Đã hiến thành công");
                tv.setTextColor(Color.parseColor("#2E7D32"));
                tv.setBackgroundResource(R.drawable.bg_status_completed);
            } else {
                tv.setText("Chờ di chuyển");
                tv.setTextColor(Color.parseColor("#E65100"));
                tv.setBackgroundResource(R.drawable.bg_status_urgent);
            }
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvTitle, tvSubTitle, tvTrangThai;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatarHD);
            tvTitle = itemView.findViewById(R.id.tvTitleHD);
            tvSubTitle = itemView.findViewById(R.id.tvSubTitleHD);
            tvTrangThai = itemView.findViewById(R.id.tvTrangThaiHD);
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarLetter, tvTenNguoiDang, tvThoiGian, tvTenBenhVien, tvDiaChi, tvNoiDung, btnToiSeDen, tvKhoangCach;
        TextView tvVolunteerProgress, tvBadgeCount;
        LinearLayout layoutChipsContainer, layoutNoiDung, layoutVolunteerStatus;
        ImageButton btnMenu;
        ImageView imgVerified;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
            tvBadgeCount = itemView.findViewById(R.id.tvBadgeCount); // Giữ lại badge nếu bạn dùng làm chấm đỏ thông báo
            tvTenNguoiDang = itemView.findViewById(R.id.tvTenNguoiDang);
            imgVerified = itemView.findViewById(R.id.imgVerified);
            tvThoiGian = itemView.findViewById(R.id.tvThoiGian);
            tvTenBenhVien = itemView.findViewById(R.id.tvTenBenhVien);
            tvDiaChi = itemView.findViewById(R.id.tvDiaChi);
            tvKhoangCach = itemView.findViewById(R.id.tvKhoangCach);
            tvNoiDung = itemView.findViewById(R.id.tvNoiDung);
            btnToiSeDen = itemView.findViewById(R.id.btnToiSeDen);
            layoutChipsContainer = itemView.findViewById(R.id.layoutChipsContainer);
            layoutNoiDung = itemView.findViewById(R.id.layoutNoiDung);
            btnMenu = itemView.findViewById(R.id.btnMenu);

            tvVolunteerProgress = itemView.findViewById(R.id.tvVolunteerProgress);
            layoutVolunteerStatus = itemView.findViewById(R.id.layoutVolunteerStatus);
        }
    }
}
