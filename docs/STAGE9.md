# GĐ9 — Dashboard, hoàn thiện và kiểm thử

Ngày kiểm chứng: 18/11/2026. Java 21.0.12.1, Spring Boot 3.5.16, Maven 3.9.16.

## Kết quả

- [x] Dashboard ADMIN: 12 tháng doanh thu, lịch hẹn hôm nay, thú sắp đến hạn/quá hạn tiêm, top 5, thẻ theo hạng. Repository làm SUM/COUNT/GROUP BY; service tạo DTO, bổ sung tháng/hạng rỗng; controller chỉ gọi service và trả view.
- [x] `GlobalExceptionHandler` dùng `@ControllerAdvice` dự phòng sau các handler nghiệp vụ: trạng thái không hợp lệ → 409, validate → 400, không tìm thấy → 404, không đủ quyền → 403, lỗi bất ngờ → 500. Lỗi phương thức/định dạng HTTP giữ 405/406/415. Không đưa exception message, stack trace, dữ liệu form/SQL lên trang lỗi hoặc log của handler chung.
- [x] Trang 404/403/500 tiếng Việt dùng chung layout và có đường về trang chủ. Handler cũ vẫn giữ flash/redirect phù hợp từng luồng.
- [x] `DemoDataRunner` profile demo: một giao dịch, chỉ tự tạo khi các bảng gốc đều trống. Các seeder cũ là thành phần được gọi theo thứ tự, không còn tự chạy qua ApplicationReadyEvent. Tài khoản mẫu không được tự tạo ở profile thường.
- [x] Bộ demo có đủ trạng thái và hạng; khóa ổn định; test chạy lại và test database có dữ liệu một phần không bổ sung bản ghi.
- [x] Rà soát 71 mapping controller, menu/trang chủ và liên kết/form theo vai trò; xem [NAVIGATION-AUDIT.md](NAVIGATION-AUDIT.md).
- [x] CSRF bật ở cả hai filter chain, kiểm thử truy cập chéo khách hàng bị chặn, DTO hiển thị không chứa mật khẩu/hash.
- [x] Javadoc tiếng Việt ở các class pattern; bỏ import/listener khởi động cũ không còn dùng. Không thấy switch/instanceof xử lý nghiệp vụ theo lớp sản phẩm.
- [x] Chuyển quyết định ưu đãi/gói ở xác nhận lịch hẹn sang service; controller còn nhận/validate input, chuẩn bị model và chọn view. Không controller nào dùng repository/EntityManager.
- [x] User/Product ghi ngày tạo qua listener với Clock, không còn `LocalDate.now()`/`LocalDateTime.now()` không tham số trong mã ứng dụng.
- [x] `mvn clean test` xanh, 470 tests, 0 failures, 0 errors, 0 skipped. JaCoCo do người dùng chấp thuận, tạo report trong pha test.
- [x] README đủ yêu cầu môi trường, tạo DB, biến môi trường, lệnh chạy, tài khoản demo và bảng pattern.
- [ ] Kiểm chứng với **SQL Server thật**: không có DB_URL/DB_USERNAME/DB_PASSWORD trong môi trường làm việc; cần thực hiện các bước bên dưới. Không tuyên bố H2 là kiểm chứng SQL Server.

## Quy ước thống kê và thiết kế

Doanh thu dùng giá đã chốt `finalAmount` của đơn COMPLETED, nhóm theo tháng **tạo đơn** vì mô hình hiện tại không lưu thời điểm hoàn tất. Cửa sổ gồm tháng hiện tại và 11 tháng trước, tháng trống là 0. Không gồm giá trị lịch hẹn, không giả lập đối soát thanh toán. Top 5 dùng tổng số lượng trên đơn COMPLETED toàn thời gian, giới hạn 5 tại truy vấn, hòa điểm theo ID tăng dần.

Lịch hôm nay theo Clock giờ Việt Nam, lấy `[00:00 hôm nay, 00:00 ngày mai)`, bỏ CANCELLED. Cảnh báo tiêm phòng theo `healthClock` và ngày hẹn thực tế, bỏ COMPLETED, đếm distinct thú: quá hạn `< hôm nay`; sắp đến hạn `[hôm nay, hôm nay + dueSoonDays]`. Vì vậy thống kê không lệ thuộc scheduler đã cập nhật cột State hay chưa. Một thú có thể xuất hiện ở cả hai nhóm. Trang danh sách tiêm phòng hiện vẫn lọc theo State được scheduler cập nhật; khi qua ngày trước lượt scheduler, danh sách có thể tạm khác số tổng hợp.

Thành viên đếm thẻ đã mở theo tổng điểm và các ngưỡng cấu hình hiện hành. Hạng không dựa vào số dư sau đổi voucher; khách chưa mở thẻ không được tính. Dashboard là giao dịch chỉ đọc, chưa thiết kế snapshot đồng thời tuyệt đối giữa các chỉ số.

Không thêm framework JS, không đổi package/class cũ và không nâng Spring Boot. Các thay đổi GĐ8 đã có trong workspace được giữ lại. H2 vẫn có scope test.

## Kiểm thử và độ phủ JaCoCo

Lệnh thực tế: `mvn -B -ntp clean test` (tương đương `mvn clean test`, thêm chế độ log gọn). HTML: `target/site/jacoco/index.html`; XML/CSV cùng thư mục; JUnit: `target/surefire-reports/`.

Bảng dưới tính tổng số covered/missed của tất cả class trong package; không lấy trung bình phần trăm class. Dòng và nhánh là hai chỉ số khác nhau, không xem phủ dòng cao là đã thử hết tổ hợp điều kiện.

| Phạm vi | Dòng (covered/tổng) | Nhánh (covered/tổng) |
| --- | --- | --- |
| `com.petcare.service` riêng | 565/572 — **98.78%** | 220/266 — **82.71%** |
| `com.petcare.service` và package con | 616/623 — **98.88%** | 247/296 — **83.45%** |
| `com.petcare.domain` riêng | 506/525 — **96.38%** | 207/310 — **66.77%** |
| `com.petcare.domain` và package con | 783/807 — **97.03%** | 291/410 — **70.98%** |

Chi tiết các package con:

| Package | Dòng | Nhánh |
| --- | --- | --- |
| `com.petcare.domain.appointment` | 40/40 — **100.00%** | 10/10 — **100.00%** |
| `com.petcare.domain.discount` | 14/14 — **100.00%** | 3/4 — **75.00%** |
| `com.petcare.domain.factory` | 18/18 — **100.00%** | Không có nhánh |
| `com.petcare.domain.loyalty` | 6/6 — **100.00%** | Không có nhánh |
| `com.petcare.domain.order` | 39/39 — **100.00%** | Không có nhánh |
| `com.petcare.domain.purchase` | 54/57 — **94.74%** | 20/26 — **76.92%** |
| `com.petcare.domain.subscription` | 35/37 — **94.59%** | 21/30 — **70.00%** |
| `com.petcare.domain.vaccination` | 71/71 — **100.00%** | 30/30 — **100.00%** |
| `com.petcare.service.matching` | 51/51 — **100.00%** | 27/30 — **90.00%** |

Các kiểm thử bổ sung:

- `DashboardIntegrationTest`: database trống, tháng không có dữ liệu, qua giao thừa theo giờ Việt Nam, biên đầu/cuối khoảng 12 tháng, chỉ tính đơn hoàn tất và giá sau giảm, top 5 hòa điểm, COUNT DISTINCT thú, biên ngày tiêm và lịch hẹn, các ngưỡng hạng.
- `StageNineDemoTest`: dữ liệu từ database H2 trống, mọi trạng thái/hạng, nhiều gói hoạt động, seed lặp không đổi, database không trống không backfill; render lỗi tiếng Việt và không lộ chi tiết; CSRF, IDOR và quyền dashboard; duyệt liên kết HTML theo 4 nhóm vai trò.
- Giữ toàn bộ hồi quy trước GĐ9: các State, hai nhóm Strategy, Factory, tranh chấp tồn kho/lịch/gói, tính nguyên tử giao dịch, snapshot quyền lợi/giá, Unicode và Clock giả.

## Các bước tự kiểm tra SQL Server

1. Tạo database mới bằng SSMS, ví dụ `CREATE DATABASE petcare_hub_gd9;`. Không xóa database đang dùng. Trỏ biến môi trường DB_URL đến database mới và đặt DB_USERNAME/DB_PASSWORD như README.
2. Chạy tại thư mục dự án:

   ```powershell
   mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
   ```

3. Đăng nhập `admin@petcare.vn / Admin@123`, từ menu **Quản trị** hoặc liên kết trang chủ mở dashboard. Kiểm tra 6 tháng có doanh thu, bảng top 5 có 5 dòng, thẻ có Đồng/Bạc/Vàng/Kim cương; các liên kết dẫn tới đúng danh sách. Dashboard doanh thu tổng cộng 6 đơn hoàn tất; trạng thái đơn còn có PENDING, CONFIRMED, SHIPPING, CANCELLED.
4. Trong SSMS chạy các truy vấn dưới. Kỳ vọng lúc vừa khởi tạo: 9 users, 27 products, 10 orders, 5 appointments, 2 owned_pets, 12 vaccination_records, 4 loyalty_accounts, 5 subscriptions (3 ACTIVE). Các con số có thể đổi sau khi thao tác web/scheduler chạy.

   ```sql
   SELECT N'users' AS bang, COUNT(*) AS so_luong FROM users
   UNION ALL SELECT N'products', COUNT(*) FROM products
   UNION ALL SELECT N'orders', COUNT(*) FROM orders
   UNION ALL SELECT N'appointments', COUNT(*) FROM appointments
   UNION ALL SELECT N'owned_pets', COUNT(*) FROM owned_pets
   UNION ALL SELECT N'vaccination_records', COUNT(*) FROM vaccination_records
   UNION ALL SELECT N'loyalty_accounts', COUNT(*) FROM loyalty_accounts
   UNION ALL SELECT N'subscriptions', COUNT(*) FROM subscriptions;
   SELECT state_name, COUNT(*) AS so_luong FROM orders GROUP BY state_name;
   SELECT state_name, COUNT(*) AS so_luong FROM vaccination_records GROUP BY state_name;
   SELECT state_name, COUNT(*) AS so_luong FROM subscriptions GROUP BY state_name;
   SELECT tier, COUNT(*) AS so_luong FROM loyalty_accounts GROUP BY tier;
   SELECT YEAR(created_at) AS nam, MONTH(created_at) AS thang, SUM(final_amount) AS doanh_thu
   FROM orders WHERE state_name = N'COMPLETED'
   GROUP BY YEAR(created_at), MONTH(created_at);
   ```

5. Kiểm tra **mọi cột chuỗi** và tiếng Việt (truy vấn thứ nhất phải không có dòng đối với schema mới):

   ```sql
   SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
   FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = N'dbo'
     AND DATA_TYPE IN (N'varchar', N'char', N'text', N'nchar', N'ntext');
   SELECT full_name FROM users WHERE email = N'admin@petcare.vn';
   SELECT name FROM products WHERE seed_key = N'matching-poodle';
   SELECT name FROM owned_pets WHERE seed_key = N'health-demo-lucky';
   ```

   Phải thấy `Quản trị viên`, `Bông – chó Poodle`, `Lucky lớn (demo)`. Qua form lưu tên `Lucky lớn đã sửa`, ghi chú `Đặt lịch khám Nguyễn An`, tải lại và khởi động lại để chắc chắn dấu còn nguyên. Với schema cũ xem mục migration Unicode trong README.
6. Ghi lại ID/số lượng; sửa tên thú và hủy một gói ACTIVE của `customer@petcare.vn`, dừng rồi chạy lại cùng lệnh demo. ID/số lượng, tên sửa và trạng thái hủy phải giữ nguyên. Seed không tự phục hồi bản ghi đã xóa. Tránh để nhiều instance cùng khởi tạo database rỗng.
7. CUSTOMER từ menu đi qua danh mục → giỏ → đặt hàng → chi tiết đơn; dịch vụ → khung giờ → xác nhận → lịch của tôi; thú của tôi → hồ sơ/sửa; thẻ → đổi voucher; gói → gia hạn. STAFF từ menu mở lịch làm việc, quản lý đơn/sản phẩm/sức khỏe. ADMIN mở quản lý thẻ/gói và giả lập ngày tiêm; kiểm tra mỗi trang có đường quay lại.
8. Dùng `loyalty.gold@petcare.vn` thử URL ID đơn, lịch và thú của khách chính: bị chặn, không thấy dữ liệu. STAFF không vào được `/admin/dashboard`. Bỏ token `_csrf` trong một POST phải bị 403. Mở URL không tồn tại thấy 404 tiếng Việt. Trang 500 đã có kiểm thử tự động, không có route lỗi cố ý trong ứng dụng.
9. Tắt profile demo bằng `mvn spring-boot:run`: liên kết giả lập ngày biến mất, dữ liệu hiện có vẫn còn, không tự tạo thêm tài khoản mẫu.

## Hạn chế còn lại

- Chưa chạy SQL Server thật, chưa kiểm tra hình ảnh giao diện trong trình duyệt trên SQL Server; kiểm thử tự động dùng H2 và render HTML bằng MockMvc.
- Demo dành cho một instance khởi tạo lần đầu. Chưa có khóa khởi tạo phân tán; nếu database có bất kỳ dữ liệu gốc, không bổ sung demo còn thiếu. Các hàm seeder con vẫn giữ API phục vụ kiểm thử và tương thích giai đoạn trước; luồng khởi động chỉ dùng runner chung.
- Đơn mẫu dùng State/policy tồn kho trực tiếp và không phát sự kiện tích điểm lịch sử; điểm của bốn thẻ được seed riêng để giữ đúng bốn hạng. Không phải dữ liệu kế toán thực tế.
- Doanh thu theo tháng tạo đơn, chưa có ngày hoàn tất/hoàn tiền và chưa gồm lịch hẹn. Dashboard chỉ đọc nhiều truy vấn, không khóa toàn bộ dữ liệu thành một snapshot tại cùng thời điểm.
- Branch coverage chưa 100%; xem report theo class để thấy các nhánh phòng vệ/cấu hình chưa chạy. Chưa thực hiện kiểm thử tải hay đánh giá bảo mật độc lập.
- Lỗi 500 chỉ log tên lớp exception để tránh lộ dữ liệu nhạy cảm; chưa có hệ thống theo dõi lỗi có mã đối chiếu. Bootstrap cần CDN; chưa bổ sung asset offline.
- Cấu hình `ddl-auto=update` và mật khẩu demo phục vụ bài tập; cần quy trình migration và quản lý tài khoản riêng trước triển khai thật.
