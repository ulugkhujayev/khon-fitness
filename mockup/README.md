# Khon Fitness mockup

Open `index.html` directly in a modern browser. There is no build step,
framework, CDN, server, or network request.

Data lives in localStorage under `khon-fitness.mockup.v4`. Use Programs >
Settings > Reset demo data to restore the seed. Programs contains the TP v3
editor, the shared exercise library, modalities, the week plan, and backup
actions.

Cardio is represented by exercises with a modality, intensity, fields, and a
steady or interval structure. Starting Norwegian 4x4 opens the timer. The
timer keeps the x60 control and finishes with one editable row per work
interval.

The browser mockup uses local demo data. Health Connect is represented by one
local draft, update checks are local, and JSON export/import uses a real file.
Wake Lock and audio support vary by browser. Android foreground notifications
and locked-phone execution are outside a direct-open browser mockup.
