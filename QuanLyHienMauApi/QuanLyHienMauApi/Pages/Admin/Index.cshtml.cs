using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using QuanLyHienMauApi.DataContext;
using Microsoft.EntityFrameworkCore;

namespace QuanLyHienMauApi.Pages.Admin
{
    public class IndexModel : PageModel
    {
        private readonly ApplicationDbContext _context;

        public IndexModel(ApplicationDbContext context)
        {
            _context = context;
        }

      
        public int CountChoDuyet { get; set; }
        public int CountXacMinhMau { get; set; }

        public async Task OnGetAsync()
        {

            CountChoDuyet = await _context.CoSoYTes.CountAsync(c => c.TrangThaiDuyet == 0);

            CountXacMinhMau = await _context.CaNhans.CountAsync(c => c.TrangThaiXacMinh == 1);

        }
    }
}
