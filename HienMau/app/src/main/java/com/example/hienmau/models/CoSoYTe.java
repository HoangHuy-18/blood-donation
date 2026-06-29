package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CoSoYTe implements Serializable {
    @SerializedName("ID")
    public int id;
    @SerializedName("MaSoThue")
    public String maSoThue;
    @SerializedName("TrangThaiDuyet")
    public int trangThaiDuyet;

    @SerializedName("ThongTinTaiKhoan")
    public NguoiDung thongTinTaiKhoan;
}
