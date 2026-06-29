using Microsoft.AspNetCore.Mvc;
using QuanLyHienMauApi.DataContext;
using QuanLyHienMauApi.Dtos;
using QuanLyHienMauApi.Models;
using Microsoft.EntityFrameworkCore;

namespace QuanLyHienMauApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class BaoCaoController : ControllerBase
    {
        private readonly ApplicationDbContext _context;

        public BaoCaoController(ApplicationDbContext context)
        {
            _context = context;
        }

        [HttpPost("GuiBaoCao")]
        public async Task<IActionResult> GuiBaoCao([FromBody] CreateBaoCaoDto dto)
        {
            
            var daBaoCao = await _context.BaoCaoYeuCaus
                .AnyAsync(b => b.YeuCauID == dto.YeuCauId && b.NguoiBaoCaoID == dto.NguoiBaoCaoId && b.TrangThai == 0);

            if (daBaoCao)
                return BadRequest("Bạn đã gửi báo cáo cho bài viết này rồi, hệ thống đang xem xét.");

            
            var baoCao = new BaoCaoYeuCau
            {
                YeuCauID = dto.YeuCauId,
                NguoiBaoCaoID = dto.NguoiBaoCaoId,
                LyDo = dto.LyDo,
                ChiTiet = dto.ChiTiet,
                TrangThai = 0
            };

            _context.BaoCaoYeuCaus.Add(baoCao);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Gửi báo cáo thành công! Cảm ơn bạn đã hỗ trợ cộng đồng." });
        }

        [HttpPost("XuLyBaoCao")]
        public async Task<IActionResult> XuLyBaoCao(int baoCaoId, int status)
        {
            var baoCao = await _context.BaoCaoYeuCaus
                .Include(b => b.YeuCau)
                .FirstOrDefaultAsync(b => b.ID == baoCaoId);

            if (baoCao == null) return NotFound("Không tìm thấy dữ liệu báo cáo.");

            if (status == 1) 
            {
                baoCao.TrangThai = 1;

                if (baoCao.YeuCau != null)
                {                
                    baoCao.YeuCau.TrangThai = 3;
                }

                await _context.SaveChangesAsync();
                return Ok(new { message = "Đã xác nhận tin giả và ẩn bài viết thành công!" });
            }
            else if (status == 2) 
            {
                baoCao.TrangThai = 2; 
                await _context.SaveChangesAsync();
                return Ok(new { message = "Đã hủy bỏ báo cáo." });
            }

            return BadRequest("Trạng thái xử lý không hợp lệ.");
        }
    }
}
