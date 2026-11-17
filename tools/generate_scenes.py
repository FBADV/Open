"""Ambientes premium (escuros, cinematográficos) para o launcher."""
import numpy as np, cv2
W,H=1920,1080
Y,X=np.mgrid[0:H,0:W].astype(np.float32); yn=Y/H; xn=X/W
rng=np.random.default_rng(3)
HZ=0.62
def c(*v): return np.array(v,np.float32)/255
def grad(stops,t):
    out=np.zeros(t.shape+(3,),np.float32)
    for (t0,c0),(t1,c1) in zip(stops[:-1],stops[1:]):
        m=(t>=t0)&(t<=t1); k=((t-t0)/max(t1-t0,1e-6))[m][:,None]; out[m]=c(*c0)+(c(*c1)-c(*c0))*k
    return out
def glow(img,cx,cy,rx,ry,col,a):
    d=((X-cx)/rx)**2+((Y-cy)/ry)**2; return img+np.exp(-d)[...,None]*c(*col)*a
def blur(img,s): return cv2.GaussianBlur(img,(0,0),s)
def noise(scale,seed):
    r=np.random.default_rng(seed); g=r.random((H//scale+2,W//scale+2)).astype(np.float32)
    return cv2.resize(g,(W+2*scale,H+2*scale),interpolation=cv2.INTER_CUBIC)[:H,:W]
def ridge(base,amp,period,seed,sharp=1.0):
    r=np.random.default_rng(seed); x=np.arange(W)
    n=sum(a*np.sin(x/(period*f)+r.uniform(0,6)) for a,f in [(1,1),(0.5,0.47),(0.25,0.21)])
    return (base-amp*(0.5+0.5*n/1.75)**sharp)*H
def floor_reflect(img,strength=0.35,blur_s=10):
    """reflexo molhado/espelhado do que está acima do horizonte"""
    hz=int(H*HZ); h=H-hz
    src=img[max(0,hz-h):hz][::-1]
    src=blur(src,blur_s)
    if src.shape[0]<h: src=np.pad(src,((0,h-src.shape[0]),(0,0),(0,0)),mode='edge')
    fade=np.linspace(strength,0,h)[:,None,None]
    out=img.copy(); out[hz:]=img[hz:]*(1-fade)+src[:h]*fade; return out
def finish(img,name,tone=(1,1,1)):
    img=img*np.array(tone,np.float32)
    v=1-0.55*(((xn-0.45)**2)*1.1+((yn-0.5)**2)*1.5)        # vinheta
    img=img*np.clip(v,0,1)[...,None]
    img=img+rng.normal(0,0.012,img.shape).astype(np.float32)  # grão de filme
    img=np.clip(img,0,1)
    cv2.imwrite(name,cv2.cvtColor((img*255).astype(np.uint8),cv2.COLOR_RGB2BGR),[cv2.IMWRITE_JPEG_QUALITY,90])

def estudio():
    img=grad([(0,(8,9,12)),(HZ,(22,24,30)),(HZ+0.001,(16,17,21)),(1,(6,7,9))],yn)
    img=glow(img,W*0.42,H*0.18,W*0.30,H*0.55,(70,80,100),0.55)      # luz de teto
    img=glow(img,W*0.42,H*HZ,W*0.42,H*0.05,(140,160,190),0.25)       # linha do horizonte
    img=glow(img,W*0.42,H*0.80,W*0.30,H*0.12,(60,66,80),0.45)        # piso iluminado
    # barras de luz no teto (softbox)
    lines=np.zeros_like(img)
    for x0 in [0.12,0.42,0.72]:
        m=(np.abs(xn-x0)<0.11)&(np.abs(yn-0.035)<0.004); lines[m]=c(210,220,240)
    img=img+blur(lines,3)*0.55+blur(lines,30)*0.9
    img=floor_reflect(img,0.22,14)
    finish(img,"p_scene_estudio.jpg")

def rua():
    img=grad([(0,(5,7,14)),(0.45,(12,16,30)),(HZ,(30,26,40)),(HZ+0.001,(10,11,16)),(1,(4,5,8))],yn)
    far=np.zeros_like(img)
    r=np.random.default_rng(5)
    for _ in range(260):   # bokeh de luzes da cidade
        cx=r.uniform(0,W); cy=r.uniform(H*0.20,H*HZ); rad=r.uniform(6,30)
        col=[(255,190,120),(255,160,90),(140,190,255),(255,230,200),(255,90,90)][r.integers(0,5)]
        a=r.uniform(0.15,0.6)*(cy/(H*HZ))
        cv2.circle(far,(int(cx),int(cy)),int(rad),tuple(float(x)/255*a for x in col),-1,cv2.LINE_AA)
    img=img+blur(far,4)+blur(far,22)*0.8
    # silhuetas de prédios muito suaves
    sk=np.zeros((H,W),np.float32); x=0
    while x<W:
        bw=int(r.integers(60,200)); bh=int(r.integers(120,380)); sk[int(H*HZ)-bh:int(H*HZ),x:x+bw]=1; x+=bw+int(r.integers(0,30))
    sk=blur(sk,8)[...,None]; img=img*(1-sk*0.55)
    img=glow(img,W*0.45,H*HZ,W*0.5,H*0.06,(255,170,110),0.18)
    img=floor_reflect(img,0.45,8)
    finish(img,"p_scene_rua.jpg",(0.95,0.97,1.05))

def estrada():
    img=grad([(0,(6,8,18)),(0.35,(18,22,46)),(0.52,(70,48,60)),(HZ,(170,96,60)),(HZ+0.001,(16,15,18)),(1,(5,5,7))],yn)
    img=glow(img,W*0.45,H*HZ,W*0.25,H*0.08,(255,150,80),0.55)
    for i,(b,a,col,p,bl) in enumerate([(0.585,0.07,(40,30,46),260,6),(0.61,0.05,(22,18,26),170,3)]):
        rd=ridge(b,a,p,20+i); m=(Y>rd[None,:])&(Y<H*HZ)
        lay=np.zeros_like(img); lay[m]=c(*col); mm=blur(m.astype(np.float32),bl)[...,None]
        img=img*(1-mm)+blur(lay,bl)*mm
    # estrada com rastros de luz (longa exposição)
    vx,vy=W*0.45,H*HZ; tt=np.clip((Y-vy)/(H-vy),0,1)
    for xb,col,wid in [(-300,(255,60,50),5),(-150,(255,80,60),3),(2200,(255,235,210),5),(2050,(255,220,190),3)]:
        lx=vx+(xb-vx)*tt; w=1+wid*tt*6
        m=np.exp(-((X-lx)/np.maximum(w,1))**2)*(Y>vy)
        img=img+m[...,None]*c(*col)*0.9
    road=(Y>vy)&(np.abs(X-vx)<(W*0.75)*tt)
    img[road]=img[road]*0.85
    img=img+blur(np.clip(img-0.6,0,1),18)*0.8          # bloom
    img=floor_reflect(img,0.25,10)
    finish(img,"p_scene_estrada.jpg")

def dunas():
    img=grad([(0,(8,10,22)),(0.40,(40,40,70)),(0.56,(150,100,90)),(HZ,(210,140,100)),(1,(210,140,100))],yn)
    img=glow(img,W*0.70,H*0.52,W*0.18,H*0.10,(255,200,150),0.6)
    stars=(rng.random((H,W))>0.9993)&(yn<0.35); img[stars]+=0.5
    layers=[(0.60,0.06,(120,76,62),(60,40,40),300,31),(0.66,0.09,(140,88,66),(54,36,36),380,32),(0.76,0.12,(150,96,70),(44,30,32),460,33),(0.90,0.12,(120,78,58),(30,22,24),520,34)]
    for i,(b,a,lit,sh,p,s) in enumerate(layers):
        rd=ridge(b,a,p,s,1.6); m=Y>rd[None,:]
        slope=np.gradient(rd); side=np.clip(0.5-slope[None,:]*1.4,0,1)
        col=c(*sh)+(c(*lit)-c(*sh))*side[...,None]
        depth=np.clip((Y-rd[None,:])/(H*0.18),0,1)[...,None]
        col=col*(1-0.45*depth)+np.exp(-((Y-rd[None,:])/3)**2)[...,None]*c(255,200,150)*0.25
        img=np.where(m[...,None],col,img)
        if i<2: img=blur(img,1.5)
    finish(img,"p_scene_dunas.jpg")

def lama():
    img=grad([(0,(8,11,12)),(0.45,(30,38,38)),(HZ,(60,66,60)),(HZ+0.001,(22,20,16)),(1,(8,7,6))],yn)
    r=np.random.default_rng(9)
    for li,(base,col,hmin,hmax,cnt,bl,fog) in enumerate([(0.60,(40,48,47),120,260,140,9,0.25),(0.62,(24,30,28),160,340,90,4,0.12),(0.645,(12,15,14),220,460,40,5,0.0)]):
        lay=np.zeros((H,W),np.float32)
        for _ in range(cnt):
            x=r.uniform(-50,W+50); h=r.uniform(hmin,hmax); w=h*r.uniform(0.22,0.32); yb=H*base
            pts=np.array([[x,yb-h],[x-w/2,yb],[x+w/2,yb]],np.int32); cv2.fillPoly(lay,[pts],1)
        lay[int(H*base):]=1; lay=blur(lay,bl)[...,None]
        img=img*(1-lay)+c(*col)*lay
        img=img+blur(glow(np.zeros_like(img),W*0.5,H*(base-0.05),W*0.8,H*0.05,(120,130,125),fog),3)
    img=img+np.exp(-((yn-0.60)/0.05)**2)[...,None]*c(90,100,96)*0.35   # névoa no horizonte
    g=Y>H*HZ
    n=noise(30,4)*0.6+noise(8,5)*0.4
    mud=grad([(0,(34,30,24)),(1,(10,9,7))],np.clip((yn-HZ)/(1-HZ),0,1))*(0.75+0.5*n[...,None])
    img=np.where(g[...,None],mud,img)
    pud=(cv2.resize(noise(70,6),(W*3,H))[:, W:2*W]>0.74)&g
    pm=blur(pud.astype(np.float32),2)[...,None]
    sky=img.copy(); hz=int(H*HZ); h=H-hz; refl=blur(img[hz-h:hz][::-1],6)
    sky[hz:]=refl*0.8
    img=img*(1-pm)+sky*pm
    finish(img,"p_scene_lama.jpg",(0.96,1.0,0.98))

for f in (estudio,rua,estrada,dunas,lama): f()

# ======================= versões de DIA =======================
def estudio_dia():
    img=grad([(0,(150,154,160)),(HZ,(200,203,208)),(HZ+0.001,(176,178,182)),(1,(120,122,126))],yn)
    img=glow(img,W*0.42,H*0.20,W*0.35,H*0.5,(255,255,255),0.25)
    img=glow(img,W*0.42,H*0.80,W*0.35,H*0.12,(255,255,255),0.18)
    lines=np.zeros_like(img)
    for x0 in [0.12,0.42,0.72]:
        m=(np.abs(xn-x0)<0.11)&(np.abs(yn-0.035)<0.004); lines[m]=c(255,255,255)
    img=img+blur(lines,3)*0.5+blur(lines,30)*0.5
    img=floor_reflect(img,0.18,14)
    finish(img,"p_scene_estudio_dia.jpg")

def rua_dia():
    img=grad([(0,(110,150,200)),(0.45,(170,195,220)),(HZ,(205,210,215)),(HZ+0.001,(90,92,98)),(1,(50,52,56))],yn)
    r=np.random.default_rng(15); sk=np.zeros((H,W),np.float32); x=0
    tones=np.zeros_like(img)
    while x<W:
        bw=int(r.integers(80,220)); bh=int(r.integers(160,460)); top=int(H*HZ)-bh
        tone=r.uniform(0.45,0.75)
        tones[top:int(H*HZ),x:x+bw]=c(150,160,175)*tone+c(255,255,255)*0.05
        for wy in range(top+10,int(H*HZ)-8,22):   # faixas de janelas (reflexo do céu)
            tones[wy:wy+8,x+6:x+bw-6]+=c(60,80,110)*r.uniform(0.1,0.35)
        sk[top:int(H*HZ),x:x+bw]=1; x+=bw+int(r.integers(4,30))
    m=blur(sk,16)[...,None]; img=img*(1-m)+blur(tones,16)*m
    img=glow(img,W*0.3,H*0.2,W*0.4,H*0.3,(255,250,235),0.25)
    img=floor_reflect(img,0.30,8)
    finish(img,"p_scene_rua_dia.jpg")

def estrada_dia():
    img=grad([(0,(70,120,190)),(0.45,(150,190,230)),(HZ,(215,225,235)),(HZ+0.001,(80,80,82)),(1,(45,45,48))],yn)
    cl=np.clip(noise(120,40)*1.4-0.65,0,1)*(yn<0.5); img=img+blur(cl,20)[...,None]*0.35
    for i,(b,a,col,p,bl) in enumerate([(0.585,0.07,(120,140,160),260,6),(0.61,0.05,(80,95,105),170,3)]):
        rd=ridge(b,a,p,20+i); m=(Y>rd[None,:])&(Y<H*HZ)
        lay=np.zeros_like(img); lay[m]=c(*col); mm=blur(m.astype(np.float32),bl)[...,None]
        img=img*(1-mm)+blur(lay,bl)*mm
    vx,vy=W*0.45,H*HZ; tt=np.clip((Y-vy)/(H-vy),0,1)
    side=(Y>vy)&(np.abs(X-vx)>(W*0.75)*tt); img[side]=img[side]*0.0+c(90,100,70)[None]*(0.7+0.3*tt[side][:,None])
    for xb,dash in [(-600,False),(2500,False),(960,True)]:
        lx=vx+(xb-vx)*tt; w=1+18*tt; on=(np.sin(1/(tt+0.02)*2.2)>0.1) if dash else True
        m=(Y>vy+2)&(np.abs(X-lx)<w)&on; img[m]=img[m]*0.3+c(235,235,230)*0.7
    img=floor_reflect(img,0.08,10)
    finish(img,"p_scene_estrada_dia.jpg")

def dunas_dia():
    img=grad([(0,(60,120,200)),(0.45,(150,195,235)),(HZ,(240,225,200)),(1,(240,225,200))],yn)
    img=glow(img,W*0.75,H*0.18,W*0.12,H*0.1,(255,255,240),0.6)
    layers=[(0.60,0.06,(236,200,150),(190,140,95),300,31),(0.66,0.09,(240,205,155),(185,132,88),380,32),(0.76,0.12,(244,210,160),(175,124,80),460,33),(0.90,0.12,(238,200,150),(160,110,70),520,34)]
    for i,(b,a,lit,sh,p,s) in enumerate(layers):
        rd=ridge(b,a,p,s,1.6); m=Y>rd[None,:]
        slope=np.gradient(rd); sd=np.clip(0.5-slope[None,:]*1.4,0,1)
        col=c(*sh)+(c(*lit)-c(*sh))*sd[...,None]
        depth=np.clip((Y-rd[None,:])/(H*0.18),0,1)[...,None]
        col=col*(1-0.2*depth)
        img=np.where(m[...,None],col,img)
        if i<2: img=blur(img,1.5)
    finish(img,"p_scene_dunas_dia.jpg")

def lama_dia():
    img=grad([(0,(150,165,170)),(0.45,(185,195,195)),(HZ,(170,180,170)),(HZ+0.001,(80,68,52)),(1,(40,32,24))],yn)
    r=np.random.default_rng(9)
    for li,(base,col,hmin,hmax,cnt,bl) in enumerate([(0.60,(110,130,120),120,260,140,9),(0.62,(70,90,75),160,340,90,4),(0.645,(40,55,42),220,460,40,5)]):
        lay=np.zeros((H,W),np.float32)
        for _ in range(cnt):
            x=r.uniform(-50,W+50); h=r.uniform(hmin,hmax); w=h*r.uniform(0.22,0.32); yb=H*base
            cv2.fillPoly(lay,[np.array([[x,yb-h],[x-w/2,yb],[x+w/2,yb]],np.int32)],1)
        lay[int(H*base):]=1; lay=blur(lay,bl)[...,None]; img=img*(1-lay)+c(*col)*lay
    img=img+np.exp(-((yn-0.60)/0.05)**2)[...,None]*c(200,210,205)*0.3
    g=Y>H*HZ; n=noise(30,4)*0.6+noise(8,5)*0.4
    mud=grad([(0,(95,78,58)),(1,(45,36,26))],np.clip((yn-HZ)/(1-HZ),0,1))*(0.8+0.4*n[...,None])
    img=np.where(g[...,None],mud,img)
    pud=(cv2.resize(noise(70,6),(W*3,H))[:, W:2*W]>0.74)&g; pm=blur(pud.astype(np.float32),2)[...,None]
    sky=img.copy(); hz=int(H*HZ); h=H-hz; sky[hz:]=blur(img[hz-h:hz][::-1],6)*0.85
    img=img*(1-pm)+sky*pm
    finish(img,"p_scene_lama_dia.jpg")

for f in (estudio_dia,rua_dia,estrada_dia,dunas_dia,lama_dia): f()
