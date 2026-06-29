namespace QuanLyHienMauApi.Dtos
{
    public class Register
    {
        public string HoTen { get; set; }
        public string SDT { get; set; }
        public string MatKhau { get; set; }
        public string Email { get; set; }
        public string DiaChi { get; set; }
        public double ViDo { get; set; }
        public double KinhDo { get; set; }
        public DateTime NgaySinh { get; set; }
    }

    public class RegisterMedical
    {
        public string HoTen { get; set; } 
        public string SDT { get; set; }
        public string MatKhau { get; set; }
        public string Email { get; set; }
        public string DiaChi { get; set; }
        public double ViDo { get; set; }
        public double KinhDo { get; set; }

        public string MaSoThue { get; set; }
        public string? AnhGiayPhep { get; set; } 
        public string? AnhQuyetDinh { get; set; }

    }
}
