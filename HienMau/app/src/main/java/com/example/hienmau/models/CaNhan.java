package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CaNhan implements Serializable {
    @SerializedName("ID")
    public int id;

    @SerializedName("NhomMau")
    public String nhomMau;

    @SerializedName("HeRh")
    public String heRh;

    @SerializedName("NgaySinh")
    public String ngaySinh;

    @SerializedName("SoLanHien")
    public int soLanHien;

    @SerializedName("ChieuCao")
    public Integer chieuCao;

    @SerializedName("CanNang")
    public Double canNang;
    @SerializedName("NgayHienGanNhat")
    public String ngayHienGanNhat;
}
