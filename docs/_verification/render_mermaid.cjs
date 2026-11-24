// Kiểm tra cú pháp và render mọi khối Mermaid, chỉ ghi kết quả trong docs/.
const fs = require('node:fs');
const path = require('node:path');
const {chromium} = require('C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1119-907-0_26.1119.907.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/playwright');
const docs = path.resolve(__dirname, '..');
(async () => {
  const browser = await chromium.launch({headless: true, executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
  try {
    const page = await browser.newPage();
    await page.setContent('<html lang="vi"><head><meta charset="UTF-8"></head><body></body></html>');
    await page.addScriptTag({url:'https://cdn.jsdelivr.net/npm/mermaid@11.12.1/dist/mermaid.min.js'});
    await page.evaluate(() => mermaid.initialize({startOnLoad:false, securityLevel:'strict', maxTextSize:100000, theme:'default', er:{useMaxWidth:false}, class:{useMaxWidth:false}}));
    const report = [];
    for (const filename of ['uml-class-diagram.md','erd.md']) {
      const content = fs.readFileSync(path.join(docs,filename),'utf8');
      const blocks = [...content.matchAll(/```mermaid\s*\n([\s\S]*?)```/g)];
      for (let i=0;i<blocks.length;i++) {
        const id='diagram_'+report.length;
        const result = await page.evaluate(async ({code,id}) => {
          await mermaid.parse(code);
          const {svg} = await mermaid.render(id,code);
          document.body.innerHTML=svg;
          const el=document.querySelector('svg');
          return {svg,viewBox:el.getAttribute('viewBox'),nodes:el.querySelectorAll('g.node,g.entity').length};
        },{code:blocks[i][1],id});
        const output=filename.replace('.md','')+'-'+(i+1)+'.svg';
        fs.writeFileSync(path.join(__dirname,output),result.svg);
        report.push({file:filename,block:i+1,syntax:'đạt',render:'đạt',svg:output,viewBox:result.viewBox});
      }
    }
    fs.writeFileSync(path.join(__dirname,'mermaid-results.json'),JSON.stringify({engine:'Mermaid 11.12.1',browser:'Microsoft Edge headless',results:report},null,2));
    console.log(JSON.stringify(report));
  } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1;});
