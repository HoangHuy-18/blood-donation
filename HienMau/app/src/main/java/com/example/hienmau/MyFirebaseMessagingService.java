package com.example.hienmau;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.hienmau.api.ApiClient;
import com.example.hienmau.models.NguoiDung;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "";
        String body = "";


        if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("message");
        }
        else if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (title != null && !title.isEmpty()) {
            hienThongBao(title, body, remoteMessage);
        }
    }
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userJson = prefs.getString("USER_DATA", null);
        if (userJson != null) {
            NguoiDung user = new com.google.gson.Gson().fromJson(userJson, NguoiDung.class);
            ApiClient.getApiService().updateToken(user.id, token).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                @Override public void onFailure(Call<Void> call, Throwable t) {}
            });
        }
    }
    private void hienThongBao(String title, String body, RemoteMessage remoteMessage) {
        String channelId = "hien_mau_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId, "Thông báo khẩn cấp", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Thông báo khẩn cấp và sự kiện hiến máu");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
        }


        Intent intent;
        String type = remoteMessage.getData().get("type");
        String yeuCauIdStr = remoteMessage.getData().get("YeuCauID");

        if ("NewRegistration".equals(type) && yeuCauIdStr != null) {

            intent = new Intent(this, DanhSachTinhNguyenActivity.class);
            intent.putExtra("YEU_CAU_ID", Integer.parseInt(yeuCauIdStr));
        } else if ("new_request".equals(type) && yeuCauIdStr != null) {

            intent = new Intent(this, ChiTietYeuCauActivity.class);
            intent.putExtra("DATA_YEU_CAU", Integer.parseInt(yeuCauIdStr)); // Gửi ID dạng int
        }else if ("ConfirmedToCome".equals(type) && yeuCauIdStr != null) {
            intent = new Intent(this, ChiTietYeuCauActivity.class);
            intent.putExtra("DATA_YEU_CAU", Integer.parseInt(yeuCauIdStr));
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Nhớ đổi icon giọt máu nếu có
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);


        if (title.contains("🆘") || title.toLowerCase().contains("khẩn cấp") || title.toLowerCase().contains("gấp")) {
            builder.setColor(Color.RED);
            builder.setVibrate(new long[]{0, 500, 200, 500, 200, 500}); // Rung dồn dập hơn
        } else {
            builder.setColor(Color.parseColor("#2E7D32")); // Màu xanh lá cho Sự kiện
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
