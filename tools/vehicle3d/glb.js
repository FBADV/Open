// Renderiza um modelo 3D (.glb/.gltf) do veículo com a iluminação do launcher:
// 360° (dia/noite) e vista de cima. Parâmetros: ?model=arquivo.glb&yaw=graus&w=&h=
import * as THREE from 'three';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
const Q = new URLSearchParams(location.search);
const W = +(Q.get('w') || 1500), H = +(Q.get('h') || 844), YAW = +(Q.get('yaw') || 0);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
renderer.setSize(W, H); renderer.setPixelRatio(1);
renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.outputColorSpace = THREE.SRGBColorSpace; renderer.setClearColor(0, 0);
document.body.appendChild(renderer.domElement);
const scene = new THREE.Scene();
scene.environment = new THREE.PMREMGenerator(renderer).fromScene(new RoomEnvironment(), 0.03).texture;
scene.add(new THREE.HemisphereLight(0xffffff, 0x2a2a2a, 0.35));
const key = new THREE.DirectionalLight(0xffffff, 2.6); key.position.set(3, 9, 5); scene.add(key);
const fill = new THREE.DirectionalLight(0xffffff, 0.7); fill.position.set(-5, 4, 6); scene.add(fill);
const rimL = new THREE.DirectionalLight(0xdfe8ff, 0.8); rimL.position.set(-6, 3, -4); scene.add(rimL);
const camera = new THREE.PerspectiveCamera(24, W / H, 0.1, 100); camera.position.set(0, 2.7, 11.2); camera.lookAt(0, 0.78, 0);
const car = new THREE.Group(); scene.add(car);
const heads = [], tails = [], glows = [];
new GLTFLoader().load(Q.get('model'), (g) => {
  const m = g.scene;
  const box = new THREE.Box3().setFromObject(m), size = box.getSize(new THREE.Vector3());
  const s = 4.40 / Math.max(size.x, size.z);            // comprimento do Compass: 4,40 m
  m.scale.setScalar(s);
  const b2 = new THREE.Box3().setFromObject(m), c = b2.getCenter(new THREE.Vector3());
  m.position.set(-c.x, -b2.min.y, -c.z);
  const holder = new THREE.Group(); holder.add(m); holder.rotation.y = THREE.MathUtils.degToRad(YAW); car.add(holder);
  // ---- faróis/lanternas: materiais indicados em ?lamps= (ou por nome), divididos frente/trás ----
  const lampNames = (Q.get('lamps') || '').split('|').filter(Boolean);
  const isLamp = (mt) => lampNames.includes(mt.name) || /light|lamp|farol|drl|led/i.test(mt.name);
  const front = { x: 0, z: 1 };                 // frente do modelo em +z (antes do yaw)
  const headPts = [];
  m.updateMatrixWorld(true);
  m.traverse(o => {
    if (!o.isMesh) return;
    const mats = Array.isArray(o.material) ? o.material.slice() : [o.material];
    const g = o.geometry; if (!g.index) return;
    const groups = g.groups.length ? g.groups : [{ start: 0, count: g.index.count, materialIndex: 0 }];
    const pos = g.attributes.position, v = new THREE.Vector3();
    const newIdx = [], newGroups = [];
    for (const gr of groups) {
      const mt = mats[gr.materialIndex || 0];
      const tris = []; for (let i = gr.start; i < gr.start + gr.count; i += 3) tris.push([g.index.getX(i), g.index.getX(i + 1), g.index.getX(i + 2)]);
      if (!isLamp(mt)) { const st = newIdx.length; tris.forEach(t => newIdx.push(...t)); newGroups.push({ start: st, count: tris.length * 3, materialIndex: gr.materialIndex || 0 }); continue; }
      const head = mt.clone(); head.name = 'farol'; const tail = mt.clone(); tail.name = 'lanterna';
      const hi = mats.push(head) - 1, ti = mats.push(tail) - 1;
      const fr = [], bk = [];
      for (const t of tris) {
        let cz = 0, cy = 0, cx = 0; for (const k of t) { v.fromBufferAttribute(pos, k).applyMatrix4(o.matrixWorld); cz += v.z / 3; cy += v.y / 3; cx += v.x / 3; }
        if (cz * front.z > 0) { fr.push(t); headPts.push([cx, cy, cz]); } else bk.push(t);
      }
      let st = newIdx.length; fr.forEach(t => newIdx.push(...t)); newGroups.push({ start: st, count: fr.length * 3, materialIndex: hi });
      st = newIdx.length; bk.forEach(t => newIdx.push(...t)); newGroups.push({ start: st, count: bk.length * 3, materialIndex: ti });
      heads.push(head); tails.push(tail);
    }
    g.setIndex(newIdx); g.clearGroups(); newGroups.forEach(x => g.addGroup(x.start, x.count, x.materialIndex)); o.material = mats;
  });
  // ---- vidros com película escura (como no modelo de referência) ----
  const glassRe = new RegExp(Q.get('glass') || 'pel[ií]cula|glass|vidro', 'i');
  m.traverse(o => { if (!o.isMesh) return; (Array.isArray(o.material) ? o.material : [o.material]).forEach(mt => {
    if (glassRe.test(mt.name)) { mt.transparent = false; mt.opacity = 1; if ('transmission' in mt) mt.transmission = 0; mt.color?.set(0x0b0f13); mt.metalness = 0.15; mt.roughness = 0.04; mt.envMapIntensity = 1.2; }
  }); });
  // ---- brilho dos faróis (sprites nos centros dos dois faróis) ----
  const fronts = headPts.filter(p => p[2] > 0); 
  if (fronts.length) {
    const maxz = Math.max(...fronts.map(p => p[2]));
    const tip = fronts.filter(p => p[2] > maxz - 0.35);
    for (const side of [1, -1]) {
      const pts = tip.filter(p => p[0] * side > 0.2); if (!pts.length) continue;
      const c = pts.reduce((a, p) => [a[0] + p[0] / pts.length, a[1] + p[1] / pts.length, a[2] + p[2] / pts.length], [0, 0, 0]);
      const cv = document.createElement('canvas'); cv.width = cv.height = 128; const x = cv.getContext('2d');
      const gr = x.createRadialGradient(64, 64, 2, 64, 64, 64); gr.addColorStop(0, 'rgba(245,250,255,1)'); gr.addColorStop(0.18, 'rgba(210,230,255,0.65)'); gr.addColorStop(1, 'rgba(200,225,255,0)');
      x.fillStyle = gr; x.fillRect(0, 0, 128, 128);
      const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: new THREE.CanvasTexture(cv), transparent: true, depthWrite: false, depthTest: true, blending: THREE.AdditiveBlending }));
      sp.scale.set(0.30, 0.30, 1); sp.position.set(c[0], c[1], c[2] + 0.06); m.add(sp); glows.push(sp);
    }
  }
  // sombra de contato
  const cv = document.createElement('canvas'); cv.width = cv.height = 256; const x = cv.getContext('2d');
  const gr = x.createRadialGradient(128, 128, 8, 128, 128, 128); gr.addColorStop(0, 'rgba(0,0,0,0.9)'); gr.addColorStop(0.5, 'rgba(0,0,0,0.5)'); gr.addColorStop(1, 'rgba(0,0,0,0)');
  x.fillStyle = gr; x.fillRect(0, 0, 256, 256);
  const blob = new THREE.Mesh(new THREE.PlaneGeometry(5.0, 2.4), new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(cv), transparent: true, depthWrite: false }));
  blob.rotation.x = -Math.PI / 2; blob.position.y = 0.002; car.add(blob);
  window.__model = m;
  window.ready = true;
}, undefined, e => { console.log('ERRO ao carregar', e?.message); });
function setNight(n) {
  renderer.toneMappingExposure = n ? 0.72 : 1.0; scene.environmentIntensity = n ? 0.28 : 1.0;
  key.intensity = n ? 0.7 : 2.6; fill.intensity = n ? 0.25 : 0.7;
  for (const mt of heads) { mt.emissive?.set(n ? 0xeaf4ff : 0x000000); mt.emissiveIntensity = n ? 3.2 : 0; }
  for (const mt of tails) { mt.emissive?.set(n ? 0xff1a14 : 0x000000); mt.emissiveIntensity = n ? 2.4 : 0; if (n) mt.color?.set(0x7a0a0a); }
  for (const g of glows) g.visible = !!n;
}
window.renderAt = (deg, night) => { car.rotation.y = THREE.MathUtils.degToRad(deg); setNight(night); renderer.render(scene, camera); return renderer.domElement.toDataURL('image/png'); };
window.renderTop = (night) => {
  const cam = new THREE.PerspectiveCamera(20, W / H, 0.1, 100); cam.position.set(0, 16, 0.0001); cam.up.set(-1, 0, 0); cam.lookAt(0, 0, 0);
  car.rotation.y = 0; setNight(night); renderer.render(scene, cam); return renderer.domElement.toDataURL('image/png');
};
