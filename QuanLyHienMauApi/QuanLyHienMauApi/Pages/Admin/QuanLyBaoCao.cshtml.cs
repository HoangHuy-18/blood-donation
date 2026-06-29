using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Models;

namespace QuanLyHienMauApi.Pages.Admin
{
    public class QuanLyBaoCaoModel : PageModel
    {
        private readonly ApplicationDbContext _context;

        public QuanLyBaoCaoModel(ApplicationDbContext context)
        {
            _context = context;
        }

        public List<BaoCaoYeuCau> DanhSachBaoCao { get; set; }

        public async Task OnGetAsync()
        {
            
            DanhSachBaoCao = await _context.BaoCaoYeuCaus
                .Include(b => b.NguoiBaoCao)
                .Include(b => b.YeuCau)
                    .ThenInclude(y => y.NguoiDang)
                .Include(b => b.YeuCau)
                    .ThenInclude(y => y.ChiTiets)
                .Where(b => b.TrangThai == 0)
                .OrderByDescending(b => b.NgayBaoCao)
                .ToListAsync();
        }
    }
}
