package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class YeuCauMau implements Serializable {
    @SerializedName("ID")
    public int id;
    @SerializedName("NguoiDangID")
    public int nguoiDangId;

    @SerializedName("BenhVienID")
    public Integer benhVienId;

    @SerializedName("TenBenhVien")
    public String tenBenhVien;

    @SerializedName("DiaChiBenhVien")
    public String diaChiBenhVien;

    @SerializedName("ViDo")
    public Double viDo;

    @SerializedName("KinhDo")
    public Double kinhDo;
    @SerializedName("SoNguoiCan")
    public Integer soNguoiCan;
    @SerializedName("NoiDung")
    public String noiDung;

    @SerializedName("TrangThai")
    public int trangThai;

    @SerializedName("NgayDang")
    public String ngayDang;
    @SerializedName("LoaiTin")
    public int loaiTin;
    @SerializedName("ChiTiets")
    public List<YeuCauMauChiTiet> chiTiets;

    @SerializedName("NguoiDang")
    public NguoiDung nguoiDang;

    @SerializedName("NgayBatDau")
    public String ngayBatDau;

    @SerializedName("NgayKetThuc")
    public String ngayKetThuc;

    @SerializedName("GioBatDau")
    public String gioBatDau;

    @SerializedName("GioKetThuc")
    public String gioKetThuc;

    @SerializedName("BenhVien")
    public CoSoYTe benhVien;

    @SerializedName("SoNguoiDaXacNhan")
    public int soNguoiDaXacNhan;
}
