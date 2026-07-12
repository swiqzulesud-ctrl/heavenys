"""
Generates assets/icon.png and assets/icon.ico for the Heavenys Launcher.
Run once: python generate_icon.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

os.makedirs("assets", exist_ok=True)

SIZE = 256
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Background circle — dark
cx, cy, r = SIZE // 2, SIZE // 2, SIZE // 2 - 4
draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(15, 15, 15, 255))

# Butter Yellow ring
draw.ellipse([cx - r, cy - r, cx + r, cy + r],
             outline=(247, 231, 142, 255), width=6)

# Star / sparkle ✦ character
try:
    font = ImageFont.truetype("arial.ttf", 130)
except Exception:
    font = ImageFont.load_default()

text = "✦"
bbox = draw.textbbox((0, 0), text, font=font)
tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
draw.text(
    (cx - tw // 2 - bbox[0], cy - th // 2 - bbox[1]),
    text, fill=(247, 231, 142, 255), font=font
)

# Save PNG
img.save("assets/icon.png")

# Save ICO (multiple sizes for Windows)
ico_sizes = [(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
icons = [img.resize(s, Image.LANCZOS) for s in ico_sizes]
icons[0].save("assets/icon.ico", format="ICO", sizes=ico_sizes,
              append_images=icons[1:])

print("Generated assets/icon.png and assets/icon.ico")
