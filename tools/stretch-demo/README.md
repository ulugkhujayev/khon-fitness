# Stretch demonstrations

Nine 3D human guides, each with two camera angles and numbered setup,
movement, and hold steps. Android plays offline sprite sheets. There is no
browser, network dependency, or runtime 3D engine.

The model uses a continuous athletic human mesh with its native skeleton and
skin weights. It has a gray body, dark knee-length shorts with a high waist,
and coral muscle highlights. The clothing color follows the rest mesh as the
body bends.
Pose coordinates are in metres, with X forward, Y up, and Z toward the
person's right. Source poses and camera choices are in `poses.py`. SceneKit
deforms and renders the mesh in `render.swift`.

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

The human mesh, skeleton, and weights come from the official MakeHuman CC0
assets. [Model provenance and rebuild instructions](model/README.md) record
the exact source commit and shape settings. `prepare_model.py` builds the
checked-in model without MakeHuman installed. The model is a build input;
Android bundles only the rendered images. Muscle highlights are visual
guides, not a clinical muscle atlas.

To review the actual packaged animations against saved previous posters:

```sh
python3 tools/stretch-demo/preview.py --out dist/qa-anatomy-revision/index.html --before dist/qa-anatomy-revision/before
python3 -m http.server 8766
```

Open `http://localhost:8766/dist/qa-anatomy-revision/`. The page plays both camera
views, mirrors the figure, and lets you inspect every frame and numbered step.
Save previous posters in `dist/qa-anatomy-revision/before/` before replacing assets.

## Visual regression check

Run `python3 tools/stretch-demo/verify_motion.py` before rendering. It uses
the renderer's actual Swift pose and skinning code to check all 64 frames.
It rejects sudden limb rolls beyond the authored motion and landmark jumps
above 15 cm per frame. It also reports the largest skin movement between
frames. These checks catch defects that numbered-pose images can miss.

Run `python3 tools/stretch-demo/verify_highlights.py` to check phase and side
masks. Inspect the rendered images too. A mask check cannot prove its color
reads correctly on the shaded model.

Run `uv run --with pillow python tools/stretch-demo/verify.py /tmp/khon-render`
after packing. It compares the last exercise rendered alone with the same
exercise rendered after all others, and checks every packaged asset size.
The renderer also rejects any camera with a rolled horizon.

The camera uses an explicit world up vector. SceneKit's one-argument
`look(at:)` uses the camera's current `worldUp`, which caused accumulated
roll in 0.4.3. The same camera check applies to the weighted human mesh.
