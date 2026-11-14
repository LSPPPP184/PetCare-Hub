# Rà soát Unicode — 14/11/2026

Phạm vi: mã nguồn, test, cấu hình và tài liệu trong repository; loại trừ `.git` và đầu ra build `target`. Không sửa database đang chạy.

## Mapping entity

Đã rà soát các entity: Accessory, Appointment, Cart, CartItem, GroomingService, HealthRecord, MonthlyCarePackage, Order, OrderItem, OwnedPet, Pet, Product, ServiceItem, User, VaccinationRecord, VetService.

Mọi `columnDefinition` tìm thấy:

| File dưới src/main/java/com/petcare/domain | Trường | Định nghĩa | Kết quả |
|---|---|---|---|
| Product.java | version | bigint default 0 | Trường số, không xung đột Unicode |
| OwnedPet.java | version | bigint default 0 | Trường số, không xung đột Unicode |

Không có mapping String ép `varchar`, `char` hoặc kiểu SQL cố định. Các `@Column(length)` chỉ quy định độ dài, không cần bỏ hoặc đổi. Bật nationalized character data ở application.properties; test metadata SQLServerDialect duyệt các thuộc tính String của mọi entity, kể cả thuộc tính kế thừa, xác nhận kiểu `nvarchar`. Kiểm tra riêng fullName giữ độ dài `nvarchar(100)`. Các enum/discriminator không phải thuộc tính Java String nhập tiếng Việt.

## SQL viết tay

Không có native query trong mã production. Các query repository là JPQL, dữ liệu tiếng Việt được truyền bằng tham số hoặc qua JPA.

Các file test có createNativeQuery (SQL không chứa literal tiếng Việt):

- AppointmentWebTest.java: đọc state_name của appointments.
- OrderServiceIntegrationTest.java: đọc state_name của orders.
- OrderSummaryRepositoryTest.java: cập nhật created_at bằng tham số.
- PetHealthDemoTest.java: đặt seed_key = null để mô phỏng dữ liệu cũ.
- PetHealthPersistenceTest.java: đọc state_name của vaccination_records.

SQL chứa tiếng Việt trong `docs/state-vaccination.md`: thông báo THROW và INSERT owned_pets, health_records, vaccination_records. Tất cả literal tiếng Việt đã có tiền tố `N`; không cần sửa. SQL trong README, docs/STAGE3A.md và docs/STAGE5B.md không có literal tiếng Việt cần chuyển đổi.

## Encoding

Đã dùng UTF-8 decoder ở chế độ báo lỗi trên toàn bộ file `.java`, `.html`, `.properties`: 206 file trước sửa, không có file không hợp lệ. Sau thêm UnicodePersistenceTest.java đã kiểm tra lại cả 207 file, tất cả hợp lệ. ASCII cũng là UTF-8 hợp lệ. Không cần chuyển encoding file nào; kiểm tra byte UTF-8 không thể khôi phục dữ liệu đã hỏng trong database.

pom.xml trước sửa chưa khai báo trực tiếp `project.build.sourceEncoding`. Nay khai báo rõ `project.build.sourceEncoding=UTF-8` và `project.reporting.outputEncoding=UTF-8`, không phụ thuộc encoding mặc định máy build. Không thêm thư viện; không đổi cấu hình H2/profile test.

## Kiểm thử

UnicodePersistenceTest lưu rồi flush/clear persistence context và đọc lại chính xác `Lucky lớn`, `Tuấn Đạt`, `Đặt hàng` trên H2. Ca riêng dựng metadata bằng SQLServerDialect và cấu hình nationalized thực tế để tránh kết luận sai chỉ vì H2 vốn hỗ trợ Unicode. Không kết nối SQL Server trong test này; bước kiểm tra SQL Server thật được ghi ở README, mục “Khi nâng cấp từ bản cũ”.

Kết quả `mvn -B -ntp test`: BUILD SUCCESS, 308 test, 0 failure, 0 error, 0 skipped. Log: `target/unicode-test.log`. Không thay đổi kết nối H2 hoặc thêm dependency. `git diff --check` không phát hiện lỗi khoảng trắng.
