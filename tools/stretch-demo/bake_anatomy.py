#!/usr/bin/env python3
"""Bake the illustration anatomy map for render.swift.

Run from the repository root:
    uv run --with numpy --with scipy --with pillow python tools/stretch-demo/bake_anatomy.py

The map lives in the rest mesh's UV space. For every texel the script knows the
rest-space position, normal, and body region. Muscle borders are authored as
curves on simple charts (front, back, side, and limb cylinders), draped onto the
surface, and mirrored to the other side. Each channel stores a distance, so the
shader draws constant-width strokes at any camera scale:

    R  distance to a major border, 0.1 mm per step, 25.5 mm maximum
    G  distance to a minor border, same scale
    B  fiber triangle wave, 0 on a fiber line, 255 where no fibers are drawn

Coordinates are metres. +X is forward, +Y up, +Z toward the person's right.
Curves are written for the left side (Z < 0 in the mesh) with lateral
distances as positive numbers; they are mirrored to the right side.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage
from scipy.spatial import cKDTree

ROOT = Path(__file__).resolve().parents[2]
MESH = ROOT / 'tools/stretch-demo/model/human_mesh.json'
OUT = ROOT / 'tools/stretch-demo/model/anatomy_map.png'
MAX_MM = 25.5
FIBER_SPACING = 0.0115  # metres between drawn fiber lines
WRAP_CENTER_X = 0.02    # torso axis for the wrap chart

GROUPS = {
    'torso': ['spine', 'chest', 'breast', 'clavicle', 'shoulder01', 'pelvis', 'root', 'neck'],
    'upperarm': ['upperarm'],
    'lowerarm': ['lowerarm'],
    'hand': ['wrist', 'metacarpal', 'finger', 'thumb'],
    'upperleg': ['upperleg'],
    'lowerleg': ['lowerleg'],
    'foot': ['foot', 'toe'],
    'head': ['head', 'jaw', 'eye', 'oris', 'oculi', 'orbicularis', 'levator', 'risorius', 'temporalis', 'special', 'tongue'],
}


# ---------------------------------------------------------------- mesh input
def load(size: int):
    data = json.loads(MESH.read_text())
    mesh = data['mesh']
    P = np.array(mesh['positions'], dtype=np.float64).reshape(-1, 3)
    N = np.array(mesh['normals'], dtype=np.float64).reshape(-1, 3)
    UV = np.array(mesh['uvs'], dtype=np.float64).reshape(-1, 2)
    T = np.array(mesh['triangles']).reshape(-1, 3)
    bones = data['skeleton']['bones']
    names = [b['name'].lower() for b in bones]
    skin = data['skin']
    offsets, joints, weights = skin['influenceOffsets'], skin['allJoints'], skin['allWeights']
    G = np.zeros((len(P), len(GROUPS)))
    keys = list(GROUPS)
    for v in range(len(P)):
        for o in range(offsets[v], offsets[v + 1]):
            name = names[joints[o]]
            for gi, key in enumerate(keys):
                if any(token in name for token in GROUPS[key]):
                    G[v, gi] += weights[o]
                    break
    bone_heads = {b['name']: np.array(b['head']) for b in bones}
    bone_tails = {b['name']: np.array(b['tail']) for b in bones}

    # Rasterize position, normal, and region weights into UV space.
    pos = np.full((size, size, 3), np.nan)
    nor = np.zeros((size, size, 3))
    grp = np.zeros((size, size, len(GROUPS)))
    px = np.stack([UV[:, 0] * size - 0.5, (1 - UV[:, 1]) * size - 0.5], 1)
    for t in T:
        a, b, c = px[t]
        x0 = max(int(np.floor(min(a[0], b[0], c[0]))), 0); x1 = min(int(np.ceil(max(a[0], b[0], c[0]))), size - 1)
        y0 = max(int(np.floor(min(a[1], b[1], c[1]))), 0); y1 = min(int(np.ceil(max(a[1], b[1], c[1]))), size - 1)
        if x1 < x0 or y1 < y0:
            continue
        xs, ys = np.meshgrid(np.arange(x0, x1 + 1), np.arange(y0, y1 + 1))
        v0, v1 = b - a, c - a
        den = v0[0] * v1[1] - v1[0] * v0[1]
        if abs(den) < 1e-12:
            continue
        qx, qy = xs - a[0], ys - a[1]
        l1 = (qx * v1[1] - v1[0] * qy) / den
        l2 = (v0[0] * qy - qx * v0[1]) / den
        l0 = 1 - l1 - l2
        inside = (l0 >= -1e-3) & (l1 >= -1e-3) & (l2 >= -1e-3)
        if not inside.any():
            continue
        w = np.stack([l0[inside], l1[inside], l2[inside]], 1)
        Y, X = ys[inside], xs[inside]
        pos[Y, X] = w @ P[t]
        n = w @ N[t]
        nor[Y, X] = n / np.linalg.norm(n, axis=1, keepdims=True)
        grp[Y, X] = w @ G[t]
    return pos, nor, grp, bone_heads, bone_tails


# ---------------------------------------------------------------- curves
def spline(points, step=0.0008):
    """Catmull-Rom through 2D control points, resampled about every 0.8 mm."""
    pts = np.array(points, dtype=np.float64)
    if len(pts) == 2:
        length = np.linalg.norm(pts[1] - pts[0])
        n = max(2, int(length / step))
        s = np.linspace(0, 1, n)[:, None]
        return pts[0] * (1 - s) + pts[1] * s
    ext = np.vstack([2 * pts[0] - pts[1], pts, 2 * pts[-1] - pts[-2]])
    out = []
    for i in range(1, len(ext) - 2):
        p0, p1, p2, p3 = ext[i - 1], ext[i], ext[i + 1], ext[i + 2]
        n = max(2, int(np.linalg.norm(p2 - p1) / step))
        for s in np.linspace(0, 1, n, endpoint=False):
            s2, s3 = s * s, s * s * s
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * s + (2 * p0 - 5 * p1 + 4 * p2 - p3) * s2 + (-p0 + 3 * p1 - 3 * p2 + p3) * s3))
    out.append(pts[-1])
    return np.array(out)


class Curve:
    def __init__(self, chart, points, kind='major', taper=(0.15, 0.15), mirror=True, region=None):
        self.chart, self.points, self.kind, self.taper, self.mirror = chart, points, kind, taper, mirror
        self.region = region


class Charts:
    """Drapes 2D chart curves onto the rest surface."""

    def __init__(self, pos, nor, grp, heads, tails):
        self.valid = np.isfinite(pos[..., 0])
        self.P = pos[self.valid]
        self.N = nor[self.valid]
        self.G = grp[self.valid]
        self.heads, self.tails = heads, tails
        keys = list(GROUPS)
        self.gi = {k: i for i, k in enumerate(keys)}
        self.cache = {}

    def region(self, name, side):
        g = self.G
        z = self.P[:, 2]
        sidemask = (z <= 0.004) if side == 'L' else (z >= -0.004)
        if name == 'torso':
            return (g[:, self.gi['torso']] > 0.35) & (g[:, self.gi['upperarm']] < 0.55) & (self.P[:, 1] > 0.95)
        if name == 'arm':
            return ((g[:, self.gi['upperarm']] + g[:, self.gi['lowerarm']] + g[:, self.gi['torso']] * 0.4) > 0.3) & sidemask & (np.abs(z) > 0.12)
        if name == 'leg':
            return ((g[:, self.gi['upperleg']] + g[:, self.gi['lowerleg']]) > 0.3) & sidemask
        return sidemask

    def limb(self, name, side):
        """Axis, frame, and limb-local coordinates for a limb segment."""
        a_name, b_name = {
            'upperarm': ('upperarm01', 'lowerarm01'), 'forearm': ('lowerarm01', 'wrist'),
            'thigh': ('upperleg01', 'lowerleg01'), 'shin': ('lowerleg01', 'foot'),
        }[name]
        a = self.heads[f'{a_name}.{side}'].copy()
        b = self.heads[f'{b_name}.{side}'].copy()
        axis = b - a
        length = np.linalg.norm(axis)
        axis /= length
        ref = np.array([1.0, 0, 0]) if name != 'forearm' else np.array([0, 1.0, 0])
        front = ref - axis * ref.dot(axis)
        front /= np.linalg.norm(front)
        lateral = np.cross(axis, front)
        # Lateral must point away from the body midline on both sides.
        mid = (a + b) / 2
        if lateral[2] * (-1 if side == 'L' else 1) < 0:
            lateral = -lateral
        return a, axis, length, front, lateral

    def limb_coords(self, P, name, side):
        a, axis, length, front, lateral = self.limb(name, side)
        d = P - a
        t = d @ axis / length
        radial = d - np.outer(d @ axis, axis)
        theta = np.degrees(np.arctan2(radial @ lateral, radial @ front))
        r = np.linalg.norm(radial, axis=1)
        return t, theta, r

    def drape(self, curve: Curve, side: str):
        """Place a chart curve on the skin. The chart coordinates stay exact;
        only the depth comes from the surface, so strokes do not zigzag."""
        sign = -1 if side == 'L' else 1
        pts = spline(curve.points)
        chart = curve.chart
        P, N = self.P, self.N
        if chart == 'wrap':
            # Cylinder around the torso: degrees from the back midline toward the
            # side, and height. It follows the flank, where the back chart is edge-on.
            mask = self.region('torso', side) & (P[:, 0] < 0.06)
            idx = np.flatnonzero(mask)
            phi = np.degrees(np.arctan2(P[idx, 2] * sign, WRAP_CENTER_X - P[idx, 0]))
            coords = np.stack([np.radians(phi) * 0.12, P[idx, 1]], 1)
            tree = cKDTree(coords)
            query = np.stack([np.radians(pts[:, 0]) * 0.12, pts[:, 1]], 1)
            dist, near = tree.query(query, k=6)
            ok = dist[:, 0] < 0.006
            w = 1 / (dist + 1e-4)
            out = (P[idx[near]] * w[..., None]).sum(1) / w.sum(1)[:, None]
            return smooth_polyline(out[ok], 4)
        if chart in ('front', 'back', 'side'):
            if chart == 'side':
                mask = self.region('torso', side) & (N[:, 2] * sign > 0.12)
                coords = np.stack([P[:, 0], P[:, 1]], 1)
            else:
                facing = N[:, 0] > 0.12 if chart == 'front' else N[:, 0] < -0.12
                mask = self.region('torso', side) & facing
                coords = np.stack([P[:, 2] * sign, P[:, 1]], 1)
            idx = np.flatnonzero(mask)
            tree = cKDTree(coords[idx])
            dist, near = tree.query(pts, k=6)
            ok = dist[:, 0] < 0.006
            w = 1 / (dist + 1e-4)
            if chart == 'side':
                depth = (P[idx[near], 2] * w).sum(1) / w.sum(1)
                out = np.stack([pts[:, 0], pts[:, 1], depth], 1)
            else:
                depth = (P[idx[near], 0] * w).sum(1) / w.sum(1)
                out = np.stack([depth, pts[:, 1], pts[:, 0] * sign], 1)
            return smooth_polyline(out[ok], 3)
        # Limb chart: points are (t, theta degrees); radius comes from the skin.
        _, name = chart.split(':')
        region = 'arm' if name in ('upperarm', 'forearm') else 'leg'
        idx = np.flatnonzero(self.region(region, side))
        t, theta, r = self.limb_coords(P[idx], name, side)
        a, axis, length, front, lateral = self.limb(name, side)
        rr = 0.045
        ang = np.radians(theta)
        coords = np.stack([t * length, np.cos(ang) * rr, np.sin(ang) * rr], 1)
        tree = cKDTree(coords)
        q_ang = np.radians(pts[:, 1])
        query = np.stack([pts[:, 0] * length, np.cos(q_ang) * rr, np.sin(q_ang) * rr], 1)
        dist, near = tree.query(query, k=8)
        ok = dist[:, 0] < 0.008
        w = 1 / (dist + 1e-4)
        radius = (r[near] * w).sum(1) / w.sum(1)
        radius = np.convolve(np.pad(radius, 6, mode='edge'), np.ones(13) / 13, mode='valid')
        direction = np.outer(np.cos(q_ang), front) + np.outer(np.sin(q_ang), lateral)
        out = a + np.outer(pts[:, 0] * length, axis) + direction * radius[:, None]
        return out[ok]


def smooth_polyline(points, sigma=6):
    """Gaussian smoothing along the draped curve; removes texel-snapping jitter."""
    if len(points) < 5:
        return points
    radius = int(sigma * 3)
    k = np.exp(-0.5 * (np.arange(-radius, radius + 1) / sigma) ** 2)
    padded = np.vstack([np.repeat(points[:1], radius, 0), points, np.repeat(points[-1:], radius, 0)])
    out = np.stack([np.convolve(padded[:, c], k / k.sum(), mode='valid') for c in range(3)], 1)
    return out


def taper_profile(n, taper):
    s = np.linspace(0, 1, n)
    a, b = taper
    w = np.ones(n)
    if a > 0:
        w = np.minimum(w, np.clip(s / a, 0, 1))
    if b > 0:
        w = np.minimum(w, np.clip((1 - s) / b, 0, 1))
    # Penalty in metres added to the distance: the stroke thins and fades out.
    return (1 - w) ** 1.5 * 0.0022


# ---------------------------------------------------------------- anatomy
def anatomy():
    C = []
    add = lambda *a, **k: C.append(Curve(*a, **k))
    # ---- chest (front chart: lateral distance, height)
    add('front', [(0.0, 1.415), (0.0, 1.33), (0.0, 1.24)], 'major', (0.2, 0.1), mirror=False)          # sternum between pectorals
    add('front', [(0.004, 1.236), (0.045, 1.222), (0.090, 1.224), (0.126, 1.245), (0.152, 1.285), (0.166, 1.325)], 'major', (0.02, 0.05))  # lower pectoral border
    add('front', [(0.105, 1.418), (0.130, 1.380), (0.150, 1.345), (0.163, 1.318)], 'major', (0.25, 0.05))  # deltopectoral groove
    add('front', [(0.018, 1.418), (0.070, 1.414), (0.120, 1.420), (0.160, 1.428)], 'minor', (0.3, 0.3))   # clavicle
    # ---- abdomen
    add('front', [(0.0, 1.228), (0.0, 1.150), (0.0, 1.070), (0.0, 1.025)], 'major', (0.05, 0.2), mirror=False)  # linea alba
    add('front', [(0.074, 1.222), (0.078, 1.170), (0.074, 1.110), (0.066, 1.055), (0.058, 1.020)], 'major', (0.05, 0.2))  # linea semilunaris
    for y in (1.183, 1.130, 1.077):
        add('front', [(0.003, y), (0.035, y - 0.004), (0.074, y + 0.004)], 'minor', (0.0, 0.1))     # tendinous intersections
    # ---- serratus and obliques (side chart: forward x, height)
    for i, (x, y) in enumerate([(0.062, 1.245), (0.068, 1.208), (0.070, 1.172)]):
        add('side', [(x - 0.036, y + 0.016), (x - 0.012, y + 0.003), (x + 0.004, y - 0.008)], 'minor', (0.4, 0.05))
    add('side', [(-0.020, 1.070), (0.020, 1.055), (0.060, 1.040)], 'minor', (0.3, 0.4))                     # oblique over iliac crest
    add('side', [(0.070, 1.150), (0.075, 1.110), (0.072, 1.060)], 'minor', (0.3, 0.3))                        # oblique front edge
    add('side', [(0.020, 1.265), (0.045, 1.215), (0.066, 1.160), (0.074, 1.120)], 'minor', (0.3, 0.3))      # serratus/external oblique boundary
    # ---- back
    # One long curve per border, following the anatomy of the back:
    add('back', [(0.0, 1.440), (0.0, 1.300), (0.0, 1.160), (0.0, 1.030)], 'minor', (0.25, 0.05), mirror=False)  # spine groove
    add('back', [(0.034, 1.500), (0.070, 1.455), (0.120, 1.428), (0.165, 1.414)], 'minor', (0.2, 0.3))       # upper trapezius, neck to shoulder
    add('back', [(0.168, 1.408), (0.125, 1.392), (0.085, 1.366), (0.066, 1.330),
                 (0.048, 1.260), (0.026, 1.190), (0.004, 1.128)], 'major', (0.08, 0.02))                        # trapezius: scapular spine, then lower fibers to T12
    add('back', [(0.080, 1.345), (0.086, 1.292), (0.098, 1.245), (0.122, 1.250), (0.148, 1.268), (0.168, 1.285)], 'minor', (0.05, 0.35))  # infraspinatus and teres over the scapula
    add('back', [(0.042, 1.215), (0.046, 1.130), (0.040, 1.035)], 'minor', (0.45, 0.05))                     # erector spinae, lateral edge
    add('wrap', [(78, 1.300), (72, 1.230), (64, 1.160), (52, 1.100), (38, 1.060), (26, 1.040)], 'major', (0.1, 0.3))  # latissimus dorsi outer edge
    # ---- neck
    add('front', [(0.014, 1.425), (0.030, 1.460), (0.048, 1.500), (0.058, 1.540)], 'major', (0.05, 0.5))    # sternocleidomastoid
    add('front', [(0.020, 1.428), (0.050, 1.440), (0.080, 1.460)], 'minor', (0.2, 0.4))                     # clavicular head / trapezius
    # ---- upper arm (t along shoulder->elbow, degrees from front toward lateral)
    add('limb:upperarm', [(0.00, -55), (0.18, -35), (0.36, 25), (0.46, 90)], 'major', (0.1, 0.0))          # front deltoid border
    add('limb:upperarm', [(0.00, -175), (0.20, 165), (0.36, 130), (0.46, 90)], 'major', (0.1, 0.0))         # rear deltoid border
    add('limb:upperarm', [(0.42, 90), (0.62, 75), (0.86, 55)], 'major', (0.0, 0.3))                         # biceps/brachialis lateral groove
    add('limb:upperarm', [(0.18, -80), (0.50, -85), (0.85, -70)], 'major', (0.2, 0.3))                      # medial biceps/triceps groove
    add('limb:upperarm', [(0.44, 112), (0.60, 140), (0.80, 172)], 'minor', (0.1, 0.4))                      # triceps long/lateral head
    add('limb:upperarm', [(0.55, 10), (0.75, 15), (0.92, 20)], 'minor', (0.3, 0.3))                         # biceps belly edge
    # ---- forearm (reference is the upper surface in the rest pose)
    add('limb:forearm', [(0.02, -40), (0.30, -30), (0.62, -10)], 'major', (0.1, 0.4))
    add('limb:forearm', [(0.02, 60), (0.28, 55), (0.60, 40)], 'major', (0.1, 0.4))
    add('limb:forearm', [(0.05, 140), (0.35, 150), (0.70, 160)], 'minor', (0.2, 0.4))
    add('limb:forearm', [(0.05, -120), (0.40, -130), (0.72, -140)], 'minor', (0.2, 0.4))
    add('limb:forearm', [(0.04, 10), (0.30, 12), (0.66, 15)], 'minor', (0.2, 0.5))                         # extensor digitorum
    add('limb:forearm', [(0.04, -85), (0.32, -80), (0.70, -75)], 'minor', (0.2, 0.5))                       # flexor carpi
    # ---- lower leg (t along knee->ankle, degrees from front toward lateral)
    add('limb:shin', [(0.08, -20), (0.40, -18), (0.80, -12)], 'major', (0.1, 0.3))                          # shin crest
    add('limb:shin', [(0.10, 40), (0.45, 45), (0.78, 30)], 'major', (0.1, 0.3))                             # tibialis/peroneal groove
    add('limb:shin', [(0.10, 100), (0.40, 105), (0.70, 110)], 'minor', (0.2, 0.3))                          # peroneal edge
    add('limb:shin', [(0.06, 150), (0.25, 125), (0.42, 140), (0.52, 170)], 'major', (0.1, 0.05))            # lateral gastrocnemius head
    add('limb:shin', [(0.06, -150), (0.28, -115), (0.48, -140), (0.58, -170)], 'major', (0.1, 0.05))        # medial gastrocnemius head
    add('limb:shin', [(0.10, 180), (0.30, 180), (0.45, 179)], 'minor', (0.2, 0.3), region='leg')           # split between heads
    add('limb:shin', [(0.52, 168), (0.75, 172), (0.92, 175)], 'minor', (0.1, 0.3))                          # achilles, lateral edge
    add('limb:shin', [(0.58, -168), (0.78, -172), (0.92, -176)], 'minor', (0.1, 0.3))                       # achilles, medial edge
    add('limb:shin', [(0.35, -75), (0.60, -85), (0.80, -100)], 'minor', (0.3, 0.3))                         # soleus medial
    return C


# ---------------------------------------------------------------- fibers
def fiber_phase(charts: Charts):
    """Fiber phase per texel; iso-lines follow the muscle fiber direction."""
    P, G = charts.P, charts.G
    gi = charts.gi
    phase = np.full(len(P), np.nan)
    z = np.abs(P[:, 2])
    x, y = P[:, 0], P[:, 1]
    torso = G[:, gi['torso']] > 0.45
    front = x > 0.0
    s = FIBER_SPACING
    # rectus abdominis and erectors: vertical fibers
    rect = torso & (z < 0.075) & (y < 1.23)
    phase[rect] = z[rect] / s
    # pectoral fan toward the humerus insertion
    pec = torso & front & (y >= 1.225) & (z < 0.17)
    ang = np.arctan2(y[pec] - 1.33, z[pec] - 0.175)
    phase[pec] = ang * 0.13 / s
    # external oblique: fibers run down and forward
    obl = torso & (z >= 0.075) & (y < 1.23) & (x > -0.03)
    phase[obl] = (z[obl] * 0.8 - y[obl] * 0.6) / s
    # back: latissimus fan from the arm, trapezius/rhomboid fibers near horizontal
    back = torso & (x <= 0.0)
    upper = back & (y >= 1.24)
    phase[upper] = (y[upper] - z[upper] * 0.35) / s
    lat = back & (y < 1.24) & (z >= 0.05)
    ang = np.arctan2(y[lat] - 1.30, z[lat] - 0.18)
    phase[lat] = ang * 0.20 / s
    erect = back & (y < 1.24) & (z < 0.05)
    phase[erect] = z[erect] / s
    # pelvis under the shorts: gluteal fibers run down and out
    glute = (G[:, gi['torso']] > 0.3) & (y < 1.0)
    phase[glute] = (y[glute] * 0.7 + z[glute] * 0.7) / s
    # limbs: fibers along the limb axis
    for name, grp in (('upperarm', 'upperarm'), ('forearm', 'lowerarm'), ('thigh', 'upperleg'), ('shin', 'lowerleg')):
        for side in 'LR':
            sel = (G[:, gi[grp]] > 0.35) & ((P[:, 2] < 0) if side == 'L' else (P[:, 2] >= 0))
            if name == 'thigh':
                sel |= (G[:, gi['torso']] <= 0.3) & (G[:, gi['upperleg']] > 0.2) & ((P[:, 2] < 0) if side == 'L' else (P[:, 2] >= 0))
            idx = np.flatnonzero(sel)
            t, theta, r = charts.limb_coords(P[idx], name, side)
            phase[idx] = np.radians(theta) * np.clip(r, 0.02, 0.09) / s
    return phase


# ---------------------------------------------------------------- bake
def distance_field(charts: Charts, curves, kind):
    """3D distance from every texel to the draped curves of one kind."""
    pts, pen = [], []
    for c in curves:
        if c.kind != kind:
            continue
        for side in ('LR' if c.mirror else 'L'):
            d = charts.drape(c, side)
            if len(d) < 2:
                continue
            pts.append(d)
            pen.append(taper_profile(len(d), c.taper))
    pts = np.vstack(pts)
    pen = np.concatenate(pen)
    tree = cKDTree(pts)
    dist, near = tree.query(charts.P, k=6, distance_upper_bound=0.03)
    total = np.where(np.isfinite(dist), dist + np.take(pen, np.minimum(near, len(pen) - 1)), np.inf)
    return total.min(1)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--size', type=int, default=1024)
    parser.add_argument('--out', type=Path, default=OUT)
    args = parser.parse_args()
    pos, nor, grp, heads, tails = load(args.size)
    charts = Charts(pos, nor, grp, heads, tails)
    curves = anatomy()
    channels = []
    for kind in ('major', 'minor'):
        d = distance_field(charts, curves, kind) * 1000  # mm
        channels.append(np.clip(d / MAX_MM, 0, 1))
    phase = fiber_phase(charts)
    tri = np.abs(np.mod(phase + 0.5, 1.0) - 0.5) * 2
    channels.append(np.where(np.isfinite(tri), tri * 0.97, 1.0))
    image = np.ones((args.size, args.size, 3))
    for c, values in enumerate(channels):
        image[..., c][charts.valid] = values
    # Extend islands past their UV borders so filtering never samples empty texels.
    _, (iy, ix) = ndimage.distance_transform_edt(~charts.valid, return_indices=True)
    image = image[iy, ix]
    Image.fromarray(np.round(image * 255).astype(np.uint8)).save(args.out, optimize=True)
    print(f'Wrote {args.out} ({args.out.stat().st_size // 1024} KB), {len(curves)} curves')


if __name__ == '__main__':
    main()
