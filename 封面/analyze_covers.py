"""
V4: 找书脊 → 判断封面侧 → 从边缘向内扫描找书肩分界

策略：
1. 在中间区域找书脊 seam（复用现有的亮度突变+边缘+方差变化 综合评分）
2. 比较书脊两侧的视觉复杂度，确定封面在哪侧
3. 在封面侧，从边缘向书脊方向扫描，找“书肩→封面”的过渡点
4. 裁切 = 过渡点 到 书脊另一侧
"""

import os
import glob
import numpy as np
from PIL import Image, ImageDraw

ANALYSIS_HEIGHT = 300
STRIP_DIR = os.path.dirname(os.path.abspath(__file__))


def load_and_downscale(path):
    img = Image.open(path).convert("RGB")
    orig_w, orig_h = img.size
    scale = ANALYSIS_HEIGHT / orig_h
    new_w = round(orig_w * scale)
    small = img.resize((new_w, ANALYSIS_HEIGHT), Image.LANCZOS)
    return img, small, scale


def compute_column_features(img_array):
    h, w, _ = img_array.shape
    gray = 0.299 * img_array[:, :, 0] + 0.587 * img_array[:, :, 1] + 0.114 * img_array[:, :, 2]

    col_brightness = gray.mean(axis=0)

    h_grad = np.zeros_like(gray)
    h_grad[:, 1:-1] = np.abs(gray[:, 2:] - gray[:, :-2]) / 2.0
    col_edge = h_grad.mean(axis=0)

    r, g, b = img_array[:, :, 0].astype(float), img_array[:, :, 1].astype(float), img_array[:, :, 2].astype(float)
    col_color_var = r.var(axis=0) + g.var(axis=0) + b.var(axis=0)

    col_bright_var = gray.var(axis=0)

    edge_threshold = 15.0
    col_continuity = (h_grad > edge_threshold).sum(axis=0) / h

    return {
        "brightness": col_brightness,
        "edge": col_edge,
        "color_var": col_color_var,
        "bright_var": col_bright_var,
        "continuity": col_continuity,
    }


def smooth(arr, window=5):
    kernel = np.ones(window) / window
    return np.convolve(arr, kernel, mode="same")


def normalize(arr):
    mn, mx = arr.min(), arr.max()
    if mx - mn < 1e-9:
        return np.zeros_like(arr)
    return (arr - mn) / (mx - mn)


def find_seam(features, width, search_start_pct=0.40, search_end_pct=0.60):
    """
    V7: Hybrid - local features (continuity weighted) + medium window bonus
    Search range 40-60% to exclude false seams
    """
    brightness = smooth(features["brightness"], 5)
    col_edge = features["edge"]
    continuity = features["continuity"]

    search_start = int(width * search_start_pct)
    search_end = int(width * search_end_pct)

    max_edge = col_edge[search_start:search_end + 1].max()
    if max_edge < 1e-9:
        max_edge = 1.0
    max_cont = continuity[search_start:search_end + 1].max()
    if max_cont < 1e-9:
        max_cont = 1.0

    med_window = max(30, int(width * 0.15))
    local_window = 30

    best_score = -1e9
    best_col = (search_start + search_end) // 2
    candidates = []

    for x in range(search_start, search_end + 1):
        left_slice = brightness[max(0, x - local_window):x]
        right_slice = brightness[x + 1:min(width, x + local_window + 1)]
        if len(left_slice) == 0 or len(right_slice) == 0:
            continue

        bright_transition = abs(right_slice.mean() - left_slice.mean())
        edge_score = col_edge[x] / max_edge

        left_var = np.var(brightness[max(0, x - local_window):x])
        right_var = np.var(brightness[x + 1:min(width, x + local_window + 1)])
        variance_change = abs(left_var - right_var)

        cont_score = continuity[x] / max_cont

        # Local score: continuity weighted higher (5x)
        local_score = (bright_transition * 2.0 +
                       edge_score * 1.0 +
                       variance_change * 1.5 +
                       cont_score * 5.0)

        # Medium window difference bonus
        ml = brightness[max(0, x - med_window):x].mean()
        mr = brightness[x:min(width, x + med_window)].mean()
        med_bright = abs(ml - mr)

        mel = col_edge[max(0, x - med_window):x].mean()
        mer = col_edge[x:min(width, x + med_window)].mean()
        med_edge = abs(mel - mer)

        bonus = med_bright * 0.3 + med_edge * 5.0

        score = local_score + bonus
        candidates.append((x, score, cont_score, bright_transition, edge_score, bonus))

        if score > best_score:
            best_score = score
            best_col = x

    # Debug: top 5 candidates
    candidates.sort(key=lambda c: c[1], reverse=True)
    for i, (cx, cs, cc, cb, ce, cg) in enumerate(candidates[:5]):
        print(f"    Top{i+1}: col={cx} ({cx/width*100:.1f}%) score={cs:.2f} "
              f"[cont={cc:.2f} bright={cb:.1f} edge={ce:.2f} bonus={cg:.2f}]")

    return best_col


def detect_cover_side(features, width, seam_col):
    """
    V7: Complexity + edge variance for cover side detection
    Complexity: higher side tends to be cover (more visual content)
    Edge variance: higher side tends to be cover (title text causes peaks/valleys)
    """
    edge = features["edge"]
    color_var = features["color_var"]
    bright_var = features["bright_var"]

    def half_complexity(start, end):
        if end <= start:
            return 0
        e = edge[start:end].mean()
        c = color_var[start:end].mean()
        b = bright_var[start:end].mean()
        return e * 3.0 + c * 0.01 + b * 0.01

    left_complex = half_complexity(0, seam_col)
    right_complex = half_complexity(seam_col, width)

    # Edge variance: cover has big title (high edge) + solid color areas (low edge) = higher variance
    left_edge_var = np.var(edge[:seam_col]) if seam_col > 0 else 0
    right_edge_var = np.var(edge[seam_col:]) if seam_col < width else 0

    # Normalize and combine
    max_c = max(left_complex, right_complex, 1e-9)
    max_v = max(left_edge_var, right_edge_var, 1e-9)

    left_score = (left_complex / max_c) * 1.0 + (left_edge_var / max_v) * 1.0
    right_score = (right_complex / max_c) * 1.0 + (right_edge_var / max_v) * 1.0

    print(f"    Complexity: left={left_complex:.2f}, right={right_complex:.2f}")
    print(f"    Edge variance: left={left_edge_var:.2f}, right={right_edge_var:.2f}")
    print(f"    Combined: left={left_score:.3f}, right={right_score:.3f}")
    return left_score >= right_score


def find_flap_boundary(features, width, seam_col, cover_is_left):
    """
    在封面侧找书肩→封面的过渡点
    更保守的策略：只有边缘区域的复杂度明显低于封面主体区域时，才认为有书肩
    """
    edge = smooth(features["edge"], 7)
    color_var = smooth(features["color_var"], 7)

    # 计算每列的局部复杂度
    local_window = max(10, int(width * 0.03))
    complexity = np.zeros(width)
    for x in range(width):
        left = max(0, x - local_window // 2)
        right = min(width, x + local_window // 2)
        complexity[x] = edge[left:right].mean() * 3.0 + color_var[left:right].mean() * 0.005
    complexity = smooth(complexity, 11)

    if cover_is_left:
        # 计算封面主体区域（靠近书脊的 60%）的平均复杂度
        cover_main_start = int(seam_col * 0.4)
        cover_main_end = seam_col
        cover_avg = complexity[cover_main_start:cover_main_end].mean()

        # 从边缘向内扫描，找到复杂度达到封面主体 50% 的位置
        threshold = cover_avg * 0.50
        search_limit = int(seam_col * 0.35)  # 书肩不会超过封面侧的 35%

        best_x = 0
        for x in range(min(search_limit, width)):
            if complexity[x] >= threshold:
                best_x = max(0, x - 3)  # 稍微往外留余量
                break

        # 验证：如果切掉的部分太少（<3%），就不切了
        if best_x < width * 0.03:
            best_x = 0

        return best_x
    else:
        cover_main_start = seam_col
        cover_main_end = seam_col + int((width - seam_col) * 0.6)
        cover_avg = complexity[cover_main_start:min(cover_main_end, width)].mean()

        threshold = cover_avg * 0.50
        search_start = width - 1
        search_limit = seam_col + int((width - seam_col) * 0.65)

        best_x = width
        for x in range(search_start, max(search_limit, 0), -1):
            if complexity[x] >= threshold:
                best_x = min(width, x + 3)
                break

        if best_x > width * 0.97:
            best_x = width

        return best_x


def draw_analysis(small, features, width, height, seam_col, crop_left, crop_right, flap_bound):
    """画分析图"""
    chart_h = 120
    n_charts = 4
    total_h = height + chart_h * n_charts + 30
    result = Image.new("RGB", (width, total_h), (255, 255, 255))

    # 标记缩略图
    marked = small.copy()
    draw = ImageDraw.Draw(marked)
    draw.line([(seam_col, 0), (seam_col, height)], fill=(255, 0, 0), width=2)  # 书脊=红
    draw.line([(crop_left, 0), (crop_left, height)], fill=(0, 255, 0), width=3)  # 裁切=绿
    draw.line([(crop_right, 0), (crop_right, height)], fill=(0, 255, 0), width=3)
    draw.line([(flap_bound, 0), (flap_bound, height)], fill=(255, 255, 0), width=2)  # 书肩=黄
    result.paste(marked, (0, 0))

    draw2 = ImageDraw.Draw(result)
    charts = [
        ("brightness", (0, 0, 200), "Brightness"),
        ("edge", (200, 0, 0), "Edge Density"),
        ("color_var", (0, 160, 0), "Color Variance"),
        ("continuity", (160, 0, 160), "Continuity"),
    ]

    for idx, (key, color, label) in enumerate(charts):
        y_off = height + idx * chart_h + 15
        arr = normalize(smooth(features[key], 5))
        draw2.text((5, y_off - 12), label, fill=color)

        for x in range(1, len(arr)):
            y1 = y_off + chart_h - 5 - int(arr[x - 1] * (chart_h - 10))
            y2 = y_off + chart_h - 5 - int(arr[x] * (chart_h - 10))
            draw2.line([(x - 1, y1), (x, y2)], fill=color, width=1)

        draw2.line([(seam_col, y_off), (seam_col, y_off + chart_h)], fill=(255, 0, 0), width=1)
        draw2.line([(crop_left, y_off), (crop_left, y_off + chart_h)], fill=(0, 200, 0), width=2)
        draw2.line([(crop_right, y_off), (crop_right, y_off + chart_h)], fill=(0, 200, 0), width=2)
        draw2.line([(flap_bound, y_off), (flap_bound, y_off + chart_h)], fill=(200, 200, 0), width=1)

    return result


def process_image(path):
    name = os.path.splitext(os.path.basename(path))[0]
    print(f"\n{'='*60}")
    print(f"Processing: {name}")

    orig, small, scale = load_and_downscale(path)
    arr = np.array(small)
    h, w, _ = arr.shape
    ratio = w / h

    print(f"  Original: {orig.size[0]}x{orig.size[1]}")
    print(f"  Analysis: {w}x{h}, ratio: {ratio:.2f}")

    features = compute_column_features(arr)

    # Step 1: 找书脊
    seam_col = find_seam(features, w)
    print(f"  Seam at: {seam_col} ({seam_col/w*100:.1f}%)")

    # Step 2: 判断封面侧
    cover_is_left = detect_cover_side(features, w, seam_col)
    print(f"  Cover is on: {'LEFT' if cover_is_left else 'RIGHT'}")

    # Step 3: 找书肩边界
    flap_bound = find_flap_boundary(features, w, seam_col, cover_is_left)
    print(f"  Flap boundary at: {flap_bound} ({flap_bound/w*100:.1f}%)")

    # Step 4: 确定裁切范围
    spine_margin = max(3, int(w * 0.01))  # 书脊宽度余量
    if cover_is_left:
        crop_left = flap_bound
        crop_right = seam_col + spine_margin
    else:
        crop_left = seam_col - spine_margin
        crop_right = flap_bound

    crop_left = max(0, crop_left)
    crop_right = min(w, crop_right)
    print(f"  Crop (analysis): [{crop_left}, {crop_right}] width={crop_right-crop_left}")

    # 映射回原图坐标
    orig_left = max(0, int(crop_left / scale))
    orig_right = min(orig.size[0], int(crop_right / scale))
    print(f"  Crop (original): [{orig_left}, {orig_right}] width={orig_right - orig_left}")

    # 画分析图
    analysis = draw_analysis(small, features, w, h, seam_col, crop_left, crop_right, flap_bound)
    analysis.save(os.path.join(STRIP_DIR, f"{name}_analysis.png"))

    # 裁切
    cropped = orig.crop((orig_left, 0, orig_right, orig.size[1]))
    cropped.save(os.path.join(STRIP_DIR, f"{name}_cropped.jpg"), quality=95)

    print(f"  Saved: {name}_analysis.png, {name}_cropped.jpg")
    return {
        "name": name,
        "ratio": ratio,
        "seam": seam_col,
        "cover_is_left": cover_is_left,
        "flap": flap_bound,
        "crop": (crop_left, crop_right),
        "crop_orig": (orig_left, orig_right),
    }


def main():
    patterns = ["*.jpg", "*.png", "*.jpeg"]
    files = []
    for p in patterns:
        files.extend(glob.glob(os.path.join(STRIP_DIR, p)))
    files = [f for f in files if "_analysis" not in f and "_cropped" not in f]

    print(f"Found {len(files)} cover images")

    results = []
    for f in sorted(files):
        try:
            r = process_image(f)
            results.append(r)
        except Exception as e:
            import traceback
            print(f"  ERROR: {e}")
            traceback.print_exc()

    print(f"\n{'='*60}")
    print("Summary:")
    for r in results:
        print(f"  {r['name']}: ratio={r['ratio']:.2f}, crop={r['crop']}, orig={r['crop_orig']}")


if __name__ == "__main__":
    main()
