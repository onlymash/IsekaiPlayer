#!/usr/bin/env python3
"""
IsekaiPlayer - GitHub Social Preview / Banner Generator (1280x640)
Generates an SVG featuring an Isekai summoning magic array and modern typography for GitHub,
then exports it to a high-resolution 1280x640 PNG using available system tools.
"""

import math
import os
import subprocess
import sys


def generate_svg() -> str:
    width = 1280
    height = 640

    # Main Magic Array Center & Scale
    cx, cy = 900, 320
    radius = 260

    # Secondary Orbit Magic Array Center
    cx2, cy2 = 220, 110
    radius2 = 100

    svg_parts = []
    svg_parts.append(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">'
    )

    # Definitions for Gradients and Filters
    svg_parts.append("""
    <defs>
        <!-- Background Linear Gradient -->
        <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#080512"/>
            <stop offset="50%" stop-color="#140B2D"/>
            <stop offset="100%" stop-color="#090E20"/>
        </linearGradient>

        <!-- Title Text Gradient -->
        <linearGradient id="titleGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#FFFFFF"/>
            <stop offset="60%" stop-color="#E2C4FF"/>
            <stop offset="100%" stop-color="#80E5FF"/>
        </linearGradient>

        <!-- Magic Glow Radial Gradients -->
        <radialGradient id="magicGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#7C4DFF" stop-opacity="0.38"/>
            <stop offset="50%" stop-color="#00E5FF" stop-opacity="0.18"/>
            <stop offset="100%" stop-color="#080512" stop-opacity="0"/>
        </radialGradient>

        <radialGradient id="centerGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.65"/>
            <stop offset="30%" stop-color="#B388FF" stop-opacity="0.35"/>
            <stop offset="100%" stop-color="#00E5FF" stop-opacity="0"/>
        </radialGradient>

        <!-- Soft Drop Shadow Filter -->
        <filter id="glowFilter" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="10" result="blur"/>
            <feComposite in="SourceGraphic" in2="blur" operator="over"/>
        </filter>

        <filter id="textGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="0" dy="5" stdDeviation="8" flood-color="#7C4DFF" flood-opacity="0.65"/>
        </filter>
    </defs>
    """)

    # 1. Base Background Rectangle
    svg_parts.append(
        f'<rect width="{width}" height="{height}" fill="url(#bgGrad)"/>'
    )

    # 2. Ambient Radial Glow Effects
    svg_parts.append(
        f'<circle cx="{cx}" cy="{cy}" r="{int(radius * 1.65)}" fill="url(#magicGlow)"/>'
    )
    svg_parts.append(
        f'<circle cx="{cx}" cy="{cy}" r="{int(radius * 0.85)}" fill="url(#centerGlow)"/>'
    )

    # 3. Main Summoning Magic Array Group
    svg_parts.append(f'<g transform="translate({cx}, {cy})">')

    # Outer Track Rings
    svg_parts.append(
        f'<circle r="{radius}" fill="none" stroke="#7C4DFF" stroke-width="4" opacity="0.8"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius * 1.06)}" fill="none" stroke="#00E5FF" stroke-width="1.8" opacity="0.45"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius * 0.68)}" fill="none" stroke="#7C4DFF" stroke-width="2.5" opacity="0.65"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius * 0.40)}" fill="none" stroke="#00E5FF" stroke-width="1.8" opacity="0.55"/>'
    )

    # 8-Direction Rune Notch Arcs
    arc_radius = int(radius * 0.85)
    arc_angles = [
        (70, 25),
        (110, 25),
        (160, 25),
        (200, 25),
        (250, 25),
        (290, 25),
        (340, 25),
        (20, 25),
    ]
    for start_a, sweep in arc_angles:
        rad_start = math.radians(start_a)
        rad_end = math.radians(start_a + sweep)
        x1, y1 = arc_radius * math.cos(rad_start), arc_radius * math.sin(
            rad_start
        )
        x2, y2 = arc_radius * math.cos(rad_end), arc_radius * math.sin(rad_end)
        svg_parts.append(
            f'<path d="M {x1:.2f} {y1:.2f} A {arc_radius} {arc_radius} 0 0 1 {x2:.2f} {y2:.2f}" '
            f'fill="none" stroke="#00E5FF" stroke-width="4.5" stroke-linecap="round" opacity="0.88"/>'
        )

    # Radial Tick Marks (36 ticks)
    inner_tick_r = radius * 0.96
    outer_tick_r = radius * 1.04
    for i in range(36):
        a_rad = math.radians(i * 10)
        cos_a, sin_a = math.cos(a_rad), math.sin(a_rad)
        tx1, ty1 = inner_tick_r * cos_a, inner_tick_r * sin_a
        tx2, ty2 = outer_tick_r * cos_a, outer_tick_r * sin_a
        svg_parts.append(
            f'<line x1="{tx1:.2f}" y1="{ty1:.2f}" x2="{tx2:.2f}" y2="{ty2:.2f}" '
            f'stroke="#00E5FF" stroke-width="1.4" opacity="0.55"/>'
        )

    # Cardinal Starburst Diamond Points
    star_dist = radius
    star_len = 38
    star_w = 12
    for deg in [0, 90, 180, 270]:
        rad = math.radians(deg)
        cos_v, sin_v = math.cos(rad), math.sin(rad)
        p_cos, p_sin = -sin_v, cos_v

        bx, by = star_dist * cos_v, star_dist * sin_v
        tip_out_x, tip_out_y = (
            bx + star_len * cos_v,
            by + star_len * sin_v,
        )
        tip_in_x, tip_in_y = (
            bx - star_len * 0.4 * cos_v,
            by - star_len * 0.4 * sin_v,
        )
        s1_x, s1_y = bx + star_w * p_cos, by + star_w * p_sin
        s2_x, s2_y = bx - star_w * p_cos, by - star_w * p_sin

        svg_parts.append(
            f'<polygon points="{tip_out_x:.2f},{tip_out_y:.2f} {s1_x:.2f},{s1_y:.2f} {tip_in_x:.2f},{tip_in_y:.2f} {s2_x:.2f},{s2_y:.2f}" '
            f'fill="#00E5FF" opacity="0.9"/>'
        )

    # Diagonal Smaller Star Points
    for deg in [45, 135, 225, 315]:
        rad = math.radians(deg)
        cos_v, sin_v = math.cos(rad), math.sin(rad)
        p_cos, p_sin = -sin_v, cos_v

        bx, by = star_dist * cos_v, star_dist * sin_v
        tip_out_x, tip_out_y = (
            bx + 22 * cos_v,
            by + 22 * sin_v,
        )
        tip_in_x, tip_in_y = (
            bx - 7 * cos_v,
            by - 7 * sin_v,
        )
        s1_x, s1_y = bx + 6 * p_cos, by + 6 * p_sin
        s2_x, s2_y = bx - 6 * p_cos, by - 6 * p_sin

        svg_parts.append(
            f'<polygon points="{tip_out_x:.2f},{tip_out_y:.2f} {s1_x:.2f},{s1_y:.2f} {tip_in_x:.2f},{tip_in_y:.2f} {s2_x:.2f},{s2_y:.2f}" '
            f'fill="#B388FF" opacity="0.78"/>'
        )

    # Aux Construction Triangle (pointing left)
    aux_r = radius * 0.58
    aux_pts = []
    for deg in [180, 60, 300]:
        rad = math.radians(deg)
        aux_pts.append(
            f'{aux_r * math.cos(rad):.2f},{aux_r * math.sin(rad):.2f}'
        )
    svg_parts.append(
        f'<polygon points="{" ".join(aux_pts)}" fill="none" stroke="#7C4DFF" stroke-width="3" opacity="0.7"/>'
    )

    # Core Deconstructed Play Triangle (pointing right)
    play_r = radius * 0.52
    play_pts = []
    for deg in [0, 120, 240]:
        rad = math.radians(deg)
        play_pts.append(
            f'{play_r * math.cos(rad):.2f},{play_r * math.sin(rad):.2f}'
        )
    svg_parts.append(
        f'<polygon points="{" ".join(play_pts)}" fill="#B388FF" fill-opacity="0.18" stroke="#00E5FF" stroke-width="3.5" opacity="0.9"/>'
    )

    # Inner Array Eye Kernel Triangle
    core_r = play_r * 0.45
    core_pts = []
    for deg in [0, 120, 240]:
        rad = math.radians(deg)
        core_pts.append(
            f'{core_r * math.cos(rad):.2f},{core_r * math.sin(rad):.2f}'
        )
    svg_parts.append(
        f'<polygon points="{" ".join(core_pts)}" fill="#FFFFFF" opacity="0.88"/>'
    )

    svg_parts.append('</g>')  # End Main Magic Array Group

    # 4. Secondary Background Orbit Magic Circle (Top-Left)
    svg_parts.append(f'<g transform="translate({cx2}, {cy2})">')
    svg_parts.append(
        f'<circle r="{radius2}" fill="none" stroke="#7C4DFF" stroke-width="1.8" opacity="0.28"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius2 * 0.75)}" fill="none" stroke="#00E5FF" stroke-width="1.2" opacity="0.22"/>'
    )
    for deg in [0, 90, 180, 270]:
        rad = math.radians(deg)
        bx, by = radius2 * math.cos(rad), radius2 * math.sin(rad)
        svg_parts.append(
            f'<circle cx="{bx:.2f}" cy="{by:.2f}" r="3" fill="#00E5FF" opacity="0.45"/>'
        )
    svg_parts.append('</g>')

    # 5. Scattered Magic Particles & Star Dust
    particles = [
        (150, 480, 3.0, '#00E5FF', 0.65),
        (420, 110, 3.5, '#B388FF', 0.55),
        (580, 530, 2.5, '#FFFFFF', 0.75),
        (650, 90, 4.0, '#00E5FF', 0.85),
        (1150, 120, 2.5, '#B388FF', 0.45),
        (1180, 520, 3.5, '#00E5FF', 0.65),
        (780, 560, 3.0, '#FFFFFF', 0.55),
        (100, 350, 2.5, '#7C4DFF', 0.45),
    ]
    for px, py, pr, pcol, popa in particles:
        svg_parts.append(
            f'<circle cx="{px}" cy="{py}" r="{pr}" fill="{pcol}" opacity="{popa}"/>'
        )

    # 6. Left Side Title, Slogan & Feature Badges
    left_x = 90

    # App Icon Graphic Emblem next to title
    icon_size = 76
    icon_cx, icon_cy = left_x + 38, 230
    svg_parts.append(
        f'<g transform="translate({icon_cx}, {icon_cy})">'
        f'<circle r="36" fill="#1E1238" stroke="#7C4DFF" stroke-width="2.5"/>'
        f'<circle r="26" fill="none" stroke="#00E5FF" stroke-width="1.8" opacity="0.85"/>'
        f'<polygon points="14,0 -9,-13 -9,13" fill="#B388FF"/>'
        f'<polygon points="6,0 -5,-6 -5,6" fill="#FFFFFF"/>'
        f'</g>'
    )

    # App Title Text
    svg_parts.append(
        f'<text x="{left_x + 96}" y="250" font-family="Roboto, System-UI, sans-serif" '
        f'font-weight="800" font-size="64" fill="url(#titleGrad)" filter="url(#textGlow)" letter-spacing="1">IsekaiPlayer</text>'
    )

    # Subtitle / Slogan
    svg_parts.append(
        f'<text x="{left_x}" y="315" font-family="Roboto, System-UI, sans-serif" '
        f'font-weight="500" font-size="22" fill="#CBB2FF" opacity="0.92" letter-spacing="0.5">'
        f'Sovereign above myriad realms • Shatter every mortal cipher</text>'
    )

    # Feature Badges Row
    badge_y = 390
    badges = [
        ("lightning", "MPV + ExoPlayer Dual Engine", "#7C4DFF", "#1F123B", "#FFD54F"),
        ("globe", "SMB / WebDAV / FTP / Local", "#00E5FF", "#0C2338", "#80D8FF"),
        ("sparkle", "Material 3 / Modern MVI", "#E040FB", "#281033", "#FF80AB"),
    ]

    badge_x = left_x
    for icon_type, label, border_col, bg_col, icon_col in badges:
        padding_h = 16
        icon_w = 20
        gap = 10
        text_len = len(label) * 9.8
        badge_w = int(padding_h + icon_w + gap + text_len + padding_h)
        badge_h = 42
        icy = badge_y + 21

        # Badge Background Pill
        svg_parts.append(
            f'<rect x="{badge_x}" y="{badge_y}" width="{badge_w}" height="{badge_h}" rx="21" '
            f'fill="{bg_col}" fill-opacity="0.8" stroke="{border_col}" stroke-width="1.8" stroke-opacity="0.85"/>'
        )

        # Draw Vector Icon
        icx = badge_x + padding_h + icon_w / 2
        if icon_type == "lightning":
            svg_parts.append(
                f'<path d="M {icx-1} {icy-8} L {icx-7} {icy+0.5} L {icx-1} {icy+0.5} L {icx-3} {icy+8} L {icx+6} {icy-1} L {icx-0.5} {icy-1} L {icx+2.5} {icy-8} Z" fill="{icon_col}"/>'
            )
        elif icon_type == "globe":
            svg_parts.append(
                f'<circle cx="{icx}" cy="{icy}" r="7.5" fill="none" stroke="{icon_col}" stroke-width="1.6"/>'
                f'<line x1="{icx-7.5}" y1="{icy}" x2="{icx+7.5}" y2="{icy}" stroke="{icon_col}" stroke-width="1.2"/>'
                f'<ellipse cx="{icx}" cy="{icy}" rx="3.6" ry="7.5" fill="none" stroke="{icon_col}" stroke-width="1.2"/>'
            )
        elif icon_type == "sparkle":
            p1 = f'M {icx-1} {icy-8} Q {icx-1} {icy} {icx+7} {icy} Q {icx-1} {icy} {icx-1} {icy+8} Q {icx-1} {icy} {icx-9} {icy} Q {icx-1} {icy} {icx-1} {icy-8} Z'
            p2 = f'M {icx+6} {icy-7} Q {icx+6} {icy-4} {icx+9} {icy-4} Q {icx+6} {icy-4} {icx+6} {icy-1} Q {icx+6} {icy-4} {icx+3} {icy-4} Q {icx+6} {icy-4} {icx+6} {icy-7} Z'
            svg_parts.append(
                f'<path d="{p1}" fill="{icon_col}"/><path d="{p2}" fill="{icon_col}" opacity="0.85"/>'
            )

        # Badge Text Label
        text_x = badge_x + padding_h + icon_w + gap
        svg_parts.append(
            f'<text x="{text_x:.1f}" y="{badge_y + 27}" font-family="Roboto, System-UI, sans-serif" '
            f'font-weight="600" font-size="15" fill="#FFFFFF" opacity="0.95">{label}</text>'
        )

        badge_x += badge_w + 14

    svg_parts.append('</svg>')
    return '\n'.join(svg_parts)


def export_to_png(svg_path: str, png_path: str) -> None:
    """Exports SVG to 1280x640 PNG using rsvg-convert, inkscape, or magick.

    Raises RuntimeError if no converter command is available in system PATH.
    """
    converters = [
        ["rsvg-convert", "-w", "1280", "-h", "640", svg_path, "-o", png_path],
        [
            "inkscape",
            svg_path,
            "--export-filename=" + png_path,
            "-w",
            "1280",
            "-h",
            "640",
        ],
        ["magick", "convert", "-background", "none", svg_path, png_path],
        ["convert", "-background", "none", svg_path, png_path],
    ]

    for cmd in converters:
        try:
            res = subprocess.run(cmd, capture_output=True, text=True)
            if res.returncode == 0 and os.path.exists(png_path):
                print(f"[SUCCESS] Converted SVG to PNG using: {cmd[0]}")
                return
        except FileNotFoundError:
            continue

    raise RuntimeError(
        "No SVG converter tool found in system PATH (tried: rsvg-convert, inkscape, magick, convert).\n"
        "Please install one of the required tool packages (e.g., 'librsvg' / 'librsvg2-bin' or 'imagemagick') and ensure it is in PATH."
    )


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    app_main_dir = os.path.join(project_root, 'app', 'src', 'main')

    svg_path = os.path.join(app_main_dir, 'github-social-preview.svg')
    png_path = os.path.join(app_main_dir, 'github-social-preview.png')

    svg_content = generate_svg()
    with open(svg_path, 'w', encoding='utf-8') as f:
        f.write(svg_content)
    print(f"[OK] Saved SVG to {svg_path}")

    try:
        export_to_png(svg_path, png_path)
        print(f"[OK] Saved 1280x640 Social Preview PNG to {png_path}")
    except RuntimeError as err:
        print(f"[ERROR] {err}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
