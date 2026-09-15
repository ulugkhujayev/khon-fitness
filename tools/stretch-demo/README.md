# Stretch demonstrations

Nine original 3D human guides, each with two camera angles and numbered setup,
movement, and hold steps. Android plays offline sprite sheets. There is no
browser, network dependency, or runtime 3D engine.

The model has separate shoulders and hips, clothing, hands, feet, a face,
and a floor mat with shadows. Pose coordinates are in metres, with X forward,
Y up, and Z toward the person's right. Source poses and camera choices are
in `poses.py`. SceneKit renders the geometry in `render.swift`.

Run from the repository root on macOS:

```sh
python3 tools/stretch-demo/poses.py
xcrun swiftc -O tools/stretch-demo/render.swift -o /tmp/khon-render
/tmp/khon-render tools/stretch-demo/poses.json /tmp/khon-demo-frames all frames
uv run --with pillow python tools/stretch-demo/pack.py /tmp/khon-demo-frames
```

The packer writes 64 frames per view in 8 by 8 WebP sheets, final-pose
thumbnails, and `guides.json` into Android assets. Each frame is 512 by 400
pixels. A sheet is 4096 by 3200, within a 4096 texture dimension. Each full sheet decodes
into about 52 MB with ARGB8888. Only the current camera sheet is retained by
the player. The library uses small thumbnails.

The guide plays automatically before Start. Selecting a numbered step pauses
on that position. Play demo restarts the lesson. Changing view preserves the
position. Starting the exercise switches to the held pose or timer-driven
repetitions. Every new phase resets demo playback. Demo playback never
advances the exercise timer.

The layout prototype is archived on local branch
`prototype/stretch-demonstrations`, commit `db7ed92`. It compared one loop,
two simultaneous views, and numbered steps with a switchable view. The last
layout keeps the human large enough to read on a phone and lets the viewer
inspect each position. The native app implements that choice. The obsolete
2D pose generator and preview have been retired.

The geometry is original, not a downloaded human asset. Visual inspection
checks the authored poses and intermediate frames; it does not establish
clinical suitability or prove that a first-time user understands every move.

## Visual regression check

Run `uv run --with pillow python tools/stretch-demo/verify.py /tmp/khon-render`
after packing. It compares the last exercise rendered alone with the same
exercise rendered after all others, and checks every packaged asset size.
The renderer also rejects any camera with a rolled horizon.

The camera uses an explicit world up vector. SceneKit's one-argument
`look(at:)` uses the camera's current `worldUp`, which caused accumulated
roll in 0.4.3. Continuous limb meshes, independently oriented pelvis and
shoulder rings, shaped soles, and higher-resolution frames replace the
segmented 0.4.3 model.
