using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Helpers;
using QuanLyHienMauApi.Models;

namespace QuanLyHienMauApi.Pages.Admin
{
    public class DuyetNhomMauModel : PageModel
    {
        private readonly ApplicationDbContext _context;
        public DuyetNhomMauModel(ApplicationDbContext context) => _context = context;

        public List<CaNhan> ListChoDuyet { get; set; }

        public async Task OnGetAsync()
        {
            ListChoDuyet = await _context.CaNhans
                .Include(c => c.ThongTinTaiKhoan)
                .Where(c => c.TrangThaiXacMinh == 1)
                .ToListAsync();
        }
    }
}
