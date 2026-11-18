# Giai đoạn 8 — Gói chăm sóc theo tháng

## Quy tắc và phạm vi

- `Subscription` dùng State riêng: `ActiveState`, `ExpiredState`, `CancelledState`; tái sử dụng `InvalidStateTransitionException`. `Clock` được inject cho mọi quyết định ngày giờ.
- Gói bắt đầu vào ngày hoàn tất đơn, kết thúc ở `startDate.plusMonths(durationMonths).minusDays(1)` và dùng được hết ngày cuối. Lượt cuối chuyển ngay sang Expired. Scheduler chạy 01:10 hằng ngày, giờ Việt Nam; kiểm tra cả quá ngày và hết lượt.
- Đơn mới chỉ chứa **một loại MonthlyCarePackage**. Mỗi suất mua tạo một Subscription riêng với đủ số lượt và số tháng đã chốt. Đơn vẫn có thể kèm phụ kiện/thú cưng. Đơn cũ trước GĐ8 có nhiều loại gói vẫn hoàn tất được và cấp đủ từng dòng/đơn vị; không đổi State của Order.
- Dịch vụ được chọn bằng checkbox trong form sản phẩm, liên kết theo ID. Mô tả `includedServices` cũ vẫn được giữ. Dòng hàng chụp mã gói, số lượt, số tháng và dịch vụ ngay lúc đặt đơn mua/gia hạn; cấp quyền lợi từ bản chụp, không đọc cấu hình danh mục mới. Dòng cũ chưa có bản chụp dùng cấu hình hiện hành lúc hoàn tất, vì không thể khôi phục lịch sử. Bản chụp có danh sách dịch vụ rỗng bị từ chối cấp, không nhầm với dữ liệu cũ thiếu bản chụp.
- Ưu tiên gói Active còn lượt, có dịch vụ và còn hạn cả hôm đặt lẫn ngày hẹn: `endDate` tăng dần, rồi ID tăng dần. Không đủ điều kiện thì áp dụng giá và giảm hạng GĐ7 bình thường.
- `ownedPet` là tùy chọn. Gói mua/seed ở giai đoạn này để trống và dùng chung cho thú cưng của khách. Lịch GĐ4 chỉ nhập tên/loài, nên không tự suy diễn tên trùng thành cùng hồ sơ; gói gắn riêng `ownedPet` không tự được chọn trong form này.
- Lịch dùng gói có giá/finalAmount = 0, lưu ID Subscription và cờ đã hoàn. Trừ/hoàn cùng giao dịch tạo/hủy lịch; dùng khóa DB hiện có và `@Version`. Hủy hai lần không hoàn hai lượt. Hoàn lượt không làm sống lại gói đã hủy hoặc đã quá ngày.
- Gia hạn khi Expired hoặc trong 7 ngày cuối: tạo **đơn mô phỏng riêng**, giữ nguyên giỏ. Điều kiện được chốt tại lúc tạo đơn, tránh hoàn lượt làm kẹt đơn gia hạn đang chờ. Chỉ một đơn gia hạn chờ cho mỗi gói; hủy đơn giải phóng quyền yêu cầu lại. Không hủy gói trong khi còn đơn gia hạn chờ.
- Hoàn tất đơn gia hạn nối thêm thời hạn và cộng lượt nếu còn hạn; nếu đã quá ngày thì bắt đầu chu kỳ mới, bỏ lượt cũ chưa dùng. Gói Cancelled phải mua mới.
- Cấp/gia hạn qua `OrderCompletedEvent` đồng bộ trong cùng giao dịch; không gọi trực tiếp `LoyaltyService`. Giảm hạng/voucher qua `LoyaltyPricingService`, cộng điểm qua Observer GĐ7. Lịch 0đ không cộng điểm tiền dịch vụ.
- `subscriptions.source_key` UNIQUE nhận diện từng đơn vị: `ORDER:<orderId>:ITEM:<orderItemId>`; nếu số lượng > 1 thì thêm `:UNIT:<n>` (bắt đầu từ 1). Khóa DB trên đơn và kiểm tra sourceKey ngăn cấp trùng khi phát lại/đồng thời. Giữ nguyên bảng `subscription_grants` có PK `order_id`: một biên nhận ghi sau toàn bộ lô trong cùng giao dịch, FK trỏ Subscription đầu tiên; gia hạn vẫn cập nhật gói đích một lần.
- Giả định dữ liệu GĐ8 cũ đã ghi nguyên tử: biên nhận cũ hoặc khóa `ORDER:<id>` là dấu toàn bộ quyền lợi đơn đã cấp. Giữ nguyên các gói gộp cũ và các lịch đang tham chiếu, không chia/xóa/cấp lại khi phát lại event. Dữ liệu bị sửa tay dở dang cần đối soát riêng.
- Demo nhận diện ba Subscription bằng `DEMO:SUBSCRIPTION:ACTIVE`, `...:EXPIRED`, `...:CANCELLED`, thuộc `customer@petcare.vn`. Dùng gói danh mục có ID nhỏ nhất, không dò theo tên. Khởi động lại không nhân bản/đặt lại trạng thái. Nếu DB có sẵn nhưng thiếu khách demo hoặc gói danh mục, seed bỏ qua; dùng DB demo mới để có đủ mẫu.

## Chạy ứng dụng

Java 21, Maven; SQL Server đã có database `petcare_hub`. H2 chỉ dùng trong test.

```powershell
cd D:\petcare-hub
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=petcare_hub;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = Read-Host 'Tên đăng nhập SQL Server'
$credential = Get-Credential -UserName $env:DB_USERNAME -Message 'Thông tin SQL Server (không ghi vào mã nguồn)'
$env:DB_PASSWORD = $credential.GetNetworkCredential().Password
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Mở http://localhost:8080. Các tài khoản dưới đây là tài khoản minh họa do seed hiện có tạo, không phải thông tin kết nối SQL Server:

| Vai trò | Email | Mật khẩu demo |
|---|---|---|
| Khách | customer@petcare.vn | Customer@123 |
| Nhân viên | staff@petcare.vn | Staff@123 |
| Quản trị | admin@petcare.vn | Admin@123 |
| Khách hạng Bạc | loyalty.silver@petcare.vn | Customer@123 |

## Tự thử trên web

1. Vào **Gói của tôi** bằng khách demo: thấy đủ trạng thái, ngày hết hạn, số lượt, dịch vụ có dấu. Menu và trang chủ đều có link. Khách chưa đăng nhập/STAFF không thấy trang này; ADMIN có link **Gói đang hoạt động** riêng.
2. Để kiểm tra trọn luồng mua mới mà không lẫn gói seed, dùng khách hạng Bạc. Vào **Danh mục → Dịch vụ → Gói tháng 1 → Thêm vào giỏ**, số lượng 1; **Đặt hàng**, nhập địa chỉ mô phỏng. Kiểm tra giảm 3% theo hạng. Chưa hoàn tất đơn thì chưa có Subscription.
3. Đăng nhập nhân viên ở cửa sổ khác: **Quản lý đơn hàng → chi tiết đơn → Xác nhận → Giao hàng → Hoàn tất**. Trở lại khách: **Gói của tôi** có một gói mới; **Thành viên** có giao dịch cộng điểm tương ứng số tiền thực trả. Tải lại/nhấn hoàn tất lại không cấp gói hoặc điểm lần hai.
4. Trong **Gói của tôi**, ghi lại số lượt và tên dịch vụ. Bấm **Đặt lịch dùng gói**, chọn đúng dịch vụ, nhân viên và ngày còn trong hạn. Chọn giờ còn cách hiện tại ít nhất 2 giờ (ngày làm việc kế tiếp thường dễ thử hơn). Nhập thú cưng và xác nhận. Chi tiết lịch có **Giá lúc đặt = 0**, **Thanh toán = 0**, mã gói và “đã dùng 1 lượt”. Số lượt giảm 1.
5. Từ **Lịch hẹn của tôi**, mở lịch trên và **Hủy** trước mốc 2 giờ. Chi tiết hiển thị “đã hoàn 1 lượt”; **Gói của tôi** tăng lại đúng 1. Tải lại hoặc gửi hủy lần nữa không tăng thêm. Nhân viên cũng có thể hủy lịch BOOKED từ **Lịch làm việc**.
6. Đặt đủ lượt còn lại ở các khung trống khác nhau: gói chuyển **Đã hết hạn / hết lượt** khi còn 0. Lịch tiếp theo tính giá thường (vẫn giảm hạng). Hủy một lịch dùng gói khi gói còn trong ngày hiệu lực sẽ hoàn lượt và chuyển Active lại.
7. Với gói Expired hoặc trong 7 ngày cuối, bấm **Gia hạn (tạo đơn mô phỏng)**; kiểm tra đơn mới, giá giảm hạng, gói chưa cộng lượt. Nhân viên hoàn tất đơn: gói được gia hạn và cộng lượt một lần. Hủy đơn PENDING/CONFIRMED cho phép tạo đơn gia hạn lại. Gói Cancelled không có nút gia hạn.
8. ADMIN vào **Gói đang hoạt động**, chỉ thấy gói còn ngày/còn lượt. Đăng nhập STAFF/CUSTOMER thử URL admin phải bị từ chối. Khách khác không hủy/gia hạn được gói của người khác.
9. Nhân viên vào **Quản lý sản phẩm → sửa gói tháng**, chọn dịch vụ bằng checkbox. Gói mua mới phải chỉ dùng được các dịch vụ đã chọn. Gói cấp trước vẫn giữ danh sách cũ. Đổi tên dịch vụ vẫn dùng được theo ID.
10. Khởi động lại với profile demo hai lần: ba khóa seed không nhân bản, lượt đã dùng/trạng thái đã hủy không bị đặt lại, chữ “Gói tháng”, “Tắm tỉa”, “Khám thú y” vẫn đầy đủ dấu.

## Kiểm chứng trên SQL Server thật

Chưa tự chạy các bước này vì không có thông tin đăng nhập SQL Server. Test tự động dùng H2 chế độ MSSQLServer và kiểm tra metadata DDL SQL Server; không thay thế kiểm chứng khóa và schema trên SQL Server thật.

- Trên bản sao DB demo, khởi động với `ddl-auto=update`; kiểm tra tạo các bảng `subscriptions`, `subscription_grants`, hai bảng liên kết dịch vụ, các cột mới của `orders`/`appointments`. Kiểm tra `source_key` UNIQUE, `order_id` PK ở biên nhận, các cột chuỗi mới là `nvarchar`.
- Bản sửa hai mục review chỉ thêm các cột nullable `snapshot_package_id` (bigint/FK), `snapshot_included_uses`, `snapshot_duration_months` (int) vào `order_items` và bảng liên kết `order_item_snapshot_services`. Không đổi/xóa cột hoặc PK cũ, không thêm cột chuỗi; `source_key` giữ `nvarchar` theo cấu hình Hibernate. Với bản sao DB cũ, chạy `ddl-auto=update` và kiểm tra các dòng cũ vẫn có snapshot NULL và giá/số lượng không đổi.
- Hoàn tất một đơn cũ có hai loại gói, trong đó một dòng có số lượng 2: phải có ba Subscription đúng quyền lợi, một biên nhận lô, điểm cộng một lần. Hai cửa sổ cùng hoàn tất chỉ một thao tác State thành công; tải lại không cấp thêm. Đơn đã cấp bằng khóa `ORDER:<id>` phải giữ nguyên gói và lượt khi phát lại.
- Đặt đơn mua/gia hạn gói 4 lượt/1 tháng/dịch vụ A; trước hoàn tất, sửa danh mục thành 1 lượt/3 tháng/dịch vụ B. Sau hoàn tất phải nhận quyền lợi 4 lượt/1 tháng/A và giữ giá cũ. Lặp lại sửa danh mục sau cấp để xác nhận quyền lợi không đổi. Với dòng cũ snapshot NULL, quyền lợi lấy cấu hình hiện hành, giá đơn vẫn giữ giá đã chốt.
- Dùng hai tab đặt hai lịch khác khung giờ khi chỉ còn 1 lượt: chỉ một lịch được giá 0, lịch kia trả phí; không có lượt âm. Hủy cùng một lịch ở hai tab: chỉ hoàn một lượt.
- Xác nhận/hoàn tất cùng đơn ở hai tab: chỉ một Subscription/biên nhận và một giao dịch điểm. Lỗi một thao tác phải rollback trọn đơn/lượt/điểm, không để lịch miễn phí mà chưa trừ lượt.
- Kiểm chứng scheduler nhanh trên **DB demo riêng**: dừng ứng dụng, đặt `$env:PETCARE_SUBSCRIPTIONS_SCHEDULER_CRON = '*/10 * * * * *'`, chạy lại profile demo. Với một gói Active thử nghiệm, sửa riêng `start_date='2019-12-01'`, `end_date='2019-12-31'` trong SQL Server. Sau 10–20 giây kiểm tra cột `state_name='EXPIRED'`, rồi tải lại web. Không chỉ dựa vào nhãn giao diện vì view cũng nhận biết gói quá ngày trước lúc scheduler chạy. Sau thử nghiệm bỏ biến bằng `Remove-Item Env:PETCARE_SUBSCRIPTIONS_SCHEDULER_CRON` và khởi động lại để trở về lịch 01:10 hằng ngày.
- Ca đúng ngày hết hạn còn Active; ngày kế tiếp Expired; còn 0 lượt Expired được kiểm tra tự động bằng Clock cố định, không cần đổi đồng hồ máy.

## Kiểm thử tự động

```powershell
mvn test
```

Surefire nạp Mockito agent ngay khi JVM khởi động từ dependency Mockito đã có, không thêm thư viện. Cách này tránh self-attach qua named pipe bị chặn trên Windows.

Bộ test GĐ8 bao gồm bảng chuyển trạng thái, hết lượt, hoàn lượt một lần, rollback, cạnh tranh lượt cuối, cấp gói phát lại đồng thời, gia hạn, scheduler Clock giả, dịch vụ ngoài gói/ngoài hạn, phân quyền và link header/trang chủ, seed chạy lại và tiếng Việt.

Kết quả ngày 17/11/2026: `mvn -B -ntp test` **BUILD SUCCESS**, 428 test, 0 failures, 0 errors, 0 skipped (gồm 33 test GĐ8). Log: `target/stage8-verified-tests.log`. Các test cũ được giữ nguyên; cơ chế `CommerceLocks` được sửa để refresh trực tiếp dưới khóa, tránh nâng khóa trên entity có version cũ khi hai yêu cầu hủy cạnh tranh.

Bản sửa hai mục review đầu ngày 18/11/2026: `mvn -B -ntp test` **BUILD SUCCESS**, 438 test, 0 failures, 0 errors, 0 skipped. Log: `target/stage8-two-fixes-all-tests.log`. Bổ sung 10 ca kiểm thử cho đơn cũ nhiều dòng/đơn vị, hoàn tất/phát lại đồng thời, rollback cả lô, khóa cũ và khóa từng đơn vị, bản chụp mua/gia hạn, fallback khi thiếu bản chụp. Điều chỉnh ca số lượng nhiều suất để yêu cầu mỗi suất một Subscription. Không đổi State của Order, không thêm thư viện.

## Hai mục review còn lại: cấu hình dịch vụ và dự tính lịch hẹn

Các file thay đổi riêng trong lần sửa này:

| Nhóm | File |
|---|---|
| Kiểm tra dịch vụ và bán gói | `domain/MonthlyCarePackage.java`, `domain/purchase/PackagePurchasePolicy.java`, `domain/subscription/PackageCoverage.java` (mới), `domain/subscription/SubscriptionBenefits.java`, `domain/OrderItem.java` (Javadoc), `dto/ProductForm.java`, `dto/ProductView.java`, `service/ProductService.java` |
| Dự tính lịch hẹn | `dto/SubscriptionPreview.java` (mới), `service/SubscriptionService.java`, `service/AppointmentService.java`, `controller/AppointmentController.java` |
| Giao diện | `templates/products/form.html`, `templates/commerce/purchase.html`, `templates/appointments/confirm.html` |
| Kiểm thử | `PackageCoverageValidationTest.java` (mới), `SubscriptionIntegrationTest.java`, `ProductFlowTest.java`, `CartPurchaseTest.java`, `SubscriptionStateTest.java` |
| Tài liệu | `docs/STAGE8.md` |

Các đường dẫn Java tính từ `src/main/java/com/petcare`, giao diện từ `src/main/resources`, test từ `src/test/java/com/petcare`. Những thay đổi GĐ8 có sẵn khác trong working tree được giữ nguyên.

Kết quả sau hai mục còn lại ngày 18/11/2026: `mvn -B -ntp test` **BUILD SUCCESS**, **462 test, 0 failures, 0 errors, 0 skipped**. Log: `target/stage8-remaining-fixes-all-tests.log`. Thêm 24 ca kiểm thử; cập nhật fixture cũ để gói hợp lệ có dịch vụ, thay ca cho phép bản chụp rỗng bằng ca từ chối cấp và rollback. `git diff --check` không báo lỗi.

- Khi lưu gói, bắt buộc chọn ít nhất một dịch vụ; mọi ID gửi từ form phải tồn tại và là tắm tỉa/thú y đang hiển thị. Form hiển thị lỗi tiếng Việt. Gói cũ thiếu cấu hình vẫn được giữ nguyên trong DB nhưng tạm ngừng bán, có cảnh báo cho khách và hướng dẫn cấu hình cho STAFF/ADMIN.
- Chính sách mua kiểm tra lại dịch vụ khi thêm giỏ, checkout và tạo đơn gia hạn. Khi cấp Subscription, kiểm tra bản chụp quyền lợi của dòng hàng; đơn cũ chưa có bản chụp dùng cấu hình hiện hành như trước. Bản chụp rỗng bị từ chối, không tự thay bằng danh sách hiện hành để che dữ liệu thiếu. Giao dịch hoàn tất bị rollback, không cấp một phần hoặc cộng điểm dở dang; không thay đổi State của Order.
- Giả định dịch vụ hợp lệ để bán/cấp là dịch vụ đặt lịch đang hiển thị. Nếu tất cả dịch vụ trong cấu hình/bản chụp đã bị ẩn, phải khôi phục dịch vụ trước khi bán/cấp tiếp. Không sửa quyền lợi Subscription đã cấp. Không thêm cột/bảng hay thư viện trong lần sửa này.
- Trang xác nhận dùng cùng điều kiện chọn gói với POST: gói chung của đúng khách, còn lượt, còn hạn hôm nay và ngày hẹn, bao gồm dịch vụ; ưu tiên ngày hết hạn rồi ID tăng dần. GET chỉ đọc, không chuyển trạng thái hay trừ lượt. Có gói thì hiện mã Subscription, lượt còn lại và giá dự kiến 0đ; không có gói thì hiện giá/giảm hạng như trước. POST vẫn kiểm tra lại và trừ dưới khóa trong cùng giao dịch tạo lịch.

Tự kiểm tra trên SQL Server với lệnh profile demo ở trên:

1. STAFF/ADMIN vào **Quản lý sản phẩm → sửa gói**, bỏ chọn tất cả dịch vụ rồi lưu: nhận lỗi tiếng Việt. Chọn một dịch vụ hợp lệ và lưu thành công.
2. Với gói cũ chưa cấu hình sẵn trong DB, xem danh mục/chi tiết bằng khách: thấy **Gói tạm ngừng bán**, không có nút mua. STAFF/ADMIN thấy cảnh báo cấu hình. Cấu hình lại gói rồi mua bình thường. Đơn cũ chưa có bản chụp thiếu dịch vụ chỉ hoàn tất được sau khi cấu hình lại.
3. Khách có gói còn lượt chọn dịch vụ trong gói và khung giờ còn hạn: trang xác nhận hiện đúng mã gói, số lượt và **0đ**. Tải lại nhiều lần rồi mở **Gói của tôi** kiểm tra lượt không đổi; chỉ khi xác nhận đặt lịch mới trừ một lượt.
4. Chọn dịch vụ ngoài gói hoặc dùng khách không có gói/hết lượt: vẫn thấy giá và giảm hạng. Mở hai tab với gói còn một lượt, đặt một tab trước; xác nhận tab còn lại ở khung giờ khác phải tính tiền thường, không trừ âm.
