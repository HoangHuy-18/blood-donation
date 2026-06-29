package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

public class KetQuaHienMau {
    @SerializedName("XacNhanId")
    public int xacNhanId;

    @SerializedName("NgayHien")
    public String ngayHien;
    @SerializedName("NhomMau")
    public String nhomMau;

    @SerializedName("HeRh")
    public String heRh;

    @SerializedName("LuongMau")
    public int luongMau;

    @SerializedName("ChieuCao")
    public int chieuCao;

    @SerializedName("CanNang")
    public double canNang;

    @SerializedName("HuyetApTamThu")
    public int huyetApTamThu;

    @SerializedName("HuyetApTamTruong")
    public int huyetApTamTruong;
}
