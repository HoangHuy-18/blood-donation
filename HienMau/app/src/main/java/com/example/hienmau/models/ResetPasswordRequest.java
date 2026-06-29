package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    @SerializedName("Email")
    public String email;
    @SerializedName("MaOTP")
    public String maOTP;
    @SerializedName("MatKhauMoi")
    public String matKhauMoi;

    public ResetPasswordRequest(String email, String maOTP, String matKhauMoi) {
        this.email = email;
        this.maOTP = maOTP;
        this.matKhauMoi = matKhauMoi;
    }
}
