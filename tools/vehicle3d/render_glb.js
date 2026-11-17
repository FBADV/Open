// uso: node render_glb.js modelo.glb [yaw] [saida]  -> saida/day/NN.png, saida/night/NN.png, saida/top_day.png, saida/top_night.png
const { chromium } = require('playwright'); const http = require('http'); const fs = require('fs'); const path = require('path');
const model = path.resolve(process.argv[2]); const yaw = process.argv[3] || '0'; const out = process.argv[4] || 'glb_out';
const srv = http.createServer((q, r) => { let u = decodeURIComponent(q.url.split('?')[0]);
  const f = u === '/model.glb' ? model : u.startsWith('/node_modules/') ? path.join(__dirname, '..', '..', u) : path.join(__dirname, u); if (!fs.existsSync(f)) { r.writeHead(404); return r.end(); }
  r.writeHead(200, { 'Content-Type': f.endsWith('.js') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : 'application/octet-stream' }); fs.createReadStream(f).pipe(r); }).listen(8768);
(async () => {
  const W = 1500, H = 844;
  const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  const p = await b.newPage({ viewport: { width: W, height: H } }); p.on('console', m => console.log(m.text())); p.on('pageerror', e => console.log('ERR', e.message));
  await p.goto(`http://localhost:8768/glb.html?model=/model.glb&yaw=${yaw}&w=${W}&h=${H}`);
  await p.waitForFunction('window.ready === true', null, { timeout: 300000 });
  for (const mode of ['day', 'night']) {
    fs.mkdirSync(path.join(out, mode), { recursive: true });
    for (let i = 0; i < (+process.env.N || 60); i++) {
      const d = await p.evaluate(([a, n]) => window.renderAt(a, n), [(215 + i * 6) % 360, mode === 'night']);
      fs.writeFileSync(path.join(out, mode, String(i).padStart(2, '0') + '.png'), Buffer.from(d.split(',')[1], 'base64'));
    }
    const t = await p.evaluate(n => window.renderTop(n), mode === 'night');
    fs.writeFileSync(path.join(out, `top_${mode}.png`), Buffer.from(t.split(',')[1], 'base64'));
  }
  await b.close(); srv.close();
})();
