#!/usr/bin/env python3
"""
IsekaiPlayer - Notification Playback Cover Placeholder Generator (512x512)
Generates SVG and Android VectorDrawable featuring an Isekai summoning magic array.
"""

import math
import os
import sys

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUTPUT_SVG = os.path.join(PROJECT_ROOT, "app", "src", "main", "playback-cover-placeholder.svg")
OUTPUT_XML = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "drawable", "ic_playback_cover_placeholder.xml")


def generate_svg() -> str:
    width = 512
    height = 512

    # Main Magic Array Center & Scale (Centered at 256, 256)
    cx, cy = 256, 256
    radius = 190

    # Secondary Orbit Magic Array Center (Top-Left background)
    cx2, cy2 = 90, 90
    radius2 = 65

    svg_parts = []
    svg_parts.append(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">'
    )

    # Definitions
    svg_parts.append("""
    <defs>
        <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#080512"/>
            <stop offset="50%" stop-color="#140B2D"/>
            <stop offset="100%" stop-color="#090E20"/>
        </linearGradient>

        <radialGradient id="magicGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#7C4DFF" stop-opacity="0.35"/>
            <stop offset="55%" stop-color="#00E5FF" stop-opacity="0.15"/>
            <stop offset="100%" stop-color="#080512" stop-opacity="0"/>
        </radialGradient>

        <radialGradient id="centerGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.6"/>
            <stop offset="40%" stop-color="#B388FF" stop-opacity="0.3"/>
            <stop offset="100%" stop-color="#00E5FF" stop-opacity="0"/>
        </radialGradient>
    </defs>
    """)

    # 1. Base Background Rectangle
    svg_parts.append(
        f'<rect width="{width}" height="{height}" fill="url(#bgGrad)"/>'
    )

    # 2. Ambient Radial Glow Effects
    svg_parts.append(
        f'<circle cx="{cx}" cy="{cy}" r="{int(radius * 1.5)}" fill="url(#magicGlow)"/>'
    )
    svg_parts.append(
        f'<circle cx="{cx}" cy="{cy}" r="{int(radius * 0.75)}" fill="url(#centerGlow)"/>'
    )

    # 3. Secondary Background Orbit Magic Circle (Top-Left)
    svg_parts.append(f'<g transform="translate({cx2}, {cy2})">')
    svg_parts.append(
        f'<circle r="{radius2}" fill="none" stroke="#7C4DFF" stroke-width="1.5" opacity="0.25"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius2 * 0.75)}" fill="none" stroke="#00E5FF" stroke-width="1.0" opacity="0.2"/>'
    )
    for deg in [0, 90, 180, 270]:
        rad = math.radians(deg)
        bx, by = radius2 * math.cos(rad), radius2 * math.sin(rad)
        svg_parts.append(
            f'<circle cx="{bx:.2f}" cy="{by:.2f}" r="2.5" fill="#00E5FF" opacity="0.4"/>'
        )
    svg_parts.append('</g>')

    # 4. Mana Dust & Celestial Starfield
    stars = [
        (50, 360, 2.5, "#00E5FF", 0.6),
        (140, 70, 3.0, "#B388FF", 0.5),
        (200, 440, 2.0, "#FFFFFF", 0.7),
        (310, 35, 3.5, "#00E5FF", 0.8),  # Shifted to avoid North tip overlap at (256, 36..66)
        (440, 70, 2.0, "#B388FF", 0.4),
        (460, 420, 3.0, "#00E5FF", 0.6),
        (360, 450, 2.5, "#FFFFFF", 0.5),
        (35, 200, 2.0, "#7C4DFF", 0.4),
    ]
    for sx, sy, sr, scolor, sopac in stars:
        svg_parts.append(
            f'<circle cx="{sx}" cy="{sy}" r="{sr}" fill="{scolor}" opacity="{sopac}"/>'
        )

    # 5. Main Summoning Magic Array Group
    svg_parts.append(f'<g transform="translate({cx}, {cy})">')

    # Concentric Track Rings
    svg_parts.append(
        f'<circle r="{radius}" fill="none" stroke="#7C4DFF" stroke-width="3.5" opacity="0.8"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius * 1.06)}" fill="none" stroke="#00E5FF" stroke-width="1.5" opacity="0.4"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius * 0.68)}" fill="none" stroke="#7C4DFF" stroke-width="2.0" opacity="0.6"/>'
    )
    svg_parts.append(
        f'<circle r="{int(radius * 0.40)}" fill="none" stroke="#00E5FF" stroke-width="1.5" opacity="0.5"/>'
    )

    # 8-Direction Rune Notch Arcs (100% Equally Spaced Arcs: 25° arc, 20° gap)
    arc_radius = int(radius * 0.85)
    num_arcs = 8
    sweep_angle = 25.0
    period = 360.0 / num_arcs  # 45.0
    phase_offset = period / 2.0  # 22.5

    arc_angles = []
    for i in range(num_arcs):
        center_deg = phase_offset + i * period
        start_deg = center_deg - sweep_angle / 2.0
        arc_angles.append((start_deg, sweep_angle))
    for start_a, sweep in arc_angles:
        rad_start = math.radians(start_a)
        rad_end = math.radians(start_a + sweep)
        x1, y1 = arc_radius * math.cos(rad_start), arc_radius * math.sin(rad_start)
        x2, y2 = arc_radius * math.cos(rad_end), arc_radius * math.sin(rad_end)
        svg_parts.append(
            f'<path d="M {x1:.2f} {y1:.2f} A {arc_radius} {arc_radius} 0 0 1 {x2:.2f} {y2:.2f}" '
            f'fill="none" stroke="#00E5FF" stroke-width="4" stroke-linecap="round" opacity="0.85"/>'
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
            f'stroke="#00E5FF" stroke-width="1.2" opacity="0.5"/>'
        )

    # Cardinal Starburst Diamond Points
    star_dist = radius
    star_len = 30
    star_w = 9
    for deg in [0, 90, 180, 270]:
        rad = math.radians(deg)
        cos_v, sin_v = math.cos(rad), math.sin(rad)
        p_cos, p_sin = -sin_v, cos_v

        bx, by = star_dist * cos_v, star_dist * sin_v
        tip_out_x, tip_out_y = bx + star_len * cos_v, by + star_len * sin_v
        tip_in_x, tip_in_y = bx - star_len * 0.4 * cos_v, by - star_len * 0.4 * sin_v
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
        tip_out_x, tip_out_y = bx + 16 * cos_v, by + 16 * sin_v
        tip_in_x, tip_in_y = bx - 5 * cos_v, by - 5 * sin_v
        s1_x, s1_y = bx + 5 * p_cos, by + 5 * p_sin
        s2_x, s2_y = bx - 5 * p_cos, by - 5 * p_sin

        svg_parts.append(
            f'<polygon points="{tip_out_x:.2f},{tip_out_y:.2f} {s1_x:.2f},{s1_y:.2f} {tip_in_x:.2f},{tip_in_y:.2f} {s2_x:.2f},{s2_y:.2f}" '
            f'fill="#B388FF" opacity="0.75"/>'
        )

    # Aux Construction Triangle (pointing left)
    aux_r = radius * 0.58
    aux_pts = [f'{aux_r * math.cos(math.radians(deg)):.2f},{aux_r * math.sin(math.radians(deg)):.2f}' for deg in [180, 60, 300]]
    svg_parts.append(
        f'<polygon points="{" ".join(aux_pts)}" fill="none" stroke="#7C4DFF" stroke-width="2.5" opacity="0.7"/>'
    )

    # Core Deconstructed Play Triangle (pointing right)
    play_r = radius * 0.52
    play_pts = [f'{play_r * math.cos(math.radians(deg)):.2f},{play_r * math.sin(math.radians(deg)):.2f}' for deg in [0, 120, 240]]
    svg_parts.append(
        f'<polygon points="{" ".join(play_pts)}" fill="#B388FF" fill-opacity="0.18" stroke="#00E5FF" stroke-width="3" opacity="0.9"/>'
    )

    # Inner Array Eye Kernel Triangle
    core_r = play_r * 0.45
    core_pts = [f'{core_r * math.cos(math.radians(deg)):.2f},{core_r * math.sin(math.radians(deg)):.2f}' for deg in [0, 120, 240]]
    svg_parts.append(
        f'<polygon points="{" ".join(core_pts)}" fill="#FFFFFF" opacity="0.85"/>'
    )

    svg_parts.append('</g>')  # End Main Magic Array Group

    svg_parts.append('</svg>')
    return '\n'.join(svg_parts)


def generate_android_vector_xml() -> str:
    # Android VectorDrawable XML version generated with exact mathematically centered coordinates (centered at 256, 256)
    cx, cy = 256, 256
    radius = 190

    xml_parts = []
    xml_parts.append('<?xml version="1.0" encoding="utf-8"?>')
    xml_parts.append('<!--')
    xml_parts.append('  ~ IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.')
    xml_parts.append('  ~ Copyright (C) 2026 onlymash')
    xml_parts.append('  -->')
    xml_parts.append('<vector xmlns:android="http://schemas.android.com/apk/res/android"')
    xml_parts.append('    xmlns:aapt="http://schemas.android.com/aapt"')
    xml_parts.append('    android:width="200dp"')
    xml_parts.append('    android:height="200dp"')
    xml_parts.append('    android:viewportWidth="512"')
    xml_parts.append('    android:viewportHeight="512">')

    # 1. Base Background Gradient
    xml_parts.append('    <path android:pathData="M 0,0 L 512,0 L 512,512 L 0,512 Z">')
    xml_parts.append('        <aapt:attr name="android:fillColor">')
    xml_parts.append('            <gradient')
    xml_parts.append('                android:startX="0" android:startY="0"')
    xml_parts.append('                android:endX="512" android:endY="512"')
    xml_parts.append('                android:type="linear">')
    xml_parts.append('                <item android:color="#080512" android:offset="0.0" />')
    xml_parts.append('                <item android:color="#140B2D" android:offset="0.5" />')
    xml_parts.append('                <item android:color="#090E20" android:offset="1.0" />')
    xml_parts.append('            </gradient>')
    xml_parts.append('        </aapt:attr>')
    xml_parts.append('    </path>')

    # 2. Main Magic Array Group Centered at 256, 256
    xml_parts.append(f'    <group android:translateX="{cx}" android:translateY="{cy}">')

    # Radial Glow
    xml_parts.append('        <path android:pathData="M 0,-280 A 280,280 0 1,1 -0.1,-280 Z">')
    xml_parts.append('            <aapt:attr name="android:fillColor">')
    xml_parts.append('                <gradient')
    xml_parts.append('                    android:centerX="0" android:centerY="0"')
    xml_parts.append('                    android:gradientRadius="280" android:type="radial">')
    xml_parts.append('                    <item android:color="#597C4DFF" android:offset="0.0" />')
    xml_parts.append('                    <item android:color="#2600E5FF" android:offset="0.55" />')
    xml_parts.append('                    <item android:color="#00080512" android:offset="1.0" />')
    xml_parts.append('                </gradient>')
    xml_parts.append('            </aapt:attr>')
    xml_parts.append('        </path>')

    # Track Circles
    xml_parts.append(f'        <path android:fillColor="@android:color/transparent" android:pathData="M 0,-{radius} A {radius},{radius} 0 1,1 -0.1,-{radius} Z" android:strokeWidth="3.5" android:strokeColor="#CC7C4DFF" />')
    xml_parts.append(f'        <path android:fillColor="@android:color/transparent" android:pathData="M 0,-{int(radius*1.06)} A {int(radius*1.06)},{int(radius*1.06)} 0 1,1 -0.1,-{int(radius*1.06)} Z" android:strokeWidth="1.5" android:strokeColor="#6600E5FF" />')
    xml_parts.append(f'        <path android:fillColor="@android:color/transparent" android:pathData="M 0,-{int(radius*0.68)} A {int(radius*0.68)},{int(radius*0.68)} 0 1,1 -0.1,-{int(radius*0.68)} Z" android:strokeWidth="2.0" android:strokeColor="#997C4DFF" />')
    xml_parts.append(f'        <path android:fillColor="@android:color/transparent" android:pathData="M 0,-{int(radius*0.40)} A {int(radius*0.40)},{int(radius*0.40)} 0 1,1 -0.1,-{int(radius*0.40)} Z" android:strokeWidth="1.5" android:strokeColor="#8000E5FF" />')

    # 8-Direction Rune Arcs (100% Equally Spaced Arcs: 25° arc, 20° gap)
    arc_radius = int(radius * 0.85)
    num_arcs = 8
    sweep_angle = 25.0
    period = 360.0 / num_arcs  # 45.0
    phase_offset = period / 2.0  # 22.5

    arc_angles = []
    for i in range(num_arcs):
        center_deg = phase_offset + i * period
        start_deg = center_deg - sweep_angle / 2.0
        arc_angles.append((start_deg, sweep_angle))
    for start_a, sweep in arc_angles:
        rad_start = math.radians(start_a)
        rad_end = math.radians(start_a + sweep)
        x1, y1 = arc_radius * math.cos(rad_start), arc_radius * math.sin(rad_start)
        x2, y2 = arc_radius * math.cos(rad_end), arc_radius * math.sin(rad_end)
        xml_parts.append(
            f'        <path android:fillColor="@android:color/transparent" '
            f'android:pathData="M {x1:.2f},{y1:.2f} A {arc_radius},{arc_radius} 0 0,1 {x2:.2f},{y2:.2f}" '
            f'android:strokeWidth="3.5" android:strokeColor="#D900E5FF" android:strokeLineCap="round" />'
        )

    # Radial Ticks
    inner_tick_r = radius * 0.96
    outer_tick_r = radius * 1.04
    tick_paths = []
    for i in range(36):
        a_rad = math.radians(i * 10)
        cos_a, sin_a = math.cos(a_rad), math.sin(a_rad)
        tx1, ty1 = inner_tick_r * cos_a, inner_tick_r * sin_a
        tx2, ty2 = outer_tick_r * cos_a, outer_tick_r * sin_a
        tick_paths.append(f'M {tx1:.2f},{ty1:.2f} L {tx2:.2f},{ty2:.2f}')
    xml_parts.append(
        f'        <path android:fillColor="@android:color/transparent" android:pathData="{" ".join(tick_paths)}" '
        f'android:strokeWidth="1.2" android:strokeColor="#8000E5FF" />'
    )

    # Cardinal Starburst Diamonds
    star_dist = radius
    star_len = 30
    star_w = 9
    for deg in [0, 90, 180, 270]:
        rad = math.radians(deg)
        cos_v, sin_v = math.cos(rad), math.sin(rad)
        p_cos, p_sin = -sin_v, cos_v
        bx, by = star_dist * cos_v, star_dist * sin_v
        tip_out_x, tip_out_y = bx + star_len * cos_v, by + star_len * sin_v
        tip_in_x, tip_in_y = bx - star_len * 0.4 * cos_v, by - star_len * 0.4 * sin_v
        s1_x, s1_y = bx + star_w * p_cos, by + star_w * p_sin
        s2_x, s2_y = bx - star_w * p_cos, by - star_w * p_sin
        xml_parts.append(
            f'        <path android:fillColor="#E600E5FF" '
            f'android:pathData="M {tip_out_x:.2f},{tip_out_y:.2f} L {s1_x:.2f},{s1_y:.2f} L {tip_in_x:.2f},{tip_in_y:.2f} L {s2_x:.2f},{s2_y:.2f} Z" />'
        )

    # Diagonal Star Points
    for deg in [45, 135, 225, 315]:
        rad = math.radians(deg)
        cos_v, sin_v = math.cos(rad), math.sin(rad)
        p_cos, p_sin = -sin_v, cos_v
        bx, by = star_dist * cos_v, star_dist * sin_v
        tip_out_x, tip_out_y = bx + 16 * cos_v, by + 16 * sin_v
        tip_in_x, tip_in_y = bx - 5 * cos_v, by - 5 * sin_v
        s1_x, s1_y = bx + 5 * p_cos, by + 5 * p_sin
        s2_x, s2_y = bx - 5 * p_cos, by - 5 * p_sin
        xml_parts.append(
            f'        <path android:fillColor="#CCB388FF" '
            f'android:pathData="M {tip_out_x:.2f},{tip_out_y:.2f} L {s1_x:.2f},{s1_y:.2f} L {tip_in_x:.2f},{tip_in_y:.2f} L {s2_x:.2f},{s2_y:.2f} Z" />'
        )

    # Aux Construction Triangle
    aux_r = radius * 0.58
    aux_pts = [f'{aux_r * math.cos(math.radians(deg)):.2f},{aux_r * math.sin(math.radians(deg)):.2f}' for deg in [180, 60, 300]]
    xml_parts.append(
        f'        <path android:fillColor="@android:color/transparent" '
        f'android:pathData="M {aux_pts[0]} L {aux_pts[1]} L {aux_pts[2]} Z" '
        f'android:strokeWidth="1.5" android:strokeColor="#6650B388" />'
    )

    # Core Play Triangle
    play_r = radius * 0.52
    play_pts = [f'{play_r * math.cos(math.radians(deg)):.2f},{play_r * math.sin(math.radians(deg)):.2f}' for deg in [0, 120, 240]]
    xml_parts.append(
        f'        <path android:fillColor="#33B388FF" '
        f'android:pathData="M {play_pts[0]} L {play_pts[1]} L {play_pts[2]} Z" />'
    )
    xml_parts.append(
        f'        <path android:fillColor="@android:color/transparent" '
        f'android:pathData="M {play_pts[0]} L {play_pts[1]} L {play_pts[2]} Z" '
        f'android:strokeWidth="2.5" android:strokeColor="#E600E5FF" />'
    )

    # Inner Eye Kernel Triangle
    core_r = play_r * 0.45
    core_pts = [f'{core_r * math.cos(math.radians(deg)):.2f},{core_r * math.sin(math.radians(deg)):.2f}' for deg in [0, 120, 240]]
    xml_parts.append(
        f'        <path android:fillColor="#E6FFFFFF" '
        f'android:pathData="M {core_pts[0]} L {core_pts[1]} L {core_pts[2]} Z" />'
    )

    xml_parts.append('    </group>')

    # 3. Secondary Orbit Magic Circle (Top-Left 90, 90)
    xml_parts.append('    <group android:translateX="90" android:translateY="90">')
    xml_parts.append('        <path android:fillColor="@android:color/transparent" android:pathData="M 0,-65 A 65,65 0 1,1 -0.1,-65 Z" android:strokeWidth="1.5" android:strokeColor="#337C4DFF" />')
    xml_parts.append('        <path android:fillColor="@android:color/transparent" android:pathData="M 0,-48 A 48,48 0 1,1 -0.1,-48 Z" android:strokeWidth="1.0" android:strokeColor="#2600E5FF" />')
    xml_parts.append('    </group>')

    # 4. Mana Dust Particles
    stars = [
        (50, 360, 2.5, "#9900E5FF"),
        (140, 70, 3.0, "#80B388FF"),
        (200, 440, 2.0, "#B2FFFFFF"),
        (310, 35, 3.5, "#CC00E5FF"),  # Shifted to avoid North tip overlap at (256, 36..66)
        (440, 70, 2.0, "#66B388FF"),
        (460, 420, 3.0, "#9900E5FF"),
        (360, 450, 2.5, "#80FFFFFF"),
        (35, 200, 2.0, "#667C4DFF"),
    ]
    for sx, sy, sr, scolor in stars:
        xml_parts.append(
            f'    <path android:fillColor="{scolor}" '
            f'android:pathData="M {sx},{sy - sr:.1f} A {sr},{sr} 0 1,1 {sx - 0.1:.1f},{sy - sr:.1f} Z" />'
        )

    xml_parts.append('</vector>')
    return '\n'.join(xml_parts)


def main():
    svg_content = generate_svg()
    with open(OUTPUT_SVG, "w", encoding="utf-8") as f:
        f.write(svg_content)
    print(f"Generated SVG: {OUTPUT_SVG}")

    xml_content = generate_android_vector_xml()
    with open(OUTPUT_XML, "w", encoding="utf-8") as f:
        f.write(xml_content)
    print(f"Generated Vector XML: {OUTPUT_XML}")


if __name__ == "__main__":
    main()
