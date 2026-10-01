from PIL import Image, ImageDraw, ImageFilter
import os, random
random.seed(4557)
S=512
im=Image.new("RGBA",(S,S),(0,0,0,0))
d=ImageDraw.Draw(im)
# shadow / legs
d.ellipse((150,430,370,500), fill=(0,0,0,80))
d.polygon([(205,310),(248,310),(238,475),(195,475)], fill=(30,27,25,245))
d.polygon([(268,310),(310,310),(330,475),(286,475)], fill=(27,25,24,245))
# ragged torso
d.polygon([(175,145),(320,138),(345,330),(165,330)], fill=(72,64,57,250))
for _ in range(26):
 x=random.randint(175,330); y=random.randint(155,315); r=random.randint(2,8)
 d.ellipse((x-r,y-r,x+r,y+r),fill=(65,34,30,random.randint(80,150)))
# long arms
d.polygon([(172,165),(195,175),(150,390),(128,385)], fill=(91,81,72,245))
d.polygon([(316,160),(338,170),(382,385),(360,392)], fill=(87,77,70,245))
# head + hair
d.ellipse((196,48,309,170),fill=(113,101,90,255))
d.polygon([(190,55),(220,25),(300,35),(325,95),(304,80),(292,145),(275,78),(245,155),(225,75),(200,130)],fill=(25,23,22,250))
# eyes
d.ellipse((222,95,238,107),fill=(220,213,178,245)); d.ellipse((269,94,285,106),fill=(220,213,178,245))
d.ellipse((228,99,233,104),fill=(25,20,18,255)); d.ellipse((275,98,280,103),fill=(25,20,18,255))
# scratches / dark dried stains, non-graphic
for _ in range(9):
 x=random.randint(210,290); y=random.randint(75,145)
 d.line((x,y,x+random.randint(-8,8),y+random.randint(6,18)),fill=(73,38,34,150),width=2)
im=im.filter(ImageFilter.GaussianBlur(0.45))
os.makedirs("src/main/resources/assets/dont_look_away/textures/gui",exist_ok=True)
im.save("src/main/resources/assets/dont_look_away/textures/gui/stalker.png")
