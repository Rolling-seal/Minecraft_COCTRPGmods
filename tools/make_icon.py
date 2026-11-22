from PIL import Image, ImageDraw
import random

S = 64
img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

BG = (6, 14, 13, 255)
BG2 = (9, 22, 19, 255)
GOLD = (176, 138, 62, 255)
GOLD_HI = (226, 197, 107, 255)
GOLD_DK = (92, 68, 30, 255)
GREEN_DK = (12, 38, 29, 255)
GREEN_1 = (30, 75, 52, 255)
GREEN_2 = (48, 112, 70, 255)
GREEN_3 = (76, 148, 88, 255)
GREEN_HI = (132, 190, 116, 255)
EYE = (214, 255, 108, 255)
EYE_HI = (255, 255, 198, 255)
BLACK = (4, 10, 8, 255)

d.rectangle((0, 0, S - 1, S - 1), fill=BG)
random.seed(7)
for _ in range(90):
    x = random.randrange(2, S - 2)
    y = random.randrange(2, S - 2)
    if (x - 32) ** 2 + (y - 32) ** 2 < 31 ** 2:
        d.point((x, y), fill=BG2 if random.random() < .7 else (12, 28, 22, 255))

# brass medallion
d.ellipse((2, 2, 61, 61), fill=(8, 19, 16, 255), outline=GOLD_DK)
d.ellipse((3, 3, 60, 60), outline=GOLD)
d.ellipse((5, 5, 58, 58), outline=(122, 94, 44, 255))
d.ellipse((7, 7, 56, 56), fill=(10, 26, 20, 255), outline=(70, 50, 24, 255))
for i in range(9, 18):
    d.ellipse((32 - i, 32 - i, 32 + i, 32 + i), fill=(10 + i // 2, 26 + i, 20 + i // 2, 255))
for x, y in [(6, 6), (57, 6), (6, 57), (57, 57)]:
    d.rectangle((x - 1, y - 1, x + 1, y + 1), fill=GOLD_HI)
    d.point((x, y), fill=GOLD_DK)

# wings, kept low and dark so the face dominates
d.polygon([(21, 30), (7, 20), (9, 28), (4, 27), (12, 35), (10, 39), (21, 37)], fill=GREEN_DK, outline=BLACK)
d.polygon([(19, 30), (9, 23), (11, 29), (7, 29), (14, 34), (13, 36), (20, 35)], fill=GREEN_1)
d.polygon([(43, 30), (57, 20), (55, 28), (60, 27), (52, 35), (54, 39), (43, 37)], fill=GREEN_DK, outline=BLACK)
d.polygon([(45, 30), (55, 23), (53, 29), (57, 29), (50, 34), (51, 36), (44, 35)], fill=GREEN_1)

# head silhouette
head = [(32, 8), (38, 10), (42, 15), (45, 22), (46, 30), (44, 37), (40, 40), (36, 41), (32, 42),
        (28, 41), (24, 40), (20, 37), (18, 30), (19, 22), (22, 15), (26, 10)]
d.polygon(head, fill=GREEN_DK, outline=BLACK)
head2 = [(32, 11), (37, 13), (40, 18), (42, 24), (43, 30), (41, 35), (38, 38), (35, 39), (32, 40),
         (29, 39), (26, 38), (23, 35), (21, 30), (22, 24), (24, 18), (27, 13)]
d.polygon(head2, fill=GREEN_2)

# crown ridges, no eyebrows
d.polygon([(32, 11), (36, 13), (39, 18), (32, 17), (25, 18), (28, 13)], fill=GREEN_3)
d.polygon([(32, 13), (35, 15), (37, 18), (32, 17), (27, 18), (29, 15)], fill=GREEN_HI)

# cheek shadows
d.polygon([(22, 30), (25, 28), (25, 35), (22, 34)], fill=GREEN_1)
d.polygon([(42, 30), (39, 28), (39, 35), (42, 34)], fill=GREEN_1)

# eyes based on the parallelogram-eye version, with a geometric curved pupil lower half
d.polygon([(19, 24), (27, 25), (29, 29), (27, 31), (22, 31), (19, 28)], fill=GREEN_DK, outline=BLACK)
d.polygon([(45, 24), (37, 25), (35, 29), (37, 31), (42, 31), (45, 28)], fill=GREEN_DK, outline=BLACK)
for y, x1, x2 in [(25, 21, 25), (26, 20, 27), (27, 20, 28), (28, 20, 28),
                  (29, 22, 28), (30, 23, 28), (31, 24, 28)]:
    d.rectangle((x1, y, x2, y), fill=EYE)
for y, x1, x2 in [(25, 39, 43), (26, 37, 44), (27, 36, 44), (28, 36, 44),
                  (29, 36, 42), (30, 36, 41), (31, 36, 40)]:
    d.rectangle((x1, y, x2, y), fill=EYE)
# pupil parallelogram A(23,27) B(26,27) C(27,30) D(24,30):
# diagonal A-C, trisection near C pulled to bottom edge, curve C -> P' -> A.
# Fill above the curve so the lower boundary is the smooth arc.
for y, x1, x2 in [(27, 23, 26), (28, 23, 27), (29, 24, 27), (30, 26, 27)]:
    d.rectangle((x1, y, x2, y), fill=BLACK)
for y, x1, x2 in [(27, 37, 40), (28, 36, 40), (29, 36, 39), (30, 36, 37)]:
    d.rectangle((x1, y, x2, y), fill=BLACK)
d.point((21, 25), fill=EYE_HI)
d.point((43, 25), fill=EYE_HI)
d.point((28, 30), fill=(160, 210, 70, 255))
d.point((36, 30), fill=(160, 210, 70, 255))

# no central black bar between the eyes; only a subtle green nose ridge below them
d.line([(32, 31), (32, 34)], fill=GREEN_1, width=1)
d.point((30, 33), fill=GREEN_1)
d.point((34, 33), fill=GREEN_1)

# tentacle beard starts at the jaw, leaving the lower face without a mouth
tentacles = [
    [(26, 35), (22, 41), (24, 47), (19, 52), (15, 51)],
    [(29, 37), (27, 44), (30, 50), (26, 56), (21, 58)],
    [(32, 38), (31, 46), (34, 52), (31, 58), (27, 61)],
    [(34, 38), (36, 45), (33, 51), (37, 57), (41, 58)],
    [(38, 35), (42, 41), (40, 47), (45, 52), (49, 51)],
    [(24, 34), (19, 38), (20, 43), (15, 46)],
    [(40, 34), (45, 38), (44, 43), (49, 46)],
    [(32, 40), (29, 49), (35, 54), (39, 49)],
]
for pts in tentacles:
    d.line(pts, fill=BLACK, width=4, joint='curve')
    d.line(pts, fill=GREEN_1, width=2, joint='curve')
for pts in tentacles:
    for i in range(1, len(pts) - 1, 2):
        x, y = pts[i]
        d.point((x, y - 1), fill=GREEN_3)

# no mouth or recess under the nose; the tentacle beard covers the jaw
d.point((32, 45), fill=GOLD_HI)

d.ellipse((1, 1, 62, 62), outline=GOLD_DK)
d.ellipse((2, 2, 61, 61), outline=GOLD)

img.save('tools/icons/coc_icon_64_v11.png')
img.resize((512, 512), Image.Resampling.NEAREST).save('tools/icons/coc_icon_512_v11.png')
print('generated v2')
