using Microsoft.AspNetCore.Mvc.ModelBinding.Validation;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace QuanLyHienMauApi.Models
{
    public class YeuCauMau
    {
        [Key]
        public int ID { get; set; }

        public int NguoiDangID { get; set; }

        // Thông tin bệnh viện linh hoạt
        public int? BenhVienID { get; set; }
        public string? TenBenhVien { get; set; }
        public string? DiaChiBenhVien { get; set; }
        public double? ViDo { get; set; }
        public double? KinhDo { get; set; }

        public string? NoiDung { get; set; }
        public int? SoNguoiCan { get; set; }

        [NotMapped] 
        public int SoNguoiDaXacNhan { get; set; }
        public int TrangThai { get; set; } = 0;
        public DateTime NgayDang { get; set; } = DateTime.Now;
        public string? NgayBatDau { get; set; }
        public string? NgayKetThuc { get; set; }
        public string? GioBatDau { get; set; }
        public string? GioKetThuc { get; set; }
        public int LoaiTin { get; set; } = 0;


        [ForeignKey("NguoiDangID")]
        [ValidateNever]
        public virtual NguoiDung? NguoiDang { get; set; }

        [ForeignKey("BenhVienID")]
        [ValidateNever]
        public virtual CoSoYTe? BenhVien { get; set; }

        public virtual ICollection<YeuCauMauChiTiet> ChiTiets { get; set; } = new List<YeuCauMauChiTiet>();
    }
}
