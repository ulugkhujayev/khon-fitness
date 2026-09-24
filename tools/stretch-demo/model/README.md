# MakeHuman mesh for stretch rendering

`human_mesh.json` is a 1.70 m adult male rest mesh prepared for the Khon
Fitness SceneKit stretch renderer. It contains the visible body, UVs, normals,
the official MakeHuman default skeleton, and the official native skin weights.

The shape uses the MakeHuman macro targets for a 25-year-old male with muscle
slider 1.0 and weight slider 0.40. The max-muscle average-weight and
min-weight targets are blended at 0.80 and 0.20. The three bundled ethnicity
targets are mixed equally, so the model does not choose one ethnicity as the
default.

Official detail targets make the anatomy easier to read in a small exercise
render. The dorsi, V-shape, stomach tone, shoulder, arm, thigh, and calf
values are recorded in `source.targets` inside `human_mesh.json`. The
max-muscle male macro supplies the base pectoral shape. A local, deterministic
chest correction flattens its rounded lower contour and lifts the lower edge.
It leaves the skeleton, skin weights, and body topology intact. The correction
uses a smooth front-chest depth cap, six smoothing passes across neighboring
vertices, and a 24 mm masked lift. `prepare_model.py` records the exact
formula.

`anatomy_map.png` is the only texture. `bake_anatomy.py` generates it from
this mesh's UVs and hand-authored muscle-border curves. It is original work,
not derived from any third-party texture. Rebuild it whenever the mesh or its
UVs change.

## Source and license

The data comes from the official MakeHuman repository at commit
[`a8bc2d54ff0ac92e78ff71431b1023eda42bf482`](https://github.com/makehumancommunity/makehuman/tree/a8bc2d54ff0ac92e78ff71431b1023eda42bf482):

- [`base.obj`](https://github.com/makehumancommunity/makehuman/blob/a8bc2d54ff0ac92e78ff71431b1023eda42bf482/makehuman/data/3dobjs/base.obj)
- [`default.mhskel`](https://github.com/makehumancommunity/makehuman/blob/a8bc2d54ff0ac92e78ff71431b1023eda42bf482/makehuman/data/rigs/default.mhskel)
- [`default_weights.mhw`](https://github.com/makehumancommunity/makehuman/blob/a8bc2d54ff0ac92e78ff71431b1023eda42bf482/makehuman/data/rigs/default_weights.mhw)
- the macro and anatomy target files listed in `human_mesh.json` and
  `prepare_model.py`

MakeHuman says its bundled base mesh, targets, modifiers, poses, and other
graphical assets are CC0. The repository also marks the weight file as CC0.
The full notice bundled by MakeHuman is copied in `LICENSE-CC0.md`. No
third-party community asset is used. The MakeHuman application code is AGPL,
but no application code or Python module is copied into this asset or needed
at runtime.

Primary license sources:

- [MakeHuman asset and output terms](https://github.com/makehumancommunity/makehuman/blob/a8bc2d54ff0ac92e78ff71431b1023eda42bf482/makehuman/license.txt#L50-L89)
- [MakeHuman CC0 legal text](https://github.com/makehumancommunity/makehuman/blob/a8bc2d54ff0ac92e78ff71431b1023eda42bf482/LICENSE.ASSETS.md)

## What is included

Only faces in the MakeHuman `body` group are exported. Every `joint-*` and
`helper-*` group is excluded. This removes rig markers and proxy geometry,
including the genital, teeth, tongue, eyelash, hair, skirt, tights, and eye
helpers. The visible body still has the source face, hands, fingers, feet, and
toes. Eyes can be added separately by the renderer.

The source mesh can carry up to nine native bone weights on a vertex.
`influenceOffsets`, `allJoints`, and `allWeights` retain every one in a compact
CSR layout. The fixed `joints` and `weights` arrays also provide four slots per
vertex for SceneKit. That packing retains the four strongest native weights
and renormalizes them. The converter never synthesizes nearest-bone weights.

## Coordinate and matrix conventions

All positions use metres in a right-handed system:

- +X points forward from the person.
- +Y points up, with the soles at Y = 0.
- +Z points toward the person's right.

The mesh and skeleton are in global rest space. Bones retain MakeHuman names.
Bone parents are indexes into the parent-before-child `bones` array, and the
root parent is `-1`. The local bone Y axis runs from head to tail. Matrices are
flat row-major arrays. Translation occupies elements 3, 7, and 11.

`skin.joints` and `skin.weights` contain four entries per render vertex. The
full weights for vertex `i` occupy
`influenceOffsets[i]..<influenceOffsets[i + 1]` in `allJoints` and
`allWeights`. All joint values index `skeleton.bones`. OBJ UV seams duplicate
render vertices, so `mesh.sourceVertexIds` records the original MakeHuman
vertex for each one.
The `hip` and `shoulder` landmarks are the midpoint of their left and right
landmarks. `chest` is 53 percent of the line from `hip` to `shoulder`. These
definitions match the center and curve controls in `poses.json`.

## Rebuild

From the repository root:

```sh
source_dir=$(mktemp -d /tmp/khon-makehuman-XXXXXX)
gh repo clone makehumancommunity/makehuman "$source_dir"
git -C "$source_dir" checkout a8bc2d54ff0ac92e78ff71431b1023eda42bf482
python3 tools/stretch-demo/prepare_model.py "$source_dir"
```

The converter uses only Python's standard library. It writes compact JSON and
does not require MakeHuman itself.
