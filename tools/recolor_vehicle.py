import numpy as np, cv2
from PIL import Image, ImageDraw, ImageFont
src=np.asarray(Image.open("cut_u2net.png").convert("RGBA")).astype(np.float32)/255
rgb=src[...,:3].copy(); al=src[...,3]
H,W=al.shape
hsv=cv2.cvtColor((rgb*255).astype(np.uint8),cv2.COLOR_RGB2HSV_FULL).astype(np.float32)
h=hsv[...,0]/255*360; s=hsv[...,1]/255; v=hsv[...,2]/255
def B(pts):  # big-crop coords -> original
    return [(x/2+200,y/2+170) for x,y in pts]
def poly(pts):
    m=Image.new("L",(W,H),0); ImageDraw.Draw(m).polygon(B(pts),fill=255); return np.asarray(m)>0
red=((h>=295)|(h<=22))&(s>0.15)&(v>0.12)&(al>0.5)
m0=red.astype(np.uint8)
# fill small holes (sky reflections enclosed by paint)
inv=(1-m0).astype(np.uint8)
n,lab,st,_=cv2.connectedComponentsWithStats(inv,8)
fill=np.zeros_like(m0)
for i in range(1,n):
    if st[i,cv2.CC_STAT_AREA]<6000: fill[lab==i]=1
m1=np.maximum(m0,fill).astype(bool)
hood=poly([(62,395),(100,352),(230,313),(420,279),(560,262),(700,262),(1000,298),(1090,312),(1105,330),(1110,440),(990,445),(745,445),(560,440),(400,455),(330,452)])
side=poly([(1108,306),(1300,298),(1440,286),(1600,262),(1680,240),(1720,300),(1725,380),(1700,420),(1610,445),(1560,540),(1420,600),(1260,640),(1150,690),(1120,600),(1110,460)])
front=poly([(40,500),(330,540),(560,545),(740,500),(770,560),(720,640),(700,690),(640,760),(440,790),(380,700),(190,650),(40,560)])
fender=poly([(740,440),(1112,436),(1118,560),(1060,520),(960,500),(850,505),(790,540),(745,520)])
m2=m1|(hood&(v>0.20))|(side&(v>0.06))|(fender&(v>0.18))|(front&(v>0.30)&(s>0.12))
ex=poly([(400,464),(560,458),(742,452),(735,505),(640,540),(520,540),(430,520)])  # farol
grill=poly([(38,398),(398,456),(398,566),(38,538)])
ex|=grill  # grade
ex|=poly([(560,90),(1180,70),(1190,120),(1100,300),(1000,298),(560,262)])  # para-brisa
ex|=poly([(1150,80),(1560,90),(1600,200),(1600,240),(1440,280),(1300,292),(1112,300)])  # janelas
ex|=poly([(150,395),(215,393),(215,425),(150,428)])&(s<0.30)  # emblema capô
ex|=poly([(1155,540),(1275,520),(1278,545),(1158,568)])&(s<0.30)  # emblema COMPASS
ex|=poly([(1150,230),(1300,230),(1300,300),(1150,300)])  # retrovisor
belt=poly([(1100,292),(1300,286),(1600,238),(1700,205),(1728,250),(1728,320),(1600,300),(1300,320),(1108,340)])
body=hood|side|fender|belt
purple=(body&(h>=240)&(s>0.08)&(v>0.05))|(belt&(v>0.10)&((h>=200)|(h<=30)))
head=poly([(400,464),(560,458),(742,452),(735,505),(640,540),(520,540),(430,520)])
m3=((m2|purple)&~ex)|(red&(grill|head))
mask=cv2.GaussianBlur(m3.astype(np.float32),(0,0),0.9)*np.clip((al-0.3)/0.4,0,1)
np.save("mask.npy",mask)
ov=rgb.copy(); ov[m3]=ov[m3]*0.3+np.array([0,1,0])*0.7
bg=np.dstack([ov,al]); im=Image.fromarray((bg*255).astype(np.uint8))
c=Image.new("RGBA",im.size,(40,40,60,255)); c.alpha_composite(im); c.convert("RGB").crop((200,170,1080,680)).resize((1760,1020)).save("mask_view.jpg",quality=85)

# ---------- pintura branca ----------
V=v.copy()
lo,hi=np.percentile(V[m3],[3,99.5])
t=np.clip((V-lo)/(hi-lo),0,1)
base=0.78+0.22*np.power(t,0.5)          # branco: sombras ~0.70, luz ~1.0
detail=V-cv2.GaussianBlur(V,(0,0),6)
L=np.clip(base+0.30*detail,0,1)
white=np.dstack([L*0.972,L*0.982,L*1.0])  # branco levemente frio (Bright White)
out=rgb*(1-mask[...,None])+white*mask[...,None]
o8=(np.clip(out,0,1)*255).astype(np.uint8)
# adesivos do para-brisa: inpaint
im_mask=np.zeros((H,W),np.uint8)
for x0,y0,x1,y1 in [(518,246,556,304),(736,228,758,249),(761,210,779,227)]:
    im_mask[y0:y1,x0:x1]=255
o8=cv2.inpaint(o8,im_mask,9,cv2.INPAINT_NS)
# carimbo: copia o vidro vizinho (à direita) sobre o adesivo grande
fm=np.zeros((H,W),np.float32); fm[244:306,514:560]=1; fm=cv2.GaussianBlur(fm,(0,0),4)[...,None]
shifted=np.roll(o8,-48,axis=1)
o8=(o8*(1-fm)+shifted*fm).astype(np.uint8)
# tom de vidro escuro sobre a área retocada, com leve textura
zone=cv2.GaussianBlur(im_mask.astype(np.float32)/255,(0,0),3)[...,None]
glass=o8.astype(np.float32)*0.78+np.random.default_rng(1).normal(0,4,o8.shape)
o8=np.clip(o8*(1-zone)+glass*zone,0,255).astype(np.uint8)
res=Image.fromarray(np.dstack([o8,(al*255).astype(np.uint8)]))
# placa Mercosul (sem caracteres)
d=ImageDraw.Draw(res)
P=[(240,477),(293,485),(293,541),(240,533)]
d.polygon(P,fill=(238,240,244,255),outline=(40,40,48,255))
d.polygon([(240,477),(293,485),(293,494),(240,486)],fill=(20,60,160,255))
res.save("white_v2.png")
bb=res.getbbox(); res.crop((bb[0]-6,bb[1]-6,bb[2]+6,bb[3]+6)).save("vehicle_v2.png",optimize=True)
c=Image.new("RGBA",res.size,(150,160,175,255)); c.alpha_composite(res)
c.convert("RGB").crop((200,170,1080,680)).resize((1760,1020)).save("white_v2_view.jpg",quality=88)
