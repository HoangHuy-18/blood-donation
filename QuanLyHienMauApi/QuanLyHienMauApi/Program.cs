using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.EntityFrameworkCore;
using QuanLyHienMauApi.DataContext;
using System.IO;
using System.Text.Json.Serialization;

var builder = WebApplication.CreateBuilder(args);

var pathToServiceAccount = Path.Combine(builder.Environment.ContentRootPath, "firebase-service-account.json");

if (FirebaseApp.DefaultInstance == null)
{
    FirebaseApp.Create(new AppOptions()
    {
        Credential = GoogleCredential.FromFile(pathToServiceAccount)
    });
}

builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.ReferenceHandler = ReferenceHandler.IgnoreCycles;

        options.JsonSerializerOptions.PropertyNamingPolicy = null;

    });

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddAntiforgery();

builder.Services.AddRazorPages();

builder.Services.AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
    .AddCookie(options =>
    {
        options.LoginPath = "/Admin/Login"; // Đường dẫn trang đăng nhập
        options.AccessDeniedPath = "/Admin/AccessDenied";
    });

var app = builder.Build();

app.UseStaticFiles();
app.UseRouting();

app.UseAuthentication(); // Xác thực danh tính
app.UseAuthorization();  // Phân quyền

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseAuthorization();

app.UseStaticFiles();

app.MapRazorPages();
app.MapControllers();

app.Run();
