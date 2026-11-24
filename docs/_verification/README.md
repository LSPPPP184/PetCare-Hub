# Kết quả kiểm chứng tài liệu

Ngày 22/11/2026, đối chiếu commit `1598cc754b3e95252fe61bcca8134cf6d0808fcc`.

## Kiểm tra nguồn và phạm vi ghi

Chỉ tạo nội dung dưới `docs/`. `git diff --exit-code -- src pom.xml AGENTS.md` đạt; Git không báo thay đổi file đã theo dõi. Sáu tài liệu chính không dùng tên class giả. Script đối chiếu xác nhận đủ 22 entity, liên kết file/dòng tồn tại, 13 đoạn Java đều trích từ nguồn và dưới 20 dòng, test dẫn chiếu tồn tại, bốn tính chất OOP mỗi tính chất có ba ví dụ.

Chi tiết: [source-results.json](source-results.json). Kiểm tra tự động tên không thay thế rà soát ngữ nghĩa: đã đọc thêm controller, service, entity, seed, template và test để phân biệt quan hệ JPA với bản chụp giá trị.

## Kiểm tra Mermaid

Đạt **9/9 khối** bằng cả `mermaid.parse()` và `mermaid.render()` với Mermaid **11.12.1**, trong Microsoft Edge chạy ẩn. SVG được sinh thật; bộ render nạp từ CDN cho công cụ kiểm chứng, không thêm dependency vào ứng dụng. Lần chạy cuối bao gồm sửa phía giữ FK của Pet/PetProfile. Không chỉ kiểm tra hàng rào Markdown.

| Tài liệu | Khối | Kết quả | SVG đã render |
| --- | --- | --- | --- |
| uml-class-diagram.md | 1 | Parse và render đạt | [Mở SVG](uml-class-diagram-1.svg) |
| uml-class-diagram.md | 2 | Parse và render đạt | [Mở SVG](uml-class-diagram-2.svg) |
| uml-class-diagram.md | 3 | Parse và render đạt | [Mở SVG](uml-class-diagram-3.svg) |
| uml-class-diagram.md | 4 | Parse và render đạt | [Mở SVG](uml-class-diagram-4.svg) |
| uml-class-diagram.md | 5 | Parse và render đạt | [Mở SVG](uml-class-diagram-5.svg) |
| uml-class-diagram.md | 6 | Parse và render đạt | [Mở SVG](uml-class-diagram-6.svg) |
| uml-class-diagram.md | 7 | Parse và render đạt | [Mở SVG](uml-class-diagram-7.svg) |
| uml-class-diagram.md | 8 | Parse và render đạt | [Mở SVG](uml-class-diagram-8.svg) |
| erd.md | 1 | Parse và render đạt | [Mở SVG](erd-1.svg) |

ERD tổng thể lớn vì chứa toàn bộ thuộc tính; nên mở SVG để phóng to. Chưa khẳng định mọi trình xem Markdown dùng phiên bản Mermaid khác sẽ trình bày giống hệt. Kết quả máy đọc: [mermaid-results.json](mermaid-results.json).

## Kiểm tra hồi quy Java

**BUILD SUCCESS** — 509 ca test, 0 thất bại, 0 lỗi, 0 bỏ qua; 43 báo cáo lớp test. Chạy Java 21, Maven offline, hoàn tất lúc 20:52:27 ngày 22/11/2026 (Asia/Saigon), thời gian Maven 2 phút 50 giây.

Để tuân thủ chỉ ghi trong docs, `prepare_test_build.py` đọc pom gốc, tạo pom kiểm chứng với nguyên dependency/plugin, chỉ định source/resource của repo và chuyển build output vào `docs/_verification/build`. Không sửa pom gốc hoặc dùng báo cáo target cũ làm kết quả mới.

```powershell
python docs/_verification/prepare_test_build.py
mvn -o -B -ntp -f docs/_verification/pom.xml test
```

Báo cáo cục bộ: `build/surefire-reports/`, `build/site/jacoco/`; log: `maven-test.log`. Thư mục build và log được bỏ qua bởi `.gitignore` ngay trong thư mục kiểm chứng để không đưa output biên dịch vào tài liệu được quản lý phiên bản.

## Chạy lại kiểm tra tài liệu

```powershell
$env:PYTHONIOENCODING = "utf-8"
python docs/_verification/check_docs.py
node docs/_verification/render_mermaid.cjs
python docs/_verification/summarize_checks.py
```

`render_mermaid.cjs` dùng đường dẫn Playwright đóng gói và Edge có trên máy kiểm chứng; khi chạy ở máy khác cần chỉ định đường dẫn công cụ tương ứng. Script không yêu cầu thêm dependency vào pom dự án. Các file SVG và JSON hiện có cho phép xem kết quả mà không chạy lại.

## Chưa xác minh bằng hệ thống thật

Chưa kết nối SQL Server của người dùng, chưa diễn tập toàn luồng UI 10 phút, chưa đo tải dữ liệu lớn. Tài khoản/giá/điểm trong kịch bản phản ánh seed mặc định chứ không xác nhận trạng thái DB hiện tại. Xem [hạn chế và danh sách cần kiểm tra](../limitations.md) cùng [bước SQL Server/profile demo](../demo-script.md).
