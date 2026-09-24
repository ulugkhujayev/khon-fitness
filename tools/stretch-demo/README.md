# Stretch demonstrations

Nine 3D human guides, each with two camera angles and numbered setup,
movement, and hold steps. Android plays offline sprite sheets. There is no
browser, network dependency, or runtime 3D engine.

The model uses a continuous athletic human mesh with its native skeleton and
skin weights. It is drawn like an anatomy atlas: near-white skin with muscle
borders, short dark hair, dark shorts from a high waist to about 6 cm below
the knees, and red-orange muscle targets. The clothing color follows the rest
mesh as the body bends.
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

A full render took 6 minutes on the development Mac. To check a few poses
first, list guides with commas and pick frames with `KHON_FRAMES`:

```sh
KHON_FRAMES=0,63 /tmp/khon-render tools/stretch-demo/poses.json /tmp/khon-test hipflexor,fold,plow,ninety frames
```

## Illustration shading

The renderer imitates an anatomy-atlas drawing. The mesh itself only has soft
MakeHuman muscle shapes, so most of the definition comes from the shader:

- `bake_anatomy.py` writes `model/anatomy_map.png` in the mesh's UV space.
  Muscle borders are hand-authored curves on simple charts: front, back, and
  side projections of the torso, a cylinder around the torso for the flank
  (`wrap`), and cylinders around the upper arm, forearm, and lower leg. The
  back uses a few long curves only: the spine groove, the trapezius (neck to
  shoulder, then along the scapular spine and down to T12), the infraspinatus
  and teres over the scapula, the erector edges, and the latissimus edge that
  sweeps from the armpit to the lower back. The script drapes each curve onto the rest surface, mirrors
  it, and stores the distance to the nearest border. Red holds major borders
  (pectorals, deltoid, linea alba and semilunaris, trapezius, scapula,
  gastrocnemius heads, shin). Green holds minor borders (abdominal segments,
  serratus, clavicle, erectors, forearm and triceps detail). Blue is a
  triangle wave across the muscle fibers. Distances are 0.1 mm per step up
  to 25.5 mm.
- The surface shader turns the distances into strokes of constant pixel width,
  a soft gray groove, and a rounded relief. The relief bends the normal
  (surface-gradient bump mapping, no tangents), so each muscle belly stays
  white on the lit side and turns gray toward its border.
- An inverted hull draws the thin dark contour. It is a copy of the posed mesh
  pushed out along its normals, front faces culled. Its width is set in
  pixels from the orthographic scale, and it thins on the hands, toes, and
  face.
- SceneKit screen-space ambient occlusion grays armpits, folds, and contacts.
- Frames render at twice the size and are averaged 2x2, so a stroke stays one
  output pixel wide.
- Highlights keep the per-vertex phase logic in `accentIntensity`. The
  renderer smooths the full-strength region over the welded surface (24
  passes, about 4 cm), so UV seams and band limits do not cut it straight.
  Each region's phase level spreads outward without blending, so a resting
  region never takes a neighbor's tint. The shader thresholds the smoothed
  region over about 1 to 2 cm for a rounded, soft target edge with a faint
  darker rim, fades it in with the phase, and draws fiber lines inside it.
- The skin/shorts edge is cut per pixel. A short hair cap sits above a hairline
  that runs from the forehead to the nape.

Parameters live in `struct Illustration` in `render.swift`: stroke widths and
darkness, groove radius, relief height and width (7 mm over 22 mm), outline
width (1.15 px), ambient occlusion (intensity 1.0, radius 6 cm), colors, the
highlight smoothing passes, and the highlight threshold (0.18 to 0.36). For side-by-side trials, override shader values
without editing the file, for example
`KHON_ILLUSTRATION=reliefHeight=4,majorDark=0.5`. `KHON_ANATOMY_MAP` points
the renderer at another map.

Rebuild the anatomy map after editing its curves (about 40 seconds):

```sh
uv run --with numpy --with scipy --with pillow python tools/stretch-demo/bake_anatomy.py
```

The shading changes color only. Pose data, skinning, cameras, frame layout,
and sheet sizes are unchanged. The earlier attempt that reshaped geometry
distorted the shoulders and feet; this one does not move a vertex.

## Packaging

The packer writes 64 frames per view in 8 by 8 WebP sheets, final-pose
thumbnails, and `guides.json` into Android assets. Sheets use WebP quality 88.
The strokes add detail, and 92 grew the bundle by 18 percent. Each frame is 512 by 400
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
