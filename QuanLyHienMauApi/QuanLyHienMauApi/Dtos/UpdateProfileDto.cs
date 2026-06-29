namespace QuanLyHienMauApi.Dtos
{
    public class UpdateProfileDto
    {
        public int UserId { get; set; }
        public string? HoTen { get; set; }

        public string? SDT { get; set; }

        public string? DiaChi { get; set; }

        public double ViDo { get; set; }
        public double KinhDo { get; set; }
    }
}
