"""Versão final do veículo: pintura branca natural (v2), vidros levemente
escurecidos, bordas refinadas (sem halo do fundo) e meia-sombra nos pneus."""
import numpy as np, cv2
from PIL import Image, ImageDraw
im=np.asarray(Image.open("white_v2.png").convert("RGBA")).astype(np.float32)/255
rgb=im[...,:3].copy(); a=im[...,3].copy(); H,W=a.shape
def B(pts): return [(x/2+200,y/2+170) for x,y in pts]
def poly(pts):
    m=Image.new("L",(W,H),0); ImageDraw.Draw(m).polygon(B(pts),fill=255); return np.asarray(m)>0
# 1) alfa nítido: curva sobre o alfa original + preenchimento de furos internos
hard=(a>0.5).astype(np.uint8)
n,lab,st,_=cv2.connectedComponentsWithStats(hard,8)
keep=1+np.argmax(st[1:,cv2.CC_STAT_AREA]); body=(lab==keep)
inv=(~body).astype(np.uint8); n2,lab2,st2,_=cv2.connectedComponentsWithStats(inv,4)
for i in range(1,n2):
    if st2[i,cv2.CC_STAT_AREA]<400: body[lab2==i]=True
alpha=np.clip((a-0.35)/0.3,0,1)*cv2.dilate(body.astype(np.uint8),np.ones((3,3),np.uint8)).astype(np.float32)
alpha=np.maximum(alpha,(body&(a>0.2)).astype(np.float32)*0)
alpha=cv2.GaussianBlur(alpha,(0,0),0.6)
# 2) descontamina só a franja (0<alfa<1) com a cor do interior
core=(alpha>0.98).astype(np.uint8)
fringe=((alpha>0.01)&(core==0)).astype(np.uint8)*255
src=(np.clip(rgb,0,1)*255).astype(np.uint8)
fixed=cv2.inpaint(src,fringe,3,cv2.INPAINT_TELEA).astype(np.float32)/255
rgb=np.where((fringe>0)[...,None],fixed,rgb)
# 2b) resíduos avermelhados/arroxeados do vermelho original -> branco
hsv=cv2.cvtColor((np.clip(rgb,0,1)*255).astype(np.uint8),cv2.COLOR_RGB2HSV_FULL).astype(np.float32)
hh=hsv[...,0]/255*360; ss=hsv[...,1]/255; vv=hsv[...,2]/255
res=((hh>=270)|(hh<=15))&(ss>0.12)&(vv>0.25)&(alpha>0.5)
rm=cv2.GaussianBlur(res.astype(np.float32),(0,0),1.0)[...,None]
gray=np.clip(0.72+0.28*vv,0,1)[...,None]*np.array([0.972,0.982,1.0])
rgb=rgb*(1-rm)+gray*rm
# 3) vidros: insulfilm leve (mantém o interior visível como num carro real)
win=poly([(560,90),(1180,70),(1190,120),(1100,300),(1000,298),(560,262)])|poly([(1150,80),(1560,90),(1600,200),(1600,240),(1440,280),(1300,292),(1112,300)])
wm=cv2.GaussianBlur(win.astype(np.float32),(0,0),1.5)[...,None]
tint=rgb*0.62+np.array([0.02,0.03,0.05])
rgb=rgb*(1-wm)+tint*wm
# 4) leve ajuste de contraste global (sai do aspecto "lavado" de foto de pátio)
lum=rgb.mean(-1,keepdims=True)
rgb=np.clip((rgb-lum)*1.05+lum,0,1)
rgb=np.clip((rgb-0.5)*1.06+0.5-0.01,0,1)
out=Image.fromarray((np.dstack([rgb,alpha])*255).astype(np.uint8))
bb=out.getbbox(); out=out.crop((bb[0]-4,bb[1]-4,bb[2]+4,bb[3]+4))
out.save("vehicle_v4.png",optimize=True)
for name,col in [("dark",(20,22,26)),("light",(225,225,228))]:
    bg=Image.new("RGBA",out.size,col+(255,)); bg.alpha_composite(out)
    bg.convert("RGB").resize((out.width*2,out.height*2)).save(f"v4_{name}.jpg",quality=90)
print(out.size)
