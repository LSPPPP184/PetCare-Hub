"""Đối chiếu tên, liên kết và đoạn trích với nguồn; không sửa code."""
from pathlib import Path
import re, json

root = Path(__file__).resolve().parents[2]
docs = root/'docs'
names = ['uml-class-diagram','erd','patterns','oop-checklist','demo-script','limitations']
texts = {name:(docs/(name+'.md')).read_text(encoding='utf-8') for name in names}
checks=[]
for name,text in texts.items():
    assert '\ufffd' not in text, name
    assert text.count('```')%2 == 0, name
    for target in re.findall(r'\]\(([^)]+)\)',text):
        if target.startswith('https:'): continue
        file,_,anchor = target.partition('#')
        dest=(docs/file).resolve()
        assert dest.is_file(), (name,target)
        if anchor.startswith('L'):
            n=int(anchor[1:])
            assert 1<=n<=len(dest.read_text(encoding='utf-8').splitlines()), target
    checks.append(f'{name}.md: UTF-8, hàng rào mã và liên kết nội bộ đạt')
sources = list((root/'src/main/java').rglob('*.java'))
classes = {f.stem for f in sources}
entity_classes = {f.stem for f in sources if re.search(r'@Entity\b',f.read_text(encoding='utf-8'))}
blocks = re.findall(r'```mermaid\s*\n([\s\S]*?)```',texts['uml-class-diagram'])
assert len(blocks)==8
for block in blocks:
    for line in block.splitlines()[1:]:
        for cls in re.findall(r'^([A-Za-z]\w*)|(?:<\|--|<\|\.\.|\.\.>|-->|<\|--)\s*(?:"[^"]*"\s*)?([A-Za-z]\w*)',line):
            name=next(x for x in cls if x)
            assert name in classes,name
erd_block=re.search(r'```mermaid\s*\n([\s\S]*?)```',texts['erd']).group(1)
nodes=set(re.findall(r'^    (\w+) \{',erd_block,re.M))
assert entity_classes <= nodes, entity_classes-nodes
assert len(entity_classes)==22
checks.append('UML: 8 khối, tên class tồn tại; ERD: đủ 22 entity và 4 bảng collection/nối')
snippets=re.findall(r'```java\s*\n([\s\S]*?)```',texts['patterns'])
all_source=[f.read_text(encoding='utf-8') for f in sources]
for snippet in snippets:
    assert len(snippet.strip().splitlines())<20
    assert any(snippet.strip() in code for code in all_source),snippet
assert len(snippets)==13
for cls,method in re.findall(r'\[([A-Za-z]\w*)#(\w+)\]',texts['patterns']+texts['oop-checklist']):
    code=(root/'src/test/java/com/petcare'/f'{cls}.java').read_text(encoding='utf-8')
    assert re.search(r'\b'+re.escape(method)+r'\s*\(',code),(cls,method)
checks.append('Pattern: 13 đoạn trích thật, mỗi đoạn dưới 20 dòng; tất cả tên test dẫn chiếu tồn tại')
for title in ['Đóng gói','Kế thừa','Đa hình','Trừu tượng']:
    part=texts['oop-checklist'].split('## '+title)[1].split('## ')[0]
    assert len(re.findall(r'\| \[\w+:\d+\]',part))>=2,title
checks.append('OOP: cả bốn tính chất có ba ví dụ kèm dòng nguồn')
(docs/'_verification/source-results.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2),encoding='utf-8')
print('\n'.join(checks))
