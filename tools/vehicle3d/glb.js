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
const key = new THREE.DirectionalLight(0xffffff, 2.2); key.position.set(2, 9, 4); scene.add(key);
const rimL = new THREE.DirectionalLight(0xdfe8ff, 0.8); rimL.position.set(-6, 3, -4); scene.add(rimL);
const camera = new THREE.PerspectiveCamera(24, W / H, 0.1, 100); camera.position.set(0, 2.7, 11.2); camera.lookAt(0, 0.78, 0);
const car = new THREE.Group(); scene.add(car);
let lights = [];
new GLTFLoader().load(Q.get('model'), (g) => {
  const m = g.scene;
  const box = new THREE.Box3().setFromObject(m), size = box.getSize(new THREE.Vector3());
  const s = 4.40 / Math.max(size.x, size.z);            // comprimento do Compass: 4,40 m
  m.scale.setScalar(s);
  const b2 = new THREE.Box3().setFromObject(m), c = b2.getCenter(new THREE.Vector3());
  m.position.set(-c.x, -b2.min.y, -c.z);
  const holder = new THREE.Group(); holder.add(m); holder.rotation.y = THREE.MathUtils.degToRad(YAW); car.add(holder);
  m.traverse(o => { if (o.isMesh && /light|lamp|farol|drl|led/i.test(o.name + ' ' + (o.material?.name || ''))) lights.push(o.material); });
  // sombra de contato
  const cv = document.createElement('canvas'); cv.width = cv.height = 256; const x = cv.getContext('2d');
  const gr = x.createRadialGradient(128, 128, 8, 128, 128, 128); gr.addColorStop(0, 'rgba(0,0,0,0.9)'); gr.addColorStop(0.5, 'rgba(0,0,0,0.5)'); gr.addColorStop(1, 'rgba(0,0,0,0)');
  x.fillStyle = gr; x.fillRect(0, 0, 256, 256);
  const blob = new THREE.Mesh(new THREE.PlaneGeometry(5.0, 2.4), new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(cv), transparent: true, depthWrite: false }));
  blob.rotation.x = -Math.PI / 2; blob.position.y = 0.002; car.add(blob);
  window.ready = true;
}, undefined, e => { console.log('ERRO ao carregar', e?.message); });
function setNight(n) {
  renderer.toneMappingExposure = n ? 0.8 : 1.0; scene.environmentIntensity = n ? 0.45 : 1.0;
  for (const mt of lights) if (mt.emissive) { mt.emissive.set(0xffffff); mt.emissiveIntensity = n ? 4 : 0.6; }
}
window.renderAt = (deg, night) => { car.rotation.y = THREE.MathUtils.degToRad(deg); setNight(night); renderer.render(scene, camera); return renderer.domElement.toDataURL('image/png'); };
window.renderTop = (night) => {
  const cam = new THREE.PerspectiveCamera(20, W / H, 0.1, 100); cam.position.set(0, 16, 0.0001); cam.up.set(-1, 0, 0); cam.lookAt(0, 0, 0);
  car.rotation.y = 0; setNight(night); renderer.render(scene, cam); return renderer.domElement.toDataURL('image/png');
};
