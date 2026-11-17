# PetCare Hub

## Khi nâng cấp từ bản cũ

Hibernate đã bật `hibernate.use_nationalized_character_data=true` để tạo cột String bằng `nvarchar` trên SQL Server. `@Column(length)` vẫn giữ giới hạn độ dài. H2 trong test giữ nguyên cấu hình kết nối.

Database `petcare_hub` đã tạo bằng bản cũ cần được xử lý thủ công: **sao lưu trước**, rồi xóa và tạo lại database nếu chấp nhận mất dữ liệu demo; hoặc đổi các cột chuỗi hiện có từ `varchar` sang `nvarchar` để giữ dữ liệu. Không dựa vào `ddl-auto=update` để chuyển kiểu các cột đã tồn tại. Khi đổi thủ công, giữ đúng độ dài, NULL/NOT NULL và xử lý index/constraint phụ thuộc. Ví dụ cho một cột:

```sql
ALTER TABLE dbo.users ALTER COLUMN full_name nvarchar(100) NOT NULL;
```

Đây chỉ là ví dụ, không phải migration đầy đủ. Cần rà soát tất cả bảng, kể cả bảng sản phẩm kế thừa, hồ sơ, đơn hàng và lịch hẹn. Những ký tự đã lưu thành `?` không tự phục hồi sau khi đổi kiểu: phải khôi phục từ bản sao lưu đúng hoặc nhập lại dữ liệu. SQL viết tay có literal tiếng Việt phải dùng `N'...'`; với ứng dụng ưu tiên bind tham số.

Sau khi cấu hình biến môi trường DB như bên dưới, chạy `mvn spring-boot:run '-Dspring-boot.run.profiles=demo'`. Trong SSMS kiểm tra `INFORMATION_SCHEMA.COLUMNS` cho thấy các cột văn bản là `nvarchar`; trên web lưu tên thú `Lucky lớn`, tên khách `Tuấn Đạt`, ghi chú `Đặt hàng`, rồi tải lại/khởi động lại để xác nhận còn nguyên dấu. Không có thao tác xóa database tự động trong bản sửa này.

Danh sách rà soát mapping, SQL và encoding: [UNICODE-AUDIT.md](docs/UNICODE-AUDIT.md).

GĐ5: hồ sơ sức khỏe, tiêm phòng và giả lập ngày — xem [hướng dẫn chạy demo và thử tay theo thứ tự](docs/STAGE5B.md).
GĐ6: quiz ghép đôi (hai Strategy), hồ sơ thú cưng, giải thích điểm và lịch sử — xem [công thức, cấu hình và hướng dẫn thử tay](docs/matching-algorithm.md).
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
ADMIN thấy menu quản trị và nhân viên; STAFF thấy menu nhân viên; CUSTOMER thấy liên kết Thẻ thành viên; ADMIN thấy Quản lý thành viên.
Đăng xuất bằng POST có CSRF. Khách chưa đăng nhập được chuyển tới đăng nhập; tài khoản thiếu quyền nhận trang 403 tiếng Việt.
Email được chuẩn hóa chữ thường. Điện thoại tùy chọn. Mật khẩu đăng ký dùng 8–72 ký tự ASCII để phù hợp giới hạn BCrypt.
Các pattern nghiệp vụ sẽ triển khai ở giai đoạn tương ứng; chưa có nghiệp vụ sản phẩm/đơn hàng trong GĐ1.


## GĐ7 — Thẻ thành viên

Đã bổ sung tích điểm qua Observer, giảm giá theo hạng bằng Strategy, đổi voucher và quản trị điểm. Xem [hướng dẫn chạy demo và kiểm thử thủ công GĐ7](docs/GD7-MANUAL.md), bao gồm luồng hoàn tất đơn để tăng điểm và lên hạng.

Kiểm chứng: `mvn test` — 387 test xanh trên Java 21 và Spring Boot 3.5.16.
