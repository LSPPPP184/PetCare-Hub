# Kịch bản demo 10 phút

Đối chiếu code ngày 22/11/2026. Đây là kịch bản thao tác dự kiến từ controller, template, seed và test; chưa xác nhận trên SQL Server của người trình bày. Không dùng dữ liệu của `docs/ui/preview` làm dữ liệu ứng dụng.

## Chuẩn bị trước giờ demo

1. Dùng Java 21, SQL Server và **database demo riêng còn trống**. DemoDataRunner chỉ seed khi tất cả bảng được kiểm tra đều trống; database đã có dữ liệu sẽ không tự bổ sung tài khoản/gói/lịch. Không xóa database đang dùng để chuẩn bị demo.
2. Khai báo `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trong môi trường phiên chạy; không ghi mật khẩu DB vào tài liệu hay application.properties. Chạy từ thư mục gốc:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

3. Mở `http://localhost:8080/`; vào `/login`. Chuẩn bị ba phiên trình duyệt tách cookie cho CUSTOMER, STAFF, ADMIN để chuyển nhanh. Mỗi ID trong `{id}` phải lấy bằng bấm liên kết của dữ liệu thật, không giả định ID cố định.
4. Giao diện đang dùng hướng A “Nhà của những người bạn”: xanh trầm `#285444`, nền ấm `#FAF6EE`, header phân quyền, menu trên màn hình nhỏ, thẻ và bảng theo template Thymeleaf. Bản preview tĩnh có dữ liệu khác nên không dùng trong demo nghiệp vụ.
5. Kiểm tra trước phụ kiện còn hàng, dịch vụ còn hoạt động, ngày làm việc tương lai và gói chưa bị dùng/hủy. Seed chạy lại giữ nguyên dữ liệu đã thao tác. Ngày T là ngày thực lúc seed; ngày H là ngày healthClock hiện trên trang giả lập.

## Tài khoản mẫu trong code

| Tài khoản | Mật khẩu demo | Vai trò và mục đích |
| --- | --- | --- |
| customer@petcare.vn | Customer@123 | CUSTOMER: đơn, hồ sơ Milo/Lucky, gói mẫu |
| staff@petcare.vn | Staff@123 | STAFF: xử lý đơn và lịch được giao |
| admin@petcare.vn | Admin@123 | ADMIN: giả lập ngày, dashboard, quản lý toàn bộ lịch |
| appointment.staff1@petcare.vn; appointment.staff2@petcare.vn | Staff@123 | STAFF phụ được seed cho lịch hẹn |
| loyalty.bronze@petcare.vn | Customer@123 | CUSTOMER: 490 điểm ban đầu, hạng Đồng, chưa có gói mẫu |
| loyalty.silver@petcare.vn | Customer@123 | CUSTOMER: 500 điểm, Bạc giảm 3%, có gói mẫu |
| loyalty.gold@petcare.vn | Customer@123 | CUSTOMER: 2.000 điểm, Vàng giảm 5%, có gói mẫu |
| loyalty.platinum@petcare.vn | Customer@123 | CUSTOMER: 5.000 điểm, nhãn giao diện “Kim cương”, giảm 10% |

Đây là mật khẩu công khai của seed demo, được BCrypt khi lưu; không phải thông tin đăng nhập production. Điểm/tỷ lệ trên theo application.properties mặc định; DB đã dùng có thể khác.

## Thao tác theo đúng thứ tự — tổng 10 phút

| Thời gian | Đường dẫn và tài khoản | Thao tác | Kết quả mong đợi và điểm nhấn |
| --- | --- | --- | --- |
| 00:00–01:40 — Mua hàng, State đơn | CUSTOMER `customer@petcare.vn`: `/products?group=ACCESSORY` → `/products/{id}` → `/cart` → `/checkout` → `/orders/{id}`. STAFF `staff@petcare.vn`: `/staff/orders/{id}` | Chọn **Phụ kiện 8**, số lượng 1, giá seed 800.000đ; nhập địa chỉ `12 Nguyễn Trãi, Thành phố Hồ Chí Minh`. Ghi ID đơn; nhân viên bấm Xác nhận → Giao hàng → Hoàn tất. | PENDING → CONFIRMED → SHIPPING → COMPLETED; kho giảm khi xác nhận; nút thay đổi theo State. Giá được chụp, đơn của customer mặc định chưa có giảm giá/voucher nên 800.000đ; hoàn tất nhận 80 điểm. Nhấn mạnh Factory tạo đúng lớp, đa hình giá, giao dịch và Observer. Nếu dữ liệu đã thay đổi, đọc giá/điểm thật thay vì khẳng định số mẫu. |
| 01:40–03:00 — Đặt lịch và quy tắc | CUSTOMER `loyalty.bronze@petcare.vn`: `/appointments/services` → `/appointments/slots?serviceId={id}&date={ngày}` → `/appointments/confirm?...` → `/appointments/{id}`; STAFF: `/staff/appointments?date={ngày}` | Chọn **Tắm tỉa 1**, ngày làm việc tương lai, nhân viên `staff@petcare.vn`, khung trống. Nhập tên thú cưng và loài; xác nhận. Mở lịch đúng ngày ở phiên STAFF, bấm Bắt đầu → Hoàn tất. | BOOKED → IN_PROGRESS → DONE. Không quá khứ, thứ Hai–thứ Bảy, 08:00–18:00, bước gợi ý 30 phút; trùng theo nhân viên bị từ chối, lịch sát nhau được phép. Khách chỉ hủy BOOKED trước ít nhất 2 giờ. Dùng bronze để tránh gói mẫu tự làm giá 0đ; giá seed Tắm tỉa 1 là 100.000đ, nhận 10 điểm và đạt 500 điểm/Bạc sau hoàn tất. |
| 03:00–05:00 — Hồ sơ tiêm và giả lập ngày | CUSTOMER `customer@petcare.vn`: `/my-pets` → `/my-pets/{id}` chọn **Milo bé (demo)**. ADMIN: `/admin/demo/health`; STAFF/ADMIN: `/staff/health` → `/staff/health/pets/{id}` | Xem lịch 6/8/12 tuần và mũi nhắc. Chọn một mũi còn SCHEDULED, ghi ngày hẹn D. ADMIN giả lập D−7 (nếu không trước H), sau đó D+1. STAFF xác nhận đã tiêm ở D+1. | D−7: DUE_SOON; D+1: OVERDUE; sau xác nhận: COMPLETED, không thể hoàn tất lần hai/đổi lịch mũi đã tiêm. Mũi hoàn tất mẫu giữ nguyên. Nói rõ chỉ healthClock tiến; lịch hẹn/gói không đổi ngày. MutableClock không cho lùi; nếu D−7 đã qua thì chọn mũi tương lai khác. |
| 05:00–06:00 — Quiz ghép đôi | CUSTOMER `customer@petcare.vn`: `/quiz` → `/quiz/results` → `/quiz/history` → `/quiz/history/{id}` | Nhập nhà nhỏ, có trẻ em, vận động 3/5, rảnh 2 giờ/ngày, ngân sách 3/5, không dị ứng, kinh nghiệm 2/5. Chạy “Cân bằng các tiêu chí”, rồi đổi “Ưu tiên ngân sách”. | Lọc trước khi chấm; tối đa ba kết quả kèm lý do/đóng góp điểm. Có seed Bông – chó Poodle, Mây – mèo Anh lông ngắn và các thú khác; không cam kết thứ hạng cố định vì hồ sơ/trạng thái có thể đổi. Cùng dữ liệu đầu vào, Strategy có thể thay thứ hạng; lịch sử giữ bản chụp. |
| 06:00–07:00 — Thẻ thành viên, giảm giá | CUSTOMER `loyalty.bronze@petcare.vn`: `/membership`, `/products?group=ACCESSORY`, `/cart` | Xem thẻ đã lên Bạc nhờ lịch vừa hoàn tất; đổi 100 điểm lấy voucher; thêm Phụ kiện 8 và xem giỏ. | totalPoints vẫn 500, currentPoints còn 400; hạng không giảm khi đổi điểm. Giá 800.000đ giảm hạng 24.000đ, voucher 10.000đ, còn 766.000đ nếu dữ liệu chưa đổi. Voucher dùng cho đơn kế tiếp, không cho lịch hẹn; xem giỏ chưa tiêu voucher, checkout mới chốt. |
| 07:00–09:00 — Gói chăm sóc | CUSTOMER `customer@petcare.vn`: `/subscriptions`; `/appointments/services` → xác nhận đặt lịch; `/subscriptions/{id}/renew` qua nút form. STAFF: `/staff/orders/{id}` | Xem ba gói mẫu ACTIVE/EXPIRED/CANCELLED; ghi lượt gói ACTIVE. Chọn dịch vụ nằm trong gói và ngày nằm trong hạn: trang xác nhận báo gói áp dụng/0đ; đặt lịch rồi kiểm tra giảm một lượt. Quay lại gói EXPIRED, bấm Gia hạn, nhân viên hoàn tất đơn qua ba bước. | Xem trước không trừ lượt; POST kiểm tra lại và trừ một lần. Hủy lịch hợp lệ hoàn lượt đúng một lần. Gia hạn tạo đơn mô phỏng, chỉ khi COMPLETED mới cộng quyền lợi. Gói tháng seed là 1 tháng/4 lượt; gia hạn dùng bản chụp ở OrderItem, không đọc lại tùy tiện danh mục đã sửa. Nêu giới hạn: chọn gói chung theo hạn sớm nhất; gói gắn riêng OwnedPet chưa dùng ở luồng đặt lịch. |
| 09:00–10:00 — Dashboard admin | ADMIN `admin@petcare.vn`: `/admin`; liên kết `/admin/members`, `/admin/subscriptions`, `/staff/health` | Trình bày doanh thu, sản phẩm bán chạy, phân bố hạng, cảnh báo và các liên kết quản trị. | Doanh thu tổng hợp đơn COMPLETED theo finalAmount và createdAt; dữ liệu seed có sáu tháng, không phải biểu đồ tĩnh. Số cảnh báo sức khỏe có thể đổi do bước giả lập. Menu/route theo vai trò; chốt bằng bốn tính chất OOP và ba pattern bắt buộc. |

## Dữ liệu và lưu ý để demo không bị gián đoạn

- `ProductDemoSeeder`: 6 Pet, 8 Accessory, 2 GroomingService, 2 VetService, 1 MonthlyCarePackage; MatchingDemoSeeder thêm 8 Pet và hồ sơ. OrderDemoSeeder tiêu kho Phụ kiện 1–6 trong đơn lịch sử, nên dùng **Phụ kiện 8** cho demo mua. Không giả định Phụ kiện 1 còn tồn kho.
- `PetHealthDemoSeeder`: Milo bé sinh T−6 tuần+3 ngày, Lucky lớn sinh T−5 năm, khóa `health-demo-milo`/`health-demo-lucky`; có bản ghi khám và mũi đã tiêm mẫu. Ngày được tính lúc seed, không tính lại mỗi lần khởi động.
- `SubscriptionDemoSeeder`: sourceKey `DEMO:SUBSCRIPTION:ACTIVE`, `EXPIRED`, `CANCELLED`, thêm MEMBER:silver/gold. Gói seed chọn theo ID nhỏ nhất, phủ các dịch vụ đặt được khi tập áp dụng còn rỗng.
- Phím/nút POST phải thao tác qua form để giữ CSRF. Không mở đường dẫn POST bằng thanh địa chỉ. STAFF chỉ thao tác lịch được giao; ADMIN có thể xử lý tất cả nếu chọn nhầm nhân viên.
- Không có reset demo bằng giao diện; MutableClock nằm trong bộ nhớ nhưng trạng thái tiêm đã cập nhật nằm trong DB. Khởi động lại không phục hồi dữ liệu demo ban đầu. Nếu cần trình diễn lặp, chuẩn bị database demo riêng trước buổi, không sửa dữ liệu trực tiếp lúc trình bày.

## SQL Server: các bước người dùng tự xác minh

1. Chạy bằng lệnh profile demo ở trên với biến môi trường đã cấu hình; xác nhận log kết nối SQL Server, không chạy H2/profile test để thay cho dev/demo.
2. Đăng nhập ba vai trò, kiểm tra tên `Quản trị viên`, `Phụ kiện`, `Milo bé (demo)` và địa chỉ có dấu sau lưu/tải lại. Trong SSMS kiểm tra cột chuỗi bằng truy vấn chỉ đọc:

```sql
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dbo'
  AND DATA_TYPE IN ('varchar', 'char', 'text', 'nvarchar', 'nchar', 'ntext')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

3. Kiểm tra PK/FK JOINED qua `products`, `service_items`, các bảng con; `orders.state_name`, `appointments.state_name`, `vaccination_records.state_name`, `subscriptions.state_name` lưu chữ. Đối chiếu bảng nối và ràng buộc trong [erd.md](erd.md).
4. Khởi động lại trên cùng DB: số seed không tăng và thao tác trước không bị reset. Với DB đã có một phần dữ liệu, xác nhận runner bỏ qua seed toàn bộ.
5. Dùng hai phiên đặt cùng nhân viên/khung giờ; chỉ một lần thành công. Thử hai lần dùng lượt cuối và hủy lặp: không âm lượt/không hoàn hai lần. Xác nhận khóa và giao dịch trên SQL Server thực tế, vì test H2 không thay thế kiểm tra hệ quản trị này.
6. Thử hồ sơ/thẻ/đơn không thuộc khách: bị chặn; thử form sửa sản phẩm đã cũ: báo xung đột, không ghi đè tồn kho/trạng thái bán. Dùng bản dữ liệu demo riêng cho thử đồng thời.

Nguồn chính: `config/*DemoSeeder.java`, `config/DemoDataRunner.java`, `controller/*Controller.java`, `service/AppointmentPolicy.java`, `service/LoyaltyService.java`, `service/SubscriptionService.java`, `templates/`, `static/css/petcare.css`; test: StageNineDemoTest, PetHealthDemoTest, SubscriptionIntegrationTest, LoyaltyIntegrationTest.
