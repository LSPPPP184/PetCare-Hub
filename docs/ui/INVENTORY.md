# Kiểm kê giao diện PetCare Hub

Ngày rà soát: 19/11/2026. Chỉ đọc mã nguồn; bản đề xuất và xem trước nằm trong `docs/ui/`.

## Nguồn và hiện trạng

- Đã đọc `AGENTS.md`, 46 file HTML trong `src/main/resources/templates`, các controller, `SecurityConfig`, `ProductSecurityConfig` và enum/lớp State liên quan.
- **Không có thư mục `src/main/resources/static` tại thời điểm rà soát.** CSS hiện tại chủ yếu là Bootstrap 5.3.3 qua CDN trong `fragments/layout.html`, kèm một ít style nội tuyến. Không có bộ CSS/JS riêng trong static để kế thừa.
- 46 template gồm 34 template trang và 12 file fragment. Một template có thể phục vụ nhiều URL/vai trò. URL dưới đây là đường dẫn ứng dụng thật, không phải đường dẫn bản xem trước.
- Phân quyền dựa trên cấu hình và điều kiện `sec:authorize`, không suy đoán từ tên trang. ADMIN truy cập nhóm STAFF nhưng **không tự động có quyền CUSTOMER**. Lịch sử quiz dành cho mọi tài khoản đã đăng nhập.

## Trang công khai / khách

Các trang công khai cũng có thể được tài khoản đã đăng nhập xem; menu đăng nhập/đăng ký chỉ hiển thị khi là khách.

| Trang | URL | Template | Nội dung và điểm vào |
| --- | --- | --- | --- |
| Trang chủ | `/` | `home.html` | Liên kết danh mục, dịch vụ, quiz và các khu vực theo vai trò |
| Danh mục | `/products` | `products/list.html` | Tìm tên, nhóm PET/ACCESSORY/SERVICE, sắp giá, phân trang |
| Chi tiết sản phẩm | `/products/{id}` | `products/detail.html` | Ảnh, thuộc tính tùy loại, giá, thao tác mua theo vai trò |
| Danh sách dịch vụ | `/appointments/services` | `appointments/services.html` | Chọn dịch vụ; các bước đặt lịch yêu cầu CUSTOMER |
| Quiz chọn thú cưng | `/quiz` | `matching/quiz.html` | Điều kiện sống và chiến lược ghép đôi |
| Kết quả quiz hiện tại | `/quiz/results` | `matching/results.html` | Điểm, lý do, giải thích; trạng thái chưa có kết quả |
| Đăng nhập | `/login` | `login.html` | Email, mật khẩu, lỗi đăng nhập/đăng xuất |
| Đăng ký | `/register` | `register.html` | Dữ liệu tài khoản và lỗi validation |

## CUSTOMER

Ngoài trang công khai, các màn hình sau dành cho khách hàng. Hồ sơ, đơn hàng và lịch sử thuộc tài khoản đang xem; prototype không thay thế kiểm tra quyền sở hữu ở service.

| Trang | URL | Template | Điểm vào / thao tác |
| --- | --- | --- | --- |
| Giỏ hàng | `/cart` | `commerce/cart.html` | Menu; cập nhật số lượng, bỏ sản phẩm |
| Thanh toán mô phỏng | `/checkout` | `commerce/checkout.html` | Menu/giỏ; thông tin nhận hàng, tổng tiền sau giảm |
| Đơn hàng của tôi | `/orders` | `commerce/orders.html` | Menu; mở chi tiết |
| Chi tiết đơn | `/orders/{id}` | `commerce/order.html` | Danh sách đơn; hủy khi hợp lệ |
| Chọn khung giờ | `/appointments/slots?serviceId=…` | `appointments/slots.html` | Từ danh sách dịch vụ; ngày, nhân viên, giờ trống |
| Xác nhận đặt lịch | `/appointments/confirm?serviceId=…&staffId=…&…` | `appointments/confirm.html` | Từ khung giờ; thông tin thú cưng, báo giá, quyền lợi gói |
| Lịch hẹn của tôi | `/appointments` | `appointments/list.html` | Menu; danh sách và trạng thái |
| Chi tiết lịch | `/appointments/{id}` | `appointments/detail.html` | Từ danh sách; hủy khi được phép |
| Thú cưng của tôi | `/my-pets` | `health/pets.html` | Menu; danh sách hồ sơ |
| Thêm / sửa thú cưng | `/my-pets/new`, `/my-pets/{id}/edit` | `health/pet-form.html` | Menu thêm, nút sửa trong hồ sơ |
| Hồ sơ sức khỏe | `/my-pets/{id}` | `health/detail.html` | Thông tin thú, timeline, lịch sử khám, dời lịch tiêm |
| Thẻ thành viên | `/membership` | `loyalty/card.html` | Điểm hiện có/tích lũy, hạng, tiến độ, đổi điểm, lịch sử |
| Gói của tôi | `/subscriptions` | `subscriptions/list.html` | Dịch vụ, thời hạn, lượt còn, gia hạn/hủy hợp lệ |

## Mọi người dùng đã đăng nhập

| Trang | URL | Template | Ghi chú |
| --- | --- | --- | --- |
| Lịch sử quiz của tôi | `/quiz/history` | `matching/history.html` | CUSTOMER, STAFF, ADMIN; có phân trang |
| Kết quả quiz đã lưu | `/quiz/history/{id}` | `matching/results.html` | Từ lịch sử; ảnh chụp dữ liệu lúc làm quiz |

## STAFF và ADMIN

| Trang | URL | Template | Nội dung |
| --- | --- | --- | --- |
| Tổng quan công việc | `/staff` | `staff.html` | Đơn chờ, lịch hôm nay, số mũi sắp đến hạn/quá hạn |
| Quản lý sản phẩm | `/staff/products` | `products/list.html` | Dùng lại danh mục, thêm nút sửa/ẩn |
| Thêm / sửa sản phẩm | `/staff/products/new`, `/staff/products/{id}/edit` | `products/form.html` | Trường theo loại sản phẩm và hồ sơ ghép đôi; sửa không ghi đè tồn kho/trạng thái |
| Quản lý đơn hàng | `/staff/orders` | `commerce/orders.html` | Lọc trạng thái |
| Chi tiết, xử lý đơn | `/staff/orders/{id}` | `commerce/order.html` | Xác nhận, giao, hoàn tất, hủy theo State |
| Lịch làm việc | `/staff/appointments` | `appointments/list.html` | Lọc ngày |
| Chi tiết lịch làm việc | `/staff/appointments/{id}` | `appointments/detail.html` | Bắt đầu, hoàn tất, hủy theo State |
| Theo dõi tiêm phòng | `/staff/health?status=…` | `health/alerts.html` | Lọc đủ 4 trạng thái; mỗi dòng là một mũi tiêm |
| Hồ sơ thú toàn cửa hàng | `/staff/health/pets` | `health/pets.html` | Phân trang, mở hồ sơ |
| Chi tiết hồ sơ | `/staff/health/pets/{id}` | `health/detail.html` | Timeline, lịch sử, xác nhận đã tiêm, dời lịch |
| Thêm bản ghi khám | `/staff/health/pets/{id}/records/new` | `health/record-form.html` | Ngày, nội dung, cân nặng, bác sĩ |

## ADMIN riêng

| Trang | URL | Template | Nội dung |
| --- | --- | --- | --- |
| Dashboard | `/admin` | `admin.html` | Lịch hôm nay, số thú sắp/quá hạn; doanh thu 12 tháng, top 5 sản phẩm, thành viên theo hạng |
| Thành viên | `/admin/members` | `loyalty/members.html` | Tìm/lọc hạng, phân trang |
| Thẻ một thành viên | `/admin/members/{id}` | `loyalty/card.html` | Dùng lại thẻ; thêm điều chỉnh điểm và lý do |
| Gói đang hoạt động | `/admin/subscriptions` | `subscriptions/list.html` | Thêm cột khách hàng; không có thao tác gia hạn của khách |
| Giả lập ngày sức khỏe | `/admin/demo/health` | `health/demo.html` | Chỉ có khi bật profile `demo`; có liên kết điều kiện trong menu/trang chủ/dashboard |

`/admin/dashboard` và `/staff/dashboard` là đường dẫn chuyển hướng tương thích về `/admin` và `/staff`, không phải template riêng.

## Trang lỗi dùng chung

`403.html` là trang từ chối truy cập `/403`. `error/403.html`, `error/404.html`, `error/500.html`, `error/request.html` phục vụ lỗi được xử lý bởi ứng dụng. Đây là đích xử lý lỗi, không cần xuất hiện như mục menu nghiệp vụ.

## Thành phần dùng lại

| Thành phần | Nguồn | Hiện trạng / lưu ý cho thiết kế |
| --- | --- | --- |
| Head, menu, footer, flash | `fragments/layout.html` | Bootstrap CDN; menu dài, wrap; flash thành công/lỗi có role thông báo |
| Điều hướng theo tính năng | `health/navigation.html`, `matching/navigation.html`, `loyalty/navigation.html`, `subscriptions/navigation.html` | Dùng trong header và trang chủ, kiểm tra vai trò và profile demo |
| Phân trang | `fragments/pagination.html` | Dùng nhiều danh sách; danh mục có phân trang riêng |
| Thẻ sản phẩm | `products/list.html` | Ảnh + tên + giá + mua + thao tác nhân viên; chưa tách thành fragment toàn thẻ |
| Ảnh và thao tác mua | `products/image.html`, `commerce/purchase.html` | Có fallback chưa có ảnh, lazy loading; mua phụ thuộc vai trò/khả dụng |
| Badge, thao tác đơn | `commerce/order-fragments.html` | Nhãn và class từ status DTO; nút do State cho phép |
| Badge, thao tác lịch | `appointments/fragments.html` | Cùng quy tắc vai trò và State |
| Thẻ mũi tiêm | `health/vaccination.html` | Dùng trong timeline và cảnh báo; nhãn, ngày hẹn/tiêm, ghi chú, dời lịch/xác nhận |
| Timeline | `health/detail.html` | Danh sách có thứ tự theo ngày hẹn, 4 trạng thái; lịch sử khám ở cột bên |
| Thẻ thành viên | `loyalty/card.html` | Tiến độ lên hạng, hai loại điểm, voucher và bảng quyền lợi; dùng lại cho admin |
| Báo giá thành viên | `loyalty/fragments.html` | Giảm theo hạng/voucher và thanh toán dự kiến |
| Kết quả quiz | `matching/results.html` | Thẻ xếp hạng, % phù hợp, lý do, giải thích, link thú cưng; không diễn đạt % như xác suất |
| Bảng | Commerce, appointments, loyalty, subscriptions, admin | Cần header rõ, số căn phải, caption, scroll trong vùng riêng trên điện thoại |
| Form | Auth, products, health, quiz, checkout, appointments | Label, required, lỗi tại trường/tóm tắt, select/date/checkbox; không bỏ hidden version/CSRF khi triển khai thật |
| Trạng thái rỗng | Danh mục, quiz, health, thành viên, gói | Thông báo rõ, liên kết quay lại thao tác phù hợp |

## Ranh giới bản xem trước

Mỗi hướng có 5 trang: trang chủ, danh mục, chi tiết, hồ sơ tiêm phòng CUSTOMER, dashboard ADMIN. Thanh công cụ “Bản xem trước” cho phép đổi giữa các vai trò để so sánh thiết kế; nó **không phải menu phân quyền sản phẩm**. Menu sản phẩm và nội dung từng trang ghi đúng bối cảnh vai trò. Những trang khác chỉ được kiểm kê/đề xuất, chưa dựng. Mọi dữ liệu, số liệu, lịch tiêm là giả lập; không gọi backend, không lưu đơn, không sửa hồ sơ, không tạo seed.
