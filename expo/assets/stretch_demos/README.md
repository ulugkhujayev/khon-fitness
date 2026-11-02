# Stretch demonstrations

Copied unchanged from the Kotlin app (`android/app/src/main/assets/stretch_demos`
in github.com/ulugkhujayev/khon-fitness). Nine guides, two camera views each.

- `<figure>-<view>.webp`: 64 frames of 512 x 400 in an 8 x 8 sheet (4096 x 3200).
- `<figure>-<view>-poster.webp`: the last frame, for library thumbnails.
- The guide data (names, steps, loop start, rep pauses) is `src/domain/stretchGuides.json`.

They are renders of the CC0 MakeHuman mesh made by `tools/stretch-demo` in the
Kotlin repo; re-render there and copy the files here. The studio background is
#F5F5F5 (`K.DemoBackdrop`), which fills the space around a frame.
