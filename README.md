# PetCare Hub

Bài tập lớn Lập trình hướng đối tượng Java: cửa hàng thú cưng, đặt lịch dịch vụ, hồ sơ sức khỏe/tiêm phòng, quiz ghép đôi, thành viên thân thiết và gói chăm sóc tháng. Giao diện tiếng Việt dùng Thymeleaf và Bootstrap 5 CDN, không có framework JavaScript.

## Yêu cầu

- Java **21**, Maven 3.9+; kiểm tra bằng `java -version` và `mvn -version`.
- Spring Boot **3.5.16** cố định; Spring Data JPA/Hibernate, Spring Security, Lombok.
- SQL Server cho chạy ứng dụng/dev/demo; H2 **chỉ dùng trong kiểm thử**.
- SQL Server bật TCP/IP; tài khoản DB có quyền tạo/cập nhật bảng trong database của dự án.
- Internet ở lần tải Maven đầu tiên và để trình duyệt tải Bootstrap CDN.

## Tạo database và cấu hình

Trong SQL Server Management Studio, dùng tài khoản có quyền tạo database:

```sql
CREATE DATABASE petcare_hub;
GO
```

Trong PowerShell tại thư mục dự án:

```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=petcare_hub;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME='<tài khoản SQL Server>'
$env:DB_PASSWORD='<mật khẩu SQL Server của bạn>'
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Mở [PetCare Hub](http://localhost:8080). Mật khẩu DB lấy từ biến môi trường, mặc định để trống; không ghi mật khẩu thật vào mã nguồn hoặc commit. `trustServerCertificate=true` dành cho môi trường phát triển; khi triển khai thật dùng chứng chỉ hợp lệ và `trustServerCertificate=false`.

Chạy bình thường sau khi có dữ liệu:

```powershell
mvn spring-boot:run
```

Profile thường không tự tạo tài khoản có mật khẩu mẫu. Profile `test` chỉ phục vụ JUnit, không dùng để chạy demo. `ddl-auto=update` phục vụ bài tập, chưa thay thế một quy trình migration production.

## Dữ liệu và tài khoản demo

`DemoDataRunner` chỉ chạy với profile `demo`, kiểm tra các bảng gốc đều trống rồi tạo toàn bộ dữ liệu trong một giao dịch. Nếu database đã có dữ liệu, runner bỏ qua toàn bộ; không bổ sung lại bản ghi đã xóa, không đặt lại trạng thái/tồn kho/điểm. Muốn xem bộ demo GĐ9 đầy đủ trên dữ liệu cũ, tạo **database demo mới**, đổi `DB_URL` rồi chạy lại. Không cần và không nên xóa database đang sử dụng.

Bộ demo tạo 9 tài khoản, 27 sản phẩm, 10 đơn đủ 5 trạng thái (6 đơn hoàn tất trải qua 6 tháng), 5 lịch hẹn đủ 4 trạng thái, 2 thú sở hữu với 12 lịch tiêm đủ 4 trạng thái, 4 thẻ ở đủ 4 hạng, 5 gói đăng ký trong đó 3 gói hoạt động. Ngày được tính theo `Clock` khi khởi tạo. Lịch đang thực hiện nằm hôm nay nếu là ngày làm việc, hoặc ngày làm việc kế tiếp nếu cửa hàng nghỉ. Các khóa email/seedKey/sourceKey ổn định giúp nhận diện dữ liệu mẫu, không dựa vào tên hiển thị.

| Vai trò / hạng | Email | Mật khẩu mẫu công khai |
| --- | --- | --- |
| ADMIN | admin@petcare.vn | Admin@123 |
| STAFF | staff@petcare.vn | Staff@123 |
| STAFF bổ sung | appointment.staff1@petcare.vn, appointment.staff2@petcare.vn | Staff@123 |
| CUSTOMER | customer@petcare.vn | Customer@123 |
| Đồng | loyalty.bronze@petcare.vn | Customer@123 |
| Bạc | loyalty.silver@petcare.vn | Customer@123 |
| Vàng | loyalty.gold@petcare.vn | Customer@123 |
| Kim cương | loyalty.platinum@petcare.vn | Customer@123 |

Đây là mật khẩu minh họa, lưu trong DB bằng BCrypt. Không dùng các tài khoản/mật khẩu mẫu cho môi trường thật. Chỉ chạy một instance trong lần khởi tạo demo đầu tiên. Tài khoản CUSTOMER chính có đơn, hồ sơ thú và gói; khách Bạc/Vàng cũng có gói hoạt động. Tài khoản chính được mở thẻ Đồng khi truy cập chức năng thành viên; số thẻ trên dashboard có thể tăng sau thao tác này.

## Luồng sử dụng và dashboard

- Khách: danh mục, chi tiết sản phẩm, dịch vụ, quiz, đăng ký/đăng nhập.
- CUSTOMER: giỏ hàng/đơn, lịch hẹn, thú cưng, thẻ thành viên, gói đã mua, lịch sử quiz của mình.
- STAFF: quản lý sản phẩm/đơn/sức khỏe và lịch được giao.
- ADMIN: các trang STAFF, toàn bộ lịch hẹn, dashboard, quản lý thành viên và gói đang hoạt động; trong demo có giả lập ngày tiêm phòng.

Dashboard có bảng HTML doanh thu 12 tháng gần nhất, lịch hẹn hôm nay (trừ lịch hủy), số thú sắp đến hạn/quá hạn tiêm, top 5 sản phẩm và số thẻ theo hạng. Quy ước: doanh thu là `finalAmount` sau giảm giá của **đơn đã hoàn tất**, nhóm theo **tháng tạo đơn**, chưa tính thu từ lịch hẹn. Top 5 theo số lượng trong đơn hoàn tất toàn thời gian; khi bằng nhau xếp theo ID sản phẩm. Mỗi nhóm cảnh báo đếm thú riêng biệt; một thú có thể có cả mũi sắp đến hạn và quá hạn. Số thẻ theo ngưỡng tổng điểm hiện hành, không tính khách chưa mở thẻ.

Menu và liên kết trong trang hiển thị theo vai trò. [Bảng kiểm toàn bộ route và đường dẫn vào](docs/NAVIGATION-AUDIT.md) ghi rõ các GET, POST và trang lỗi.

## Kiểm thử và độ phủ

```powershell
mvn clean test
```

JUnit 5/Mockito và các kiểm thử tích hợp tự bật profile `test`, dùng H2 ở chế độ MSSQLServer, không cần tài khoản SQL Server. JaCoCo đã được chấp thuận bổ sung; báo cáo được tạo ngay trong pha `test`:

- `target/site/jacoco/index.html`: báo cáo HTML theo package/class/dòng.
- `target/site/jacoco/jacoco.csv` và `jacoco.xml`: dữ liệu máy đọc.
- `target/surefire-reports/`: kết quả JUnit.
- [Báo cáo GĐ9, độ phủ service/domain và giới hạn kiểm chứng](docs/STAGE9.md).

Kiểm thử gồm State/Strategy/Factory, giao dịch và tranh chấp dữ liệu, Clock giả ở các mốc thời gian, phân quyền/CSRF/IDOR, Unicode, demo lặp lại, truy vấn dashboard và duyệt liên kết HTML từ trang chủ theo từng vai trò. H2 không thay thế kiểm chứng SQL Server thật; các bước thủ công nằm trong báo cáo GĐ9.

## Pattern → file → lý do dùng

Đường dẫn dưới đây tính từ `src/main/java/com/petcare/`.

| Pattern | File / package chính | Lý do dùng | Kiểm thử |
| --- | --- | --- | --- |
| State — Order | `domain/order/OrderState.java`, `PendingState.java`, `ConfirmedState.java`, `ShippingState.java`, `CompletedState.java`, `CancelledState.java` | Mỗi trạng thái quyết định hành động được phép và hoàn kho khi hủy | `OrderStateTest`, `OrderServiceIntegrationTest` |
| State — Appointment | `domain/appointment/AppointmentState.java`, `BookedState.java`, `InProgressState.java`, `DoneState.java`, `CancelledState.java` | Ngăn chuyển bước lịch hẹn trái phép | `AppointmentStateTest`, `AppointmentWebTest` |
| State — VaccinationRecord | `domain/vaccination/VaccinationState.java`, `ScheduledState.java`, `DueSoonState.java`, `OverdueState.java`, `CompletedState.java` | Tách quy tắc thời gian, đổi lịch và hoàn tất mũi tiêm | `VaccinationStateTest`, `VaccinationSchedulerTest` |
| State — Subscription | `domain/subscription/SubscriptionState.java`, `ActiveState.java`, `ExpiredState.java`, `CancelledState.java` | Kiểm soát dùng lượt, hết hạn, hủy và gia hạn | `SubscriptionStateTest`, `SubscriptionIntegrationTest` |
| Strategy — Matching | `service/matching/MatchingStrategy.java`, `WeightedScoreStrategy.java`, `BudgetFirstStrategy.java` | Hoán đổi thuật toán ghép đôi mà không phân nhánh theo loại | `MatchingStrategyTest`, `MatchingServiceTest` |
| Strategy — Discount | `domain/discount/DiscountStrategy.java`, `BronzeDiscount.java`, `SilverDiscount.java`, `GoldDiscount.java`, `PlatinumDiscount.java` | Mỗi hạng cung cấp một chính sách giảm giá | `LoyaltyDomainTest`, `LoyaltyIntegrationTest` |
| Factory — Product | `domain/factory/ProductFactory.java` | Bảng hàm tạo đúng lớp sản phẩm, tránh switch theo loại | `ProductFactoryTest` |
| Policy / Adapter mua hàng | `domain/purchase/ProductPurchasePolicy.java`, `PurchasePolicies.java` | Tách quy tắc mua/trừ/hoàn tồn của từng sản phẩm | `CartPurchaseTest` |
| Observer — sự kiện Spring | `event/LoyaltyEventListener.java`, `SubscriptionEventListener.java`, `VaccinationStatusChangedListener.java` | Tích điểm/cấp gói/thông báo sau hành vi nghiệp vụ, giữ các module tách biệt | `LoyaltyIntegrationTest`, `SubscriptionIntegrationTest`, `VaccinationSchedulerTest` |

Bốn tính chất OOP: entity đóng gói trạng thái và không mở setter nghiệp vụ; `Product`/`ServiceItem` thể hiện kế thừa và trừu tượng; cách tính giá, policy, State và Strategy thể hiện đa hình. Các class pattern có Javadoc tiếng Việt.

## Unicode và nâng cấp database cũ

Hibernate bật `hibernate.use_nationalized_character_data=true` để tạo cột chuỗi bằng `nvarchar` trên SQL Server. `ddl-auto=update` **không bảo đảm** chuyển các cột `varchar` đã tồn tại. Sao lưu trước khi chuyển kiểu cột thủ công, giữ độ dài, NULL/NOT NULL và xử lý index/constraint phụ thuộc. Ví dụ:

```sql
ALTER TABLE dbo.users ALTER COLUMN full_name nvarchar(100) NOT NULL;
```

Đây không phải migration đầy đủ. Dữ liệu đã thành `?` không tự phục hồi khi đổi kiểu; cần bản sao lưu đúng hoặc nhập lại. Literal SQL tiếng Việt dùng tiền tố `N`, ví dụ `N'Nguyễn An'`; ứng dụng dùng bind tham số. Kiểm tra thông tin cột và lưu/tải lại tên có dấu theo [UNICODE-AUDIT.md](docs/UNICODE-AUDIT.md).

## Tài liệu các giai đoạn

[Kế hoạch](docs/PLAN.md) · [Mua hàng](docs/STAGE3B.md) · [Lịch hẹn](docs/STAGE4.md) · [Sức khỏe](docs/STAGE5B.md) · [Ghép đôi](docs/matching-algorithm.md) · [Thành viên](docs/GD7-MANUAL.md) · [Gói tháng](docs/STAGE8.md) · [GĐ9](docs/STAGE9.md).
