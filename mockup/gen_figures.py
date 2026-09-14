#!/usr/bin/env python3
"""Regenerates the Figures.poses block in the app from mockup/figures.json. Run from the repo root."""
import json, re
d = json.load(open('mockup/figures.json'))
J = ["head","shoulder","chest","hip","kneeF","footF","kneeB","footB","elbowF","handF","elbowB","handB"]
pose = lambda p: "p(" + ", ".join(f"{p[j][0]}f, {p[j][1]}f" for j in J) + ")"
entries = [f'        // {f["cue"]}\n        "{k}" to ({pose(f["a"])}\n            to {pose(f["b"])}),' for k, f in d.items() if not k.startswith('_')]
path = 'android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/components/Figure.kt'
s = open(path).read()
s = re.sub(r'(val poses: Map<String, Pair<Pose, Pose>> = mapOf\(\n).*?(\n    \)\n)', lambda m: m.group(1) + "\n".join(entries) + m.group(2), s, flags=re.S)
metadata = "    val moving = setOf(" + ", ".join(json.dumps(k) for k, f in d.items() if not k.startswith('_') and f.get('motion') == 'reps') + ")\n"
metadata += "    private val cues = mapOf(\n" + "\n".join("        " + json.dumps(k) + " to " + json.dumps(f['cue'], ensure_ascii=False) + "," for k,f in d.items() if not k.startswith('_')) + "\n    )\n"
metadata += '    fun cue(key: String): String = cues[key].orEmpty()\n'
s = re.sub(r'    // BEGIN FIGURE METADATA.*?    // END FIGURE METADATA\n', '', s, flags=re.S)
s = s.replace('    val keys: List<String>', '    // BEGIN FIGURE METADATA\n' + metadata + '    // END FIGURE METADATA\n    val keys: List<String>')
open(path, 'w').write(s)
print("wrote", len(entries), "figures")
