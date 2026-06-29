using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace QuanLyHienMauApi.Models
{
    public class YeuCauMauChiTiet
    {
        [Key]
        public int ID { get; set; }

        public int YeuCauID { get; set; }

        [Required]
        public string NhomMau { get; set; }
        [Required]
        public string Rh { get; set; }

        public int? SoDonVi { get; set; }

        public int MucDoKhanCap { get; set; } = 1;

        [ForeignKey("YeuCauID")]
        public virtual YeuCauMau? YeuCau { get; set; }
    }
}
