package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class XacNhanDoiTac implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("yeuCauID")
    public int yeuCauID;

    @SerializedName("benhVienChiaSeID")
    public int benhVienChiaSeID;

    @SerializedName("ngayXacNhan")
    public String ngayXacNhan;

    @SerializedName("trangThai")
    public int trangThai;

    @SerializedName("yeuCau")
    public YeuCauMau yeuCau;

    @SerializedName("benhVienChiaSe")
    public NguoiDung benhVienChiaSe;

    // Thuộc tính ảo phục vụ bọc dữ liệu hiển thị giống Tình nguyện viên
    @SerializedName("nguoiHien")
    public NguoiDung nguoiHien;

    @SerializedName("trangThaiConfirm")
    public int trangThaiConfirm;
}
