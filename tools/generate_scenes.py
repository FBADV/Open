import numpy as np
from PIL import Image, ImageFilter, ImageDraw
W,H=1920,1080
rng=np.random.default_rng(7)
Y,X=np.mgrid[0:H,0:W].astype(np.float32); yn=Y/H; xn=X/W

def lerp(a,b,t): return a+(b-a)*t
def col(*c): return np.array(c,np.float32)/255
def vgrad(stops,t):
    out=np.zeros(t.shape+(3,),np.float32)
    for (t0,c0),(t1,c1) in zip(stops[:-1],stops[1:]):
        m=(t>=t0)&(t<=t1); k=((t-t0)/(t1-t0))[m][:,None]
        out[m]=lerp(col(*c0),col(*c1),k)
    return out
def noise1d(n,scale,seed,oct=4):
    r=np.random.default_rng(seed); x=np.arange(n); out=np.zeros(n)
    amp=1;f=scale
    for o in range(oct):
        pts=r.standard_normal(int(n/f)+3); out+=amp*np.interp(x/f,np.arange(len(pts)),pts); amp*=0.5; f/=2
    return out
def noise2d(scale,seed,oct=4):
    r=np.random.default_rng(seed); out=np.zeros((H,W),np.float32); amp=1; s=scale
    for o in range(oct):
        g=r.random((int(H/s)+2,int(W/s)+2)).astype(np.float32)
        im=Image.fromarray((g*255).astype(np.uint8)).resize((int((W/s+2)*s),int((H/s+2)*s)),Image.BICUBIC)
        out+=amp*(np.asarray(im)[:H,:W].astype(np.float32)/255-0.5); amp*=0.5; s=max(s/2,1)
    return out
def glow(cx,cy,r,c,img,strength=1.0):
    d=np.sqrt((X-cx)**2+(Y-cy)**2)/r; g=np.exp(-d**2)*strength
    return img+g[...,None]*col(*c)
def save(img,name):
    img=np.clip(img,0,1)
    # vignette + grain
    v=1-0.35*(((xn-0.5)**2)*1.2+((yn-0.55)**2)*1.6)
    img=img*v[...,None]+rng.normal(0,0.008,img.shape)
    Image.fromarray((np.clip(img,0,1)*255).astype(np.uint8)).save(name,quality=86,optimize=True,progressive=True)

HZ=0.60
# ---------------- ESTRADA (sunset highway) ----------------
def estrada():
    sky=vgrad([(0,(28,30,74)),(0.30,(92,64,140)),(0.48,(238,128,96)),(0.60,(255,196,130)),(1,(255,196,130))],yn)
    img=glow(W*0.42,H*HZ,W*0.35,(255,150,80),sky,0.6)
    img=glow(W*0.42,H*HZ-10,70,(255,240,200),img,1.2)
    # mountain layers
    for i,(base,amp,c,sc) in enumerate([(0.50,0.08,(120,80,120),300),(0.54,0.06,(80,52,92),200),(0.575,0.04,(48,34,64),120)]):
        ridge=(base+amp*noise1d(W,sc,10+i)/2.0)*H
        m=Y>ridge[None,:]
        img[m]=lerp(img[m],col(*c),0.92)
    # ground
    g=Y>H*HZ
    ground=vgrad([(0,(40,46,40)),(HZ,(52,58,44)),(1,(26,30,24))],yn)
    img[g]=ground[g]
    # road trapezoid to vanishing point
    vx=W*0.42; vy=H*HZ
    def inside(xl,xr):  # road edges at bottom
        t=(Y-vy)/(H-vy); left=vx+(xl-vx)*t; right=vx+(xr-vx)*t; return (Y>vy)&(X>left)&(X<right),t,left,right
    road,t,l,r=inside(-500,2600)
    asph=vgrad([(0,(70,66,80)),(HZ,(78,70,84)),(1,(38,38,44))],yn)
    asph+=noise2d(3,5,2)[...,None]*0.05
    img[road]=asph[road]
    # sky reflection on asphalt near horizon
    img=np.where(road[...,None],img+np.exp(-((Y-vy)/60)**2)[...,None]*col(120,70,50)*0.6,img)
    # edge lines
    for xb,wd in [(-500,0.012),(2600,0.012)]:
        tt=np.clip((Y-vy)/(H-vy),0,1); lx=vx+(xb-vx)*tt; w=6+60*tt
        m=(Y>vy)&(np.abs(X-lx)<w*0.5)
        img[m]=lerp(img[m],col(235,232,220),0.85)
    # center dashes (lane between)
    for xb in [700,1450]:
        tt=np.clip((Y-vy)/(H-vy),0,1); lx=vx+(xb-vx)*tt; w=3+30*tt
        z=1/(tt+0.02); dash=(np.sin(z*2.2)>0.2)
        m=(Y>vy+2)&(np.abs(X-lx)<w*0.5)&dash
        img[m]=lerp(img[m],col(240,236,224),0.8)
    save(img,"scene_estrada.jpg")

# ---------------- DUNAS (golden desert) ----------------
def dunas():
    sky=vgrad([(0,(64,110,170)),(0.35,(150,170,190)),(0.52,(250,200,150)),(1,(250,200,150))],yn)
    img=glow(W*0.70,H*0.40,W*0.25,(255,190,120),sky,0.7)
    img=glow(W*0.70,H*0.40,55,(255,250,225),img,1.6)
    layers=[(0.56,0.08,(214,150,96),(150,90,56),260,21),(0.64,0.10,(226,160,100),(160,96,58),220,22),(0.76,0.12,(232,170,110),(150,88,50),300,23),(0.92,0.12,(240,182,120),(170,104,62),360,24)]
    for base,amp,lit,shade,sc,sd in layers:
        r_=np.random.default_rng(sd); x_=np.arange(W)
        n=0.6*np.abs(np.sin(x_/(sc*1.9)+r_.uniform(0,6)))+0.4*(0.5+0.5*np.sin(x_/(sc*0.9)+r_.uniform(0,6)))
        ridge=(base-amp*n)*H
        dr=np.gradient(ridge)  # slope -> light/shadow
        m=Y>ridge[None,:]
        depth=np.clip((Y-ridge[None,:])/(H*0.25),0,1)
        side=np.clip(0.5+dr[None,:]*0.9,0,1)
        c=lerp(col(*shade),col(*lit),side[...,None]*np.ones((H,1,1)))
        c=c*(1-0.25*depth[...,None])
        # ridge highlight
        hl=np.exp(-((Y-ridge[None,:])/4)**2)*0.25
        c=c+hl[...,None]
        img[m]=c[m]
    # sand ripples
    rip=np.sin((Y*0.35+noise2d(40,9,3)*60))*0.02
    img=np.where((Y>H*0.6)[...,None],img+rip[...,None],img)
    save(img,"scene_dunas.jpg")

# ---------------- RUA (city night) ----------------
def rua():
    sky=vgrad([(0,(10,14,34)),(0.4,(30,34,78)),(0.58,(84,60,120)),(1,(84,60,120))],yn)
    img=sky.copy()
    im=Image.fromarray((np.clip(img,0,1)*255).astype(np.uint8)); d=ImageDraw.Draw(im)
    r=np.random.default_rng(3)
    for layer,(base,hmin,hmax,c,wl) in enumerate([(0.62,120,380,(36,34,62),0.25),(0.62,80,300,(22,22,40),0.45)]):
        x=-20
        while x<W:
            bw=int(r.integers(60,170)); bh=int(r.integers(hmin,hmax)); top=int(H*base)-bh
            d.rectangle([x,top,x+bw,int(H*base)],fill=c)
            for wy in range(top+12,int(H*base)-10,18):
                for wx in range(x+8,x+bw-10,14):
                    if r.random()<wl:
                        lc=(255,int(r.integers(190,230)),int(r.integers(120,170))) if r.random()<0.8 else (170,210,255)
                        d.rectangle([wx,wy,wx+6,wy+9],fill=lc)
            x+=bw+int(r.integers(2,20))
    img=np.asarray(im).astype(np.float32)/255
    g=Y>H*0.62
    street=vgrad([(0,(20,20,30)),(0.62,(40,38,56)),(1,(14,14,22))],yn)+noise2d(3,4,2)[...,None]*0.04
    img[g]=street[g]
    # wet reflections: mirrored blurred skyline
    top=img[int(H*0.30):int(H*0.62)][::-1]
    refl=np.asarray(Image.fromarray((top*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))).astype(np.float32)/255
    h=refl.shape[0]; y0=int(H*0.62)
    fade=np.linspace(0.55,0,h)[:,None,None]
    img[y0:y0+h]=img[y0:y0+h]*(1-fade)+refl*fade
    # streetlights bokeh
    for i in range(14):
        cx=r.uniform(0,W); cy=r.uniform(H*0.35,H*0.6); rad=r.uniform(8,26)
        img=glow(cx,cy,rad*3,(255,190,110),img,0.5)
    vx=W*0.5; vy=H*0.62
    for xb,dash in [(-900,False),(2800,False),(960,True)]:
        tt=np.clip((Y-vy)/(H-vy),0,1); lx=vx+(xb-vx)*tt; w=3+40*tt
        z=1/(tt+0.02); on=(np.sin(z*2.2)>0.1) if dash else True
        m=(Y>vy+2)&(np.abs(X-lx)<w*0.5)&on
        img[m]=lerp(img[m],col(230,220,200),0.55)
    save(img,"scene_rua.jpg")

# ---------------- LAMA (muddy forest trail) ----------------
def lama():
    sky=vgrad([(0,(60,74,80)),(0.4,(130,140,130)),(0.55,(190,180,150)),(1,(190,180,150))],yn)
    img=glow(W*0.3,H*0.45,W*0.3,(230,200,150),sky,0.4)
    im=Image.fromarray((np.clip(img,0,1)*255).astype(np.uint8)); d=ImageDraw.Draw(im)
    r=np.random.default_rng(11)
    for layer,(base,c,hmin,hmax,cnt) in enumerate([(0.56,(84,98,86),90,200,120),(0.60,(50,64,52),120,280,90),(0.64,(28,38,30),180,380,50)]):
        for i in range(cnt):
            x=r.uniform(-50,W+50); h=r.uniform(hmin,hmax); w=h*r.uniform(0.25,0.4); yb=H*base+r.uniform(-8,8)
            d.polygon([(x,yb-h),(x-w/2,yb),(x+w/2,yb)],fill=c)
        d.rectangle([0,int(H*base),W,H],fill=c)
    img=np.asarray(im).astype(np.float32)/255
    g=Y>H*0.62
    mud=vgrad([(0,(70,54,38)),(0.62,(96,74,50)),(1,(52,38,26))],yn)
    n=noise2d(25,31,5)
    mud=mud*(1+n[...,None]*0.22)
    img[g]=mud[g]
    # puddles reflecting sky
    pn=np.asarray(Image.fromarray(((noise2d(90,44,2)+0.5)*255).clip(0,255).astype(np.uint8)).resize((W*3,H)).crop((W,0,W*2,H)).filter(ImageFilter.GaussianBlur(3))).astype(np.float32)/255-0.5
    pa=np.clip((pn-0.16)/0.05,0,1)*g*np.clip((Y-H*0.63)/(H*0.1),0,1)
    pud_c=vgrad([(0,(160,166,150)),(1,(120,124,110))],yn)*0.85
    img=img*(1-pa[...,None]*0.8)+pud_c*pa[...,None]*0.8
    # tire ruts converging
    vx=W*0.35; vy=H*0.62
    for xb in [-200,500,1500,2300]:
        tt=np.clip((Y-vy)/(H-vy),0,1); lx=vx+(xb-vx)*tt; w=4+90*tt
        m=(Y>vy)&(np.abs(X-lx)<w*0.5)
        img[m]=img[m]*0.62
    # mud splatter grain
    img=img+noise2d(2,77,1)[...,None]*0.06*g[...,None]
    save(img,"scene_lama.jpg")

estrada(); dunas(); rua(); lama()
print("ok")
