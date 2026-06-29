using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace QuanLyHienMauApi.Models
{
    public class BaoCaoYeuCau
    {
        [Key]
        public int ID { get; set; }

        // Liên kết tới bài viết đăng tin hiến máu (YeuCauMau)
        public int YeuCauID { get; set; }
        [ForeignKey("YeuCauID")]
        public virtual YeuCauMau? YeuCau { get; set; }

        // Liên kết tới người bấm báo cáo (NguoiDung)
        public int NguoiBaoCaoID { get; set; }
        [ForeignKey("NguoiBaoCaoID")]
        public virtual NguoiDung? NguoiBaoCao { get; set; }

        [Required]
        public string LyDo { get; set; } // "Tin giả / Sai sự thật", "Spam", "Ngôn từ không phù hợp"

        public string? ChiTiet { get; set; }

        public DateTime NgayBaoCao { get; set; } = DateTime.Now;

        // Trạng thái xử lý của Admin
        // 0: Chờ xử lý, 1: Đã duyệt báo cáo (Khóa bài viết), 2: Hủy báo cáo (Bài viết an toàn)
        public int TrangThai { get; set; } = 0;
    }
}
