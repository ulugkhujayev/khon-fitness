# Stretch guide audit, build 0.4.5

Date: 2026-09-17. Method: frame-by-frame review of the packaged sprite sheets
(all step frames plus midpoints, both views), then one research pass per
stretch with opencode muse-spark 1.3 at xhigh, web sources cited in the nine
per-stretch files in this folder. Sides follow the app's left-side cues; the
app mirrors the image for the right side.

## Cross-cutting findings

1. Highlights are static. The mask in render.swift isAccent() depends on
   exercise and side only. Every stretch glows from frame 0, before load.
   All nine sources say the stretch builds through the steps. Fix: key the
   highlight to the keyframe, with intensity none / light / full.
2. Coral marks contracting muscles as stretched in three guides. Glute in hip
   flexor (it drives the pelvic tuck), glute and front quad in WGS (they hold
   the lunge), lower back in fold (it brakes the descent). If contraction is
   worth showing, use a second color. Never coral.
3. Glute highlights land on the shorts. The glute box is mostly under the
   shorts mask, so 90/90 shows a sliver on clothing and WGS shows a patch on
   the waistband. Either paint on top of the shorts or move the region to the
   exposed upper hamstring and hip.
4. Calves are missing in fold, elephant, and plow. All three stretch
   gastrocnemius with the knee straight.
5. No cue gives a hold time or breath. Sources give 20 to 30 s static holds,
   1 to 3 s per side for elephant, one breath per phase for cat cow.
6. Background tone differs per sheet. 90/90 view 0 and plow view 1 are near
   white. The others are grey.

## Per stretch

### World's greatest stretch (left foot forward)

Targets: right iliopsoas and rectus femoris, right calf, left glute and
proximal hamstring, left adductors on the elbow drop, left pec, obliques and
thoracic rotators on the reach. Both glutes and quads plus the whole upper
back is wrong.

Highlight map: set up = right hip front, right calf, left glute. Lower = add
left adductors. Rotate = add left chest, left obliques, left mid back. Never
the bent left quad, never the right upper back.

Pose: left foot outside the left hand, shin vertical. Right hand stays planted
in Rotate; the figure cannot support and reach with the same arm. Right knee
lifted, right glute on.

Cues: "Step left foot outside left hand, knee over ankle. Keep right leg
straight." / "Plant right hand, drop left elbow inside left foot. Hips
square." / "Open chest left, reach left arm up. Eyes follow."

### Thread the needle (left arm threads)

Targets: left posterior deltoid, infraspinatus, teres minor, rhomboids, mid
trapezius, lats, thoracic rotators. The centre upper back plus whole deltoid
highlight is close but sided wrong and includes the front deltoid.

Highlight map: set up = none. Thread = light on left rear shoulder, left
scapular border, mid thoracic. Hold = full on the same. Never the left front
deltoid or chest.

Pose: fine. Ear, not top of head, on the mat. Right arm vertical.

Cues are keepable. Add "Exhale" to Thread and "Breathe" to Hold.

### 90/90 (left leg front)

Targets: left glute max, piriformis and deep rotators, posterior capsule.
Right iliopsoas, rectus femoris, TFL on the back leg. The current sliver on
the shorts marks nothing.

Highlight map: set up = none. Front leg = left posterior hip. Back leg = add
right front hip and groin. Lean = left posterior hip full, right front hip
light. Never the lumbar spine or knees.

Pose: this is the biggest pose defect. The back right leg is extended out to
the side. It must fold: thigh 90 deg from the torso, knee 90 deg, lower leg
pointing back, inner knee and calf on the floor. Front leg: outer knee and
calf down, calf parallel to the torso. Both sit bones down.

Cues: "Sit tall, both sit bones down." / "Lower left knee out front, bent 90
degrees, calf on floor." / "Fold right leg to the side, knee 90 degrees, inner
leg down." / "Lengthen spine, hinge at hips over front shin, breathe."

### Plow

Targets: erector spinae and deep posterior spinal muscles, glute max,
hamstrings, calves. Secondary: pecs and front deltoid as the shoulders tuck.
Current lower back plus hamstrings is right but incomplete and constant.

Highlight map: set up = none. Lift = light hamstrings and calves. Roll = add
glutes, lumbar and thoracic paraspinals. Hold = full posterior chain, light
chest and neck extensors. Never the front of the neck, quads, or arms.

Pose: hips must stack over the shoulders, weight on the upper back, neck with
space. The Hold cue "reach toes toward the floor" invites a collapsed spine.
Sources allow feet hovering or on a support.

Cues: keep set up and lift. Roll: "Slowly roll hips up and carry legs
overhead. No swinging." Hold: "Rest toes on floor or support. Weight on upper
back, not neck."

### Hip flexor (right knee down)

Targets: right iliopsoas. Secondary: right rectus femoris, TFL, sartorius.
The right glute contracts to tuck the pelvis. It is not stretched. The
mid-quad belly is not the primary target.

Highlight map: set up = none. Tuck = light on right hip crease and upper
front thigh. Hold = stronger on the same. Never the glute in coral.

Pose: the three frames are nearly identical, so the user sees nothing to
copy. Frame 32 needs a visible posterior tilt: tailbone under, low back
flattens. Frame 63 adds a 3 to 5 cm forward hip glide with the front knee
over the ankle. The rear hand should rest on the front thigh, not press into
it.

Cues: "Kneel on right knee under hip. Left foot flat ahead, knee over
ankle." / "Tuck tailbone under. Squeeze right glute. Ribs down." / "Stay
tall. Shift hips one inch forward. Breathe."

### Hamstring fold

Targets: hamstrings both sides, calves, glute max. Lower back gets traction,
not a prime stretch, and lighting it rewards rounding.

Highlight map: stand = none. Fold = posterior thighs mid, calves light. Hold =
posterior thighs full, calves and gluteal fold light. Lumbar never in coral.

Pose: fine. Keep a visible micro-bend at the knees in Fold and Hold.

Cues: Fold cue "Bend at your hips and reach toward the floor" invites
rounding. Replace with "Hinge at the hip crease with a long spine, knees soft,
hands to shins or floor." Hold: add "Breathe slowly and hold 20 to 30
seconds."

### Elephant walks

Targets: hamstrings and gastrocnemius of the straight leg only, alternating.
The current single fixed hamstring is wrong for half the reps and misses the
calf.

Highlight map: set up = both posterior thighs and upper calves, low. Bend one
knee = straight leg only, posterior thigh and calf. Switch = the other leg.
Never the bent leg.

Pose: the knee bend is too small to read. Bent knee clearly flexed with the
heel lifted, straight knee locked with the heel down. Hands stay planted.
Hold 1 to 3 s per side, not a continuous sway.

Cues: "Stand feet hip-width. Hinge at hips. Hands on floor or blocks." /
"Lift left heel, bend left knee. Straighten right leg and squeeze right
thigh." / "Straighten left leg and squeeze left thigh as you bend right
knee."

### Cat cow

Targets alternate. Cat (round): erector spinae, multifidus, neck extensors,
rhomboids. Cow (arch): rectus abdominis, obliques, pecs, iliopsoas. The
current lower back plus belly in both phases mixes stretch and contraction.

Highlight map: set up = none. Round = bilateral thoracic and lumbar erectors
and between the blades. Arch = rectus abdominis, external obliques, chest.
Lumbar extensors never glow in Arch.

Pose: uncross the ankles. Feet hip-width, tops flat. Round needs a tucked
pelvis and a gentle chin tuck, not a dropped head.

Cues: Round: "Exhale. Push floor away, round spine up, tuck chin gently."
Arch: "Inhale. Lift tailbone, lower belly, open chest, gaze forward."

### Shoulder internal rotation (left arm)

Targets: left infraspinatus, teres minor, posterior deltoid, posterior
capsule. The current top and front deltoid cap is the wrong side of the
joint. Those fibres shorten in this position.

Highlight map: stand = none. Reach back = light on left rear shoulder below
the scapular spine and the rear deltoid. Slide up = stronger on the same.
Never the front deltoid, upper trapezius, or chest.

Pose: the hand reaches mid back in Slide up. Sources put the useful range
between the sacrum and T12; above that the shoulder compensates. Lower the
hand. Keep the left elbow tucked near the side and the shoulder down.

Cues: "Stand tall, feet hip width, shoulders down, gaze forward." / "Bend
left elbow, rest back of hand on sacrum, elbow close." / "Slide hand up low
back until mild pull. Breathe, hold 15 to 30 seconds."

## Priority order

1. Keyframe-aware highlights with intensity. Fixes finding 1 for all nine.
2. Wrong targets: shoulder IR (rear not front), WGS (one-sided, no upper
   back at set up), elephant (follow the straight leg), hip flexor (no glute),
   cat cow (alternate front and back).
3. Pose: 90/90 back leg fold, hip flexor visible tuck and shift, elephant
   knee bend, shoulder IR hand height, cat cow ankles.
4. Add calves to fold, elephant, plow. Move glute regions off the shorts.
5. Cue rewrites above, plus hold time and breath.
6. Unify sheet backgrounds.

Sources: see the per-stretch files, 8 to 11 URLs each.
