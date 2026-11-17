# GĐ7 — Thẻ thành viên

## Thiết kế và quy ước

- `LoyaltyAccount` giữ điểm hiện có, điểm tích lũy và hạng; quan hệ một-một với khách hàng, có `@Version` và CHECK số điểm không âm.
- `DiscountStrategy` có bốn lớp cụ thể và `DiscountStrategyFactory`. `apply(amount)` trả về tiền sau giảm theo hạng. Tỷ lệ, tên tiếng Việt, ngưỡng và quyền lợi nằm tại `petcare.loyalty.tiers.*` trong `application.properties`.
- Mặc định: Đồng 0 điểm/0%, Bạc 500/3%, Vàng 2.000/5%, Kim cương 5.000/10%. Giảm theo hạng làm tròn HALF_UP đến hai chữ số thập phân.
- Giá trị tiếng Việt trong `.properties` được viết bằng Unicode escape (`\uXXXX`) để Spring nạp đúng dấu. Khi sửa tỷ lệ, cập nhật cả mô tả quyền lợi tương ứng.
- Điểm nhận được = phần nguyên của `finalAmount / 10000`. Không cộng khi đặt, xác nhận, giao hàng, bắt đầu dịch vụ hoặc hủy. Số tiền nhỏ hơn 10.000đ vẫn ghi EARN 0 điểm để đánh dấu đã xử lý.
- Order/Appointment chỉ phát sự kiện hoàn tất, không gọi `LoyaltyService`. Observer đồng bộ, chạy **trong cùng giao dịch**, bắt buộc có giao dịch (`MANDATORY`). Lỗi cộng điểm rollback cả trạng thái và sổ điểm. Không dùng AFTER_COMMIT vì cần tính nguyên tử, chưa có outbox để phục hồi lỗi sau commit.
- Khóa User của khách trước khi tạo/khóa tài khoản; kiểm tra nguồn và ghi EARN dưới cùng khóa. `sourceKey` không null và UNIQUE (`EARN:ORDER:<id>` hoặc `EARN:APPOINTMENT:<id>`), tránh vấn đề UNIQUE với nhiều giá trị NULL của SQL Server. Phát lại event không tăng điểm; gọi lại chuyển trạng thái vẫn bị State từ chối như trước.
- Giảm giá được lưu tại thời điểm tạo. Thay đổi hạng/giá/cấu hình không đổi tiền của đơn và lịch đã lưu. Lịch hẹn trước GĐ7 có cột mới null được đọc là giảm 0 và thanh toán bằng giá cũ; không xóa dữ liệu cũ.
- Đổi 100 điểm lấy một voucher 10.000đ; mỗi khách giữ tối đa một voucher chờ dùng. Voucher tự áp dụng cho đơn hàng kế tiếp có tiền phải trả, sau giảm theo hạng, tối đa bằng tiền đơn. Không dùng cho lịch hẹn. **Giả định đơn giản hóa:** phần dư không hoàn, đơn bị hủy không hoàn voucher/điểm; giao dịch tạo đơn thất bại thì voucher được rollback. Quy ước này hiển thị ngay ở trang thẻ.
- REDEEM và ADJUST có giao dịch, khóa tài khoản và lịch sử. Trừ điểm không giảm `totalPoints`/hạng. Điều chỉnh dương tăng cả hai loại điểm; điều chỉnh âm chỉ trừ điểm hiện có. Lý do và email quản trị viên được lưu.
- Clock được inject cho giao dịch điểm và thời gian tạo đơn. Các lớp và package cũ được giữ; các test cũ chỉ bổ sung dọn sổ điểm trước khi xóa dữ liệu có khóa ngoại, và cấu hình Clock cho JPA slice.

## Sửa bốn mục review GĐ7

- Ngưỡng cấu hình hiện hành áp dụng ngay cho mọi tài khoản: báo giá, thẻ và danh sách đều tính hạng từ `totalPoints`. Cột `tier` được đồng bộ trong giao dịch cập nhật tài khoản; luồng đọc không ghi lại cột này để tránh khóa ghi. Đổi ngưỡng có thể tăng hoặc giảm hạng, nhưng tiêu điểm không đổi tổng tích lũy nên không làm giảm hạng. Không tính lại giảm giá của đơn/lịch đã tạo.
- Danh sách ADMIN chỉ hiển thị các thẻ đã mở, lọc bằng khoảng điểm ngay trong truy vấn repository, 20 thành viên/trang, mới nhất trước. Không tạo tài khoản khi xem danh sách. Thẻ được mở khi khách lần đầu vào trang thẻ, đặt đơn/lịch hoặc khi có giao dịch điểm; xem chi tiết một khách chưa mở thẻ chỉ hiển thị số dư 0, không ghi DB.
- Mã khách không tồn tại hoặc thuộc STAFF/ADMIN trả 404 khi xem/điều chỉnh thẻ. CUSTOMER vẫn không được vào trang quản trị hoặc đọc thẻ khách khác bằng mã truyền vào.
- Kiểm thử bổ sung giữ giao dịch đọc danh sách mở trong lúc luồng khác đặt đơn, hoàn tất và đổi điểm; đếm SQL để kiểm tra không có N+1/khóa ghi; kiểm tra hoàn tất/replay lịch hẹn đồng thời và đơn hàng + lịch hẹn cùng cộng điểm vào một tài khoản.

Thử thêm trên SQL Server thật bằng lệnh demo bên dưới:

1. Với khách đã có 600 điểm Bạc, tạo một đơn và một lịch hẹn rồi ghi lại tiền đã chốt. Đổi ngưỡng Bạc từ 500 lên 1.000 trong cấu hình, khởi động lại: thẻ, báo giá mới và bộ lọc Đồng phải thống nhất; đơn/lịch cũ giữ nguyên tiền. Khôi phục ngưỡng 500 khi thử xong.
2. ADMIN mở danh sách, lọc và chuyển trang; khách chưa mở thẻ không được tạo tài khoản chỉ vì thao tác này. Cùng lúc, dùng phiên khách khác đặt đơn/đổi điểm và phiên STAFF hoàn tất đơn để kiểm tra không bị chặn bởi khóa ghi từ danh sách.
3. ADMIN mở mã không tồn tại, mã STAFF và mã ADMIN: nhận 404; CUSTOMER mở đường dẫn quản trị của khách khác: nhận 403. Hai phiên nhân viên hoàn tất cùng lịch hẹn chỉ tạo một EARN; hoàn tất một đơn và một lịch cùng khách phải ghi đủ hai nguồn và đúng tổng điểm.

Các ca đồng thời tự động dùng H2; các bước trên cần xác nhận thêm trên SQL Server thật vì cơ chế khóa của hai DB khác nhau.

## Chạy demo trên SQL Server

Tạo database `petcare_hub` nếu chưa có, bật TCP/IP, dùng tài khoản SQL Server có quyền tạo/cập nhật bảng. Với database đã có, giữ nguyên dữ liệu; ứng dụng dùng `ddl-auto=update` như các giai đoạn trước.

Trong PowerShell tại `D:\petcare-hub` (Java 21):

```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=petcare_hub;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME='sa'
$env:DB_PASSWORD=Read-Host 'Mật khẩu SQL Server' -MaskInput
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Mở <http://localhost:8080>. H2 chỉ dùng khi chạy test, không dùng cho demo.

Tài khoản demo GĐ7 (mật khẩu mẫu `Customer@123`, lưu BCrypt):

| Email | Hạng ban đầu | Điểm ban đầu |
|---|---|---:|
| loyalty.bronze@petcare.vn | Đồng | 490 |
| loyalty.silver@petcare.vn | Bạc | 500 |
| loyalty.gold@petcare.vn | Vàng | 2000 |
| loyalty.platinum@petcare.vn | Kim cương | 5000 |

Seed theo email và `DEMO:LOYALTY:<hạng>`, không dựa vào tên. Nếu đổi ngưỡng cấu hình, seed khách Đồng ở dưới ngưỡng Bạc và các hạng khác tại ngưỡng tương ứng. Chạy lại không đặt lại điểm đã sử dụng. Các tài khoản quản lý cũ khi database ban đầu rỗng: `admin@petcare.vn / Admin@123`, `staff@petcare.vn / Staff@123`; nếu đã thay đổi mật khẩu thì dùng mật khẩu hiện tại.

## Tự thử trên web

1. **Điều hướng:** khách chưa đăng nhập và STAFF không thấy liên kết GĐ7. CUSTOMER thấy **Thẻ thành viên** ở cả menu và trang chủ; ADMIN thấy **Quản lý thành viên**. ADMIN mở danh sách, lọc từng hạng, bấm **Xem thẻ và điều chỉnh điểm**. Mọi trang mới đều có đường dẫn từ giao diện.
2. **Chuẩn bị đơn 100.000đ:** đăng nhập ADMIN/STAFF, vào **Quản lý sản phẩm**, tạo một phụ kiện giá 100.000đ, tồn kho ít nhất 2. Có thể dùng sản phẩm có sẵn; cần tổng tiền thanh toán tối thiểu 100.000đ để đủ 10 điểm lên Bạc từ 490.
3. **Đặt đơn:** đăng nhập `loyalty.bronze@petcare.vn`, mở **Thẻ thành viên**, kiểm tra 490 điểm, hạng Đồng, còn 10 điểm lên Bạc. Chưa đổi voucher. Vào danh mục, thêm phụ kiện 100.000đ vào giỏ, kiểm tra giảm 0đ, thanh toán 100.000đ; đặt hàng và ghi nhớ mã đơn.
4. **Hoàn tất đơn:** đăng xuất, đăng nhập ADMIN/STAFF, vào **Quản lý đơn hàng**, mở đúng mã đơn. Bấm lần lượt **Xác nhận → Giao hàng → Hoàn tất**. Điểm chỉ cộng ở bước cuối.
5. **Xem tăng điểm và lên hạng:** quay về khách Đồng, mở **Thẻ thành viên**: hiện 500 điểm hiện có và tích lũy, hạng Bạc, thêm đúng một dòng tích điểm +10 liên kết về đơn. Đơn cũ vẫn thanh toán 100.000đ; đơn mới 100.000đ được giảm 3.000đ, còn 97.000đ. Tải lại không tăng điểm thêm. Nếu tài khoản đã dùng trước đó, dùng khách mới và ADMIN điều chỉnh lên 490 điểm để thử lại; seed không tự đặt lại.
6. **Đổi điểm:** bấm đổi 100 điểm; số dư 500 → 400, tổng tích lũy vẫn 500 và hạng Bạc. Nút đổi tiếp bị vô hiệu khi còn voucher. Đơn mới 100.000đ giảm 3.000đ theo hạng và 10.000đ voucher, thanh toán 87.000đ. Khi hoàn thành nhận 8 điểm; đơn sau chỉ còn giảm theo hạng. Có thể thử đơn dưới 10.000đ để thấy thanh toán không âm.
7. **Lịch hẹn:** đăng nhập khách Vàng, vào **Đặt lịch dịch vụ**, chọn dịch vụ/nhân viên/giờ trống. Trang xác nhận hiển thị giảm 5%; sau đặt, trang chi tiết giữ tiền đã chốt. Đăng nhập ADMIN hoặc đúng nhân viên được phân công, vào **Lịch làm việc**, chọn đúng ngày, mở lịch, bấm **Bắt đầu → Hoàn tất**. Khách thấy điểm tăng bằng phần nguyên của tiền sau giảm/10.000 và đúng một giao dịch liên kết đến lịch.
8. **Điều chỉnh:** ADMIN mở thẻ từ danh sách, cộng/trừ điểm với lý do có dấu, kiểm tra lịch sử gồm lý do và email ADMIN. Thử bỏ lý do, trừ quá số dư hoặc nhập 0: bị từ chối, số dư giữ nguyên. STAFF/CUSTOMER truy cập trực tiếp đường dẫn quản trị nhận 403.
9. **Chốt giá:** tạo đơn/lịch khi khách ở Bạc; ADMIN cộng đủ điểm lên Vàng. Mở lại đơn/lịch cũ vẫn giữ tiền giảm 3%; đơn/lịch mới hưởng 5%.
10. **Seed và tiếng Việt:** ghi số lượng khách/giao dịch mẫu, dừng và chạy lại cùng lệnh demo hai lần. Bốn khách GĐ7 không tăng bản sao; điểm đã đổi/điều chỉnh giữ nguyên. Kiểm tra “Đồng”, “Bạc”, “Vàng”, “Kim cương”, tên khách, quyền lợi và lịch sử đủ dấu.
11. **Đồng thời trên SQL Server thật:** mở hai tab khách có đúng 100 điểm, gửi đổi điểm gần đồng thời. Chỉ một yêu cầu thành công, điểm còn 0 và chỉ một REDEEM. Thử hai tab gửi đặt hàng trên cùng giỏ khi có voucher: chỉ một đơn được tạo, voucher được trừ một lần. Đây là bước cần kiểm chứng trên DB thật ngoài các test H2.

## Kiểm chứng schema và dữ liệu SQL Server

Trong SSMS, truy vấn chỉ đọc sau khi khởi động demo:

```sql
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME IN ('loyalty_accounts', 'point_transactions')
  AND DATA_TYPE IN ('varchar','nvarchar','char','nchar');
-- Các cột chuỗi phải là nvarchar (hibernate.use_nationalized_character_data=true).

SELECT u.email, u.full_name, a.tier, a.total_points, a.current_points
FROM loyalty_accounts a JOIN users u ON u.id=a.customer_id
WHERE u.email LIKE 'loyalty.%@petcare.vn';

SELECT source_key, COUNT(*) AS copies FROM point_transactions
GROUP BY source_key HAVING COUNT(*) > 1;
-- Không có dòng trùng; kiểm tra cả unique index của source_key trong SSMS.

SELECT type, description, points, created_at FROM point_transactions
WHERE source_key LIKE 'DEMO:LOYALTY:%';
-- Bốn giao dịch seed, nội dung tiếng Việt còn nguyên dấu.
```

## Kiểm thử tự động

```powershell
mvn test
```

Bao gồm Strategy/rate/ngưỡng đúng biên, Clock cố định, Observer cho cả đơn/lịch, replay đồng thời, rollback hoàn tất và voucher, không âm, hạng không giảm khi đổi điểm, snapshot giá, preview giỏ/đặt lịch, quyền truy cập/CSRF, điều hướng menu/trang chủ bốn vai trò, và seed chạy lại không trùng. Kết quả SQL Server thật cần người vận hành kiểm tra bằng các bước trên; test tự động dùng H2 ở profile `test`.

Kết quả ngày 17/11/2026: **387 test, 0 thất bại, 0 lỗi, 0 bỏ qua — BUILD SUCCESS**, Java 21, Spring Boot 3.5.16. Chưa kiểm chứng trên SQL Server thật do chưa có thông tin đăng nhập.
