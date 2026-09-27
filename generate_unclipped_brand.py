import zlib
import struct
import math
import os

def write_png(filename, width, height, rgba_data):
    def chunk(chunk_type, data):
        return struct.pack(">I", len(data)) + chunk_type + data + struct.pack(">I", zlib.crc32(chunk_type + data) & 0xffffffff)

    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)  # filter type 0
        raw_data.extend(rgba_data[y * width * 4 : (y + 1) * width * 4])

    png_bytes = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png_bytes += chunk(b"IHDR", ihdr)
    png_bytes += chunk(b"IDAT", zlib.compress(bytes(raw_data), 6))
    png_bytes += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, "wb") as f:
        f.write(png_bytes)

def lerp_color(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return (
        int(c1[0] + (c2[0] - c1[0]) * t),
        int(c1[1] + (c2[1] - c1[1]) * t),
        int(c1[2] + (c2[2] - c1[2]) * t),
        int(c1[3] + (c2[3] - c1[3]) * t)
    )

def point_in_polygon(px, py, poly):
    inside = False
    n = len(poly)
    for i in range(n):
        x1, y1 = poly[i]
        x2, y2 = poly[(i + 1) % n]
        if ((y1 > py) != (y2 > py)) and (px < (x2 - x1) * (py - y1) / (y2 - y1 + 1e-9) + x1):
            inside = not inside
    return inside

def sample_bezier(p0, p1, p2, p3, steps=16):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        u = 1 - t
        x = u*u*u*p0[0] + 3*u*u*t*p1[0] + 3*u*t*t*p2[0] + t*t*t*p3[0]
        y = u*u*u*p0[1] + 3*u*u*t*p1[1] + 3*u*t*t*p2[1] + t*t*t*p3[1]
        pts.append((x, y))
    return pts

# --- 1. RENDER BRAND ICON (512x512) WITH SAFE MARGINS ---
def render_brand_icon(size=512, transparent_bg=False):
    ss = 2
    w = size * ss
    h = size * ss
    buffer = bytearray(w * h * 4)

    cx, cy = w / 2.0, h / 2.0
    half_box = w * 0.47
    corner_r = w * 0.20

    def in_squircle(px, py):
        dx = abs(px - cx)
        dy = abs(py - cy)
        if dx > half_box or dy > half_box: return False
        if dx <= half_box - corner_r or dy <= half_box - corner_r: return True
        qx = dx - (half_box - corner_r)
        qy = dy - (half_box - corner_r)
        return qx*qx + qy*qy <= corner_r * corner_r

    if not transparent_bg:
        # Background gradient
        for y in range(h):
            for x in range(w):
                if in_squircle(x, y):
                    idx = (y * w + x) * 4
                    t_bg = y / h
                    bg_col = lerp_color((255, 255, 255, 255), (240, 247, 255, 255), t_bg)
                    buffer[idx : idx + 4] = bytes(bg_col)

        # Bottom subtle accent wave
        wave_pts = []
        wave_pts.append((w * 0.03, h * 0.90))
        wave_pts.extend(sample_bezier((w * 0.03, h * 0.90), (w * 0.35, h * 0.95), (w * 0.65, h * 0.88), (w * 0.97, h * 0.72), 24))
        wave_pts.append((w * 0.97, h * 0.97))
        wave_pts.append((w * 0.03, h * 0.97))

        c_wave_left = (56, 189, 248, 220)
        c_wave_right = (2, 132, 199, 230)
        for y in range(int(h * 0.65), h):
            for x in range(w):
                if in_squircle(x, y) and point_in_polygon(x, y, wave_pts):
                    idx = (y * w + x) * 4
                    t_w = x / w
                    col = lerp_color(c_wave_left, c_wave_right, t_w)
                    buffer[idx : idx + 4] = bytes(col)

    # Emblem Dimensions inside the 512 canvas (scaled for ~20% breathing room)
    # Center of emblem at x = 0.50 * w, y = 0.40 * h
    scale = w * 0.82
    e_ox = w * 0.50 - scale * 0.518
    e_oy = h * 0.38 - scale * 0.370

    def pt_map(pt):
        return (e_ox + pt[0] * scale, e_oy + pt[1] * scale)

    def bezier_map(p0, p1, p2, p3, steps=12):
        return [pt_map(p) for p in sample_bezier(p0, p1, p2, p3, steps)]

    # Top-Left Orange Wing
    poly_tl = [pt_map((0.318, 0.205))]
    poly_tl.extend(bezier_map((0.318, 0.205), (0.380, 0.170), (0.440, 0.155), (0.518, 0.144)))
    poly_tl.append(pt_map((0.518, 0.328)))
    poly_tl.extend(bezier_map((0.518, 0.328), (0.440, 0.310), (0.370, 0.310), (0.318, 0.320)))

    # Top-Right Orange Wing
    poly_tr = [pt_map((0.542, 0.328)), pt_map((0.542, 0.176))]
    poly_tr.extend(bezier_map((0.542, 0.176), (0.600, 0.150), (0.660, 0.140), (0.690, 0.160)))
    poly_tr.extend(bezier_map((0.690, 0.160), (0.700, 0.180), (0.695, 0.260), (0.686, 0.334)))

    # Blue Rescuing Bridge Wave
    poly_blue = [pt_map((0.318, 0.342)), pt_map((0.518, 0.342))]
    poly_blue.extend(bezier_map((0.518, 0.342), (0.570, 0.395), (0.640, 0.420), (0.690, 0.404), 14))
    poly_blue.append(pt_map((0.690, 0.585)))
    poly_blue.extend(bezier_map((0.690, 0.585), (0.650, 0.625), (0.580, 0.636), (0.530, 0.636), 12))
    poly_blue.append(pt_map((0.530, 0.485)))
    poly_blue.append(pt_map((0.492, 0.485)))
    poly_blue.append(pt_map((0.492, 0.628)))
    poly_blue.append(pt_map((0.318, 0.628)))

    c_orange_top = (255, 193, 7, 255)
    c_orange_bot = (235, 60, 0, 255)
    c_cyan_light = (0, 220, 255, 255)
    c_cyan_mid = (0, 150, 255, 255)
    c_blue_royal = (0, 90, 235, 255)
    c_blue_ocean = (0, 45, 175, 255)

    for y in range(h):
        for x in range(w):
            idx = (y * w + x) * 4
            if point_in_polygon(x, y, poly_tl) or point_in_polygon(x, y, poly_tr):
                t_y = (y - pt_map((0, 0.14))[1]) / (scale * 0.19)
                t_y = max(0.0, min(1.0, t_y))
                col = lerp_color(c_orange_top, c_orange_bot, t_y)
                buffer[idx : idx + 4] = bytes(col)
            elif point_in_polygon(x, y, poly_blue):
                t_vert = (y - pt_map((0, 0.34))[1]) / (scale * 0.30)
                t_horiz = (x - pt_map((0.318, 0))[0]) / (scale * 0.372)
                wave_t = t_vert + 0.25 * math.sin(max(0.0, min(1.0, t_horiz)) * math.pi)
                if wave_t < 0.38:
                    col = lerp_color(c_cyan_light, c_cyan_mid, wave_t / 0.38)
                elif wave_t < 0.70:
                    col = lerp_color(c_cyan_mid, c_blue_royal, (wave_t - 0.38) / 0.32)
                else:
                    col = lerp_color(c_blue_royal, c_blue_ocean, (wave_t - 0.70) / 0.30)
                buffer[idx : idx + 4] = bytes(col)

    # Wordmark "KINASSIST" (Centered with safe 18% horizontal margins)
    navy = (16, 53, 90, 255)
    text_y = h * 0.655
    text_h = h * 0.088
    text_w = w * 0.620  # leaves 19% padding on both sides
    start_x = (w - text_w) / 2.0

    def slant(pt):
        base_y = text_y + text_h
        shear = 0.14
        return (pt[0] + (base_y - pt[1]) * shear, pt[1])

    letter_polys = []
    # K
    bx = start_x
    letter_polys.append([slant((bx, text_y)), slant((bx + text_w*0.024, text_y)), slant((bx + text_w*0.024, text_y + text_h)), slant((bx, text_y + text_h))])
    letter_polys.append([slant((bx + text_w*0.020, text_y + text_h*0.48)), slant((bx + text_w*0.082, text_y)), slant((bx + text_w*0.110, text_y)), slant((bx + text_w*0.040, text_y + text_h*0.56))])
    letter_polys.append([slant((bx + text_w*0.034, text_y + text_h*0.50)), slant((bx + text_w*0.076, text_y + text_h*0.50)), slant((bx + text_w*0.112, text_y + text_h)), slant((bx + text_w*0.080, text_y + text_h))])
    # I
    ix = start_x + text_w * 0.136
    letter_polys.append([slant((ix, text_y)), slant((ix + text_w*0.026, text_y)), slant((ix + text_w*0.026, text_y + text_h)), slant((ix, text_y + text_h))])
    # N
    nx = start_x + text_w * 0.188
    letter_polys.append([slant((nx, text_y)), slant((nx + text_w*0.024, text_y)), slant((nx + text_w*0.024, text_y + text_h)), slant((nx, text_y + text_h))])
    letter_polys.append([slant((nx + text_w*0.015, text_y)), slant((nx + text_w*0.042, text_y)), slant((nx + text_w*0.096, text_y + text_h)), slant((nx + text_w*0.068, text_y + text_h))])
    letter_polys.append([slant((nx + text_w*0.074, text_y)), slant((nx + text_w*0.098, text_y)), slant((nx + text_w*0.098, text_y + text_h)), slant((nx + text_w*0.074, text_y + text_h))])
    # A
    ax = start_x + text_w * 0.320
    letter_polys.append([slant((ax + text_w*0.040, text_y)), slant((ax + text_w*0.066, text_y)), slant((ax, text_y + text_h)), slant((ax - text_w*0.024, text_y + text_h))])
    letter_polys.append([slant((ax + text_w*0.040, text_y)), slant((ax + text_w*0.066, text_y)), slant((ax + text_w*0.110, text_y + text_h)), slant((ax + text_w*0.084, text_y + text_h))])
    letter_polys.append([slant((ax + text_w*0.010, text_y + text_h*0.60)), slant((ax + text_w*0.082, text_y + text_h*0.60)), slant((ax + text_w*0.082, text_y + text_h*0.74)), slant((ax + text_w*0.010, text_y + text_h*0.74))])
    def make_s(sx):
        return [
            [slant((sx, text_y)), slant((sx + text_w*0.088, text_y)), slant((sx + text_w*0.088, text_y + text_h*0.22)), slant((sx, text_y + text_h*0.22))],
            [slant((sx, text_y)), slant((sx + text_w*0.026, text_y)), slant((sx + text_w*0.026, text_y + text_h*0.50)), slant((sx, text_y + text_h*0.50))],
            [slant((sx, text_y + text_h*0.40)), slant((sx + text_w*0.088, text_y + text_h*0.40)), slant((sx + text_w*0.088, text_y + text_h*0.60)), slant((sx, text_y + text_h*0.60))],
            [slant((sx + text_w*0.062, text_y + text_h*0.50)), slant((sx + text_w*0.088, text_y + text_h*0.50)), slant((sx + text_w*0.088, text_y + text_h)), slant((sx + text_w*0.062, text_y + text_h))],
            [slant((sx, text_y + text_h*0.78)), slant((sx + text_w*0.088, text_y + text_h*0.78)), slant((sx + text_w*0.088, text_y + text_h)), slant((sx, text_y + text_h))]
        ]
    letter_polys.extend(make_s(start_x + text_w * 0.442))
    letter_polys.extend(make_s(start_x + text_w * 0.548))
    i2x = start_x + text_w * 0.655
    letter_polys.append([slant((i2x, text_y)), slant((i2x + text_w*0.026, text_y)), slant((i2x + text_w*0.026, text_y + text_h)), slant((i2x, text_y + text_h))])
    letter_polys.extend(make_s(start_x + text_w * 0.706))
    tx = start_x + text_w * 0.812
    letter_polys.append([slant((tx, text_y)), slant((tx + text_w*0.110, text_y)), slant((tx + text_w*0.110, text_y + text_h*0.22)), slant((tx, text_y + text_h*0.22))])
    letter_polys.append([slant((tx + text_w*0.042, text_y)), slant((tx + text_w*0.068, text_y)), slant((tx + text_w*0.068, text_y + text_h)), slant((tx + text_w*0.042, text_y + text_h))])

    for p in letter_polys:
        min_x = max(0, int(min(pt[0] for pt in p)))
        max_x = min(w - 1, int(max(pt[0] for pt in p)) + 1)
        min_y = max(0, int(min(pt[1] for pt in p)))
        max_y = min(h - 1, int(max(pt[1] for pt in p)) + 1)
        for py in range(min_y, max_y + 1):
            for px in range(min_x, max_x + 1):
                if point_in_polygon(px, py, p):
                    idx = (py * w + px) * 4
                    buffer[idx : idx + 4] = bytes(navy)

    # Downsample
    out_buffer = bytearray(size * size * 4)
    samples = ss * ss
    for out_y in range(size):
        for out_x in range(size):
            r_acc, g_acc, b_acc, a_acc = 0, 0, 0, 0
            for sy in range(ss):
                for sx in range(ss):
                    in_idx = ((out_y * ss + sy) * w + (out_x * ss + sx)) * 4
                    r_acc += buffer[in_idx]
                    g_acc += buffer[in_idx + 1]
                    b_acc += buffer[in_idx + 2]
                    a_acc += buffer[in_idx + 3]
            out_idx = (out_y * size + out_x) * 4
            out_buffer[out_idx] = r_acc // samples
            out_buffer[out_idx + 1] = g_acc // samples
            out_buffer[out_idx + 2] = b_acc // samples
            out_buffer[out_idx + 3] = a_acc // samples

    return bytes(out_buffer)

# --- 2. RENDER PURE EMBLEM MARK (512x512) ON TRANSPARENT BG ---
def render_emblem_mark(size=512):
    ss = 2
    w = size * ss
    h = size * ss
    buffer = bytearray(w * h * 4)

    # Center emblem at (0.50 * w, 0.50 * h)
    scale = w * 1.15
    e_ox = w * 0.50 - scale * 0.518
    e_oy = h * 0.50 - scale * 0.380

    def pt_map(pt):
        return (e_ox + pt[0] * scale, e_oy + pt[1] * scale)

    def bezier_map(p0, p1, p2, p3, steps=12):
        return [pt_map(p) for p in sample_bezier(p0, p1, p2, p3, steps)]

    poly_tl = [pt_map((0.318, 0.205))]
    poly_tl.extend(bezier_map((0.318, 0.205), (0.380, 0.170), (0.440, 0.155), (0.518, 0.144)))
    poly_tl.append(pt_map((0.518, 0.328)))
    poly_tl.extend(bezier_map((0.518, 0.328), (0.440, 0.310), (0.370, 0.310), (0.318, 0.320)))

    poly_tr = [pt_map((0.542, 0.328)), pt_map((0.542, 0.176))]
    poly_tr.extend(bezier_map((0.542, 0.176), (0.600, 0.150), (0.660, 0.140), (0.690, 0.160)))
    poly_tr.extend(bezier_map((0.690, 0.160), (0.700, 0.180), (0.695, 0.260), (0.686, 0.334)))

    poly_blue = [pt_map((0.318, 0.342)), pt_map((0.518, 0.342))]
    poly_blue.extend(bezier_map((0.518, 0.342), (0.570, 0.395), (0.640, 0.420), (0.690, 0.404), 14))
    poly_blue.append(pt_map((0.690, 0.585)))
    poly_blue.extend(bezier_map((0.690, 0.585), (0.650, 0.625), (0.580, 0.636), (0.530, 0.636), 12))
    poly_blue.append(pt_map((0.530, 0.485)))
    poly_blue.append(pt_map((0.492, 0.485)))
    poly_blue.append(pt_map((0.492, 0.628)))
    poly_blue.append(pt_map((0.318, 0.628)))

    c_orange_top = (255, 193, 7, 255)
    c_orange_bot = (235, 60, 0, 255)
    c_cyan_light = (0, 220, 255, 255)
    c_cyan_mid = (0, 150, 255, 255)
    c_blue_royal = (0, 90, 235, 255)
    c_blue_ocean = (0, 45, 175, 255)

    for y in range(h):
        for x in range(w):
            idx = (y * w + x) * 4
            if point_in_polygon(x, y, poly_tl) or point_in_polygon(x, y, poly_tr):
                t_y = (y - pt_map((0, 0.14))[1]) / (scale * 0.19)
                t_y = max(0.0, min(1.0, t_y))
                col = lerp_color(c_orange_top, c_orange_bot, t_y)
                buffer[idx : idx + 4] = bytes(col)
            elif point_in_polygon(x, y, poly_blue):
                t_vert = (y - pt_map((0, 0.34))[1]) / (scale * 0.30)
                t_horiz = (x - pt_map((0.318, 0))[0]) / (scale * 0.372)
                wave_t = t_vert + 0.25 * math.sin(max(0.0, min(1.0, t_horiz)) * math.pi)
                if wave_t < 0.38:
                    col = lerp_color(c_cyan_light, c_cyan_mid, wave_t / 0.38)
                elif wave_t < 0.70:
                    col = lerp_color(c_cyan_mid, c_blue_royal, (wave_t - 0.38) / 0.32)
                else:
                    col = lerp_color(c_blue_royal, c_blue_ocean, (wave_t - 0.70) / 0.30)
                buffer[idx : idx + 4] = bytes(col)

    # Downsample
    out_buffer = bytearray(size * size * 4)
    samples = ss * ss
    for out_y in range(size):
        for out_x in range(size):
            r_acc, g_acc, b_acc, a_acc = 0, 0, 0, 0
            for sy in range(ss):
                for sx in range(ss):
                    in_idx = ((out_y * ss + sy) * w + (out_x * ss + sx)) * 4
                    r_acc += buffer[in_idx]
                    g_acc += buffer[in_idx + 1]
                    b_acc += buffer[in_idx + 2]
                    a_acc += buffer[in_idx + 3]
            out_idx = (out_y * size + out_x) * 4
            out_buffer[out_idx] = r_acc // samples
            out_buffer[out_idx + 1] = g_acc // samples
            out_buffer[out_idx + 2] = b_acc // samples
            out_buffer[out_idx + 3] = a_acc // samples

    return bytes(out_buffer)

# --- 3. RENDER RASTER LAUNCHER ICONS (For mipmap density buckets) ---
def render_launcher_raster(size, is_round=False):
    ss = 2
    w = size * ss
    h = size * ss
    buffer = bytearray(w * h * 4)

    cx, cy = w / 2.0, h / 2.0
    rad_sq = (w * 0.48) ** 2
    half_box = w * 0.46
    corner_r = w * 0.22

    def in_shape(px, py):
        if is_round:
            dx = px - cx
            dy = py - cy
            return dx*dx + dy*dy <= rad_sq
        else:
            dx = abs(px - cx)
            dy = abs(py - cy)
            if dx > half_box or dy > half_box: return False
            if dx <= half_box - corner_r or dy <= half_box - corner_r: return True
            qx = dx - (half_box - corner_r)
            qy = dy - (half_box - corner_r)
            return qx*qx + qy*qy <= corner_r * corner_r

    # Background gradient
    for y in range(h):
        for x in range(w):
            if in_shape(x, y):
                idx = (y * w + x) * 4
                t_bg = y / h
                bg_col = lerp_color((255, 255, 255, 255), (240, 247, 255, 255), t_bg)
                buffer[idx : idx + 4] = bytes(bg_col)

    # Bottom accent wave
    wave_pts = []
    wave_pts.append((0, h * 0.88))
    wave_pts.extend(sample_bezier((0, h * 0.88), (w * 0.35, h * 0.95), (w * 0.65, h * 0.88), (w, h * 0.72), 20))
    wave_pts.append((w, h))
    wave_pts.append((0, h))

    c_wave_left = (56, 189, 248, 220)
    c_wave_right = (2, 132, 199, 230)
    for y in range(int(h * 0.65), h):
        for x in range(w):
            if in_shape(x, y) and point_in_polygon(x, y, wave_pts):
                idx = (y * w + x) * 4
                t_w = x / w
                col = lerp_color(c_wave_left, c_wave_right, t_w)
                buffer[idx : idx + 4] = bytes(col)

    # Emblem centered comfortably with 20% safe margin
    scale = w * 0.90
    e_ox = w * 0.50 - scale * 0.518
    e_oy = h * 0.48 - scale * 0.380

    def pt_map(pt):
        return (e_ox + pt[0] * scale, e_oy + pt[1] * scale)

    def bezier_map(p0, p1, p2, p3, steps=12):
        return [pt_map(p) for p in sample_bezier(p0, p1, p2, p3, steps)]

    poly_tl = [pt_map((0.318, 0.205))]
    poly_tl.extend(bezier_map((0.318, 0.205), (0.380, 0.170), (0.440, 0.155), (0.518, 0.144)))
    poly_tl.append(pt_map((0.518, 0.328)))
    poly_tl.extend(bezier_map((0.518, 0.328), (0.440, 0.310), (0.370, 0.310), (0.318, 0.320)))

    poly_tr = [pt_map((0.542, 0.328)), pt_map((0.542, 0.176))]
    poly_tr.extend(bezier_map((0.542, 0.176), (0.600, 0.150), (0.660, 0.140), (0.690, 0.160)))
    poly_tr.extend(bezier_map((0.690, 0.160), (0.700, 0.180), (0.695, 0.260), (0.686, 0.334)))

    poly_blue = [pt_map((0.318, 0.342)), pt_map((0.518, 0.342))]
    poly_blue.extend(bezier_map((0.518, 0.342), (0.570, 0.395), (0.640, 0.420), (0.690, 0.404), 14))
    poly_blue.append(pt_map((0.690, 0.585)))
    poly_blue.extend(bezier_map((0.690, 0.585), (0.650, 0.625), (0.580, 0.636), (0.530, 0.636), 12))
    poly_blue.append(pt_map((0.530, 0.485)))
    poly_blue.append(pt_map((0.492, 0.485)))
    poly_blue.append(pt_map((0.492, 0.628)))
    poly_blue.append(pt_map((0.318, 0.628)))

    c_orange_top = (255, 193, 7, 255)
    c_orange_bot = (235, 60, 0, 255)
    c_cyan_light = (0, 220, 255, 255)
    c_cyan_mid = (0, 150, 255, 255)
    c_blue_royal = (0, 90, 235, 255)
    c_blue_ocean = (0, 45, 175, 255)

    for y in range(h):
        for x in range(w):
            if not in_shape(x, y): continue
            idx = (y * w + x) * 4
            if point_in_polygon(x, y, poly_tl) or point_in_polygon(x, y, poly_tr):
                t_y = (y - pt_map((0, 0.14))[1]) / (scale * 0.19)
                t_y = max(0.0, min(1.0, t_y))
                col = lerp_color(c_orange_top, c_orange_bot, t_y)
                buffer[idx : idx + 4] = bytes(col)
            elif point_in_polygon(x, y, poly_blue):
                t_vert = (y - pt_map((0, 0.34))[1]) / (scale * 0.30)
                t_horiz = (x - pt_map((0.318, 0))[0]) / (scale * 0.372)
                wave_t = t_vert + 0.25 * math.sin(max(0.0, min(1.0, t_horiz)) * math.pi)
                if wave_t < 0.38:
                    col = lerp_color(c_cyan_light, c_cyan_mid, wave_t / 0.38)
                elif wave_t < 0.70:
                    col = lerp_color(c_cyan_mid, c_blue_royal, (wave_t - 0.38) / 0.32)
                else:
                    col = lerp_color(c_blue_royal, c_blue_ocean, (wave_t - 0.70) / 0.30)
                buffer[idx : idx + 4] = bytes(col)

    # Downsample
    out_buffer = bytearray(size * size * 4)
    samples = ss * ss
    for out_y in range(size):
        for out_x in range(size):
            r_acc, g_acc, b_acc, a_acc = 0, 0, 0, 0
            for sy in range(ss):
                for sx in range(ss):
                    in_idx = ((out_y * ss + sy) * w + (out_x * ss + sx)) * 4
                    r_acc += buffer[in_idx]
                    g_acc += buffer[in_idx + 1]
                    b_acc += buffer[in_idx + 2]
                    a_acc += buffer[in_idx + 3]
            out_idx = (out_y * size + out_x) * 4
            out_buffer[out_idx] = r_acc // samples
            out_buffer[out_idx + 1] = g_acc // samples
            out_buffer[out_idx + 2] = b_acc // samples
            out_buffer[out_idx + 3] = a_acc // samples

    return bytes(out_buffer)

# --- 4. RENDER WIDE HORIZONTAL BANNER (720x240) ---
def render_wide_banner(bw=720, bh=240):
    ss = 2
    w = bw * ss
    h = bh * ss

    buffer = bytearray(w * h * 4)
    cx, cy = w / 2.0, h / 2.0
    half_w = w * 0.48
    half_h = h * 0.46
    corner_r = h * 0.18

    def in_box(px, py):
        dx = abs(px - cx)
        dy = abs(py - cy)
        if dx > half_w or dy > half_h: return False
        if dx <= half_w - corner_r or dy <= half_h - corner_r: return True
        qx = dx - (half_w - corner_r)
        qy = dy - (half_h - corner_r)
        return qx*qx + qy*qy <= corner_r * corner_r

    for y in range(h):
        for x in range(w):
            if in_box(x, y):
                idx = (y * w + x) * 4
                t_bg = y / h
                bg_col = lerp_color((255, 255, 255, 255), (242, 248, 254, 255), t_bg)
                buffer[idx : idx + 4] = bytes(bg_col)

    wave_pts = []
    wave_pts.append((w * 0.02, h * 0.88))
    wave_pts.extend(sample_bezier((w * 0.02, h * 0.88), (w * 0.35, h * 0.95), (w * 0.65, h * 0.86), (w * 0.98, h * 0.62), 24))
    wave_pts.append((w * 0.98, h * 0.96))
    wave_pts.append((w * 0.02, h * 0.96))

    c_wave_left = (56, 189, 248, 230)
    c_wave_right = (2, 132, 199, 240)
    for y in range(int(h * 0.55), h):
        for x in range(w):
            if in_box(x, y) and point_in_polygon(x, y, wave_pts):
                idx = (y * w + x) * 4
                t_w = x / w
                col = lerp_color(c_wave_left, c_wave_right, t_w)
                buffer[idx : idx + 4] = bytes(col)

    e_scale = h * 1.35
    e_ox = w * 0.18 - e_scale * 0.50
    e_oy = h * 0.48 - e_scale * 0.38

    def to_banner(pt):
        return (e_ox + pt[0] * e_scale, e_oy + pt[1] * e_scale)

    def sample_banner_bezier(p0, p1, p2, p3, steps=12):
        return [to_banner(p) for p in sample_bezier(p0, p1, p2, p3, steps)]

    b_tl = [to_banner((0.318, 0.205))]
    b_tl.extend(sample_banner_bezier((0.318, 0.205), (0.380, 0.170), (0.440, 0.155), (0.518, 0.144)))
    b_tl.append(to_banner((0.518, 0.328)))
    b_tl.extend(sample_banner_bezier((0.518, 0.328), (0.440, 0.310), (0.370, 0.310), (0.318, 0.320)))

    b_tr = [to_banner((0.542, 0.328)), to_banner((0.542, 0.176))]
    b_tr.extend(sample_banner_bezier((0.542, 0.176), (0.600, 0.150), (0.660, 0.140), (0.690, 0.160)))
    b_tr.extend(sample_banner_bezier((0.690, 0.160), (0.700, 0.180), (0.695, 0.260), (0.686, 0.334)))

    b_blue = [to_banner((0.318, 0.342)), to_banner((0.518, 0.342))]
    b_blue.extend(sample_banner_bezier((0.518, 0.342), (0.570, 0.395), (0.640, 0.420), (0.690, 0.404), 14))
    b_blue.append(to_banner((0.690, 0.585)))
    b_blue.extend(sample_banner_bezier((0.690, 0.585), (0.650, 0.625), (0.580, 0.636), (0.530, 0.636), 12))
    b_blue.append(to_banner((0.530, 0.485)))
    b_blue.append(to_banner((0.492, 0.485)))
    b_blue.append(to_banner((0.492, 0.628)))
    b_blue.append(to_banner((0.318, 0.628)))

    c_orange_top = (255, 193, 7, 255)
    c_orange_bot = (235, 60, 0, 255)
    c_cyan_light = (0, 220, 255, 255)
    c_cyan_mid = (0, 150, 255, 255)
    c_blue_royal = (0, 90, 235, 255)
    c_blue_ocean = (0, 45, 175, 255)

    for y in range(h):
        for x in range(int(w * 0.35)):
            if not in_box(x, y): continue
            idx = (y * w + x) * 4
            if point_in_polygon(x, y, b_tl) or point_in_polygon(x, y, b_tr):
                t_y = (y - (e_oy + 0.14 * e_scale)) / (0.19 * e_scale)
                t_y = max(0.0, min(1.0, t_y))
                col = lerp_color(c_orange_top, c_orange_bot, t_y)
                buffer[idx : idx + 4] = bytes(col)
            elif point_in_polygon(x, y, b_blue):
                t_vert = (y - (e_oy + 0.34 * e_scale)) / (0.30 * e_scale)
                t_horiz = (x - (e_ox + 0.318 * e_scale)) / (0.372 * e_scale)
                wave_t = t_vert + 0.25 * math.sin(t_horiz * math.pi)
                if wave_t < 0.38:
                    col = lerp_color(c_cyan_light, c_cyan_mid, wave_t / 0.38)
                elif wave_t < 0.70:
                    col = lerp_color(c_cyan_mid, c_blue_royal, (wave_t - 0.38) / 0.32)
                else:
                    col = lerp_color(c_blue_royal, c_blue_ocean, (wave_t - 0.70) / 0.30)
                buffer[idx : idx + 4] = bytes(col)

    navy = (16, 53, 90, 255)
    text_y = h * 0.38
    text_h = h * 0.28
    text_w = w * 0.60
    start_x = w * 0.35

    def slant_b(pt):
        base_y = text_y + text_h
        shear = 0.15
        return (pt[0] + (base_y - pt[1]) * shear, pt[1])

    letter_polys = []
    bx = start_x
    letter_polys.append([slant_b((bx, text_y)), slant_b((bx + text_w*0.024, text_y)), slant_b((bx + text_w*0.024, text_y + text_h)), slant_b((bx, text_y + text_h))])
    letter_polys.append([slant_b((bx + text_w*0.020, text_y + text_h*0.48)), slant_b((bx + text_w*0.082, text_y)), slant_b((bx + text_w*0.110, text_y)), slant_b((bx + text_w*0.040, text_y + text_h*0.56))])
    letter_polys.append([slant_b((bx + text_w*0.034, text_y + text_h*0.50)), slant_b((bx + text_w*0.076, text_y + text_h*0.50)), slant_b((bx + text_w*0.112, text_y + text_h)), slant_b((bx + text_w*0.080, text_y + text_h))])
    ix = start_x + text_w * 0.136
    letter_polys.append([slant_b((ix, text_y)), slant_b((ix + text_w*0.026, text_y)), slant_b((ix + text_w*0.026, text_y + text_h)), slant_b((ix, text_y + text_h))])
    nx = start_x + text_w * 0.188
    letter_polys.append([slant_b((nx, text_y)), slant_b((nx + text_w*0.024, text_y)), slant_b((nx + text_w*0.024, text_y + text_h)), slant_b((nx, text_y + text_h))])
    letter_polys.append([slant_b((nx + text_w*0.015, text_y)), slant_b((nx + text_w*0.042, text_y)), slant_b((nx + text_w*0.096, text_y + text_h)), slant_b((nx + text_w*0.068, text_y + text_h))])
    letter_polys.append([slant_b((nx + text_w*0.074, text_y)), slant_b((nx + text_w*0.098, text_y)), slant_b((nx + text_w*0.098, text_y + text_h)), slant_b((nx + text_w*0.074, text_y + text_h))])
    ax = start_x + text_w * 0.320
    letter_polys.append([slant_b((ax + text_w*0.040, text_y)), slant_b((ax + text_w*0.066, text_y)), slant_b((ax, text_y + text_h)), slant_b((ax - text_w*0.024, text_y + text_h))])
    letter_polys.append([slant_b((ax + text_w*0.040, text_y)), slant_b((ax + text_w*0.066, text_y)), slant_b((ax + text_w*0.110, text_y + text_h)), slant_b((ax + text_w*0.084, text_y + text_h))])
    letter_polys.append([slant_b((ax + text_w*0.010, text_y + text_h*0.60)), slant_b((ax + text_w*0.082, text_y + text_h*0.60)), slant_b((ax + text_w*0.082, text_y + text_h*0.74)), slant_b((ax + text_w*0.010, text_y + text_h*0.74))])
    def make_s_b(sx):
        return [
            [slant_b((sx, text_y)), slant_b((sx + text_w*0.088, text_y)), slant_b((sx + text_w*0.088, text_y + text_h*0.22)), slant_b((sx, text_y + text_h*0.22))],
            [slant_b((sx, text_y)), slant_b((sx + text_w*0.026, text_y)), slant_b((sx + text_w*0.026, text_y + text_h*0.50)), slant_b((sx, text_y + text_h*0.50))],
            [slant_b((sx, text_y + text_h*0.40)), slant_b((sx + text_w*0.088, text_y + text_h*0.40)), slant_b((sx + text_w*0.088, text_y + text_h*0.60)), slant_b((sx, text_y + text_h*0.60))],
            [slant_b((sx + text_w*0.062, text_y + text_h*0.50)), slant_b((sx + text_w*0.088, text_y + text_h*0.50)), slant_b((sx + text_w*0.088, text_y + text_h)), slant_b((sx + text_w*0.062, text_y + text_h))],
            [slant_b((sx, text_y + text_h*0.78)), slant_b((sx + text_w*0.088, text_y + text_h*0.78)), slant_b((sx + text_w*0.088, text_y + text_h)), slant_b((sx, text_y + text_h))]
        ]
    letter_polys.extend(make_s_b(start_x + text_w * 0.442))
    letter_polys.extend(make_s_b(start_x + text_w * 0.548))
    i2x = start_x + text_w * 0.655
    letter_polys.append([slant_b((i2x, text_y)), slant_b((i2x + text_w*0.026, text_y)), slant_b((i2x + text_w*0.026, text_h + text_y)), slant_b((i2x, text_y + text_h))])
    letter_polys.extend(make_s_b(start_x + text_w * 0.706))
    tx = start_x + text_w * 0.812
    letter_polys.append([slant_b((tx, text_y)), slant_b((tx + text_w*0.110, text_y)), slant_b((tx + text_w*0.110, text_y + text_h*0.22)), slant_b((tx, text_y + text_h*0.22))])
    letter_polys.append([slant_b((tx + text_w*0.042, text_y)), slant_b((tx + text_w*0.068, text_y)), slant_b((tx + text_w*0.068, text_y + text_h)), slant_b((tx + text_w*0.042, text_y + text_h))])

    for p in letter_polys:
        min_x = max(0, int(min(pt[0] for pt in p)))
        max_x = min(w - 1, int(max(pt[0] for pt in p)) + 1)
        min_y = max(0, int(min(pt[1] for pt in p)))
        max_y = min(h - 1, int(max(pt[1] for pt in p)) + 1)
        for py in range(min_y, max_y + 1):
            for px in range(min_x, max_x + 1):
                if point_in_polygon(px, py, p):
                    idx = (py * w + px) * 4
                    buffer[idx : idx + 4] = bytes(navy)

    out_buffer = bytearray(bw * bh * 4)
    samples = ss * ss
    for out_y in range(bh):
        for out_x in range(bw):
            r_acc, g_acc, b_acc, a_acc = 0, 0, 0, 0
            for sy in range(ss):
                for sx in range(ss):
                    in_idx = ((out_y * ss + sy) * w + (out_x * ss + sx)) * 4
                    r_acc += buffer[in_idx]
                    g_acc += buffer[in_idx + 1]
                    b_acc += buffer[in_idx + 2]
                    a_acc += buffer[in_idx + 3]
            out_idx = (out_y * bw + out_x) * 4
            out_buffer[out_idx] = r_acc // samples
            out_buffer[out_idx + 1] = g_acc // samples
            out_buffer[out_idx + 2] = b_acc // samples
            out_buffer[out_idx + 3] = a_acc // samples

    return bytes(out_buffer)

# --- EXECUTION: GENERATE ALL ASSETS WITH ZERO CROPPING ---
res_base = "app/src/main/res"

# 1. Drawable Brand Assets
# Brand icon: 512x512 with safe 19% padding
brand_icon = render_brand_icon(512, transparent_bg=False)
write_png(f"{res_base}/drawable/kinassist_brand_icon.png", 512, 512, brand_icon)
print("Generated kinassist_brand_icon.png (512x512)")

# Transparent emblem mark: 512x512 with safe padding
emblem_mark = render_emblem_mark(512)
write_png(f"{res_base}/drawable/kinassist_logo_mark.png", 512, 512, emblem_mark)
print("Generated kinassist_logo_mark.png (512x512)")

# Wide banner: 720x240
wide_banner = render_wide_banner(720, 240)
write_png(f"{res_base}/drawable/kinassist_brand_banner.png", 720, 240, wide_banner)
print("Generated kinassist_brand_banner.png (720x240)")

# 2. Raster Mipmap Icons across all 5 densities
specs = [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]
for folder, size in specs:
    dir_path = f"{res_base}/mipmap-{folder}"
    # Square / squircle icon
    sq_png = render_launcher_raster(size, is_round=False)
    write_png(f"{dir_path}/ic_launcher.png", size, size, sq_png)
    # Round icon (safely inscribed circle)
    rnd_png = render_launcher_raster(size, is_round=True)
    write_png(f"{dir_path}/ic_launcher_round.png", size, size, rnd_png)
    print(f"Generated {folder} ic_launcher.png & ic_launcher_round.png ({size}x{size})")

print("All raster assets generated successfully!")
