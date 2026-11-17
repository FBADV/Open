const { chromium } = require('playwright');
const http = require('http'); const fs = require('fs'); const path = require('path');
const root = __dirname;
const srv = http.createServer((q, r) => {
  const f = path.join(root, decodeURIComponent(q.url.split('?')[0]));
  if (!fs.existsSync(f)) { r.writeHead(404); return r.end(); }
  r.writeHead(200, { 'Content-Type': f.endsWith('.js') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : 'application/octet-stream' });
  fs.createReadStream(f).pipe(r);
}).listen(8765);
(async () => {
  const angles = (process.argv[2] || '35').split(',').map(Number);
  const night = process.argv[3] === 'night';
  const out = process.argv[4] || 'out';
  const w = process.argv[5] || 1000, h = process.argv[6] || 560;
  fs.mkdirSync(out, { recursive: true });
  const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  const p = await b.newPage({ viewport: { width: +w, height: +h } });
  p.on('console', m => console.log('page:', m.text())); p.on('pageerror', e => console.log('ERR', e.message));
  await p.goto(`http://localhost:8765/index.html?w=${w}&h=${h}`);
  await p.waitForFunction('window.ready === true', null, { timeout: 120000 });
  for (const [i, a] of angles.entries()) {
    const d = await p.evaluate(([a, n]) => window.renderAt(a, n), [a, night]);
    fs.writeFileSync(path.join(out, `f_${String(i).padStart(3, '0')}.png`), Buffer.from(d.split(',')[1], 'base64'));
  }
  await b.close(); srv.close();
})();
