import numpy as np, colorsys
from PIL import Image, ImageFilter, ImageDraw
im=Image.open("cut_u2net.png").convert("RGBA")
a=np.asarray(im).astype(np.float32)/255
rgb=a[...,:3]; al=a[...,3]
mx=rgb.max(-1); mn=rgb.min(-1); v=mx; s=np.where(mx>0,(mx-mn)/np.maximum(mx,1e-6),0)
r,g,b=rgb[...,0],rgb[...,1],rgb[...,2]
# hue in degrees
d=np.maximum(mx-mn,1e-6)
h=np.where(mx==r,((g-b)/d)%6,np.where(mx==g,(b-r)/d+2,(r-g)/d+4))*60
paintish=((h>=245)|(h<=30)) & (s>0.12) & (v>0.12)
m=paintish.astype(np.float32)
# hood polygon: any hue (sky reflections) where not too dark
hood=Image.new("L",im.size,0); ImageDraw.Draw(hood).polygon([(238,352),(300,318),(470,300),(690,318),(700,330),(600,372),(560,392),(420,402),(300,388),(238,368)],fill=255)
hood=np.asarray(hood)>0
m=np.maximum(m, (hood & (v>0.28)).astype(np.float32))
side=Image.new("L",im.size,0); ImageDraw.Draw(side).polygon([(752,328),(905,306),(1010,292),(1052,300),(1064,362),(1012,370),(990,420),(905,470),(762,512),(748,420)],fill=255)
side=np.asarray(side)>0
m=np.maximum(m,(side & (v>0.30)).astype(np.float32))
# close small holes
mk=Image.fromarray((m*255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(1.0))
mask=np.asarray(mk).astype(np.float32)/255*(al>0.5)*np.clip((v-0.10)/0.10,0,1)
L=np.clip(0.52+ (v**0.85)*0.60,0,1)
white=np.stack([L*0.962,L*0.972,L*0.99],-1)
out=rgb*(1-mask[...,None])+white*mask[...,None]
res=Image.fromarray((np.dstack([out,al])*255).astype(np.uint8))
# blank the dealer plate
ImageDraw.Draw(res).polygon([(242,480),(292,488),(291,540),(241,532)],fill=(232,234,238,255))
res.save("white_raw.png")
bg=Image.new("RGBA",im.size,(70,90,110,255)); bg.alpha_composite(res); bg.convert("RGB").save("white_prev.jpg",quality=85)
Image.fromarray((mask*255).astype(np.uint8)).save("mask.png")
