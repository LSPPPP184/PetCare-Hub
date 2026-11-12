# Giai đoạn 4 — Đặt lịch dịch vụ

## Phạm vi và quy ước

- Appointment lưu khách, thông tin thú cưng, dịch vụ Grooming/Vet, STAFF thực hiện, giờ bắt đầu/kết thúc, ghi chú, giá chốt và enum `stateName`. Có `@Version`; State object dựng lại bằng Factory khi đọc. Không thay đổi State của Order.
- Giá chốt từ `calculatePrice()` và endTime từ durationMinutes lúc đặt; chỉnh giá/thời lượng sản phẩm sau đó không đổi lịch cũ. Tiền dùng BigDecimal(19,2).
- Clock inject dùng `Asia/Ho_Chi_Minh`. DB lưu LocalDateTime theo giờ Việt Nam; mọi màn hình ghi rõ múi giờ.
- Mặc định làm việc 08:00–18:00, thứ Hai–thứ Bảy. Cấu hình `petcare.appointments.open`, `close`, `working-days`, `slot-minutes` trong application.properties; khung giờ gợi ý cách nhau 30 phút. Cả thời gian bắt đầu và kết thúc phải nằm trong cùng ca làm việc, không đặt trong quá khứ. POST kiểm tra lại chính sách, không tin dữ liệu ẩn trên form.
- Khoảng thời gian là `[start, end)`: lịch kết thúc 10:30 không cản lịch bắt đầu 10:30. Chỉ cấm trùng cùng STAFF, kể cả giữa hai dịch vụ khác nhau. Lịch CANCELLED không chiếm chỗ; các trạng thái khác vẫn chiếm khoảng đã đặt.
- Dùng lại CommerceLocks để khóa hàng users của STAFF trước khi kiểm tra trùng và ghi lịch trong cùng transaction. Thay đổi State cũng khóa STAFF rồi Appointment; không dùng khóa Java hoặc cơ chế khóa thứ hai. Danh sách giờ trống chỉ mang tính gợi ý, được kiểm tra lại khi xác nhận.
- CUSTOMER chỉ xem/hủy lịch của mình. Hủy khi còn **đúng 2 giờ trở lên** và State còn BOOKED. STAFF chỉ xem/cập nhật lịch được giao, ADMIN quản lý tất cả. STAFF/ADMIN được hủy BOOKED không áp dụng mốc hai giờ để xử lý sự cố.
- Giả định: mọi STAFF có thể thực hiện cả Grooming và Vet; tên bác sĩ trong VetService hiện chỉ là mô tả dịch vụ, chưa ánh xạ thành tài khoản hoặc chứng chỉ. Chưa có ca làm việc riêng/ngày nghỉ riêng cho từng người.
- Giả định: STAFF/ADMIN chủ động bấm bắt đầu/hoàn tất; không thêm ràng buộc thời điểm bắt đầu/hoàn tất ngoài bảng State. Chưa có đổi lịch, thanh toán, email hay phí hủy.
- Header/trang chủ liên kết đến dịch vụ đặt lịch (mọi vai trò), lịch cá nhân (CUSTOMER), lịch làm việc (STAFF/ADMIN). Từ dịch vụ tới chọn ngày/giờ, xác nhận, chi tiết; các trang con có liên kết quay lại. Trang sản phẩm Grooming/Vet có liên kết đặt lịch cho CUSTOMER.

## State

| Trạng thái | book | start | finish | cancel |
|---|---|---|---|---|
| Chưa khởi tạo | BOOKED | Lỗi | Lỗi | Lỗi |
| BOOKED | Lỗi | IN_PROGRESS | Lỗi | CANCELLED |
| IN_PROGRESS | Lỗi | Lỗi | DONE | Lỗi |
| DONE | Lỗi | Lỗi | Lỗi | Lỗi |
| CANCELLED | Lỗi | Lỗi | Lỗi | Lỗi |

book chỉ được thực hiện một lần trong factory khởi tạo Appointment. Các hành động sai dùng lại `domain.order.InvalidStateTransitionException` với thông báo tiếng Việt. Nút lấy từ State; nút hủy của khách còn xét mốc hai giờ bằng Clock.

## Kiểm thử

Kết quả `mvn -B -ntp test`: **212 test đạt, 0 failure, 0 error, 0 skipped**, gồm 55 test mới cho GĐ4 (Java 21, H2).

```powershell
mvn test
mvn test '-Dtest=AppointmentPolicyTest,AppointmentStateTest,AppointmentWebTest,AppointmentDemoTest'
```

- Clock cố định trong test; kiểm tra giờ Việt Nam, ngày/giờ làm việc, quá khứ trong giờ hợp lệ, giới hạn thời lượng, các biên trùng lịch và mốc hủy đúng/trước hai giờ.
- Toàn bộ 16 ô chuyển trạng thái sau khi đặt, khởi tạo BOOKED, lưu/đọc lại enum, snapshot tiền/thời lượng.
- MockMvc qua giao dịch thật, tắt OSIV: luồng đặt/tìm giờ/bắt đầu/hoàn tất/hủy, thông báo tiếng Việt, CSRF, giả mạo URL, quyền sở hữu/nhân viên được giao, ADMIN và menu theo vai trò.
- Hai yêu cầu đồng thời cùng STAFF/giờ chỉ một yêu cầu thành công; Future timeout chỉ phát hiện treo khóa, không dùng sleep để giả định thứ tự.
- Demo tạo STAFF bằng BCrypt, lịch nhiều trạng thái, không nhân đôi khi chạy lại. Test H2 không thay thế việc kiểm chứng SQL Server thật.

## Dữ liệu demo

Sau các seeder cũ, ApplicationReadyEvent kích hoạt seeder lịch hẹn trong profile demo:

- Bổ sung `appointment.staff1@petcare.vn` và `appointment.staff2@petcare.vn`, mật khẩu mẫu `Staff@123` (lưu BCrypt); giữ nguyên tài khoản cũ.
- Nếu bảng appointments rỗng và có dịch vụ hợp lệ/khách: tạo 5 lịch, gồm 2 BOOKED cùng giờ ngày làm việc kế tiếp cho hai STAFF khác nhau; 1 CANCELLED cùng khung giờ; 1 DONE ngày làm việc trước; 1 IN_PROGRESS ở ngày làm việc sau ngày BOOKED.
- Lịch IN_PROGRESS ở ngày tương lai chỉ minh họa State để thao tác demo. Thời gian và giá snapshot lấy từ dịch vụ đang có. Không ghi đè hoặc nhân đôi lịch khi bảng đã có dữ liệu.

## Tự kiểm tra trên SQL Server

Chưa chạy trên SQL Server thật vì không có thông tin đăng nhập. Dùng database phát triển và cấu hình DB_URL, DB_USERNAME, DB_PASSWORD theo README.

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

1. Mở http://localhost:8080. Kiểm tra log tạo bảng appointments, khóa ngoại/index không có lỗi. Với database cũ, tài khoản/sản phẩm mẫu chỉ được tạo theo điều kiện của seeder GĐ1/GĐ2; cần có ít nhất một Grooming/Vet hợp lệ để có lịch mẫu.
2. Khi chưa đăng nhập, từ trang chủ chọn **Đặt lịch dịch vụ**: xem danh sách và liên kết đăng nhập; không thấy lịch cá nhân hoặc lịch làm việc.
3. Đăng nhập `customer@petcare.vn / Customer@123`. Chọn dịch vụ → ngày làm việc tương lai → khung giờ và STAFF → nhập tên thú cưng, loài, ghi chú → **Xác nhận đặt lịch**. Kiểm tra thông báo thành công, giá, giờ kết thúc và **Lịch hẹn của tôi**.
4. Mở hai phiên CUSTOMER, cùng giữ một trang xác nhận cho cùng STAFF/giờ. Gửi phiên thứ nhất rồi phiên thứ hai: phiên sau phải báo đã có lịch. Chọn cùng giờ nhưng STAFF khác thì được đặt. Lịch nối tiếp đúng endTime vẫn được phép.
5. Thử Chủ nhật hoặc ngày quá khứ: không có giờ phù hợp. Ngày làm việc chỉ có khung kết thúc không quá 18:00. Nếu đổi cấu hình giờ/ngày làm việc, khởi động lại và kiểm tra danh sách tương ứng.
6. Đăng nhập đúng STAFF được giao (`staff@petcare.vn / Staff@123` hoặc tài khoản appointment.staff1/2 ở trên). Chọn **Lịch làm việc**, chọn ngày đã đặt, mở chi tiết → **Bắt đầu** → **Hoàn tất**. Kiểm tra badge/nút thay đổi; STAFF khác không xem/sửa được lịch này. ADMIN (`admin@petcare.vn / Admin@123`) thấy và quản lý toàn bộ.
7. CUSTOMER tạo lịch khác cách hiện tại hơn hai giờ rồi hủy từ lịch cá nhân. Quay lại chọn giờ: khung vừa hủy phải trống. Với lịch dưới hai giờ, nút hủy không xuất hiện; POST từ trang cũ vẫn phải bị từ chối bằng thông báo tiếng Việt. Đúng mốc hai giờ được kiểm chứng bằng test Clock cố định.
8. Dùng ADMIN xem ngày làm việc kế tiếp để thấy hai lịch BOOKED demo cùng giờ, khác STAFF; chọn ngày làm việc trước để xem DONE và ngày sau nhóm BOOKED để xem IN_PROGRESS. CANCELLED không cản khung giờ mới. Khởi động lại profile demo, số lịch mẫu không tăng.
9. Kiểm tra hồi quy mua hàng/đơn hàng GĐ3. Với SQL Server, thử đồng thời hai lượt đặt cùng nhân viên và kiểm tra chỉ có một bản ghi không hủy trong khoảng đó; H2 xanh chưa chứng minh cấu hình khóa/isolation thực tế của SQL Server.
