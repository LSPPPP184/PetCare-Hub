# Giai đoạn 2 — Sản phẩm

## Chạy

Giữ nguyên hướng dẫn SQL Server và biến môi trường trong README của GĐ1.

```powershell
mvn test
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

- Khách: http://localhost:8080/products
- ADMIN/STAFF: http://localhost:8080/staff/products
- Profile `demo` chỉ thêm 6 thú cưng, 8 phụ kiện và 5 dịch vụ khi bảng products rỗng.
- Form chọn loại bằng nút Đổi loại trước khi nhập; khi sửa không được đổi lớp sản phẩm.
- Ẩn là xóa mềm: bản ghi vẫn tồn tại, khách không xem được kể cả biết ID.

## Giả định

- Giá là VNĐ, BigDecimal, tối đa hai chữ số thập phân. Thú cưng không cộng phụ phí ở GĐ2; grooming cộng phụ phí kích thước đã nhập; các loại còn lại dùng giá niêm yết.
- Dịch vụ cần thời lượng dương. Bác sĩ hiện là tên, chưa liên kết lịch làm việc hay tài khoản. Gói tháng cần số tháng và lượt dương, mô tả dịch vụ.
- Nhóm lọc: thú cưng, phụ kiện, dịch vụ. Mỗi trang 12 sản phẩm. Truy vấn JPA đa hình lọc tên và trạng thái hiển thị ngay tại database; khách không tải các bản ghi đã ẩn. Lọc nhóm, sắp xếp theo calculatePrice và cắt trang vẫn thực hiện trong bộ nhớ để giữ đúng đa hình. Cách này dành cho danh mục nhỏ của bài tập; chưa tối ưu phân trang cho danh mục lớn.
- Trạng thái thú cưng là dữ liệu danh mục; chưa có mua hàng/giữ chỗ. State đơn hàng thuộc GĐ3.
- Header chung có mục Danh mục cho mọi người; mục Quản lý sản phẩm chỉ dành cho STAFF/ADMIN. Trang chủ có nút Xem danh mục và ba lối tắt dùng group=PET, ACCESSORY, SERVICE. Header và trang chủ của GĐ1 đã được cập nhật theo yêu cầu bổ sung điều hướng; logic GĐ1 giữ nguyên.
- Ảnh xuất hiện ở danh sách và chi tiết khi URL HTTP/HTTPS hợp lệ. Có thể để trống; khi thiếu hoặc URL cũ không hợp lệ, trang hiển thị thông báo chưa có ảnh. Trình duyệt tải ảnh từ URL đã nhập; nếu nguồn ảnh không truy cập được, văn bản thay thế mô tả sản phẩm vẫn có sẵn.
- Lỗi nhập sai kiểu trong form sản phẩm có thông báo tiếng Việt qua messages.properties.
- H2 kiểm thử không xác nhận khả năng kết nối SQL Server thực tế.
