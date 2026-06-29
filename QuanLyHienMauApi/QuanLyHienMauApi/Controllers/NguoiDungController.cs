using Azure.Core;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Dtos;
using QuanLyHienMauApi.Helpers;
using QuanLyHienMauApi.Models;
using System.Net;
using System.Net.Mail;

namespace QuanLyHienMauApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class NguoiDungController : ControllerBase
    {
        private readonly ApplicationDbContext _context;
        private readonly IWebHostEnvironment _environment;

        public NguoiDungController(ApplicationDbContext context, IWebHostEnvironment environment)
        {
            _context = context;
            _environment = environment;
        }

        // ĐĂNG KÝ
        // ĐĂNG KÝ CÁ NHÂN
        [HttpPost("register-personal")]
        public async Task<IActionResult> RegisterPersonal(Register dto)
        {
            // Kiểm tra Email 
            if (await _context.NguoiDungs.AnyAsync(u => u.Email == dto.Email))
                return BadRequest("Email này đã được sử dụng bởi một tài khoản khác!");

            // Nếu  muốn vẫn giữ SDT là duy nhất thì kiểm tra thêm
            if (await _context.NguoiDungs.AnyAsync(u => u.SDT == dto.SDT))
                return BadRequest("Số điện thoại này đã được đăng ký!");

            // 1. Lưu NguoiDung
            var user = new NguoiDung
            {
                HoTen = dto.HoTen,
                SDT = dto.SDT,
                MatKhau = dto.MatKhau,
                Email = dto.Email,
                DiaChi = dto.DiaChi,
                ViDo = dto.ViDo,
                KinhDo = dto.KinhDo,
                LoaiTaiKhoan = 0
            };
            _context.NguoiDungs.Add(user);
            await _context.SaveChangesAsync(); 

            var caNhan = new CaNhan
            {
                ID = user.ID,
                NgaySinh = dto.NgaySinh,
                NhomMau = null,
                HeRh = null
            };
            _context.CaNhans.Add(caNhan);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Đăng ký thành công!" });
        }

        // ĐĂNG KÝ TỔ CHỨC
        [HttpPost("register-medical")]
        public async Task<IActionResult> RegisterMedical(RegisterMedical dto)
        {
            if (await _context.NguoiDungs.AnyAsync(u => u.Email == dto.Email))
                return BadRequest("Email tổ chức này đã tồn tại trên hệ thống!");

            if (await _context.NguoiDungs.AnyAsync(u => u.SDT == dto.SDT))
                return BadRequest("Số điện thoại tổ chức đã được sử dụng!");

            // 1. Lưu NguoiDung
            var user = new NguoiDung
            {
                HoTen = dto.HoTen,
                SDT = dto.SDT,
                MatKhau = dto.MatKhau,
                Email = dto.Email,
                DiaChi = dto.DiaChi,
                ViDo = dto.ViDo,
                KinhDo = dto.KinhDo,
                LoaiTaiKhoan = 1
            };
            _context.NguoiDungs.Add(user);
            await _context.SaveChangesAsync();

            // 2. Xử lý ảnh và Lưu CoSoYTe
            string pathGiayPhep = await SaveBase64ToImage(dto.AnhGiayPhep, "giayphep");
            string pathQuyetDinh = await SaveBase64ToImage(dto.AnhQuyetDinh, "quyetdinh");

            var csyt = new CoSoYTe
            {
                ID = user.ID,
                MaSoThue = dto.MaSoThue,
                AnhGiayPhep = pathGiayPhep,
                AnhQuyetDinh = pathQuyetDinh,
                TrangThaiDuyet = 0
            };
            _context.CoSoYTes.Add(csyt);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Đăng ký thành công, chờ phê duyệt!" });
        }
        private async Task<string> SaveBase64ToImage(string base64String, string prefix)
        {
            if (string.IsNullOrEmpty(base64String)) return null;

            try
            {
                // Tạo tên file theo ý bạn: ngày_tên_duy_nhất.jpg
                string fileName = $"{DateTime.Now:yyyyMMdd}_{prefix}_{Guid.NewGuid()}.jpg";
                string uploadsFolder = Path.Combine(_environment.WebRootPath, "uploads/documents");

                if (!Directory.Exists(uploadsFolder)) Directory.CreateDirectory(uploadsFolder);

                string filePath = Path.Combine(uploadsFolder, fileName);

                // Giải mã chuỗi Base64 và lưu
                byte[] imageBytes = Convert.FromBase64String(base64String);
                await System.IO.File.WriteAllBytesAsync(filePath, imageBytes);

                return "/uploads/documents/" + fileName; // Trả về đường dẫn để lưu vào DB
            }
            catch
            {
                return null; // Nếu lỗi thì trả về null hoặc xử lý tùy ý
            }
        }

        [HttpPost("forgot-password")]
        public async Task<IActionResult> GuiMaXacNhan(string email)
        {
            var user = await _context.NguoiDungs.AnyAsync(u => u.Email == email);
            if (!user) return NotFound("Email này không tồn tại trên hệ thống!");

            // 1. Tạo mã ngẫu nhiên 6 số
            string otp = new Random().Next(100000, 999999).ToString();

            // 2. Lưu vào bảng XacNhanEmail (Xóa mã cũ của email này nếu có)
            var oldCodes = _context.XacNhanEmails.Where(x => x.Email == email);
            _context.XacNhanEmails.RemoveRange(oldCodes);

            _context.XacNhanEmails.Add(new XacNhanEmail
            {
                Email = email,
                MaXacNhan = otp,
                ThoiGianHetHan = DateTime.Now.AddMinutes(5) // Hết hạn sau 5 phút
            });
            await _context.SaveChangesAsync();

            // 3. Gửi Email 
            _ = Task.Run(() => SendEmail(email, "Mã xác nhận đặt lại mật khẩu", $"Mã của bạn là: {otp}"));

            return Ok("Mã xác nhận đã được gửi vào Email của bạn.");
        }

        [HttpPost("reset-password")]
        public async Task<IActionResult> DatLaiMatKhau([FromBody] ResetPasswordRequest request)
        {
            var check = await _context.XacNhanEmails
                .FirstOrDefaultAsync(x => x.Email == request.Email && x.MaXacNhan == request.MaOTP && x.ThoiGianHetHan > DateTime.Now);

            if (check == null) return BadRequest("Mã xác nhận không đúng hoặc đã hết hạn!");

            var user = await _context.NguoiDungs.FirstOrDefaultAsync(u => u.Email == request.Email);
            if (user != null)
            {
                user.MatKhau = request.MatKhauMoi;
                _context.XacNhanEmails.Remove(check); 
                await _context.SaveChangesAsync();
                return Ok("Đặt lại mật khẩu thành công!");
            }
            return NotFound();
        }

        private void SendEmail(string toEmail, string subject, string body)
        {
            try
            {
                var fromAddress = new MailAddress("huycon368@gmail.com", "Hiến máu nhân đạo");
                var toAddress = new MailAddress(toEmail);
                const string fromPassword = "sphr hlyr jvxf mwdr"; // Đây là App Password của Google

                var smtp = new SmtpClient
                {
                    Host = "smtp.gmail.com",
                    Port = 587,
                    EnableSsl = true,
                    DeliveryMethod = SmtpDeliveryMethod.Network,
                    UseDefaultCredentials = false,
                    Credentials = new NetworkCredential(fromAddress.Address, fromPassword)
                };

                using (var message = new MailMessage(fromAddress, toAddress)
                {
                    Subject = subject,
                    Body = body
                })
                {
                    smtp.Send(message);
                }
            }
            catch (Exception ex)
            {
                // Ghi log lỗi nếu không gửi được mail
                Console.WriteLine("Lỗi gửi mail: " + ex.Message);
            }
        }

        [HttpPost("UpdateToken")]
        public async Task<IActionResult> UpdateToken([FromForm] int userId, [FromForm] string token)
        {
            var user = await _context.NguoiDungs.FindAsync(userId);
            if (user == null) return NotFound("Không tìm thấy người dùng");

            // Cập nhật Token mới nhất
            user.TokenFCM = token;

            await _context.SaveChangesAsync();
            return Ok(new { Message = "Cập nhật Token thành công" });
        }

        
        [HttpGet("GetLocation/{userId}")]
        public async Task<IActionResult> GetLocation(int userId)
        {
            var user = await _context.NguoiDungs
                .Select(u => new { u.ID, u.ViDo, u.KinhDo, u.HoTen })
                .FirstOrDefaultAsync(u => u.ID == userId);

            if (user == null) return NotFound("Không tìm thấy người dùng");

            return Ok(user);
        }

       
        [HttpPost("UpdateLocation")]
        public async Task<IActionResult> UpdateLocation(int userId, double viDo, double kinhDo)
        {
            var user = await _context.NguoiDungs.FindAsync(userId);
            if (user == null) return NotFound();

            user.ViDo = viDo;
            user.KinhDo = kinhDo;

            await _context.SaveChangesAsync();
            return Ok();
        }

        // ĐĂNG NHẬP
        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginRequest request)
        {
            var user = await _context.NguoiDungs
                .FirstOrDefaultAsync(u => u.Email == request.Email && u.MatKhau == request.MatKhau);

            if (user == null)
            {
                return Unauthorized("Email hoặc mật khẩu không chính xác!");
            }

            if (user.LoaiTaiKhoan == 1)
            {
                var csyt = await _context.CoSoYTes.FirstOrDefaultAsync(c => c.ID == user.ID);
                if (csyt != null && csyt.TrangThaiDuyet == 0)
                {
                    return BadRequest("Tài khoản tổ chức của bạn đang chờ phê duyệt!");
                }
            }

            return Ok(user);
        }

        [HttpGet("GetProfile/{userId}")]
        public async Task<IActionResult> GetProfile(int userId)
        {
            var user = await _context.NguoiDungs
                .Include(u => u.CaNhan)
                .Include(u => u.CoSoYTe)
                .FirstOrDefaultAsync(u => u.ID == userId);

            if (user == null) return NotFound("Người dùng không tồn tại");

            // Lấy thông tin chung
            var profile = new UserProfileDto
            {
                Id = user.ID,
                HoTen = user.HoTen,
                Email = user.Email,
                Sdt = user.SDT,
                DiaChi = user.DiaChi,
                LoaiTaiKhoan = user.LoaiTaiKhoan 
            };

            if (user.LoaiTaiKhoan == 0) 
            {
                profile.NhomMau = user.CaNhan?.NhomMau ?? "Chưa rõ";
                profile.HeRh = user.CaNhan?.HeRh ?? "";

               
                profile.SoLanHien = user.CaNhan?.SoLanHien ?? 0;
                profile.NgayHienGanNhat = user.CaNhan?.NgayHienGanNhat;

               
                int count = profile.SoLanHien;
                profile.DanhHieu = count >= 10 ? "Ân nhân máu"
                                 : (count >= 5 ? "Chiến sĩ hồng"
                                 : "Người gieo hy vọng");
            }
            else 
            {
                profile.MaSoThue = user.CoSoYTe?.MaSoThue ?? "Chưa cập nhật";
                profile.DanhHieu = "Cơ sở y tế đối tác";
            }

            return Ok(profile);
        }

        [HttpPost("UpdateProfile")]
        public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileDto updateDto)
        {
            // 1. Tìm User kèm theo thông tin chi tiết (nếu cần)
            var user = await _context.NguoiDungs
                                     .FirstOrDefaultAsync(u => u.ID == updateDto.UserId);

            if (user == null) return NotFound("Không tìm thấy người dùng.");

           
            user.SDT = updateDto.SDT;
            user.DiaChi = updateDto.DiaChi;

           
            user.ViDo = updateDto.ViDo;
            user.KinhDo = updateDto.KinhDo;

           
            if (user.LoaiTaiKhoan == 0)
            {
                user.HoTen = updateDto.HoTen;
            }

            try
            {
                // Lưu chốt toàn bộ các thay đổi xuống CSDL
                await _context.SaveChangesAsync();
                return Ok(new { message = "Cập nhật thông tin hồ sơ định vị thành công!" });
            }
            catch (Exception ex)
            {
                return BadRequest("Lỗi khi cập nhật dữ liệu hồ sơ: " + ex.Message);
            }
        }

        [HttpPost("Logout")]
        public async Task<IActionResult> Logout([FromForm] int userId)
        {
            var user = await _context.NguoiDungs.FindAsync(userId);
            if (user != null)
            {
                user.TokenFCM = null; // Xóa token khi đăng xuất
                await _context.SaveChangesAsync();
            }
            return Ok(new { Message = "Đăng xuất thành công" });
        }

        [HttpPost("UploadVerifyBlood")]
        public async Task<IActionResult> UploadVerifyBlood(
            [FromForm] int userId,
            [FromForm] string nhomMau,
            [FromForm] string heRh,
            [FromForm] List<IFormFile> images) 
        {
            var user = await _context.NguoiDungs
         .Include(u => u.CaNhan)
         .FirstOrDefaultAsync(u => u.ID == userId);

            // KIỂM TRA TỪNG BƯỚC
            if (user == null)
            {
                return NotFound($"LỖI: Không tìm thấy ID {userId} trong bảng NguoiDungs");
            }

            if (user.CaNhan == null)
            {
                return NotFound($"LỖI: Tìm thấy User {userId} nhưng bảng CaNhans lại không có dòng nào khớp ID này!");
            }

            try
            {
                
                string uploadsFolder = Path.Combine(_environment.WebRootPath, "uploads/verify");
                if (!Directory.Exists(uploadsFolder)) Directory.CreateDirectory(uploadsFolder);
                List<string> savedPaths = new List<string>();
                foreach (var image in images)
                {
                    string uniqueFileName = Guid.NewGuid().ToString() + "_" + image.FileName;
                    string filePath = Path.Combine(uploadsFolder, uniqueFileName);
                    using (var fileStream = new FileStream(filePath, FileMode.Create)) { await image.CopyToAsync(fileStream); }
                    savedPaths.Add("/uploads/verify/" + uniqueFileName);
                }

                // 2. CẬP NHẬT NHÓM MÁU NGƯỜI DÙNG KHAI BÁO
                user.CaNhan.NhomMau = nhomMau;
                user.CaNhan.HeRh = heRh;
                user.CaNhan.AnhXacMinh = string.Join(";", savedPaths);
                user.CaNhan.TrangThaiXacMinh = 1; // Chờ duyệt

                await _context.SaveChangesAsync();
                return Ok(new { message = "Đã gửi yêu cầu xác minh!" });
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("DuyetXacMinhMau")]
        public async Task<IActionResult> DuyetXacMinhMau(int id, int status)
        {
            var profile = await _context.CaNhans
                .Include(c => c.ThongTinTaiKhoan)
                .FirstOrDefaultAsync(c => c.ID == id);

            if (profile == null) return NotFound("Không tìm thấy hồ sơ.");

            if (status == 1) // DUYỆT
            {
                profile.TrangThaiXacMinh = 2; // Đã duyệt
                await _context.SaveChangesAsync();

                // Gửi thông báo FCM thành công
                if (!string.IsNullOrEmpty(profile.ThongTinTaiKhoan?.TokenFCM))
                {
                    _ = Task.Run(() => FcmHelper.SendNotification(
                        new List<string> { profile.ThongTinTaiKhoan.TokenFCM },
                        "✅ Xác minh nhóm máu thành công",
                        $"Nhóm máu {profile.NhomMau}{profile.HeRh} của bạn đã được xác thực.",
                        "VerifySuccess", id));
                }
                return Ok(new { message = "Đã phê duyệt thành công!" });
            }
            else // TỪ CHỐI
            {
                profile.TrangThaiXacMinh = 0;
                profile.AnhXacMinh = null; // Xóa link ảnh để người dùng gửi lại
                await _context.SaveChangesAsync();

                // Gửi thông báo FCM từ chối
                if (!string.IsNullOrEmpty(profile.ThongTinTaiKhoan?.TokenFCM))
                {
                    _ = Task.Run(() => FcmHelper.SendNotification(
                        new List<string> { profile.ThongTinTaiKhoan.TokenFCM },
                        "❌ Xác minh nhóm máu bị từ chối",
                        "Ảnh bằng chứng không rõ ràng hoặc không khớp. Vui lòng gửi lại.",
                        "VerifyFailed", id));
                }
                return Ok(new { message = "Đã từ chối hồ sơ." });
            }
        }

        [HttpGet("GetLichSuHienMau/{userId}")]
        public async Task<IActionResult> GetLichSuHienMau(int userId)
        {
            // RÀNG BUỘC CHẮC CHẮN: KẾT THÚC SỰ KIỆN (TrangThai == 2) THÌ MỚI XUẤT HIỆN LỊCH SỬ HIẾN
            var lichSu = await _context.XacNhanHienMaus
                .Include(x => x.YeuCau)
                .Where(x => x.NguoiHienID == userId &&
                            x.TrangThaiConfirm == 2 &&
                            x.YeuCau != null &&
                            x.YeuCau.TrangThai == 2)
                .OrderByDescending(x => x.NgayXacNhan)
                .Select(x => new {
                    x.ID,
                    x.YeuCauID,
                    x.NguoiHienID,
                    x.TrangThaiConfirm,
                    x.NgayXacNhan,
                    x.QRCode,
                    x.NhomMauXacNhan,
                    x.RhXacNhan,
                    x.LuongMau,
                    x.ChieuCao,
                    x.CanNang,
                    x.YeuCau
                })
                .ToListAsync();

            return Ok(lichSu);
        }
    }

    // Class phụ để nhận dữ liệu đăng nhập
    public class LoginRequest
    {
        public string Email { get; set; }
        public string MatKhau { get; set; }
    }
}
