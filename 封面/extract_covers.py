#!/usr/bin/env python3
"""从 Komga 原始书籍文件中随机提取50张封面（第一页原图）"""

import os
import sqlite3
import zipfile
from pathlib import Path
from urllib.parse import unquote
from urllib.request import url2pathname

DB_PATH = r'G:\04 AI\komga\config-dir\localdb.sqlite'
OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
COUNT = 50

# 清理旧的提取文件
for f in os.listdir(OUTPUT_DIR):
    if f.endswith(('.jpg', '.jpeg', '.png', '.webp', '.gif', '.bmp')) \
       and not any(k in f for k in ['_analysis', '_cropped']):
        os.remove(os.path.join(OUTPUT_DIR, f))
        
print("已清理旧文件")

conn = sqlite3.connect(DB_PATH)
cursor = conn.cursor()

# 查询有双页封面的书籍（缩略图宽>高），获取书籍文件路径
cursor.execute("""
    SELECT b.ID, b.URL, m.MEDIA_TYPE
    FROM BOOK b
    JOIN THUMBNAIL_BOOK tb ON tb.BOOK_ID = b.ID
    JOIN MEDIA m ON m.BOOK_ID = b.ID
    WHERE tb.SELECTED = 1
      AND tb.WIDTH > tb.HEIGHT
      AND m.STATUS = 'READY'
    ORDER BY RANDOM()
    LIMIT ?
""", (COUNT * 3,))

rows = cursor.fetchall()
conn.close()
print(f"候选书籍: {len(rows)}")

IMAGE_EXTS = {'.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp'}

saved = 0
for book_id, url, media_type in rows:
    if saved >= COUNT:
        break

    try:
        # file:///C:/path -> C:\path
        if url.startswith('file:'):
            path = url2pathname(unquote(url[5:]))
        else:
            path = url
    except Exception:
        continue

    if not os.path.exists(path):
        continue

    try:
        with zipfile.ZipFile(path, 'r') as zf:
            entries = sorted([
                n for n in zf.namelist()
                if not n.endswith('/') and Path(n).suffix.lower() in IMAGE_EXTS
            ])
            if not entries:
                continue
            data = zf.read(entries[0])
            ext = Path(entries[0]).suffix.lower().lstrip('.')

        filename = f"{book_id}.{ext}"
        with open(os.path.join(OUTPUT_DIR, filename), 'wb') as f:
            f.write(data)
        saved += 1
        if saved % 10 == 0:
            print(f"  已保存 {saved}/{COUNT}")

    except Exception as e:
        continue

print(f"完成，共保存 {saved} 张原始封面到 {OUTPUT_DIR}")
