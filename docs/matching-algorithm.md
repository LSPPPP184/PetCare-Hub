# GĐ6 — Quiz ghép đôi thú cưng (Strategy)

## Thiết kế và giả định

- `PetProfile` gắn một-một với `Pet`, cascade theo thú cưng; mức từ 1–5, thời gian cần chăm sóc từ 0,25–24 giờ/ngày. Form sản phẩm hiện có của STAFF/ADMIN nhập được toàn bộ hồ sơ. Hồ sơ mới mặc định mức 3, 2 giờ/ngày, chưa xác nhận thân thiện với trẻ; nhân viên cần nhập đúng thông tin thực tế.
- `QuizAnswers` chứa diện tích nhà, trẻ em, vận động, thời gian, ngân sách, dị ứng và kinh nghiệm. Thời gian rảnh cho phép 0–24 giờ; ngân sách là khả năng chăm sóc tương đối, không phải giá mua thú cưng.
- `MatchingController` chỉ nhận/kiểm tra form, gọi `MatchingService`, trả view. `MatchingStrategy` có hai triển khai `WeightedScoreStrategy` và `BudgetFirstStrategy`; phần chung nằm trong `AbstractScoreStrategy`. Không dùng ML, JavaScript hoặc thư viện mới.
- Khách, CUSTOMER, STAFF, ADMIN đều được làm quiz. Tất cả tài khoản đăng nhập có lịch sử riêng; khách chỉ lưu kết quả trong phiên. STAFF/ADMIN được sửa hồ sơ theo quyền quản lý sản phẩm sẵn có.
- Kết quả chỉ là mức phù hợp theo hồ sơ minh họa, không phải xác suất thành công hoặc đánh giá thú y. Mức rụng lông thấp không bảo đảm không gây dị ứng.

## Bước 1: lọc cứng

Ứng viên phải `active=true`, `status=AVAILABLE`, có `PetProfile`. Bỏ ứng viên nếu:

1. Nhà có trẻ em và `childFriendly=false`.
2. Có dị ứng lông và `sheddingLevel > 2`.
3. Nhà nhỏ và `spaceNeed > 2`.

Các điều kiện kết hợp bằng AND, dùng chung cho cả hai strategy. Không nới lỏng bộ lọc khi thiếu kết quả; có thể trả 0–3 gợi ý.

## Bước 2: chuẩn hóa và chấm điểm

Quy ước khả năng không gian của nhà nhỏ/vừa/lớn lần lượt là `H = 2/3/5`. Với mức khả năng `a`, nhu cầu `n`, đặt:

```text
capacity(a, n) = 1 − max(0, n − a) / 4
normalizedWeight_i = weight_i / Σ(weight_i)
points_i = (normalizedWeight_i × 100) × fit_i
scorePercent = Σ(points_i)
```

| Tiêu chí | Độ khớp fit (0–1) | Trọng số cân bằng | Trọng số ưu tiên ngân sách |
|---|---|---:|---:|
| Kích thước | capacity(H, sizeLevel) | 10 | 10 |
| Không gian | capacity(H, spaceNeed) | 15 | 15 |
| Vận động | 1 − abs(activityLevel − energyLevel) / 4 | 20 | 20 |
| Thời gian | min(1, freeHoursPerDay / timeNeedHoursPerDay) | 20 | 20 |
| Ngân sách | capacity(budgetLevel, careCostLevel) | 20 | 60 |
| Kinh nghiệm | capacity(experienceLevel, experienceNeeded) | 10 | 10 |
| Rụng lông | Dị ứng: 1 − (sheddingLevel − 1) / 4; không dị ứng: 1 | 5 | 5 |
| **Tổng** | | **100** | **140** |

Khả năng dư so với nhu cầu không bị phạt. Vận động dùng khoảng cách hai chiều vì chủ rất năng động cũng có thể ít phù hợp với thú ít năng lượng. Trẻ em là điều kiện lọc, không cộng điểm thưởng. Kích thước và không gian là hai thông số riêng của hồ sơ.

Chia trọng số cho tổng trước khi nhân để tránh tràn số trung gian. Tổng trọng số của từng strategy phải hữu hạn và dương; điểm kết quả phải hữu hạn, thuộc [0, 100]. Chỉ sai số cộng dấu phẩy động sát 100 (tối đa 7 ULP tại 100) được đưa về 100; giá trị vượt miền thực sự bị từ chối bằng thông báo tiếng Việt.

Sắp xếp điểm thô giảm dần, hòa điểm thì ID tăng dần; service lấy tối đa 3 kết quả. Lời giải thích hiển thị trọng số chuẩn hóa theo phần trăm, tổng là 100%, không dùng mẫu số thô nhỏ dễ bị làm tròn thành 0. Độ khớp và trọng số hiển thị tối đa 6 chữ số có nghĩa; số rất nhỏ dùng ký hiệu khoa học. Điểm thành phần hiển thị 2 số lẻ, riêng điểm dương quá nhỏ để hiện khác 0 dùng 6 chữ số có nghĩa. Công thức cộng đúng các điểm thành phần đã hiển thị, kèm hiệu chỉnh làm tròn nếu cần để bằng điểm tổng hiển thị 2 số lẻ. Việc định dạng không thay đổi điểm thô hoặc thứ hạng; tổng các trọng số thành phần đã định dạng có thể có sai số làm tròn. Lịch sử đã lưu giữ nguyên bản chụp cũ.

### Ví dụ chứng minh hoán đổi

Cùng đáp án: nhà lớn, không trẻ em, vận động 5, rảnh 2 giờ, ngân sách 1, không dị ứng, kinh nghiệm 5. Hai thú đều đạt các tiêu chí còn lại:

- A năng lượng 5, chi phí 5, cần 2 giờ: cân bằng **80%**, ưu tiên ngân sách **57,14%**.
- B năng lượng 1, chi phí 1, cần 8 giờ: cân bằng **65%**, ưu tiên ngân sách **75%**.

Strategy cân bằng xếp A trước B; strategy ưu tiên ngân sách xếp B trước A. Unit test dùng đúng bộ dữ liệu này.

## Cấu hình

Trong `application.properties`:

```properties
petcare.matching.default-strategy=weighted
petcare.matching.budget-multiplier=3
petcare.matching.weights.size=10
petcare.matching.weights.space=15
petcare.matching.weights.activity=20
petcare.matching.weights.time=20
petcare.matching.weights.budget=20
petcare.matching.weights.experience=10
petcare.matching.weights.shedding=5
```

Đổi mặc định thành `budget-first`, hoặc chọn strategy trên form mỗi lần gửi. Tất cả trọng số phải hữu hạn và dương; hệ số ngân sách phải hữu hạn, lớn hơn 1. Strategy thứ hai nhân trọng số ngân sách với hệ số rồi chuẩn hóa theo tổng mới. Validation kiểm tra tổng thực tế của cả hai strategy, từ chối tổng bằng 0 hoặc không hữu hạn khi khởi động. Tham số strategy ngoài danh sách bị từ chối trên form.

## Lưu trữ, đồng thời và Unicode

- `QuizResult` lưu chủ tài khoản, bản sao đáp án, strategy và thời điểm từ `Clock` được inject. Clock thật dùng múi giờ `Asia/Ho_Chi_Minh`, không dùng Clock giả lập tiêm phòng. Test cố định thời điểm qua ranh giới năm.
- `quiz_matches` lưu top 3 theo thứ tự: ID/tên pet, điểm, lý do và công thức. Sửa pet hoặc cấu hình sau đó không tính lại lịch sử. Link đến sản phẩm phản ánh trạng thái hiện tại; sản phẩm đã ẩn có thể trả 404.
- Mọi truy vấn xem lịch sử chi tiết đều kèm email chủ đang đăng nhập; truy cập kết quả người khác trả 404. POST có CSRF; PRG tránh gửi lại khi tải lại trang kết quả.
- Sửa sản phẩm tiếp tục dùng giao dịch, khóa `CommerceLocks` và phiên bản `Product`. `PetProfile` có `@Version` riêng và hidden version để phát hiện form cũ khi chỉ hồ sơ thay đổi. Không bind trạng thái/tồn kho từ form sửa vào các trường do luồng đơn hàng quản lý.
- Tương thích form sản phẩm GĐ2–GĐ5: request không gửi trường `profile.*` giữ nguyên hồ sơ. Request có gửi hồ sơ phải mang đúng phiên bản; form GĐ6 luôn có hidden version. Pet mới chưa gửi hồ sơ được tạo các mức mặc định hợp lệ.
- Mapping chuỗi kế thừa cấu hình `hibernate.use_nationalized_character_data=true`, tạo `nvarchar` trên SQL Server. Khóa seed hồ sơ là chuỗi không null, unique; UUID cho hồ sơ thường, khóa cố định cho demo.
- Seeder GĐ6 chạy sau dữ liệu sản phẩm cũ, thêm 8 thú riêng biệt theo `matching-poodle`, `matching-golden`, `matching-husky`, `matching-corgi`, `matching-british`, `matching-siamese`, `matching-rabbit`, `matching-hamster`. Giữ nguyên 19 sản phẩm GĐ2 (database demo mới có 27 sản phẩm, 14 Pet). Pet cũ chưa có hồ sơ được bổ sung với khóa `matching-existing-{id}`.
- Khởi động lại không tạo bản sao hoặc ghi đè tên, hồ sơ, trạng thái đã chỉnh. Tám thú mới đều AVAILABLE lần seed đầu. Nếu đã mua/giữ chỗ/ẩn thú demo, lần seed sau không tự phục hồi AVAILABLE; dùng database demo riêng mới nếu cần lại đủ bộ trình diễn.

## Chạy và thử tay

Yêu cầu Java 21, Maven, SQL Server đã tạo database. Không dùng H2 cho demo. Trong PowerShell:

```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=petcare_hub;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME='<tài khoản SQL Server>'
$env:DB_PASSWORD='<mật khẩu SQL Server của bạn>'
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Mở `http://localhost:8080`. Tài khoản mẫu đã có được mô tả trong README; không tạo thêm tài khoản/mật khẩu ở GĐ6.

1. Khi chưa đăng nhập: ở menu và trang chủ mở **Quiz chọn thú cưng**, nhập nhà lớn, có trẻ em, vận động 4, 3 giờ, ngân sách 4, không dị ứng, kinh nghiệm 4. Gửi form, xem tối đa 3 kết quả, phần trăm, lý do, công thức thay số và link sản phẩm. Không có link lịch sử cho khách.
2. Làm lại với nhà nhỏ và dị ứng: kết quả chỉ có nhu cầu không gian ≤2 và rụng lông ≤2. Bật trẻ em: mọi kết quả phải có hồ sơ thân thiện. Thử 0 giờ rảnh để thấy điểm thời gian bằng 0. Nhập thiếu/ngoài 1–5 để thấy lỗi xác thực.
3. Chọn lần lượt **Cân bằng các tiêu chí** và **Ưu tiên ngân sách** với cùng đáp án. Muốn chứng minh đảo thứ hạng rõ ràng, tạo hai Pet A/B bằng form quản lý theo ví dụ trên và tạm ẩn ứng viên khác trong database demo riêng; không phải mọi bộ đáp án đều đảo thứ hạng.
4. Đăng nhập CUSTOMER, làm quiz, mở **Lịch sử quiz của tôi**, chọn **Xem điểm và giải thích**. Tải lại kết quả không tạo thêm lịch sử. Đăng xuất, đăng nhập tài khoản khác và mở URL chi tiết vừa lưu: trả 404; khách được yêu cầu đăng nhập.
5. Với STAFF/ADMIN: mở **Quản lý sản phẩm** từ menu hoặc **Quản lý hồ sơ ghép đôi thú cưng** ở trang chủ, chọn sửa Pet. Nhập mức 1–5 và giờ chăm sóc, lưu, làm quiz lại. Mở form ở hai tab, lưu thay đổi hồ sơ ở tab đầu rồi tab thứ hai: tab cũ phải báo xung đột. Trang quản lý không xuất hiện với CUSTOMER/khách.
6. Đổi tên một thú demo thành **Tên đã sửa vẫn giữ dấu**, sửa hồ sơ; khởi động ứng dụng hai lần. Kiểm tra tên, hồ sơ, số lượng thú và lịch sử không bị nhân đôi hoặc mất dấu. Kết quả lịch sử cũ giữ nguyên lời giải thích và điểm.
7. Kiểm tra SQL Server thật bằng SSMS: `pet_profiles.pet_id` có UNIQUE, mỗi Pet có tối đa một hồ sơ; `pet_profiles.seed_key` không trùng. Kiểm tra các cột chuỗi trong `pet_profiles`, `quiz_results`, `quiz_matches` đều là `nvarchar`. Lưu và đọc lại tên **Bông – chó Poodle**, đáp án và lý do có dấu sau khi khởi động lại. DDL/H2 test không thay thế bước này.

Kiểm thử tự động (H2 chỉ trong test):

```powershell
mvn test
```

Bao gồm test bộ lọc/biên, điểm và thứ tự, hai strategy đảo thứ hạng, cấu hình, Clock cố định, seed lặp và Unicode, lịch sử riêng, CSRF, form hồ sơ/phiên bản, luồng MockMvc đầu-cuối và link menu/trang chủ của cả bốn vai trò. Các test hồi quy GĐ1–GĐ5 được giữ lại; số lượng seed mong đợi ở `ProductDemoTest` được cập nhật để tính thêm 8 Pet GĐ6.

Kết quả kiểm chứng ngày 15/11/2026: `mvn -B -ntp test` trên Java 21 — **333 test, 0 lỗi, 0 thất bại, 0 bỏ qua**, gồm 25 ca GĐ6. SQL Server thật cần kiểm tra thủ công theo bước 7 vì phiên triển khai không có thông tin đăng nhập database.

Sau sửa hai mục review ngày 17/11/2026: `mvn -B -ntp test` — **356 test, 0 lỗi, 0 thất bại, 0 bỏ qua**. Bổ sung 23 ca kiểm tra tỷ lệ trọng số (hệ số `1e-320`, `1e-300`, `1e-5`, `1`, `1000`, `1e300`, `1e305`) cho cả hai strategy, tổng không hợp lệ, miền điểm, công thức với trọng số nhỏ và phép cộng các điểm đã hiển thị. Thẻ kết quả và công thức cùng làm tròn `HALF_UP`; ca MockMvc `2.675% → 2.68%` kiểm chứng sự nhất quán này.
