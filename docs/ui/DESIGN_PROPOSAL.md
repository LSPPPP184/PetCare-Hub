# Ba hướng thiết kế PetCare Hub

## Mục tiêu và phạm vi

Giữ nguyên luồng Thymeleaf, quyền truy cập và State hiện tại. Ưu tiên người dùng đọc tiếng Việt, tìm sản phẩm, theo dõi lịch tiêm và xử lý công việc. Chỉ đề xuất và dựng HTML/CSS trong `docs/ui/`; chưa tích hợp vào `src/`.

Các bản mẫu chạy ngoại tuyến, không tải Bootstrap, Google Fonts, Bootstrap Icons hay thư viện mới. Minh họa SVG tự dựng nằm trong preview. JavaScript thuần chỉ dùng cho tìm/lọc/sắp sản phẩm, cập nhật chi tiết theo sản phẩm đã chọn và phản hồi nút mô phỏng. Biểu đồ hoàn toàn bằng HTML/CSS.

## A — Nhà của những người bạn (ấm áp, thân thiện)

Phong cách cửa hàng gần gũi: nền giấy kem, xanh lá trầm, đất nung; tiêu đề serif, khối hình vòm, minh họa thú cưng lớn. Trang chủ kể câu chuyện chăm sóc; danh mục có bộ lọc bên trái; hồ sơ giống sổ chăm sóc; dashboard nhẹ và thoáng.

| Token | Giá trị |
| --- | --- |
| Chính | `#285444` |
| Phụ | `#A3472C` |
| Nền / bề mặt | `#FAF6EE` / `#FFFFFF` |
| Chữ / chữ phụ | `#253C31` / `#586258` |
| Thành công | `#166534` |
| Cảnh báo | `#854D0E` |
| Lỗi | `#991B1B` |
| Nền nhấn | `#E8EDD9` |

- Phông: tiêu đề `"Times New Roman", "Segoe UI", Arial, serif`; nội dung `"Segoe UI", Arial, sans-serif`. Nguồn: phông cài sẵn Windows/macOS; không tải từ mạng. Times New Roman, Segoe UI và Arial có ký tự tiếng Việt đầy đủ (đã kiểm tra bảng glyph của font cài trên máy); nếu thiếu Times New Roman, tiêu đề chuyển sang Segoe UI/Arial để giữ bộ dấu nhất quán; cuối cùng mới dùng generic serif/sans-serif của hệ điều hành. Không dùng Georgia vì bản cài trên máy thiếu nhiều glyph tiếng Việt. Có dòng thử toàn bộ nhóm dấu trong trang chọn hướng.
- Cỡ chữ: body 16px/1.65, phụ 14px, h1 trang chủ 40–64px, h1 trang trong 32–44px, h2 26–32px, h3 20px; tiêu đề line-height 1.12–1.2 để không cắt dấu.
- Bo góc: card 24px, input 12px, nút tròn 999px, hình hero vòm. Bóng `0 12px 32px #253C310D`.
- Khoảng cách: nhịp 8px; card 24px; khoảng section 40–64px; khung tối đa 1200px, lề 24px desktop/16px mobile.
- Thẻ nền trắng, vùng ảnh pastel; nút xanh chữ trắng, nút phụ nền trong viền xanh; bảng đường kẻ mảnh; form label đặt trên, nền trắng, focus xanh đậm.
- Ưu: dễ tạo cảm giác chăm sóc, hợp cả bán hàng lẫn hồ sơ sức khỏe, nhận diện riêng mà vẫn gần Bootstrap.
- Nhược: tiêu đề serif và khoảng trắng chiếm diện tích; dashboard cần giữ body sans-serif và số căn thẳng để không chậm đọc.

## B — Chăm sóc tinh gọn (hiện đại, tối giản)

Phong cách cửa hàng chuyên nghiệp, thiên về dữ liệu: nền xám lạnh, mực đậm và teal. Tiêu đề sans-serif đậm, lưới chính xác, đường kẻ phân cấp; hero hai nửa cân bằng, dải thông tin gọn; bộ lọc danh mục nằm ngang; dashboard có thanh điều hướng dọc ở desktop.

| Token | Giá trị |
| --- | --- |
| Chính | `#115E59` |
| Phụ | `#334155` |
| Nền / bề mặt | `#F4F7F8` / `#FFFFFF` |
| Chữ / chữ phụ | `#172B35` / `#52616B` |
| Thành công | `#166534` |
| Cảnh báo | `#854D0E` |
| Lỗi | `#991B1B` |
| Nền nhấn | `#DDEDEB` |

- Phông toàn bộ: `"Segoe UI", Arial, sans-serif` (phông hệ điều hành; đầy đủ dấu tiếng Việt; không phụ thuộc Google Fonts). Số dùng `font-variant-numeric: tabular-nums`.
- Cỡ chữ: body 16px/1.6, phụ 14px, hero 40–64px, h1 trang trong 32–44px, h2 26–32px, h3 20px. Heading weight 650–750.
- Bo góc: card 8px, input/nút 6px. Không dùng bóng ở trạng thái thường; hover nhấn border. Đường kẻ `#B8C8CF`.
- Khoảng cách: nhịp 8px, card 24px, khung tối đa 1200px; thông tin quản trị dày hơn A nhưng vẫn giữ vùng bấm 44px.
- Card và bảng nền trắng, đầu bảng xám nhẹ; nút chính teal; form gọn, label rõ, không dùng placeholder thay nhãn.
- Ưu: dễ đọc số liệu, dễ triển khai bằng Thymeleaf/Bootstrap, phù hợp nhân viên dùng hằng ngày.
- Nhược: ít cảm xúc hơn A; cần minh họa và nội dung tốt để cửa hàng không giống phần mềm quản trị thuần túy.

## C — Hội bạn bốn chân (tươi sáng, năng động)

Phong cách vui tươi với tím đậm, vàng nắng, viền mực và bóng cứng. Hero tiêu đề lớn, minh họa nghiêng nhẹ như nhãn dán; các ô khám phá có nền xen kẽ; bộ lọc ngang; dashboard số liệu thành ô màu nhưng bảng vẫn nền trắng.

| Token | Giá trị |
| --- | --- |
| Chính | `#55308C` |
| Phụ | `#FFCF4A` (chữ tối `#282238`, không dùng chữ trắng) |
| Nền / bề mặt | `#FFFDF5` / `#FFFFFF` |
| Chữ / chữ phụ | `#282238` / `#60586C` |
| Thành công | `#166534` |
| Cảnh báo | `#854D0E` |
| Lỗi | `#991B1B` |
| Nền nhấn | `#EEE5FF` |

- Phông: `"Segoe UI", Arial, sans-serif`; nguồn hệ điều hành, hỗ trợ bộ ký tự tiếng Việt, có fallback cho máy thiếu font. Không nhúng phông ngoài.
- Cỡ chữ: body 16px/1.65, phụ 14px, hero 40–64px đậm 800, h1 trang trong 32–44px, h2 26–32px, h3 20px. Không dùng chữ hoa dài để tránh khó đọc dấu.
- Bo góc: card 18px, input/nút 10px; viền 2px `#282238`; bóng `5px 5px 0 #282238`.
- Khoảng cách: nhịp 8px; card 24px; section 40–64px, mobile 24–32px. Giảm xoay trang trí trên điện thoại.
- Thẻ sản phẩm nền xen kẽ tím nhạt/vàng nhạt/xanh; nút chính tím chữ trắng; nút phụ vàng chữ tối; bảng viền rõ, form trắng có outline focus.
- Ưu: cá tính, dễ nhớ, phù hợp người trẻ; minh họa SVG đồng nhất và chạy offline.
- Nhược: hình khối đậm dễ gây mệt khi nhiều dữ liệu; không dùng màu trang trí thay cho màu trạng thái, cần tiết chế ở màn hình quản trị.

## Hệ trạng thái thống nhất cho cả A / B / C

Màu thương hiệu thay đổi, ý nghĩa trạng thái không thay đổi. Badge có **chữ tiếng Việt + ký hiệu**, không chỉ dựa vào màu. Badge tối thiểu 14px, weight 600, nền đặc, có border cùng màu chữ; tương phản chữ/nền được tính bằng độ sáng tương đối sRGB trong `verify_preview.py` (ngưỡng 4.5:1).

| Nhóm | Chữ / viền | Nền | Ký hiệu | Ý nghĩa |
| --- | --- | --- | --- | --- |
| Thông tin | `#1E40AF` | `#DBEAFE` | ◷ | Đã lên lịch / xác nhận |
| Đang xử lý | `#5B21B6` | `#EDE9FE` | ↻ | Đang thực hiện / giao |
| Thành công | `#166534` | `#DCFCE7` | ✓ | Hoàn tất / đang có hiệu lực |
| Cần chú ý | `#854D0E` | `#FEF3C7` | ! | Chờ / sắp tới hạn / hết hạn |
| Lỗi / quá hạn | `#991B1B` | `#FEE2E2` | ! | Cần xử lý quá hạn |
| Kết thúc trung tính | `#374151` | `#F3F4F6` | × | Đã hủy |

| Đối tượng | Mã State → nhãn → nhóm màu |
| --- | --- |
| Order | `PENDING` → Chờ xác nhận → chú ý; `CONFIRMED` → Đã xác nhận → thông tin; `SHIPPING` → Đang giao → đang xử lý; `COMPLETED` → Hoàn thành → thành công; `CANCELLED` → Đã hủy → trung tính |
| Appointment | `BOOKED` → Đã đặt → thông tin; `IN_PROGRESS` → Đang thực hiện → đang xử lý; `DONE` → Hoàn tất → thành công; `CANCELLED` → Đã hủy → trung tính |
| VaccinationRecord | `SCHEDULED` → Đã lên lịch → thông tin; `DUE_SOON` → Sắp đến hạn → chú ý; `OVERDUE` → Quá hạn → lỗi; `COMPLETED` → Đã tiêm → thành công |
| Subscription | `ACTIVE` → Đang hoạt động → thành công; `EXPIRED` → Đã hết hạn / hết lượt → chú ý; `CANCELLED` → Đã hủy → trung tính |

Đã hủy là một kết thúc hợp lệ nên dùng xám, dành đỏ cho quá hạn/lỗi cần hành động. Trang dashboard mỗi hướng có bảng mẫu đủ 16 badge để đối chiếu. Không thay nhãn nghiệp vụ của ứng dụng.

## Responsive và khả năng sử dụng

- 320–639px: một cột, menu wrap, nút đủ 44px, filter xuống dòng, thẻ và timeline không tràn màn hình. 640–959px: hai cột sản phẩm; từ 960px: ba cột, các vùng thông tin chia đôi.
- Form có label, focus visible 3px, trạng thái thông báo `aria-live`; có skip link tới nội dung và `aria-current` cho trang đang mở.
- Biểu đồ 12 tháng có nhãn số trực tiếp, đơn vị triệu đồng, caption và bảng số liệu thay thế trong `details`; chỉ vùng biểu đồ/bảng cuộn ngang trên mobile, không cuộn ngang toàn trang.
- Bộ lọc tìm tên không phân biệt dấu; lọc nhóm và sắp giá thực sự trên dữ liệu mẫu, có trạng thái rỗng và nút đặt lại. Khi tắt JS vẫn đọc được toàn bộ sản phẩm và có thông báo giới hạn.
- Không mô phỏng quyền truy cập bằng cách giấu CSS. Thanh đổi trang prototype được ghi nhãn riêng; triển khai thật phải dùng lại `sec:authorize`, CSRF, version và các action được State cho phép.
- Ngày mẫu cố định 19/11/2026; 4 mũi tiêm là 4 bản ghi minh họa, không phải 4 giai đoạn diễn ra đồng thời của một mũi. Giữ lời nhắc lịch mẫu không phải tư vấn thú y.

## Đề xuất chọn A

**Khuyên chọn A — Nhà của những người bạn.** Hướng này cân bằng giữa cửa hàng thân thiện và chăm sóc sức khỏe, giúp PetCare Hub có nhận diện riêng trong bài tập lớn mà vẫn dễ chuyển sang fragment Thymeleaf và Bootstrap. Giữ sans-serif ở form, bảng và dashboard để đọc dữ liệu rõ. B phù hợp hơn nếu ưu tiên tốc độ thao tác nội bộ; C phù hợp nếu muốn định vị vui nhộn và cá tính mạnh.

Không có đề nghị duyệt thư viện/phông ngoài trong vòng này. Sau khi chọn hướng mới lập kế hoạch tích hợp; vòng hiện tại không sửa menu, template, Java hay database của ứng dụng.
