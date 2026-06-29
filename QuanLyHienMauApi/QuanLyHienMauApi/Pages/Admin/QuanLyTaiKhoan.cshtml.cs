using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Models;

namespace QuanLyHienMauApi.Pages.Admin
{
    public class QuanLyTaiKhoanModel : PageModel
    {
        private readonly ApplicationDbContext _context;
        public QuanLyTaiKhoanModel(ApplicationDbContext context) => _context = context;

        public List<NguoiDung> DanhSachTaiKhoan { get; set; } = new();
        public int TongCaNhan { get; set; }
        public int TongCoSo { get; set; }

        public async Task OnGetAsync(string search, int? loaitk)
        {
            // Thống kê nhanh
            TongCaNhan = await _context.NguoiDungs.CountAsync(u => u.LoaiTaiKhoan == 0);
            TongCoSo = await _context.NguoiDungs.CountAsync(u => u.LoaiTaiKhoan == 1);

            var query = _context.NguoiDungs.AsQueryable();

            // Lọc theo loại tài khoản
            if (loaitk.HasValue) query = query.Where(u => u.LoaiTaiKhoan == loaitk);

            // Tìm kiếm theo tên hoặc SĐT
            if (!string.IsNullOrEmpty(search))
                query = query.Where(u => u.HoTen.Contains(search) || u.SDT.Contains(search));

            DanhSachTaiKhoan = await query.OrderByDescending(u => u.ID).ToListAsync();
        }

        // Handler xử lý khóa/mở khóa tài khoản
        public async Task<IActionResult> OnPostToggleLockAsync(int id)
        {
            var user = await _context.NguoiDungs.FindAsync(id);
            if (user == null) return NotFound();

          
            user.TrangThai = (user.TrangThai == 0) ? 1 : 0;

            await _context.SaveChangesAsync();
            return RedirectToPage();
        }
    }
}
