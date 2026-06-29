package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class XacNhanHienMau implements Serializable {
    @SerializedName("ID")
    public int id;
    @SerializedName("YeuCauID")
    public int yeuCauId;
    @SerializedName("NguoiHienID")
    public int nguoiHienId;
    @SerializedName("TrangThaiConfirm")
    public int trangThaiConfirm;
    @SerializedName("NgayXacNhan")
    public String ngayXacNhan;
    @SerializedName("QRCode")
    public String qrCode;
    @SerializedName("ChieuCao")
    public Integer chieuCao;

    @SerializedName("CanNang")
    public Double canNang;

    @SerializedName("HuyetApTamThu")
    public Integer huyetApTamThu;

    @SerializedName("HuyetApTamTruong")
    public Integer huyetApTamTruong;
    @SerializedName("NhomMauXacNhan")
    public String nhomMauXacNhan;

    @SerializedName("RhXacNhan")
    public String rhXacNhan;

    @SerializedName("LuongMau")
    public int luongMau;
    @SerializedName("YeuCau")
    public YeuCauMau yeuCau;
    @SerializedName("NguoiHien")
    public NguoiDung nguoiHien;

    public int tempCount = 0;
}
