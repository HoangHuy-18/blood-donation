package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class RegisterMedical implements Serializable {
    @SerializedName("MaSoThue")
    public String maSoThue;

    @SerializedName("HoTen")
    public String hoTen;

    @SerializedName("SDT")
    public String sdt;

    @SerializedName("MatKhau")
    public String matKhau;

    @SerializedName("Email")
    public String email;

    @SerializedName("DiaChi")
    public String diaChi;

    @SerializedName("ViDo")
    public double viDo;

    @SerializedName("KinhDo")
    public double kinhDo;

    @SerializedName("AnhGiayPhep")
    public String anhGiayPhep;

    @SerializedName("AnhQuyetDinh")
    public String anhQuyetDinh;

    public RegisterMedical(String maSoThue, String hoTen, String sdt, String matKhau,
                           String email, String diaChi, double viDo, double kinhDo) {
        this.maSoThue = maSoThue;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.matKhau = matKhau;
        this.email = email;
        this.diaChi = diaChi;
        this.viDo = viDo;
        this.kinhDo = kinhDo;
        this.anhGiayPhep = "";
        this.anhQuyetDinh = "";
    }

    public void setAnhGiayPhep(String anhGiayPhep) {
        this.anhGiayPhep = anhGiayPhep;
    }

    public void setAnhQuyetDinh(String anhQuyetDinh) {
        this.anhQuyetDinh = anhQuyetDinh;
    }
}
