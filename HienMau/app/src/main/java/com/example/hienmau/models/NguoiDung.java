package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NguoiDung implements Serializable {
    @SerializedName("ID")
    public int id;
    @SerializedName("HoTen")
    public String hoTen;

    @SerializedName("SDT")
    public String sdt;

    @SerializedName("Email")
    public String email;

    @SerializedName("DiaChi")
    public String diaChi;

    @SerializedName("ViDo")
    public Double viDo;

    @SerializedName("KinhDo")
    public Double kinhDo;

    @SerializedName("LoaiTaiKhoan")
    public int loaiTaiKhoan;
    @SerializedName("CaNhan")
    public CaNhan caNhan;
}
