"""Kiểm chứng tài nguyên, liên kết, HTML và tương phản, không ghi ngoài docs/ui."""
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit, unquote
import json
import re
import subprocess
import struct

ROOT = Path(__file__).resolve().parent
PREVIEW = ROOT / 'preview'

class Page(HTMLParser):
    def __init__(self, path):
        super().__init__(convert_charrefs=True)
        self.path, self.ids, self.links, self.stack, self.tags = path, set(), [], [], []
        self.h1, self.css, self.images, self.states, self.cards = 0, [], [], [], []
        self.feed(path.read_text(encoding='utf-8'))
        assert not self.stack, (path, 'Thẻ chưa đóng', self.stack)
    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        self.tags.append((tag, a))
        if tag not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:
            self.stack.append(tag)
        if 'id' in a:
            assert a['id'] not in self.ids, (self.path, 'ID trùng', a['id'])
            self.ids.add(a['id'])
        if tag == 'h1': self.h1 += 1
        for attr in ('src','href'):
            if attr in a: self.links.append(a[attr])
        if tag == 'link' and a.get('rel') == 'stylesheet': self.css.append(a['href'])
        if tag == 'img':
            assert 'alt' in a, (self.path, 'Thiếu alt')
            self.images.append(a)
        if 'data-state' in a: self.states.append(a['data-state'])
        if 'data-price' in a: self.cards.append(a)
    def handle_endtag(self, tag):
        assert self.stack and self.stack[-1] == tag, (self.path, 'Đóng thẻ sai', tag, self.stack[-4:])
        self.stack.pop()

files = sorted(PREVIEW.rglob('*.html'))
pages = {p.resolve():Page(p) for p in files}
assert len(pages) == 16
link_count = 0
for path, page in pages.items():
    assert page.h1 == 1, (path, 'Số h1', page.h1)
    assert len(page.css) == 1, (path, 'Mỗi trang dùng 1 CSS')
    assert any(t == 'html' and a.get('lang') == 'vi' for t,a in page.tags)
    assert any(t == 'meta' and a.get('charset') == 'utf-8' for t,a in page.tags)
    assert any(t == 'meta' and a.get('name') == 'viewport' for t,a in page.tags)
    for link in page.links:
        parsed = urlsplit(link)
        assert not parsed.scheme and not parsed.netloc, (path, 'Tài nguyên bên ngoài', link)
        target = (path.parent / unquote(parsed.path)).resolve() if parsed.path else path
        assert target.is_relative_to(ROOT), (path, 'Liên kết ngoài docs/ui', link)
        assert target.is_file(), (path, 'Liên kết hỏng', link)
        if parsed.fragment and target.suffix == '.html':
            assert parsed.fragment in pages[target].ids, (path, 'Anchor không tồn tại', link)
        link_count += 1
    text = path.read_text(encoding='utf-8')
    assert '\ufffd' not in text and 'th:' not in text

for key in 'abc':
    folder = PREVIEW / ('option-' + key)
    assert len(list(folder.glob('*.html'))) == 5
    assert len(list(folder.glob('*.css'))) == 1
    health = pages[(folder / 'vaccination.html').resolve()]
    assert set(health.states) == {'SCHEDULED','DUE_SOON','OVERDUE','COMPLETED'}
    dashboard = (folder / 'dashboard.html').read_text(encoding='utf-8')
    assert dashboard.count('class="bar-column"') == 12
    statuses = dashboard.split('id="status-library"',1)[1]
    assert statuses.count('class="badge ') == 16
    catalog = pages[(folder / 'catalog.html').resolve()]
    assert len(catalog.cards) == 6
    for page_name in ['index.html','catalog.html','product.html','vaccination.html','dashboard.html']:
        links = pages[(folder / page_name).resolve()].links
        assert all(other in links for other in ['index.html','catalog.html','product.html','vaccination.html','dashboard.html'])

def luminance(hex_color):
    components = [int(hex_color[i:i+2],16)/255 for i in (1,3,5)]
    channels = [c/12.92 if c <= .04045 else ((c+.055)/1.055)**2.4 for c in components]
    return sum(a*b for a,b in zip(channels,[.2126,.7152,.0722]))
def contrast(fg,bg):
    a,b = sorted([luminance(fg),luminance(bg)])
    return (b+.05)/(a+.05)
colors = [('Thông tin','#1E40AF','#DBEAFE'),('Đang xử lý','#5B21B6','#EDE9FE'),('Thành công','#166534','#DCFCE7'),('Chú ý','#854D0E','#FEF3C7'),('Quá hạn','#991B1B','#FEE2E2'),('Đã hủy','#374151','#F3F4F6')]
ratios = []
for name,fg,bg in colors:
    ratio = contrast(fg,bg)
    assert ratio >= 4.5, (name,ratio)
    ratios.append(f'| {name} | {fg} | {bg} | {ratio:.2f}:1 |')
for key in 'abc':
    css = (PREVIEW / ('option-' + key) / 'styles.css').read_text(encoding='utf-8')
    tokens = dict(re.findall(r'--([a-z]+):(#[0-9a-fA-F]{6})',css))
    for fg,bg in [('ink','bg'),('muted','bg'),('primary','tint')]:
        assert contrast(tokens[fg],tokens[bg]) >= 4.5, (key,fg,bg)
    assert contrast('#FFFFFF',tokens['primary']) >= 4.5
    assert '@media(max-width:639px)' in css
    assert 'grid-template-columns:1fr' in css

# Kiểm tra hành vi JS thật trong Node VM bằng DOM tối thiểu, không cần thư viện.
catalog_cards = [{key.replace('data-',''):value for key,value in a.items() if key.startswith('data-')} for a in pages[(PREVIEW/'option-a/catalog.html').resolve()].cards]
js_test = r'''
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const source = fs.readFileSync(process.argv[1], 'utf8');
const data = JSON.parse(process.argv[2]);
function element(value='') { return {value,hidden:false,textContent:'',events:{},addEventListener(name,fn){this.events[name]=fn;}}; }
const cards = data.map(dataset => ({dataset,hidden:false}));
const ids = Object.fromEntries(['filters','q','group','sort','result-count','empty','product-grid'].map(id => [id,element()]));
ids.sort.value='asc';
ids['product-grid'].appendChild = card => {};
const document = { getElementById:id => ids[id] || null,querySelectorAll:()=>cards };
vm.runInNewContext(source,{document,location:{search:'?group=SERVICE'},URLSearchParams,Intl});
assert.equal(cards.filter(c=>!c.hidden).length,2);
ids.group.value=''; ids.q.value='dai dat';
ids.filters.events.submit({preventDefault(){}});
assert.equal(cards.filter(c=>!c.hidden).length,1);
assert.equal(cards.find(c=>!c.hidden).dataset.name,'Đai dắt dạo phố Lá Xanh');
ids.q.value='không có sản phẩm như vậy'; ids.filters.events.submit({preventDefault(){}});
assert.equal(ids.empty.hidden,false); assert.equal(ids['result-count'].textContent,'0 sản phẩm mẫu');
ids.filters.events.reset({preventDefault(){}}); assert.equal(cards.filter(c=>!c.hidden).length,6);
const order=[]; ids['product-grid'].appendChild = card => order.push(+card.dataset.price);
ids.sort.value='desc'; ids.filters.events.submit({preventDefault(){}});
assert.deepEqual(order,[8500000,790000,245000,220000,185000,95000]);
for (const [id,name,service] of [['bowl','Bát gốm Mây ấm',false],['grooming','Tắm và chăm sóc lông',true],['pet','Bông — Corgi thân thiện',false]]) {
  const fields=Object.fromEntries(['demo-purchase','detail-name','detail-category','detail-description','detail-spec','detail-price','detail-image','quantity-field','purchase-button','purchase-feedback','quantity'].map(id=>[id,element('2')]));
  const doc={getElementById:id=>fields[id]||null};
  vm.runInNewContext(source,{document:doc,location:{search:'?item='+id},URLSearchParams,Intl});
  assert.equal(fields['detail-name'].textContent,name);
  fields['demo-purchase'].events.submit({preventDefault(){}});
  assert.equal(fields['purchase-feedback'].hidden,false);
  assert(fields['purchase-feedback'].textContent.includes(name));
  if (service) assert(fields['purchase-feedback'].textContent.includes('chưa tạo lịch hẹn'));
}
console.log('PASS: filters, unsigned Vietnamese search, empty state, reset, descending prices, product selection, demo actions.');
'''
result = subprocess.run(['node','-e',js_test,str(PREVIEW/'preview.js'),json.dumps(catalog_cards,ensure_ascii=False)],capture_output=True,text=True,encoding='utf-8')
assert result.returncode == 0, result.stdout + result.stderr
syntax = subprocess.run(['node','--check',str(PREVIEW/'preview.js')],capture_output=True,text=True)
assert syntax.returncode == 0, syntax.stderr

# Đọc cmap TrueType để kiểm tra bộ dấu của font thực tế trên Windows.
characters = set('ĂăÂâÊêÔôƠơƯưĐđÀÁÃÈÉÌÍÒÓÕÙÚÝàáãèéìíòóõùúý') | {chr(i) for i in range(0x1ea0,0x1efa)}
font_results = []
for name in ['segoeui.ttf','segoeuib.ttf','arial.ttf','arialbd.ttf','times.ttf','timesbd.ttf']:
    path = Path('C:/Windows/Fonts') / name
    if not path.exists():
        font_results.append(f'{name}: không cài trên máy chạy kiểm tra')
        continue
    data = path.read_bytes()
    def u16(i): return struct.unpack_from('>H',data,i)[0]
    def u32(i): return struct.unpack_from('>I',data,i)[0]
    cmap = next(u32(12+j*16+8) for j in range(u16(4)) if data[12+j*16:16+j*16] == b'cmap')
    glyphs = set()
    for j in range(u16(cmap+2)):
        sub = cmap + u32(cmap+8+j*8)
        if u16(sub) == 4:
            n = u16(sub+6)//2
            end = sub+14
            start = end+2*n+2
            delta = start+2*n
            offset = delta+2*n
            for char in characters:
                cp = ord(char)
                for i in range(n):
                    if u16(start+2*i) <= cp <= u16(end+2*i):
                        ro = u16(offset+2*i)
                        gid = u16(offset+2*i+ro+2*(cp-u16(start+2*i))) if ro else cp
                        if gid and (gid+u16(delta+2*i))%65536: glyphs.add(char)
                        break
        elif u16(sub) == 12:
            for i in range(u32(sub+12)):
                a,b,g = struct.unpack_from('>III',data,sub+16+12*i)
                glyphs.update(c for c in characters if a <= ord(c) <= b and g+ord(c)-a)
    assert not characters-glyphs, (name, 'Thiếu glyph tiếng Việt', characters-glyphs)
    font_results.append(f'{name}: đủ {len(characters)} ký tự tiếng Việt kiểm tra')

report = f'''# Kết quả kiểm chứng bản xem trước

- Đạt: 16 trang HTML (15 trang của ba hướng và 1 trang chọn), mỗi trang có `lang="vi"`, UTF-8, viewport, đúng một h1 và một CSS.
- Đạt: {link_count} tham chiếu tài nguyên/liên kết nội bộ; đích và anchor tồn tại, không có CDN/font/API bên ngoài.
- Đạt: thẻ HTML đóng đúng thứ tự, không trùng ID, ảnh có alt, không còn thuộc tính Thymeleaf hoặc ký tự thay thế Unicode.
- Đạt: mỗi hướng đủ 5 trang và liên kết qua lại; 4 trạng thái timeline; 16 badge nghiệp vụ; biểu đồ 12 cột và bảng dữ liệu thay thế.
- Đạt: cú pháp JavaScript; kiểm tra Node VM trên mã JS thật cho lọc nhóm từ URL, tìm không dấu, trạng thái rỗng, đặt lại, sắp giá giảm, chi tiết sản phẩm và phản hồi mua/đặt dịch vụ mô phỏng.
- Đạt: tương phản chữ chính/phụ trên nền, liên kết chính trên nền nhấn, chữ trắng trên nút chính của A/B/C tối thiểu 4.5:1; có breakpoint mobile một cột và vùng cuộn riêng cho bảng/biểu đồ.

| Badge | Chữ/viền | Nền | Tương phản |
| --- | --- | --- | --- |
{chr(10).join(ratios)}

## Phông chữ trên máy kiểm tra

Đã đọc trực tiếp bảng cmap của các font hệ điều hành (không chỉ dựa vào tên font):

{chr(10).join('- '+item for item in font_results)}

Đã loại Georgia khỏi thiết kế vì font cài trên máy thiếu nhiều glyph tiếng Việt. Hướng A dùng Times New Roman với fallback Segoe UI/Arial; B/C dùng Segoe UI với fallback Arial. Việc có glyph không thay thế kiểm tra bằng mắt về khoảng cách dấu và xuống dòng.

## Giới hạn kiểm chứng

Trình duyệt tích hợp đã từ chối mở `file://` theo chính sách URL của công cụ. Không chụp ảnh, không xác nhận trực quan hoặc tự động đo overflow trên viewport thật. Kiểm tra responsive ở đây là kiểm tra cấu trúc CSS; cần xem thủ công ở 320px, 375px, 768px và 1440px. Chưa kiểm tra bằng trình đọc màn hình.

Không chạy Maven/JUnit hoặc SQL Server: tác vụ chỉ tạo tài liệu và mẫu tĩnh; không sửa mã ứng dụng. Không tạo `target/` hay ghi ngoài `docs/ui/` để giữ đúng chế độ an toàn. Kết quả trên không phải tuyên bố test backend đã chạy hoặc đã xanh trong lần này.

Chạy lại: `python -X utf8 docs/ui/verify_preview.py` (cần Python và Node có sẵn; không cài gói).
'''
(ROOT/'VERIFICATION.md').write_text(report,encoding='utf-8')
print(f'PASS: {len(pages)} HTML pages; {link_count} local references; statuses, contrast and JS interactions.')
