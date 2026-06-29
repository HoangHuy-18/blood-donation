using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace QuanLyHienMauApi.Models
{
    public class XacNhanHienMau
    {
        [Key]
        public int ID { get; set; }

        public int? YeuCauID { get; set; }  

        [Required]
        public int NguoiHienID { get; set; }

        public int TrangThaiConfirm { get; set; } = 0;

        public DateTime NgayXacNhan { get; set; }
        public string? QRCode { get; set; }
        public int? ChieuCao { get; set; }      
        public double? CanNang { get; set; }    
        public int? HuyetApTamThu { get; set; } 
        public int? HuyetApTamTruong { get; set; }
        public string? NhomMauXacNhan { get; set; } 
        public string? RhXacNhan { get; set; }  
        public int? LuongMau { get; set; }

        [ForeignKey("YeuCauID")]
        public virtual YeuCauMau? YeuCau { get; set; }

        [ForeignKey("NguoiHienID")]
        public virtual NguoiDung? NguoiHien { get; set; }
    }
}
    