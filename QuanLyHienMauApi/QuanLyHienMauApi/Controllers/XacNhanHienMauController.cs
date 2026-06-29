using FirebaseAdmin.Messaging;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Dtos;
using QuanLyHienMauApi.Helpers;
using QuanLyHienMauApi.Models;
using System;
using System.Collections.Generic;


namespace QuanLyHienMauApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class XacNhanHienMauController : ControllerBase
    {
        private readonly ApplicationDbContext _context;

        public XacNhanHienMauController(ApplicationDbContext context)
        {
            _context = context;
        }

        [HttpGet("BaiVietCuaToi/{userId}")]
        public async Task<IActionResult> GetMyPosts(int userId)
        {
            var list = await _context.XacNhanHienMaus
                .Include(x => x.YeuCau)
                    .ThenInclude(y => y.ChiTiets)
                .Include(x => x.NguoiHien)
                .Where(x => x.YeuCau.NguoiDangID == userId)
                .ToListAsync();
            return Ok(list);
        }

        [HttpPost("DangKyHienMau")]
        public async Task<IActionResult> DangKyHienMau(int yeuCauId, int nguoiHienId)
        {
            try
            {
                // 1. Kiểm tra đăng ký
                var check = await _context.XacNhanHienMaus
                    .AnyAsync(x => x.YeuCauID == yeuCauId && x.NguoiHienID == nguoiHienId);
                if (check) return BadRequest("Bạn đã đăng ký bài viết này rồi.");

                var lanHienCuoi = await _context.XacNhanHienMaus
                    .Where(x => x.NguoiHienID == nguoiHienId && x.TrangThaiConfirm == 2)
                    .OrderByDescending(x => x.NgayXacNhan) 
                    .FirstOrDefaultAsync();

                if (lanHienCuoi != null)
                {
                   
                    DateTime ngayHienThucTe = lanHienCuoi.NgayXacNhan;
                    TimeSpan khoangCach = DateTime.Now - ngayHienThucTe;

                    if (khoangCach.TotalDays < 85) 
                    {
                        int soNgayConLai = 85 - (int)khoangCach.TotalDays;
                        return BadRequest($"Bạn vừa hiến máu vào ngày {ngayHienThucTe:dd/MM/yyyy}. " +
                                         $"Vui lòng đợi thêm {soNgayConLai} ngày nữa để đảm bảo sức khỏe.");
                    }
                }

                // 2. Tạo mã QR và bản ghi mới
                string uniqueQrCode = "HM-" + Guid.NewGuid().ToString().Substring(0, 8).ToUpper();

                var xacNhan = new XacNhanHienMau
                {
                    YeuCauID = yeuCauId,
                    NguoiHienID = nguoiHienId,
                    TrangThaiConfirm = 0,
                    NgayXacNhan = DateTime.Now,
                    QRCode = uniqueQrCode
                };

                _context.XacNhanHienMaus.Add(xacNhan);
                await _context.SaveChangesAsync();

                // 3. Lấy thông tin để gửi thông báo
                var yeuCau = await _context.YeuCauMaus
                    .Include(y => y.NguoiDang)
                    .FirstOrDefaultAsync(y => y.ID == yeuCauId);

                var nguoiHien = await _context.NguoiDungs.FindAsync(nguoiHienId);

                // Kiểm tra an toàn trước khi xây dựng nội dung thông báo
                if (yeuCau != null && nguoiHien != null && yeuCau.NguoiDang?.TokenFCM != null)
                {
                    string title = "";
                    string body = "";
                    string hospitalName = yeuCau.TenBenhVien ?? "Cơ sở y tế";
                    int loaiTK = yeuCau.NguoiDang.LoaiTaiKhoan;

                    if (yeuCau.LoaiTin == 1) // SỰ KIỆN
                    {
                        title = "🔔 Có tình nguyện viên mới!";
                        body = $"{nguoiHien.HoTen} vừa đăng ký tham gia sự kiện tại {hospitalName}.";
                    }
                    else // KHẨN CẤP
                    {
                        if (loaiTK == 1) // Bệnh viện đăng
                        {
                            title = "🏥 Bệnh viện: Có người đến hỗ trợ!";
                            body = $"Tình nguyện viên {nguoiHien.HoTen} đã đăng ký đến hỗ trợ ca cấp cứu tại {hospitalName}.";
                        }
                        else // Cá nhân đăng
                        {
                            title = "🆘 Có người đang đến giúp bạn!";
                            body = $"{nguoiHien.HoTen} đã xác nhận sẽ đến hiến máu hỗ trợ bạn tại {hospitalName}.";
                        }
                    }

                    // 4. Gửi thông báo (Sử dụng await trực tiếp)
                    var tokenList = new List<string> { yeuCau.NguoiDang.TokenFCM };
                    await FcmHelper.SendNotification(tokenList, title, body, "NewRegistration", yeuCauId);
                }

                return Ok(new { Message = "Đăng ký thành công!", QrCode = uniqueQrCode });
            }
            catch (Exception ex)
            {
                return BadRequest("Lỗi hệ thống: " + ex.Message);
            }
        }

        [HttpPost("XacNhanDenHien")]
        public async Task<IActionResult> XacNhanDenHien(int xacNhanId)
        {
            try
            {
                // 1. Lấy thông tin xác nhận kèm theo bài đăng và người đăng bài
                var xn = await _context.XacNhanHienMaus
                    .Include(x => x.YeuCau)
                        .ThenInclude(y => y.NguoiDang) 
                    .Include(x => x.NguoiHien)
                    .FirstOrDefaultAsync(x => x.ID == xacNhanId);

                if (xn == null) return NotFound("Không tìm thấy bản ghi xác nhận.");

                // 2. Cập nhật trạng thái
                xn.TrangThaiConfirm = 1;
               
                await _context.SaveChangesAsync();

                // 3. Gửi thông báo cho Người Hiến (Donor)
                if (xn.NguoiHien?.TokenFCM != null)
                {
                    string title = "✅ Xác nhận thành công!";
                    string body = "";
                    string hospitalName = xn.YeuCau?.TenBenhVien ?? "Cơ sở y tế";
                    int loaiTK = xn.YeuCau?.NguoiDang?.LoaiTaiKhoan ?? 0;
                    int loaiTin = xn.YeuCau?.LoaiTin ?? 0; // 1: Sự kiện, 0: Khẩn cấp

                    // PHÂN LOẠI THÔNG BÁO THEO LOẠI TIN VÀ LOẠI TÀI KHOẢN
                    if (loaiTin == 1)
                    {
                       
                        title = "📅 Hẹn gặp bạn tại sự kiện!";
                        body = $"Bạn đã được xác nhận tham gia ngày hội hiến máu tại {hospitalName}. Hẹn gặp bạn tại địa điểm sự kiện nhé!";
                    }
                    else
                    {
                        // TRƯỜNG HỢP 2 & 3: TIN KHẨN CẤP (LoaiTin == 0)
                        if (loaiTK == 1)
                        {
                            // Bệnh viện đăng khẩn cấp
                            title = "✅ Bệnh viện đã chốt hỗ trợ";
                            body = $"Bệnh viện {hospitalName} đã xác nhận bạn sẽ đến hiến máu. Mời bạn di chuyển đến khoa huyết học để làm thủ tục.";
                        }
                        else
                        {
                            // Cá nhân đăng khẩn cấp
                            title = "❤️ Cảm ơn bạn đã hỗ trợ";
                            body = $"Người đăng tin tại {hospitalName} đã chọn bạn giúp đỡ. Hãy di chuyển đến bệnh viện để hỗ trợ họ ngay nhé!";
                        }
                    }

                    try
                    {
                        var tokenList = new List<string> { xn.NguoiHien.TokenFCM };
                        await FcmHelper.SendNotification(tokenList, title, body, "ConfirmedToCome", xn.YeuCauID ?? 0);
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine("Lỗi gửi FCM cho người hiến: " + ex.Message);
                    }
                }

                return Ok("Đã xác nhận tình nguyện viên.");
            }
            catch (Exception ex)
            {
                return BadRequest("Lỗi: " + ex.Message);
            }
        }

        [HttpPost("XacNhanDenHienBangQR/{qrCode}")]
        public async Task<IActionResult> XacNhanDenHienBangQR(string qrCode)
        {
            try
            {
                // 1. Làm sạch chuỗi đầu vào (Rất quan trọng)
                if (string.IsNullOrEmpty(qrCode)) return BadRequest("Mã QR trống.");
                string cleanQrCode = qrCode.Trim().ToLower();

                // 2. Tìm bản ghi (Sử dụng Equals hoặc so sánh trực tiếp)
                var xn = await _context.XacNhanHienMaus
                    .Include(x => x.NguoiHien)
                    .Include(x => x.YeuCau)
                    .FirstOrDefaultAsync(x => x.QRCode != null && x.QRCode.ToLower() == cleanQrCode);

                // 3. Nếu không tìm thấy, hãy log ra để Debug
                if (xn == null)
                {
                    // Dòng này giúp bạn xem trong bảng Output của Visual Studio xem Server thực tế nhận được gì
                    Console.WriteLine($"[DEBUG] Khong tim thay QR: '{cleanQrCode}'");
                    return NotFound($"Mã QR '{qrCode}' không tồn tại trong hệ thống.");
                }

                if (xn.TrangThaiConfirm >= 2) return BadRequest("Tình nguyện viên này đã hoàn tất hiến máu.");

                xn.TrangThaiConfirm = 1;
                await _context.SaveChangesAsync();

                // 4. Thông báo cho người hiến
                if (xn.NguoiHien?.TokenFCM != null)
                {
                    string title = "📍 Check-in thành công";
                    string body = $"Bạn đã được xác nhận có mặt tại {xn.YeuCau?.TenBenhVien}.";
                    await FcmHelper.SendNotification(new List<string> { xn.NguoiHien.TokenFCM }, title, body, "CheckInSuccess", xn.YeuCauID ?? 0);
                }

                return Ok(new { Message = "Check-in thành công!", HoTen = xn.NguoiHien?.HoTen });
            }
            catch (Exception ex)
            {
                return BadRequest("Lỗi Check-in: " + ex.Message);
            }
        }

        [HttpGet("GetThongTinBangQR/{qrCode}")]
        public async Task<IActionResult> GetThongTinBangQR(string qrCode)
        {
            var xn = await _context.XacNhanHienMaus
                .Include(x => x.NguoiHien)
                    .ThenInclude(u => u.CaNhan)
                .Include(x => x.YeuCau)
                .FirstOrDefaultAsync(x => x.QRCode == qrCode);

            if (xn == null) return NotFound("Mã QR không hợp lệ.");
            if (xn.TrangThaiConfirm >= 2) return BadRequest("Tình nguyện viên này đã hoàn tất hiến máu rồi.");

            // Trả về dữ liệu để Android điền sẵn vào Form (Họ tên, Nhóm máu hiện tại...)
            return Ok(new
            {
                XacNhanId = xn.ID,
                HoTen = xn.NguoiHien?.HoTen,
                Sdt = xn.NguoiHien?.SDT,
                NhomMauHienTai = xn.NguoiHien?.CaNhan?.NhomMau,
                HeRhHienTai = xn.NguoiHien?.CaNhan?.HeRh,
                TenSuKien = xn.YeuCau?.TenBenhVien
            });
        }

        [HttpPost("HoanTatHienMau")]
        public async Task<IActionResult> HoanTatHienMau([FromBody] KetQuaHienMau request)
        {
            return await CoreLogicHoanTat(request);
        }

        [HttpPost("XacNhanThuCong")]
        public async Task<IActionResult> MedicalConfirm(int xacNhanID)
        {
           
            var xn = await _context.XacNhanHienMaus
                .Include(x => x.NguoiHien).ThenInclude(u => u.CaNhan)
                .FirstOrDefaultAsync(x => x.ID == xacNhanID);

            if (xn == null) return NotFound("Không tìm thấy bản ghi.");

            
            int chieuCaoThucTe = xn.ChieuCao ?? 165;
            double canNangThucTe = xn.CanNang ?? 55.0;

           
            var lanHienTruoc = await _context.XacNhanHienMaus
                .Where(x => x.NguoiHienID == xn.NguoiHienID && x.TrangThaiConfirm == 2 && x.ChieuCao != null)
                .OrderByDescending(x => x.NgayXacNhan)
                .FirstOrDefaultAsync();

            if (lanHienTruoc != null)
            {
                chieuCaoThucTe = lanHienTruoc.ChieuCao ?? 165;
                canNangThucTe = lanHienTruoc.CanNang ?? 55.0;
            }

            // 3. Đóng gói dữ liệu bàn giao cho hàm Core xử lý
            var request = new KetQuaHienMau
            {
                XacNhanId = xacNhanID,
                NhomMau = xn.NguoiHien?.CaNhan?.NhomMau ?? "O",
                HeRh = xn.NguoiHien?.CaNhan?.HeRh ?? "+",
                LuongMau = xn.LuongMau ?? 250,
                NgayHien = DateTime.Now,
                ChieuCao = chieuCaoThucTe,
                CanNang = canNangThucTe,
                HuyetApTamThu = 120,
                HuyetApTamTruong = 80
            };

            return await CoreLogicHoanTat(request);
        }

        private async Task<IActionResult> CoreLogicHoanTat(KetQuaHienMau request)
        {
            using (var transaction = await _context.Database.BeginTransactionAsync())
            {
                try
                {
                    var xn = await _context.XacNhanHienMaus
                        .Include(x => x.YeuCau)
                        .Include(x => x.NguoiHien).ThenInclude(u => u.CaNhan)
                        .FirstOrDefaultAsync(x => x.ID == request.XacNhanId);

                    if (xn == null) return NotFound("Bản ghi không tồn tại.");

                    if (request.HuyetApTamThu > 140 || request.HuyetApTamThu < 90 ||
                        request.HuyetApTamTruong > 90 || request.HuyetApTamTruong < 60)
                    {
                        return BadRequest("Cảnh báo an toàn: Chỉ số Huyết áp nằm ngoài ngưỡng cho phép hiến máu! " +
                                         "Hệ thống tự động khóa lệnh để bảo vệ sức khỏe của tình nguyện viên.");
                    }

                   
                    double chieuCaoMet = (double)request.ChieuCao / 100;
                    double bmi = request.CanNang / (chieuCaoMet * chieuCaoMet);

                    
                    if (bmi < 18.5 && request.LuongMau > 250)
                    {
                        return BadRequest($"Tình nguyện viên có thể trạng gầy (BMI: {bmi:F1} < 18.5). " +
                                         "Hệ thống khuyến nghị chỉ cho phép tiếp nhận thể tích máu tối đa là 250ml.");
                    }
                   
                    xn.TrangThaiConfirm = 2; // Trạng thái 2: Viện xác nhận / Hoàn tất
                    xn.NgayXacNhan = request.NgayHien;
                    xn.NhomMauXacNhan = request.NhomMau;
                    xn.RhXacNhan = request.HeRh;
                    xn.LuongMau = request.LuongMau;

                    // Ghi nhận trực tiếp dữ liệu đo đạc thực tế vào CSDL
                    xn.ChieuCao = request.ChieuCao;
                    xn.CanNang = request.CanNang;
                    xn.HuyetApTamThu = request.HuyetApTamThu;
                    xn.HuyetApTamTruong = request.HuyetApTamTruong;

                    await _context.SaveChangesAsync();
                    await transaction.CommitAsync(); // Chốt lưu toàn vẹn tất cả các bảng dữ liệu liên quan

                    // Thông báo tức thì cho ca KHẨN CẤP qua Firebase
                    if (xn.YeuCau?.LoaiTin == 0 && xn.NguoiHien?.TokenFCM != null)
                    {
                        string title = "❤️ Cảm ơn bạn rất nhiều!";
                        string body = $"Bạn đã hỗ trợ thành công ca cấp cứu tại {xn.YeuCau?.TenBenhVien}. Chúc bạn thật nhiều sức khỏe!";
                        await FcmHelper.SendNotification(new List<string> { xn.NguoiHien.TokenFCM }, title, body, "DonationSuccess", xn.YeuCauID ?? 0);
                    }

                    return Ok(new { Message = "Đã chốt kết quả lâm sàng và cập nhật hồ sơ người hiến thành công!" });
                }
                catch (Exception ex)
                {
                    await transaction.RollbackAsync(); // Hủy bỏ toàn bộ tiến trình nếu có lỗi bất ngờ
                    return BadRequest("Lỗi hệ thống khi lưu kết quả: " + ex.Message);
                }
            }
        }

        [HttpGet("GetDanhSachNguoiHien/{yeuCauId}")]
        public async Task<IActionResult> GetDanhSachNguoiHien(int yeuCauId)
        {
            // 1. Kiểm tra thông tin bài đăng gốc xem thuộc phân hệ nào
            var yeuCauGoc = await _context.YeuCauMaus.FindAsync(yeuCauId);
            if (yeuCauGoc == null) return NotFound("Bài đăng không tồn tại.");

            // 2. NẾU LÀ TIN LIÊN VIỆN B2B (LoaiTin == 2): Đọc dữ liệu từ bảng XacNhanDoiTac
            if (yeuCauGoc.LoaiTin == 2)
            {
                var doiTacList = await _context.XacNhanDoiTacs
                    .Include(x => x.BenhVienChiaSe)
                    .Include(x => x.YeuCau)
                    .Where(x => x.YeuCauID == yeuCauId)
                    .Select(x => new
                    {
                        ID = x.ID,               
                        YeuCauID = x.YeuCauID,   
                        TrangThaiConfirm = 1,    
                        YeuCau = x.YeuCau,      
                        NguoiHien = new
                        {
                            ID = x.BenhVienChiaSeID,
                            HoTen = x.BenhVienChiaSe.HoTen,
                            SDT = x.BenhVienChiaSe.SDT,
                            LoaiTaiKhoan = x.BenhVienChiaSe.LoaiTaiKhoan
                        }
                    })
                    .ToListAsync();

                return Ok(doiTacList);
            }


            var tinhNguyenVienList = await _context.XacNhanHienMaus
                .Include(x => x.NguoiHien)
                .ThenInclude(u => u.CaNhan)
                .Include(x => x.YeuCau)
                .Where(x => x.YeuCauID == yeuCauId)
                .Select(x => new
                {
                    id = x.ID,
                    yeuCauID = x.YeuCauID,
                    trangThaiConfirm = x.TrangThaiConfirm,
                    yeuCau = x.YeuCau,
                    nguoiHien = new
                    {
                        id = x.NguoiHienID,
                        hoTen = x.NguoiHien.HoTen,
                        sdt = x.NguoiHien.SDT,
                        loaiTaiKhoan = x.NguoiHien.LoaiTaiKhoan,
                        caNhan = x.NguoiHien.CaNhan
                    },
                    
                    chieuCao = x.ChieuCao,
                    canNang = x.CanNang,
                    huyetApTamThu = x.HuyetApTamThu,
                    huyetApTamTruong = x.HuyetApTamTruong,
                    luongMau = x.LuongMau,
                    ngayXacNhan = x.NgayXacNhan
                })
                .ToListAsync();

            return Ok(tinhNguyenVienList);
        }

        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteRegistration(int id, [FromQuery] int? loaiTin)
        {
            try
            {

                if (loaiTin.HasValue && loaiTin.Value == 2)
                {
                    var giaoDichLienVien = await _context.XacNhanDoiTacs
                        .Include(x => x.YeuCau)
                        .FirstOrDefaultAsync(x => x.ID == id);

                    if (giaoDichLienVien == null)
                        return NotFound(new { message = "Không tìm thấy hồ sơ xác nhận điều phối liên viện này trên hệ thống." });

                    if (giaoDichLienVien.TrangThai == 1)
                        return BadRequest(new { message = "Giao dịch san sẻ kho máu này đã hoàn tất giao nhận vật lý, không thể hủy bỏ hồ sơ!" });

                    int? currentYeuCauId = giaoDichLienVien.YeuCauID;
           
                    _context.XacNhanDoiTacs.Remove(giaoDichLienVien);
                    await _context.SaveChangesAsync();

                    if (currentYeuCauId.HasValue)
                    {
                        var yeuCau = await _context.YeuCauMaus.FindAsync(currentYeuCauId);

                       
                        if (yeuCau != null && yeuCau.TrangThai != 0)
                        {
                            yeuCau.TrangThai = 0;
                            await _context.SaveChangesAsync();
                        }
                    }

                    return Ok(new { Message = "Đã hủy bỏ liên kết điều phối với cơ sở y tế đối tác và cập nhật trạng thái kho tin thành công." });
                }

               
                var xacNhan = await _context.XacNhanHienMaus
                    .Include(x => x.YeuCau)
                    .FirstOrDefaultAsync(x => x.ID == id);

                if (xacNhan == null)
                    return NotFound("Không tìm thấy bản ghi đăng ký của tình nguyện viên.");

                if (xacNhan.TrangThaiConfirm >= 2)
                    return BadRequest("Không thể xóa yêu cầu của tình nguyện viên đã hoàn thành hiến máu.");

                int? currentYeuCauIdPersonal = xacNhan.YeuCauID;

                _context.XacNhanHienMaus.Remove(xacNhan);
                await _context.SaveChangesAsync();

                if (currentYeuCauIdPersonal.HasValue)
                {
                    var yeuCau = await _context.YeuCauMaus.FindAsync(currentYeuCauIdPersonal);

                    if (yeuCau != null && yeuCau.SoNguoiCan.HasValue)
                    {
                        int remainingConfirmed = await _context.XacNhanHienMaus
                            .CountAsync(x => x.YeuCauID == currentYeuCauIdPersonal && x.TrangThaiConfirm >= 1);

                        if (remainingConfirmed < yeuCau.SoNguoiCan.Value)
                        {
                            yeuCau.TrangThai = 0;
                            await _context.SaveChangesAsync();
                        }
                    }
                }

                return Ok(new { Message = "Đã xóa tình nguyện viên khỏi danh sách tiếp nhận." });
            }
            catch (Exception ex)
            {
                return BadRequest("Lỗi hệ thống khi thực thi lệnh gỡ bỏ: " + ex.Message);
            }
        }

        [HttpPost("NguoiHienHuyDen/{yeuCauId}/{userId}")]
        public async Task<IActionResult> NguoiHienHuyDen(int yeuCauId, int userId)
        {
            try
            {
                
                var xn = await _context.XacNhanHienMaus
                    .Include(x => x.NguoiHien)
                    .Include(x => x.YeuCau)
                        .ThenInclude(y => y.NguoiDang)
                    .FirstOrDefaultAsync(x => x.YeuCauID == yeuCauId &&
                                             (x.NguoiHienID == userId || x.YeuCau.NguoiDangID == userId));

                if (xn == null)
                    return NotFound("Không tìm thấy thông tin đăng ký hoặc bạn không có quyền xử lý ca hiến này.");

               
                if (xn.TrangThaiConfirm >= 2)
                    return BadRequest("Ca hiến máu đã hoàn thành và chốt kết quả lâm sàng, không thể hủy hẹn.");

                string tenNguoiHien = xn.NguoiHien?.HoTen ?? "Tình nguyện viên";
                string tenBv = xn.YeuCau?.TenBenhVien ?? "Bệnh viện";
                string tokenNguoiDang = xn.YeuCau?.NguoiDang?.TokenFCM;
                string tokenNguoiHiển = xn.NguoiHien?.TokenFCM;
                int? currentYeuCauId = xn.YeuCauID;
                int trangThaiTruocKhiHuy = xn.TrangThaiConfirm;

               
                _context.XacNhanHienMaus.Remove(xn);
                
                await _context.SaveChangesAsync();

               
                if (currentYeuCauId.HasValue)
                {
                    var yeuCau = await _context.YeuCauMaus.FindAsync(currentYeuCauId);

                    if (yeuCau != null && yeuCau.SoNguoiCan.HasValue)
                    {
                       
                        int remainingConfirmed = await _context.XacNhanHienMaus
                            .CountAsync(x => x.YeuCauID == currentYeuCauId && x.TrangThaiConfirm >= 1);
         
                        if (remainingConfirmed < yeuCau.SoNguoiCan.Value)
                        {
                            yeuCau.TrangThai = 0;
                            await _context.SaveChangesAsync();
                        }
                    }
                }
                
                bool laNguoiHienBamHuy = (xn.NguoiHienID == userId);

                if (laNguoiHienBamHuy)
                {
                    if (trangThaiTruocKhiHuy == 1 && !string.IsNullOrEmpty(tokenNguoiDang))
                    {
                        string title = "⚠️ Tình nguyện viên hủy ca cấp cứu";
                        string body = $"Tình nguyện viên {tenNguoiHien} đã hủy hẹn di chuyển đến hiến máu tại {tenBv}. Tin cấp cứu đã mở lại để nhận hỗ trợ mới.";

                        await FcmHelper.SendNotification(new List<string> { tokenNguoiDang }, title, body, "DonorCancelled", yeuCauId);
                    }
                }
                else
                {            
                    if (!string.IsNullOrEmpty(tokenNguoiHiển))
                    {
                        string title = "📢 Lịch hẹn cấp cứu đã bị hủy";
                        string body = $"Ca hỗ trợ của bạn tại {tenBv} đã được phía tiếp nhận hủy. Cảm ơn tấm lòng vàng của bạn!";

                        await FcmHelper.SendNotification(new List<string> { tokenNguoiHiển }, title, body, "OwnerCancelledDonor", yeuCauId);
                    }
                }

                return Ok(new { Message = "Hủy hẹn hiến máu thành công!" });
            }
            catch (Exception ex)
            {
                return BadRequest("Lỗi hệ thống khi hủy hẹn: " + ex.Message);
            }
        }

        [HttpPost("KetThucSuKien/{yeuCauId}")]
        public async Task<IActionResult> KetThucSuKien(int yeuCauId)
        {
            using (var transaction = await _context.Database.BeginTransactionAsync())
            {
                try
                {
                    var yeuCau = await _context.YeuCauMaus.FindAsync(yeuCauId);
                    if (yeuCau == null) return NotFound("Không tìm thấy sự kiện.");

                    // 1. Chuyển trạng thái bài đăng sự kiện gốc sang Đã đóng (Mã số 2)
                    // Bước này kích hoạt mở khóa điều kiện .Where để hiển thị lịch sử cho những người hiến THÀNH CÔNG
                    yeuCau.TrangThai = 2;

                    // 2. Lấy danh sách những người THỰC SỰ HIẾN THÀNH CÔNG (TrangThaiConfirm == 2) để duyệt hồ sơ tĩnh
                    var dsThanhCong = await _context.XacNhanHienMaus
                        .Include(x => x.NguoiHien).ThenInclude(u => u.CaNhan)
                        .Where(x => x.YeuCauID == yeuCauId && x.TrangThaiConfirm == 2)
                        .ToListAsync();

                    foreach (var item in dsThanhCong)
                    {
                        if (item.NguoiHien?.CaNhan != null)
                        {
                            // Đồng bộ kết quả lâm sàng vào hồ sơ sức khỏe nền
                            item.NguoiHien.CaNhan.NhomMau = item.NhomMauXacNhan;
                            item.NguoiHien.CaNhan.HeRh = item.RhXacNhan;
                            item.NguoiHien.CaNhan.NgayHienGanNhat = item.NgayXacNhan;
                            item.NguoiHien.CaNhan.ChieuCao = item.ChieuCao;
                            item.NguoiHien.CaNhan.CanNang = item.CanNang;

                            // Chỉ cộng dồn số lần hiến chính thức cho người hiến thành công
                            item.NguoiHien.CaNhan.SoLanHien += 1;
                        }

                        // Bắn thông báo Firebase tri ân xuất bản chứng nhận số đến điện thoại tình nguyện viên
                        if (!string.IsNullOrEmpty(item.NguoiHien?.TokenFCM))
                        {
                            string title = "❤️ Cảm ơn tấm lòng vàng của bạn!";
                            string body = $"Sự kiện tại {yeuCau.TenBenhVien} đã kết thúc. Mốc lịch sử hiến thành công {item.LuongMau}ml đã được ghi nhận vào hồ sơ của bạn.";
                            await FcmHelper.SendNotification(new List<string> { item.NguoiHien.TokenFCM }, title, body, "EventSuccess", yeuCauId);
                        }
                    }
        
                    var dsCaKhongHienThucTe = await _context.XacNhanHienMaus
                        .Where(x => x.YeuCauID == yeuCauId && (x.TrangThaiConfirm == 4 || x.TrangThaiConfirm < 2))
                        .ToListAsync();

                    if (dsCaKhongHienThucTe.Any())
                    {
                       
                        _context.XacNhanHienMaus.RemoveRange(dsCaKhongHienThucTe);
                    }
                    
                    await _context.SaveChangesAsync();
                    await transaction.CommitAsync();

                    return Ok(new
                    {
                        Message = "Sự kiện kết thúc thành công, hệ thống đã quét sạch dữ liệu ca không hiến thực tế!",
                        SoLuongThanhCong = dsThanhCong.Count,
                        SoCaDaQuetXoa = dsCaKhongHienThucTe.Count
                    });
                }
                catch (Exception ex)
                {
                    await transaction.RollbackAsync();
                    return BadRequest("Lỗi khi kết thúc sự kiện: " + ex.Message);
                }
            }
        }

        [HttpPost("DanhDauKhongHien/{xacNhanId}")]
        public async Task<IActionResult> DanhdauKhongHien(int xacNhanId)
        {
            var xn = await _context.XacNhanHienMaus.FindAsync(xacNhanId);
            if (xn == null) return NotFound("Không tìm thấy bản ghi.");

            if (xn.TrangThaiConfirm == 3)
                return BadRequest("Sự kiện này đã đóng sổ hoàn toàn, không thể chỉnh sửa.");

            xn.TrangThaiConfirm = 4; // Không tham gia / Hủy hiến thực tế
            await _context.SaveChangesAsync();

            return Ok(new { Message = "Đã đánh dấu tình nguyện viên không tham gia hiến máu thực tế." });
        }

        [HttpGet("GetLichSuHien/{nguoiHienId}")]
        public async Task<IActionResult> GetLichSuHien(int nguoiHienId)
        {
            // ĐÃ SỬA CƠ CHẾ ĐỂ NGƯỜI HIẾN THẤY THẺ KHI ĐANG ĐI (TrangThaiConfirm == 1)
            var list = await _context.XacNhanHienMaus
                .Include(x => x.YeuCau)
                    .ThenInclude(y => y.ChiTiets)
                .Where(x => x.NguoiHienID == nguoiHienId &&
                            (        
                                (x.TrangThaiConfirm == 2 && x.YeuCau != null && x.YeuCau.TrangThai == 2)
                                ||
                                (x.TrangThaiConfirm == 1 && x.YeuCau != null && x.YeuCau.TrangThai == 0)
                                ||
                                (x.TrangThaiConfirm == 0 && x.YeuCau != null && x.YeuCau.TrangThai == 0)
                            ))
                .OrderByDescending(x => x.NgayXacNhan)
                .ToListAsync();

            return Ok(list);
        }
    }
}
