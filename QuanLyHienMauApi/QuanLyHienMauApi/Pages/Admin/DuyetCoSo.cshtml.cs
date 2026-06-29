using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Models;

namespace QuanLyHienMauApi.Pages.Admin
{
    public class DuyetCoSoModel : PageModel
    {
        private readonly ApplicationDbContext _context;

        public DuyetCoSoModel(ApplicationDbContext context)
        {
            _context = context;
        }

        public List<CoSoYTe> DanhSachCho { get; set; }

        public async Task OnGetAsync()
        {
            DanhSachCho = await _context.CoSoYTes
                .Include(c => c.ThongTinTaiKhoan)
                .Where(c => c.TrangThaiDuyet == 0)
                .ToListAsync();
        }
    }
}

