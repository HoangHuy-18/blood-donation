using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using System.Security.Claims;

namespace QuanLyHienMauApi.Pages.Admin
{
    public class LoginModel : PageModel
    {
        private readonly ApplicationDbContext _context;
        public LoginModel(ApplicationDbContext context) => _context = context;

        [BindProperty] public string SDT { get; set; }
        [BindProperty] public string MatKhau { get; set; }
        public string Message { get; set; }

        public async Task<IActionResult> OnPostAsync()
        {

            if (string.IsNullOrEmpty(SDT) || string.IsNullOrEmpty(MatKhau))
            {
                Message = "Vui lòng nhập đầy đủ số điện thoại và mật khẩu!";
                return Page();
            }
            

            string sdtTrim = SDT.Trim();
            string passTrim = MatKhau.Trim();
            
            var admin = await _context.NguoiDungs
                .FirstOrDefaultAsync(u => u.SDT == sdtTrim
                                       && u.MatKhau == passTrim
                                       && u.LoaiTaiKhoan == 2);

            if (admin != null)
            {
                var claims = new List<Claim> {
            new Claim(ClaimTypes.Name, admin.HoTen),
            new Claim(ClaimTypes.Role, "Admin")
        };

                var claimsIdentity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
                await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, new ClaimsPrincipal(claimsIdentity));

                return RedirectToPage("/Admin/Index");
            }

            Message = "Sai thông tin hoặc bạn không có quyền truy cập!";
            return Page();
        }
    }
}
