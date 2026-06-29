package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class RegisterPersonal implements Serializable {
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

    @SerializedName("NgaySinh")
    public String ngaySinh;

    @SerializedName("ViDo")
    public double viDo;

    @SerializedName("KinhDo")
    public double kinhDo;

    public RegisterPersonal(String hoTen, String sdt, String matKhau, String email,
                           String diaChi, String ngaySinh,
                            double viDo, double kinhDo) {
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.matKhau = matKhau;
        this.email = email;
        this.diaChi = diaChi;
        this.ngaySinh = ngaySinh;
        this.viDo = viDo;
        this.kinhDo = kinhDo;
    }
}
