# PetCare Hub
GĐ4: đặt lịch dịch vụ và lịch làm việc — xem [quy tắc, tài khoản demo và các bước thử tay](docs/STAGE4.md).
GĐ3B: giao diện giỏ hàng, đặt hàng và quản lý đơn đã có; xem [hướng dẫn và kịch bản thử tay](docs/STAGE3B.md).
GĐ1: Java 21, Maven, Spring Boot 3.5.16, SQL Server.

## Chạy
Tạo database bằng SQL Server Management Studio:
```sql
CREATE DATABASE petcare_hub;
GO
```
Đảm bảo SQL Server bật TCP/IP và tài khoản có quyền tạo/cập nhật bảng.
Trong PowerShell:
```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=petcare_hub;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME='sa'
$env:DB_PASSWORD='<mật khẩu SQL Server>'
mvn spring-boot:run
```
Mở http://localhost:8080. Bootstrap tải qua CDN, cần Internet cho định dạng.
Cấu hình tin cậy chứng chỉ mặc định dành cho phát triển; production dùng chứng chỉ hợp lệ và trustServerCertificate=false.

## Kiểm thử
```powershell
mvn test
```
Test tự bật profile test, dùng H2, không cần SQL Server.

## Tài khoản mẫu
Chỉ tạo khi bảng users rỗng:
- admin@petcare.vn / Admin@123
- staff@petcare.vn / Staff@123
- customer@petcare.vn / Customer@123

Đổi mật khẩu mẫu trước khi đưa lên môi trường thực tế. Đăng ký công khai chỉ tạo CUSTOMER.
ADMIN thấy menu quản trị và nhân viên; STAFF thấy menu nhân viên; CUSTOMER thấy nhãn khách hàng thân thiết.
Đăng xuất bằng POST có CSRF. Khách chưa đăng nhập được chuyển tới đăng nhập; tài khoản thiếu quyền nhận trang 403 tiếng Việt.
Email được chuẩn hóa chữ thường. Điện thoại tùy chọn. Mật khẩu đăng ký dùng 8–72 ký tự ASCII để phù hợp giới hạn BCrypt.
Các pattern nghiệp vụ sẽ triển khai ở giai đoạn tương ứng; chưa có nghiệp vụ sản phẩm/đơn hàng trong GĐ1.

