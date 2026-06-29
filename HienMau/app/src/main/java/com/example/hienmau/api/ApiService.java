package com.example.hienmau.api;

import com.example.hienmau.models.CreateBaoCaoRequest;
import com.example.hienmau.models.DiemCoDinhResponse;
import com.example.hienmau.models.KetQuaHienMau;
import com.example.hienmau.models.LoginRequest;
import com.example.hienmau.models.NguoiDung;
import com.example.hienmau.models.RegisterMedical;
import com.example.hienmau.models.RegisterPersonal;
import com.example.hienmau.models.RegistrationResponse;
import com.example.hienmau.models.ResetPasswordRequest;
import com.example.hienmau.models.UpdateProfileRequest;
import com.example.hienmau.models.UserProfileResponse;
import com.example.hienmau.models.XacNhanHienMau;
import com.example.hienmau.models.YeuCauMau;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/NguoiDung/login")
    Call<NguoiDung> login(@Body LoginRequest request);

    @POST("api/NguoiDung/register-personal")
    Call<ResponseBody> registerPersonal(@Body RegisterPersonal dto);

    @POST("api/NguoiDung/register-medical")
    Call<ResponseBody> registerMedical(@Body RegisterMedical dto);

    @POST("api/NguoiDung/forgot-password")
    Call<ResponseBody> forgotPassword(@Query("email") String email);

    @POST("api/NguoiDung/reset-password")
    Call<ResponseBody> resetPassword(@Body ResetPasswordRequest request);

    @POST("api/NguoiDung/UpdateProfile")
    Call<okhttp3.ResponseBody> updateProfile(@Body UpdateProfileRequest request);

    @POST("api/YeuCauMau")
    Call<YeuCauMau> postYeuCau(@Body YeuCauMau yeuCau);

    @GET("api/YeuCauMau")
    Call<List<YeuCauMau>> getActiveRequests(@Query("loaiTin") Integer loaiTin);
    @GET("api/YeuCauMau/{id}")
    Call<YeuCauMau> getChiTietYeuCau(@Path("id") int id);
    @FormUrlEncoded
    @POST("api/NguoiDung/UpdateToken")
    Call<Void> updateToken(@Field("userId") int userId, @Field("token") String token);

    @FormUrlEncoded
    @POST("api/NguoiDung/Logout")
    Call<Void> logout(@Field("userId") int userId);

    @POST("api/XacNhanHienMau/DangKyHienMau")
    Call<RegistrationResponse> dangKyHienMau(@Query("yeuCauId") int yeuCauId, @Query("nguoiHienId") int nguoiHienId);
    @POST("api/XacNhanHienMau/XacNhanThuCong")
    Call<Void> medicalConfirm(@Query("xacNhanID") int xacNhanId);
    @GET("api/XacNhanHienMau/GetLichSuHien/{nguoiHienId}")
    Call<List<XacNhanHienMau>> getLichSuHien(@Path("nguoiHienId") int userId);
    @GET("api/XacNhanHienMau/GetDanhSachNguoiHien/{yeuCauId}")
    Call<List<XacNhanHienMau>> getDanhSachNguoiHien(@Path("yeuCauId") int id);
    @DELETE("api/XacNhanHienMau/{id}")
    Call<ResponseBody> xoaTinhNguyenVien(
            @Path("id") int id,
            @Query("loaiTin") int loaiTin
    );
    @POST("api/XacNhanHienMau/XacNhanDenHien")
    Call<Void> xacNhanDenHien(@Query("xacNhanId") int xacNhanId);
    @POST("api/XacNhanHienMau/DanhDauKhongHien/{xacNhanId}")
    Call<Void> danhDauKhongHien(@Path("xacNhanId") int xacNhanId);
    @GET("api/NguoiDung/GetLocation/{userId}")
    Call<NguoiDung> getLiveLocation(@Path("userId") int userId);

    @POST("api/NguoiDung/UpdateLocation")
    Call<Void> updateLocation(
            @Query("userId") int userId,
            @Query("viDo") double viDo,
            @Query("kinhDo") double kinhDo
    );
    @GET("api/XacNhanHienMau/BaiVietCuaToi/{userId}")
    Call<List<XacNhanHienMau>> BaiVietCuaToi(@Path("userId") int userId);

    @DELETE("api/YeuCauMau/{id}")
    Call<ResponseBody> xoaYeuCauMau(@Path("id") int id);

    @POST("api/XacNhanHienMau/XacNhanDenHienBangQR/{qrCode}")
    Call<Void> xacNhanDenHienBangQR(@Path(value = "qrCode", encoded = true) String qrCode);

    @POST("api/XacNhanHienMau/HoanTatHienMau")
    Call<Void> hoanTatHienMau(@Body KetQuaHienMau request);

    @POST("api/XacNhanHienMau/KetThucSuKien/{yeuCauId}")
    Call<Void> ketThucSuKien(@Path("yeuCauId") int yeuCauId);

    @PUT("api/YeuCauMau/{id}")
    Call<YeuCauMau> updateYeuCau(@Path("id") int id, @Body YeuCauMau yeuCau);
    @GET("api/NguoiDung/GetProfile/{userId}")
    Call<UserProfileResponse> getUserProfile(@Path("userId") int userId);

    @GET("api/CoSoYTe/GetFixedPoints")
    Call<List<DiemCoDinhResponse>> getFixedPoints();
    @GET("api/NguoiDung/GetLichSuHienMau/{userId}")
    Call<List<XacNhanHienMau>> getLichSuHienMau(@Path("userId") int userId);

    @Multipart
    @POST("api/NguoiDung/UploadVerifyBlood")
    Call<ResponseBody> uploadVerifyBlood(
            @Part("userId") RequestBody userId,
            @Part("nhomMau") RequestBody nhomMau,
            @Part("heRh") RequestBody heRh,
            @Part List<MultipartBody.Part> images
    );

    @POST("api/BaoCao/GuiBaoCao")
    Call<okhttp3.ResponseBody> guiBaoCaoBaiViet(@Body CreateBaoCaoRequest request);

    @POST("api/XacNhanHienMau/NguoiHienHuyDen/{yeuCauId}/{userId}")
    Call<ResponseBody> nguoiHienHuyDen(
            @Path("yeuCauId") int yeuCauId,
            @Path("userId") int userId
    );

    @POST("api/YeuCauMau/XacNhanChiaSeMau")
    Call<ResponseBody> xacNhanChiaSeMau(
            @Query("yeuCauId") int yeuCauId,
            @Query("hospitalId") int hospitalId
    );
}