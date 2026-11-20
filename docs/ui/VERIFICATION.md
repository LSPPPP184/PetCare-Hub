# Kết quả kiểm chứng bản xem trước

- Đạt: 16 trang HTML (15 trang của ba hướng và 1 trang chọn), mỗi trang có `lang="vi"`, UTF-8, viewport, đúng một h1 và một CSS.
- Đạt: 391 tham chiếu tài nguyên/liên kết nội bộ; đích và anchor tồn tại, không có CDN/font/API bên ngoài.
- Đạt: thẻ HTML đóng đúng thứ tự, không trùng ID, ảnh có alt, không còn thuộc tính Thymeleaf hoặc ký tự thay thế Unicode.
- Đạt: mỗi hướng đủ 5 trang và liên kết qua lại; 4 trạng thái timeline; 16 badge nghiệp vụ; biểu đồ 12 cột và bảng dữ liệu thay thế.
- Đạt: cú pháp JavaScript; kiểm tra Node VM trên mã JS thật cho lọc nhóm từ URL, tìm không dấu, trạng thái rỗng, đặt lại, sắp giá giảm, chi tiết sản phẩm và phản hồi mua/đặt dịch vụ mô phỏng.
- Đạt: tương phản chữ chính/phụ trên nền, liên kết chính trên nền nhấn, chữ trắng trên nút chính của A/B/C tối thiểu 4.5:1; có breakpoint mobile một cột và vùng cuộn riêng cho bảng/biểu đồ.

| Badge | Chữ/viền | Nền | Tương phản |
| --- | --- | --- | --- |
| Thông tin | #1E40AF | #DBEAFE | 7.15:1 |
| Đang xử lý | #5B21B6 | #EDE9FE | 7.57:1 |
| Thành công | #166534 | #DCFCE7 | 6.49:1 |
| Chú ý | #854D0E | #FEF3C7 | 6.15:1 |
| Quá hạn | #991B1B | #FEE2E2 | 6.80:1 |
| Đã hủy | #374151 | #F3F4F6 | 9.37:1 |

## Phông chữ trên máy kiểm tra

Đã đọc trực tiếp bảng cmap của các font hệ điều hành (không chỉ dựa vào tên font):

- segoeui.ttf: đủ 130 ký tự tiếng Việt kiểm tra
- segoeuib.ttf: đủ 130 ký tự tiếng Việt kiểm tra
- arial.ttf: đủ 130 ký tự tiếng Việt kiểm tra
- arialbd.ttf: đủ 130 ký tự tiếng Việt kiểm tra
- times.ttf: đủ 130 ký tự tiếng Việt kiểm tra
- timesbd.ttf: đủ 130 ký tự tiếng Việt kiểm tra

Đã loại Georgia khỏi thiết kế vì font cài trên máy thiếu nhiều glyph tiếng Việt. Hướng A dùng Times New Roman với fallback Segoe UI/Arial; B/C dùng Segoe UI với fallback Arial. Việc có glyph không thay thế kiểm tra bằng mắt về khoảng cách dấu và xuống dòng.

## Giới hạn kiểm chứng

Trình duyệt tích hợp đã từ chối mở `file://` theo chính sách URL của công cụ. Không chụp ảnh, không xác nhận trực quan hoặc tự động đo overflow trên viewport thật. Kiểm tra responsive ở đây là kiểm tra cấu trúc CSS; cần xem thủ công ở 320px, 375px, 768px và 1440px. Chưa kiểm tra bằng trình đọc màn hình.

Không chạy Maven/JUnit hoặc SQL Server: tác vụ chỉ tạo tài liệu và mẫu tĩnh; không sửa mã ứng dụng. Không tạo `target/` hay ghi ngoài `docs/ui/` để giữ đúng chế độ an toàn. Kết quả trên không phải tuyên bố test backend đã chạy hoặc đã xanh trong lần này.

Chạy lại: `python -X utf8 docs/ui/verify_preview.py` (cần Python và Node có sẵn; không cài gói).
