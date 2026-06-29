package com.example.hienmau.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hienmau.R;
import com.example.hienmau.models.XacNhanHienMau;
import com.google.android.material.button.MaterialButton;

import java.util.List;
public class VolunteerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> mData;
    private Context context;
    private int loaiTin;
    private OnVolunteerActionListener actionListener;

    public interface OnVolunteerActionListener {
        void onConfirm(XacNhanHienMau item);
        void onInputResult(XacNhanHienMau item);
    }

    public VolunteerAdapter(List<Object> mData, Context context, int loaiTin, OnVolunteerActionListener listener) {
        this.mData = mData;
        this.context = context;
        this.loaiTin = loaiTin;
        this.actionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (mData.get(position) instanceof String) return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_header_danh_sach, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_tinh_nguyen_vien, parent, false);
            return new ItemViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) mData.get(position));
        } else {
            ItemViewHolder h = (ItemViewHolder) holder;
            XacNhanHienMau item = (XacNhanHienMau) mData.get(position);

            boolean isEventClosed = (item.yeuCau != null && item.yeuCau.trangThai == 2);

            if (loaiTin == -1) {
                int totalDonations = 0;
                for (Object obj : mData) if (obj instanceof XacNhanHienMau) totalDonations++;

                int currentDataIndex = 0;
                for (int i = 0; i <= position; i++) {
                    if (mData.get(i) instanceof XacNhanHienMau) currentDataIndex++;
                }
                int donationNumber = totalDonations - currentDataIndex + 1;

                h.tvTen.setText("Lần " + String.format("%02d", donationNumber));
                h.tvTen.setTextColor(Color.parseColor("#D32F2F"));

                String tenBV = (item.yeuCau != null) ? item.yeuCau.tenBenhVien : "Địa điểm không xác định";
                h.tvSdt.setText("📍 " + tenBV);

                h.tvAvatar.setText(String.valueOf(donationNumber));
                h.tvAvatar.setBackgroundResource(R.drawable.bg_circle_red);

                h.tvTrangThaiCheckIn.setVisibility(View.VISIBLE);
                String dateStr = (item.ngayXacNhan != null) ? formatDate(item.ngayXacNhan) : "---";

                int chieuCao = (item.chieuCao != null) ? item.chieuCao : 0;
                double canNang = (item.canNang != null) ? item.canNang : 0.0;

                h.tvTrangThaiCheckIn.setText("📅 " + dateStr + " | 🩸 " + item.luongMau + "ml" +
                        " | 📐 " + chieuCao + "cm - " + canNang + "kg");
                h.tvTrangThaiCheckIn.setTextColor(Color.parseColor("#424242"));

                h.btnCall.setVisibility(View.GONE);
                h.btnConfirm.setVisibility(View.GONE);
            }

            else {
                h.btnCall.setVisibility(View.VISIBLE);

                if (item.nguoiHien != null) {
                    h.tvTen.setText(item.nguoiHien.hoTen);
                    h.tvSdt.setText(item.nguoiHien.sdt);

                    if (item.nguoiHien.hoTen != null && !item.nguoiHien.hoTen.isEmpty()) {
                        h.tvAvatar.setText(item.nguoiHien.hoTen.substring(0, 1).toUpperCase());
                    }

                    h.btnCall.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.nguoiHien.sdt));
                        context.startActivity(intent);
                    });
                }

                h.tvTrangThaiCheckIn.setVisibility(View.GONE);

                if (loaiTin == 2) {
                    h.tvTrangThaiCheckIn.setVisibility(View.VISIBLE);
                    h.tvTrangThaiCheckIn.setText("🏥 Đối tác sẵn sàng điều phối | Bấm nút Gọi hoặc Hủy hỗ trợ");
                    h.tvTrangThaiCheckIn.setTextColor(Color.parseColor("#2E7D32"));

                    h.btnConfirm.setVisibility(View.VISIBLE);
                    h.btnConfirm.setEnabled(true);
                    h.btnConfirm.setText("HỦY BỎ");
                    h.btnConfirm.setTextColor(Color.WHITE);
                    h.btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));

                    h.btnConfirm.setOnClickListener(v -> {
                        if (actionListener != null) {
                            // ĐỂ ĐẢM BẢO KHÔNG BỊ LỆCH VỊ TRÍ:
                            // Sử dụng hàm getAdapterPosition() để định vị dòng thực tế đang hiển thị trên màn hình
                            int actualPos = h.getAdapterPosition();
                            if (actualPos != RecyclerView.NO_POSITION) {
                                XacNhanHienMau clickedItem = (XacNhanHienMau) mData.get(actualPos);
                                actionListener.onConfirm(clickedItem); // Truyền đúng item chứa ID gốc từ DB
                            }
                        }
                    });
                }

                else if (loaiTin == 1) {
                    if (item.trangThaiConfirm == 4) {
                        h.btnConfirm.setVisibility(View.VISIBLE); h.btnConfirm.setText("SỬA KQ");
                        h.btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                        h.tvTrangThaiCheckIn.setVisibility(View.VISIBLE); h.tvTrangThaiCheckIn.setText("Không hiến ❌");
                        if (isEventClosed) { h.btnConfirm.setEnabled(false); h.btnConfirm.setTextColor(Color.parseColor("#9E9E9E")); }
                        else { h.btnConfirm.setEnabled(true); h.btnConfirm.setOnClickListener(v -> actionListener.onInputResult(item)); }
                    }
                    else if (item.trangThaiConfirm == 1) {
                        h.btnConfirm.setVisibility(View.VISIBLE); h.btnConfirm.setText("NHẬP KQ");
                        h.btnConfirm.setTextColor(Color.parseColor("#4CAF50"));
                        h.btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                        h.tvTrangThaiCheckIn.setVisibility(View.VISIBLE); h.tvTrangThaiCheckIn.setText("Đã đến ✅"); h.tvTrangThaiCheckIn.setTextColor(Color.parseColor("#2E7D32"));
                        if (isEventClosed) { h.btnConfirm.setEnabled(false); h.btnConfirm.setTextColor(Color.parseColor("#9E9E9E")); }
                        else { h.btnConfirm.setEnabled(true); h.btnConfirm.setOnClickListener(v -> actionListener.onInputResult(item)); }
                    }
                    else if (item.trangThaiConfirm >= 2) {
                        h.btnConfirm.setVisibility(View.VISIBLE); h.btnConfirm.setText("SỬA KQ");
                        h.btnConfirm.setTextColor(Color.parseColor("#4CAF50"));
                        h.btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                        h.tvTrangThaiCheckIn.setVisibility(View.VISIBLE); h.tvTrangThaiCheckIn.setText("HOÀN TẤT ❤️"); h.tvTrangThaiCheckIn.setTextColor(Color.parseColor("#D32F2F"));
                        if (isEventClosed) { h.btnConfirm.setEnabled(false); h.btnConfirm.setTextColor(Color.parseColor("#9E9E9E")); }
                        else { h.btnConfirm.setEnabled(true); h.btnConfirm.setOnClickListener(v -> actionListener.onInputResult(item)); }
                    }
                    else {
                        h.btnConfirm.setVisibility(View.GONE);
                        h.tvTrangThaiCheckIn.setVisibility(View.VISIBLE); h.tvTrangThaiCheckIn.setText("Chưa đến"); h.tvTrangThaiCheckIn.setTextColor(Color.parseColor("#9E9E9E"));
                    }
                }

                else {
                    String bloodType = "Chưa rõ"; int chieuCao = 0; double canNang = 0.0;
                    if (item.nguoiHien != null && item.nguoiHien.caNhan != null) {
                        String nhomMau = item.nguoiHien.caNhan.nhomMau; String heRh = item.nguoiHien.caNhan.heRh;
                        if (nhomMau != null && !nhomMau.isEmpty()) bloodType = nhomMau + (heRh != null ? heRh : "");
                        if (item.nguoiHien.caNhan.chieuCao != null) chieuCao = item.nguoiHien.caNhan.chieuCao;
                        if (item.nguoiHien.caNhan.canNang != null) canNang = item.nguoiHien.caNhan.canNang;
                    }
                    h.tvTrangThaiCheckIn.setVisibility(View.VISIBLE);
                    if (item.trangThaiConfirm >= 1) {
                        h.btnConfirm.setVisibility(View.GONE);
                        h.tvTrangThaiCheckIn.setText("📌 Đã chốt | Nhóm máu: " + bloodType + " | " + chieuCao + "cm - " + canNang + "kg");
                        h.tvTrangThaiCheckIn.setTextColor(Color.parseColor("#1976D2"));
                    } else {
                        h.btnConfirm.setVisibility(View.VISIBLE); h.btnConfirm.setEnabled(true); h.btnConfirm.setText("XÁC NHẬN");
                        h.btnConfirm.setOnClickListener(v -> actionListener.onConfirm(item));
                        h.tvTrangThaiCheckIn.setText("Nhóm máu: " + bloodType + " | Thể Trạng: " + chieuCao + "cm - " + canNang + "kg");
                    }
                }
            }
        }
    }

    private String formatDate(String dateStr) {
        try {
            String ymd = dateStr.substring(0, 10);
            String[] parts = ymd.split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    public int getItemCount() { return mData.size(); }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        public HeaderViewHolder(View v) { super(v); tvHeader = v.findViewById(R.id.tvHeaderTitle); }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvTen, tvSdt, tvTrangThaiCheckIn;
        MaterialButton btnConfirm;
        ImageButton btnCall;
        public ItemViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvAvatarDS);
            tvTen = v.findViewById(R.id.tvTenNguoiHien);
            tvSdt = v.findViewById(R.id.tvSdtNguoiHien);
            btnCall = v.findViewById(R.id.btnGoiNguoiHien);
            tvTrangThaiCheckIn = v.findViewById(R.id.tvTrangThaiCheckIn);
            btnConfirm = v.findViewById(R.id.btnXacNhanHien);
        }
    }
}
