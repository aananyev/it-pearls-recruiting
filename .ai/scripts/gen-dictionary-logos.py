#!/usr/bin/env python3
"""Генерация тематических логотипов Dictionary Edit-форм HRM HuntTech.

Рисует 200x200 PNG в палитре HuntTech (фон #172638, символ #f8fafc,
акцент #e74c3c) простыми геометрическими примитивами PIL и раскладывает
во все 7 тем: icons/dictionaries/<name>.png.
"""
import math
import os
from PIL import Image, ImageDraw

BG = (23, 38, 56, 255)       # #172638
FG = (248, 250, 252, 255)    # #f8fafc
ACC = (231, 76, 60, 255)     # #e74c3c
MUT = (148, 163, 184, 255)   # #94a3b8 приглушённый серый

THEMES = ["halo", "havana", "helium", "hover",
          "hunttech-modern", "hunttech-modern-dark", "hunttech-modern-light"]

SIZE = 200
C = SIZE // 2  # центр


def canvas():
    img = Image.new("RGBA", (SIZE, SIZE), BG)
    return img, ImageDraw.Draw(img)


def ring(d, cx, cy, r, fill=FG, width=8):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=fill, width=width)


def save(img, name):
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                       "modules", "web", "themes", name)
    # файл кладётся в <theme>/icons/dictionaries/<name>.png
    d = os.path.join(out, "icons", "dictionaries")
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, name + ".png"))


# ---- iteraction: две круговые стрелки цикла ----
def iteraction():
    img, d = canvas()
    ring(d, C, C, 62, FG, 10)
    # стрелки по кругу
    for ang in (45, 225):
        a = math.radians(ang)
        x1 = C + 62 * math.cos(a)
        y1 = C + 62 * math.sin(a)
        x2 = C + 62 * math.cos(a + 0.55)
        y2 = C + 62 * math.sin(a + 0.55)
        # треугольник-наконечник
        tip = (x1, y1)
        base1 = (C + 52 * math.cos(a + 0.18), C + 52 * math.sin(a + 0.18))
        base2 = (C + 52 * math.cos(a - 0.18), C + 52 * math.sin(a - 0.18))
        d.polygon([tip, base1, base2], fill=FG)
    d.ellipse([C - 16, C - 16, C + 16, C + 16], fill=ACC)
    return img


# ---- specialisation: мишень ----
def specialisation():
    img, d = canvas()
    ring(d, C, C, 70, FG, 8)
    ring(d, C, C, 46, FG, 8)
    ring(d, C, C, 22, FG, 8)
    d.ellipse([C - 8, C - 8, C + 8, C + 8], fill=ACC)
    return img


# ---- country: глобус с меридианами и пин-маркером ----
def country():
    img, d = canvas()
    ring(d, C, C, 62, FG, 8)
    d.line([C, C - 62, C, C + 62], fill=FG, width=6)      # вертикаль
    d.line([C - 62, C, C + 62, C], fill=FG, width=6)      # горизонталь
    d.arc([C - 62, C - 34, C + 62, C + 34], 0, 180, fill=MUT, width=5)
    d.arc([C - 62, C - 34, C + 62, C + 34], 180, 360, fill=MUT, width=5)
    d.arc([C - 34, C - 62, C + 34, C + 62], 90, 270, fill=MUT, width=5)
    d.arc([C - 34, C - 62, C + 34, C + 62], -90, 90, fill=MUT, width=5)
    # пин сверху
    d.polygon([(C - 14, C - 88), (C + 14, C - 88), (C, C - 58)], fill=ACC)
    d.ellipse([C - 9, C - 97, C + 9, C - 79], fill=ACC)
    return img


# ---- city: силуэты зданий ----
def city():
    img, d = canvas()
    # левое здание
    d.rectangle([C - 84, C - 46, C - 30, C + 62], fill=FG)
    d.rectangle([C - 74, C - 78, C - 40, C - 46], fill=FG)
    # окна левого
    for dx in (-68, -46):
        for dy in (-30, 0, 30):
            d.rectangle([dx, C + dy - 12, dx + 10, C + dy + 12], fill=BG)
    # среднее высокое
    d.rectangle([C - 24, C - 70, C + 26, C + 62], fill=MUT)
    for dx in (-14, 6):
        for dy in (-52, -24, 4, 32):
            d.rectangle([dx, C + dy - 10, dx + 8, C + dy + 10], fill=BG)
    # правое здание
    d.rectangle([C + 34, C - 28, C + 86, C + 62], fill=FG)
    for dx in (44, 66):
        for dy in (-14, 12):
            d.rectangle([dx, C + dy - 10, dx + 10, C + dy + 10], fill=BG)
    return img


# ---- region: карта с дорогами и пин ----
def region():
    img, d = canvas()
    # контур карты-плашки
    d.polygon([(C - 78, C - 62), (C - 12, C - 74), (C + 66, C - 46),
               (C + 80, C + 30), (C + 20, C + 66), (C - 60, C + 52),
               (C - 82, C + 10)], outline=FG, width=8)
    # дороги
    d.line([C - 40, C - 50, C + 30, C + 40], fill=MUT, width=7)
    d.line([C - 60, C + 20, C + 50, C - 20], fill=MUT, width=7)
    # пин-точка
    d.ellipse([C - 10, C - 14, C + 10, C + 6], fill=ACC)
    return img


# ---- ownershup: документ с ключом ----
def ownershup():
    img, d = canvas()
    # документ
    d.rounded_rectangle([C - 58, C - 78, C + 58, C + 70], radius=10,
                        outline=FG, width=8)
    d.line([C - 34, C - 40, C + 34, C - 40], fill=FG, width=7)
    d.line([C - 34, C - 10, C + 34, C - 10], fill=FG, width=7)
    d.line([C - 34, C + 20, C + 10, C + 20], fill=FG, width=7)
    # ключ (акцент)
    d.ellipse([C - 10, C + 30, C + 22, C + 62], outline=ACC, width=8)
    d.line([C + 16, C + 40, C + 44, C + 12], fill=ACC, width=8)
    d.line([C + 44, C + 12, C + 56, C + 12], fill=ACC, width=8)
    d.line([C + 40, C + 16, C + 52, C + 28], fill=ACC, width=8)
    return img


# ---- position: бейдж с человечком ----
def position():
    img, d = canvas()
    # бейдж
    d.rounded_rectangle([C - 66, C - 80, C + 66, C + 70], radius=16,
                        outline=FG, width=8)
    # человечек
    d.ellipse([C - 20, C - 56, C + 20, C - 16], fill=FG)
    d.pieslice([C - 44, C + 8, C + 44, C + 96], 180, 360, fill=FG)
    d.ellipse([C - 16, C + 44, C + 16, C + 60], fill=ACC)
    return img


# ---- file-type: файл с линиями текста ----
def file_type():
    img, d = canvas()
    d.rounded_rectangle([C - 62, C - 78, C + 62, C + 70], radius=10,
                        outline=FG, width=8)
    # загнутый уголок
    d.polygon([(C + 22, C - 78), (C + 62, C - 78), (C + 62, C - 38),
               (C + 22, C - 38)], fill=BG)
    d.line([C + 22, C - 78, C + 62, C - 38], fill=FG, width=8)
    d.line([C + 22, C - 78, C + 22, C - 38], fill=FG, width=8)
    # текст
    for dy in (-16, 8, 32):
        d.line([C - 34, C + dy, C + 30, C + dy], fill=MUT, width=7)
    d.line([C - 34, C + 48, C + 10, C + 48], fill=ACC, width=7)
    return img


# ---- grade: лестница уровней ----
def grade():
    img, d = canvas()
    for i in range(4):
        y = C + 66 - i * 30
        x0 = C - 78 + i * 26
        d.line([x0, y, C + 70, y], fill=FG if i % 2 == 0 else MUT, width=9)
        d.line([x0, y, x0, y - 30], fill=FG if i % 2 == 0 else MUT, width=9)
    d.line([C + 70, C - 24, C + 70, C + 66], fill=FG, width=9)
    d.ellipse([C + 60, C - 40, C + 80, C - 20], fill=ACC)
    return img


# ---- currency: монета с символом валюты ----
def currency():
    img, d = canvas()
    ring(d, C, C, 70, FG, 10)
    # символ "$" из линий
    d.line([C + 6, C - 46, C + 6, C + 46], fill=FG, width=10)
    d.line([C - 14, C - 24, C + 26, C - 24], fill=FG, width=9)
    d.line([C - 14, C + 24, C + 26, C + 24], fill=FG, width=9)
    d.line([C + 6, C - 58, C + 6, C - 40], fill=ACC, width=9)
    d.line([C + 6, C + 40, C + 6, C + 58], fill=ACC, width=9)
    return img


# ---- outstaffing-rates: шкала с процентами ----
def outstaffing_rates():
    img, d = canvas()
    # горизонтальная шкала
    d.rounded_rectangle([C - 76, C - 10, C + 76, C + 10], radius=10,
                        outline=FG, width=8)
    # заполнение
    d.rounded_rectangle([C - 76, C - 10, C + 6, C + 10], radius=10,
                        fill=FG, width=0)
    # стрелка вверх
    d.polygon([(C - 6, C - 52), (C + 26, C - 52), (C + 10, C - 84)],
              outline=FG, width=8)
    d.line([C - 6, C - 52, C + 26, C - 52], fill=FG, width=8)
    d.line([C + 10, C - 84, C + 10, C - 52], fill=FG, width=8)
    d.line([C - 6, C - 52, C + 10, C - 84], fill=FG, width=8)
    d.line([C + 10, C - 84, C + 26, C - 52], fill=FG, width=8)
    # акцентная точка на шкале
    d.ellipse([C + 2, C - 22, C + 22, C - 2], fill=ACC)
    return img


# ---- employee-work-status: часы со статусом ----
def employee_work_status():
    img, d = canvas()
    ring(d, C, C, 70, FG, 10)
    d.line([C, C - 46, C, C], fill=FG, width=9)
    d.line([C, C, C + 34, C + 16], fill=FG, width=9)
    d.ellipse([C - 9, C - 9, C + 9, C + 9], fill=FG)
    # акцентная точка внизу
    d.ellipse([C - 14, C + 46, C + 14, C + 74], fill=ACC)
    return img


# ---- sign-icons: звезда/иконка ----
def sign_icons():
    img, d = canvas()
    cx, cy = C, C - 4
    pts = []
    for i in range(10):
        r = 74 if i % 2 == 0 else 30
        a = math.pi / 2 + i * math.pi / 5
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    d.polygon(pts, outline=FG, width=8)
    d.ellipse([C - 12, C + 44, C + 12, C + 68], fill=ACC)
    return img


GENERATORS = {
    "iteraction": iteraction,
    "specialisation": specialisation,
    "country": country,
    "city": city,
    "region": region,
    "ownershup": ownershup,
    "position": position,
    "file-type": file_type,
    "grade": grade,
    "currency": currency,
    "outstaffing-rates": outstaffing_rates,
    "employee-work-status": employee_work_status,
    "sign-icons": sign_icons,
}

if __name__ == "__main__":
    for name, fn in GENERATORS.items():
        img = fn()
        for theme in THEMES:
            save(img, theme)
        print("OK", name, img.size, img.mode)
