# Anatomy report review

Reviewed on 2026-09-17 for the 0.4.6 figure revision. `REPORT.md` remains the
original audit. This file records implementation decisions and source limits.

## Accepted findings

The renderer used one fixed highlight mask per exercise. It did not receive
the animation phase. That explains the constant color, the wrong leg during
elephant walks, and the mixed front/back targets during cat cow. The corrected
design uses a strength per target region at each numbered pose and blends
between them with the body movement.

The report correctly separates the contracting rear glute from the hip-flexor
target. ACE explicitly describes the glute squeeze as a way to increase the
opposite action at the hip. Its instructions also support the upright torso,
rear knee under the hip, and front knee over the ankle.
[ACE kneeling hip-flexor guide](https://www.acefitness.org/resources/everyone/exercise-library/142/kneeling-hip-flexor-stretch/).

Cleveland Clinic supports the two bent knees, rear lower leg pointing back,
and a hip hinge for the 90/90 progression. The existing authored knee angles
need measurement as well as image review. A camera can hide a correctly bent
leg, and changing its length would not fix that problem.
[Cleveland Clinic 90/90 guide](https://health.clevelandclinic.org/90-90-stretch).

Rehab Hero describes alternating knee bends with heel lifts to isolate one
leg. Cleveland Clinic describes cat cow with a gentle chin tuck, separated
feet resting on their tops, and breathing tied to each phase.
[Rehab Hero elephant walks](https://www.rehabhero.ca/exercise/elephant-walks),
[Cleveland Clinic cat cow](https://health.clevelandclinic.org/cat-cow-stretch).

## Limits and choices

- Highlights show approximate target regions. The mesh has no separate deep
  muscle geometry. A hip overlay cannot distinguish iliopsoas from nearby
  structures. Color intensity describes the lesson phase, not measured tissue
  strain. Targets under shorts remain visible as overlays on the clothing.
- The WGS summary conflicts with itself about glutes. The flexed front hip
  can receive a target overlay. The contracting rear glute and supporting
  front quadriceps do not receive the stretch color.
- Keep hold duration in the existing routine timer. Adding fixed seconds to
  the image cues would conflict with user-edited durations. Add breathing and
  form cues without another timer or an instruction to lock the knees.
- The AAOS internal-rotation exercise uses a stick pulled sideways behind
  the back. It does not prove the report's exact muscle map for an upward
  hand slide. The report's other cited clinician also cautions that the hand
  slide combines several movements. Keep the illustration restrained and the
  cue mild, with the hand on the low back and no forced reach.
  [AAOS shoulder program](https://www.orthoinfo.org/recovery/rotator-cuff-and-shoulder-conditioning-program),
  [Mike Reinold's discussion](https://mikereinold.com/behind-the-back-shoulder-internal-rotation/).
- Plow needs a controlled entry and space beneath the neck. Do not use a neck
  highlight as an instruction to bear weight there. ACE describes support
  under the shoulders or feet as possible modifications.
  [ACE plow discussion](https://www.acefitness.org/certifiednewsarticle/1182/are-some-yoga-poses-more-harmful-than-helpful/).

Rendered images and the installed APK remain the acceptance evidence. A
passing motion check alone does not establish correct anatomy or good form.
