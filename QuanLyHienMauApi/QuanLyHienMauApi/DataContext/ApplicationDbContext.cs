using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.Dtos;
using QuanLyHienMauApi.Models;

namespace QuanLyHienMauApi.DataContext
{
    public class ApplicationDbContext : DbContext
    {
        public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options) : base(options)
        {
        }

        // Khai báo các bảng dữ liệu
        public DbSet<NguoiDung> NguoiDungs { get; set; }
        public DbSet<CaNhan> CaNhans { get; set; }
        public DbSet<CoSoYTe> CoSoYTes { get; set; }
        public DbSet<YeuCauMau> YeuCauMaus { get; set; }
        public DbSet<YeuCauMauChiTiet> YeuCauMauChiTiets { get; set; }
        public DbSet<XacNhanHienMau> XacNhanHienMaus { get; set; }
        public DbSet<XacNhanEmail> XacNhanEmails { get; set; }
        public DbSet<BaoCaoYeuCau> BaoCaoYeuCaus { get; set; }
        public DbSet<XacNhanDoiTac> XacNhanDoiTacs { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            // Cấu hình để EF không tự ý đổi tên bảng thành số nhiều nếu bạn không muốn
            modelBuilder.Entity<NguoiDung>().ToTable("NguoiDung");
            modelBuilder.Entity<CaNhan>().ToTable("CaNhan");
            modelBuilder.Entity<CoSoYTe>().ToTable("CoSoYTe");
            modelBuilder.Entity<YeuCauMau>().ToTable("YeuCauMau");
            modelBuilder.Entity<YeuCauMauChiTiet>().ToTable("YeuCauMauChiTiet");
            modelBuilder.Entity<XacNhanHienMau>().ToTable("XacNhanHienMau");
            modelBuilder.Entity<XacNhanEmail>().ToTable("XacNhanEmail");
            modelBuilder.Entity<BaoCaoYeuCau>().ToTable("BaoCaoYeuCau");
            modelBuilder.Entity<XacNhanDoiTac>().ToTable("XacNhanDoiTac");

            // Cấu hình Quan hệ
            modelBuilder.Entity<CaNhan>()
                .HasOne(c => c.ThongTinTaiKhoan)
                .WithOne(u => u.CaNhan)
                .HasForeignKey<CaNhan>(c => c.ID);

            
            modelBuilder.Entity<CoSoYTe>()
                .HasOne(c => c.ThongTinTaiKhoan)
                .WithOne(u => u.CoSoYTe)
                .HasForeignKey<CoSoYTe>(c => c.ID);
        }
    }
}
