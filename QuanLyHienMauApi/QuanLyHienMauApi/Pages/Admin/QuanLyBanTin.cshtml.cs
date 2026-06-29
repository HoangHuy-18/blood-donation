using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Models;

namespace QuanLyHienMauApi.Pages.Admin
{
    public class QuanLyBanTinModel : PageModel
    {
        private readonly ApplicationDbContext _context;
        public QuanLyBanTinModel(ApplicationDbContext context) => _context = context;

        public int TotalUrgent { get; set; }
        public int TotalEvents { get; set; }
        public int TotalBloodCollected { get; set; }

        public int CurrentTab { get; set; }
        public List<YeuCauMau> ListBanTin { get; set; } = new();

        public async Task OnGetAsync(int? loaiTin, string search, int trangThai = 0)
        {
            CurrentTab = trangThai;

            IQueryable<YeuCauMau> query = _context.YeuCauMaus
            .Include(y => y.ChiTiets)
            .Include(y => y.NguoiDang);

            if (trangThai == 0)
            {
                // Tab Đang mở: Hiện cả tin đang hiện (0) và tin đang ẩn (1)
                query = query.Where(y => y.TrangThai == 0 || y.TrangThai == 1);

                TotalUrgent = await _context.YeuCauMaus.CountAsync(y => (y.TrangThai == 0 || y.TrangThai == 1) && y.LoaiTin == 0);
                TotalEvents = await _context.YeuCauMaus.CountAsync(y => (y.TrangThai == 0 || y.TrangThai == 1) && y.LoaiTin == 1);
            }
            else
            {
               
                query = query.Where(y => y.TrangThai == 2);

                TotalUrgent = await _context.YeuCauMaus.CountAsync(y => y.TrangThai == 2 && y.LoaiTin == 0);
                TotalEvents = await _context.YeuCauMaus.CountAsync(y => y.TrangThai == 2 && y.LoaiTin == 1);
            }

            // Tổng máu luôn lấy từ các ca đã hiến thành công (TrangThaiConfirm = 2)
            TotalBloodCollected = await _context.XacNhanHienMaus
                .Where(x => x.TrangThaiConfirm == 2)
                .SumAsync(x => x.LuongMau ?? 0);

            if (loaiTin.HasValue) query = query.Where(y => y.LoaiTin == loaiTin);
            if (!string.IsNullOrEmpty(search))
                query = query.Where(y => y.TenBenhVien.Contains(search) || y.NoiDung.Contains(search));

            ListBanTin = await query.OrderByDescending(y => y.NgayDang).ToListAsync();
        }
    }
}
