#!/usr/bin/env python3
"""
V9.4: 全包书腰裁剪
- 左边界：8%-25%范围找同色竖条(书耳/封面) → 取右边缘
- 右边界：50%-60%范围找同色竖条(书脊/封底) → 取左边缘
- 固定阈值优先，自适应阈值fallback
- 竖条评分：均匀度+两侧对比度+连续性
"""

import os, glob, time
import numpy as np
from PIL import Image
import matplotlib
import matplotlib.pyplot as plt
from scipy import ndimage
from scipy.ndimage import gaussian_filter1d

matplotlib.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'SimSun', 'DejaVu Sans']
matplotlib.rcParams['axes.unicode_minus'] = False

def load_and_resize(path, max_width=2000):
    img = Image.open(path).convert('RGB')
    w, h = img.size
    if w > max_width:
        ratio = max_width / w
        img = img.resize((max_width, int(h * ratio)), Image.LANCZOS)
    return np.array(img), w / img.size[0]

def compute_features(img_gray):
    h, w = img_gray.shape
    col_var = np.std(img_gray.astype(float), axis=0)
    v_diff = np.abs(np.diff(img_gray.astype(float), axis=0))
    col_cont = (v_diff < 12).sum(axis=0) / (h - 1)
    
    h_grad = np.abs(np.diff(img_gray.astype(float), axis=1))
    col_h = np.mean(h_grad, axis=0)
    col_h = np.append(col_h, col_h[-1])
    sobel_x = ndimage.sobel(img_gray.astype(float), axis=1)
    sobel_y = ndimage.sobel(img_gray.astype(float), axis=0)
    edge_mag = np.sqrt(sobel_x**2 + sobel_y**2)
    col_edge = np.mean(edge_mag, axis=0)
    complexity = col_h * 0.6 + col_edge * 0.4
    sigma = max(5, int(w * 0.01))
    complexity_smooth = gaussian_filter1d(complexity, sigma=sigma)
    return col_var, col_cont, complexity_smooth, complexity

def find_scored_strips(col_var, col_cont, complexity_smooth,
                       search_start, search_end,
                       min_width=5, max_width=100):
    """
    找所有候选竖条并评分
    固定阈值: var<30, cont>0.80
    自适应fallback: var<p40, cont>p60
    """
    if search_start >= search_end:
        return []
    
    rv = col_var[search_start:search_end]
    rc = col_cont[search_start:search_end]
    
    # 固定阈值
    is_uniform = (rv < 30) & (rc > 0.80)
    
    if not is_uniform.any():
        # 自适应fallback
        vt = np.percentile(rv, 40)
        ct = np.percentile(rc, 60)
        is_uniform = (rv < vt) & (rc > ct)
    
    if not is_uniform.any():
        return []
    
    padded = np.concatenate([[False], is_uniform, [False]])
    d = np.diff(padded.astype(int))
    starts = np.where(d == 1)[0]
    ends = np.where(d == -1)[0]
    
    candidates = []
    window = max(30, int(len(complexity_smooth) * 0.02))
    
    for i in range(len(starts)):
        s = search_start + starts[i]
        e = search_start + ends[i]
        w = e - s
        if w < min_width or w > max_width:
            continue
        
        avg_var = np.mean(col_var[s:e])
        avg_cont = np.mean(col_cont[s:e])
        uniformity = 1.0 / (avg_var + 1)
        
        left_c = np.mean(complexity_smooth[max(0, s-window):s])
        right_c = np.mean(complexity_smooth[e:min(len(complexity_smooth), e+window)])
        strip_c = np.mean(complexity_smooth[s:e])
        diff_l = abs(left_c - strip_c)
        diff_r = abs(right_c - strip_c)
        contrast = (diff_l + diff_r) / (max(left_c, right_c, strip_c) + 1e-6)
        
        score = uniformity * 2 + contrast * 3 + avg_cont * 1
        
        candidates.append({
            'start': s, 'end': e, 'width': w,
            'avg_var': avg_var, 'avg_cont': avg_cont,
            'contrast': contrast, 'score': score
        })
    
    candidates.sort(key=lambda x: x['score'], reverse=True)
    return candidates

def find_left_boundary(col_var, col_cont, cpx_smooth, w):
    """左边界: 8%-25%找竖条, 取右边缘"""
    search_start = int(w * 0.08)
    search_end = int(w * 0.25)
    min_w = max(5, int(w * 0.004))
    max_w = int(w * 0.05)
    
    cands = find_scored_strips(col_var, col_cont, cpx_smooth,
                               search_start, search_end,
                               min_width=min_w, max_width=max_w)
    
    if cands:
        best = cands[0]
        strip = (best['start'], best['end'])
        return strip[1], strip, cands, "strip"
    
    # fallback：复杂度跃升
    region = cpx_smooth[search_start:search_end]
    if len(region) > 0:
        win = max(10, int(w * 0.02))
        best_jump = 0
        best_pos = search_start
        for i in range(win, len(region) - win):
            left_avg = np.mean(region[max(0, i-win):i])
            right_avg = np.mean(region[i:i+win])
            jump = right_avg - left_avg
            if jump > best_jump:
                best_jump = jump
                best_pos = search_start + i
        rng = np.percentile(region, 90) - np.percentile(region, 10)
        if best_jump > rng * 0.3:
            return best_pos, None, [], "cpx_jump"
    
    return int(w * 0.08), None, [], "default"

def find_right_boundary(col_var, col_cont, cpx_smooth, w, left_crop):
    """右边界: 50%-60%找竖条, 取左边缘"""
    search_start = max(int(w * 0.50), left_crop + int(w * 0.25))
    search_end = int(w * 0.60)
    min_w = max(5, int(w * 0.004))
    max_w = int(w * 0.05)
    
    cands = find_scored_strips(col_var, col_cont, cpx_smooth,
                               search_start, search_end,
                               min_width=min_w, max_width=max_w)
    
    if cands:
        best = cands[0]
        strip = (best['start'], best['end'])
        # 用右边缘（竖条之后才是封底），保证至少到50%
        right = max(strip[1], int(w * 0.50))
        return right, strip, cands, "strip"
    
    # 扩大范围再试 (48%-62%)
    search_start2 = max(int(w * 0.48), left_crop + int(w * 0.22))
    search_end2 = int(w * 0.62)
    cands2 = find_scored_strips(col_var, col_cont, cpx_smooth,
                                search_start2, search_end2,
                                min_width=min_w, max_width=max_w)
    if cands2 and cands2[0]['score'] >= 2.0:
        best = cands2[0]
        strip = (best['start'], best['end'])
        right = strip[1]
        # 竖条在52%以下 → 是封面/书脊分界，需加书脊宽度
        if right < int(w * 0.52):
            right += int(w * 0.04)
        right = max(right, int(w * 0.52))
        return right, strip, cands2, "strip_wide"
    
    # fallback: 复杂度谷值
    s_s = max(int(w * 0.35), left_crop + int(w * 0.15))
    s_e = int(w * 0.60)
    region = cpx_smooth[s_s:s_e]
    if len(region) > 0:
        spine_center = s_s + np.argmin(region)
        right = min(spine_center + int(w * 0.03), int(w * 0.55))
        return right, None, [], "complexity"
    
    return int(w * 0.52), None, [], "default"

def refine_edges(col_var, col_cont, cpx_smooth, left_crop, right_crop, w):
    """对初步裁剪结果进行边缘精修：
    - 去除裁剪内部紧贴边缘的竖向空白/书脊条(高连续、低方差)
    - 仅在小范围内(≤10%宽度)微调，避免过度移动
    返回: (new_left, new_right, note)
    """
    L, R = left_crop, right_crop
    max_shift = max(int(w * 0.10), 10)
    min_width = max(int(w * 0.28), 50)
    moved = []

    # 往右推左边界：跳过高连续低方差的条带
    j = L
    limit = min(L + max_shift, R - min_width)
    while j < limit and col_cont[j] > 0.78 and col_var[j] < 28:
        j += 1
    if j > L:
        L = j
        moved.append(f"L+{j-left_crop}")

    # 往左拉右边界：去掉内侧空白/条带
    j = R
    limit = max(R - max_shift, L + min_width)
    while j > limit and col_cont[j-1] > 0.78 and col_var[j-1] < 28:
        j -= 1
    if j < R:
        R = j
        moved.append(f"R-{right_crop-j}")

    note = ",".join(moved) if moved else "none"
    return L, R, note

def fallback_best_window(col_var, col_cont, cpx_smooth, w, h):
    """兜底：在整幅图上用固定宽度滑窗寻找"最像前封面"的区域。
    - 窗口宽度 ~ [0.62, 0.80] * h（按常见封面纵横比估算），再裁剪到不超过图像宽度
    - 评分 = 窗口内复杂度均值 - 边缘空白惩罚
    返回: (start, end)
    """
    if w <= 0 or h <= 0:
        return 0, max(1, w)
    win = int(min(max(0.62 * h, 0.25 * w), 0.80 * h, 0.60 * w))
    win = max(50, min(win, w - 1))
    step = max(5, int(w * 0.005))

    best_s, best_score = 0, -1e9
    for s in range(0, w - win + 1, step):
        e = s + win
        score = float(np.mean(cpx_smooth[s:e]))
        # 左右边缘罚分：贴近边界且高连续低方差
        lb = slice(s, min(e, s + max(20, int(w * 0.01))))
        rb = slice(max(s, e - max(20, int(w * 0.01))), e)
        if (np.mean(col_cont[lb]) > 0.80 and np.mean(col_var[lb]) < 25):
            score -= 10
        if (np.mean(col_cont[rb]) > 0.80 and np.mean(col_var[rb]) < 25):
            score -= 10
        if score > best_score:
            best_score, best_s = score, s
    return best_s, min(w, best_s + win)

def safe_save(obj, path, is_plt=False, **kwargs):
    for attempt in range(3):
        try:
            if is_plt:
                obj.savefig(path, **kwargs)
            else:
                obj.save(path, **kwargs)
            return True
        except OSError:
            if attempt < 2:
                time.sleep(0.5)
            else:
                try:
                    if is_plt:
                        obj.canvas.draw()
                        buf = obj.canvas.buffer_rgba()
                        Image.frombuffer('RGBA', obj.canvas.get_width_height(), buf).save(path)
                    return True
                except Exception as e:
                    print(f"  保存失败: {e}")
                    return False

def analyze_and_crop(img_path, output_dir=None):
    print(f"\n{'='*60}")
    print(f"处理: {os.path.basename(img_path)}")
    print(f"{'='*60}")
    
    img, scale = load_and_resize(img_path, max_width=2000)
    img_gray = np.mean(img, axis=2).astype(np.uint8)
    h, w = img_gray.shape
    print(f"尺寸: {w}x{h} (缩放: {scale:.2f})")
    
    col_var, col_cont, cpx_s, cpx_r = compute_features(img_gray)
    
    left_crop, left_strip, lcands, lmethod = find_left_boundary(col_var, col_cont, cpx_s, w)
    if left_strip:
        print(f"左: {lmethod} [{left_strip[0]},{left_strip[1]}] w={left_strip[1]-left_strip[0]}")
    else:
        print(f"左: {lmethod}")
    for c in lcands[:3]:
        print(f"  L候选 [{c['start']},{c['end']}] w={c['width']} var={c['avg_var']:.1f} "
              f"cont={c['avg_cont']:.3f} ctr={c['contrast']:.2f} sc={c['score']:.2f}")
    print(f"左边界: {left_crop} ({left_crop/w*100:.1f}%)")
    
    right_crop, right_strip, rcands, rmethod = find_right_boundary(col_var, col_cont, cpx_s, w, left_crop)
    if right_strip:
        print(f"右: {rmethod} [{right_strip[0]},{right_strip[1]}] w={right_strip[1]-right_strip[0]}")
    else:
        print(f"右: {rmethod}")
    for c in rcands[:3]:
        print(f"  R候选 [{c['start']},{c['end']}] w={c['width']} var={c['avg_var']:.1f} "
              f"cont={c['avg_cont']:.3f} ctr={c['contrast']:.2f} sc={c['score']:.2f}")
    print(f"右边界: {right_crop} ({right_crop/w*100:.1f}%)")
    
    # 边缘精修：去掉贴边的空白/书脊条
    L2, R2, note = refine_edges(col_var, col_cont, cpx_s, left_crop, right_crop, w)
    if (L2, R2) != (left_crop, right_crop):
        print(f"精修: {note} -> [{L2},{R2}]")
        left_crop, right_crop = L2, R2
    
    if right_crop <= left_crop + int(w * 0.20):
        right_crop = left_crop + int(w * 0.30)
        print(f"警告: 过窄 → 右={right_crop}")
    
    crop_w = right_crop - left_crop
    orig_l = int(left_crop * scale)
    orig_r = int(right_crop * scale)
    print(f"裁剪: [{left_crop},{right_crop}] {crop_w}px({crop_w/w*100:.1f}%) → [{orig_l},{orig_r}]")
    
    # === 裁剪合理性校验 ===
    crop_ratio = crop_w / h if h > 0 else 0
    skip_crop = False

    # 如果左右边界明显偏离正常区间（左<5%或右>62%），尝试滑窗兜底
    if (left_crop < int(w * 0.05)) or (right_crop > int(w * 0.62)):
        fw_l, fw_r = fallback_best_window(col_var, col_cont, cpx_s, w, h)
        fb_w = fw_r - fw_l
        fb_ratio = fb_w / h if h > 0 else 0
        desired_min, desired_max = 0.60, 0.85
        adopt = False
        if crop_ratio > desired_max and desired_min <= fb_ratio <= desired_max:
            adopt = True
        if crop_ratio < desired_min and desired_min <= fb_ratio <= desired_max:
            adopt = True
        if not adopt and ((left_crop < int(w * 0.05)) or (right_crop > int(w * 0.62))):
            # 边界极端也放宽采用条件
            adopt = fb_ratio >= desired_min * 0.95
        
        # 复杂度比较：兜底窗口的复杂度显著更高则采用
        if not adopt:
            cur_score = float(np.mean(cpx_s[left_crop:right_crop])) if right_crop > left_crop else 0
            fb_score = float(np.mean(cpx_s[fw_l:fw_r]))
            if fb_score >= cur_score * 1.05 and desired_min * 0.9 <= fb_ratio <= desired_max * 1.05:
                adopt = True
        if adopt:
            print(f"兜底: 滑窗 [{fw_l},{fw_r}] 取代初选 [{left_crop},{right_crop}] (fb_ratio={fb_ratio:.2f})")
            left_crop, right_crop = fw_l, fw_r
            # 兜底后再做一次边缘精修
            L2, R2, note2 = refine_edges(col_var, col_cont, cpx_s, left_crop, right_crop, w)
            if (L2, R2) != (left_crop, right_crop):
                print(f"兜底后精修: {note2} -> [{L2},{R2}]")
                left_crop, right_crop = L2, R2
            crop_w = right_crop - left_crop
            orig_l = int(left_crop * scale)
            orig_r = int(right_crop * scale)
            crop_ratio = crop_w / h
        
        # 最终上限保护：过宽时限制到 0.85h（兜底内的快速限制）
        if crop_ratio > desired_max:
            target_w = int(desired_max * h)
            if right_crop - left_crop > target_w:
                right_crop = left_crop + target_w
                crop_w = target_w
                orig_r = int(right_crop * scale)
                crop_ratio = crop_w / h
    
    # 全局上限保护：过宽时限制到 0.85h（不论是否采用兜底）
    desired_min, desired_max = 0.60, 0.85
    if crop_ratio > desired_max:
        target_w = int(desired_max * h)
        # 哪侧更“空”就从哪侧收缩
        lb = max(0, left_crop)
        rb = min(w, right_crop)
        band = max(5, int(w * 0.02))
        left_band = float(np.mean(cpx_s[lb:min(w, lb + band)]))
        right_band = float(np.mean(cpx_s[max(0, rb - band):rb]))
        if right_band < left_band and rb - target_w >= lb + 10:
            right_crop = rb - target_w
        else:
            left_crop = max(0, rb - target_w)
        crop_w = right_crop - left_crop
        orig_l = int(left_crop * scale)
        orig_r = int(right_crop * scale)
        crop_ratio = crop_w / h
    
    # 裁剪过窄且左边界靠内 → 检查左侧是否有真实内容（非空白/折页）
    if crop_ratio < 0.6 and left_crop > int(w * 0.05):
        left_cpx = np.mean(cpx_s[0:left_crop]) if left_crop > 0 else 0
        crop_cpx = np.mean(cpx_s[left_crop:right_crop]) if right_crop > left_crop else 0
        if left_cpx > crop_cpx * 0.3:
            print(f"修正: 左侧有内容(cpx={left_cpx:.1f} vs {crop_cpx:.1f})，重置左边界为0")
            left_crop = 0
            left_strip = None
            lmethod = "reset"
            crop_w = right_crop
            orig_l = 0
            orig_r = int(right_crop * scale)
            crop_ratio = crop_w / h
    
    # 兜底：仍然过窄就保留原图
    if crop_ratio < 0.6:
        print(f"跳过裁剪: 宽高比 {crop_ratio:.2f} < 0.6，保留原图")
        skip_crop = True
    
    # 可视化
    fig, axes = plt.subplots(4, 1, figsize=(16, 16))
    axes[0].imshow(img)
    axes[0].axvline(x=left_crop, color='lime', linewidth=2, label=f'L {left_crop}')
    axes[0].axvline(x=right_crop, color='red', linewidth=2, label=f'R {right_crop}')
    if left_strip:
        axes[0].axvspan(left_strip[0], left_strip[1], alpha=0.3, color='yellow', label='L strip')
    if right_strip:
        axes[0].axvspan(right_strip[0], right_strip[1], alpha=0.3, color='magenta', label='R strip')
    title_suffix = ' [SKIPPED]' if skip_crop else ''
    axes[0].set_title(os.path.basename(img_path) + title_suffix)
    axes[0].legend(loc='upper right', fontsize=8)
    
    xr = np.arange(w)
    ax1 = axes[1]; ax2 = ax1.twinx()
    ax1.plot(xr, col_var, 'b-', alpha=0.5, linewidth=0.5)
    ax1.set_ylim(0, min(100, np.percentile(col_var, 98)*1.2))
    ax2.plot(xr, col_cont, 'r-', alpha=0.5, linewidth=0.5)
    ax2.set_ylim(0, 1.1)
    ax1.axvline(x=left_crop, color='lime'); ax1.axvline(x=right_crop, color='red')
    ax1.set_title('var(b) + cont(r)')
    
    axes[2].plot(xr, cpx_r, 'gray', alpha=0.3, linewidth=0.5)
    axes[2].plot(xr, cpx_s, 'b-', linewidth=1.5)
    axes[2].axvline(x=left_crop, color='lime', linewidth=2)
    axes[2].axvline(x=right_crop, color='red', linewidth=2)
    axes[2].set_title('complexity')
    
    if skip_crop:
        axes[3].imshow(img)
        axes[3].set_title('SKIPPED - 保留原图')
    else:
        axes[3].imshow(img[:, left_crop:right_crop, :])
        axes[3].set_title(f'crop [{left_crop},{right_crop}] -> [{orig_l},{orig_r}]')
    
    plt.tight_layout()
    if output_dir is None:
        output_dir = os.path.dirname(img_path)
    base = os.path.splitext(os.path.basename(img_path))[0].replace('[','(').replace(']',')')
    
    safe_save(fig, os.path.join(output_dir, f'{base}_analysis.png'), is_plt=True, dpi=100, bbox_inches='tight')
    plt.close()
    
    orig_img = Image.open(img_path).convert('RGB')
    ow, oh = orig_img.size
    if skip_crop:
        safe_save(orig_img, os.path.join(output_dir, f'{base}_cropped.jpg'), quality=95)
        print(f"SKIPPED")
    else:
        safe_save(orig_img.crop((orig_l, 0, orig_r, oh)),
                  os.path.join(output_dir, f'{base}_cropped.jpg'), quality=95)
        print(f"OK")
    
    return {
        'file': os.path.basename(img_path),
        'left_crop': orig_l, 'right_crop': orig_r,
        'left_strip': left_strip, 'right_strip': right_strip,
        'lm': lmethod, 'rm': rmethod,
        'pct': f"{left_crop/w*100:.1f}%-{right_crop/w*100:.1f}%",
        'skipped': skip_crop
    }

def main():
    d = os.path.dirname(os.path.abspath(__file__))
    files = []
    for p in ['*.jpg','*.jpeg','*.png']:
        files.extend(glob.glob(os.path.join(d, p)))
    files = [f for f in files if '_analysis' not in f and '_cropped' not in f]
    print(f"找到 {len(files)} 张图片")
    
    results = []
    for f in sorted(files):
        try:
            results.append(analyze_and_crop(f))
        except Exception as e:
            print(f"失败: {os.path.basename(f)}: {e}")
            import traceback; traceback.print_exc()
    
    skipped = [r for r in results if r.get('skipped')]
    cropped = [r for r in results if not r.get('skipped')]
    print(f"\n{'='*60}")
    print(f"汇总: 裁剪 {len(cropped)} 张, 跳过 {len(skipped)} 张")
    print(f"{'='*60}")
    for r in results:
        ls = f"L[{r['left_strip'][0]}-{r['left_strip'][1]}]" if r['left_strip'] else f"L:{r['lm']}"
        rs = f"R[{r['right_strip'][0]}-{r['right_strip'][1]}]" if r['right_strip'] else f"R:{r['rm']}"
        tag = ' SKIP' if r.get('skipped') else ''
        print(f"{r['file'][:28]:28s} [{r['left_crop']:5d},{r['right_crop']:5d}] {r['pct']:14s} {ls:20s} {rs}{tag}")

if __name__ == '__main__':
    main()
