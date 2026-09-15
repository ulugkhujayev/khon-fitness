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
thumbnails, and `guides.json` into Android assets. Each full sheet decodes
into about 15 MB with RGB565. Only the current camera sheet is retained by
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
