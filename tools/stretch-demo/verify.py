"""Check rendered camera consistency and packaged sprite geometry.

Run with: uv run --with pillow python tools/stretch-demo/verify.py /tmp/khon-render
The renderer must be compiled from the current render.swift first.
"""
import json
import subprocess
import sys
import tempfile
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

renderer = str(Path(sys.argv[1]).resolve())
poses = Path('tools/stretch-demo/poses.json')
assets = Path('android/app/src/main/assets/stretch_demos')
with tempfile.TemporaryDirectory(prefix='khon-camera-check-') as tmp:
    # The old camera accumulated roll because look(at:) reused its worldUp.
    # Rendering the last guide alone must match rendering it after all others.
    full, single = Path(tmp) / 'all', Path(tmp) / 'single'
    subprocess.run([renderer, str(poses), str(full), 'all'], check=True)
    subprocess.run([renderer, str(poses), str(single), 'wgs'], check=True)
    for rendered in (single / 'wgs').glob('*.png'):
        a = Image.open(rendered).convert('RGB')
        b = Image.open(full / 'wgs' / rendered.name).convert('RGB')
        diff = ImageChops.difference(a, b)
        # GPU edge and overlapping-clothing pixels can vary by a few samples.
        # A changed camera would move thousands of pixels, not a handful.
        assert max(ImageStat.Stat(diff).mean) < 0.01, f'View depends on render order: {rendered.name}'
        changed = diff.convert('L').point(lambda value: 255 if value > 8 else 0).histogram()[255]
        assert changed < 40, f'View shifted {changed} pixels: {rendered.name}'
    print('Camera output is stable alone or last, within GPU edge tolerance.')

catalog = json.loads((assets / 'guides.json').read_text())
assert set(catalog) == set(json.loads(poses.read_text()))
for key, guide in catalog.items():
    cols = guide['columns']
    rows = (guide['frameCount'] + cols - 1) // cols
    width, height = guide['frameWidth'], guide['frameHeight']
    expected = width * cols, height * rows
    assert max(expected) <= 4096, f'{key} exceeds a 4096 texture dimension'
    for angle in range(2):
        assert Image.open(assets / f'{key}-{angle}.webp').size == expected
        assert Image.open(assets / f'{key}-{angle}-poster.webp').size == (width, height)
    frames = [step['frame'] for step in guide['steps']]
    assert frames == sorted(set(frames))
    assert frames[0] == 0 and frames[-1] == guide['frameCount'] - 1
print(f'{len(catalog)} guides: all sheets, thumbnails, and step ranges verified.')
