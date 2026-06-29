using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace QuanLyHienMauApi.Models
{
    public class CoSoYTe
    {
        [Key]
        [ForeignKey("ThongTinTaiKhoan")]
        public int ID { get; set; }

        public string? MaSoThue { get; set; }

        public string? AnhGiayPhep { get; set; }

        public string? AnhQuyetDinh { get; set; }

        public int TrangThaiDuyet { get; set; } = 0;

        // Liên kết ngược lại bảng NguoiDung
        public virtual NguoiDung? ThongTinTaiKhoan { get; set; }
    }
}

