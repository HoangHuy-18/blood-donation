namespace QuanLyHienMauApi.Dtos
{
    public class CreateBaoCaoDto
    {
        public int YeuCauId { get; set; }
        public int NguoiBaoCaoId { get; set; }
        public string LyDo { get; set; }
        public string? ChiTiet { get; set; }
    }
}
