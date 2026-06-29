package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {
    @SerializedName("UserId")
    private int userId;

    @SerializedName("HoTen")
    private String hoTen;

    @SerializedName("SDT")
    private String sdt;

    @SerializedName("DiaChi")
    private String diaChi;

    @SerializedName("ViDo")
    private double viDo;

    @SerializedName("KinhDo")
    private double kinhDo;

    public UpdateProfileRequest() {}

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public double getViDo() { return viDo; }
    public void setViDo(double viDo) { this.viDo = viDo; }

    public double getKinhDo() { return kinhDo; }
    public void setKinhDo(double kinhDo) { this.kinhDo = kinhDo; }
}
