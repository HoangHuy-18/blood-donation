using Microsoft.AspNetCore.Mvc;
using QuanLyHienMauApi.DataContext;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.Models;
using QuanLyHienMauApi.Helpers;

namespace QuanLyHienMauApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class YeuCauMauController : ControllerBase
    {
        private readonly ApplicationDbContext _context;

        public YeuCauMauController(ApplicationDbContext context)
        {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> GetActiveRequests(int? loaiTin)
        {
            var query = _context.YeuCauMaus
                .Include(y => y.ChiTiets)
                .Include(y => y.NguoiDang)
                .Include(y => y.BenhVien)
                    .ThenInclude(b => b.ThongTinTaiKhoan)
                .Where(y => y.TrangThai == 0);

            if (loaiTin.HasValue)
            {
                query = query.Where(y => y.LoaiTin == loaiTin.Value);
            }

            var list = await query
                .OrderByDescending(y => y.NgayDang)
                .ToListAsync();

            foreach (var item in list)
            {
                item.SoNguoiDaXacNhan = await _context.XacNhanHienMaus
                    .CountAsync(x => x.YeuCauID == item.ID && x.TrangThaiConfirm >= 1);
            }

            return Ok(list);
        }

        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var item = await _context.YeuCauMaus
                .Include(y => y.ChiTiets)
                .Include(y => y.NguoiDang)
                .Include(y => y.BenhVien)
                    .ThenInclude(b => b.ThongTinTaiKhoan)
                .FirstOrDefaultAsync(y => y.ID == id);

            if (item == null) return NotFound();
            return Ok(item);
        }

        [HttpPost]
        public async Task<IActionResult> PostYeuCau([FromBody] YeuCauMau yeuCau)
        {
            try
            {
                if ((yeuCau.LoaiTin == 0 || yeuCau.LoaiTin == 2) && (yeuCau.ChiTiets == null || yeuCau.ChiTiets.Count == 0))
                    return BadRequest("Yêu cầu điều phối hoặc khẩn cấp bắt buộc phải cấu hình ít nhất một nhóm máu.");

                bool coLichTrung = false;
                if (yeuCau.LoaiTin == 1)
                {
                    coLichTrung = await _context.YeuCauMaus
                         .AnyAsync(y => y.NguoiDangID == yeuCau.NguoiDangID &&
                                        y.LoaiTin == 1 && // Phải trùng với tin loại Sự kiện
                                        y.TrangThai == 0 &&
                                        y.NgayBatDau == yeuCau.NgayBatDau &&
                                        y.NgayKetThuc == yeuCau.NgayKetThuc &&
                                        y.GioBatDau == yeuCau.GioBatDau &&
                                        y.GioKetThuc == yeuCau.GioKetThuc);
                }

                if (coLichTrung)
                {
                    return BadRequest("Bạn đã có một ngày hội sự kiện được cấu hình trùng hoàn toàn khung ngày và giờ này trên hệ thống!");
                }

                yeuCau.NgayDang = DateTime.Now;
                yeuCau.TrangThai = 0;

                _context.YeuCauMaus.Add(yeuCau);
                await _context.SaveChangesAsync();

                var potentialDonors = await _context.NguoiDungs
                    .Include(u => u.CaNhan)
                    .Where(u => u.LoaiTaiKhoan == 0 && !string.IsNullOrEmpty(u.TokenFCM))
                    .ToListAsync();

                var nguoiDang = await _context.NguoiDungs.FindAsync(yeuCau.NguoiDangID);
                int loaiTK = nguoiDang?.LoaiTaiKhoan ?? 0;
                string tenBV = yeuCau.TenBenhVien ?? "Cơ sở y tế";

                string title = "";
                string body = "";
                string type = "new_request";

                if (yeuCau.LoaiTin == 1)
                {
                    title = "📅 Sự kiện hiến máu mới";
                    body = $"Bệnh viện {tenBV} tổ chức ngày hội hiến máu. Tham gia ngay!";

                    var tokens = potentialDonors.Select(d => d.TokenFCM).ToList();
                    if (tokens.Any()) await FcmHelper.SendNotification(tokens, title, body, type, yeuCau.ID);
                }
                else if (yeuCau.LoaiTin == 2)
                {
                    title = "🏥 CỨU VIỆN: Cơ sở khan hiếm nguồn máu!";
                    var nhomMauYeuCau = string.Join(", ", yeuCau.ChiTiets.Select(c => c.NhomMau + c.Rh));
                    body = $"Cơ sở y tế {tenBV} đang gặp tình trạng cạn kiệt nhóm máu {nhomMauYeuCau} trong kho. Vui lòng hỗ trợ điều phối!";

                    type = "b2b_request";

                    var partnerHospitals = await _context.NguoiDungs
                        .Where(u => u.LoaiTaiKhoan == 1 && u.ID != yeuCau.NguoiDangID && !string.IsNullOrEmpty(u.TokenFCM))
                        .Select(u => u.TokenFCM)
                        .ToListAsync();

                    if (partnerHospitals.Any())
                        await FcmHelper.SendNotification(partnerHospitals, title, body, type, yeuCau.ID);
                }
                else
                {
                    var bloodTypeGroups = new Dictionary<string, List<string>>();
                    double maxDistance = 20.0;

                    foreach (var donor in potentialDonors)
                    {
                        if (donor.CaNhan == null || string.IsNullOrEmpty(donor.CaNhan.NhomMau)) continue;

                        if (yeuCau.ViDo.HasValue && yeuCau.KinhDo.HasValue && donor.ViDo.HasValue && donor.KinhDo.HasValue)
                        {
                            double distance = FcmHelper.CalculateDistance(
                                yeuCau.ViDo.Value, yeuCau.KinhDo.Value,
                                donor.ViDo.Value, donor.KinhDo.Value
                            );

                            if (distance > maxDistance) continue;
                        }
                        else
                        {
                            continue;
                        }

                        bool canDonate = false;
                        foreach (var chiTiet in yeuCau.ChiTiets)
                        {
                            if (FcmHelper.CheckBloodCompatibility(
                                donor.CaNhan.NhomMau, donor.CaNhan.HeRh,
                                chiTiet.NhomMau, chiTiet.Rh))
                            {
                                canDonate = true;
                                break;
                            }
                        }

                        if (canDonate)
                        {
                            if (!bloodTypeGroups.ContainsKey(donor.CaNhan.NhomMau))
                                bloodTypeGroups[donor.CaNhan.NhomMau] = new List<string>();

                            bloodTypeGroups[donor.CaNhan.NhomMau].Add(donor.TokenFCM);
                        }
                    }

                    if (bloodTypeGroups.Any())
                    {
                        var nhomMauYeuCau = string.Join(", ", yeuCau.ChiTiets.Select(c => c.NhomMau + c.Rh));
                        if (loaiTK == 1)
                        {
                            title = "🏥 Bệnh viện cần máu gấp!";
                            body = $"Bệnh viện {tenBV} đang cần máu {nhomMauYeuCau} khẩn cấp cho bệnh nhân.";
                        }
                        else
                        {
                            title = "🆘 Cần giúp đỡ khẩn cấp!";
                            body = $"Có bệnh nhân tại {tenBV} đang cần máu {nhomMauYeuCau} gấp. Bạn chính là hy vọng của họ!";
                        }
                        foreach (var entry in bloodTypeGroups)
                        {
                            await FcmHelper.SendNotification(entry.Value, title, body, type, yeuCau.ID);
                        }
                    }
                }

                yeuCau.SoNguoiDaXacNhan = await _context.XacNhanHienMaus
                    .CountAsync(x => x.YeuCauID == yeuCau.ID && x.TrangThaiConfirm >= 1);

                return Ok(yeuCau);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Lỗi gửi thông báo: " + ex.Message);
                return BadRequest("Lỗi: " + ex.Message);
            }
        }

        [HttpGet("GetRequests")]
        public async Task<IActionResult> GetRequests()
        {
            var requests = await _context.YeuCauMaus
                .Include(y => y.NguoiDang)
                .Include(y => y.ChiTiets)
                .Where(y => y.LoaiTin == 2 && y.TrangThai == 0)
                .OrderByDescending(y => y.NgayDang)
                .ToListAsync();

            return Ok(requests);
        }

        [HttpPost("XacNhanChiaSeMau")]
        public async Task<IActionResult> XacNhanChiaSeMau(int yeuCauId, int hospitalId)
        {
            var yeuCau = await _context.YeuCauMaus
                .Include(y => y.NguoiDang)
                .FirstOrDefaultAsync(y => y.ID == yeuCauId);

            if (yeuCau == null)
                return NotFound("Bài đăng kêu gọi nguồn máu này không tồn tại hoặc đã bị ẩn!");

            var vienChiaSe = await _context.NguoiDungs.FindAsync(hospitalId);
            if (vienChiaSe == null)
                return NotFound("Không tìm thấy thông tin cơ sở y tế thực hiện lệnh hỗ trợ!");

            bool daXacNhanRoi = await _context.XacNhanDoiTacs
                     .AnyAsync(x => x.YeuCauID == yeuCauId && x.BenhVienChiaSeID == hospitalId);

            if (daXacNhanRoi)
            {
                return BadRequest(new { message = "Cơ sở y tế của bạn đã xác nhận chia sẻ nguồn máu cho bài đăng này trước đó rồi!" });
            }

            var giaoDich = new XacNhanDoiTac
            {
                YeuCauID = yeuCauId,
                BenhVienChiaSeID = hospitalId,
                TrangThai = 0
            };

            _context.XacNhanDoiTacs.Add(giaoDich);
            await _context.SaveChangesAsync();

            string tokenFcmVienA = yeuCau.NguoiDang?.TokenFCM;
            string tenVienB = vienChiaSe.HoTen;

            if (!string.IsNullOrEmpty(tokenFcmVienA))
            {
                _ = Task.Run(() => {
                    try
                    {
                        FcmHelper.SendNotification(
                            new List<string> { tokenFcmVienA },
                            "🏥 Có cơ sở y tế sẵn sàng chia sẻ máu!",
                            $"{tenVienB} đã xác nhận sẽ chia sẻ nguồn máu của bạn.",
                            "PartnerSupport",
                            yeuCauId
                        );
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine("Lỗi bắn thông báo FCM liên viện: " + ex.Message);
                    }
                });
            }

            return Ok(new { message = "Xác nhận điều phối san sẻ nguồn máu liên viện thành công!" });
        }

        [HttpPut("{id}")]
        public async Task<IActionResult> PutYeuCau(int id, [FromBody] YeuCauMau updateData)
        {
            if (id != updateData.ID) return BadRequest("ID không khớp.");

            var existingYeuCau = await _context.YeuCauMaus
                .Include(y => y.ChiTiets)
                .FirstOrDefaultAsync(y => y.ID == id);

            if (existingYeuCau == null) return NotFound("Không tìm thấy bài đăng.");

            if (existingYeuCau.TrangThai != 0)
                return BadRequest("Không thể sửa bài đăng đã hoàn thành hoặc đã đóng.");

            try
            {
                
                bool coLichTrung = false;
                if (updateData.LoaiTin == 1)
                {
                    coLichTrung = await _context.YeuCauMaus
                        .AnyAsync(y => y.ID != id &&
                                       y.NguoiDangID == existingYeuCau.NguoiDangID &&
                                       y.LoaiTin == 1 &&
                                       y.TrangThai == 0 &&
                                       y.NgayBatDau == updateData.NgayBatDau &&
                                       y.NgayKetThuc == updateData.NgayKetThuc &&
                                       y.GioBatDau == updateData.GioBatDau &&
                                       y.GioKetThuc == updateData.GioKetThuc);
                }

                if (coLichTrung)
                {
                    return BadRequest("Khung ngày và giờ điều chỉnh bị trùng với một ngày hội sự kiện đang hoạt động khác của bạn!");
                }

                existingYeuCau.NoiDung = updateData.NoiDung;
                existingYeuCau.SoNguoiCan = updateData.SoNguoiCan;
                existingYeuCau.TenBenhVien = updateData.TenBenhVien;
                existingYeuCau.DiaChiBenhVien = updateData.DiaChiBenhVien;
                existingYeuCau.ViDo = updateData.ViDo;
                existingYeuCau.KinhDo = updateData.KinhDo;
                existingYeuCau.NgayBatDau = updateData.NgayBatDau;
                existingYeuCau.NgayKetThuc = updateData.NgayKetThuc;
                existingYeuCau.GioBatDau = updateData.GioBatDau;
                existingYeuCau.GioKetThuc = updateData.GioKetThuc;
                existingYeuCau.LoaiTin = updateData.LoaiTin;

                _context.YeuCauMauChiTiets.RemoveRange(existingYeuCau.ChiTiets);

                if (updateData.ChiTiets != null && updateData.ChiTiets.Count > 0)
                {
                    foreach (var detail in updateData.ChiTiets)
                    {
                        existingYeuCau.ChiTiets.Add(new YeuCauMauChiTiet
                        {
                            YeuCauID = id,
                            NhomMau = detail.NhomMau,
                            Rh = detail.Rh,
                            SoDonVi = detail.SoDonVi
                        });
                    }
                }
                else if (updateData.LoaiTin == 0)
                {
                    return BadRequest("Tin khẩn cấp bắt buộc phải có ít nhất một nhóm máu.");
                }

                await _context.SaveChangesAsync();
                existingYeuCau.SoNguoiDaXacNhan = await _context.XacNhanHienMaus
                    .CountAsync(x => x.YeuCauID == existingYeuCau.ID && x.TrangThaiConfirm >= 1);

                return Ok(existingYeuCau);
            }
            catch (Exception ex)
            {
                return BadRequest("Lỗi khi cập nhật: " + ex.Message);
            }
        }

        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteYeuCauMau(int id)
        {
            var yeuCau = await _context.YeuCauMaus
                .Include(y => y.ChiTiets)
                .FirstOrDefaultAsync(y => y.ID == id);

            if (yeuCau == null) return NotFound("Không tìm thấy bài đăng yêu cầu hiến máu.");

            if (yeuCau.LoaiTin == 0)
            {
                bool coNguoiDangDen = await _context.XacNhanHienMaus.AnyAsync(x => x.YeuCauID == id && x.TrangThaiConfirm == 1);
                if (coNguoiDangDen) return BadRequest("Bài đăng đang có tình nguyện viên chốt lịch hẹn di chuyển đến hỗ trợ khẩn cấp, không được phép xóa!");
            }
            else if (yeuCau.LoaiTin == 1)
            {
                bool coNguoiDangKySuKien = await _context.XacNhanHienMaus.AnyAsync(x => x.YeuCauID == id);
                if (coNguoiDangKySuKien) return BadRequest("Sự kiện hiến máu này đã phát sinh danh sách đăng ký. Tổ chức chỉ được phép sử dụng tính năng 'Đóng sự kiện'!");
            }
            else if (yeuCau.LoaiTin == 2)
            {
                bool coDoiTacXacNhan = await _context.XacNhanDoiTacs.AnyAsync(x => x.YeuCauID == id);
                if (coDoiTacXacNhan) return BadRequest("Yêu cầu điều phối liên viện này đã được một cơ sở y tế đối tác xác nhận san sẻ nguồn cung. Bạn không được phép xóa bài viết này!");
            }

            var registrationsUnfinished = _context.XacNhanHienMaus.Where(x => x.YeuCauID == id && x.TrangThaiConfirm < 2);
            _context.XacNhanHienMaus.RemoveRange(registrationsUnfinished);

            var registrationsFinished = await _context.XacNhanHienMaus.Where(x => x.YeuCauID == id && x.TrangThaiConfirm == 2).ToListAsync();
            foreach (var xn in registrationsFinished) { xn.YeuCauID = null; }

            var lienKetDoiTac = _context.XacNhanDoiTacs.Where(x => x.YeuCauID == id);
            if (lienKetDoiTac.Any())
            {
                _context.XacNhanDoiTacs.RemoveRange(lienKetDoiTac);
            }

            _context.YeuCauMauChiTiets.RemoveRange(yeuCau.ChiTiets);
            _context.YeuCauMaus.Remove(yeuCau);

            await _context.SaveChangesAsync();

            return Ok(new { message = "Đã dọn dẹp hệ thống và xóa bài đăng kêu gọi thành công!" });
        }
    }
}
