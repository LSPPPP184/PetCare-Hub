# GĐ5B — Giao diện sức khỏe và demo ngày giả

## Phạm vi

- CUSTOMER: danh sách thú cưng, thêm hồ sơ và sinh lịch mẫu trong cùng giao dịch, sửa thông tin mô tả, hồ sơ với timeline tiêm phòng và lịch sử khám, dời lịch của thú mình.
- STAFF/ADMIN: xem hồ sơ mọi thú, thêm bản ghi khám, xem/lọc lịch tiêm toàn cửa hàng, xác nhận ngày đã tiêm và dời lịch.
- Badge: Scheduled xám, DueSoon vàng, Overdue đỏ, Completed xanh. Quyền thao tác lấy từ `canComplete()` / `canReschedule()`; service gọi domain để kiểm tra lại khi POST.
- ADMIN trong profile `demo`: đặt ngày cho `MutableClock`, chạy scheduler và lưu trạng thái mới ngay. Ngoài demo không có bean MutableClock hoặc endpoint giả lập.
- Header và trang chủ có liên kết theo vai trò tới danh sách, thêm thú, tổng hợp, bộ lọc sắp đến hạn/quá hạn, hồ sơ cửa hàng và giả lập. Danh sách dẫn tới hồ sơ/sửa/thêm bản ghi; hồ sơ có các liên kết tương ứng và đường quay lại.

Không thay đổi logic State, template hoặc scheduler của GĐ5A; scheduler chỉ đổi cách inject Clock sức khỏe. Không thêm thư viện, không đổi Spring Boot hoặc package hiện có.

## Quy ước nghiệp vụ

1. Giữ quy tắc GĐ5A: chỉ STAFF/ADMIN xác nhận “Đã tiêm”; CUSTOMER chỉ dời lịch thú mình. Ngày đã tiêm nằm từ ngày sinh tới ngày hiện tại của Clock.
2. Form sửa chỉ cập nhật tên, giống, giới tính, ghi chú. Loài và ngày sinh là căn cứ lịch tiêm nên được giữ cố định; POST giả mạo cũng bị từ chối. Chủ thú cưng luôn lấy từ phiên đăng nhập.
3. Dời lịch trên web nhận ngày hôm nay hoặc tương lai. Domain 5A vẫn giữ khả năng nhập ngày quá khứ; không sửa quy tắc State.
4. Ngày khám từ ngày sinh tới hôm nay. Cân nặng tùy chọn, dương, tối đa 99999,99 kg với hai chữ số thập phân.
5. Form sửa có version; dời lịch kiểm tra version của mũi. Các thao tác ghi khóa OwnedPet trước khi thay đổi và chạy trong transaction. OwnedPet có `@Version` với mặc định DB bằng 0 để bổ sung vào bảng đã có dữ liệu. VaccinationRecord giữ cơ chế version cũ chống ghi đè với scheduler.
6. Demo dùng `@Qualifier("healthClock")` riêng cho sức khỏe/tiêm phòng và dữ liệu mẫu sức khỏe. Clock mặc định của lịch hẹn và module khác vẫn là Clock thật. Ngoài demo, healthClock trỏ tới chính Clock thật. Bean healthClock không là ứng viên inject mặc định (`defaultCandidate=false`), nên không chiếm Clock của module khác hoặc Clock cố định của test.
7. Clock sức khỏe chỉ tiến vì State không tự đảo ngược DueSoon/Overdue. Chọn ngày thật hiện tại lấy đầy đủ giờ thật từ Clock nguồn; không đưa về 00:00 và không lùi thời điểm đã xác nhận. Chọn ngày tương lai mới lấy 00:00 theo múi giờ Việt Nam; chọn lại ngày tương lai đang giả lập giữ nguyên thời điểm. Đã tiến sang tương lai thì không được quay về ngày thật trước đó. Khởi động lại Clock bắt đầu từ giờ thật, nhưng ngày/trạng thái đã ghi vào DB vẫn được giữ.
8. Mỗi lượt giả lập được đồng bộ trên trạng thái dùng chung của MutableClock. Ngày đề xuất chỉ có hiệu lực trong luồng chạy scheduler, kể cả các view múi giờ của Clock. Luồng khác đọc thời điểm đã xác nhận. Scheduler commit giao dịch riêng thành công mới công bố ngày mới; lỗi lưu hoặc xung đột version bỏ ngày tạm, giữ nguyên Clock cũ và trả thông báo tiếng Việt. Service tạm ngừng transaction của bên gọi để không công bố Clock trước một commit còn treo.
9. Seed theo ngày khởi động: `Milo bé (demo)` gần 6 tuần tuổi và `Lucky lớn (demo)` 5 tuổi cho `customer@petcare.vn`; mỗi con có bản ghi khám và mũi đã tiêm minh họa. Milo có DueSoon/Scheduled; Lucky có Overdue/Scheduled; các mũi minh họa Completed màu xanh. Nhận diện bằng `seed_key` ổn định (`health-demo-milo`, `health-demo-lucky`) và chủ, không dùng tên. Giao dịch seed khóa chủ để chống tạo trùng đồng thời. Form không nhận hoặc sửa seedKey.
10. Nâng cấp dữ liệu demo 5B cũ chưa có seedKey: tìm theo chủ, loài và mũi cố định `Mũi đã tiêm (demo)`; gán khóa cho hồ sơ cũ, không đổi tên hoặc tạo thêm lịch/khám. Nếu đã có nhiều bản sao cùng loài từ lỗi cũ, dừng với thông báo để người dùng xác định hồ sơ gốc; không tự đoán hoặc xóa dữ liệu.
11. Chó/mèo có lịch mẫu; loài khác chưa có. Đây là dữ liệu minh họa, không phải tư vấn thú y.

## Chạy và tự thử theo thứ tự

Yêu cầu Java 21, Maven, SQL Server với TCP/IP và tài khoản được tạo/cập nhật bảng. H2 chỉ dành cho test. Nên dùng database demo riêng để ngày giả không tác động dữ liệu sử dụng thật.

Tạo database demo trong SQL Server Management Studio:

```sql
CREATE DATABASE petcare_hub_demo;
GO
```

Chạy tại thư mục dự án trong PowerShell; nhập mật khẩu qua lời nhắc, không ghi vào file cấu hình:

```powershell
cd D:\petcare-hub
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=petcare_hub_demo;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME=Read-Host 'Tên đăng nhập SQL Server'
$env:DB_PASSWORD=Read-Host 'Mật khẩu SQL Server' -MaskInput
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Nếu Windows PowerShell cũ chưa hỗ trợ `-MaskInput`, dùng PowerShell 7 hoặc thiết lập DB_PASSWORD bằng cơ chế biến môi trường của máy. Không đưa mật khẩu thật vào source.

1. Mở <http://localhost:8080>. Khi chưa đăng nhập, kiểm tra header và trang chủ không có các liên kết quản lý sức khỏe/giả lập.
2. Đăng nhập `customer@petcare.vn` / `Customer@123` (tài khoản mẫu chỉ được tạo khi bảng users trống). Chọn **Thú cưng của tôi**. Kiểm tra có Milo bé và Lucky lớn; mở từng hồ sơ để thấy đủ bốn màu, thông tin và lịch sử khám.
3. Từ menu/trang chủ chọn **Thêm thú cưng**, tạo một chó hoặc mèo với ngày sinh không ở tương lai. Sau lưu, hồ sơ có lịch mẫu. Với thú từ 4 tuổi trở lên, chỉ có mũi nhắc gần nhất đã tới hạn và các mũi tương lai theo quy tắc 5A.
4. Chọn **Sửa thông tin**, đổi tên/ghi chú và lưu. Kiểm tra lịch tiêm không nhân đôi; loài/ngày sinh giữ nguyên. Mở form sửa trong hai tab; lưu tab đầu rồi lưu tab còn lại: lần sau phải báo dữ liệu đã thay đổi.
5. Trên hồ sơ, chọn mũi chưa hoàn tất, nhập ngày tương lai và bấm **Dời lịch**. Kiểm tra ngày hẹn/badge cập nhật. Mũi xanh không có nút dời lịch. CUSTOMER không có nút xác nhận đã tiêm hoặc thêm bản ghi khám.
6. Đăng xuất, đăng ký một CUSTOMER khác. Danh sách phải trống; thử URL hồ sơ/sửa của customer đầu tiên phải nhận 403. Quay lại tài khoản mẫu khi cần.
7. Đăng nhập `staff@petcare.vn` / `Staff@123`. Chọn **Theo dõi tiêm phòng**: mặc định thấy sắp đến hạn và quá hạn của toàn cửa hàng. Dùng bộ lọc lần lượt bốn trạng thái; kiểm tra các liên kết **Sắp đến hạn tiêm**, **Quá hạn tiêm** từ cả menu và trang chủ.
8. Chọn **Hồ sơ sức khỏe cửa hàng** → một thú → **Thêm bản ghi khám**. Nhập ngày hôm nay, loại khám, kết quả, cân nặng và bác sĩ; lưu và kiểm tra lịch sử. Ngày tương lai/trước ngày sinh phải bị từ chối.
9. Trên hồ sơ, chọn một mũi chưa hoàn tất, nhập ngày đã tiêm không ở tương lai, bấm **Đã tiêm**. Badge chuyển xanh; không còn nút hoàn tất/dời lịch của mũi đó. Với mũi nhắc hằng năm, mũi kế tiếp không bị tạo trùng.
10. Đăng nhập `admin@petcare.vn` / `Admin@123`. Mở hồ sơ **Milo bé (demo)**, ghi lại ngày hẹn **D** của một mũi xám chưa thao tác. Chọn mũi ở tương lai trên 7 ngày; không dùng mũi đã hoàn tất ở bước 9.
11. Chọn **Giả lập ngày tiêm phòng** từ menu/trang chủ, nhập **D − 7 ngày**, bấm **Giả lập ngày**. Mở lại hồ sơ: mũi chuyển vàng. Đặt **D**: vẫn vàng. Đặt **D + 1 ngày**: chuyển đỏ và xuất hiện trong bộ lọc Quá hạn. Mũi xanh vẫn xanh. Nhập ngày lùi phải bị từ chối.
12. STAFF/CUSTOMER không được thấy liên kết giả lập; truy cập trực tiếp hoặc POST vào `/admin/demo/health` phải bị chặn. Dừng ứng dụng và chạy `mvn spring-boot:run` (không profile demo): ADMIN cũng không còn liên kết, endpoint giả lập trả 404.

## Kiểm chứng SQL Server

MockMvc/JPA được kiểm tra trên H2 ở profile test; cần thực hiện các bước web trên SQL Server thật để xác nhận driver, quyền DDL và khóa trên môi trường của bạn.

- Nếu nâng cấp database từ 5A có sẵn thú cưng: kiểm tra log khởi động không có lỗi thêm cột `owned_pets.version`; sửa được hồ sơ cũ. Cột mới có mặc định 0.
- Sau thao tác, chạy truy vấn chỉ đọc trong SSMS:

```sql
SELECT id, name, birth_date, version, seed_key FROM owned_pets ORDER BY id;
SELECT owned_pet_id, vaccine_name, scheduled_date, administered_date, state_name, version
FROM vaccination_records ORDER BY owned_pet_id, scheduled_date;
SELECT owned_pet_id, record_date, type, description, weight, vet_name
FROM health_records ORDER BY owned_pet_id, record_date;
```

- Đổi tên hai thú demo rồi khởi động lại với cùng database: ID thú, khóa seed, số lịch và số bản ghi khám không đổi. Nếu nâng cấp dữ liệu cũ, kiểm tra cột `seed_key` được thêm và hai hồ sơ cũ được gán đúng khóa mà không nhân đôi. Clock sức khỏe bắt đầu lại theo ngày thật; State quá hạn/hoàn tất không tự lùi.
- Trước khi giả lập tương lai, chọn lại ngày thật hiện tại: Clock sức khỏe không về 00:00. Ghi nhận các khung đặt lịch và quyền hủy một lịch sát mốc hai tiếng; sau giả lập sức khỏe sang tương lai, các quy tắc lịch hẹn vẫn tính theo thời gian thật. Test tự động dùng Clock cố định để kiểm tra chính xác mốc 11:59/12:00 khi giờ thật là 10:00.
- Hai tab cùng dời một mũi phải chỉ lưu một yêu cầu có version ban đầu. Hai lần xác nhận một mũi phải chỉ thành công lần đầu; không có mũi nhắc trùng.

## Kiểm thử tự động

Kết quả sau sửa bốn mục review ngày 13/11/2026 trên Java 21: `mvn -B -ntp test` **BUILD SUCCESS — 306 test, 0 failure, 0 error, 0 skipped** (thêm 7 ca so với 299 test trước sửa). Windows sandbox chặn truy cập Java/Mockito nên lượt xác nhận chạy ngoài sandbox; không bỏ qua test hoặc thay cấu hình build. Log tại `target/demo-review-full-test.log`.

```powershell
mvn test
mvn test '-Dtest=PetHealthWebTest,PetHealthDemoTest,MutableClockTest'
```

Các ca mới kiểm tra luồng thêm/sửa/sinh lịch, version cũ, quyền sở hữu GET/POST, CSRF, phân quyền xác nhận/thêm khám, ngày và dữ liệu sai, lọc toàn cửa hàng, badge/nút theo State, menu/trang chủ theo bốn vai trò, bật/tắt endpoint demo, seed không trùng, mốc D−8/D−7/D/D+1, Completed bất biến, múi giờ và ngày nhuận của MutableClock. Các kiểm thử 5A tiếp tục kiểm tra State, rollback và hoàn tất đồng thời.

Sửa review bổ sung: Clock nguồn cố định lúc 10:00 ngày 13/11/2026 và Spring context/DB mới sau mỗi ca demo; chọn lại hôm nay lấy đúng giờ; chọn lại ngày giả không lùi; đặt/hủy lịch hẹn giữ nguyên mốc hai tiếng và giờ làm việc; lỗi database/xung đột version sau `saveAndFlush` rollback toàn bộ snapshot trạng thái, ngày và version rồi thử lại thành công; luồng khác không đọc được ngày tạm; đổi tên rồi seed lại không nhân đôi; nâng cấp seed cũ đã đổi tên không mất dữ liệu.

## Các file sửa trong lượt xử lý review

| Nhóm | File | Thay đổi |
|---|---|---|
| Clock | `config/AppointmentClockConfig.java`, `config/MutableClock.java` | Clock thật độc lập, healthClock có qualifier; ngày tạm theo luồng và chỉ công bố sau thành công; cùng ngày giữ đúng giờ |
| Điều phối | `service/DemoDateService.java` | Chạy scheduler trong giao dịch riêng và thông báo lỗi tiếng Việt khi không thể xác nhận ngày |
| Inject | `service/OwnedPetService.java`, `service/PetHealthService.java`, `service/VaccinationService.java`, `scheduler/VaccinationScheduler.java` | Constructor nhận healthClock bằng qualifier tường minh, không sửa logic State |
| Seed | `config/PetHealthDemoSeeder.java`, `domain/OwnedPet.java`, `repository/OwnedPetRepository.java` | Khóa seed ổn định, khóa chủ khi seed, nhận lại dữ liệu cũ đã đổi tên |
| Kiểm thử | `PetHealthDemoTest.java`, `MutableClockTest.java`, `VaccinationSchedulingConfigTest.java` | Clock cố định/cô lập từng ca, các hồi quy review và qualifier trong context kiểm thử |
| Hướng dẫn | `templates/health/demo.html`, `docs/STAGE5B.md` | Mô tả đúng phạm vi Clock, giả định về cùng ngày, seed và kết quả kiểm thử |

Đường dẫn Java chính ở `src/main/java/com/petcare/`, test ở `src/test/java/com/petcare/`, template ở `src/main/resources/`. Các file 5B khác đang có thay đổi từ trước được giữ nguyên trong lượt sửa review này.
