namespace QuanLyHienMauApi.Dtos
{
    public class UserProfileDto
    {
        // Thông tin chung
        public int Id { get; set; }
        public string HoTen { get; set; }
        public string Email { get; set; }
        public string Sdt { get; set; }
        public string DiaChi { get; set; }
        public int LoaiTaiKhoan { get; set; } // 0: Cá nhân, 1: Bệnh viện

        public double ViDo { get; set; }
        public double KinhDo { get; set; }

       
        public string? NhomMau { get; set; }
        public string? HeRh { get; set; }
        public int SoLanHien { get; set; }
        public DateTime? NgayHienGanNhat { get; set; }

        
        public string? MaSoThue { get; set; }

      
        public string DanhHieu { get; set; }
    }
}
