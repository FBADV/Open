import * as THREE from 'three';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';

const Q = new URLSearchParams(location.search);
const W = +(Q.get('w') || 1000), H = +(Q.get('h') || 560);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
renderer.setSize(W, H); renderer.setPixelRatio(1);
renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.0;
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.shadowMap.enabled = false; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.setClearColor(0x000000, 0);
document.body.appendChild(renderer.domElement);
const scene = new THREE.Scene();
const pmrem = new THREE.PMREMGenerator(renderer);
scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.03).texture;
const camera = new THREE.PerspectiveCamera(24, W / H, 0.1, 100);
camera.position.set(0, 2.7, 11.2); camera.lookAt(0, 0.78, 0);

const M = {
  paint: new THREE.MeshPhysicalMaterial({ color: 0xf2f2ef, roughness: 0.2, clearcoat: 1, clearcoatRoughness: 0.03, envMapIntensity: 1.3 }),
  roof: new THREE.MeshPhysicalMaterial({ color: 0x08090b, roughness: 0.45, metalness: 0.1, clearcoat: 0.25, clearcoatRoughness: 0.4, envMapIntensity: 0.12 }),
  glass: new THREE.MeshBasicMaterial({ color: 0x0b0d11, side: THREE.DoubleSide }),
  matte: new THREE.MeshStandardMaterial({ color: 0x151618, roughness: 0.78 }),
  gloss: new THREE.MeshPhysicalMaterial({ color: 0x0a0b0d, roughness: 0.15, clearcoat: 1 }),
  chrome: new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.07, metalness: 1 }),
  rubber: new THREE.MeshStandardMaterial({ color: 0x111111, roughness: 0.9 }),
  rim: new THREE.MeshStandardMaterial({ color: 0xd4d7db, roughness: 0.2, metalness: 1 }),
  rimDark: new THREE.MeshStandardMaterial({ color: 0x24272b, roughness: 0.45, metalness: 0.7 }),
  mesh: new THREE.MeshStandardMaterial({ color: 0x0c0d0f, roughness: 0.55, metalness: 0.4 }),
  lens: new THREE.MeshPhysicalMaterial({ color: 0x101318, roughness: 0.03, metalness: 0.5, clearcoat: 1, side: THREE.DoubleSide }),
  drl: new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: 0xe8f3ff, emissiveIntensity: 1.4, side: THREE.DoubleSide }),
  tail: new THREE.MeshPhysicalMaterial({ color: 0x6e0a0e, roughness: 0.1, clearcoat: 1, emissive: 0x300000, emissiveIntensity: 0.5, side: THREE.DoubleSide }),
  amber: new THREE.MeshStandardMaterial({ color: 0xd88b20, emissive: 0x402000, side: THREE.DoubleSide }),
  plate: new THREE.MeshStandardMaterial({ color: 0xf0f0f0, roughness: 0.5 }),
  blue: new THREE.MeshStandardMaterial({ color: 0x1d3f9a, roughness: 0.5 }),
};
const car = new THREE.Group(); scene.add(car);
let castAll = true;
const add = (g, m, p = car) => { const o = new THREE.Mesh(g, m); o.castShadow = castAll; o.receiveShadow = true; p.add(o); return o; };
const S = (e0, e1, x) => { const t = Math.min(1, Math.max(0, (x - e0) / (e1 - e0))); return t * t * (3 - 2 * t); };
const lerp = (a, b, t) => a + (b - a) * t;

// ===== dimensões (Compass: 4,40 x 1,82 x 1,64 m; entre-eixos 2,64; pneu 235/45R19) =====
const XF = 2.22, XR = -2.18, AX = 1.32, WR = 0.347, AR = 0.43;
// meia-largura em planta (afunila e arredonda nas pontas)
function hw(x) {
  const mid = 0.905;
  if (x > 1.55) { const t = (x - 1.55) / (XF - 1.55); return mid * Math.pow(1 - Math.pow(t, 3.2), 1 / 3.2) * (1 - 0.06 * t) + 0.0 * t; }
  if (x < -1.65) { const t = (-1.65 - x) / (-1.65 - XR); return mid * Math.pow(1 - Math.pow(t, 4), 1 / 4) * (1 - 0.04 * t); }
  return mid;
}
// topo da carroceria inferior (capô / cintura / tampa traseira)
function yTop(x) {
  if (x > 1.05) return lerp(0.99, 0.925, S(1.05, 2.05, x)) - 0.05 * S(2.02, XF, x);
  if (x < -1.95) return 1.06 - 0.05 * S(-2.05, XR, x);
  return lerp(1.055, 0.985, S(-1.95, 1.05, x));
}
// base: sobe sobre as rodas (abre as caixas de roda) e nas pontas
function yBot(x) {
  let y = 0.30 + 0.12 * S(1.85, XF, x) + 0.14 * S(-1.85, XR, x);
  for (const ax of [AX, -AX]) { const d = Math.abs(x - ax); if (d < AR) y = Math.max(y, WR + 0.02 + Math.sqrt(AR * AR - d * d)); }
  return y;
}
// seção transversal da carroceria (z,y) — retângulo arredondado com ombro e vinco
function bodySection(x, n = 56) {
  const w = hw(x), yt = yTop(x), yb = yBot(x);
  const pts = [];
  const h = yt - yb;
  for (let i = 0; i <= n; i++) {
    const t = i / n;                 // 0 = base lado -z ... 0.5 = topo ... 1 = base lado +z
    const a = Math.PI * (t - 0.5);   // -90..90 graus
    // superelipse: lados quase retos, cantos arredondados
    const c = Math.cos(a), s = Math.sin(a);
    const pw = 0.22;
    const zz = Math.sign(s) * Math.pow(Math.abs(s), pw);
    const yy = Math.pow(Math.abs(c), 0.30);
    let z = w * zz, y = yb + h * (0.18 + 0.82 * yy);
    pts.push([z, y]);
  }
  // fundo plano
  return [[-w * 0.94, yb], ...pts, [w * 0.94, yb]];
}
function loft(xs, sectionFn, mat, closeEnds = true) {
  const rings = xs.map(x => sectionFn(x).map(([z, y]) => new THREE.Vector3(x, y, z)));
  const m = rings[0].length, pos = [], idx = [];
  rings.forEach(r => r.forEach(v => pos.push(v.x, v.y, v.z)));
  for (let i = 0; i < rings.length - 1; i++) for (let j = 0; j < m - 1; j++) {
    const a = i * m + j, b = a + 1, c = a + m, d = c + 1;
    idx.push(a, b, c, b, d, c);
  }
  if (closeEnds) for (const [ri, flip] of [[0, true], [rings.length - 1, false]]) {
    const r = rings[ri]; const cx = r.reduce((s, v) => s + v.x, 0) / m, cy = r.reduce((s, v) => s + v.y, 0) / m;
    const ci = pos.length / 3; pos.push(cx, cy, 0);
    for (let j = 0; j < m - 1; j++) { const a = ri * m + j, b = a + 1; flip ? idx.push(ci, a, b) : idx.push(ci, b, a); }
  }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setIndex(idx); g.computeVertexNormals();
  return add(g, mat);
}
const xsBody = []; for (let i = 0; i <= 160; i++) { const t = i / 160; xsBody.push(lerp(XR, XF, 0.5 - 0.5 * Math.cos(Math.PI * t))); }
loft(xsBody, x => bodySection(x), M.paint);

// ===== estufa (teto preto, colunas pretas) =====
const GX0 = 1.10, GX1 = -2.10;
function roofY(x) {
  if (x > 0.05) return lerp(1.585, yTop(GX0) - 0.005, Math.pow(S(0.05, GX0, x), 0.95));                 // para-brisa
  if (x < -1.45) return lerp(1.59, 1.065, Math.pow(S(-1.45, GX1, x), 1.15));                             // vidro traseiro
  return 1.585 + 0.035 * Math.sin(Math.PI * (x - 0.05) / (-1.5));                                        // teto arqueado
}
function ghSection(x, n = 40) {
  const yb = yTop(x) - 0.01, yt = roofY(x), wb = hw(x) - 0.075, wt = wb - 0.23;
  const pts = [];
  for (let i = 0; i <= n; i++) {
    const t = i / n, a = Math.PI * (t - 0.5), s = Math.sin(a), c = Math.cos(a);
    const yy = Math.pow(Math.abs(c), 0.22), zz = Math.sign(s) * Math.pow(Math.abs(s), 0.35);
    const y = lerp(yb, yt, yy);
    const k = (y - yb) / Math.max(yt - yb, 1e-3);
    pts.push([zz * lerp(wb, wt, k), y]);
  }
  return pts;
}
const xsGh = []; for (let i = 0; i <= 120; i++) xsGh.push(lerp(GX1, GX0, i / 120));
loft(xsGh, x => ghSection(x), M.roof);
castAll = false;
// largura da estufa num ponto (x,y) — para colar vidros e frisos
function ghZ(x, y) {
  const yb = yTop(x) - 0.01, yt = roofY(x), wb = hw(x) - 0.075, wt = wb - 0.23;
  const k = Math.min(1, Math.max(0, (y - yb) / Math.max(yt - yb, 1e-3)));
  const c = Math.pow(k, 1 / 0.22), sn = Math.sqrt(Math.max(0, 1 - c * c));
  return Math.pow(sn, 0.35) * lerp(wb, wt, k);
}

// vidros laterais + friso cromado (contorno DLO do Limited)
function sideGlass(sign) {
  const out = (x, y, d = 0.006) => new THREE.Vector3(x, y, sign * (ghZ(x, y) + d));
  // contorno: base da janela -> coluna A -> teto -> coluna D
  const c = [];
  const yb = (x) => yTop(x) + 0.045;
  for (let i = 0; i <= 30; i++) { const x = lerp(-1.80, 0.92, i / 30); c.push([x, yb(x)]); }
  for (let i = 1; i <= 16; i++) { const t = i / 16; c.push([lerp(0.92, 0.08, t), lerp(yb(0.92), 1.515, Math.pow(t, 0.85))]); }
  for (let i = 1; i <= 20; i++) { const t = i / 20; c.push([lerp(0.08, -1.38, t), 1.515 + 0.02 * Math.sin(Math.PI * t)]); }
  for (let i = 1; i <= 12; i++) { const t = i / 12; c.push([lerp(-1.38, -1.80, t), lerp(1.515, yb(-1.80), Math.pow(t, 1.4))]); }
  // malha densa dentro do contorno (acompanha a curvatura da estufa)
  const inside = (x, y) => { let r = false; for (let i = 0, j = c.length - 1; i < c.length; j = i++) { const [xi, yi] = c[i], [xj, yj] = c[j]; if (((yi > y) !== (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) r = !r; } return r; };
  const xs0 = Math.min(...c.map(v => v[0])), xs1 = Math.max(...c.map(v => v[0])), ys0 = Math.min(...c.map(v => v[1])), ys1 = Math.max(...c.map(v => v[1]));
  const NX = 220, NY = 70, gp = [], gi = [], ins = [];
  for (let j = 0; j <= NY; j++) for (let i = 0; i <= NX; i++) {
    const x = lerp(xs0, xs1, i / NX), y = lerp(ys0, ys1, j / NY); const v = out(x, y, 0.012);
    gp.push(v.x, v.y, v.z); ins.push(inside(x, y));
  }
  const id = (i, j) => j * (NX + 1) + i;
  for (let j = 0; j < NY; j++) for (let i = 0; i < NX; i++) {
    const a = id(i, j), b = id(i + 1, j), cc = id(i, j + 1), d = id(i + 1, j + 1);
    if (ins[a] && ins[b] && ins[cc] && ins[d]) { gi.push(a, b, cc, b, d, cc); }
  }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.Float32BufferAttribute(gp, 3)); g.setIndex(gi);
  add(g, M.glass);
  add(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(c.map(([x, y]) => out(x, y, 0.02)), true), 300, 0.014, 8, true), M.chrome);
  // colunas B e C em preto brilhante (faixas densas sobre o vidro)
  for (const [x0, w] of [[-0.34, 0.11], [-1.19, 0.12]]) {
    const pos = [], idx = []; const NY = 30, NX = 4;
    for (let j = 0; j <= NY; j++) for (let i = 0; i <= NX; i++) {
      const y = lerp(yb(x0) - 0.01, 1.53, j / NY); const x = x0 - 0.03 * (j / NY) + lerp(-w / 2, w / 2, i / NX);
      const v = out(x, y, 0.017); pos.push(v.x, v.y, v.z);
    }
    for (let j = 0; j < NY; j++) for (let i = 0; i < NX; i++) { const a = j * (NX + 1) + i; idx.push(a, a + 1, a + NX + 1, a + 1, a + NX + 2, a + NX + 1); }
    const bg = new THREE.BufferGeometry(); bg.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); bg.setIndex(idx); bg.computeVertexNormals();
    add(bg, new THREE.MeshBasicMaterial({ color: 0x0d0e11, side: THREE.DoubleSide }));
  }
}
sideGlass(1); sideGlass(-1);

// ===== superfícies auxiliares coladas na carroceria =====
// x da face frontal/traseira na largura z
function faceX(z, front) {
  let lo = front ? 0 : XR, hi = front ? XF : 0;
  for (let k = 0; k < 40; k++) { const m = (lo + hi) / 2; const ok = hw(m) >= Math.abs(z); if (front) { if (ok) lo = m; else hi = m; } else { if (ok) hi = m; else lo = m; } }
  return front ? lo : hi;
}
// faixa na face (frente/traseira) entre alturas y0..y1 e larguras z0..z1
function faceStrip(z0, z1, y0, y1, mat, front, d = 0.006, n = 40) {
  const pos = [], idx = [];
  for (let i = 0; i <= n; i++) {
    const z = lerp(z0, z1, i / n); const x = faceX(z, front) + (front ? d : -d);
    pos.push(x, y0, z, x, y1, z);
    if (i < n) { const a = i * 2; idx.push(a, a + 1, a + 2, a + 1, a + 3, a + 2); }
  }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setIndex(idx); g.computeVertexNormals();
  return add(g, mat);
}
// faixa na lateral (z = hw) entre x0..x1 e y0..y1
function sideStrip(x0, x1, y0, y1, mat, sign, d = 0.004, n = 60) {
  const pos = [], idx = [];
  for (let i = 0; i <= n; i++) {
    const x = lerp(x0, x1, i / n); const z = sign * (hw(x) + d);
    pos.push(x, y0, z, x, y1, z);
    if (i < n) { const a = i * 2; idx.push(a, a + 1, a + 2, a + 1, a + 3, a + 2); }
  }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setIndex(idx); g.computeVertexNormals();
  return add(g, mat);
}

// ===== frente =====
// faixa preta superior (entre faróis), faróis com DRL em "C", grade de 7 fendas
faceStrip(-0.80, 0.80, 0.745, 0.835, M.gloss, true, 0.004);
faceStrip(-0.40, 0.40, 0.455, 0.745, M.gloss, true, 0.004);
for (const s of [1, -1]) {
  faceStrip(s * 0.40, s * 0.86, 0.745, 0.83, M.lens, true, 0.008);
  faceStrip(s * 0.42, s * 0.86, 0.815, 0.827, M.drl, true, 0.011).userData.drl = true;
  faceStrip(s * 0.80, s * 0.86, 0.75, 0.80, M.amber, true, 0.012);
  // projetor
  const zp = s * 0.56; const pj = add(new THREE.CylinderGeometry(0.028, 0.028, 0.02, 24).rotateZ(Math.PI / 2), M.chrome); pj.position.set(faceX(zp, true) + 0.012, 0.782, zp);
  // neblina e moldura
  faceStrip(s * 0.52, s * 0.70, 0.43, 0.48, M.lens, true, 0.008);
  faceStrip(s * 0.52, s * 0.70, 0.475, 0.484, M.drl, true, 0.011);
}
{
  const gap = 0.103, sw = 0.076, sh = 0.235, yc = 0.60;
  for (let i = -3; i <= 3; i++) {
    const z = i * gap, x = faceX(z, true);
    const r = new THREE.Shape(); const w = sw / 2, h = sh / 2, q = 0.024;
    r.moveTo(-w + q, -h); r.lineTo(w - q, -h); r.quadraticCurveTo(w, -h, w, -h + q); r.lineTo(w, h - q); r.quadraticCurveTo(w, h, w - q, h); r.lineTo(-w + q, h); r.quadraticCurveTo(-w, h, -w, h - q); r.lineTo(-w, -h + q); r.quadraticCurveTo(-w, -h, -w + q, -h);
    const hole = new THREE.Path(); const w2 = w - 0.011, h2 = h - 0.011; hole.moveTo(-w2, -h2); hole.lineTo(w2, -h2); hole.lineTo(w2, h2); hole.lineTo(-w2, h2); hole.lineTo(-w2, -h2); r.holes.push(hole);
    const fr = add(new THREE.ExtrudeGeometry(r, { depth: 0.02, bevelEnabled: true, bevelSize: 0.003, bevelThickness: 0.003, bevelSegments: 2 }), M.chrome);
    fr.rotation.y = Math.PI / 2; fr.position.set(x - 0.004, yc, z);
    const ms = add(new THREE.PlaneGeometry(sw - 0.02, sh - 0.02), M.mesh); ms.rotation.y = Math.PI / 2; ms.position.set(x + 0.002, yc, z);
  }
}
// para-choque inferior preto, entrada de ar, friso cromado, placa
faceStrip(-0.80, 0.80, yBot(XF - 0.05) - 0.02, 0.40, M.matte, true, 0.004);
faceStrip(-0.45, 0.45, 0.40, 0.49, M.mesh, true, 0.006);
faceStrip(-0.58, 0.58, 0.318, 0.33, M.chrome, true, 0.008);
faceStrip(-0.19, 0.19, 0.405, 0.525, M.plate, true, 0.012);
faceStrip(-0.19, 0.19, 0.50, 0.525, M.blue, true, 0.014);

// ===== traseira =====
for (const s of [1, -1]) {
  faceStrip(s * 0.30, s * 0.90, 0.90, 0.99, M.tail, false, 0.008);
  faceStrip(s * 0.32, s * 0.88, 0.944, 0.952, M.drl, false, 0.011).material = M.tail;
}
faceStrip(-0.30, 0.30, 0.91, 0.98, M.gloss, false, 0.008);
faceStrip(-0.85, 0.85, yBot(XR + 0.05) - 0.02, 0.48, M.matte, false, 0.004);
faceStrip(-0.18, 0.18, 0.62, 0.74, M.plate, false, 0.01);
faceStrip(-0.55, 0.55, 0.43, 0.442, M.chrome, false, 0.008);

// ===== laterais: soleira, caixas de roda, frisos, maçanetas, vincos de porta =====
for (const s of [1, -1]) {
  sideStrip(-AX + AR + 0.02, AX - AR - 0.02, 0.30, 0.43, M.matte, s, 0.006);
  sideStrip(-AX + AR + 0.06, AX - AR - 0.06, 0.428, 0.438, M.chrome, s, 0.009);
  for (const ax of [AX, -AX]) {
    // alargador: faixa em arco acompanhando a caixa de roda
    const pos = [], idx = []; const n = 48;
    for (let i = 0; i <= n; i++) {
      const a = Math.PI * i / n; const ci = Math.cos(a), si = Math.sin(a);
      const r0 = AR + 0.005, r1 = AR + 0.075;
      for (const r of [r0, r1]) { const x = ax + r * ci, y = WR + 0.02 + r * si; pos.push(x, y, s * (hw(x) + 0.012)); }
      if (i < n) { const b = i * 2; idx.push(b, b + 1, b + 2, b + 1, b + 3, b + 2); }
    }
    const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setIndex(idx); g.computeVertexNormals();
    add(g, M.matte);
    // fundo escuro da caixa de roda
  }
  for (const dx of [0.90, -0.33, -1.30]) {
    const pts = []; for (let i = 0; i <= 20; i++) { const y = lerp(0.45, yTop(dx) + 0.02, i / 20); pts.push(new THREE.Vector3(dx - 0.02 * (i / 20), y, s * (hw(dx) + 0.002))); }
    add(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts), 20, 0.0035, 6), M.gloss);
  }
  for (const hx of [0.18, -0.92]) { const h = add(new THREE.BoxGeometry(0.17, 0.026, 0.02), M.paint); h.position.set(hx, 0.92, s * (hw(hx) + 0.006)); }
  // retrovisor
  const mg = new THREE.SphereGeometry(0.105, 28, 18); mg.scale(1.25, 0.72, 0.8);
  const mm = add(mg, M.roof); mm.position.set(0.74, 1.10, s * (hw(0.74) + 0.06));
  const arm = add(new THREE.BoxGeometry(0.10, 0.035, 0.09), M.roof); arm.position.set(0.80, 1.06, s * (hw(0.8) + 0.0));
  // rack de teto
  const rp = []; for (let i = 0; i <= 20; i++) { const x = lerp(-1.45, 0.05, i / 20); rp.push(new THREE.Vector3(x, roofY(x) + 0.018, s * (ghZ(x, roofY(x)) - 0.05))); }
  add(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(rp), 40, 0.013, 10), M.gloss);
}

// ===== rodas aro 19 =====
function wheel() {
  const g = new THREE.Group();
  const tw = 0.235 / 2, ri = 0.243, ro = WR, prof = [];
  for (let i = 0; i <= 28; i++) { const a = -Math.PI / 2 + Math.PI * i / 28; prof.push(new THREE.Vector2(ri + (ro - ri) * (0.5 + 0.5 * Math.pow(Math.max(0, Math.cos(a)), 0.35)), tw * Math.sin(a))); }
  add(new THREE.LatheGeometry(prof, 80).rotateX(Math.PI / 2), M.rubber, g);
  add(new THREE.CylinderGeometry(ri, ri, 0.2, 64, 1, true).rotateX(Math.PI / 2), M.rimDark, g);
  const dish = add(new THREE.CylinderGeometry(ri - 0.006, ri - 0.006, 0.01, 64).rotateX(Math.PI / 2), M.rimDark, g); dish.position.z = 0.05;
  const lip = add(new THREE.TorusGeometry(ri - 0.006, 0.011, 10, 80), M.rim, g); lip.position.z = 0.1;
  const hub = add(new THREE.CylinderGeometry(0.065, 0.07, 0.05, 32).rotateX(Math.PI / 2), M.rim, g); hub.position.z = 0.09;
  for (let k = 0; k < 10; k++) for (const off of [-0.07, 0.07]) {
    const sp = new THREE.BoxGeometry(0.18, 0.024, 0.026); sp.translate(0.062 + 0.09, 0, 0);
    const m = add(sp, M.rim, g); m.rotation.z = (k / 10) * Math.PI * 2 + off; m.position.z = 0.088;
  }
  const cap = add(new THREE.CylinderGeometry(0.034, 0.034, 0.012, 24).rotateX(Math.PI / 2), M.roof, g); cap.position.z = 0.118;
  const disc = add(new THREE.CylinderGeometry(0.165, 0.165, 0.02, 48).rotateX(Math.PI / 2), M.rimDark, g); disc.position.z = -0.02;
  return g;
}
castAll = true;
for (const ax of [AX, -AX]) for (const s of [1, -1]) {
  const w = wheel(); w.position.set(ax, WR, s * (hw(ax) - 0.10)); if (s < 0) w.rotation.y = Math.PI; car.add(w);
}

// ===== luz e sombra =====
scene.add(new THREE.HemisphereLight(0xffffff, 0x2a2a2a, 0.35));
const key = new THREE.DirectionalLight(0xffffff, 2.4); key.position.set(2, 9, 4); key.castShadow = true;
key.shadow.mapSize.set(2048, 2048); Object.assign(key.shadow.camera, { left: -4, right: 4, top: 4, bottom: -4 }); key.shadow.radius = 10; key.shadow.blurSamples = 24; key.shadow.bias = -0.0006; key.shadow.normalBias = 0.03;
scene.add(key);
const rim = new THREE.DirectionalLight(0xdfe8ff, 0.8); rim.position.set(-6, 3, -4); scene.add(rim);
const ground = new THREE.Mesh(new THREE.PlaneGeometry(16, 16), new THREE.ShadowMaterial({ opacity: 0.35 })); ground.rotation.x = -Math.PI / 2; ground.receiveShadow = true; scene.add(ground);
{
  const c = document.createElement('canvas'); c.width = c.height = 256; const x = c.getContext('2d');
  const gr = x.createRadialGradient(128, 128, 8, 128, 128, 128); gr.addColorStop(0, 'rgba(0,0,0,0.9)'); gr.addColorStop(0.5, 'rgba(0,0,0,0.5)'); gr.addColorStop(1, 'rgba(0,0,0,0)');
  x.fillStyle = gr; x.fillRect(0, 0, 256, 256);
  const blob = new THREE.Mesh(new THREE.PlaneGeometry(5.0, 2.4), new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(c), transparent: true, depthWrite: false }));
  blob.rotation.x = -Math.PI / 2; blob.position.y = 0.002; car.add(blob);
  // sombra de contato de cada pneu
  for (const ax of [AX, -AX]) for (const sz of [1, -1]) {
    const b = new THREE.Mesh(new THREE.PlaneGeometry(0.75, 0.38), new THREE.MeshBasicMaterial({ map: blob.material.map, transparent: true, opacity: 0.9, depthWrite: false }));
    b.rotation.x = -Math.PI / 2; b.position.set(ax, 0.003, sz * (hw(ax) - 0.10)); car.add(b);
  }
}
const beams = new THREE.Group(); car.add(beams);
for (const s of [1, -1]) {
  const c = document.createElement('canvas'); c.width = c.height = 128; const x = c.getContext('2d');
  const gr = x.createRadialGradient(64, 64, 2, 64, 64, 64); gr.addColorStop(0, 'rgba(240,248,255,1)'); gr.addColorStop(0.2, 'rgba(200,225,255,0.6)'); gr.addColorStop(1, 'rgba(200,225,255,0)');
  x.fillStyle = gr; x.fillRect(0, 0, 128, 128);
  const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: new THREE.CanvasTexture(c), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending }));
  sp.scale.set(0.38, 0.38, 1); sp.position.set(XF + 0.06, 0.79, s * 0.60); beams.add(sp);
}
window.renderAt = (deg, night) => {
  car.rotation.y = THREE.MathUtils.degToRad(deg);
  beams.visible = !!night;
  M.drl.emissiveIntensity = night ? 5 : 1.4;
  M.tail.emissive.set(night ? 0xff1010 : 0x300000); M.tail.emissiveIntensity = night ? 2.2 : 0.5;
  renderer.toneMappingExposure = night ? 0.8 : 1.0;
  scene.environmentIntensity = night ? 0.45 : 1.0;
  renderer.render(scene, camera);
  return renderer.domElement.toDataURL('image/png');
};
window.__car = car;
window.ready = true;

// vista de cima: câmera acima do carro, frente apontando para o topo da imagem
window.renderTop = (night) => {
  const cam = new THREE.PerspectiveCamera(20, renderer.domElement.width / renderer.domElement.height, 0.1, 100);
  cam.position.set(0, 16, 0.0001); cam.up.set(-1, 0, 0); cam.lookAt(0, 0, 0);
  car.rotation.y = 0;
  beams.visible = !!night;
  M.drl.emissiveIntensity = night ? 5 : 1.4;
  M.tail.emissive.set(night ? 0xff1010 : 0x300000); M.tail.emissiveIntensity = night ? 2.2 : 0.5;
  renderer.toneMappingExposure = night ? 0.8 : 1.0; scene.environmentIntensity = night ? 0.45 : 1.0;
  renderer.render(scene, cam);
  return renderer.domElement.toDataURL('image/png');
};
