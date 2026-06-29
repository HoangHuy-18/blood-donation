package com.example.hienmau.models;

import com.google.gson.annotations.SerializedName;

public class CreateBaoCaoRequest {
    @SerializedName("YeuCauId")
    public int yeuCauId;

    @SerializedName("NguoiBaoCaoId")
    public int nguoiBaoCaoId;

    @SerializedName("LyDo")
    public String lyDo;

    @SerializedName("ChiTiet")
    public String chiTiet;

    public CreateBaoCaoRequest(int yeuCauId, int nguoiBaoCaoId, String lyDo, String chiTiet) {
        this.yeuCauId = yeuCauId;
        this.nguoiBaoCaoId = nguoiBaoCaoId;
        this.lyDo = lyDo;
        this.chiTiet = chiTiet;
    }
}
