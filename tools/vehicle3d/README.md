# Veículo 3D do launcher

O palco usa quadros pré-renderizados (72 posições de dia, 72 de noite com faróis acesos
e a vista de cima) em `app/src/main/assets/car360/`.

## Trocar pelo seu modelo 3D (.glb / .gltf)

```bash
npm install three@0.169.0                     # na raiz do repositório
node tools/vehicle3d/render_glb.js meu_compass.glb 0 /tmp/render   # 0 = giro extra (graus) se a frente não ficar à esquerda
python3 tools/vehicle3d/package_frames.py /tmp/render
./gradlew assembleRelease
```

`render_glb.js` ajusta a escala para 4,40 m (Compass), apoia o carro no piso, aplica a
iluminação de estúdio do launcher e acende no modo noite os materiais com nome
`light`, `lamp`, `farol`, `drl` ou `led`.

## Trocar por imagens prontas

Gere 72 PNGs com fundo transparente (câmera ¾ elevada ~15°, giro de 5° entre quadros,
começando de frente-esquerda), coloque em `day/` (e opcionalmente `night/`), mais
`top_day.png`/`top_night.png`, e rode `package_frames.py`. Também é possível importar
pelo próprio app: *Configurações → Veículo → Fotos 360° / Vista de cima*.

## Modelo atual

`models/compass-branco-teto-preto.glb` — Jeep Compass branco, teto preto, película escura e
rodas Limited 19″ (créditos em `models/CREDITS.md`, licença CC BY 4.0). Para renderizar de novo:

```bash
N=72 START=215 LAMPS="Jeep Compass_mat_4 #1" node tools/vehicle3d/render_glb.js \
  tools/vehicle3d/models/compass-branco-teto-preto.glb 90 /tmp/render
python3 tools/vehicle3d/package_frames.py /tmp/render
```

`yaw 90` coloca a frente do modelo no eixo usado pelo launcher; `LAMPS` indica o material
das lentes (dividido automaticamente em farol branco na frente e lanterna vermelha atrás).
