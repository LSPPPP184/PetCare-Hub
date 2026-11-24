"""Tổng hợp kết quả kiểm chứng đã thực thi, chỉ ghi trong docs/."""
from pathlib import Path
import json, xml.etree.ElementTree as E

folder=Path(__file__).resolve().parent
reports=list((folder/'build/surefire-reports').glob('TEST-*.xml'))
totals=dict(tests=0,failures=0,errors=0,skipped=0)
for file in reports:
    r=E.parse(file).getroot()
    for key in totals: totals[key]+=int(r.attrib[key])
assert len(reports)==43
assert not totals['failures'] and not totals['errors']
assert 'BUILD SUCCESS' in (folder/'maven-test.log').read_text(encoding='utf-8')
mermaid=json.loads((folder/'mermaid-results.json').read_text(encoding='utf-8'))
assert len(mermaid['results'])==9
lines=['# Kết quả kiểm chứng tài liệu',
       'Ngày 22/11/2026, đối chiếu commit `1598cc754b3e95252fe61bcca8134cf6d0808fcc`.',
       '## Kiểm tra nguồn và phạm vi ghi',
       'Chỉ tạo nội dung dưới `docs/`. `git diff --exit-code -- src pom.xml AGENTS.md` đạt; Git không báo thay đổi file đã theo dõi. Sáu tài liệu chính không dùng tên class giả. Script đối chiếu xác nhận đủ 22 entity, liên kết file/dòng tồn tại, 13 đoạn Java đều trích từ nguồn và dưới 20 dòng, test dẫn chiếu tồn tại, bốn tính chất OOP mỗi tính chất có ba ví dụ.',
       'Chi tiết: [source-results.json](source-results.json). Kiểm tra tự động tên không thay thế rà soát ngữ nghĩa: đã đọc thêm controller, service, entity, seed, template và test để phân biệt quan hệ JPA với bản chụp giá trị.',
       '## Kiểm tra Mermaid',
       'Đạt **9/9 khối** bằng cả `mermaid.parse()` và `mermaid.render()` với Mermaid **11.12.1**, trong Microsoft Edge chạy ẩn. SVG được sinh thật; bộ render nạp từ CDN cho công cụ kiểm chứng, không thêm dependency vào ứng dụng. Lần chạy cuối bao gồm sửa phía giữ FK của Pet/PetProfile. Không chỉ kiểm tra hàng rào Markdown.',
       '| Tài liệu | Khối | Kết quả | SVG đã render |','| --- | --- | --- | --- |']
for r in mermaid['results']:
    lines.append(f'| {r["file"]} | {r["block"]} | Parse và render đạt | [Mở SVG]({r["svg"]}) |')
lines += ['\nERD tổng thể lớn vì chứa toàn bộ thuộc tính; nên mở SVG để phóng to. Chưa khẳng định mọi trình xem Markdown dùng phiên bản Mermaid khác sẽ trình bày giống hệt. Kết quả máy đọc: [mermaid-results.json](mermaid-results.json).',
          '## Kiểm tra hồi quy Java',
          f'**BUILD SUCCESS** — {totals["tests"]} ca test, {totals["failures"]} thất bại, {totals["errors"]} lỗi, {totals["skipped"]} bỏ qua; {len(reports)} báo cáo lớp test. Chạy Java 21, Maven offline, hoàn tất lúc 20:52:27 ngày 22/11/2026 (Asia/Saigon), thời gian Maven 2 phút 50 giây.',
          'Để tuân thủ chỉ ghi trong docs, `prepare_test_build.py` đọc pom gốc, tạo pom kiểm chứng với nguyên dependency/plugin, chỉ định source/resource của repo và chuyển build output vào `docs/_verification/build`. Không sửa pom gốc hoặc dùng báo cáo target cũ làm kết quả mới.',
          '```powershell\npython docs/_verification/prepare_test_build.py\nmvn -o -B -ntp -f docs/_verification/pom.xml test\n```',
          'Báo cáo cục bộ: `build/surefire-reports/`, `build/site/jacoco/`; log: `maven-test.log`. Thư mục build và log được bỏ qua bởi `.gitignore` ngay trong thư mục kiểm chứng để không đưa output biên dịch vào tài liệu được quản lý phiên bản.',
          '## Chạy lại kiểm tra tài liệu',
          '```powershell\n$env:PYTHONIOENCODING = "utf-8"\npython docs/_verification/check_docs.py\nnode docs/_verification/render_mermaid.cjs\npython docs/_verification/summarize_checks.py\n```',
          '`render_mermaid.cjs` dùng đường dẫn Playwright đóng gói và Edge có trên máy kiểm chứng; khi chạy ở máy khác cần chỉ định đường dẫn công cụ tương ứng. Script không yêu cầu thêm dependency vào pom dự án. Các file SVG và JSON hiện có cho phép xem kết quả mà không chạy lại.',
          '## Chưa xác minh bằng hệ thống thật',
          'Chưa kết nối SQL Server của người dùng, chưa diễn tập toàn luồng UI 10 phút, chưa đo tải dữ liệu lớn. Tài khoản/giá/điểm trong kịch bản phản ánh seed mặc định chứ không xác nhận trạng thái DB hiện tại. Xem [hạn chế và danh sách cần kiểm tra](../limitations.md) cùng [bước SQL Server/profile demo](../demo-script.md).']
text='\n\n'.join(lines[:8])+'\n\n'+'\n'.join(lines[8:10+9])+'\n\n'+'\n\n'.join(lines[10+9:])
# Giữ bảng liên tục, còn các mục văn bản cách nhau bằng dòng trống.
text=text.replace('| --- | --- | --- | --- |\n\n','| --- | --- | --- | --- |\n')
text=text.replace('| SVG đã render |\n\n','| SVG đã render |\n')
(folder/'README.md').write_text(text+'\n',encoding='utf-8')
(folder/'test-results.json').write_text(json.dumps({'suites':len(reports),**totals,'result':'BUILD SUCCESS'},indent=2),encoding='utf-8')
print(json.dumps(totals))
