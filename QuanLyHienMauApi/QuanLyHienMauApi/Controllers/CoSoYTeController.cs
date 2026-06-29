using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Dtos;
using System.Net;
using System.Net.Mail;

namespace QuanLyHienMauApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class CoSoYTeController : ControllerBase
    {
        private readonly ApplicationDbContext _context;
        private readonly IWebHostEnvironment _environment;

        public CoSoYTeController(ApplicationDbContext context, IWebHostEnvironment environment)
        {
            _context = context;
            _environment = environment;
        }

        [HttpPost("upload-ho-so")]
        public async Task<IActionResult> UploadHoSo([FromForm] UploadHoSo request)
        {
            // Kiểm tra file từ request
            if (request.File == null || request.File.Length == 0)
                return BadRequest("Không có tệp tin nào được chọn!");

            // 1. Tạo thư mục lưu trữ
            string uploadsFolder = Path.Combine(_environment.WebRootPath, "uploads/documents");
            if (!Directory.Exists(uploadsFolder)) Directory.CreateDirectory(uploadsFolder);

                // 2. Tạo tên file duy nhất
            string uniqueFileName = Guid.NewGuid().ToString() + "_" + request.File.FileName;
            string filePath = Path.Combine(uploadsFolder, uniqueFileName);

            // 3. Lưu file
            using (var fileStream = new FileStream(filePath, FileMode.Create))
            {
                await request.File.CopyToAsync(fileStream);
            }

            // 4. Cập nhật Database
            var csyt = await _context.CoSoYTes.FindAsync(request.CoSoID);
            if (csyt == null) return NotFound("Không tìm thấy cơ sở y tế!");

            string fileUrl = "/uploads/documents/" + uniqueFileName;

            if (request.LoaiAnh == "GiayPhep") csyt.AnhGiayPhep = fileUrl;
            else if (request.LoaiAnh == "QuyetDinh") csyt.AnhQuyetDinh = fileUrl;

            await _context.SaveChangesAsync();

            return Ok(new { message = "Upload thành công!", path = fileUrl });
        }

        [HttpPost("DuyetHoSo")]
        public async Task<IActionResult> DuyetHoSo(int id, int status, string? lyDo)
        {
            var csyt = await _context.CoSoYTes
                .Include(c => c.ThongTinTaiKhoan)
                .FirstOrDefaultAsync(c => c.ID == id);

            if (csyt == null) return NotFound("Không tìm thấy thông tin hồ sơ của cơ sở này!");

            string emailNguoiDung = csyt.ThongTinTaiKhoan?.Email;
            string hoTen = csyt.ThongTinTaiKhoan?.HoTen ?? "Cơ sở y tế";
            string tieuDe = "Thông báo kết quả duyệt hồ sơ pháp lý - Blood Connect";

          
            if (status == 1)
            {
                csyt.TrangThaiDuyet = 1; 
                await _context.SaveChangesAsync();
               
                if (!string.IsNullOrEmpty(emailNguoiDung))
                {
                    string noiDungDuyet = $"Chào {hoTen},\n\n" +
                                          $"Chúc mừng bạn! Hồ sơ xác thực pháp lý tổ chức của bạn đã được Ban quản trị hệ thống Blood Connect phê duyệt thành công.\n\n" +
                                          $"Hiện tại, tài khoản đã được kích hoạt hoàn toàn. Bạn đã có thể mở ứng dụng di động để đăng nhập, thực hiện đăng tin điều phối máu khẩn cấp hoặc khởi tạo các chiến dịch hiến máu lưu động.\n\n" +
                                          $"Trân trọng cảm ơn sự đồng hành của bạn!";

                    _ = Task.Run(() => {
                        try { SendEmail(emailNguoiDung, tieuDe, noiDungDuyet); }
                        catch (Exception ex) { Console.WriteLine("Lỗi gửi mail phê duyệt ngầm: " + ex.Message); }
                    });
                }

                return Ok(new { message = "Đã phê duyệt và gửi email chúc mừng thành công!" });
            }
           
            else if (status == 2)
            {
                if (!string.IsNullOrEmpty(emailNguoiDung))
                {
                    string noiDungTuChoi = $"Chào {hoTen},\n\nHồ sơ đăng ký tài khoản tổ chức của bạn đã bị từ chối do lý do sau đây:\n- {lyDo}\n\nVui lòng kiểm tra lại tính minh chứng của tài liệu chứng từ và tiến hành thực hiện đăng ký lại trên ứng dụng.\n\nTrân trọng!";

                    _ = Task.Run(() => {
                        try { SendEmail(emailNguoiDung, tieuDe, noiDungTuChoi); }
                        catch (Exception ex) { Console.WriteLine("Lỗi gửi mail từ chối ngầm: " + ex.Message); }
                    });
                }

              
                var listFiles = new List<string>();
                if (!string.IsNullOrEmpty(csyt.AnhGiayPhep)) listFiles.Add(csyt.AnhGiayPhep);
                if (!string.IsNullOrEmpty(csyt.AnhQuyetDinh)) listFiles.Add(csyt.AnhQuyetDinh);

                var user = csyt.ThongTinTaiKhoan;
                _context.CoSoYTes.Remove(csyt);
                if (user != null) _context.NguoiDungs.Remove(user);
                await _context.SaveChangesAsync();

                foreach (var relativePath in listFiles)
                {
                    string physicalPath = Path.Combine(_environment.WebRootPath, relativePath.TrimStart('/'));
                    if (System.IO.File.Exists(physicalPath)) System.IO.File.Delete(physicalPath);
                }

                return Ok(new { message = "Đã gửi mail thông báo từ chối và quét sạch hồ sơ rác!" });
            }
            return BadRequest("Yêu cầu thao tác không hợp lệ.");
        }

        private void SendEmail(string toEmail, string subject, string body)
        {
            try
            {
                var fromAddress = new MailAddress("huycon368@gmail.com", "Hiến máu nhân đạo");
                var toAddress = new MailAddress(toEmail);
                const string fromPassword = "sphr hlyr jvxf mwdr";

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

        [HttpGet("GetFixedPoints")]
        public IActionResult GetFixedPoints()
        {
            var points = new List<DiemHienMauCoDinhDto>
        {
            new DiemHienMauCoDinhDto {
                TenDiaDiem = "Điểm hiến máu cố định Hoàn Kiếm",
                DiaChi = "18 Quán Sứ, Hoàn Kiếm, Hà Nội",
                SDT = "02437183154",
                ThoiGianLamViec = "8h - 11h45 & 13h30 - 16h30 | Thứ 3 - Chủ nhật",
                ViDo = 21.029156153720137, KinhDo = 105.84621103152722
            },
            new DiemHienMauCoDinhDto {
                TenDiaDiem = "Điểm hiến máu cố định quận Thanh Xuân",
                DiaChi = "132 Quan Nhân, Thanh Xuân, Hà Nội",
                SDT = "02432079699",
                ThoiGianLamViec = "8h - 11h45 & 13h30 - 16h30 | Thứ 3 - Chủ nhật",
                ViDo = 21.00582960552321, KinhDo = 105.81147546355673
            },
            new DiemHienMauCoDinhDto {
                TenDiaDiem = "Điểm hiến máu cố định Đống Đa",
                DiaChi = "Số 1 Hoàng Cầu, Đống Đa, Hà Nội",
                SDT = "02432066267",
                ThoiGianLamViec = "8h - 11h45 & 13h30 - 16h30 | Thứ 3 - Chủ nhật",
                ViDo = 21.020172780856118, KinhDo = 105.82559412419928
            }
        };
            return Ok(points);
        }
    }
}
