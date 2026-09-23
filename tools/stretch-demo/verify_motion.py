#!/usr/bin/env python3
"""Check limb roll continuity using the real Swift renderer, without rendering PNGs.

Run on macOS from any directory:
    python3 tools/stretch-demo/verify_motion.py

The check compiles render.swift's definitions with a temporary diagnostic main.
It samples the same 64 eased poses as the packer. For each main limb bone, its
largest basis-axis turn may exceed the authored limb-direction turn by at most
30 degrees per adjacent frame. This allowance permits intentional gradual roll
but rejects the 90 to 170 degree single-frame flips found during mesh review.
It scales with the authored swing, so fast movement alone does not fail.
It also rejects a landmark that moves over 15 cm in one frame. That catches
an IK bend-direction flip even when bone movement follows the bad landmark.

Original failures included fold/shoulderir upperarm01.L at 0->1, wgs at 60->61,
ninety foot.R at 31->32, and plow foot.L at 2->3. The fold surface jumped 10.5 cm
while its elbow moved 2.8 mm. Surface displacement is recorded as supporting
evidence, not a separate arbitrary pass/fail threshold. The per-guide surface
measurement reads the actual render geometry, including hand and foot contact
translations. Visual and packaging QA remain needed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path


PROBE = r'''
let probeArgs = CommandLine.arguments
let guides = try JSONDecoder().decode([String:Guide].self,
    from:Data(contentsOf:URL(fileURLWithPath:probeArgs[1])))
let mesh = try HumanModel(path:probeArgs[2])
let allowance = Float(probeArgs[3])!
let includeContacts = probeArgs[4] == "contacts"
let controls = [
    ("upperarm01", "shoulder", "elbow", ["upperarm"]),
    ("lowerarm01", "elbow", "wrist", ["lowerarm"]),
    ("wrist", "wrist", "hand", ["wrist", "metacarpal", "finger", "thumb"]),
    ("upperleg01", "hip", "knee", ["upperleg"]),
    ("lowerleg01", "knee", "ankle", ["lowerleg"]),
    ("foot", "ankle", "toe", ["foot", "toe"]),
]
func angle(_ a:V, _ b:V) -> Float {
    return acos(max(-1, min(1, simd_dot(unit(a), unit(b))))) * 180 / Float.pi
}
func basisTurn(_ a:M, _ b:M) -> Float {
    return (0..<3).map { c in
        let av = a[c], bv = b[c]
        return angle(V(av.x,av.y,av.z), V(bv.x,bv.y,bv.z))
    }.max()!
}
func skinPoint(_ i:Int, _ transforms:[M]) -> V {
    var result = V.zero
    for offset in mesh.influenceRange(i) {
        let bone = mesh.influenceJoint(offset), weight = mesh.influenceWeight(offset)
        result += point(transforms[bone],mesh.positions[i]) * weight
    }
    return result
}
func renderedSurface(_ pose:[String:V], _ key:String, _ focus:String) -> [V] {
    let source = mesh.geometry(pose,exercise:key,focus:focus).sources(for:.vertex).first!
    precondition(source.usesFloatComponents && source.componentsPerVector == 3)
    return source.data.withUnsafeBytes { bytes in
        (0..<source.vectorCount).map { i in
            let offset = source.dataOffset+i*source.dataStride
            func component(_ c:Int) -> Float {
                let at = offset+c*source.bytesPerComponent
                if source.bytesPerComponent == 4 { return bytes.loadUnaligned(fromByteOffset:at,as:Float.self) }
                precondition(source.bytesPerComponent == 8)
                return Float(bytes.loadUnaligned(fromByteOffset:at,as:Double.self))
            }
            return V(component(0),component(1),component(2))
        }
    }
}
var failures = [[String:Any]]()
var landmarkFailures = [[String:Any]]()
var worstByGuide = [[String:Any]]()
var maxSurfaceByGuide = [[String:Any]]()
var maxTurn:Float = 0
var maxTurnRecord:[String:Any] = [:]
var comparisons = 0
for key in guides.keys.sorted() {
    let g = guides[key]!
    var previousPose:[String:V]? = nil
    var previousDeltas:[M]? = nil
    var previousSurface:[V]? = nil
    var worstExcess:Float = -Float.greatestFiniteMagnitude
    var worst:[String:Any] = [:]
    var maxSurface:Float = 0
    var surfaceRecord:[String:Any] = [:]
    for f in 0..<64 {
        let pos = Float(f)/63 * Float(g.poses.count-1)
        let i = min(g.poses.count-2,Int(pos)), t0 = pos-Float(i)
        let pose = interpolate(g.poses[i],g.poses[i+1],t0*t0*(3-2*t0),key)
        let deltas = mesh.boneDeltas(pose)
        let transforms = mesh.boneNames.indices.map {
            deltas[$0]*mesh.restMatrices[$0]*mesh.inverseBindMatrices[$0]
        }
        let surface = includeContacts ? renderedSurface(pose,key,g.focus) : mesh.positions.indices.map { skinPoint($0,transforms) }
        if let oldSurface = previousSurface, let oldPose = previousPose {
            let landmarkSteps = pose.keys.filter { !["face","up","pelvisUp"].contains($0) }
                .map { ($0,simd_length(pose[$0]!-oldPose[$0]!)) }
            let largestLandmark = landmarkSteps.max { $0.1 < $1.1 }!
            let landmarkMovement = largestLandmark.1
            if landmarkMovement > 0.15 {
                landmarkFailures.append(["guide":key,"landmark":largestLandmark.0,
                    "fromFrame":f-1,"toFrame":f,"stepMetres":landmarkMovement])
            }
            for vertex in surface.indices {
                let step = simd_length(surface[vertex]-oldSurface[vertex])
                if step>maxSurface {
                    maxSurface=step
                    surfaceRecord=["guide":key,"fromFrame":f-1,"toFrame":f,
                        "surfaceVertex":vertex,"maxSurfaceDisplacementMetres":step,
                        "maxAuthoredLandmarkDisplacementMetres":landmarkMovement]
                }
            }
        }
        if let oldPose = previousPose, let oldDeltas = previousDeltas {
            for bone in mesh.boneNames.indices {
                let turn = basisTurn(oldDeltas[bone],deltas[bone])
                if turn>maxTurn {
                    maxTurn=turn
                    maxTurnRecord=["guide":key,"bone":mesh.boneNames[bone],
                        "fromFrame":f-1,"toFrame":f,"boneTurnDegrees":turn]
                }
            }
            for (prefix,root,tip,tokens) in controls {
                for side in ["L","R"] {
                    let name = prefix + "." + side
                    guard let bone = mesh.boneNames.firstIndex(of:name) else {
                        fatalError("Missing continuity-check bone: \(name)")
                    }
                    let turn = basisTurn(oldDeltas[bone],deltas[bone])
                    let authored = angle(oldPose[tip+side]!-oldPose[root+side]!,
                                         pose[tip+side]!-pose[root+side]!)
                    let excess = turn-authored
                    comparisons += 1
                    var result:[String:Any] = ["guide":key,"bone":name,
                        "fromFrame":f-1,"toFrame":f,"boneTurnDegrees":turn,
                        "authoredAxisTurnDegrees":authored,"excessDegrees":excess,
                        "allowedExcessDegrees":allowance]
                    if excess > allowance {
                        let before = mesh.boneNames.indices.map {
                            oldDeltas[$0]*mesh.restMatrices[$0]*mesh.inverseBindMatrices[$0]
                        }
                        let after = mesh.boneNames.indices.map {
                            deltas[$0]*mesh.restMatrices[$0]*mesh.inverseBindMatrices[$0]
                        }
                        var jump:Float = 0
                        var vertex = -1
                        for j in mesh.positions.indices where mesh.boneWeight(j,tokens,side:side)>0.5 {
                            let distance = simd_length(skinPoint(j,after)-skinPoint(j,before))
                            if distance>jump { jump=distance;vertex=j }
                        }
                        result["maxSurfaceDisplacementMetres"] = jump
                        result["surfaceVertex"] = vertex
                        result["rootDisplacementMetres"] = simd_length(pose[root+side]!-oldPose[root+side]!)
                        result["tipDisplacementMetres"] = simd_length(pose[tip+side]!-oldPose[tip+side]!)
                        failures.append(result)
                    }
                    if excess>worstExcess { worstExcess=excess;worst=result }
                }
            }
        }
        previousPose = pose
        previousDeltas = deltas
        previousSurface = surface
    }
    worstByGuide.append(worst)
    maxSurfaceByGuide.append(surfaceRecord)
}
let report:[String:Any] = ["frameCount":64,"comparisons":comparisons,
    "allowedExcessDegrees":allowance,"failures":failures,
    "landmarkStepLimitMetres":0.15,"landmarkFailures":landmarkFailures,"worstByGuide":worstByGuide,
    "maxBoneTurn":maxTurnRecord,"maxSurfaceByGuide":maxSurfaceByGuide,
    "includesGroundContact":includeContacts]
let data = try JSONSerialization.data(withJSONObject:report,options:[.prettyPrinted,.sortedKeys])
FileHandle.standardOutput.write(data)
'''


def main() -> int:
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--renderer', type=Path, default=root / 'tools/stretch-demo/render.swift')
    parser.add_argument('--poses', type=Path, default=root / 'tools/stretch-demo/poses.json')
    parser.add_argument('--mesh', type=Path, default=root / 'tools/stretch-demo/model/human_mesh.json')
    parser.add_argument('--output', type=Path, default=root / 'dist/qa-anatomy/motion-report.json')
    parser.add_argument('--skin-only', action='store_true', help='Fast iteration without ground contact. Omit for final validation.')
    args = parser.parse_args()
    source = args.renderer.read_text()
    marker = 'let args=CommandLine.arguments'
    if source.count(marker) != 1:
        parser.error('Cannot identify the renderer entry point. Update the prelude boundary.')
    # Fail closed if the renderer changes its sampling count instead of silently
    # verifying a different frame sequence from the one it packages.
    if 'let frameCount=64' not in source:
        parser.error('Renderer frame count changed. Update the probe sampling contract.')
    inputs = {'renderer': source.encode(), 'poses': args.poses.read_bytes(),
              'mesh': args.mesh.read_bytes()}
    with tempfile.TemporaryDirectory(prefix='khon-motion-check-') as directory:
        directory = Path(directory)
        swift = directory / 'motion.swift'
        executable = directory / 'motion'
        for name in ['poses', 'mesh']:
            (directory / f'{name}.json').write_bytes(inputs[name])
        swift.write_text(source.split(marker)[0] + PROBE)
        subprocess.run(['xcrun', 'swiftc', '-O', str(swift), '-o', str(executable)], check=True)
        output = subprocess.check_output([
            str(executable), str(directory / 'poses.json'), str(directory / 'mesh.json'), '30',
            'skin' if args.skin_only else 'contacts',
        ], text=True, cwd=root)
    report = json.loads(output)
    report['renderer'] = str(args.renderer.resolve())
    report['mesh'] = str(args.mesh.resolve())
    report['sha256'] = {name: hashlib.sha256(data).hexdigest() for name, data in inputs.items()}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    for failure in report['failures']:
        print(f"FAIL {failure['guide']} {failure['bone']} "
              f"{failure['fromFrame']}->{failure['toFrame']}: "
              f"bone {failure['boneTurnDegrees']:.1f} deg, "
              f"authored {failure['authoredAxisTurnDegrees']:.1f} deg, "
              f"surface {failure['maxSurfaceDisplacementMetres']*100:.1f} cm")
    for failure in report['landmarkFailures']:
        print(f"FAIL {failure['guide']} {failure['landmark']} "
              f"{failure['fromFrame']}->{failure['toFrame']}: "
              f"landmark {failure['stepMetres']*100:.1f} cm")
    print(f"{len(report['failures'])} failures in {report['comparisons']} comparisons. "
          f"Report: {args.output}")
    turn = report['maxBoneTurn']
    surface = max(report['maxSurfaceByGuide'], key=lambda row: row['maxSurfaceDisplacementMetres'])
    print(f"Maximum bone turn: {turn['boneTurnDegrees']:.1f} deg, "
          f"{turn['guide']} {turn['bone']} {turn['fromFrame']}->{turn['toFrame']}.")
    print(f"Maximum surface step: {surface['maxSurfaceDisplacementMetres']*100:.1f} cm, "
          f"{surface['guide']} {surface['fromFrame']}->{surface['toFrame']}, "
          f"authored landmark step {surface['maxAuthoredLandmarkDisplacementMetres']*100:.1f} cm.")
    return 1 if report['failures'] or report['landmarkFailures'] else 0


if __name__ == '__main__':
    raise SystemExit(main())
