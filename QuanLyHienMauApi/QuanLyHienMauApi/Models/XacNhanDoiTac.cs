using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace QuanLyHienMauApi.Models
{
    public class XacNhanDoiTac
    {
        [Key]
        public int ID { get; set; }

        public int YeuCauID { get; set; }

        public int BenhVienChiaSeID { get; set; }

        public DateTime NgayXacNhan { get; set; } = DateTime.Now;

        public int TrangThai { get; set; } = 0; 

       
        [ForeignKey("YeuCauID")]
        public virtual YeuCauMau YeuCau { get; set; }

        [ForeignKey("BenhVienChiaSeID")]
        public virtual NguoiDung BenhVienChiaSe { get; set; }
    }
}
