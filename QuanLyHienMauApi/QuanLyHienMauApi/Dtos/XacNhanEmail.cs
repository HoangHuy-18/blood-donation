namespace QuanLyHienMauApi.Dtos
{
    public class XacNhanEmail
    {
        public int ID { get; set; }
        public string Email { get; set; }
        public string MaXacNhan { get; set; }
        public DateTime ThoiGianHetHan { get; set; }
    }

    
    public class ResetPasswordRequest
    {
        public string Email { get; set; }
        public string MaOTP { get; set; }
        public string MatKhauMoi { get; set; }
    }
}
