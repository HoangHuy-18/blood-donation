package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class YeuCauMauChiTiet implements Serializable {
    @SerializedName("ID")
    public int id;
    @SerializedName("NhomMau")
    public String nhomMau;
    @SerializedName("Rh")
    public String rh;
    @SerializedName("SoDonVi")
    public int soDonVi;

    @SerializedName("MucDoKhanCap")
    public int mucDoKhanCap;
}
