package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DiemCoDinhResponse implements Serializable {
    @SerializedName("TenDiaDiem")
    public String tenDiaDiem;

    @SerializedName("DiaChi")
    public String diaChi;

    @SerializedName("SDT")
    public String sdt;

    @SerializedName("ThoiGianLamViec")
    public String thoiGianLamViec;

    @SerializedName("ViDo")
    public double viDo;

    @SerializedName("KinhDo")
    public double kinhDo;
}
