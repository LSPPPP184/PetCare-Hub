# Rà soát điều hướng GĐ9 — 18/11/2026

Đã đối chiếu toàn bộ mapping trong các controller với menu, liên kết và form Thymeleaf. Kiểm thử `StageNineDemoTest.allFeatureGetRoutesAreReachableByFollowingLinksForTheirRoles` xuất phát từ `/`, duyệt các `href` nội bộ bằng MockMvc cho khách/CUSTOMER/STAFF/ADMIN và yêu cầu HTTP 200. Mỗi mẫu trang lấy một ID và một bộ query làm đại diện; kiểm thử module riêng kiểm tra những trạng thái khác. Chi tiết lịch sử quiz cần gửi quiz trước, được kiểm chứng trong `MatchingWebTest`.

**Kết quả:** không phát hiện trang nghiệp vụ mồ côi. Hai dashboard đã có menu từ trước nhưng thiếu liên kết trực tiếp trong phần nội dung trang chủ; đã bổ sung đúng vai trò. Dashboard quản trị mới có liên kết đến đơn hàng, lịch làm việc, cảnh báo tiêm phòng, sửa sản phẩm top 5 và thành viên. Các nút POST chỉ hiện khi vai trò/State cho phép.

404/403/500 là trang phản hồi lỗi, không phải chức năng cần gắn menu; đều dùng header/footer chung và có nút Về trang chủ. `/403` do Security chuyển tiếp, `/error` do Spring Boot xử lý. Không cung cấp route gây lỗi 500 trong mã production; test dùng controller giả trong test.

## Toàn bộ route ứng dụng

| Controller | HTTP / route | Vai trò | Đường dẫn vào / form |
| --- | --- | --- | --- |
| AppointmentController | `GET /appointments/services` | Công khai | Menu dịch vụ / lịch của tôi; chọn dịch vụ → khung giờ → xác nhận; danh sách → chi tiết |
| AppointmentController | `GET /appointments/slots` | CUSTOMER, chỉ dữ liệu của mình | Menu dịch vụ / lịch của tôi; chọn dịch vụ → khung giờ → xác nhận; danh sách → chi tiết |
| AppointmentController | `GET /appointments/confirm` | CUSTOMER, chỉ dữ liệu của mình | Menu dịch vụ / lịch của tôi; chọn dịch vụ → khung giờ → xác nhận; danh sách → chi tiết |
| AppointmentController | `POST /appointments` | CUSTOMER, chỉ dữ liệu của mình | Menu dịch vụ / lịch của tôi; chọn dịch vụ → khung giờ → xác nhận; danh sách → chi tiết |
| AppointmentController | `GET /appointments` | CUSTOMER, chỉ dữ liệu của mình | Menu dịch vụ / lịch của tôi; chọn dịch vụ → khung giờ → xác nhận; danh sách → chi tiết |
| AppointmentController | `GET /appointments/{id}` | CUSTOMER, chỉ dữ liệu của mình | Menu dịch vụ / lịch của tôi; chọn dịch vụ → khung giờ → xác nhận; danh sách → chi tiết |
| AppointmentController | `POST /appointments/{id}/cancel` | CUSTOMER, chỉ dữ liệu của mình | Menu dịch vụ / lịch của tôi; chọn dịch vụ → khung giờ → xác nhận; danh sách → chi tiết |
| AuthController | `GET /login` | Công khai | Menu khách / form đăng ký |
| AuthController | `GET /register` | Công khai | Menu khách / form đăng ký |
| AuthController | `POST /register` | Công khai | Menu khách / form đăng ký |
| CartController | `GET /cart` | CUSTOMER, chỉ dữ liệu của mình | Menu CUSTOMER; chi tiết sản phẩm; form giỏ hàng / thanh toán |
| CartController | `POST /cart/items` | CUSTOMER, chỉ dữ liệu của mình | Menu CUSTOMER; chi tiết sản phẩm; form giỏ hàng / thanh toán |
| CartController | `POST /cart/items/{id}/quantity` | CUSTOMER, chỉ dữ liệu của mình | Menu CUSTOMER; chi tiết sản phẩm; form giỏ hàng / thanh toán |
| CartController | `POST /cart/items/{id}/remove` | CUSTOMER, chỉ dữ liệu của mình | Menu CUSTOMER; chi tiết sản phẩm; form giỏ hàng / thanh toán |
| CartController | `GET /checkout` | CUSTOMER, chỉ dữ liệu của mình | Menu CUSTOMER; chi tiết sản phẩm; form giỏ hàng / thanh toán |
| CartController | `POST /checkout` | CUSTOMER, chỉ dữ liệu của mình | Menu CUSTOMER; chi tiết sản phẩm; form giỏ hàng / thanh toán |
| DemoDateController | `GET /admin/demo/health` | ADMIN | Menu và trang chủ Giả lập ngày tiêm phòng (chỉ demo); form chọn ngày |
| DemoDateController | `POST /admin/demo/health` | ADMIN | Menu và trang chủ Giả lập ngày tiêm phòng (chỉ demo); form chọn ngày |
| LoyaltyController | `GET /membership` | CUSTOMER, chỉ dữ liệu của mình | Menu Thẻ thành viên / Quản lý thành viên → chi tiết thẻ; form đổi voucher / điều chỉnh |
| LoyaltyController | `POST /membership/redeem` | CUSTOMER, chỉ dữ liệu của mình | Menu Thẻ thành viên / Quản lý thành viên → chi tiết thẻ; form đổi voucher / điều chỉnh |
| LoyaltyController | `GET /admin/members` | ADMIN | Menu Thẻ thành viên / Quản lý thành viên → chi tiết thẻ; form đổi voucher / điều chỉnh |
| LoyaltyController | `GET /admin/members/{id}` | ADMIN | Menu Thẻ thành viên / Quản lý thành viên → chi tiết thẻ; form đổi voucher / điều chỉnh |
| LoyaltyController | `POST /admin/members/{id}/adjust` | ADMIN | Menu Thẻ thành viên / Quản lý thành viên → chi tiết thẻ; form đổi voucher / điều chỉnh |
| MatchingController | `GET /quiz` | Công khai | Menu Quiz / Kết quả gần nhất / Lịch sử → chi tiết kết quả đã lưu; form gửi quiz |
| MatchingController | `POST /quiz` | Công khai | Menu Quiz / Kết quả gần nhất / Lịch sử → chi tiết kết quả đã lưu; form gửi quiz |
| MatchingController | `GET /quiz/results` | Công khai | Menu Quiz / Kết quả gần nhất / Lịch sử → chi tiết kết quả đã lưu; form gửi quiz |
| MatchingController | `GET /quiz/history` | Đã đăng nhập, chỉ lịch sử của mình | Menu Quiz / Kết quả gần nhất / Lịch sử → chi tiết kết quả đã lưu; form gửi quiz |
| MatchingController | `GET /quiz/history/{id}` | Đã đăng nhập, chỉ lịch sử của mình | Menu Quiz / Kết quả gần nhất / Lịch sử → chi tiết kết quả đã lưu; form gửi quiz |
| OrderController | `GET /orders` | CUSTOMER, chỉ dữ liệu của mình | Menu Đơn hàng của tôi → chi tiết; nút hủy khi State cho phép |
| OrderController | `GET /orders/{id}` | CUSTOMER, chỉ dữ liệu của mình | Menu Đơn hàng của tôi → chi tiết; nút hủy khi State cho phép |
| OrderController | `POST /orders/{id}/cancel` | CUSTOMER, chỉ dữ liệu của mình | Menu Đơn hàng của tôi → chi tiết; nút hủy khi State cho phép |
| OwnedPetController | `GET /my-pets` | CUSTOMER, chỉ dữ liệu của mình | Menu Thú cưng của tôi / Thêm thú cưng → hồ sơ → sửa; form đổi lịch trong timeline |
| OwnedPetController | `GET /my-pets/new` | CUSTOMER, chỉ dữ liệu của mình | Menu Thú cưng của tôi / Thêm thú cưng → hồ sơ → sửa; form đổi lịch trong timeline |
| OwnedPetController | `POST /my-pets` | CUSTOMER, chỉ dữ liệu của mình | Menu Thú cưng của tôi / Thêm thú cưng → hồ sơ → sửa; form đổi lịch trong timeline |
| OwnedPetController | `GET /my-pets/{id}` | CUSTOMER, chỉ dữ liệu của mình | Menu Thú cưng của tôi / Thêm thú cưng → hồ sơ → sửa; form đổi lịch trong timeline |
| OwnedPetController | `GET /my-pets/{id}/edit` | CUSTOMER, chỉ dữ liệu của mình | Menu Thú cưng của tôi / Thêm thú cưng → hồ sơ → sửa; form đổi lịch trong timeline |
| OwnedPetController | `POST /my-pets/{id}/edit` | CUSTOMER, chỉ dữ liệu của mình | Menu Thú cưng của tôi / Thêm thú cưng → hồ sơ → sửa; form đổi lịch trong timeline |
| OwnedPetController | `POST /my-pets/vaccinations/{id}/reschedule` | CUSTOMER, chỉ dữ liệu của mình | Menu Thú cưng của tôi / Thêm thú cưng → hồ sơ → sửa; form đổi lịch trong timeline |
| PageController | `GET /` | Công khai | Logo / menu và trang chủ theo vai trò; /403 do Security chuyển tiếp |
| PageController | `GET /admin/dashboard` | ADMIN | Logo / menu và trang chủ theo vai trò; /403 do Security chuyển tiếp |
| PageController | `GET /staff/dashboard` | STAFF, ADMIN | Logo / menu và trang chủ theo vai trò; /403 do Security chuyển tiếp |
| PageController | `GET /403` | Công khai | Logo / menu và trang chủ theo vai trò; /403 do Security chuyển tiếp |
| ProductController | `GET /products` | Công khai | Menu Danh mục / Quản lý sản phẩm → chi tiết / thêm / sửa; form lưu / ẩn |
| ProductController | `GET /products/{id}` | Công khai | Menu Danh mục / Quản lý sản phẩm → chi tiết / thêm / sửa; form lưu / ẩn |
| ProductController | `GET /staff/products` | STAFF, ADMIN | Menu Danh mục / Quản lý sản phẩm → chi tiết / thêm / sửa; form lưu / ẩn |
| ProductController | `GET /staff/products/new` | STAFF, ADMIN | Menu Danh mục / Quản lý sản phẩm → chi tiết / thêm / sửa; form lưu / ẩn |
| ProductController | `GET /staff/products/{id}/edit` | STAFF, ADMIN | Menu Danh mục / Quản lý sản phẩm → chi tiết / thêm / sửa; form lưu / ẩn |
| ProductController | `POST /staff/products/save` | STAFF, ADMIN | Menu Danh mục / Quản lý sản phẩm → chi tiết / thêm / sửa; form lưu / ẩn |
| ProductController | `POST /staff/products/{id}/hide` | STAFF, ADMIN | Menu Danh mục / Quản lý sản phẩm → chi tiết / thêm / sửa; form lưu / ẩn |
| StaffAppointmentController | `GET /staff/appointments` | STAFF, ADMIN | Menu Lịch làm việc → chi tiết; nút thao tác theo State |
| StaffAppointmentController | `GET /staff/appointments/{id}` | STAFF, ADMIN | Menu Lịch làm việc → chi tiết; nút thao tác theo State |
| StaffAppointmentController | `POST /staff/appointments/{id}/start` | STAFF, ADMIN | Menu Lịch làm việc → chi tiết; nút thao tác theo State |
| StaffAppointmentController | `POST /staff/appointments/{id}/finish` | STAFF, ADMIN | Menu Lịch làm việc → chi tiết; nút thao tác theo State |
| StaffAppointmentController | `POST /staff/appointments/{id}/cancel` | STAFF, ADMIN | Menu Lịch làm việc → chi tiết; nút thao tác theo State |
| StaffOrderController | `GET /staff/orders` | STAFF, ADMIN | Menu Quản lý đơn hàng → chi tiết; nút thao tác theo State |
| StaffOrderController | `GET /staff/orders/{id}` | STAFF, ADMIN | Menu Quản lý đơn hàng → chi tiết; nút thao tác theo State |
| StaffOrderController | `POST /staff/orders/{id}/confirm` | STAFF, ADMIN | Menu Quản lý đơn hàng → chi tiết; nút thao tác theo State |
| StaffOrderController | `POST /staff/orders/{id}/ship` | STAFF, ADMIN | Menu Quản lý đơn hàng → chi tiết; nút thao tác theo State |
| StaffOrderController | `POST /staff/orders/{id}/complete` | STAFF, ADMIN | Menu Quản lý đơn hàng → chi tiết; nút thao tác theo State |
| StaffOrderController | `POST /staff/orders/{id}/cancel` | STAFF, ADMIN | Menu Quản lý đơn hàng → chi tiết; nút thao tác theo State |
| StaffPetHealthController | `GET /staff/health` | STAFF, ADMIN | Menu Theo dõi tiêm phòng / Hồ sơ sức khỏe → hồ sơ → thêm bản ghi; form trong timeline |
| StaffPetHealthController | `GET /staff/health/pets` | STAFF, ADMIN | Menu Theo dõi tiêm phòng / Hồ sơ sức khỏe → hồ sơ → thêm bản ghi; form trong timeline |
| StaffPetHealthController | `GET /staff/health/pets/{id}` | STAFF, ADMIN | Menu Theo dõi tiêm phòng / Hồ sơ sức khỏe → hồ sơ → thêm bản ghi; form trong timeline |
| StaffPetHealthController | `GET /staff/health/pets/{id}/records/new` | STAFF, ADMIN | Menu Theo dõi tiêm phòng / Hồ sơ sức khỏe → hồ sơ → thêm bản ghi; form trong timeline |
| StaffPetHealthController | `POST /staff/health/pets/{id}/records` | STAFF, ADMIN | Menu Theo dõi tiêm phòng / Hồ sơ sức khỏe → hồ sơ → thêm bản ghi; form trong timeline |
| StaffPetHealthController | `POST /staff/health/vaccinations/{id}/complete` | STAFF, ADMIN | Menu Theo dõi tiêm phòng / Hồ sơ sức khỏe → hồ sơ → thêm bản ghi; form trong timeline |
| StaffPetHealthController | `POST /staff/health/vaccinations/{id}/reschedule` | STAFF, ADMIN | Menu Theo dõi tiêm phòng / Hồ sơ sức khỏe → hồ sơ → thêm bản ghi; form trong timeline |
| SubscriptionController | `GET /subscriptions` | CUSTOMER, chỉ dữ liệu của mình | Menu Gói của tôi / Gói đang hoạt động; form hủy / gia hạn trên danh sách |
| SubscriptionController | `GET /admin/subscriptions` | ADMIN | Menu Gói của tôi / Gói đang hoạt động; form hủy / gia hạn trên danh sách |
| SubscriptionController | `POST /subscriptions/{id}/cancel` | CUSTOMER, chỉ dữ liệu của mình | Menu Gói của tôi / Gói đang hoạt động; form hủy / gia hạn trên danh sách |
| SubscriptionController | `POST /subscriptions/{id}/renew` | CUSTOMER, chỉ dữ liệu của mình | Menu Gói của tôi / Gói đang hoạt động; form hủy / gia hạn trên danh sách |

Tổng: **71 mapping controller**. Ngoài ra Spring Security xử lý `POST /login` (form đăng nhập) và `POST /logout` (form header); cả hai có CSRF. Spring Boot xử lý `/error` theo mã HTTP.

## Rà soát quyền và liên kết

- Khách chỉ thấy đăng ký/đăng nhập và các trang công khai; không thấy đơn, thú sở hữu, lịch cá nhân, thẻ hay gói.
- CUSTOMER thấy dữ liệu của mình; đoán ID của đơn/lịch/thú/gói người khác bị service từ chối.
- STAFF xem lịch được giao; ADMIN xem mọi lịch. Khách và STAFF không có quyền dashboard/thành viên/gói quản trị.
- Các route thay đổi dữ liệu dùng POST; form Thymeleaf thêm `_csrf`. Thiếu token bị chặn trước controller.
- GET không biết đường dẫn được đưa tới 404 công khai; phương thức khác không khớp nhóm route cho phép bị từ chối. Dispatcher ERROR/FORWARD được phép để trang lỗi không quay vòng đăng nhập.
- Chỉ kiểm tra tự động nội dung HTML/HTTP và quyền; chưa thực hiện kiểm tra hình ảnh trên trình duyệt với SQL Server thật.
