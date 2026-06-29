using System.ComponentModel.DataAnnotations;

namespace QuanLyHienMauApi.Models
{
    public class NguoiDung
    {
        [Key]
        public int ID { get; set; }

        [Required]
        [StringLength(100)]
        public string HoTen { get; set; }

        [Required]
        [StringLength(15)]
        public string SDT { get; set; }

        [Required] [StringLength(15)]
        public string MatKhau { get; set; }

        [Required]
        [StringLength(100)]
        public string Email { get; set; }

        public string? DiaChi { get; set; }
        public double? ViDo { get; set; }
        public double? KinhDo { get; set; }

        public int LoaiTaiKhoan { get; set; } = 0; // 0: CaNhan, 1: CoSoYTe, 2: Admin
        public int TrangThai { get; set; } = 0;

        public string? TokenFCM { get; set; }

        public virtual CaNhan? CaNhan { get; set; }
        public virtual CoSoYTe? CoSoYTe { get; set; }
    }
}
