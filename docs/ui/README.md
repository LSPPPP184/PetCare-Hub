# Xem trước giao diện PetCare Hub

Mở trực tiếp **`D:\petcare-hub\docs\ui\preview\index.html`** bằng Chrome, Edge hoặc Firefox. Không cần chạy Spring Boot, SQL Server, Maven hay cài thư viện. Có thể mở bằng File Explorer, nhấp đúp file `index.html`, hoặc kéo file vào trình duyệt.

- [Trang chọn ba hướng](preview/index.html)
- [A — Nhà của những người bạn](preview/option-a/index.html) — khuyên chọn
- [B — Chăm sóc tinh gọn](preview/option-b/index.html)
- [C — Hội bạn bốn chân](preview/option-c/index.html)
- [Kiểm kê giao diện hiện có](INVENTORY.md)
- [Đề xuất thiết kế và màu trạng thái](DESIGN_PROPOSAL.md)
- [Kết quả kiểm chứng và giới hạn](VERIFICATION.md)

## Cách so sánh

1. Mở từng hướng từ trang chọn. Thanh “Bản xem trước” trên mọi trang liên kết đủ 5 màn hình.
2. Ở danh mục, thử nhập `dai dat`, lọc “Phụ kiện”, đổi giá tăng/giảm, thử từ khóa không có kết quả rồi “Đặt lại”. Liên kết dịch vụ từ trang chủ lọc sẵn nhóm dịch vụ.
3. Bấm tên sản phẩm để mở đúng chi tiết; thử nút thêm giỏ hoặc chọn dịch vụ. Thông báo chỉ minh họa, không gửi request, không lưu giỏ/đơn/lịch.
4. Mở hồ sơ Bông: timeline có Đã tiêm, Quá hạn, Sắp đến hạn, Đã lên lịch. Ngày tham chiếu cố định **19/11/2026**, không tự thay đổi theo ngày máy.
5. Mở dashboard: xem 12 cột doanh thu, mở “Xem bảng số liệu 12 tháng”, xem top 5, thành viên đủ 4 hạng và bảng đủ 16 badge.
6. Dùng công cụ responsive của trình duyệt ở 320px, 375px, 768px và 1440px; thử zoom 200%, di chuyển bằng Tab, mở trang không có mạng. Bảng/biểu đồ được cuộn trong vùng riêng trên màn hình hẹp.

Prototype ghi rõ khách/CUSTOMER/ADMIN theo bối cảnh. Thanh chuyển màn hình là công cụ so sánh thiết kế; không phải cơ chế đăng nhập hoặc menu phân quyền của ứng dụng thật. STAFF đã được kiểm kê đầy đủ nhưng không có trang mẫu riêng trong phạm vi 5 trang được yêu cầu.

## Các file đã tạo

Tất cả **32 file** nằm trong `docs/ui/`:

```text
docs/ui/
├── INVENTORY.md
├── DESIGN_PROPOSAL.md
├── README.md
├── VERIFICATION.md
├── build_previews.py
├── verify_preview.py
└── preview/
    ├── index.html
    ├── index.css
    ├── preview.js
    ├── assets/
    │   ├── friends.svg
    │   ├── harness.svg
    │   ├── bowl.svg
    │   ├── toy.svg
    │   └── care.svg
    ├── option-a/
    │   ├── index.html
    │   ├── catalog.html
    │   ├── product.html
    │   ├── vaccination.html
    │   ├── dashboard.html
    │   └── styles.css
    ├── option-b/
    │   ├── index.html
    │   ├── catalog.html
    │   ├── product.html
    │   ├── vaccination.html
    │   ├── dashboard.html
    │   └── styles.css
    └── option-c/
        ├── index.html
        ├── catalog.html
        ├── product.html
        ├── vaccination.html
        ├── dashboard.html
        └── styles.css
```

`build_previews.py` là nguồn dựng lại các HTML/CSS/JS/SVG, dùng thư viện chuẩn Python. Nếu muốn sửa và tái dựng nhất quán, sửa script này; chạy lại sẽ ghi đè các file được sinh trong `preview/`. Mỗi hướng có đúng một `styles.css` dùng cho cả năm trang. `preview.js` và minh họa SVG được dùng chung giữa các hướng.

Chạy từ `D:\petcare-hub` nếu cần tái dựng và kiểm tra:

```powershell
python -X utf8 docs/ui/build_previews.py
python -X utf8 docs/ui/verify_preview.py
```

Kiểm chứng dùng Python và Node có sẵn, không cài dependency. Chỉ mở HTML thì không cần Python hoặc Node.

## Phạm vi an toàn và giới hạn

- Không sửa file trong `src/`, không đổi POM, cấu hình, database, seed, Git branch hoặc menu của ứng dụng.
- Nội dung, tên, giá, số lượng, doanh thu và lịch tiêm đều là dữ liệu giả tiếng Việt. Minh họa SVG được tự dựng; không tải ảnh/phông/thư viện bên ngoài.
- Không dùng Google Fonts, Bootstrap Icons hay framework JavaScript; không có hạng mục thư viện cần duyệt ở vòng này.
- Trình duyệt tích hợp từ chối mở `file://` theo chính sách công cụ. Đã bỏ qua chụp màn hình tùy chọn; không tạo file ảnh chụp hoặc tuyên bố đã kiểm chứng trực quan. Kiểm tra CSS chưa thay thế việc xem trên điện thoại thật.
- Không chạy bộ test backend vì không sửa ứng dụng và yêu cầu chỉ tạo file trong `docs/ui/` (Maven thường tạo `target/`). Không có phần SQL Server thật cần kiểm chứng trong vòng giao diện tĩnh này; không cần profile `demo` để xem các mẫu.

Hướng đề nghị triển khai ở vòng sau: **A**, giữ kiểu chữ sans-serif ở bảng/form để thao tác rõ ràng. Vòng sau chỉ thực hiện khi có yêu cầu tích hợp riêng.
