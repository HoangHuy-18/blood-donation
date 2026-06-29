namespace QuanLyHienMauApi.Dtos
{
    public class KetQuaHienMau
    {
        public int XacNhanId { get; set; }
        public DateTime NgayHien { get; set; }
        public string NhomMau { get; set; }
        public string HeRh { get; set; }
        public int LuongMau { get; set; }

        public int ChieuCao { get; set; }
        public double CanNang { get; set; }
        public int HuyetApTamThu { get; set; }
        public int HuyetApTamTruong { get; set; }
    }
}
