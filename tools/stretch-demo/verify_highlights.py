#!/usr/bin/env python3
"""Verify phase and side behavior in render.swift's vertex highlights."""

from __future__ import annotations

import argparse
import json
import subprocess
import tempfile
from pathlib import Path


PROBE = r'''
let probeArgs=CommandLine.arguments
let guides=try JSONDecoder().decode([String:Guide].self,from:Data(contentsOf:URL(fileURLWithPath:probeArgs[1])))
let mesh=try HumanModel(path:probeArgs[2])
let hipX=mesh.restLandmarks["hip"]!.x
let middleZ=(mesh.restLandmarks["hipL"]!.z+mesh.restLandmarks["hipR"]!.z)*0.5
var rows=[[String:Any]]()
for key in guides.keys.sorted() {
    let guide=guides[key]!
    for phase in guide.poses.indices {
        var left:Float=0,right:Float=0,front:Float=0,rear:Float=0,shorts:Float=0,maximum:Float=0
        var active=0
        for vertex in mesh.positions.indices {
            let strength=mesh.accentIntensity(vertex,key,guide.focus,Float(phase))
            if strength>0.02 { active += 1 }
            maximum=max(maximum,strength)
            if mesh.positions[vertex].z<middleZ { left += strength } else { right += strength }
            if mesh.positions[vertex].x>hipX { front += strength } else { rear += strength }
            if mesh.isShorts(vertex) { shorts += strength }
        }
        rows.append(["exercise":key,"phase":phase,"activeVertices":active,"maxIntensity":maximum,
            "leftIntensity":left,"rightIntensity":right,"frontIntensity":front,
            "rearIntensity":rear,"shortsIntensity":shorts])
    }
}
let data=try JSONSerialization.data(withJSONObject:["checks":rows],options:[.prettyPrinted,.sortedKeys])
FileHandle.standardOutput.write(data)
'''


def main() -> int:
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--renderer', type=Path, default=root / 'tools/stretch-demo/render.swift')
    parser.add_argument('--poses', type=Path, default=root / 'tools/stretch-demo/poses.json')
    parser.add_argument('--mesh', type=Path, default=root / 'tools/stretch-demo/model/human_mesh.json')
    parser.add_argument('--output', type=Path, default=root / 'dist/qa-anatomy-revision/highlights/highlight-report.json')
    args = parser.parse_args()

    source = args.renderer.read_text()
    marker = 'let args=CommandLine.arguments'
    if source.count(marker) != 1:
        parser.error('Cannot identify the renderer entry point')
    with tempfile.TemporaryDirectory(prefix='khon-highlight-check-') as directory:
        directory = Path(directory)
        swift = directory / 'highlights.swift'
        executable = directory / 'highlights'
        swift.write_text(source.split(marker)[0] + PROBE)
        subprocess.run(['xcrun', 'swiftc', '-O', str(swift), '-o', str(executable)], check=True)
        raw = subprocess.check_output([str(executable), str(args.poses), str(args.mesh)], text=True, cwd=root)

    report = json.loads(raw)
    rows = {(row['exercise'], row['phase']): row for row in report['checks']}
    failures: list[str] = []

    def require(condition: bool, message: str) -> None:
        if not condition:
            failures.append(message)

    for exercise in ['hipflexor', 'shoulderir', 'needle', 'fold', 'catcow', 'ninety', 'plow']:
        require(rows[exercise, 0]['activeVertices'] == 0, f'{exercise} setup is not blank')
    require(rows['wgs', 0]['activeVertices'] > 0, 'WGS setup lost its faint targets')
    require(rows['elephant', 0]['leftIntensity'] > 0 and rows['elephant', 0]['rightIntensity'] > 0,
            'elephant setup is not faintly bilateral')
    require(rows['elephant', 1]['rightIntensity'] > rows['elephant', 1]['leftIntensity'] * 8,
            'elephant bend phase does not isolate the right straight leg')
    require(rows['elephant', 2]['leftIntensity'] > rows['elephant', 2]['rightIntensity'] * 8,
            'elephant switch phase does not isolate the left straight leg')
    require(rows['hipflexor', 2]['rightIntensity'] > rows['hipflexor', 2]['leftIntensity'] * 8,
            'hip flexor hold is not right-sided')
    require(rows['shoulderir', 2]['leftIntensity'] > rows['shoulderir', 2]['rightIntensity'] * 8,
            'shoulder rotation hold is not left-sided')
    require(rows['needle', 2]['leftIntensity'] > rows['needle', 2]['rightIntensity'] * 4,
            'thread-the-needle hold is not left-sided')
    require(rows['catcow', 1]['rearIntensity'] > rows['catcow', 1]['frontIntensity'] * 3,
            'cat round does not isolate the back')
    require(rows['catcow', 2]['frontIntensity'] > rows['catcow', 2]['rearIntensity'] * 2,
            'cow arch does not isolate the front')
    require(rows['ninety', 1]['leftIntensity'] > rows['ninety', 1]['rightIntensity'] * 8,
            '90/90 front-leg phase is not left-sided')
    require(rows['ninety', 2]['rightIntensity'] > 0 and rows['ninety', 3]['leftIntensity'] > rows['ninety', 3]['rightIntensity'],
            '90/90 back-leg and lean phases have the wrong balance')
    require(rows['plow', 1]['activeVertices'] < rows['plow', 2]['activeVertices'] <= rows['plow', 3]['activeVertices'],
            'plow posterior chain does not grow by phase')
    require(rows['ninety', 3]['shortsIntensity'] > 0 and rows['wgs', 0]['shortsIntensity'] > 0,
            'hip targets do not overlay the shorts')

    report['failures'] = failures
    report['status'] = 'passed' if not failures else 'failed'
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    print(f"{len(report['checks'])} phase checks, {len(failures)} failures. Report: {args.output}")
    for failure in failures:
        print(f'FAIL {failure}')
    return 1 if failures else 0


if __name__ == '__main__':
    raise SystemExit(main())
