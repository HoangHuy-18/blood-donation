package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {
    @SerializedName("Id")
    public int id;
    @SerializedName("HoTen")
    public String hoTen;
    @SerializedName("Email")
    public String email;
    @SerializedName("Sdt")
    public String sdt;
    @SerializedName("DiaChi")
    public String diaChi;
    @SerializedName("LoaiTaiKhoan")
    public int loaiTaiKhoan; // 0: Cá nhân, 1: BV

    @SerializedName("ViDo")
    public double viDo;
    @SerializedName("KinhDo")
    public double kinhDo;

    // Dành cho cá nhân
    @SerializedName("NhomMau")
    public String nhomMau;
    @SerializedName("HeRh")
    public String heRh;
    @SerializedName("SoLanHien")
    public int soLanHien;
    @SerializedName("NgayHienGanNhat")
    public String ngayHienGanNhat;
    @SerializedName("TrangThaiXacMinh")
    public int trangThaiXacMinh;

    // Dành cho bệnh viện
    @SerializedName("MaSoThue")
    public String maSoThue;

    @SerializedName("DanhHieu")
    public String danhHieu;
}
