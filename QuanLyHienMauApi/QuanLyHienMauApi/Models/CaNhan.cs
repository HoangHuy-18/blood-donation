using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace QuanLyHienMauApi.Models
{
    public class CaNhan
    {
        [Key]
        [ForeignKey("ThongTinTaiKhoan")]
        public int ID { get; set; }

        public string? NhomMau { get; set; }
        public string? HeRh { get; set; }

        public DateTime? NgaySinh { get; set; }
        public int SoLanHien { get; set; } = 0;
        public DateTime? NgayHienGanNhat { get; set; }
        public string? AnhXacMinh { get; set; }
        public int TrangThaiXacMinh { get; set; } = 0;
        public int? ChieuCao { get; set; }     
        public double? CanNang { get; set; }
        public virtual NguoiDung? ThongTinTaiKhoan { get; set; }
    }
}

