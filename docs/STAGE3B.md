# Giai đoạn 3B — Giao diện giỏ hàng và đơn hàng

## Phạm vi và giả định

- Dùng lại CartService/OrderService, giữ nguyên toàn bộ quy tắc chuyển trạng thái và trừ/hoàn kho của 3A.
- State bổ sung `getAllowedActions()` chỉ để mô tả các nút được hiển thị. Unit test đối chiếu thông tin này với đủ 20 ô của bảng chuyển trạng thái. POST vẫn gọi service kiểm tra lại, kể cả khi người dùng giữ trang cũ hoặc tự sửa request.
- CUSTOMER có `/cart`, `/checkout`, `/orders`, `/orders/{id}`. STAFF/ADMIN có `/staff/orders` và `/staff/orders/{id}`, lọc theo trạng thái. Khách chưa đăng nhập được xem danh mục và có liên kết đăng nhập để mua hàng.
- Header và trang chủ có liên kết theo vai trò đến giỏ, đặt hàng, lịch sử đơn và quản lý đơn. Trang chi tiết đơn mở từ danh sách đơn; các trang con có liên kết quay lại. Thêm giỏ có trên cả danh mục lẫn chi tiết sản phẩm.
- Customer ID lấy từ phiên đăng nhập ở service, không nhận từ form. Truy cập chi tiết/hủy đơn kiểm tra chủ đơn; mọi thao tác ghi qua POST có CSRF. DTO được dựng trong transaction, không chứa mật khẩu và không cần OSIV để render.
- Giao diện dùng policy mua hàng có sẵn: Pet AVAILABLE mua một con, phụ kiện đủ kho, gói tháng mua qua giỏ; dịch vụ grooming/thú y cần lịch hẹn nên hiện lý do thay vì nút thêm giỏ. Kiểm tra lại khi thêm, đổi số lượng, đặt và xác nhận.
- Giá trong giỏ là giá hiện tại; đơn giá được chụp khi đặt đơn. Tạm tính/thành tiền dùng BigDecimal. PENDING chưa giữ hàng. Chưa thêm phí giao hàng, thanh toán trực tuyến hoặc giảm giá mới.
- Nút hủy của khách chỉ có ở PENDING/CONFIRMED. SHIPPING/COMPLETED/CANCELLED không được hủy. Mỗi trạng thái có badge màu và nhãn tiếng Việt.
- Lỗi nghiệp vụ và lỗi request giỏ hàng trở về trang danh sách tương ứng bằng flash tiếng Việt; địa chỉ thiếu/quá dài hiển thị lỗi ngay tại form đặt hàng. Không dùng Referer để điều hướng lỗi.
- Danh sách đơn hiện chưa phân trang; phạm vi 3B chỉ yêu cầu lịch sử và lọc trạng thái.
- Bản sửa review: `QuantityForm` không mặc định số lượng. Request thiếu `quantity` bị từ chối bằng flash tiếng Việt và giỏ không đổi; ô thêm giỏ vẫn hiển thị sẵn 1.
- Lịch sử/quản lý đơn dùng `OrderSummaryView` với mã đơn, ngày, trạng thái, thành tiền đã chốt, số dòng và tên khách để giữ nguyên cột hiển thị. Truy vấn projection đếm dòng tại DB, không tải entity dòng hàng/sản phẩm; chỉ trang chi tiết dùng `OrderView` đầy đủ. Không thêm cột giao diện.

## Kiểm thử

```powershell
mvn test
mvn test '-Dtest=CommerceWebTest,OrderStateTest'
```

CommerceWebTest dùng H2 riêng, tắt OSIV và không bao cả test trong transaction. Kiểm tra thêm/đổi/xóa giỏ, đặt hàng, xác nhận/giao/hoàn tất, hủy chờ/đã xác nhận, trừ/hoàn kho, Pet đã bán, lọc và badge/nút theo State. Kiểm tra thêm quyền sở hữu, CSRF, request sửa tay, dữ liệu sai, sản phẩm bị ẩn và menu/trang chủ đúng vai trò.

Test sửa review xác nhận thiếu hẳn tham số số lượng không đổi giỏ, số lượng hợp lệ vẫn cập nhật và form thêm giỏ vẫn có giá trị 1. `OrderSummaryRepositoryTest` kiểm tra tiền đã chốt, số dòng khác tổng số lượng, thứ tự/lọc theo khách và trạng thái; Hibernate Statistics xác nhận truy vấn danh sách chạy một câu SQL và không tải entity/collection.

Kết quả sau sửa review `mvn -B -ntp test`: 157 test đạt, 0 failure, 0 error, 0 skipped (Java 21, H2).

## Tự kiểm tra trên SQL Server

Chưa kiểm chứng SQL Server thật vì không có thông tin đăng nhập. Tạo database `petcare_hub` và đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` theo README. Không ghi mật khẩu thật vào file.

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Mở http://localhost:8080. Tài khoản mẫu chỉ được tạo nếu bảng users rỗng; sản phẩm demo chỉ được tạo nếu bảng products rỗng. Dùng tài khoản đã có nếu database không còn rỗng. Có thể dùng hai trình duyệt/phiên riêng để giữ phiên CUSTOMER và STAFF.

1. **CUSTOMER thêm giỏ và đặt hàng:** đăng nhập `customer@petcare.vn / Customer@123`; từ trang chủ vào Danh mục, thêm phụ kiện số lượng 2 và một Pet AVAILABLE. Vào Giỏ hàng, thử cập nhật số lượng, xóa rồi thêm lại. Chọn Đặt hàng, nhập địa chỉ và gửi. Kiểm tra flash thành công, giỏ trống, đơn Chờ xác nhận trong Đơn hàng của tôi; mở chi tiết kiểm tra địa chỉ và tiền.
2. **STAFF xác nhận:** đăng nhập `staff@petcare.vn / Staff@123`; chọn Quản lý đơn hàng, lọc Chờ xác nhận, mở đơn vừa tạo và bấm Xác nhận. Kiểm tra badge Đã xác nhận, phụ kiện giảm đúng số lượng, Pet đã bán; không thể thêm Pet đó lần nữa.
3. **STAFF giao:** bấm Giao hàng. Badge chuyển Đang giao, chỉ còn nút Hoàn tất; CUSTOMER không còn nút Hủy đơn.
4. **STAFF hoàn tất:** bấm Hoàn tất. Kiểm tra badge Hoàn thành, không còn nút chuyển trạng thái; lọc Hoàn thành phải thấy đơn. CUSTOMER mở lịch sử/chi tiết thấy trạng thái mới và giá lúc mua.
5. **CUSTOMER hủy đơn khác:** tạo đơn mới và hủy ngay khi Chờ xác nhận; tồn kho không đổi. Tạo thêm một đơn, nhờ STAFF xác nhận rồi CUSTOMER hủy trước khi giao; phụ kiện được hoàn đúng số lượng và Pet trở lại AVAILABLE. Đơn đã hoàn tất ở bước 4 không thể hủy.
6. **Kiểm tra phân quyền/lỗi:** khách chưa đăng nhập không thấy menu giỏ/đơn; CUSTOMER không thấy quản lý đơn; STAFF/ADMIN không thấy giỏ của khách. Tài khoản CUSTOMER khác không xem/hủy được đơn không thuộc mình. Thử nhập số lượng vượt tồn kho để kiểm tra flash tiếng Việt, và thử hai tab thao tác cùng đơn để xác nhận thao tác cũ bị từ chối.
7. **Kiểm tra bản sửa review:** với giỏ có số lượng 3, dùng công cụ phát triển trình duyệt bỏ thuộc tính `name` của ô số lượng trước khi bấm Cập nhật; phải nhận flash lỗi và số lượng vẫn là 3. Tải lại trang, nhập 4 rồi cập nhật phải thành công. Mở lịch sử/quản lý đơn, thử bộ lọc trống và từng trạng thái, kiểm tra các cột, badge/nút và trang chi tiết giữ nguyên. Nếu bật log SQL trên database thử nghiệm, truy vấn danh sách chỉ lấy các trường tóm tắt và COUNT dòng hàng, không truy vấn lấy sản phẩm cho từng đơn.
