(function () {
  "use strict";

  const STORAGE_KEY = "khon-fitness.mockup.v4";
  const TODAY = "2026-09-08";
  const APP_VERSION = "v0.3.0";
  const SCHEMA_VERSION = 4;

  const MUSCLE_GROUPS = [
    "chest", "upper back", "lats", "shoulders", "biceps", "triceps",
    "forearms", "core", "lower back", "glutes", "quads", "hamstrings", "calves"
  ];
  const CARDIO_FIELDS = ["time", "distance", "avgHr", "watts", "laps", "poolLength", "stroke"];
  const INTENSITIES = ["low", "moderate", "high"];
  const DAY_NAMES = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
  const DAY_KEYS = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"];
  const DAY_LETTERS = ["M", "T", "W", "T", "F", "S", "S"];
  const FULL_SCREEN_ROUTES = ["program-editor", "block-editor", "exercise-editor", "modality-editor", "cardio-form", "timer", "session-detail"];
  const COLOR_PRESETS = [
    "#f0a35a", "#71b7ff", "#73d6a2", "#b99aff", "#ff8f9f", "#e5c07b",
    "#8bd3dd", "#c792ea", "#f78c6c", "#a8d08d", "#d19a66", "#9a9a9a"
  ];

  const MODALITY_SEEDS = [
    { id: "bike", name: "Bike", color: "#b99aff" },
    { id: "run", name: "Run", color: "#73d6a2" },
    { id: "swim", name: "Swim", color: "#71b7ff" },
    { id: "row", name: "Row", color: "#ff8f9f" }
  ];

  const STRENGTH_SEEDS = [
    { id: "barbell-bench-press", name: "Barbell Bench Press", primaryMuscle: "chest", secondaryMuscles: ["shoulders", "triceps"], stepKg: 2.5 },
    { id: "leg-press", name: "Leg Press", primaryMuscle: "quads", secondaryMuscles: ["glutes"], stepKg: 5 },
    { id: "shoulder-press", name: "Shoulder Press", primaryMuscle: "shoulders", secondaryMuscles: ["triceps"], stepKg: 2.5 },
    { id: "cable-seated-row", name: "Cable Seated Row", primaryMuscle: "upper back", secondaryMuscles: ["lats", "biceps"], stepKg: 2.5 },
    { id: "walking-lunge", name: "Walking Lunge", primaryMuscle: "quads", secondaryMuscles: ["glutes", "hamstrings"], stepKg: 2.5 },
    { id: "lat-pulldown", name: "Lat Pulldown", primaryMuscle: "lats", secondaryMuscles: ["biceps"], stepKg: 2.5 },
    { id: "lateral-raise", name: "Lateral Raise", primaryMuscle: "shoulders", secondaryMuscles: [], stepKg: 1 },
    { id: "leg-curl", name: "Leg Curl", primaryMuscle: "hamstrings", secondaryMuscles: ["calves"], stepKg: 2.5 },
    { id: "incline-db-press", name: "Incline DB Press", primaryMuscle: "chest", secondaryMuscles: ["shoulders", "triceps"], stepKg: 2.5 },
    { id: "calf-raises", name: "Calf Raises", primaryMuscle: "calves", secondaryMuscles: [], stepKg: 5 },
    { id: "biceps-curl", name: "Biceps Curl", primaryMuscle: "biceps", secondaryMuscles: ["forearms"], stepKg: 2.5 },
    { id: "triceps-extension", name: "Triceps Extension", primaryMuscle: "triceps", secondaryMuscles: [], stepKg: 2.5 },
    { id: "romanian-deadlift", name: "Romanian Deadlift", primaryMuscle: "hamstrings", secondaryMuscles: ["glutes", "lower back"], stepKg: 5 },
    { id: "crunch", name: "Crunch", primaryMuscle: "core", secondaryMuscles: [], stepKg: 2.5 },
    { id: "hyperextension", name: "Hyperextension", primaryMuscle: "lower back", secondaryMuscles: ["glutes", "hamstrings"], stepKg: 2.5 },
    { id: "goblet-squat", name: "Goblet Squat", primaryMuscle: "quads", secondaryMuscles: ["glutes", "core"], stepKg: 2.5 },
    { id: "dumbbell-row", name: "Dumbbell Row", primaryMuscle: "upper back", secondaryMuscles: ["lats", "biceps"], stepKg: 2.5 },
    { id: "push-up", name: "Push-up", primaryMuscle: "chest", secondaryMuscles: ["shoulders", "triceps"], stepKg: 0.5 },
    { id: "hip-thrust", name: "Hip Thrust", primaryMuscle: "glutes", secondaryMuscles: ["hamstrings"], stepKg: 5 },
    { id: "face-pull", name: "Face Pull", primaryMuscle: "upper back", secondaryMuscles: ["shoulders"], stepKg: 2.5 },
    { id: "plank", name: "Plank", primaryMuscle: "core", secondaryMuscles: ["lower back"], stepKg: 1 }
  ];

  const CARDIO_SEEDS = [
    {
      id: "norwegian-4x4", name: "Norwegian 4x4", modalityId: "bike", intensity: "high",
      structure: { type: "intervals", warmupSec: 0, workSec: 240, restSec: 180, rounds: 4 },
      fields: ["time", "avgHr", "watts"]
    },
    {
      id: "zone-2-run", name: "Zone 2 run", modalityId: "run", intensity: "low",
      structure: { type: "steady" }, fields: ["distance", "time", "avgHr"]
    },
    {
      id: "zone-2-swim", name: "Zone 2 swim", modalityId: "swim", intensity: "low",
      structure: { type: "steady" }, fields: ["poolLength", "laps", "time", "stroke"]
    },
    {
      id: "tabata-bike", name: "Tabata bike", modalityId: "bike", intensity: "high",
      structure: { type: "intervals", warmupSec: 0, workSec: 20, restSec: 10, rounds: 8 },
      fields: ["time", "avgHr", "watts"]
    }
  ];

  const BASE_LOADS = {
    "barbell-bench-press": 60, "leg-press": 130, "shoulder-press": 30,
    "cable-seated-row": 45, "walking-lunge": 16, "lat-pulldown": 55,
    "lateral-raise": 8, "leg-curl": 42.5, "incline-db-press": 22.5,
    "calf-raises": 70, "biceps-curl": 12.5, "triceps-extension": 25,
    "romanian-deadlift": 70, "crunch": 35
  };

  let timerHandle = null;
  let audioContext = null;
  let wakeLock = null;
  let toastTimer = null;
  let state = loadState();

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function uid(prefix) {
    return prefix + "-" + Date.now().toString(36) + "-" + Math.random().toString(36).slice(2, 7);
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function dateFromIso(iso) {
    const parts = String(iso).split("-").map(Number);
    return new Date(parts[0], parts[1] - 1, parts[2]);
  }

  function isoFromDate(date) {
    return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0");
  }

  function addDays(iso, amount) {
    const date = dateFromIso(iso);
    date.setDate(date.getDate() + amount);
    return isoFromDate(date);
  }

  function dateTimeFor(date, time) {
    return date + "T" + (time || "18:00:00");
  }

  function formatDate(iso, options) {
    if (!iso) return "Unknown date";
    return dateFromIso(iso).toLocaleDateString("en-US", options || { weekday: "short", month: "short", day: "numeric" });
  }

  function formatDateLong(iso) {
    return formatDate(iso, { weekday: "long", month: "long", day: "numeric", year: "numeric" });
  }

  function formatMonth(monthKey) {
    const parts = String(monthKey).split("-").map(Number);
    return new Date(parts[0], parts[1] - 1, 1).toLocaleDateString("en-US", { month: "long", year: "numeric" });
  }

  function monthKeyFor(iso) {
    return String(iso).slice(0, 7);
  }

  function weekdayIndex(iso) {
    return (dateFromIso(iso).getDay() + 6) % 7;
  }

  function numberOr(value, fallback) {
    const number = Number(value);
    return Number.isFinite(number) ? number : fallback;
  }

  function finiteOrNull(value) {
    if (value === "" || value == null) return null;
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
  }

  function formatNumber(value, digits) {
    if (value == null || !Number.isFinite(Number(value))) return "-";
    return Number(value).toFixed(digits == null ? 1 : digits).replace(/\.0$/, "");
  }

  function formatSeconds(seconds) {
    if (!Number.isFinite(Number(seconds))) return "-";
    const whole = Math.max(0, Math.round(Number(seconds)));
    return Math.floor(whole / 60) + ":" + String(whole % 60).padStart(2, "0");
  }

  function formatMinutes(seconds) {
    const value = numberOr(seconds, 0);
    if (value >= 60 && value % 60 === 0) return formatNumber(value / 60, 0) + " min";
    return formatNumber(value, 0) + " sec";
  }

  function roundTo(value, precision) {
    const factor = Math.pow(10, precision == null ? 1 : precision);
    return Math.round(Number(value) * factor) / factor;
  }

  function titleCase(value) {
    return String(value || "").replace(/\b\w/g, function (letter) { return letter.toUpperCase(); });
  }

  function slugify(value, fallback) {
    const slug = String(value || "").toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    return slug || fallback || uid("record");
  }

  function muscleAlias(value) {
    const text = String(value || "").toLowerCase().trim();
    const aliases = { back: "upper back", arms: "biceps", legs: "quads", shoulder: "shoulders" };
    return aliases[text] || (MUSCLE_GROUPS.indexOf(text) !== -1 ? text : "core");
  }

  function normalizeModality(value, index) {
    const raw = value || {};
    const color = String(raw.color || "").match(/^#[0-9a-f]{6}$/i) ? String(raw.color) : COLOR_PRESETS[index % COLOR_PRESETS.length];
    return {
      id: String(raw.id || slugify(raw.name, "modality-" + index)),
      name: String(raw.name || "Modality"),
      color: color
    };
  }

  function normalizeStructure(value) {
    const raw = value || {};
    if (raw.type !== "intervals") return { type: "steady" };
    return {
      type: "intervals",
      warmupSec: Math.max(0, Math.round(numberOr(raw.warmupSec, 0))),
      workSec: Math.max(1, Math.round(numberOr(raw.workSec, 30))),
      restSec: Math.max(0, Math.round(numberOr(raw.restSec, 30))),
      rounds: Math.max(1, Math.round(numberOr(raw.rounds, 4)))
    };
  }

  function defaultCardioFields(modalityId) {
    if (modalityId === "swim") return ["poolLength", "laps", "time", "stroke"];
    if (modalityId === "run") return ["distance", "time", "avgHr"];
    return ["time", "distance", "avgHr", "watts"];
  }

  function normalizeCardioFields(value, modalityId) {
    const fields = Array.isArray(value) ? value.filter(function (field) { return CARDIO_FIELDS.indexOf(field) !== -1; }) : [];
    return Array.from(new Set(fields.length ? fields : defaultCardioFields(modalityId)));
  }

  function normalizeExercise(value, index) {
    const raw = value || {};
    const inferredKind = raw.kind === "cardio" || raw.modalityId || raw.structure ? "cardio" : "strength";
    const base = {
      id: String(raw.id || uid("exercise") + "-" + index),
      name: String(raw.name || "Unnamed exercise"),
      kind: inferredKind,
      notes: String(raw.notes || ""),
      archived: Boolean(raw.archived),
      builtin: Boolean(raw.builtin)
    };
    if (inferredKind === "strength") {
      const primary = raw.primaryMuscle || raw.primaryMuscleGroup || raw.muscle;
      base.primaryMuscle = muscleAlias(primary);
      base.secondaryMuscles = Array.from(new Set((Array.isArray(raw.secondaryMuscles) ? raw.secondaryMuscles : (raw.secondaryMuscleGroups || [])).map(muscleAlias))).filter(function (group) {
        return group !== base.primaryMuscle;
      });
      base.stepKg = Math.max(0.1, numberOr(raw.stepKg, 2.5));
    } else {
      base.modalityId = String(raw.modalityId || raw.modality || "run");
      base.intensity = INTENSITIES.indexOf(raw.intensity) !== -1 ? raw.intensity : "moderate";
      base.structure = normalizeStructure(raw.structure);
      base.fields = normalizeCardioFields(raw.fields, base.modalityId);
    }
    return base;
  }

  function normalizePlanEntry(value) {
    if (!value || value === "rest") return { itemType: "rest", itemId: null };
    const itemType = value.itemType || (value.type === "program" || value.type === "exercise" ? value.type : null);
    if (itemType === "program" || itemType === "exercise") return { itemType: itemType, itemId: value.itemId || value.id || null };
    return { itemType: "rest", itemId: null };
  }

  function normalizeProgram(value, index) {
    const raw = value || {};
    return {
      id: String(raw.id || "program-" + index),
      name: String(raw.name || "Untitled program"),
      active: Boolean(raw.active),
      updatedAt: String(raw.updatedAt || dateTimeFor(TODAY, "08:00:00")),
      blocks: Array.isArray(raw.blocks) ? raw.blocks.map(function (block, blockIndex) {
        const rawBlock = block || {};
        return {
          id: String(rawBlock.id || "block-" + index + "-" + blockIndex),
          superset: Boolean(rawBlock.superset),
          exercises: Array.isArray(rawBlock.exercises) ? rawBlock.exercises.slice(0, 2).map(function (exercise, exerciseIndex) {
            const rawExercise = exercise || {};
            return {
              slotId: String(rawExercise.slotId || rawBlock.id + "-" + String.fromCharCode(97 + exerciseIndex)),
              exerciseId: String(rawExercise.exerciseId || "barbell-bench-press"),
              sets: Math.max(1, Math.round(numberOr(rawExercise.sets, 3))),
              minReps: Math.max(1, Math.round(numberOr(rawExercise.minReps, 8))),
              maxReps: Math.max(1, Math.round(numberOr(rawExercise.maxReps, 12)))
            };
          }) : []
        };
      }) : []
    };
  }

  function exerciseById(data, id) {
    return (data.exercises || []).find(function (exercise) { return exercise.id === id; }) || null;
  }

  function modalityById(data, id) {
    return (data.modalities || []).find(function (modality) { return modality.id === id; }) || null;
  }

  function normalizeProgramSnapshot(value, data) {
    if (!value || !Array.isArray(value.blocks)) return null;
    return {
      programId: String(value.programId || ""),
      programName: String(value.programName || "Program"),
      blocks: value.blocks.map(function (block, blockIndex) {
        const rawBlock = block || {};
        return {
          id: String(rawBlock.id || "snapshot-block-" + blockIndex),
          superset: Boolean(rawBlock.superset),
          exercises: (Array.isArray(rawBlock.exercises) ? rawBlock.exercises : []).slice(0, 2).map(function (rawExercise, exerciseIndex) {
            const library = exerciseById(data, rawExercise.exerciseId);
            return {
              slotId: String(rawExercise.slotId || rawBlock.id + "-" + exerciseIndex),
              exerciseId: String(rawExercise.exerciseId || ""),
              exerciseName: String(rawExercise.exerciseName || (library && library.name) || "Exercise"),
              primaryMuscle: muscleAlias(rawExercise.primaryMuscle || rawExercise.primaryMuscleGroup || (library && library.primaryMuscle)),
              secondaryMuscles: Array.isArray(rawExercise.secondaryMuscles) ? rawExercise.secondaryMuscles.slice() : (library ? library.secondaryMuscles.slice() : []),
              notes: String(rawExercise.notes || (library && library.notes) || ""),
              stepKg: Math.max(0.1, numberOr(rawExercise.stepKg || (library && library.stepKg), 2.5)),
              sets: Math.max(1, Math.round(numberOr(rawExercise.sets, 3))),
              minReps: Math.max(1, Math.round(numberOr(rawExercise.minReps, 8))),
              maxReps: Math.max(1, Math.round(numberOr(rawExercise.maxReps, 12)))
            };
          })
        };
      })
    };
  }

  function normalizeCardioSnapshot(value, data) {
    if (!value) return null;
    const library = exerciseById(data, value.exerciseId);
    const source = library && library.kind === "cardio" ? library : value;
    return {
      exerciseId: String(value.exerciseId || (library && library.id) || ""),
      name: String(value.name || (library && library.name) || "Cardio"),
      kind: "cardio",
      modalityId: String(value.modalityId || (source && source.modalityId) || "run"),
      intensity: INTENSITIES.indexOf(value.intensity || (source && source.intensity)) !== -1 ? (value.intensity || source.intensity) : "moderate",
      structure: normalizeStructure(value.structure || (source && source.structure)),
      fields: normalizeCardioFields(value.fields || (source && source.fields), value.modalityId || (source && source.modalityId)),
      notes: String(value.notes || (library && library.notes) || "")
    };
  }

  function normalizeWorkRows(value, snapshot) {
    const source = Array.isArray(value) ? value : [];
    const rows = [];
    const rounds = snapshot && snapshot.structure.type === "intervals" ? snapshot.structure.rounds : 0;
    for (let index = 0; index < rounds; index += 1) {
      const raw = source[index] || {};
      const row = { index: index + 1 };
      ["time", "distance", "avgHr", "watts", "laps", "poolLength", "stroke", "pace"].forEach(function (field) {
        if (raw[field] != null && raw[field] !== "") row[field] = field === "stroke" ? String(raw[field]) : finiteOrNull(raw[field]);
        else if (snapshot.fields.indexOf(field) !== -1) row[field] = null;
      });
      rows.push(row);
    }
    return rows;
  }

  function normalizeSession(value, index, data) {
    const raw = value || {};
    const inferredKind = raw.kind === "cardio" || raw.cardioExerciseSnapshot || (raw.cardio && !raw.programSnapshot) ? "cardio" : "strength";
    const programSnapshot = inferredKind === "strength" ? normalizeProgramSnapshot(raw.programSnapshot, data) : null;
    let cardioExerciseSnapshot = inferredKind === "cardio" ? normalizeCardioSnapshot(raw.cardioExerciseSnapshot, data) : null;
    if (!cardioExerciseSnapshot && inferredKind === "cardio") {
      cardioExerciseSnapshot = data.exercises.find(function (exercise) { return exercise.kind === "cardio"; }) || null;
      cardioExerciseSnapshot = cardioExerciseSnapshot ? normalizeCardioSnapshot(cardioExerciseSnapshot, data) : null;
    }
    const rawCardio = raw.cardio && typeof raw.cardio === "object" ? raw.cardio : {};
    const cardio = {};
    ["time", "distance", "avgHr", "watts", "laps", "poolLength", "stroke"].forEach(function (field) {
      if (rawCardio[field] != null && rawCardio[field] !== "") cardio[field] = field === "stroke" ? String(rawCardio[field]) : finiteOrNull(rawCardio[field]);
    });
    if (cardio.time == null && rawCardio.timeMinutes != null) cardio.time = Math.max(0, numberOr(rawCardio.timeMinutes, 0) * 60);
    const rows = normalizeWorkRows(rawCardio.workRows || rawCardio.intervals, cardioExerciseSnapshot || { structure: { type: "steady" }, fields: [] });
    if (rows.length) cardio.workRows = rows;
    return {
      id: String(raw.id || "session-" + index),
      date: String(raw.date || TODAY),
      dateTime: String(raw.dateTime || dateTimeFor(raw.date || TODAY)),
      kind: inferredKind,
      programSnapshot: programSnapshot,
      cardioExerciseSnapshot: cardioExerciseSnapshot,
      setLogs: Array.isArray(raw.setLogs) ? raw.setLogs.map(function (log, logIndex) {
        return {
          id: String(log.id || "log-" + index + "-" + logIndex),
          blockId: String(log.blockId || ""),
          slotId: String(log.slotId || ""),
          exerciseId: String(log.exerciseId || ""),
          exerciseIndex: numberOr(log.exerciseIndex, 0),
          setIndex: numberOr(log.setIndex, 0),
          weightKg: finiteOrNull(log.weightKg),
          reps: finiteOrNull(log.reps),
          status: ["done", "skipped", "pending"].indexOf(log.status) !== -1 ? log.status : "pending"
        };
      }) : [],
      cardio: cardio,
      note: String(raw.note || ""),
      effort: finiteOrNull(raw.effort),
      finished: raw.finished !== false,
      source: String(raw.source || "manual"),
      createdAt: String(raw.createdAt || raw.dateTime || dateTimeFor(raw.date || TODAY)),
      updatedAt: String(raw.updatedAt || raw.dateTime || dateTimeFor(raw.date || TODAY))
    };
  }

  function makeProgramBlock(id, firstId, secondId) {
    return {
      id: id,
      superset: true,
      exercises: [
        { slotId: id + "-a", exerciseId: firstId, sets: 3, minReps: 8, maxReps: 12 },
        { slotId: id + "-b", exerciseId: secondId, sets: 3, minReps: 8, maxReps: 12 }
      ]
    };
  }

  function makeProgram() {
    return {
      id: "tp-v3",
      name: "TP v3",
      active: true,
      blocks: [
        makeProgramBlock("block-1", "barbell-bench-press", "leg-press"),
        makeProgramBlock("block-2", "shoulder-press", "cable-seated-row"),
        makeProgramBlock("block-3", "walking-lunge", "lat-pulldown"),
        makeProgramBlock("block-4", "lateral-raise", "leg-curl"),
        makeProgramBlock("block-5", "incline-db-press", "calf-raises"),
        makeProgramBlock("block-6", "biceps-curl", "triceps-extension"),
        makeProgramBlock("block-7", "romanian-deadlift", "crunch")
      ],
      updatedAt: dateTimeFor(TODAY, "08:00:00")
    };
  }

  function makeRecoveryProgram() {
    return {
      id: "recovery-week",
      name: "Recovery week",
      active: false,
      blocks: [
        { id: "recovery-1", superset: true, exercises: [
          { slotId: "recovery-1-a", exerciseId: "goblet-squat", sets: 2, minReps: 8, maxReps: 10 },
          { slotId: "recovery-1-b", exerciseId: "dumbbell-row", sets: 2, minReps: 8, maxReps: 10 }
        ] },
        { id: "recovery-2", superset: false, exercises: [
          { slotId: "recovery-2-a", exerciseId: "plank", sets: 3, minReps: 30, maxReps: 45 }
        ] }
      ],
      updatedAt: dateTimeFor(TODAY, "08:00:00")
    };
  }

  function activeProgram(data) {
    return data.programs.find(function (program) { return program.active; }) || data.programs[0] || null;
  }

  function getProgram(data, id) {
    return data.programs.find(function (program) { return program.id === id; }) || null;
  }

  function getSession(data, id) {
    return data.sessions.find(function (session) { return session.id === id; }) || null;
  }

  function exerciseDetails(data, exerciseId, fallback) {
    return exerciseById(data, exerciseId) || fallback || { id: exerciseId, name: "Unknown exercise", kind: "strength", primaryMuscle: "core", secondaryMuscles: [], stepKg: 2.5, notes: "" };
  }

  function snapshotExercise(prescription, data, blockId, index) {
    const library = exerciseDetails(data, prescription.exerciseId);
    return {
      slotId: String(prescription.slotId || blockId + "-" + index),
      exerciseId: prescription.exerciseId,
      exerciseName: library.name,
      primaryMuscle: library.primaryMuscle || "core",
      secondaryMuscles: (library.secondaryMuscles || []).slice(),
      notes: library.notes || "",
      stepKg: numberOr(library.stepKg, 2.5),
      sets: Math.max(1, Math.round(numberOr(prescription.sets, 3))),
      minReps: Math.max(1, Math.round(numberOr(prescription.minReps, 8))),
      maxReps: Math.max(1, Math.round(numberOr(prescription.maxReps, 12)))
    };
  }

  function makeSnapshot(program, data) {
    return {
      programId: program.id,
      programName: program.name,
      blocks: (program.blocks || []).map(function (block) {
        return {
          id: block.id,
          superset: Boolean(block.superset && block.exercises.length === 2),
          exercises: block.exercises.slice(0, 2).map(function (prescription, index) {
            return snapshotExercise(prescription, data, block.id, index);
          })
        };
      })
    };
  }

  function seedWeight(exerciseId, sessionIndex, data) {
    const base = numberOr(BASE_LOADS[exerciseId], 10);
    const exercise = exerciseById(data || state.data, exerciseId);
    const step = exercise ? numberOr(exercise.stepKg, 2.5) : 2.5;
    const stepsBack = [2, 2, 1, 1, 0, 0][sessionIndex] || 0;
    return Math.max(step, base - stepsBack * step);
  }

  function makeSetLog(sessionId, blockId, exercise, exerciseIndex, setIndex, weightKg, reps, status) {
    return {
      id: sessionId + "-" + (exercise.slotId || blockId + "-" + exerciseIndex) + "-" + setIndex,
      blockId: blockId,
      slotId: exercise.slotId || blockId + "-" + exerciseIndex,
      exerciseId: exercise.exerciseId,
      exerciseIndex: exerciseIndex,
      setIndex: setIndex,
      weightKg: finiteOrNull(weightKg),
      reps: finiteOrNull(reps),
      status: status || "pending"
    };
  }

  function maxSets(exercises) {
    return (exercises || []).reduce(function (max, exercise) { return Math.max(max, numberOr(exercise.sets, 0)); }, 0);
  }

  function findLastExerciseLogs(data, exerciseId, beforeDate, excludeId) {
    const sessions = data.sessions.slice().sort(function (a, b) { return String(b.dateTime).localeCompare(String(a.dateTime)); });
    for (const session of sessions) {
      if (excludeId && session.id === excludeId) continue;
      if (session.kind !== "strength" || !session.finished || (beforeDate && session.date >= beforeDate)) continue;
      const logs = (session.setLogs || []).filter(function (log) {
        return log.exerciseId === exerciseId && log.status === "done";
      }).sort(function (a, b) { return a.setIndex - b.setIndex; });
      if (logs.length) return { session: session, logs: logs };
    }
    return null;
  }

  function buildGymLogs(data, snapshot, date, sessionIndex, sessionId, finished) {
    const logs = [];
    snapshot.blocks.forEach(function (block) {
      function addLog(exercise, exerciseIndex, setIndex) {
        const prior = finished ? null : findLastExerciseLogs(data, exercise.exerciseId, date);
        const priorLog = prior && prior.logs[setIndex] ? prior.logs[setIndex] : null;
        const weight = priorLog ? priorLog.weightKg : seedWeight(exercise.exerciseId, sessionIndex || 0, data);
        const reps = priorLog ? priorLog.reps : 8 + ((setIndex + ((sessionIndex || 0) * 2)) % 5);
        logs.push(makeSetLog(sessionId, block.id, exercise, exerciseIndex, setIndex, weight, reps, finished ? "done" : "pending"));
      }
      if (block.superset) {
        for (let setIndex = 0; setIndex < maxSets(block.exercises); setIndex += 1) {
          block.exercises.forEach(function (exercise, exerciseIndex) {
            if (setIndex < exercise.sets) addLog(exercise, exerciseIndex, setIndex);
          });
        }
      } else {
        block.exercises.forEach(function (exercise, exerciseIndex) {
          for (let setIndex = 0; setIndex < exercise.sets; setIndex += 1) addLog(exercise, exerciseIndex, setIndex);
        });
      }
    });
    return logs;
  }

  function makeGymSession(data, date, sessionIndex) {
    const program = activeProgram(data) || makeProgram();
    const id = "gym-" + date.replace(/-/g, "");
    const snapshot = makeSnapshot(program, data);
    return {
      id: id,
      date: date,
      dateTime: dateTimeFor(date, sessionIndex % 2 ? "18:45:00" : "19:10:00"),
      kind: "strength",
      programSnapshot: snapshot,
      cardioExerciseSnapshot: null,
      setLogs: buildGymLogs(data, snapshot, date, sessionIndex, id, true),
      cardio: {},
      note: sessionIndex === 5 ? "Solid Saturday session" : "TP v3 completed",
      effort: 6 + (sessionIndex % 3),
      finished: true,
      source: "manual",
      createdAt: dateTimeFor(date, "17:55:00"),
      updatedAt: dateTimeFor(date, "20:05:00")
    };
  }

  function cardioSnapshotFromExercise(data, exerciseId) {
    const exercise = exerciseById(data, exerciseId);
    if (!exercise || exercise.kind !== "cardio") return null;
    return {
      exerciseId: exercise.id,
      name: exercise.name,
      kind: "cardio",
      modalityId: exercise.modalityId,
      intensity: exercise.intensity,
      structure: clone(exercise.structure),
      fields: exercise.fields.slice(),
      notes: exercise.notes || ""
    };
  }

  function makeCardioSession(data, id, date, exerciseId, cardio, note, effort, finished) {
    return {
      id: id,
      date: date,
      dateTime: dateTimeFor(date, "07:10:00"),
      kind: "cardio",
      programSnapshot: null,
      cardioExerciseSnapshot: cardioSnapshotFromExercise(data, exerciseId),
      setLogs: [],
      cardio: cardio || {},
      note: note || "",
      effort: effort == null ? null : numberOr(effort, 6),
      finished: finished !== false,
      source: "manual",
      createdAt: dateTimeFor(date, "06:40:00"),
      updatedAt: dateTimeFor(date, "07:40:00")
    };
  }

  function bikeRows(seed) {
    return [0, 1, 2, 3].map(function (index) {
      return { index: index + 1, avgHr: seed.hr + index * 2, watts: seed.watts + index * 4 };
    });
  }

  function defaultWeekPlan() {
    return {
      mon: { itemType: "exercise", itemId: "zone-2-swim" },
      tue: { itemType: "program", itemId: "tp-v3" },
      wed: { itemType: "exercise", itemId: "zone-2-run" },
      thu: { itemType: "exercise", itemId: "norwegian-4x4" },
      fri: { itemType: "exercise", itemId: "zone-2-swim" },
      sat: { itemType: "program", itemId: "tp-v3" },
      sun: { itemType: "rest", itemId: null }
    };
  }

  function createSeedData() {
    const data = {
      schemaVersion: SCHEMA_VERSION,
      modalities: MODALITY_SEEDS.map(normalizeModality),
      exercises: STRENGTH_SEEDS.map(function (seed, index) {
        return normalizeExercise({ id: seed.id, name: seed.name, kind: "strength", primaryMuscle: seed.primaryMuscle, secondaryMuscles: seed.secondaryMuscles, stepKg: seed.stepKg, builtin: index < 14 }, index);
      }).concat(CARDIO_SEEDS.map(function (seed, index) {
        return normalizeExercise(Object.assign({ kind: "cardio", builtin: true, notes: "" }, seed), index + STRENGTH_SEEDS.length);
      })),
      programs: [makeProgram(), makeRecoveryProgram()],
      sessions: [],
      bodyweights: [
        { id: "bw-0819", date: "2026-08-19", kg: 79.8 },
        { id: "bw-0822", date: "2026-08-22", kg: 79.6 },
        { id: "bw-0826", date: "2026-08-26", kg: 79.4 },
        { id: "bw-0830", date: "2026-08-30", kg: 79.7 },
        { id: "bw-0903", date: "2026-09-03", kg: 79.2 },
        { id: "bw-0908", date: TODAY, kg: 79.1 }
      ],
      weekPlan: defaultWeekPlan(),
      overrides: {},
      settings: { appVersion: APP_VERSION, lastUpdateCheck: null }
    };
    ["2026-08-18", "2026-08-22", "2026-08-25", "2026-08-29", "2026-09-01", "2026-09-05"].forEach(function (date, index) {
      data.sessions.push(makeGymSession(data, date, index));
    });
    data.sessions.push(makeCardioSession(data, "swim-20260821", "2026-08-21", "zone-2-swim", { poolLength: 25, laps: 32, time: 30.3 * 60, stroke: "Freestyle" }, "Easy aerobic swim", 5, true));
    data.sessions.push(makeCardioSession(data, "run-20260826", "2026-08-26", "zone-2-run", { distance: 5.1, time: 28 * 60, avgHr: 149 }, "Steady park run", 6, true));
    data.sessions.push(makeCardioSession(data, "bike-20260827", "2026-08-27", "norwegian-4x4", { time: 31 * 60, distance: 14.2, avgHr: 163, watts: 204, workRows: bikeRows({ hr: 158, watts: 198 }) }, "Four clean work intervals", 8, true));
    data.sessions.push(makeCardioSession(data, "bike-20260902", "2026-09-02", "norwegian-4x4", { time: 30.5 * 60, avgHr: 166, watts: 211, workRows: bikeRows({ hr: 161, watts: 205 }) }, "Strong second half", 8, true));
    data.sessions.push(makeCardioSession(data, "swim-20260904", "2026-09-04", "zone-2-swim", { poolLength: 25, laps: 40, time: 37.3 * 60, stroke: "Mixed" }, "Longer Friday swim", 7, true));
    return data;
  }

  function normalizeData(value) {
    if (!value || typeof value !== "object" || !Array.isArray(value.exercises) || !Array.isArray(value.programs) || !Array.isArray(value.sessions)) {
      return createSeedData();
    }
    const raw = clone(value);
    const modalities = (Array.isArray(raw.modalities) ? raw.modalities : MODALITY_SEEDS).map(normalizeModality);
    const exercises = raw.exercises.map(normalizeExercise);
    const data = {
      schemaVersion: SCHEMA_VERSION,
      modalities: modalities,
      exercises: exercises,
      programs: raw.programs.map(normalizeProgram),
      sessions: [],
      bodyweights: Array.isArray(raw.bodyweights) ? raw.bodyweights.map(function (entry, index) {
        return { id: String(entry.id || "bw-" + index), date: String(entry.date || TODAY), kg: Math.max(0, numberOr(entry.kg, 0)) };
      }) : [],
      weekPlan: {},
      overrides: {},
      settings: Object.assign({ appVersion: APP_VERSION, lastUpdateCheck: null }, raw.settings || {})
    };
    data.sessions = raw.sessions.map(function (session, index) { return normalizeSession(session, index, data); });
    const plan = defaultWeekPlan();
    DAY_KEYS.forEach(function (day) { data.weekPlan[day] = normalizePlanEntry((raw.weekPlan && raw.weekPlan[day]) || plan[day]); });
    Object.keys(raw.overrides || {}).forEach(function (date) { data.overrides[date] = normalizePlanEntry(raw.overrides[date]); });
    let activeFound = false;
    data.programs.forEach(function (program) {
      if (program.active && !activeFound) activeFound = true;
      else if (program.active) program.active = false;
    });
    if (!activeFound && data.programs[0]) data.programs[0].active = true;
    return data;
  }

  function loadState() {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      if (!stored) return { data: createSeedData(), ui: emptyUi() };
      const parsed = JSON.parse(stored);
      return { data: normalizeData(parsed && parsed.data ? parsed.data : parsed), ui: emptyUi() };
    } catch (error) {
      return { data: createSeedData(), ui: emptyUi() };
    }
  }

  function saveState() {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ data: state.data }));
    } catch (error) {
      state.ui.storageError = true;
    }
  }

  function emptyUi() {
    return {
      route: "today",
      activeTab: "today",
      selectedDate: TODAY,
      selectedSessionId: null,
      selectedProgramId: "tp-v3",
      selectedBlockId: null,
      selectedExerciseId: "barbell-bench-press",
      selectedModalityId: "bike",
      calendarMonth: "2026-09",
      expandedBlockId: null,
      modal: null,
      detailEditing: false,
      bodyweightDraft: "",
      editorDraft: null,
      editorIsNew: false,
      modalityDraft: null,
      libraryFilters: { kind: "all", muscle: "all", modality: "all" },
      showArchived: false,
      cardioDraft: null,
      toast: null,
      timer: null,
      pressDate: null,
      pressTimer: null,
      pressPoint: null,
      pointerDragDate: null,
      longPressTriggered: false,
      dragDate: null
    };
  }

  function itemForEntry(data, entry) {
    const normalized = normalizePlanEntry(entry);
    if (normalized.itemType === "program") return getProgram(data, normalized.itemId);
    if (normalized.itemType === "exercise") return exerciseById(data, normalized.itemId);
    return null;
  }

  function plannedItem(data, date) {
    const override = data.overrides && data.overrides[date];
    const template = data.weekPlan[DAY_KEYS[weekdayIndex(date)]] || { itemType: "rest", itemId: null };
    const entry = normalizePlanEntry(override || template);
    return { entry: entry, source: override ? "override" : "template", item: itemForEntry(data, entry) };
  }

  function itemName(data, entry) {
    const normalized = normalizePlanEntry(entry);
    if (normalized.itemType === "rest") return "Rest";
    const item = itemForEntry(data, normalized);
    return item ? item.name : "Unassigned";
  }

  function itemColor(data, entry) {
    const normalized = normalizePlanEntry(entry);
    if (normalized.itemType === "program") return "#f0a35a";
    const item = itemForEntry(data, normalized);
    const modality = item && item.modalityId ? modalityById(data, item.modalityId) : null;
    return modality ? modality.color : "#9a9a9a";
  }

  function itemDotMode(data, entry) {
    const normalized = normalizePlanEntry(entry);
    if (normalized.itemType === "rest") return "none";
    if (normalized.itemType === "program") return "filled";
    const item = itemForEntry(data, normalized);
    return item && item.intensity === "high" ? "filled" : "ring";
  }

  function itemPreview(data, entry) {
    const normalized = normalizePlanEntry(entry);
    if (normalized.itemType === "rest") return "Recovery";
    const item = itemForEntry(data, normalized);
    if (!item) return "Choose an item";
    if (normalized.itemType === "program") {
      const sets = item.blocks.reduce(function (sum, block) {
        return sum + block.exercises.reduce(function (inner, exercise) { return inner + numberOr(exercise.sets, 0); }, 0);
      }, 0);
      return item.blocks.length + " blocks · " + sets + " sets";
    }
    const modality = modalityById(data, item.modalityId);
    if (item.structure.type === "intervals") {
      return item.structure.rounds + " × " + formatMinutes(item.structure.workSec) + " on " + (modality ? modality.name.toLowerCase() : "cardio");
    }
    return "Steady · " + (modality ? modality.name : "Cardio");
  }

  function currentBodyweight() {
    const entries = state.data.bodyweights.slice().sort(function (a, b) { return b.date.localeCompare(a.date); });
    return entries[0] ? entries[0].kg : null;
  }

  function sessionsOnDate(data, date) {
    return data.sessions.filter(function (session) { return session.date === date; }).sort(function (a, b) {
      return String(a.dateTime).localeCompare(String(b.dateTime));
    });
  }

  function sessionDisplay(data, session) {
    if (session.kind === "strength") {
      return {
        name: session.programSnapshot ? session.programSnapshot.programName : "Gym",
        color: "#f0a35a",
        mode: "filled",
        secondary: sessionStats(session).done + " sets"
      };
    }
    const snapshot = session.cardioExerciseSnapshot;
    const modality = snapshot && modalityById(data, snapshot.modalityId);
    return {
      name: snapshot ? snapshot.name : "Cardio",
      color: modality ? modality.color : "#9a9a9a",
      mode: snapshot && snapshot.intensity === "high" ? "filled" : "ring",
      secondary: cardioHeadline(data, session)
    };
  }

  function renderDot(color, mode, className) {
    return '<span class="dot ' + (mode || "none") + " " + (className || "") + '" style="--dot-color:' + escapeHtml(color || "#9a9a9a") + '" aria-hidden="true"></span>';
  }

  function renderWeekStrip() {
    const dates = Array.from({ length: 7 }, function (_, index) { return addDays(TODAY, index - weekdayIndex(TODAY)); });
    let html = '<div class="week-strip" aria-label="This week">';
    dates.forEach(function (date, index) {
      const plan = plannedItem(state.data, date);
      html += '<button class="week-cell ' + (date === TODAY ? "is-today" : "") + '" data-action="open-plan" data-date="' + date + '" data-scope="override" type="button" aria-label="' + escapeHtml(DAY_NAMES[index] + ", " + itemName(state.data, plan.entry)) + '"><span class="week-label">' + DAY_LETTERS[index] + '</span><span class="week-number">' + Number(date.slice(-2)) + '</span><span class="week-dot-wrap">' + renderDot(itemColor(state.data, plan.entry), itemDotMode(state.data, plan.entry), "week-dot") + (plan.source === "override" ? '<span class="override-mark" aria-label="Date override"></span>' : "") + '</span></button>';
    });
    return html + "</div>";
  }

  function renderNumberControl(options) {
    const value = options.value == null || options.value === "" ? "" : formatNumber(options.value, options.precision == null ? 1 : options.precision);
    const inputId = options.id;
    const unit = options.unit || "";
    const context = ' data-owner="' + escapeHtml(options.owner || "") + '" data-field="' + escapeHtml(options.field || "") + '"' +
      (options.sessionId ? ' data-session-id="' + escapeHtml(options.sessionId) + '"' : "") +
      (options.logId ? ' data-log-id="' + escapeHtml(options.logId) + '"' : "") +
      (options.index != null ? ' data-index="' + escapeHtml(options.index) + '"' : "") +
      (options.programId ? ' data-program-id="' + escapeHtml(options.programId) + '"' : "") +
      (options.blockId ? ' data-block-id="' + escapeHtml(options.blockId) + '"' : "") +
      (options.exerciseIndex != null ? ' data-exercise-index="' + escapeHtml(options.exerciseIndex) + '"' : "");
    const delta = options.delta != null ? '<span class="delta" id="delta-' + escapeHtml(inputId) + '" aria-live="polite">+' + escapeHtml(formatNumber(options.delta, options.precision == null ? 1 : options.precision)) + "</span>" : '<span class="delta" id="delta-' + escapeHtml(inputId) + '" aria-live="polite"></span>';
    return '<div class="number-field"><label class="sr-only" for="' + escapeHtml(inputId) + '">' + escapeHtml(options.label || "Number") + '</label><div class="number-control"><button class="step-button" data-action="step-input" data-target-id="' + escapeHtml(inputId) + '" data-direction="-1" type="button" aria-label="Decrease ' + escapeHtml(options.label || "value") + '">-</button><input id="' + escapeHtml(inputId) + '" type="number" inputmode="' + (options.precision ? "decimal" : "numeric") + '" value="' + escapeHtml(value) + '" min="' + escapeHtml(options.min == null ? 0 : options.min) + '" step="' + escapeHtml(options.step == null ? 1 : options.step) + '"' + context + ' data-action="numeric-input" aria-label="' + escapeHtml(options.label || "Number") + '"><span class="number-unit" aria-hidden="true">' + escapeHtml(unit) + '</span><button class="step-button" data-action="step-input" data-target-id="' + escapeHtml(inputId) + '" data-direction="1" type="button" aria-label="Increase ' + escapeHtml(options.label || "value") + '">+</button></div>' + delta + "</div>";
  }

  function renderToday() {
    const active = unfinishedSession(state.data);
    if (active) return renderLiveSession(active, false);
    const plan = plannedItem(state.data, TODAY);
    const latest = currentBodyweight();
    const inputValue = state.ui.bodyweightDraft == null ? "" : state.ui.bodyweightDraft;
    let html = '<div class="screen-inner today-screen"><header class="screen-header"><h1>Today</h1></header>' + renderWeekStrip();
    html += '<section class="today-item"><h2>' + escapeHtml(itemName(state.data, plan.entry)) + '</h2><p class="today-preview">' + escapeHtml(itemPreview(state.data, plan.entry)) + '</p>';
    if (plan.entry.itemType !== "rest" && plan.item) html += '<button class="primary-button" data-action="start-item" type="button">Start</button>';
    html += '</section><section class="bodyweight-section"><div class="section-title bodyweight-heading"><h2>Bodyweight</h2><span class="latest-value">' + (latest == null ? "-" : escapeHtml(formatNumber(latest, 1)) + " kg") + '</span></div>';
    html += renderSparkline() + '<div class="bodyweight-entry">' + renderNumberControl({ id: "bodyweight-input", label: "Bodyweight in kilograms", value: inputValue, min: 0, step: 0.1, precision: 1, unit: "kg", owner: "bodyweight", field: "kg" }) + '<button class="text-button" data-action="add-bodyweight" type="button">Add</button></div></section></div>';
    return html;
  }


  function niceStep(span, target) {
    const raw = span / Math.max(1, target);
    const steps = [0.5, 1, 2, 5, 10, 20, 25, 50, 100];
    for (let i = 0; i < steps.length; i++) if (steps[i] >= raw) return steps[i];
    return steps[steps.length - 1];
  }

  function shortDate(iso) {
    const d = new Date(iso + "T00:00:00");
    return d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
  }

  function renderLineChart(opts) {
    const series = opts.series || [];
    const width = 358;
    const height = opts.height || 176;
    const padL = 40, padR = 14, padT = 22, padB = 24;
    const plotW = width - padL - padR;
    const plotH = height - padT - padB;
    const decimals = opts.decimals == null ? 1 : opts.decimals;
    if (!series.length) return '<svg class="chart-svg" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeHtml(opts.label || "Chart") + '"><text class="chart-empty" x="' + (width / 2) + '" y="' + (height / 2) + '" text-anchor="middle">No data yet</text></svg>';
    let vmin = Math.min.apply(null, series.map(function (p) { return p.value; }));
    let vmax = Math.max.apply(null, series.map(function (p) { return p.value; }));
    if (vmin === vmax) { vmin -= 1; vmax += 1; }
    const step = niceStep(vmax - vmin, 3);
    const yMin = Math.floor(vmin / step) * step;
    const yMax = Math.ceil(vmax / step) * step;
    const ticks = [];
    for (let v = yMin; v <= yMax + step / 1000; v += step) ticks.push(Math.round(v * 1000) / 1000);
    const yFor = function (v) { return padT + (yMax - v) * plotH / (yMax - yMin); };
    const n = series.length;
    const xFor = function (i) { return n === 1 ? padL + plotW / 2 : padL + i * plotW / (n - 1); };
    const pts = series.map(function (p, i) { return { x: xFor(i), y: yFor(p.value), v: p.value, date: p.date }; });
    const line = pts.map(function (p, i) { return (i ? "L" : "M") + p.x.toFixed(1) + " " + p.y.toFixed(1); }).join(" ");
    const area = line + " L" + pts[n - 1].x.toFixed(1) + " " + (padT + plotH).toFixed(1) + " L" + pts[0].x.toFixed(1) + " " + (padT + plotH).toFixed(1) + " Z";
    let svg = '<svg class="chart-svg" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeHtml(opts.label || "Chart") + '">';
    ticks.forEach(function (t) {
      const y = yFor(t).toFixed(1);
      svg += '<line class="chart-grid" x1="' + padL + '" x2="' + (width - padR) + '" y1="' + y + '" y2="' + y + '"></line>';
      svg += '<text class="chart-tick" x="' + (padL - 8) + '" y="' + y + '" text-anchor="end" dominant-baseline="middle">' + formatNumber(t, step < 1 ? 1 : 0) + '</text>';
    });
    svg += '<path class="chart-area" d="' + area + '"></path><path class="chart-line" d="' + line + '"></path>';
    const labelAll = n <= 7;
    const maxIdx = pts.reduce(function (best, p, i) { return p.v > pts[best].v ? i : best; }, 0);
    pts.forEach(function (p, i) {
      const isLast = i === n - 1;
      svg += '<circle class="chart-marker' + (isLast ? " is-last" : "") + '" cx="' + p.x.toFixed(1) + '" cy="' + p.y.toFixed(1) + '" r="' + (isLast ? 4.5 : 3.5) + '"></circle>';
      if (labelAll || isLast || i === maxIdx) {
        let anchor = "middle";
        if (i === 0 && n > 1) anchor = "start";
        if (isLast && n > 1) anchor = "end";
        svg += '<text class="chart-value' + (isLast ? " is-last" : "") + '" x="' + p.x.toFixed(1) + '" y="' + (p.y - 9).toFixed(1) + '" text-anchor="' + anchor + '">' + formatNumber(p.v, decimals) + (opts.unit && isLast ? " " + opts.unit : "") + '</text>';
      }
    });
    const xLabelIdx = [];
    if (n === 1) xLabelIdx.push(0);
    else {
      const maxLabels = Math.max(2, Math.min(4, Math.floor(plotW / 70)));
      for (let k = 0; k < maxLabels; k++) xLabelIdx.push(Math.round(k * (n - 1) / (maxLabels - 1)));
    }
    xLabelIdx.filter(function (v, i, a) { return a.indexOf(v) === i; }).forEach(function (i) {
      const p = pts[i];
      let anchor = "middle";
      if (i === 0 && n > 1) anchor = "start";
      if (i === n - 1 && n > 1) anchor = "end";
      svg += '<text class="chart-tick" x="' + p.x.toFixed(1) + '" y="' + (height - 6) + '" text-anchor="' + anchor + '">' + escapeHtml(shortDate(p.date)) + '</text>';
    });
    return svg + '</svg>';
  }

  function renderSparkline() {
    const start = addDays(TODAY, -29);
    const entries = state.data.bodyweights.filter(function (entry) { return entry.date >= start && entry.date <= TODAY; }).sort(function (a, b) { return a.date.localeCompare(b.date); });
    return '<div class="chart chart-compact">' + renderLineChart({ series: entries.map(function (e) { return { date: e.date, value: e.kg }; }), height: 120, decimals: 1, unit: "kg", label: "Bodyweight, last 30 days" }) + '</div>';
  }

  function unfinishedSession(data) {
    return data.sessions.find(function (session) { return !session.finished; }) || null;
  }

  function logsForExercise(session, block, exerciseIndex) {
    const exercise = block.exercises[exerciseIndex];
    return (session.setLogs || []).filter(function (log) {
      return log.blockId === block.id && (log.slotId === exercise.slotId || (log.exerciseIndex === exerciseIndex && log.exerciseId === exercise.exerciseId));
    }).sort(function (a, b) { return a.setIndex - b.setIndex; });
  }

  function orderedBlockLogs(session, block) {
    const ordered = [];
    const refs = (block.exercises || []).map(function (exercise, index) { return { exercise: exercise, index: index, logs: logsForExercise(session, block, index) }; });
    function add(ref, setIndex) {
      const log = ref.logs.find(function (item) { return item.setIndex === setIndex; });
      if (log) ordered.push({ log: log, block: block, exercise: snapshotExercise(ref.exercise, state.data, block.id, ref.index), exerciseIndex: ref.index });
    }
    if (block.superset) {
      for (let setIndex = 0; setIndex < maxSets(block.exercises); setIndex += 1) {
        refs.forEach(function (ref) { if (setIndex < ref.exercise.sets) add(ref, setIndex); });
      }
    } else {
      refs.forEach(function (ref) {
        for (let setIndex = 0; setIndex < ref.exercise.sets; setIndex += 1) add(ref, setIndex);
      });
    }
    return ordered;
  }

  function orderedSessionLogs(session) {
    if (!session || !session.programSnapshot) return [];
    return session.programSnapshot.blocks.reduce(function (all, block) { return all.concat(orderedBlockLogs(session, block)); }, []);
  }

  function blockComplete(session, block) {
    const logs = orderedBlockLogs(session, block);
    return logs.length > 0 && logs.every(function (item) { return item.log.status === "done" || item.log.status === "skipped"; });
  }

  function currentBlock(session) {
    if (!session || !session.programSnapshot) return null;
    return session.programSnapshot.blocks.find(function (block) { return !blockComplete(session, block); }) || session.programSnapshot.blocks[session.programSnapshot.blocks.length - 1] || null;
  }

  function sessionStats(session) {
    const logs = session && session.setLogs ? session.setLogs : [];
    const done = logs.filter(function (log) { return log.status === "done"; });
    const volume = done.reduce(function (sum, log) { return sum + numberOr(log.weightKg, 0) * numberOr(log.reps, 0); }, 0);
    return { done: done.length, total: logs.length, volume: volume };
  }

  function findLog(session, logId) {
    return session && (session.setLogs || []).find(function (log) { return log.id === logId; });
  }

  function previousLogFor(data, session, log) {
    const prior = findLastExerciseLogs(data, log.exerciseId, session.date, session.id);
    return prior && prior.logs.find(function (item) { return item.setIndex === log.setIndex; }) || null;
  }

  function deltaFor(data, session, log, field) {
    const prior = previousLogFor(data, session, log);
    if (!prior) return null;
    const current = numberOr(log[field], NaN);
    const before = numberOr(prior[field], NaN);
    if (!Number.isFinite(current) || !Number.isFinite(before) || current <= before) return null;
    return current - before;
  }

  function newBestNames(data, session) {
    const best = [];
    (session.setLogs || []).filter(function (log) { return log.status === "done"; }).forEach(function (log) {
      const current = numberOr(log.weightKg, 0) * (1 + numberOr(log.reps, 0) / 30);
      const prior = data.sessions.filter(function (other) {
        return other.kind === "strength" && other.finished && other.id !== session.id && other.date <= session.date;
      }).reduce(function (max, other) {
        return Math.max(max, (other.setLogs || []).filter(function (item) {
          return item.exerciseId === log.exerciseId && item.status === "done";
        }).reduce(function (inner, item) {
          return Math.max(inner, numberOr(item.weightKg, 0) * (1 + numberOr(item.reps, 0) / 30));
        }, 0));
      }, 0);
      if (current > prior) {
        const exercise = exerciseById(data, log.exerciseId);
        if (exercise && best.indexOf(exercise.name) === -1) best.push(exercise.name);
      }
    });
    return best;
  }

  function renderLiveSession(session, detailMode) {
    const isReadOnly = detailMode && !state.ui.detailEditing;
    const stats = sessionStats(session);
    const current = currentBlock(session);
    const expanded = state.ui.expandedBlockId || (current && current.id);
    let html = "";
    if (detailMode) html += renderEditorTopbar("History", "back-to-history", session.programSnapshot ? session.programSnapshot.programName : "Gym", "toggle-detail-edit", state.ui.detailEditing ? "Done" : "Edit");
    html += '<div class="screen-inner tight"><header class="screen-header"><h1>' + escapeHtml(session.programSnapshot ? session.programSnapshot.programName : "Gym") + '</h1><p class="muted small">' + escapeHtml(formatDateLong(session.date)) + '</p><div class="progress-line" aria-label="' + stats.done + " of " + stats.total + ' sets logged"><span style="width:' + (stats.total ? Math.round(stats.done / stats.total * 100) : 0) + '%"></span></div><p class="progress-count">' + stats.done + " / " + stats.total + ' sets</p></header><div class="block-list">';
    (session.programSnapshot ? session.programSnapshot.blocks : []).forEach(function (block, index) {
      html += renderSessionBlock(session, block, index, block.id === expanded, isReadOnly);
    });
    html += "</div>";
    if (!detailMode) html += '<div class="session-footer"><button class="primary-button" data-action="finish-session" data-session-id="' + escapeHtml(session.id) + '" type="button">Finish</button></div>';
    return html + "</div>";
  }

  function renderEditorTopbar(backLabel, backAction, title, doneAction, doneLabel) {
    return '<div class="editor-topbar"><button class="text-button back-button" data-action="' + escapeHtml(backAction) + '" type="button">‹ ' + escapeHtml(backLabel) + '</button><h1>' + escapeHtml(title) + '</h1><button class="text-button done-button" data-action="' + escapeHtml(doneAction) + '" type="button">' + escapeHtml(doneLabel || "Done") + "</button></div>";
  }

  function renderSessionBlock(session, block, index, isExpanded, isReadOnly) {
    const logs = orderedBlockLogs(session, block);
    const done = blockComplete(session, block);
    const names = block.exercises.map(function (exercise, exerciseIndex) { return snapshotExercise(exercise, state.data, block.id, exerciseIndex).exerciseName; }).join(" · ");
    let html = '<article class="block-row ' + (done ? "is-done" : "") + '"><button class="block-heading" data-action="toggle-block" data-block-id="' + escapeHtml(block.id) + '" type="button" aria-expanded="' + String(Boolean(isExpanded)) + '"><span class="block-index">' + (index + 1) + '</span><span class="row-primary"><span class="row-name">' + escapeHtml(names) + '</span></span><span class="row-secondary">' + logs.length + ' sets</span><span class="block-status" aria-hidden="true">' + (done ? "✓" : (isExpanded ? "⌃" : "›")) + '</span></button>';
    if (isExpanded) html += '<div class="set-list">' + logs.map(function (item) { return renderSetRow(session, block, item, isReadOnly); }).join("") + "</div>";
    return html + "</article>";
  }

  function renderSetRow(session, block, item, isReadOnly) {
    const log = item.log;
    const exercise = item.exercise;
    const setNumber = String.fromCharCode(65 + item.exerciseIndex) + (log.setIndex + 1);
    const prior = previousLogFor(state.data, session, log);
    const weightDelta = deltaFor(state.data, session, log, "weightKg");
    const repsDelta = deltaFor(state.data, session, log, "reps");
    let html = '<div class="set-row ' + (log.status === "done" ? "is-done" : "") + '"><button class="round-check ' + (log.status === "done" ? "is-done" : (log.status === "skipped" ? "is-skipped" : "")) + '" data-action="toggle-set" data-session-id="' + escapeHtml(session.id) + '" data-log-id="' + escapeHtml(log.id) + '" type="button" aria-label="' + escapeHtml(log.status === "done" ? "Unlog " + exercise.exerciseName + " " + setNumber : "Log " + exercise.exerciseName + " " + setNumber) + '">' + (log.status === "done" ? "✓" : (log.status === "skipped" ? "-" : "")) + '</button><div class="set-main"><div class="set-title"><span class="set-number">' + escapeHtml(setNumber) + '</span>' + (isReadOnly ? '<span class="exercise-button">' + escapeHtml(exercise.exerciseName) + '</span>' : '<button class="exercise-button" data-action="swap-exercise" data-session-id="' + escapeHtml(session.id) + '" data-block-id="' + escapeHtml(block.id) + '" data-exercise-index="' + item.exerciseIndex + '" type="button">' + escapeHtml(exercise.exerciseName) + '</button>') + '</div></div><span class="set-last">' + (prior ? escapeHtml(formatNumber(prior.weightKg, 1) + " × " + formatNumber(prior.reps, 0)) : "") + '</span>';
    if (isReadOnly) {
      html += '<div class="read-only-values"><strong>' + formatNumber(log.weightKg, 1) + ' kg</strong><span>×</span><strong>' + formatNumber(log.reps, 0) + ' reps</strong></div>';
    } else {
      html += '<div class="set-values">' + renderNumberControl({ id: "set-" + log.id + "-weight", label: "Weight in kilograms for " + exercise.exerciseName + " " + setNumber, value: log.weightKg, min: 0, step: exercise.stepKg, precision: exercise.stepKg < 1 || exercise.stepKg % 1 ? 1 : 0, unit: "kg", owner: "set", field: "weightKg", sessionId: session.id, logId: log.id, delta: weightDelta }) + renderNumberControl({ id: "set-" + log.id + "-reps", label: "Reps for " + exercise.exerciseName + " " + setNumber, value: log.reps, min: 0, step: 1, precision: 0, unit: "reps", owner: "set", field: "reps", sessionId: session.id, logId: log.id, delta: repsDelta }) + "</div>";
    }
    return html + "</div>";
  }

  function renderHistory() {
    let html = '<div class="screen-inner"><header class="screen-title-row screen-header"><h1>History</h1><button class="text-button" data-action="health-import" type="button">Import</button></header>';
    html += renderCalendar() + renderSelectedDay() + renderBodyweightHistory() + renderExerciseProgress();
    return html + "</div>";
  }

  function calendarCells(monthKey) {
    const parts = monthKey.split("-").map(Number);
    const first = new Date(parts[0], parts[1] - 1, 1);
    const start = new Date(parts[0], parts[1] - 1, 1 - ((first.getDay() + 6) % 7));
    return Array.from({ length: 42 }, function (_, index) {
      const date = new Date(start);
      date.setDate(start.getDate() + index);
      return isoFromDate(date);
    });
  }

  function renderCalendar() {
    const cells = calendarCells(state.ui.calendarMonth);
    let html = '<section class="history-section"><div class="calendar-toolbar"><button class="text-button calendar-nav" data-action="calendar-month" data-direction="-1" type="button" aria-label="Previous month">‹</button><h2>' + escapeHtml(formatMonth(state.ui.calendarMonth)) + '</h2><button class="text-button calendar-nav" data-action="calendar-month" data-direction="1" type="button" aria-label="Next month">›</button></div><div class="calendar-grid">';
    DAY_LETTERS.forEach(function (day) { html += '<span class="weekday">' + day + "</span>"; });
    cells.forEach(function (date) {
      const sessions = sessionsOnDate(state.data, date);
      const plan = plannedItem(state.data, date);
      const dots = sessions.map(function (session) {
        const display = sessionDisplay(state.data, session);
        return '<span class="calendar-dot ' + display.mode + '" style="--dot-color:' + escapeHtml(display.color) + '" aria-hidden="true"></span>';
      }).join("");
      const plannedDot = !sessions.length && plan.entry.itemType !== "rest" && date >= TODAY ? '<span class="calendar-dot planned ' + itemDotMode(state.data, plan.entry) + '" style="--dot-color:' + escapeHtml(itemColor(state.data, plan.entry)) + '" aria-hidden="true"></span>' : '<span class="calendar-dot none" aria-hidden="true"></span>';
      const overrideMark = plan.source === "override" ? '<span class="override-mark" aria-label="Date override"></span>' : "";
      const outside = monthKeyFor(date) !== state.ui.calendarMonth;
      html += '<button class="calendar-day ' + (outside ? "is-outside " : "") + (date === TODAY ? "is-today " : "") + (date === state.ui.selectedDate ? "is-selected" : "") + '" draggable="true" data-action="select-day" data-date="' + date + '" type="button" aria-label="' + escapeHtml(formatDateLong(date) + (sessions.length ? ", " + sessions.length + " sessions" : "")) + '"><span class="day-number">' + Number(date.slice(-2)) + '</span><span class="day-dots">' + dots + plannedDot + overrideMark + "</span></button>";
    });
    return html + "</div></section>";
  }

  function renderSelectedDay() {
    const date = state.ui.selectedDate;
    const plan = plannedItem(state.data, date);
    const sessions = sessionsOnDate(state.data, date);
    let html = '<section class="selected-day"><h2>' + escapeHtml(formatDateLong(date)) + '</h2><div class="list">';
    if (plan.entry.itemType !== "rest") html += '<div class="list-row"><span class="dot-column">' + renderDot(itemColor(state.data, plan.entry), itemDotMode(state.data, plan.entry)) + '</span><span class="row-primary"><span class="row-name">' + escapeHtml(itemName(state.data, plan.entry)) + '</span></span><span class="row-secondary">planned</span></div>';
    sessions.forEach(function (session) {
      const display = sessionDisplay(state.data, session);
      html += '<button class="list-row history-session-row" data-action="open-session" data-session-id="' + escapeHtml(session.id) + '" type="button"><span class="dot-column">' + renderDot(display.color, display.mode) + '</span><span class="row-primary"><span class="row-name">' + escapeHtml(display.name) + '</span></span><span class="row-secondary">' + escapeHtml(display.secondary) + '</span><span class="row-chevron">›</span></button>';
    });
    if (!sessions.length && plan.entry.itemType === "rest") html += '<p class="empty-line">No session saved.</p>';
    return html + "</div></section>";
  }


  function renderBodyweightHistory() {
    const entries = state.data.bodyweights.slice().sort(function (a, b) { return a.date.localeCompare(b.date); });
    if (!entries.length) return "";
    const svg = renderLineChart({ series: entries.map(function (e) { return { date: e.date, value: e.kg }; }), height: 176, decimals: 1, unit: "kg", label: "Bodyweight history" });
    return '<section class="history-section"><div class="section-title"><h2>Bodyweight</h2><span class="latest-value">' + formatNumber(entries[entries.length - 1].kg, 1) + ' kg</span></div><div class="chart">' + svg + "</div></section>";
  }

  function topSet(logs) {
    return logs.filter(function (log) { return log.status === "done"; }).sort(function (a, b) {
      return numberOr(b.weightKg, 0) - numberOr(a.weightKg, 0) || numberOr(b.reps, 0) - numberOr(a.reps, 0);
    })[0] || null;
  }

  function bestEpleySet(logs) {
    return logs.filter(function (log) { return log.status === "done"; }).sort(function (a, b) {
      return epley(b) - epley(a) || numberOr(b.weightKg, 0) - numberOr(a.weightKg, 0) || numberOr(b.reps, 0) - numberOr(a.reps, 0);
    })[0] || null;
  }

  function epley(log) {
    return numberOr(log.weightKg, 0) * (1 + numberOr(log.reps, 0) / 30);
  }

  function exerciseProgress(data, exerciseId) {
    return data.sessions.filter(function (session) {
      return session.kind === "strength" && session.finished && (session.setLogs || []).some(function (log) { return log.exerciseId === exerciseId && log.status === "done"; });
    }).map(function (session) {
      const logs = session.setLogs.filter(function (log) { return log.exerciseId === exerciseId; });
      const top = topSet(logs);
      const best = bestEpleySet(logs);
      return top && best ? { date: session.date, dateTime: session.dateTime, top: top, estimated1RM: epley(best), sessionId: session.id } : null;
    }).filter(Boolean).sort(function (a, b) { return a.date.localeCompare(b.date) || a.dateTime.localeCompare(b.dateTime); });
  }

  function renderExerciseProgress() {
    const strengthExercises = state.data.exercises.filter(function (exercise) { return exercise.kind === "strength"; });
    const selected = strengthExercises.some(function (exercise) { return exercise.id === state.ui.selectedExerciseId; }) ? state.ui.selectedExerciseId : (strengthExercises[0] && strengthExercises[0].id);
    state.ui.selectedExerciseId = selected;
    const exercise = exerciseById(state.data, selected);
    let html = '<section class="history-section"><div class="field-row"><span class="field-label">Exercise</span><button class="field-value button-value" data-action="open-history-exercise" type="button"><span>' + escapeHtml(exercise ? exercise.name : "Choose exercise") + '</span><span class="row-chevron">›</span></button></div>';
    html += renderExerciseProgressCard(selected) + "</section>";
    return html;
  }

  function renderExerciseProgressCard(exerciseId) {
    const exercise = exerciseById(state.data, exerciseId);
    const points = exerciseProgress(state.data, exerciseId);
    let html = '<div class="progress-section"><h2>Estimated 1RM</h2>';
    if (!points.length) return html + '<p class="empty-line">No logged sets yet.</p></div>';
    html += renderProgressSvg(points) + '<div class="top-set-list">';
    points.slice().reverse().forEach(function (point) {
      html += '<div class="top-set-row"><span>' + escapeHtml(formatDate(point.date)) + '</span><strong>' + formatNumber(point.top.weightKg, 1) + " kg × " + formatNumber(point.top.reps, 0) + " reps</strong></div>";
    });
    html += '</div><table class="history-table"><caption>Sessions, newest first</caption><thead><tr><th>Date</th><th>Estimated 1RM</th><th>Sets</th></tr></thead><tbody>';
    points.slice().reverse().forEach(function (point) {
      const session = getSession(state.data, point.sessionId);
      const logs = session.setLogs.filter(function (log) { return log.exerciseId === exerciseId; }).sort(function (a, b) { return a.setIndex - b.setIndex; });
      html += '<tr><td>' + escapeHtml(formatDate(point.date)) + '</td><td>' + formatNumber(point.estimated1RM, 1) + ' kg</td><td>' + logs.map(function (log) { return formatNumber(log.weightKg, 1) + " × " + formatNumber(log.reps, 0); }).join("<br>") + '</td></tr>';
    });
    return html + "</tbody></table></div>";
  }


  function renderProgressSvg(points) {
    return '<div class="chart progress-chart">' + renderLineChart({ series: points.map(function (p) { return { date: p.date, value: Math.round(p.estimated1RM * 2) / 2 }; }), height: 190, decimals: 1, unit: "kg", label: "Estimated 1RM" }) + '</div>';
  }

  function renderProgramSegments(active) {
    const links = [["programs", "Programs"], ["exercise-library", "Library"], ["modalities", "Modalities"], ["week-plan", "Week plan"], ["settings", "Settings"]];
    return '<nav class="program-segments" aria-label="Programs sections">' + links.map(function (link) { return '<button class="segment ' + (active === link[0] ? "is-active" : "") + '" data-action="program-route" data-route="' + link[0] + '" type="button">' + link[1] + "</button>"; }).join("") + "</nav>";
  }

  function renderPrograms() {
    let html = '<div class="screen-inner"><header class="screen-header"><h1>Programs</h1></header>' + renderProgramSegments("programs") + '<section class="section"><div class="section-title"><h2>Programs</h2><button class="text-button add-button" data-action="add-program" type="button">+</button></div><div class="list">';
    state.data.programs.forEach(function (program) {
      html += '<button class="list-row" data-action="open-program" data-program-id="' + escapeHtml(program.id) + '" type="button"><span class="dot-column">' + renderDot("#f0a35a", program.active ? "active" : "empty") + '</span><span class="row-primary"><span class="row-name">' + escapeHtml(program.name) + '</span></span><span class="row-secondary">' + program.blocks.length + ' blocks</span><span class="row-chevron">›</span></button>';
    });
    return html + "</div></section></div>";
  }

  function renderProgramEditor(program) {
    if (!program) return renderEditorTopbar("Programs", "back-programs", "Program", "back-programs") + '<div class="screen-inner"><p class="empty-line">That program is not available.</p></div>';
    let html = renderEditorTopbar("Programs", "back-programs", "Program", "done-program");
    html += '<div class="screen-inner editor-inner"><div class="field-row"><span class="field-label">Name</span><input class="inline-input" data-action="program-name" data-program-id="' + escapeHtml(program.id) + '" value="' + escapeHtml(program.name) + '" aria-label="Program name"></div><div class="field-row switch-row"><span class="field-label">Active</span><input class="toggle-input" data-action="program-active" data-program-id="' + escapeHtml(program.id) + '" type="checkbox"' + (program.active ? " checked" : "") + ' aria-label="Active program"></div><section class="section"><div class="section-title"><h2>Blocks</h2><button class="text-button add-button" data-action="add-block" data-program-id="' + escapeHtml(program.id) + '" type="button">+</button></div><div class="list">';
    program.blocks.forEach(function (block, index) { html += renderProgramBlockRow(block, index); });
    return html + "</div></section></div>";
  }

  function renderProgramBlockRow(block, index) {
    const names = block.exercises.map(function (prescription) { const exercise = exerciseById(state.data, prescription.exerciseId); return exercise ? exercise.name : "Exercise"; }).join(" · ");
    const sets = block.exercises.reduce(function (sum, exercise) { return sum + numberOr(exercise.sets, 0); }, 0);
    return '<button class="list-row" data-action="open-block" data-block-id="' + escapeHtml(block.id) + '" type="button"><span class="dot-column">' + renderDot("#f0a35a", "empty") + '</span><span class="row-primary"><span class="row-name">Block ' + (index + 1) + " · " + escapeHtml(names) + '</span></span><span class="row-secondary">' + sets + ' sets' + (block.superset ? " · superset" : "") + '</span><span class="row-chevron">›</span></button>';
  }

  function renderBlockEditor(program, block) {
    if (!program || !block) return renderEditorTopbar("Program", "back-program-editor", "Block", "back-program-editor") + '<div class="screen-inner"><p class="empty-line">That block is not available.</p></div>';
    const blockIndex = program.blocks.findIndex(function (item) { return item.id === block.id; });
    let html = renderEditorTopbar("Program", "back-program-editor", "Block " + (blockIndex + 1), "done-block");
    html += '<div class="screen-inner editor-inner"><div class="field-row switch-row"><span class="field-label">Superset</span><input class="toggle-input" data-action="toggle-superset" data-program-id="' + escapeHtml(program.id) + '" data-block-id="' + escapeHtml(block.id) + '" type="checkbox"' + (block.superset ? " checked" : "") + ' aria-label="Superset block"></div><div class="block-editor-list">';
    block.exercises.forEach(function (prescription, index) {
      const exercise = exerciseById(state.data, prescription.exerciseId);
      html += '<div class="block-exercise"><div class="block-exercise-name"><strong>' + String.fromCharCode(65 + index) + '</strong><button class="field-value button-value" data-action="open-program-exercise" data-program-id="' + escapeHtml(program.id) + '" data-block-id="' + escapeHtml(block.id) + '" data-exercise-index="' + index + '" type="button"><span>' + escapeHtml(exercise ? exercise.name : "Choose exercise") + '</span><span class="row-chevron">›</span></button></div>';
      html += renderEditorNumberRow("Sets", { id: "program-" + block.id + "-" + index + "-sets", label: "Sets for exercise " + (index + 1), value: prescription.sets, min: 1, step: 1, precision: 0, unit: "sets", owner: "program", field: "sets", programId: program.id, blockId: block.id, exerciseIndex: index });
      html += renderEditorNumberRow("Min reps", { id: "program-" + block.id + "-" + index + "-min", label: "Minimum reps for exercise " + (index + 1), value: prescription.minReps, min: 1, step: 1, precision: 0, unit: "reps", owner: "program", field: "minReps", programId: program.id, blockId: block.id, exerciseIndex: index });
      html += renderEditorNumberRow("Max reps", { id: "program-" + block.id + "-" + index + "-max", label: "Maximum reps for exercise " + (index + 1), value: prescription.maxReps, min: 1, step: 1, precision: 0, unit: "reps", owner: "program", field: "maxReps", programId: program.id, blockId: block.id, exerciseIndex: index }) + "</div>";
    });
    html += '</div>';
    if (block.exercises.length < 2) html += '<button class="text-button" data-action="add-exercise-to-block" data-program-id="' + escapeHtml(program.id) + '" data-block-id="' + escapeHtml(block.id) + '" type="button">+ Add exercise</button>';
    html += '<div class="reorder-actions"><button class="text-button" data-action="move-block" data-program-id="' + escapeHtml(program.id) + '" data-block-id="' + escapeHtml(block.id) + '" data-direction="-1" type="button">Move up</button><button class="text-button" data-action="move-block" data-program-id="' + escapeHtml(program.id) + '" data-block-id="' + escapeHtml(block.id) + '" data-direction="1" type="button">Move down</button></div><div class="editor-bottom-actions"><button class="text-button danger-button" data-action="remove-block" data-program-id="' + escapeHtml(program.id) + '" data-block-id="' + escapeHtml(block.id) + '" type="button">Remove block</button></div></div>';
    return html;
  }

  function renderEditorNumberRow(label, options) {
    return '<div class="editor-number-row"><span class="field-label">' + escapeHtml(label) + '</span>' + renderNumberControl(options) + '</div>';
  }

  function filteredExercises() {
    const filters = state.ui.libraryFilters;
    return state.data.exercises.filter(function (exercise) {
      if (!state.ui.showArchived && exercise.archived) return false;
      if (filters.kind !== "all" && exercise.kind !== filters.kind) return false;
      if (filters.muscle !== "all" && exercise.kind === "strength" && exercise.primaryMuscle !== filters.muscle && exercise.secondaryMuscles.indexOf(filters.muscle) === -1) return false;
      if (filters.muscle !== "all" && exercise.kind === "cardio") return false;
      if (filters.modality !== "all" && (exercise.kind !== "cardio" || exercise.modalityId !== filters.modality)) return false;
      return true;
    }).sort(function (a, b) { return Number(a.archived) - Number(b.archived) || a.name.localeCompare(b.name); });
  }

  function renderExerciseLibrary() {
    const filters = state.ui.libraryFilters;
    let html = '<div class="screen-inner"><header class="screen-title-row screen-header"><h1>Library</h1><button class="text-button" data-action="open-library-overflow" type="button" aria-label="More library options">···</button></header>' + renderProgramSegments("exercise-library") + '<div class="filter-row"><button class="filter-chip ' + (filters.kind !== "all" ? "is-active" : "") + '" data-action="open-filter" data-filter="kind" type="button">Kind: ' + escapeHtml(filters.kind === "all" ? "Any" : titleCase(filters.kind)) + '</button><button class="filter-chip ' + (filters.muscle !== "all" ? "is-active" : "") + '" data-action="open-filter" data-filter="muscle" type="button">Muscle: ' + escapeHtml(filters.muscle === "all" ? "Any" : titleCase(filters.muscle)) + '</button><button class="filter-chip ' + (filters.modality !== "all" ? "is-active" : "") + '" data-action="open-filter" data-filter="modality" type="button">Modality: ' + escapeHtml(filters.modality === "all" ? "Any" : titleCase(filters.modality)) + '</button></div><section class="section"><div class="section-title"><h2>Exercises</h2><button class="text-button add-button" data-action="add-exercise" type="button">+</button></div><div class="list">';
    const exercises = filteredExercises();
    if (!exercises.length) html += '<p class="empty-line">No exercises match.</p>';
    exercises.forEach(function (exercise) {
      const color = exercise.kind === "cardio" ? itemColor(state.data, { itemType: "exercise", itemId: exercise.id }) : "#f0a35a";
      const secondary = exercise.kind === "strength" ? titleCase(exercise.primaryMuscle) : titleCase((modalityById(state.data, exercise.modalityId) || {}).name || "Cardio");
      html += '<button class="list-row ' + (exercise.archived ? "is-archived" : "") + '" data-action="open-exercise" data-exercise-id="' + escapeHtml(exercise.id) + '" type="button"><span class="dot-column">' + renderDot(color, exercise.archived ? "ring" : "filled") + '</span><span class="row-primary"><span class="row-name">' + escapeHtml(exercise.name) + '</span></span><span class="row-secondary">' + escapeHtml(secondary) + '</span><span class="row-chevron">›</span></button>';
    });
    return html + "</div></section></div>";
  }

  function renderModalities() {
    let html = '<div class="screen-inner"><header class="screen-header"><h1>Modalities</h1></header>' + renderProgramSegments("modalities") + '<section class="section"><div class="section-title"><h2>Modalities</h2><button class="text-button add-button" data-action="add-modality" type="button">+</button></div><div class="list">';
    state.data.modalities.forEach(function (modality) {
      html += '<button class="list-row" data-action="open-modality" data-modality-id="' + escapeHtml(modality.id) + '" type="button"><span class="dot-column">' + renderDot(modality.color, "filled") + '</span><span class="row-primary"><span class="row-name">' + escapeHtml(modality.name) + '</span></span><span class="row-chevron">›</span></button>';
    });
    return html + "</div></section></div>";
  }

  function renderWeekPlan() {
    let html = '<div class="screen-inner"><header class="screen-header"><h1>Week plan</h1></header>' + renderProgramSegments("week-plan") + '<section class="section"><div class="list">';
    DAY_KEYS.forEach(function (day, index) {
      const entry = normalizePlanEntry(state.data.weekPlan[day]);
      html += '<button class="list-row" data-action="open-plan" data-date="' + addDays(TODAY, index - weekdayIndex(TODAY)) + '" data-scope="week" type="button"><span class="dot-column">' + renderDot(itemColor(state.data, entry), itemDotMode(state.data, entry)) + '</span><span class="row-primary"><span class="row-name">' + DAY_NAMES[index] + '</span></span><span class="row-secondary">' + escapeHtml(itemName(state.data, entry)) + '</span><span class="row-chevron">›</span></button>';
    });
    return html + "</div></section></div>";
  }

  function renderSettings() {
    let html = '<div class="screen-inner"><header class="screen-header"><h1>Settings</h1></header>' + renderProgramSegments("settings") + '<section class="section"><div class="list"><button class="list-row" data-action="export-json" type="button"><span class="dot-column"></span><span class="row-primary">Export</span><span class="row-chevron">›</span></button><button class="list-row" data-action="import-json" type="button"><span class="dot-column"></span><span class="row-primary">Import</span><span class="row-chevron">›</span></button><button class="list-row" data-action="check-update" type="button"><span class="dot-column"></span><span class="row-primary">Check for update</span><span class="row-secondary">' + escapeHtml(state.data.settings.appVersion || APP_VERSION) + '</span><span class="row-chevron">›</span></button><button class="list-row" data-action="reset-demo" type="button"><span class="dot-column"></span><span class="row-primary danger-button">Reset demo data</span><span class="row-chevron">›</span></button></div></section></div>';
    return html;
  }

  function renderExerciseEditor() {
    const draft = state.ui.editorDraft;
    if (!draft) return renderEditorTopbar("Library", "back-library", "Exercise", "back-library") + '<div class="screen-inner"><p class="empty-line">That exercise is not available.</p></div>';
    let html = renderEditorTopbar("Library", "back-library", state.ui.editorIsNew ? "New exercise" : draft.name, "done-exercise");
    html += '<div class="screen-inner editor-inner"><div class="field-row"><span class="field-label">Name</span><input class="inline-input" data-action="exercise-text" data-field="name" value="' + escapeHtml(draft.name) + '" aria-label="Exercise name"></div><div class="field-row"><span class="field-label">Kind</span><button class="field-value button-value" data-action="open-editor-select" data-control="kind" type="button"><span>' + titleCase(draft.kind) + '</span><span class="row-chevron">›</span></button></div>';
    if (draft.kind === "strength") {
      html += '<div class="field-row"><span class="field-label">Primary muscle</span><button class="field-value button-value" data-action="open-editor-select" data-control="primaryMuscle" type="button"><span>' + escapeHtml(titleCase(draft.primaryMuscle)) + '</span><span class="row-chevron">›</span></button></div><div class="field-row"><span class="field-label">Secondary muscles</span><button class="field-value button-value" data-action="open-editor-select" data-control="secondaryMuscles" type="button"><span>' + escapeHtml(draft.secondaryMuscles.length ? draft.secondaryMuscles.map(titleCase).join(", ") : "None") + '</span><span class="row-chevron">›</span></button></div><div class="field-row"><span class="field-label">Weight step</span>' + renderNumberControl({ id: "exercise-step", label: "Weight step in kilograms", value: draft.stepKg, min: 0.1, step: 0.1, precision: 1, unit: "kg", owner: "exercise-editor", field: "stepKg" }) + "</div>";
    } else {
      const structure = draft.structure.type === "intervals" ? "Intervals · " + draft.structure.rounds + " rounds" : "Steady";
      html += '<div class="field-row"><span class="field-label">Modality</span><button class="field-value button-value" data-action="open-editor-select" data-control="modality" type="button"><span>' + escapeHtml((modalityById(state.data, draft.modalityId) || {}).name || "Choose modality") + '</span><span class="row-chevron">›</span></button></div><div class="field-row"><span class="field-label">Intensity</span><button class="field-value button-value" data-action="open-editor-select" data-control="intensity" type="button"><span>' + titleCase(draft.intensity) + '</span><span class="row-chevron">›</span></button></div><div class="field-row"><span class="field-label">Structure</span><button class="field-value button-value" data-action="open-editor-select" data-control="structure" type="button"><span>' + escapeHtml(structure) + '</span><span class="row-chevron">›</span></button></div>';
      if (draft.structure.type === "intervals") {
        html += renderEditorNumberRow("Warm-up", { id: "structure-warmup", label: "Warm-up seconds", value: draft.structure.warmupSec, min: 0, step: 1, precision: 0, unit: "sec", owner: "exercise-structure", field: "warmupSec" });
        html += renderEditorNumberRow("Work", { id: "structure-work", label: "Work seconds", value: draft.structure.workSec, min: 1, step: 1, precision: 0, unit: "sec", owner: "exercise-structure", field: "workSec" });
        html += renderEditorNumberRow("Rest", { id: "structure-rest", label: "Rest seconds", value: draft.structure.restSec, min: 0, step: 1, precision: 0, unit: "sec", owner: "exercise-structure", field: "restSec" });
        html += renderEditorNumberRow("Rounds", { id: "structure-rounds", label: "Interval rounds", value: draft.structure.rounds, min: 1, step: 1, precision: 0, unit: "rounds", owner: "exercise-structure", field: "rounds" });
      }
      html += '<div class="field-row"><span class="field-label">Fields</span><button class="field-value button-value" data-action="open-editor-select" data-control="fields" type="button"><span>' + escapeHtml(draft.fields.map(fieldLabel).join(", ")) + '</span><span class="row-chevron">›</span></button></div>';
    }
    html += '<div class="field-row"><span class="field-label">Notes</span><input class="inline-input" data-action="exercise-text" data-field="notes" value="' + escapeHtml(draft.notes) + '" aria-label="Exercise notes"></div><div class="editor-bottom-actions"><button class="text-button" data-action="archive-exercise" type="button">' + (draft.archived ? "Restore" : "Archive") + '</button><button class="text-button" data-action="open-merge-exercise" type="button"' + (state.ui.editorIsNew ? " disabled" : "") + '>Merge into…</button>';
    if (state.ui.editorIsNew) html += '<button class="text-button danger-button" data-action="cancel-new-exercise" type="button">Delete</button>';
    else {
      const reason = exerciseDeleteReason(draft.id);
      html += '<button class="text-button danger-button" data-action="delete-exercise" type="button"' + (reason ? " disabled" : "") + '>Delete</button>' + (reason ? '<p class="reason">' + escapeHtml(reason) + '</p>' : "");
    }
    return html + "</div></div>";
  }

  function fieldLabel(field) {
    const labels = { time: "Time", distance: "Distance", avgHr: "Average HR", watts: "Watts", laps: "Laps", poolLength: "Pool length", stroke: "Stroke" };
    return labels[field] || field;
  }

  function exerciseDeleteReason(exerciseId) {
    const sessionUses = state.data.sessions.some(function (session) {
      return (session.setLogs || []).some(function (log) { return log.exerciseId === exerciseId; }) || (session.cardioExerciseSnapshot && session.cardioExerciseSnapshot.exerciseId === exerciseId);
    });
    if (sessionUses) return "Used in a session";
    return "";
  }

  function renderModalityEditor() {
    const draft = state.ui.modalityDraft;
    if (!draft) return renderEditorTopbar("Modalities", "back-modalities", "Modality", "back-modalities") + '<div class="screen-inner"><p class="empty-line">That modality is not available.</p></div>';
    let html = renderEditorTopbar("Modalities", "back-modalities", state.ui.editorIsNew ? "New modality" : draft.name, "done-modality");
    html += '<div class="screen-inner editor-inner"><div class="field-row"><span class="field-label">Name</span><input class="inline-input" data-action="modality-text" value="' + escapeHtml(draft.name) + '" placeholder="Bike, rowing…" aria-label="Modality name"></div><button class="field-row" data-action="open-color-picker" type="button"><span class="field-label">Color</span><span class="field-value"><span>' + renderDot(draft.color, "filled") + '</span><span class="row-chevron">›</span></span></button></div>';
    return html;
  }

  function cardioFieldsForSnapshot(snapshot) {
    return snapshot && Array.isArray(snapshot.fields) ? snapshot.fields : [];
  }

  function cardioMetric(data, session, field) {
    const value = session.cardio && session.cardio[field];
    if (value == null) return "-";
    if (field === "time") return formatSeconds(value);
    if (field === "distance") return formatNumber(value, 1) + " km";
    if (field === "avgHr") return formatNumber(value, 0) + " bpm";
    if (field === "watts") return formatNumber(value, 0) + " W";
    if (field === "laps") return formatNumber(value, 0) + " laps";
    if (field === "poolLength") return formatNumber(value, 0) + " m";
    return String(value);
  }

  function cardioHeadline(data, session) {
    const snapshot = session.cardioExerciseSnapshot;
    const fields = cardioFieldsForSnapshot(snapshot);
    const parts = [];
    if (fields.indexOf("distance") !== -1 && session.cardio.distance != null) parts.push(formatNumber(session.cardio.distance, 1) + " km");
    if (fields.indexOf("laps") !== -1 && session.cardio.laps != null) parts.push(formatNumber(session.cardio.laps, 0) + " laps");
    if (fields.indexOf("time") !== -1 && session.cardio.time != null) parts.push(formatSeconds(session.cardio.time));
    return parts.slice(0, 2).join(" · ") || "Cardio";
  }

  function renderSessionDetail(session) {
    if (!session) return renderEditorTopbar("History", "back-to-history", "Session", "back-to-history") + '<div class="screen-inner"><p class="empty-line">That session is not available.</p></div>';
    if (session.kind === "strength") return renderLiveSession(session, true);
    if (state.ui.detailEditing) {
      if (!state.ui.cardioDraft || state.ui.cardioDraft.sessionId !== session.id) state.ui.cardioDraft = draftFromSession(session, "session-detail");
      return renderCardioForm();
    }
    const display = sessionDisplay(state.data, session);
    let html = renderEditorTopbar("History", "back-to-history", display.name, "edit-cardio-session", "Edit");
    html += '<div class="screen-inner editor-inner"><p class="muted small">' + escapeHtml(formatDateLong(session.date)) + '</p><div class="list">';
    (session.cardioExerciseSnapshot.fields || []).forEach(function (field) { html += '<div class="field-row"><span class="field-label">' + escapeHtml(fieldLabel(field)) + '</span><span class="field-value">' + escapeHtml(cardioMetric(state.data, session, field)) + '</span></div>'; });
    if (session.effort != null) html += '<div class="field-row"><span class="field-label">Effort</span><span class="field-value">' + session.effort + ' / 10</span></div>';
    if (session.note) html += '<div class="field-row"><span class="field-label">Notes</span><span class="field-value">' + escapeHtml(session.note) + '</span></div>';
    html += "</div>" + renderSavedIntervals(session) + "</div>";
    return html;
  }

  function renderSavedIntervals(session) {
    const rows = session.cardio && session.cardio.workRows ? session.cardio.workRows : [];
    if (!rows.length) return "";
    const fields = session.cardioExerciseSnapshot.fields || [];
    const metrics = fields.filter(function (field) { return field === "avgHr" || field === "watts" || field === "distance"; }).slice(0, 2);
    let html = '<section class="history-section"><h2>Work intervals</h2><div class="saved-interval-list">';
    rows.forEach(function (row) {
      html += '<div class="saved-interval-row"><strong>' + row.index + '</strong>' + metrics.map(function (field) { return '<span>' + escapeHtml(fieldLabel(field)) + ' ' + escapeHtml(row[field] == null ? "-" : formatNumber(row[field], field === "avgHr" || field === "watts" ? 0 : 1)) + '</span>'; }).join("") + '</div>';
    });
    return html + "</div></section>";
  }

  function createCardioDraft(exercise, returnRoute) {
    const snapshot = cardioSnapshotFromExercise(state.data, exercise.id);
    const draft = {
      sessionId: null,
      returnRoute: returnRoute || "today",
      date: TODAY,
      exerciseId: exercise.id,
      snapshot: snapshot,
      effort: 6,
      note: "",
      fields: {},
      workRows: []
    };
    snapshot.fields.forEach(function (field) {
      draft.fields[field] = field === "poolLength" ? 25 : (field === "stroke" ? "Freestyle" : "");
    });
    if (snapshot.structure.type === "intervals") draft.workRows = Array.from({ length: snapshot.structure.rounds }, function (_, index) { return { index: index + 1 }; });
    return draft;
  }

  function draftFromSession(session, returnRoute) {
    const snapshot = session.cardioExerciseSnapshot;
    const draft = {
      sessionId: session.id,
      returnRoute: returnRoute || "session-detail",
      date: session.date,
      exerciseId: snapshot.exerciseId,
      snapshot: clone(snapshot),
      effort: session.effort == null ? "" : session.effort,
      note: session.note || "",
      fields: {},
      workRows: clone(session.cardio.workRows || [])
    };
    snapshot.fields.forEach(function (field) {
      const value = session.cardio[field];
      draft.fields[field] = field === "time" && value != null ? value / 60 : (value == null ? "" : value);
    });
    return draft;
  }

  function renderCardioForm() {
    const draft = state.ui.cardioDraft;
    if (!draft || !draft.snapshot) return renderEditorTopbar("Today", "back-to-today", "Cardio", "back-to-today") + '<div class="screen-inner"><p class="empty-line">Choose a cardio exercise first.</p></div>';
    const snapshot = draft.snapshot;
    const backLabel = draft.returnRoute === "session-detail" ? "History" : "Today";
    const backAction = draft.returnRoute === "session-detail" ? "back-to-history" : "back-to-today";
    let html = '<form data-form="cardio"><div class="editor-topbar"><button class="text-button back-button" data-action="' + backAction + '" type="button">‹ ' + backLabel + '</button><h1>' + escapeHtml(snapshot.name) + '</h1><button class="text-button done-button" type="submit">Done</button></div><div class="screen-inner editor-inner"><div class="field-row"><span class="field-label">Date</span><input class="inline-input" name="date" data-action="cardio-date" type="date" value="' + escapeHtml(draft.date) + '" required aria-label="Session date"></div>';
    html += '<div class="field-row"><span class="field-label">Effort</span>' + renderNumberControl({ id: "cardio-effort", label: "Effort from 1 to 10", value: draft.effort, min: 1, step: 1, precision: 0, unit: "/10", owner: "cardio", field: "effort" }) + '</div><div class="cardio-fields">';
    snapshot.fields.forEach(function (field) { html += renderCardioField(draft, field); });
    html += "</div>";
    if (snapshot.structure.type === "intervals") html += renderIntervalForm(draft);
    html += '<div class="field-row"><span class="field-label">Notes</span><input class="inline-input" data-action="cardio-text" name="note" value="' + escapeHtml(draft.note) + '" aria-label="Session notes"></div></div></form>';
    return html;
  }

  function renderCardioField(draft, field) {
    const value = draft.fields[field];
    if (field === "stroke") return '<button class="field-row" data-action="open-cardio-select" data-control="stroke" type="button"><span class="field-label">Stroke</span><span class="field-value"><span>' + escapeHtml(value || "Choose stroke") + '</span><span class="row-chevron">›</span></span></button>';
    const options = {
      time: { unit: "min", step: 0.1, precision: 1, min: 0 },
      distance: { unit: "km", step: 0.1, precision: 1, min: 0 },
      avgHr: { unit: "bpm", step: 1, precision: 0, min: 0 },
      watts: { unit: "W", step: 1, precision: 0, min: 0 },
      laps: { unit: "laps", step: 1, precision: 0, min: 0 },
      poolLength: { unit: "m", step: 1, precision: 0, min: 1 }
    }[field];
    return '<div class="field-row"><span class="field-label">' + escapeHtml(fieldLabel(field)) + '</span>' + renderNumberControl({ id: "cardio-" + field, label: fieldLabel(field), value: value, min: options.min, step: options.step, precision: options.precision, unit: options.unit, owner: "cardio-field", field: field }) + '</div>';
  }

  function renderIntervalForm(draft) {
    const fields = draft.snapshot.fields.filter(function (field) { return field === "avgHr" || field === "watts" || field === "distance"; }).slice(0, 2);
    if (!fields.length) return "";
    let html = '<section class="history-section"><h2>Work intervals</h2><div class="interval-form-list">';
    draft.workRows.forEach(function (row, index) {
      html += '<div class="interval-form-row" data-interval-row="' + index + '"><strong>' + row.index + '</strong>' + fields.map(function (field) {
        const options = field === "distance" ? { unit: "km", step: 0.1, precision: 1 } : (field === "avgHr" ? { unit: "bpm", step: 1, precision: 0 } : { unit: "W", step: 1, precision: 0 });
        return renderNumberControl({ id: "interval-" + index + "-" + field, label: fieldLabel(field) + " for round " + row.index, value: row[field] == null ? "" : row[field], min: 0, step: options.step, precision: options.precision, unit: options.unit, owner: "cardio-interval", field: field, index: index });
      }).join("") + "</div>";
    });
    return html + "</div></section>";
  }

  function renderModal() {
    const modal = state.ui.modal;
    if (!modal) return "";
    if (modal.type === "plan") return renderPlanSheet(modal);
    if (modal.type === "choice") return renderChoiceSheet(modal);
    if (modal.type === "filter") return renderFilterSheet(modal);
    if (modal.type === "overflow") return renderOverflowSheet();
    if (modal.type === "color") return renderColorSheet(modal);
    if (modal.type === "merge") return renderMergeSheet(modal);
    if (modal.type === "history-exercise") return renderHistoryExerciseSheet();
    if (modal.type === "summary") return renderSummarySheet(modal);
    if (modal.type === "health-import") return renderHealthImportSheet(modal);
    if (modal.type === "import") return renderImportSheet();
    return "";
  }

  function sheetFrame(title, body, footer) {
    return '<div class="modal-layer" data-action="close-modal" role="presentation"><section class="sheet" role="dialog" aria-modal="true" aria-label="' + escapeHtml(title) + '"><div class="sheet-head"><h2>' + escapeHtml(title) + '</h2></div><div class="sheet-body">' + body + (footer || "") + "</div></section></div>";
  }

  function sheetChoice(label, value, selected, color) {
    return '<button class="sheet-choice" data-action="choose-option" data-value="' + escapeHtml(value) + '" type="button"><span class="row-primary">' + (color ? renderDot(color, "filled") : "") + '<span>' + escapeHtml(label) + '</span></span><span class="checkmark" aria-hidden="true">' + (selected ? "✓" : "") + '</span></button>';
  }

  function renderPlanSheet(modal) {
    const current = normalizePlanEntry(modal.entry);
    let body = "";
    if (modal.scope === "override" && state.data.overrides[modal.date]) body += '<div class="sheet-list"><button class="sheet-choice" data-action="restore-plan" data-date="' + escapeHtml(modal.date) + '" type="button"><span class="row-primary">Use template</span><span class="checkmark">' + (current.itemType === normalizePlanEntry(state.data.weekPlan[DAY_KEYS[weekdayIndex(modal.date)]]).itemType && current.itemId === normalizePlanEntry(state.data.weekPlan[DAY_KEYS[weekdayIndex(modal.date)]]).itemId ? "✓" : "") + '</span></button></div>';
    body += '<div class="sheet-group"><div class="sheet-group-title">Programs</div><div class="sheet-list">' + state.data.programs.map(function (program) { return sheetChoice(program.name, "program:" + program.id, current.itemType === "program" && current.itemId === program.id, "#f0a35a"); }).join("") + '</div></div>';
    body += '<div class="sheet-group"><div class="sheet-group-title">Cardio exercises</div><div class="sheet-list">' + state.data.exercises.filter(function (exercise) { return exercise.kind === "cardio" && !exercise.archived; }).map(function (exercise) { return sheetChoice(exercise.name, "exercise:" + exercise.id, current.itemType === "exercise" && current.itemId === exercise.id, itemColor(state.data, { itemType: "exercise", itemId: exercise.id })); }).join("") + '</div></div>';
    body += '<div class="sheet-group"><div class="sheet-group-title">Rest</div><div class="sheet-list">' + sheetChoice("Rest", "rest:", current.itemType === "rest", "#9a9a9a") + "</div></div>";
    return sheetFrame(modal.scope === "week" ? "Week plan" : formatDateLong(modal.date), body);
  }

  function choiceOptions(modal) {
    switch (modal.control) {
      case "kind": return [{ value: "strength", label: "Strength" }, { value: "cardio", label: "Cardio" }];
      case "program-exercise": return state.data.exercises.filter(function (exercise) { return exercise.kind === "strength" && !exercise.archived; }).map(function (exercise) { return { value: exercise.id, label: exercise.name }; });
      case "primaryMuscle": return MUSCLE_GROUPS.map(function (muscle) { return { value: muscle, label: titleCase(muscle) }; });
      case "secondaryMuscles": return MUSCLE_GROUPS.map(function (muscle) { return { value: muscle, label: titleCase(muscle) }; });
      case "modality": return state.data.modalities.map(function (modality) { return { value: modality.id, label: modality.name, color: modality.color }; });
      case "intensity": return INTENSITIES.map(function (intensity) { return { value: intensity, label: titleCase(intensity) }; });
      case "structure": return [{ value: "steady", label: "Steady" }, { value: "intervals", label: "Intervals" }];
      case "fields": return CARDIO_FIELDS.map(function (field) { return { value: field, label: fieldLabel(field) }; });
      case "stroke": return [{ value: "Freestyle", label: "Freestyle" }, { value: "Mixed", label: "Mixed" }, { value: "Breaststroke", label: "Breaststroke" }, { value: "Backstroke", label: "Backstroke" }];
      default: return [];
    }
  }

  function editorSelectedValue(control) {
    const draft = state.ui.editorDraft;
    if (!draft) return null;
    if (control === "structure") return draft.structure.type;
    return draft[control];
  }

  function renderChoiceSheet(modal) {
    const options = choiceOptions(modal);
    const selected = modal.selected == null ? editorSelectedValue(modal.control) : modal.selected;
    const multi = modal.control === "secondaryMuscles" || modal.control === "fields";
    const values = multi ? (Array.isArray(selected) ? selected : []) : [selected];
    const body = '<div class="sheet-list">' + options.map(function (option) { return sheetChoice(option.label, option.value, values.indexOf(option.value) !== -1, option.color); }).join("") + "</div>";
    const footer = multi ? '<div class="sheet-foot"><button class="text-button" data-action="sheet-done" type="button">Done</button></div>' : "";
    return sheetFrame(titleCase(modal.control), body, footer);
  }

  function filterOptions(filter) {
    if (filter === "kind") return [{ value: "all", label: "Any kind" }, { value: "strength", label: "Strength" }, { value: "cardio", label: "Cardio" }];
    if (filter === "muscle") return [{ value: "all", label: "Any muscle" }].concat(MUSCLE_GROUPS.map(function (muscle) { return { value: muscle, label: titleCase(muscle) }; }));
    return [{ value: "all", label: "Any modality" }].concat(state.data.modalities.map(function (modality) { return { value: modality.id, label: modality.name, color: modality.color }; }));
  }

  function renderFilterSheet(modal) {
    const current = state.ui.libraryFilters[modal.filter];
    const body = '<div class="sheet-list">' + filterOptions(modal.filter).map(function (option) { return sheetChoice(option.label, option.value, current === option.value, option.color); }).join("") + "</div>";
    return sheetFrame(titleCase(modal.filter), body);
  }

  function renderOverflowSheet() {
    const body = '<div class="sheet-list"><button class="sheet-choice" data-action="toggle-archived" type="button"><span class="row-primary">Show archived</span><span class="checkmark">' + (state.ui.showArchived ? "✓" : "") + '</span></button></div>';
    return sheetFrame("Library options", body);
  }

  function renderColorSheet(modal) {
    const body = '<div class="color-grid">' + COLOR_PRESETS.map(function (color) { return '<button class="color-choice ' + (modal.color === color ? "is-selected" : "") + '" style="--choice-color:' + color + '" data-action="choose-color" data-color="' + color + '" type="button" aria-label="Choose color ' + color + '"></button>'; }).join("") + "</div>";
    return sheetFrame("Color", body);
  }

  function renderMergeSheet(modal) {
    const loser = exerciseById(state.data, modal.loserId);
    const options = state.data.exercises.filter(function (exercise) { return exercise.id !== modal.loserId && (!loser || exercise.kind === loser.kind); });
    const body = '<p class="muted small">Keep one exercise. Its history and program references will receive the selected name.</p><div class="sheet-list">' + options.map(function (exercise) { return '<button class="sheet-choice" data-action="choose-merge-survivor" data-exercise-id="' + escapeHtml(exercise.id) + '" type="button"><span class="row-primary">' + escapeHtml(exercise.name) + '</span><span class="checkmark">' + (modal.survivorId === exercise.id ? "✓" : "") + '</span></button>'; }).join("") + '</div><div class="sheet-foot"><button class="text-button" data-action="confirm-merge" type="button"' + (modal.survivorId ? "" : " disabled") + '>Merge' + (loser ? " " + escapeHtml(loser.name) : "") + '</button></div>';
    return sheetFrame("Merge into…", body);
  }

  function renderHistoryExerciseSheet() {
    const exercises = state.data.exercises.filter(function (exercise) { return exercise.kind === "strength"; });
    const body = '<div class="sheet-list">' + exercises.map(function (exercise) { return sheetChoice(exercise.name, exercise.id, exercise.id === state.ui.selectedExerciseId, "#f0a35a"); }).join("") + "</div>";
    return sheetFrame("Exercise", body);
  }

  function renderSummarySheet(modal) {
    const bests = modal.bests.length ? modal.bests.join(", ") : "None";
    const body = '<div class="sheet-list"><div class="field-row"><span class="field-label">Sets done</span><span class="field-value">' + modal.stats.done + '</span></div><div class="field-row"><span class="field-label">Volume</span><span class="field-value">' + formatNumber(modal.stats.volume, 0) + ' kg</span></div><div class="field-row"><span class="field-label">New bests</span><span class="field-value">' + escapeHtml(bests) + '</span></div></div><div class="sheet-foot"><button class="text-button" data-action="close-summary" type="button">Done</button></div>';
    return sheetFrame("Session saved", body);
  }

  function renderHealthImportSheet(modal) {
    const draft = modal.draft;
    const body = '<div class="sheet-list"><div class="field-row"><span class="field-label">Source</span><span class="field-value">' + escapeHtml(draft.source) + '</span></div><div class="field-row"><span class="field-label">Distance</span><span class="field-value">' + formatNumber(draft.distance, 1) + ' km</span></div><div class="field-row"><span class="field-label">Time</span><span class="field-value">' + escapeHtml(formatSeconds(draft.time)) + '</span></div></div><div class="sheet-foot"><button class="text-button" data-action="confirm-health-draft" type="button">Save draft</button></div>';
    return sheetFrame("Health Connect", body);
  }

  function renderImportSheet() {
    const body = '<div class="sheet-list"><button class="sheet-choice" data-action="import-merge" type="button"><span class="row-primary">Merge</span><span class="row-chevron">›</span></button><button class="sheet-choice" data-action="import-replace" type="button"><span class="row-primary">Replace</span><span class="row-chevron">›</span></button></div>';
    return sheetFrame("Import JSON", body);
  }

  function renderRoute() {
    switch (state.ui.route) {
      case "history": return renderHistory();
      case "programs": return renderPrograms();
      case "program-editor": return renderProgramEditor(getProgram(state.data, state.ui.selectedProgramId));
      case "block-editor": return renderBlockEditor(getProgram(state.data, state.ui.selectedProgramId), getProgram(state.data, state.ui.selectedProgramId) && getProgram(state.data, state.ui.selectedProgramId).blocks.find(function (block) { return block.id === state.ui.selectedBlockId; }));
      case "exercise-library": return renderExerciseLibrary();
      case "exercise-editor": return renderExerciseEditor();
      case "modalities": return renderModalityListRoute();
      case "modality-editor": return renderModalityEditor();
      case "week-plan": return renderWeekPlan();
      case "settings": return renderSettings();
      case "session-detail": return renderSessionDetail(getSession(state.data, state.ui.selectedSessionId));
      case "cardio-form": return renderCardioForm();
      case "timer": return renderTimer();
      default: return renderToday();
    }
  }

  function renderModalityListRoute() {
    return renderModalities();
  }

  function renderApp() {
    const screen = document.getElementById("screen");
    if (screen) screen.innerHTML = renderRoute();
    const nav = document.getElementById("bottom-nav");
    if (nav) {
      nav.hidden = FULL_SCREEN_ROUTES.indexOf(state.ui.route) !== -1;
      Array.from(nav.querySelectorAll(".nav-item")).forEach(function (item) { item.classList.toggle("is-active", item.dataset.tab === state.ui.activeTab); });
    }
    const modalRoot = document.getElementById("modal-root");
    if (modalRoot) modalRoot.innerHTML = renderModal();
    const toast = document.getElementById("toast");
    if (toast) {
      toast.textContent = state.ui.toast ? state.ui.toast.message : "";
      toast.className = "toast" + (state.ui.toast ? " is-visible " + state.ui.toast.kind : "");
    }
  }

  function commit(mutator, message) {
    mutator(state.data);
    saveState();
    renderApp();
    if (message) showToast(message);
  }

  function showToast(message, kind) {
    state.ui.toast = { message: message, kind: kind || "" };
    renderApp();
    if (toastTimer) window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(function () { state.ui.toast = null; renderApp(); }, 2600);
  }

  function resetUi(route) {
    const next = emptyUi();
    next.route = route || "today";
    next.activeTab = route === "history" ? "history" : (route === "programs" || route === "settings" || route === "exercise-library" || route === "modalities" || route === "week-plan" ? "programs" : "today");
    return next;
  }

  function startPlannedItem() {
    const plan = plannedItem(state.data, TODAY);
    if (plan.entry.itemType === "rest" || !plan.item) {
      showToast("Today is a rest day.");
      return;
    }
    if (plan.entry.itemType === "program") startGymSession(TODAY, plan.item.id);
    else if (plan.item.structure.type === "intervals") startTimer(plan.item.id, TODAY);
    else {
      state.ui.cardioDraft = createCardioDraft(plan.item, "today");
      state.ui.route = "cardio-form";
      state.ui.activeTab = "today";
      renderApp();
    }
  }

  function startGymSession(date, programId) {
    const existing = unfinishedSession(state.data);
    if (existing) {
      state.ui.route = existing.kind === "strength" ? "today" : "cardio-form";
      state.ui.activeTab = "today";
      if (existing.kind === "cardio") state.ui.cardioDraft = draftFromSession(existing, "today");
      renderApp();
      return;
    }
    const program = getProgram(state.data, programId) || activeProgram(state.data) || makeProgram();
    const snapshot = makeSnapshot(program, state.data);
    const id = uid("gym");
    state.data.sessions.push({
      id: id,
      date: date,
      dateTime: dateTimeFor(date, "18:00:00"),
      kind: "strength",
      programSnapshot: snapshot,
      cardioExerciseSnapshot: null,
      setLogs: buildGymLogs(state.data, snapshot, date, 0, id, false),
      cardio: {},
      note: "",
      effort: null,
      finished: false,
      source: "manual",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
    state.ui.expandedBlockId = null;
    state.ui.route = "today";
    state.ui.activeTab = "today";
    saveState();
    renderApp();
  }

  function toggleSet(sessionId, logId) {
    const session = getSession(state.data, sessionId);
    const log = findLog(session, logId);
    if (!session || !log || session.finished) return;
    const wasDone = log.status === "done";
    log.status = wasDone ? "pending" : "done";
    session.updatedAt = new Date().toISOString();
    saveState();
    renderApp();
    if (!wasDone) focusNextUnlogged(session, log.id);
  }

  function focusNextUnlogged(session, afterLogId) {
    const ordered = orderedSessionLogs(session);
    const currentIndex = ordered.findIndex(function (item) { return item.log.id === afterLogId; });
    const next = ordered.slice(currentIndex + 1).concat(ordered.slice(0, currentIndex + 1)).find(function (item) { return item.log.status === "pending"; });
    if (!next) return;
    state.ui.expandedBlockId = next.block.id;
    renderApp();
    window.requestAnimationFrame(function () {
      const input = document.getElementById("set-" + next.log.id + "-weight");
      if (!input) return;
      input.focus({ preventScroll: true });
      input.select();
      if (typeof input.scrollIntoView === "function") input.scrollIntoView({ behavior: "smooth", block: "center" });
    });
  }

  function updateNumberFromInput(input) {
    const owner = input.dataset.owner;
    const field = input.dataset.field;
    const value = finiteOrNull(input.value);
    if (owner === "set") {
      const session = getSession(state.data, input.dataset.sessionId);
      const log = findLog(session, input.dataset.logId);
      if (!session || !log) return;
      log[field] = value;
      session.updatedAt = new Date().toISOString();
      saveState();
      updateDeltaInDom(session, log, field, input.id);
      return;
    }
    if (owner === "bodyweight") {
      state.ui.bodyweightDraft = value == null ? "" : value;
      return;
    }
    if (owner === "program") {
      const program = getProgram(state.data, input.dataset.programId);
      const block = program && program.blocks.find(function (item) { return item.id === input.dataset.blockId; });
      const prescription = block && block.exercises[Number(input.dataset.exerciseIndex)];
      if (prescription) { prescription[field] = Math.max(1, Math.round(numberOr(value, prescription[field]))); program.updatedAt = new Date().toISOString(); saveState(); }
      return;
    }
    if (owner === "exercise-editor") {
      if (state.ui.editorDraft) state.ui.editorDraft[field] = Math.max(0.1, numberOr(value, state.ui.editorDraft[field]));
      return;
    }
    if (owner === "exercise-structure") {
      if (state.ui.editorDraft && state.ui.editorDraft.structure) state.ui.editorDraft.structure[field] = Math.max(field === "workSec" || field === "rounds" ? 1 : 0, Math.round(numberOr(value, state.ui.editorDraft.structure[field])));
      return;
    }
    if (owner === "cardio") {
      if (state.ui.cardioDraft) state.ui.cardioDraft[field] = value == null ? "" : value;
      return;
    }
    if (owner === "cardio-field") {
      if (state.ui.cardioDraft) state.ui.cardioDraft.fields[field] = value == null ? "" : value;
      return;
    }
    if (owner === "cardio-interval") {
      const row = state.ui.cardioDraft && state.ui.cardioDraft.workRows[Number(input.dataset.index)];
      if (row) row[field] = value == null ? "" : value;
    }
  }

  function updateDeltaInDom(session, log, field, inputId) {
    const delta = deltaFor(state.data, session, log, field);
    const element = document.getElementById("delta-" + inputId);
    if (element) element.textContent = delta == null ? "" : "+" + formatNumber(delta, field === "reps" ? 0 : 1);
  }

  function stepInput(button) {
    const input = document.getElementById(button.dataset.targetId);
    if (!input) return;
    const step = numberOr(input.step, 1);
    const current = numberOr(input.value, 0);
    const stepText = String(step);
    const precision = stepText.indexOf(".") === -1 ? 0 : stepText.split(".")[1].length;
    const next = Math.max(numberOr(input.min, 0), roundTo(current + Number(button.dataset.direction) * step, precision));
    input.value = String(next);
    updateNumberFromInput(input);
    if (input.dataset.owner === "set") {
      const session = getSession(state.data, input.dataset.sessionId);
      const log = findLog(session, input.dataset.logId);
      if (session && log) updateDeltaInDom(session, log, input.dataset.field, input.id);
    }
  }

  function finishSession(sessionId) {
    const session = getSession(state.data, sessionId);
    if (!session || session.kind !== "strength") return;
    session.setLogs.forEach(function (log) { if (log.status === "pending") log.status = "skipped"; });
    session.finished = true;
    session.updatedAt = new Date().toISOString();
    const stats = sessionStats(session);
    const bests = newBestNames(state.data, session);
    saveState();
    state.ui.modal = { type: "summary", stats: stats, bests: bests };
    renderApp();
  }

  function addBodyweight() {
    const value = finiteOrNull(state.ui.bodyweightDraft);
    if (value == null || value < 0) {
      showToast("Enter bodyweight first.", "error");
      return;
    }
    state.ui.bodyweightDraft = "";
    commit(function (data) {
      const existing = data.bodyweights.find(function (entry) { return entry.date === TODAY; });
      if (existing) existing.kg = value;
      else data.bodyweights.push({ id: uid("bw"), date: TODAY, kg: value });
    }, "Bodyweight added.");
  }

  function openPlan(date, scope) {
    const plan = plannedItem(state.data, date);
    state.ui.modal = { type: "plan", date: date, scope: scope || "override", entry: clone(plan.entry) };
    renderApp();
  }

  function encodePlanChoice(value) {
    const parts = String(value || "rest:").split(":");
    return normalizePlanEntry({ itemType: parts[0] === "program" ? "program" : (parts[0] === "exercise" ? "exercise" : "rest"), itemId: parts[1] || null });
  }

  function choosePlan(value) {
    const modal = state.ui.modal;
    if (!modal || modal.type !== "plan") return;
    const entry = encodePlanChoice(value);
    if (modal.scope === "week") state.data.weekPlan[DAY_KEYS[weekdayIndex(modal.date)]] = entry;
    else state.data.overrides[modal.date] = entry;
    state.ui.modal = null;
    commit(function () {}, "Plan changed.");
  }

  function restorePlan(date) {
    delete state.data.overrides[date];
    state.ui.modal = null;
    commit(function () {}, "Template restored.");
  }

  function swapPlanDates(sourceDate, targetDate) {
    if (!sourceDate || !targetDate || sourceDate === targetDate) return;
    const source = plannedItem(state.data, sourceDate).entry;
    const target = plannedItem(state.data, targetDate).entry;
    state.data.overrides[sourceDate] = clone(target);
    state.data.overrides[targetDate] = clone(source);
    commit(function () {}, "Days swapped.");
  }

  function updateProgram(programId, updater, message) {
    const program = getProgram(state.data, programId);
    if (!program) return;
    updater(program);
    program.updatedAt = new Date().toISOString();
    commit(function () {}, message);
  }

  function addProgram() {
    const program = { id: uid("program"), name: "New program", active: false, blocks: [], updatedAt: new Date().toISOString() };
    state.data.programs.push(program);
    state.ui.selectedProgramId = program.id;
    state.ui.route = "program-editor";
    state.ui.activeTab = "programs";
    saveState();
    renderApp();
  }

  function activateProgram(programId) {
    state.data.programs.forEach(function (program) { program.active = program.id === programId; });
    commit(function () {}, "Program selected.");
  }

  function moveBlock(programId, blockId, direction) {
    updateProgram(programId, function (program) {
      const index = program.blocks.findIndex(function (block) { return block.id === blockId; });
      const next = index + direction;
      if (index < 0 || next < 0 || next >= program.blocks.length) return;
      const moved = program.blocks.splice(index, 1)[0];
      program.blocks.splice(next, 0, moved);
    }, "Program order saved.");
  }

  function removeBlock(programId, blockId) {
    updateProgram(programId, function (program) { program.blocks = program.blocks.filter(function (block) { return block.id !== blockId; }); }, "Block removed.");
    state.ui.route = "program-editor";
    renderApp();
  }

  function addBlock(programId) {
    updateProgram(programId, function (program) {
      const first = state.data.exercises.find(function (exercise) { return exercise.kind === "strength"; });
      if (first) program.blocks.push({ id: uid("block"), superset: false, exercises: [{ slotId: uid("slot"), exerciseId: first.id, sets: 3, minReps: 8, maxReps: 12 }] });
    }, "Block added.");
  }

  function addExerciseToBlock(programId, blockId) {
    updateProgram(programId, function (program) {
      const block = program.blocks.find(function (item) { return item.id === blockId; });
      const first = state.data.exercises.find(function (exercise) { return exercise.kind === "strength"; });
      if (!block || !first || block.exercises.length >= 2) return;
      block.exercises.push({ slotId: uid("slot"), exerciseId: first.id, sets: 3, minReps: 8, maxReps: 12 });
    }, "Exercise added.");
  }

  function removeExerciseFromBlock(programId, blockId, exerciseIndex) {
    updateProgram(programId, function (program) {
      const block = program.blocks.find(function (item) { return item.id === blockId; });
      if (!block || block.exercises.length < 2) return;
      block.exercises.splice(exerciseIndex, 1);
      block.superset = false;
    }, "Exercise removed.");
  }

  function toggleSuperset(programId, blockId, checked) {
    const program = getProgram(state.data, programId);
    const block = program && program.blocks.find(function (item) { return item.id === blockId; });
    if (!block) return;
    block.superset = Boolean(checked && block.exercises.length === 2);
    saveState();
    renderApp();
  }

  function addExerciseDraft() {
    state.ui.editorIsNew = true;
    state.ui.editorDraft = normalizeExercise({ id: uid("exercise"), name: "", kind: "strength", primaryMuscle: "chest", secondaryMuscles: [], stepKg: 2.5, notes: "", builtin: false }, 0);
    state.ui.route = "exercise-editor";
    state.ui.activeTab = "programs";
    renderApp();
  }

  function openExerciseEditor(exerciseId) {
    const exercise = exerciseById(state.data, exerciseId);
    if (!exercise) return;
    state.ui.editorIsNew = false;
    state.ui.editorDraft = clone(exercise);
    state.ui.route = "exercise-editor";
    state.ui.activeTab = "programs";
    renderApp();
  }

  function saveExerciseEditor() {
    const draft = state.ui.editorDraft;
    if (!draft || !String(draft.name || "").trim()) {
      showToast("Name the exercise first.", "error");
      return;
    }
    const normalized = normalizeExercise(draft, 0);
    if (state.ui.editorIsNew) state.data.exercises.push(normalized);
    else {
      const index = state.data.exercises.findIndex(function (exercise) { return exercise.id === normalized.id; });
      if (index !== -1) state.data.exercises[index] = normalized;
    }
    state.ui.editorDraft = null;
    state.ui.editorIsNew = false;
    state.ui.route = "exercise-library";
    state.ui.activeTab = "programs";
    saveState();
    renderApp();
    showToast("Exercise saved.");
  }

  function archiveExercise() {
    if (!state.ui.editorDraft) return;
    state.ui.editorDraft.archived = !state.ui.editorDraft.archived;
    renderApp();
  }

  function deleteExercise(exerciseId) {
    if (exerciseDeleteReason(exerciseId)) return;
    if (!window.confirm("Delete this exercise?")) return;
    state.data.programs.forEach(function (program) {
      program.blocks.forEach(function (block) {
        block.exercises = block.exercises.filter(function (exercise) { return exercise.exerciseId !== exerciseId; });
        if (block.exercises.length < 2) block.superset = false;
      });
    });
    state.data.exercises = state.data.exercises.filter(function (exercise) { return exercise.id !== exerciseId; });
    state.ui.editorDraft = null;
    state.ui.route = "exercise-library";
    commit(function () {}, "Exercise deleted.");
  }

  function replaceExerciseInSnapshot(snapshot, loserId, survivor) {
    if (!snapshot || !Array.isArray(snapshot.blocks)) return;
    snapshot.blocks.forEach(function (block) {
      block.exercises.forEach(function (exercise) {
        if (exercise.exerciseId === loserId) {
          exercise.exerciseId = survivor.id;
          exercise.exerciseName = survivor.name;
          exercise.primaryMuscle = survivor.primaryMuscle || exercise.primaryMuscle;
          exercise.secondaryMuscles = survivor.secondaryMuscles ? survivor.secondaryMuscles.slice() : exercise.secondaryMuscles;
          exercise.stepKg = survivor.stepKg || exercise.stepKg;
          exercise.notes = survivor.notes || exercise.notes;
        }
      });
    });
  }

  function mergeExercises(loserId, survivorId) {
    const loser = exerciseById(state.data, loserId);
    const survivor = exerciseById(state.data, survivorId);
    if (!loser || !survivor || loser.id === survivor.id || loser.kind !== survivor.kind) return;
    state.data.programs.forEach(function (program) {
      program.blocks.forEach(function (block) {
        block.exercises.forEach(function (exercise) { if (exercise.exerciseId === loserId) exercise.exerciseId = survivorId; });
      });
    });
    Object.keys(state.data.weekPlan).forEach(function (day) {
      const entry = state.data.weekPlan[day];
      if (entry.itemType === "exercise" && entry.itemId === loserId) entry.itemId = survivorId;
    });
    Object.keys(state.data.overrides).forEach(function (date) {
      const entry = state.data.overrides[date];
      if (entry.itemType === "exercise" && entry.itemId === loserId) entry.itemId = survivorId;
    });
    state.data.sessions.forEach(function (session) {
      (session.setLogs || []).forEach(function (log) { if (log.exerciseId === loserId) log.exerciseId = survivorId; });
      replaceExerciseInSnapshot(session.programSnapshot, loserId, survivor);
      if (session.cardioExerciseSnapshot && session.cardioExerciseSnapshot.exerciseId === loserId) {
        session.cardioExerciseSnapshot = cardioSnapshotFromExercise(state.data, survivorId) || session.cardioExerciseSnapshot;
      }
    });
    state.data.exercises = state.data.exercises.filter(function (exercise) { return exercise.id !== loserId; });
    state.ui.editorDraft = null;
    state.ui.modal = null;
    state.ui.route = "exercise-library";
    state.ui.activeTab = "programs";
    commit(function () {}, "Exercise history merged.");
  }

  function addModalityDraft() {
    state.ui.editorIsNew = true;
    state.ui.modalityDraft = { id: uid("modality"), name: "", color: COLOR_PRESETS[0] };
    state.ui.route = "modality-editor";
    state.ui.activeTab = "programs";
    renderApp();
  }

  function openModalityEditor(modalityId) {
    const modality = modalityById(state.data, modalityId);
    if (!modality) return;
    state.ui.editorIsNew = false;
    state.ui.modalityDraft = clone(modality);
    state.ui.route = "modality-editor";
    state.ui.activeTab = "programs";
    renderApp();
  }

  function saveModalityEditor() {
    const draft = state.ui.modalityDraft;
    if (!draft || !String(draft.name || "").trim()) {
      showToast("Name the modality first.", "error");
      return;
    }
    const normalized = normalizeModality(draft, 0);
    if (state.ui.editorIsNew) state.data.modalities.push(normalized);
    else {
      const index = state.data.modalities.findIndex(function (modality) { return modality.id === normalized.id; });
      if (index !== -1) state.data.modalities[index] = normalized;
    }
    state.ui.modalityDraft = null;
    state.ui.editorIsNew = false;
    state.ui.route = "modalities";
    state.ui.activeTab = "programs";
    saveState();
    renderApp();
    showToast("Modality saved.");
  }

  function openChoice(control) {
    const multi = control === "secondaryMuscles" || control === "fields";
    state.ui.modal = { type: "choice", control: control, multi: multi, selected: multi ? clone(editorSelectedValue(control) || []) : editorSelectedValue(control) };
    renderApp();
  }

  function applyChoice(control, value) {
    const draft = state.ui.editorDraft;
    if (!draft) return;
    if (control === "kind") {
      draft.kind = value;
      if (value === "strength") {
        delete draft.modalityId; delete draft.intensity; delete draft.structure; delete draft.fields;
        draft.primaryMuscle = draft.primaryMuscle || "chest"; draft.secondaryMuscles = draft.secondaryMuscles || []; draft.stepKg = draft.stepKg || 2.5;
      } else {
        delete draft.primaryMuscle; delete draft.secondaryMuscles; delete draft.stepKg;
        draft.modalityId = draft.modalityId || (state.data.modalities[0] && state.data.modalities[0].id) || "bike";
        draft.intensity = draft.intensity || "moderate"; draft.structure = draft.structure || { type: "steady" }; draft.fields = draft.fields || defaultCardioFields(draft.modalityId);
      }
    } else if (control === "structure") {
      draft.structure = value === "intervals" ? Object.assign({ type: "intervals" }, draft.structure && draft.structure.type === "intervals" ? draft.structure : { warmupSec: 0, workSec: 30, restSec: 30, rounds: 4 }) : { type: "steady" };
    } else draft[control] = value;
  }

  function chooseColor(color) {
    if (state.ui.modalityDraft) state.ui.modalityDraft.color = color;
    state.ui.modal = null;
    renderApp();
  }

  function updateFilter(filter, value) {
    state.ui.libraryFilters[filter] = value;
    state.ui.modal = null;
    renderApp();
  }

  function createTimerPhases(snapshot) {
    const structure = snapshot.structure;
    if (!structure || structure.type !== "intervals") return [];
    const phases = [];
    if (structure.warmupSec > 0) phases.push({ name: "Warm-up", kind: "warmup", seconds: structure.warmupSec, round: 0 });
    for (let round = 1; round <= structure.rounds; round += 1) {
      phases.push({ name: "Work", kind: "work", seconds: structure.workSec, round: round });
      if (round < structure.rounds && structure.restSec > 0) phases.push({ name: "Rest", kind: "rest", seconds: structure.restSec, round: round });
    }
    return phases;
  }

  function startTimer(exerciseId, date) {
    const exercise = exerciseById(state.data, exerciseId);
    const snapshot = exercise && cardioSnapshotFromExercise(state.data, exerciseId);
    if (!snapshot || snapshot.structure.type !== "intervals") {
      if (exercise) {
        state.ui.cardioDraft = createCardioDraft(exercise, "today");
        state.ui.route = "cardio-form";
        renderApp();
      }
      return;
    }
    stopTimerLoop();
    const phases = createTimerPhases(snapshot);
    state.ui.timer = { exerciseId: exerciseId, snapshot: snapshot, date: date || TODAY, phases: phases, phaseIndex: 0, remaining: phases[0] ? phases[0].seconds : 0, elapsed: 0, running: true, speed: 1, lastWall: performance.now() };
    state.ui.modal = null;
    state.ui.route = "timer";
    state.ui.activeTab = "today";
    renderApp();
    primeAudio();
    requestWakeLock();
    startTimerLoop();
  }

  function startTimerLoop() {
    stopTimerLoop();
    timerHandle = window.setInterval(timerTick, 50);
  }

  function stopTimerLoop() {
    if (timerHandle) window.clearInterval(timerHandle);
    timerHandle = null;
  }

  function requestWakeLock() {
    if (typeof navigator === "undefined" || !navigator.wakeLock || !navigator.wakeLock.request) return;
    navigator.wakeLock.request("screen").then(function (lock) { wakeLock = lock; }).catch(function () { wakeLock = null; });
  }

  function releaseWakeLock() {
    if (!wakeLock || !wakeLock.release) return;
    wakeLock.release().catch(function () {});
    wakeLock = null;
  }

  function primeAudio() {
    try {
      if (!audioContext) audioContext = new (window.AudioContext || window.webkitAudioContext)();
      if (audioContext.state === "suspended") audioContext.resume();
    } catch (error) {
      audioContext = null;
    }
  }

  function playBeep(kind) {
    try {
      if (!audioContext) return;
      const oscillator = audioContext.createOscillator();
      const gain = audioContext.createGain();
      oscillator.type = "sine";
      oscillator.frequency.value = kind === "long" ? 660 : 880;
      gain.gain.setValueAtTime(0.0001, audioContext.currentTime);
      gain.gain.exponentialRampToValueAtTime(kind === "long" ? 0.28 : 0.16, audioContext.currentTime + 0.01);
      gain.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + (kind === "long" ? 0.45 : 0.08));
      oscillator.connect(gain);
      gain.connect(audioContext.destination);
      oscillator.start();
      oscillator.stop(audioContext.currentTime + (kind === "long" ? 0.5 : 0.1));
    } catch (error) {
      // Audio restrictions must not stop the timer.
    }
  }

  function playPhaseWarning(speed) {
    const gap = speed === 60 ? 8 : 140;
    playBeep("short");
    window.setTimeout(function () { playBeep("short"); }, gap);
    window.setTimeout(function () { playBeep("short"); }, gap * 2);
  }

  function timerTick() {
    const timer = state.ui.timer;
    if (!timer || !timer.running) return;
    const now = performance.now();
    const elapsedWall = Math.max(0, (now - timer.lastWall) / 1000);
    timer.lastWall = now;
    advanceTimer(elapsedWall * timer.speed);
    renderApp();
  }

  function advanceTimer(seconds) {
    const timer = state.ui.timer;
    if (!timer || !timer.running) return;
    let remainingToSpend = Math.max(0, Number(seconds) || 0);
    while (remainingToSpend > 0 && state.ui.timer && state.ui.timer.running) {
      const phase = timer.phases[timer.phaseIndex];
      if (!phase) { finishTimer(); return; }
      if (timer.remaining > 3 && timer.remaining - remainingToSpend <= 3) playPhaseWarning(timer.speed);
      const spend = Math.min(timer.remaining, remainingToSpend);
      timer.remaining -= spend;
      timer.elapsed += spend;
      remainingToSpend -= spend;
      if (timer.remaining > 0) continue;
      if (timer.phaseIndex >= timer.phases.length - 1) { playBeep("long"); finishTimer(); return; }
      timer.phaseIndex += 1;
      timer.remaining = timer.phases[timer.phaseIndex].seconds;
    }
  }

  function pauseTimer() {
    if (!state.ui.timer) return;
    state.ui.timer.running = !state.ui.timer.running;
    state.ui.timer.lastWall = performance.now();
    if (state.ui.timer.running) { primeAudio(); requestWakeLock(); } else releaseWakeLock();
    renderApp();
  }

  function stopTimer() {
    stopTimerLoop();
    releaseWakeLock();
    state.ui.timer = null;
    state.ui.route = "today";
    state.ui.activeTab = "today";
    renderApp();
  }

  function emptyWorkRow(snapshot, index) {
    const row = { index: index + 1 };
    snapshot.fields.forEach(function (field) { if (field !== "time" || snapshot.structure.type === "intervals") row[field] = null; });
    return row;
  }

  function finishTimer() {
    const timer = state.ui.timer;
    if (!timer) return;
    stopTimerLoop();
    releaseWakeLock();
    const totalSeconds = timer.phases.reduce(function (sum, phase) { return sum + phase.seconds; }, 0);
    const cardio = { time: totalSeconds, workRows: Array.from({ length: timer.snapshot.structure.rounds }, function (_, index) { return emptyWorkRow(timer.snapshot, index); }) };
    const id = uid("cardio");
    const session = makeCardioSession(state.data, id, timer.date, timer.exerciseId, cardio, "", 6, false);
    state.data.sessions.push(session);
    state.ui.timer = null;
    state.ui.cardioDraft = draftFromSession(session, "today");
    state.ui.route = "cardio-form";
    state.ui.activeTab = "today";
    state.ui.selectedSessionId = id;
    saveState();
    renderApp();
  }

  function saveCardioFromForm(form) {
    const draft = state.ui.cardioDraft;
    if (!draft || !draft.snapshot) return;
    const formData = new FormData(form);
    draft.date = String(formData.get("date") || TODAY);
    draft.note = String(formData.get("note") || "").trim();
    const cardio = {};
    draft.snapshot.fields.forEach(function (field) {
      const value = draft.fields[field];
      if (field === "stroke") cardio[field] = value || "Freestyle";
      else if (field === "time") cardio[field] = value === "" ? null : Math.max(0, numberOr(value, 0) * 60);
      else if (field === "poolLength") cardio[field] = Math.max(1, numberOr(value, 25));
      else if (field === "laps") cardio[field] = Math.max(0, Math.round(numberOr(value, 0)));
      else cardio[field] = value === "" ? null : Math.max(0, numberOr(value, 0));
    });
    if (draft.snapshot.structure.type === "intervals") {
      cardio.workRows = draft.workRows.map(function (source, index) {
        const row = { index: index + 1 };
        draft.snapshot.fields.forEach(function (field) { if (source[field] != null && source[field] !== "") row[field] = field === "stroke" ? source[field] : numberOr(source[field], 0); });
        return row;
      });
    }
    const effort = Math.min(10, Math.max(1, Math.round(numberOr(draft.effort, 6))));
    if (draft.sessionId) {
      const session = getSession(state.data, draft.sessionId);
      if (session) {
        session.date = draft.date;
        session.dateTime = dateTimeFor(draft.date, "07:10:00");
        session.cardio = cardio;
        session.note = draft.note;
        session.effort = effort;
        session.finished = true;
        session.updatedAt = new Date().toISOString();
      }
    } else {
      state.data.sessions.push(makeCardioSession(state.data, uid("cardio"), draft.date, draft.exerciseId, cardio, draft.note, effort, true));
    }
    const returnRoute = draft.returnRoute;
    state.ui.cardioDraft = null;
    state.ui.route = returnRoute === "session-detail" ? "session-detail" : "today";
    state.ui.activeTab = returnRoute === "session-detail" ? "history" : "today";
    saveState();
    renderApp();
    showToast(draft.snapshot.name + " saved.");
  }

  function createHealthDraft() {
    return { id: "health-draft-1", source: "Samsung Health", exerciseId: "zone-2-run", date: "2026-09-07", distance: 4.2, time: 1570, avgHr: 148, note: "Imported draft from Samsung Health", effort: 6 };
  }

  function confirmHealthDraft() {
    const modal = state.ui.modal;
    if (!modal || modal.type !== "health-import") return;
    const draft = modal.draft;
    if (!state.data.sessions.some(function (session) { return session.id === draft.id; })) {
      state.data.sessions.push(makeCardioSession(state.data, draft.id, draft.date, draft.exerciseId, { distance: draft.distance, time: draft.time, avgHr: draft.avgHr }, draft.note, draft.effort, true));
    }
    state.ui.modal = null;
    state.ui.selectedDate = draft.date;
    commit(function () {}, "Health draft saved.");
  }

  function exportPayload() {
    return JSON.stringify({ format: "khon-fitness-mockup", schemaVersion: SCHEMA_VERSION, exportedAt: new Date().toISOString(), data: state.data }, null, 2);
  }

  function exportJson() {
    const blob = new Blob([exportPayload()], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "khon-fitness-backup-" + TODAY + ".json";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(function () { URL.revokeObjectURL(url); }, 0);
    showToast("JSON backup downloaded.");
  }

  function normalizeImportedData(value) {
    if (!value || typeof value !== "object" || !Array.isArray(value.exercises) || !Array.isArray(value.programs) || !Array.isArray(value.sessions)) return null;
    return normalizeData(value);
  }

  function importJsonFile(file) {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function () {
      try {
        const parsed = JSON.parse(String(reader.result || ""));
        const candidate = parsed && parsed.data ? parsed.data : parsed;
        const normalized = normalizeImportedData(candidate);
        if (!normalized) throw new Error("Invalid backup");
        state.ui.modal = { type: "import", data: normalized };
        renderApp();
      } catch (error) {
        showToast("Could not read that JSON backup.", "error");
      }
    };
    reader.onerror = function () { showToast("Could not read that file.", "error"); };
    reader.readAsText(file);
  }

  function mergeImportedData(imported) {
    const merged = clone(state.data);
    function addUnique(target, incoming, key) {
      const ids = {};
      target.forEach(function (item) { ids[item[key]] = true; });
      (incoming || []).forEach(function (item) { if (!ids[item[key]]) { target.push(clone(item)); ids[item[key]] = true; } });
    }
    addUnique(merged.modalities, imported.modalities, "id");
    addUnique(merged.exercises, imported.exercises, "id");
    addUnique(merged.programs, imported.programs, "id");
    addUnique(merged.sessions, imported.sessions, "id");
    const dates = {};
    merged.bodyweights.forEach(function (entry) { dates[entry.date] = true; });
    (imported.bodyweights || []).forEach(function (entry) { if (!dates[entry.date]) { merged.bodyweights.push(clone(entry)); dates[entry.date] = true; } });
    Object.keys(imported.overrides || {}).forEach(function (date) { if (!merged.overrides[date]) merged.overrides[date] = clone(imported.overrides[date]); });
    return normalizeData(merged);
  }

  function replaceImportedData(imported) {
    stopTimerLoop();
    state.data = normalizeData(imported);
    state.ui = resetUi("settings");
    state.ui.activeTab = "programs";
    state.ui.modal = null;
    saveState();
    renderApp();
    showToast("Backup restored.");
  }

  function checkForUpdate() {
    state.data.settings.lastUpdateCheck = new Date().toISOString();
    commit(function () {}, APP_VERSION + " is the latest.");
  }

  function resetDemo() {
    if (!window.confirm("Reset all local demo edits and return to seeded data?")) return;
    stopTimerLoop();
    state = { data: createSeedData(), ui: emptyUi() };
    saveState();
    renderApp();
    showToast("Demo data reset.");
  }

  function navigateTab(tab) {
    if (["today", "history", "programs"].indexOf(tab) === -1) return;
    if (state.ui.route === "timer" && tab !== "today") return;
    state.ui.activeTab = tab;
    state.ui.route = tab;
    state.ui.modal = null;
    state.ui.detailEditing = false;
    if (tab === "today") state.ui.bodyweightDraft = "";
    renderApp();
  }

  function navigateProgramRoute(route) {
    state.ui.activeTab = "programs";
    state.ui.route = route;
    state.ui.modal = null;
    renderApp();
  }

  function openSessionDetail(sessionId) {
    if (!getSession(state.data, sessionId)) return;
    state.ui.selectedSessionId = sessionId;
    state.ui.route = "session-detail";
    state.ui.activeTab = "history";
    state.ui.detailEditing = false;
    state.ui.modal = null;
    state.ui.expandedBlockId = null;
    renderApp();
  }

  function addMonths(monthKey, delta) {
    const parts = monthKey.split("-").map(Number);
    const date = new Date(parts[0], parts[1] - 1 + delta, 1);
    return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0");
  }

  function openSwap(sessionId, blockId, exerciseIndex) {
    const session = getSession(state.data, sessionId);
    const block = session && session.programSnapshot && session.programSnapshot.blocks.find(function (item) { return item.id === blockId; });
    if (!block) return;
    state.ui.modal = { type: "merge", loserId: block.exercises[exerciseIndex].exerciseId, survivorId: null, swap: true, sessionId: sessionId, blockId: blockId, exerciseIndex: exerciseIndex };
    renderApp();
  }

  function applySwap(survivorId) {
    const modal = state.ui.modal;
    const session = modal && getSession(state.data, modal.sessionId);
    const block = session && session.programSnapshot && session.programSnapshot.blocks.find(function (item) { return item.id === modal.blockId; });
    const exercise = exerciseById(state.data, survivorId);
    if (!modal || !session || !block || !exercise) return;
    const target = block.exercises[modal.exerciseIndex];
    target.exerciseId = exercise.id;
    target.exerciseName = exercise.name;
    target.primaryMuscle = exercise.primaryMuscle || target.primaryMuscle;
    target.secondaryMuscles = exercise.secondaryMuscles ? exercise.secondaryMuscles.slice() : target.secondaryMuscles;
    target.stepKg = exercise.stepKg || target.stepKg;
    target.notes = exercise.notes || target.notes;
    session.setLogs.filter(function (log) { return log.blockId === modal.blockId && log.exerciseIndex === modal.exerciseIndex; }).forEach(function (log) { log.exerciseId = exercise.id; });
    state.ui.modal = null;
    commit(function () {}, "Exercise swapped.");
  }

  function calendarCellFromEvent(event) {
    const target = event.target && event.target.closest ? event.target.closest(".calendar-day[data-date]") : null;
    return target && target.dataset.date ? target : null;
  }

  function clearPress() {
    if (state.ui.pressTimer) window.clearTimeout(state.ui.pressTimer);
    state.ui.pressTimer = null;
    state.ui.pressDate = null;
    state.ui.pressPoint = null;
  }

  function onClick(event) {
    const element = event.target && event.target.closest ? event.target.closest("[data-action]") : null;
    if (!element) return;
    const action = element.dataset.action;
    switch (action) {
      case "navigate": navigateTab(element.dataset.tab); break;
      case "start-item": startPlannedItem(); break;
      case "add-bodyweight": addBodyweight(); break;
      case "toggle-block": state.ui.expandedBlockId = element.dataset.blockId; renderApp(); break;
      case "toggle-set": toggleSet(element.dataset.sessionId, element.dataset.logId); break;
      case "step-input": stepInput(element); break;
      case "swap-exercise": openSwap(element.dataset.sessionId, element.dataset.blockId, Number(element.dataset.exerciseIndex)); break;
      case "finish-session": finishSession(element.dataset.sessionId); break;
      case "open-session": openSessionDetail(element.dataset.sessionId); break;
      case "back-to-history": state.ui.route = "history"; state.ui.activeTab = "history"; state.ui.detailEditing = false; state.ui.cardioDraft = null; renderApp(); break;
      case "back-to-today": state.ui.route = "today"; state.ui.activeTab = "today"; state.ui.cardioDraft = null; renderApp(); break;
      case "back-programs": navigateProgramRoute("programs"); break;
      case "back-program-editor": state.ui.route = "program-editor"; renderApp(); break;
      case "back-library": navigateProgramRoute("exercise-library"); break;
      case "back-modalities": navigateProgramRoute("modalities"); break;
      case "toggle-detail-edit": state.ui.detailEditing = !state.ui.detailEditing; renderApp(); break;
      case "edit-cardio-session": {
        const session = getSession(state.data, state.ui.selectedSessionId);
        if (session) { state.ui.detailEditing = true; state.ui.cardioDraft = draftFromSession(session, "session-detail"); state.ui.route = "cardio-form"; renderApp(); }
        break;
      }
      case "select-day":
        if (state.ui.longPressTriggered) { state.ui.longPressTriggered = false; break; }
        if (element.classList.contains("is-outside")) state.ui.calendarMonth = monthKeyFor(element.dataset.date);
        state.ui.selectedDate = element.dataset.date;
        renderApp();
        break;
      case "open-plan": openPlan(element.dataset.date, element.dataset.scope || "override"); break;
      case "calendar-month": state.ui.calendarMonth = addMonths(state.ui.calendarMonth, Number(element.dataset.direction)); renderApp(); break;
      case "health-import": state.ui.modal = { type: "health-import", draft: createHealthDraft() }; renderApp(); break;
      case "confirm-health-draft": confirmHealthDraft(); break;
      case "program-route": navigateProgramRoute(element.dataset.route || "programs"); break;
      case "open-program": state.ui.selectedProgramId = element.dataset.programId; state.ui.route = "program-editor"; state.ui.activeTab = "programs"; renderApp(); break;
      case "add-program": addProgram(); break;
      case "program-active": activateProgram(element.dataset.programId); break;
      case "open-block": state.ui.selectedBlockId = element.dataset.blockId; state.ui.route = "block-editor"; renderApp(); break;
      case "done-program": state.ui.route = "programs"; saveState(); renderApp(); break;
      case "done-block": state.ui.route = "program-editor"; saveState(); renderApp(); break;
      case "move-block": moveBlock(element.dataset.programId, element.dataset.blockId, Number(element.dataset.direction)); break;
      case "remove-block": if (window.confirm("Remove this block from the program?")) removeBlock(element.dataset.programId, element.dataset.blockId); break;
      case "add-block": addBlock(element.dataset.programId); break;
      case "add-exercise-to-block": addExerciseToBlock(element.dataset.programId, element.dataset.blockId); break;
      case "open-program-exercise": {
        const program = getProgram(state.data, element.dataset.programId);
        const block = program && program.blocks.find(function (item) { return item.id === element.dataset.blockId; });
        const prescription = block && block.exercises[Number(element.dataset.exerciseIndex)];
        state.ui.modal = { type: "choice", control: "program-exercise", multi: false, selected: prescription ? prescription.exerciseId : null, programId: element.dataset.programId, blockId: element.dataset.blockId, exerciseIndex: Number(element.dataset.exerciseIndex) };
        renderApp();
        break;
      }
      case "open-exercise": openExerciseEditor(element.dataset.exerciseId); break;
      case "add-exercise": addExerciseDraft(); break;
      case "done-exercise": saveExerciseEditor(); break;
      case "archive-exercise": archiveExercise(); break;
      case "cancel-new-exercise": state.ui.editorDraft = null; state.ui.editorIsNew = false; state.ui.route = "exercise-library"; renderApp(); break;
      case "delete-exercise": deleteExercise(state.ui.editorDraft && state.ui.editorDraft.id); break;
      case "open-merge-exercise": if (state.ui.editorDraft) { state.ui.modal = { type: "merge", loserId: state.ui.editorDraft.id, survivorId: null }; renderApp(); } break;
      case "open-filter": state.ui.modal = { type: "filter", filter: element.dataset.filter }; renderApp(); break;
      case "open-library-overflow": state.ui.modal = { type: "overflow" }; renderApp(); break;
      case "toggle-archived": state.ui.showArchived = !state.ui.showArchived; state.ui.modal = null; renderApp(); break;
      case "open-modality": openModalityEditor(element.dataset.modalityId); break;
      case "add-modality": addModalityDraft(); break;
      case "done-modality": saveModalityEditor(); break;
      case "open-color-picker": state.ui.modal = { type: "color", color: state.ui.modalityDraft && state.ui.modalityDraft.color }; renderApp(); break;
      case "export-json": exportJson(); break;
      case "import-json": document.getElementById("import-file").click(); break;
      case "check-update": checkForUpdate(); break;
      case "reset-demo": event.preventDefault(); resetDemo(); break;
      case "import-replace": if (state.ui.modal && state.ui.modal.type === "import") replaceImportedData(state.ui.modal.data); break;
      case "import-merge": if (state.ui.modal && state.ui.modal.type === "import") { state.data = mergeImportedData(state.ui.modal.data); state.ui.modal = null; saveState(); renderApp(); showToast("Backup merged."); } break;
      case "choose-plan-item": choosePlan(element.dataset.value); break;
      case "restore-plan": restorePlan(element.dataset.date); break;
      case "open-editor-select": openChoice(element.dataset.control); break;
      case "open-cardio-select": state.ui.modal = { type: "choice", control: "stroke", selected: state.ui.cardioDraft && state.ui.cardioDraft.fields.stroke }; renderApp(); break;
      case "choose-option": chooseOption(element.dataset.value); break;
      case "sheet-done": if (state.ui.modal) { applyChoice(state.ui.modal.control, state.ui.modal.selected); state.ui.modal = null; renderApp(); } break;
      case "choose-color": chooseColor(element.dataset.color); break;
      case "choose-merge-survivor": if (state.ui.modal) { state.ui.modal.survivorId = element.dataset.exerciseId; renderApp(); } break;
      case "confirm-merge": if (state.ui.modal && state.ui.modal.survivorId) { if (state.ui.modal.swap) applySwap(state.ui.modal.survivorId); else mergeExercises(state.ui.modal.loserId, state.ui.modal.survivorId); } break;
      case "open-history-exercise": state.ui.modal = { type: "history-exercise" }; renderApp(); break;
      case "close-summary": state.ui.modal = null; renderApp(); break;
      case "close-modal": if (element.classList.contains("modal-layer") && event.target === element) { state.ui.modal = null; renderApp(); } break;
      case "timer-pause": pauseTimer(); break;
      case "timer-stop": stopTimer(); break;
      case "timer-speed": if (state.ui.timer) { state.ui.timer.speed = state.ui.timer.speed === 60 ? 1 : 60; state.ui.timer.lastWall = performance.now(); renderApp(); } break;
      default: break;
    }
  }

  function chooseOption(value) {
    const modal = state.ui.modal;
    if (!modal) return;
    if (modal.type === "plan") { choosePlan(value); return; }
    if (modal.type === "filter") { updateFilter(modal.filter, value); return; }
    if (modal.type === "history-exercise") { state.ui.selectedExerciseId = value; state.ui.modal = null; renderApp(); return; }
    if (modal.type !== "choice") return;
    if (modal.control === "program-exercise") {
      const program = getProgram(state.data, modal.programId);
      const block = program && program.blocks.find(function (item) { return item.id === modal.blockId; });
      if (block && block.exercises[modal.exerciseIndex]) block.exercises[modal.exerciseIndex].exerciseId = value;
      state.ui.modal = null;
      saveState();
      renderApp();
      return;
    }
    if (modal.control === "stroke" && state.ui.cardioDraft) {
      state.ui.cardioDraft.fields.stroke = value;
      state.ui.modal = null;
      renderApp();
      return;
    }
    if (modal.multi) {
      const values = Array.isArray(modal.selected) ? modal.selected.slice() : [];
      const index = values.indexOf(value);
      if (index === -1) values.push(value); else values.splice(index, 1);
      modal.selected = values;
      renderApp();
      return;
    }
    applyChoice(modal.control, value);
    state.ui.modal = null;
    renderApp();
  }

  function onInput(event) {
    const element = event.target;
    if (!element) return;
    if (element.dataset.action === "numeric-input") { updateNumberFromInput(element); return; }
    if (element.dataset.action === "exercise-text") { if (state.ui.editorDraft) state.ui.editorDraft[element.dataset.field] = element.value; return; }
    if (element.dataset.action === "modality-text") { if (state.ui.modalityDraft) state.ui.modalityDraft.name = element.value; return; }
    if (element.dataset.action === "program-name") { const program = getProgram(state.data, element.dataset.programId); if (program) { program.name = element.value; saveState(); } return; }
    if (element.dataset.action === "cardio-text") { if (state.ui.cardioDraft) state.ui.cardioDraft.note = element.value; return; }
  }

  function onChange(event) {
    const element = event.target;
    if (!element) return;
    if (element.id === "import-file") {
      importJsonFile(element.files && element.files[0]);
      element.value = "";
      return;
    }
    if (element.dataset.action === "program-active") { activateProgram(element.dataset.programId); return; }
    if (element.dataset.action === "toggle-superset") { toggleSuperset(element.dataset.programId, element.dataset.blockId, element.checked); return; }
    if (element.dataset.action === "cardio-date" && state.ui.cardioDraft) state.ui.cardioDraft.date = element.value;
  }

  function onSubmit(event) {
    const form = event.target;
    if (!form || !form.dataset) return;
    event.preventDefault();
    if (form.dataset.form === "cardio") saveCardioFromForm(form);
  }

  function onKeydown(event) {
    if (event.key === "Escape" && state.ui.modal) { state.ui.modal = null; renderApp(); }
  }

  function onContextMenu(event) {
    const cell = calendarCellFromEvent(event);
    if (!cell || cell.classList.contains("is-outside")) return;
    event.preventDefault();
    openPlan(cell.dataset.date, "override");
  }

  function onPointerDown(event) {
    const cell = calendarCellFromEvent(event);
    if (!cell || cell.classList.contains("is-outside")) return;
    clearPress();
    state.ui.pressDate = cell.dataset.date;
    state.ui.pressPoint = [event.clientX || 0, event.clientY || 0];
    state.ui.pressTimer = window.setTimeout(function () {
      const date = state.ui.pressDate;
      if (!date) return;
      state.ui.longPressTriggered = true;
      clearPress();
      openPlan(date, "override");
      window.setTimeout(function () { state.ui.longPressTriggered = false; }, 900);
    }, 500);
  }

  function onPointerMove(event) {
    if (!state.ui.pressDate || !state.ui.pressPoint) return;
    const distance = Math.hypot((event.clientX || 0) - state.ui.pressPoint[0], (event.clientY || 0) - state.ui.pressPoint[1]);
    if (distance > 8) { state.ui.pointerDragDate = state.ui.pressDate; clearPress(); }
  }

  function onPointerUp(event) {
    const source = state.ui.pointerDragDate;
    const target = calendarCellFromEvent(event || { target: null });
    state.ui.pointerDragDate = null;
    if (source && target && source !== target.dataset.date && !target.classList.contains("is-outside")) { clearPress(); swapPlanDates(source, target.dataset.date); return; }
    clearPress();
  }

  function onDragStart(event) {
    const cell = calendarCellFromEvent(event);
    if (!cell || cell.classList.contains("is-outside")) return;
    state.ui.dragDate = cell.dataset.date;
    if (event.dataTransfer) { event.dataTransfer.effectAllowed = "move"; event.dataTransfer.setData("text/plain", cell.dataset.date); }
  }

  function onDragOver(event) {
    if (calendarCellFromEvent(event)) event.preventDefault();
  }

  function onDrop(event) {
    const cell = calendarCellFromEvent(event);
    if (!cell || cell.classList.contains("is-outside")) return;
    event.preventDefault();
    const source = state.ui.dragDate || (event.dataTransfer && event.dataTransfer.getData("text/plain"));
    state.ui.dragDate = null;
    swapPlanDates(source, cell.dataset.date);
  }

  function renderTimer() {
    const timer = state.ui.timer;
    if (!timer) return '<div class="timer-screen phase-rest"><div class="timer-main"><p>Timer stopped.</p></div></div>';
    const phase = timer.phases[timer.phaseIndex] || { name: "Done", kind: "work", round: 0 };
    const snapshot = timer.snapshot;
    const roundText = phase.kind === "warmup" ? "Warm-up" : "Round " + phase.round + " / " + snapshot.structure.rounds;
    return '<div class="timer-screen phase-' + phase.kind + '"><header class="timer-header"><button class="text-button timer-stop" data-action="timer-stop" type="button">Stop</button><div><h1>' + escapeHtml(snapshot.name) + '</h1><p>' + escapeHtml((modalityById(state.data, snapshot.modalityId) || {}).name || "") + '</p></div><button class="text-button timer-pause" data-action="timer-pause" type="button">' + (timer.running ? "Pause" : "Resume") + '</button></header><main class="timer-main"><p class="timer-phase">' + escapeHtml(phase.name) + '</p><div class="timer-seconds">' + Math.max(0, Math.ceil(timer.remaining)) + '</div><div class="timer-info"><strong>' + escapeHtml(roundText) + '</strong><span>' + escapeHtml(formatSeconds(timer.elapsed)) + ' elapsed</span></div><div class="timer-controls"><button class="speed-toggle ' + (timer.speed === 60 ? "is-active" : "") + '" data-action="timer-speed" type="button" aria-pressed="' + String(timer.speed === 60) + '">x60</button></div></main></div>';
  }

  function exposeTestApi() {
    window.__khonFitnessMockup = {
      storageKey: STORAGE_KEY,
      getData: function () { return clone(state.data); },
      reset: function () { stopTimerLoop(); state = { data: createSeedData(), ui: emptyUi() }; saveState(); renderApp(); },
      startGym: function (programId) { startGymSession(TODAY, programId); return unfinishedSession(state.data); },
      firstSetId: function () { const session = unfinishedSession(state.data); return session && session.setLogs[0] ? session.setLogs[0].id : null; },
      logSet: function (sessionId, logId) { toggleSet(sessionId, logId); },
      activeElementId: function () { return document.activeElement && document.activeElement.id; },
      navigate: function (tab) { navigateTab(tab); },
      openRoute: function (route) { navigateProgramRoute(route); },
      startIntervalTimer: function (exerciseId, fallbackExerciseId) {
        const selected = exerciseById(state.data, exerciseId) ? exerciseId : (exerciseById(state.data, fallbackExerciseId) ? fallbackExerciseId : exerciseId);
        startTimer(selected, TODAY);
      },
      setTimerSpeed: function (speed) { if (state.ui.timer) { state.ui.timer.speed = speed === 60 ? 60 : 1; state.ui.timer.lastWall = performance.now(); renderApp(); } },
      advanceTimer: function (seconds) { advanceTimer(seconds); renderApp(); },
      getTimer: function () { return state.ui.timer ? clone(state.ui.timer) : null; },
      addBodyweight: function (value) { state.ui.bodyweightDraft = value; addBodyweight(); },
      mergeExercises: function (loserId, survivorId) { mergeExercises(loserId, survivorId); },
      exportText: function () { return exportPayload(); },
      importText: function (text, mode) {
        const parsed = JSON.parse(text);
        const imported = normalizeImportedData(parsed.data || parsed);
        if (!imported) throw new Error("Invalid backup");
        state.data = mode === "merge" ? mergeImportedData(imported) : imported;
        saveState();
        renderApp();
      }
    };
  }

  function boot() {
    document.addEventListener("click", onClick);
    document.addEventListener("input", onInput);
    document.addEventListener("change", onChange);
    document.addEventListener("submit", onSubmit);
    document.addEventListener("keydown", onKeydown);
    document.addEventListener("contextmenu", onContextMenu);
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("pointermove", onPointerMove);
    document.addEventListener("pointerup", onPointerUp);
    document.addEventListener("pointercancel", onPointerUp);
    document.addEventListener("dragstart", onDragStart);
    document.addEventListener("dragover", onDragOver);
    document.addEventListener("drop", onDrop);
    window.addEventListener("blur", clearPress);
    window.addEventListener("beforeunload", function () { stopTimerLoop(); releaseWakeLock(); });
    exposeTestApi();
    renderApp();
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot);
  else boot();
}());
