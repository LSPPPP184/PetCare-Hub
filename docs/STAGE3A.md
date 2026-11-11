# Giai đoạn 3A — Backend giỏ hàng và đơn hàng

## Phạm vi và quy tắc

- Bổ sung domain, repository, service và test. Không có endpoint hoặc trang mới. Bản sửa review chỉ điều chỉnh luồng sửa sản phẩm GĐ2 để phối hợp với đơn hàng.
- Cart duy nhất cho mỗi CUSTOMER. Thêm cùng phụ kiện sẽ cộng số lượng; cập nhật số lượng phải dương. Pet luôn có quantity=1 và phải AVAILABLE. Sản phẩm đã ẩn không thể thêm/đặt/xác nhận.
- Grooming và thú y cần lịch hẹn nên bị từ chối ở giỏ. Gói tháng được mua, nhưng chưa cấp quyền lợi subscription (GĐ8).
- `placeOrderFromCart(customerId, shippingAddress)` chụp giá đa hình thành unitPrice, tính tổng, lưu PENDING và xóa các dòng giỏ trong cùng transaction. Chưa giảm giá: discountAmount=0, finalAmount=totalAmount. Tiền dùng BigDecimal với scale 2, precision tối đa 19; không tự làm tròn.
- PENDING chưa giữ hàng. `confirm` kiểm tra lại tồn kho dưới khóa, trừ phụ kiện và đặt Pet=SOLD ngay. `cancel` từ CONFIRMED hoàn đúng số lượng và đặt Pet=AVAILABLE; từ PENDING không hoàn kho. Không hủy khi đã giao/hoàn thành. Các thao tác lặp bị từ chối, không trừ/hoàn lần hai.
- State chỉ lưu enum `stateName` trong bảng orders. `getState()` dựng lại đối tượng bất biến qua OrderStateFactory. Bảng Markdown đầy đủ nằm trong Javadoc của OrderState.
- Service kiểm tra người đăng nhập từ SecurityContext và user trong DB: chỉ CUSTOMER quản lý giỏ của mình; STAFF/ADMIN confirm/ship/complete; chủ đơn hoặc STAFF/ADMIN cancel trong trạng thái hợp lệ.
- Khóa user để tuần tự hóa giỏ/checkout; khóa order để bảo vệ chuyển trạng thái; khóa product theo ID tăng dần và refresh dữ liệu trước khi kiểm tra tồn kho. Ngoại lệ làm rollback cả trạng thái, tồn kho và giỏ.
- Adapter trong `domain.purchase` dùng lại API `toForm/update` của sản phẩm. Không có if/switch phân loại sản phẩm ở CartService/OrderService.
- Product có `@Version`; form sửa gửi version và được đối chiếu sau khi lấy khóa `CommerceLocks`. Form cũ, thiếu version hoặc version không hợp lệ không được lưu và báo “Dữ liệu đã được thay đổi, vui lòng tải lại trang”. Cột version mặc định 0 cho dữ liệu hiện có.
- Chọn cách tối thiểu: chỉ nhập tồn kho/trạng thái Pet khi tạo sản phẩm. Form sửa cập nhật đa hình qua `updateCatalog`, bỏ qua tồn kho/trạng thái dù request tự gửi các trường này. Sau khi tạo, các trường đó chỉ đổi qua confirm/cancel; không bổ sung thao tác điều chỉnh tồn kho riêng.

## Bảng chuyển trạng thái

| Trạng thái | confirm | ship | complete | cancel |
|---|---|---|---|---|
| PENDING | CONFIRMED | Lỗi | Lỗi | CANCELLED, không hoàn kho |
| CONFIRMED | Lỗi | SHIPPING | Lỗi | CANCELLED, hoàn kho |
| SHIPPING | Lỗi | Lỗi | COMPLETED | Lỗi |
| COMPLETED | Lỗi | Lỗi | Lỗi | Lỗi |
| CANCELLED | Lỗi | Lỗi | Lỗi | Lỗi |

## Chạy test

```powershell
mvn test
mvn test '-Dtest=OrderStateTest,CartPurchaseTest,OrderServiceIntegrationTest'
```

Test dùng H2 riêng cho giao dịch thương mại, không cần SQL Server. Có kiểm tra toàn bộ 20 ô bảng State, proxy/đọc lại DB, snapshot giá, rollback, quyền sở hữu, mua Pet đã bán, xác nhận đồng thời và checkout đồng thời. Thời hạn Future chỉ để phát hiện treo khóa; không dùng sleep để giả định thứ tự chạy.

Test sửa review kiểm tra form cũ sau confirm/cancel; form có version mới vẫn không ghi tồn kho/trạng thái; sửa tên/giá thành công cho mọi loại sản phẩm. Test rollback hoàn kho gây tràn số nguyên ở dòng sau, kiểm chứng dòng đầu đã được hoàn và flush xuống DB, rồi đọc lại trong transaction mới để xác nhận tồn kho, trạng thái đơn và version đều trở về trước khi hủy.

## Tự kiểm chứng với SQL Server

Chưa xác nhận trên SQL Server thật vì không có thông tin đăng nhập. Dùng database phát triển riêng, cấu hình DB_URL, DB_USERNAME, DB_PASSWORD trong môi trường theo README; không ghi mật khẩu vào repo.

1. Khởi động ứng dụng:

   ```powershell
   mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
   ```

2. Kiểm tra log kết nối, tạo khóa ngoại và bảng mới không có lỗi. Trong SSMS chạy:

   ```sql
   SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_NAME IN ('carts', 'cart_items', 'orders', 'order_items');
   SELECT COLUMN_NAME, DATA_TYPE, NUMERIC_PRECISION, NUMERIC_SCALE
   FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_NAME IN ('orders', 'order_items');
   ```

   Cần có bốn bảng; các cột tiền là decimal/numeric(19,2); orders lưu state_name dạng chuỗi.

3. Mở `/products`, đăng nhập ba tài khoản demo và xác nhận màn hình GĐ1/GĐ2 vẫn hoạt động. GĐ3A chưa có UI/API nên chưa thể thao tác đơn hàng bằng trình duyệt.
4. Để kiểm chứng nghiệp vụ và khóa trên SQL Server, dùng một cấu hình integration test riêng trỏ đến database thử nghiệm, gọi các service với SecurityContext của customer/staff như OrderServiceIntegrationTest. Không đổi datasource của bộ test H2 sang database có dữ liệu cần giữ (test có dọn dữ liệu). Kiểm tra: confirm trừ kho/Pet=SOLD; cancel hoàn kho; thiếu hàng rollback; hai xác nhận đồng thời chỉ một đơn mua được Pet. Đây là phần còn cần chạy trên SQL Server, kết quả H2 không thay thế được.

5. Với database đã có sản phẩm, kiểm tra cột `products.version` được thêm và không có giá trị NULL. Mở cùng sản phẩm trên hai tab quản lý: lưu tên/giá ở tab thứ nhất, lưu tab cũ phải nhận thông báo tải lại và dữ liệu không bị ghi đè. Form sửa không còn trường nhập tồn kho/trạng thái Pet.
6. Khi kiểm thử service trên SQL Server, giữ form cũ trước confirm rồi thử lưu sau confirm: tồn kho không tăng lại, Pet vẫn SOLD. Lặp lại sau cancel và kiểm tra version tăng. Chạy thêm tình huống hoàn kho thất bại ở dòng sau theo test `failedSecondRestorationRollsBackFirstRestorationAndOrderState`; toàn bộ thay đổi phải rollback.
