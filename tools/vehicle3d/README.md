# Veículo 3D do launcher

O palco usa quadros pré-renderizados (60 posições de dia, 60 de noite com faróis acesos
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

Gere 60 PNGs com fundo transparente (câmera ¾ elevada ~15°, giro de 6° entre quadros,
começando de frente-esquerda), coloque em `day/` (e opcionalmente `night/`), mais
`top_day.png`/`top_night.png`, e rode `package_frames.py`. Também é possível importar
pelo próprio app: *Configurações → Veículo → Fotos 360° / Vista de cima*.

`placeholder_model.js` é o modelo provisório (estilizado) usado até a renderização final.
