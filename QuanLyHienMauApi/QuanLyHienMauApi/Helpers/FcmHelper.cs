using FirebaseAdmin.Messaging;

namespace QuanLyHienMauApi.Helpers
{
    public static class FcmHelper
    {
        // Hàm tính khoảng cách giữa 2 điểm GPS (trả về km)
        public static double CalculateDistance(double lat1, double lon1, double lat2, double lon2)
        {
            var R = 6371; // Bán kính Trái Đất
            var dLat = ToRadians(lat2 - lat1);
            var dLon = ToRadians(lon2 - lon1);
            var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                    Math.Cos(ToRadians(lat1)) * Math.Cos(ToRadians(lat2)) *
                    Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
            var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
            return R * c;
        }

        private static double ToRadians(double deg) => deg * (Math.PI / 180);

        // Hàm gửi thông báo đến danh sách Token
        public static async Task SendNotification(List<string> tokens, string title, string body, string type, int yeuCauId)
        {
            if (tokens == null || tokens.Count == 0) return;

            var validTokens = tokens.Where(t => !string.IsNullOrEmpty(t)).ToList();
            if (!validTokens.Any()) return;

            var message = new MulticastMessage()
            {
                Tokens = tokens,
                Notification = new Notification() { Title = title, Body = body },
                Android = new AndroidConfig()
                {
                    Priority = Priority.High,
                    TimeToLive = TimeSpan.FromHours(1),
                    Notification = new AndroidNotification()
                    {
                        Sound = "default",
                        Sticky = true,
                        DefaultSound = true,
                        DefaultVibrateTimings = true
                    }
                },
                Data = new Dictionary<string, string>() {
                    { "title", title },
                    { "message", body },
                    { "type", type },
                    { "YeuCauID", yeuCauId.ToString() }
                }
            };
            try
            {
                await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(message);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Lỗi FCM: " + ex.Message);
            }
        }
        public static bool CheckBloodCompatibility(string donorBlood, string donorRh, string recipientBlood, string recipientRh)
        {
            // donorBlood: "A", donorRh: "+"
            // recipientBlood: "A", recipientRh: "+"

            // 1. Kiểm tra hệ Rh trước (Người Rh- có thể hiến cho cả + và -, nhưng người Rh+ chỉ hiến được cho +)
            if (donorRh == "+" && recipientRh == "-") return false;

            // 2. Kiểm tra nhóm máu theo bảng tương thích (ABO)
            return recipientBlood switch
            {
                "A" => (donorBlood == "A" || donorBlood == "O"),
                "B" => (donorBlood == "B" || donorBlood == "O"),
                "AB" => true, // Nhóm AB nhận được tất cả
                "O" => (donorBlood == "O"),
                _ => false
            };
        }
    }
}
