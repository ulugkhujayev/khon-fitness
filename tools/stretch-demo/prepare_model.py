#!/usr/bin/env python3
"""Build the compact Khon Fitness rest mesh from official MakeHuman assets."""

from __future__ import annotations

import argparse
import json
import math
from collections import deque
from pathlib import Path


TARGETS = {
    "makehuman/data/targets/macrodetails/african-male-young.target": 1 / 3,
    "makehuman/data/targets/macrodetails/asian-male-young.target": 1 / 3,
    "makehuman/data/targets/macrodetails/caucasian-male-young.target": 1 / 3,
    "makehuman/data/targets/macrodetails/universal-male-young-maxmuscle-averageweight.target": 0.8,
    "makehuman/data/targets/macrodetails/universal-male-young-maxmuscle-minweight.target": 0.2,
    "makehuman/data/targets/torso/torso-muscle-dorsi-incr.target": 0.7,
    "makehuman/data/targets/torso/torso-vshape-incr.target": 0.25,
    "makehuman/data/targets/stomach/stomach-tone-incr.target": 1.0,
    "makehuman/data/targets/armslegs/l-upperarm-shoulder-muscle-incr.target": 0.85,
    "makehuman/data/targets/armslegs/r-upperarm-shoulder-muscle-incr.target": 0.85,
    "makehuman/data/targets/armslegs/l-upperarm-muscle-incr.target": 0.65,
    "makehuman/data/targets/armslegs/r-upperarm-muscle-incr.target": 0.65,
    "makehuman/data/targets/armslegs/l-lowerarm-muscle-incr.target": 0.35,
    "makehuman/data/targets/armslegs/r-lowerarm-muscle-incr.target": 0.35,
    "makehuman/data/targets/armslegs/l-upperleg-muscle-incr.target": 0.8,
    "makehuman/data/targets/armslegs/r-upperleg-muscle-incr.target": 0.8,
    "makehuman/data/targets/armslegs/l-lowerleg-muscle-incr.target": 0.45,
    "makehuman/data/targets/armslegs/r-lowerleg-muscle-incr.target": 0.45,
}


def add(a, b):
    return [a[i] + b[i] for i in range(3)]


def sub(a, b):
    return [a[i] - b[i] for i in range(3)]


def mul(a, scalar):
    return [value * scalar for value in a]


def dot(a, b):
    return sum(a[i] * b[i] for i in range(3))


def cross(a, b):
    return [
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    ]


def normalized(value, fallback=(1.0, 0.0, 0.0)):
    length = math.sqrt(dot(value, value))
    if length < 1e-10:
        return list(fallback)
    return [component / length for component in value]


def average(points):
    return [sum(point[i] for point in points) / len(points) for i in range(3)]


def load_obj(path):
    positions = []
    uvs = []
    faces = []
    group = None
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "v":
            positions.append([float(value) for value in parts[1:4]])
        elif parts[0] == "vt":
            uvs.append([float(value) for value in parts[1:3]])
        elif parts[0] == "g":
            group = parts[1]
        elif parts[0] == "f" and group == "body":
            face = []
            for corner in parts[1:]:
                indexes = corner.split("/")
                vertex = int(indexes[0]) - 1
                uv = int(indexes[1]) - 1 if len(indexes) > 1 and indexes[1] else None
                face.append((vertex, uv))
            faces.append(face)
    return positions, uvs, faces


def apply_target(positions, path, weight):
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if not parts or parts[0].startswith("#"):
            continue
        index = int(parts[0])
        delta = [float(value) * weight for value in parts[1:4]]
        positions[index] = add(positions[index], delta)


def mat_inverse_rigid(matrix):
    rotation = [[matrix[row * 4 + col] for col in range(3)] for row in range(3)]
    translation = [matrix[row * 4 + 3] for row in range(3)]
    transposed = [[rotation[col][row] for col in range(3)] for row in range(3)]
    inverse_translation = [-dot(row, translation) for row in transposed]
    return [
        transposed[0][0], transposed[0][1], transposed[0][2], inverse_translation[0],
        transposed[1][0], transposed[1][1], transposed[1][2], inverse_translation[1],
        transposed[2][0], transposed[2][1], transposed[2][2], inverse_translation[2],
        0.0, 0.0, 0.0, 1.0,
    ]


def mat_multiply(a, b):
    return [
        sum(a[row * 4 + k] * b[k * 4 + col] for k in range(4))
        for row in range(4)
        for col in range(4)
    ]


def compact(values, digits=6):
    return [round(value, digits) if isinstance(value, float) else value for value in values]


def sculpt_flat_pectoral(mesh):
    """Flatten and lift the lower chest without changing the rest rig or topology.

    Work on the six-decimal exported rest positions to keep the approved sculpt
    reproducible. The compact support leaves the back, arms and abdomen alone.
    Smooth by MakeHuman vertex ID so UV seam copies receive the same movement.
    """
    def smooth(start, end, value):
        t = max(0.0, min(1.0, (value - start) / (end - start)))
        return t * t * (3 - 2 * t)

    original = list(zip(*[iter(mesh["positions"])] * 3))
    positions = [list(point) for point in original]
    source_ids = mesh["sourceVertexIds"]
    for point in positions:
        x, y, z = point
        mask = (
            smooth(1.15, 1.21, y) * (1 - smooth(1.30, 1.39, y))
            * (1 - smooth(0.14, 0.19, abs(z))) * smooth(0.045, 0.09, x)
        )
        cap = 0.124 - 0.55 * z * z + 0.12 * (y - 1.25)
        distance = x - cap
        point[0] -= mask * 0.5 * (distance + math.sqrt(distance * distance + 0.010 * 0.010))

    displacement = {source_id: original[i][0] - positions[i][0] for i, source_id in enumerate(source_ids)}
    neighbors = {source_id: set() for source_id in source_ids}
    triangles = list(zip(*[iter(mesh["triangles"])] * 3))
    for a, b, c in triangles:
        for first, second in ((a, b), (b, c), (c, a)):
            neighbors[source_ids[first]].add(source_ids[second])
            neighbors[source_ids[second]].add(source_ids[first])
    for _ in range(6):
        displacement = {
            source_id: 0.5 * delta + 0.5 * sum(displacement[index] for index in sorted(neighbors[source_id])) / len(neighbors[source_id])
            if delta > 0 and neighbors[source_id] else delta
            for source_id, delta in displacement.items()
        }
    for index, point in enumerate(positions):
        point[0] = original[index][0] - displacement[source_ids[index]]
        x, y, z = point
        point[1] += (
            0.024 * smooth(1.16, 1.225, y) * (1 - smooth(1.235, 1.35, y))
            * smooth(0.012, 0.04, abs(z)) * (1 - smooth(0.12, 0.18, abs(z)))
            * smooth(0.06, 0.10, x)
        )

    # Rebuild area-weighted normals on the sculpt, including UV seam copies.
    normals = {source_id: [0.0, 0.0, 0.0] for source_id in source_ids}
    for a, b, c in triangles:
        normal = cross(sub(positions[b], positions[a]), sub(positions[c], positions[a]))
        for index in (a, b, c):
            source_id = source_ids[index]
            normals[source_id] = add(normals[source_id], normal)
    mesh["positions"] = compact([value for point in positions for value in point])
    mesh["normals"] = compact([
        value / (math.sqrt(dot(normals[source_id], normals[source_id])) or 1)
        for source_id in source_ids for value in normals[source_id]
    ])


def build(source, output):
    obj_path = source / "makehuman/data/3dobjs/base.obj"
    skeleton_path = source / "makehuman/data/rigs/default.mhskel"
    weights_path = source / "makehuman/data/rigs/default_weights.mhw"
    positions, source_uvs, faces = load_obj(obj_path)
    for relative_path, weight in TARGETS.items():
        apply_target(positions, source / relative_path, weight)

    visible_source_vertices = sorted({vertex for face in faces for vertex, _ in face})
    minimum_y = min(positions[index][1] for index in visible_source_vertices)
    maximum_y = max(positions[index][1] for index in visible_source_vertices)
    scale = 1.7 / (maximum_y - minimum_y)

    # MakeHuman is Y-up and faces +Z. MakeHuman +X is the person's left.
    # Khon Fitness faces +X and uses +Z for the person's right.
    def convert(point):
        return [
            point[2] * scale,
            (point[1] - minimum_y) * scale,
            -point[0] * scale,
        ]

    converted_source_positions = [convert(position) for position in positions]

    vertex_map = {}
    output_positions = []
    output_uvs = []
    output_source_ids = []
    triangles = []

    def output_vertex(corner):
        if corner not in vertex_map:
            source_vertex, source_uv = corner
            vertex_map[corner] = len(output_positions)
            output_positions.append(converted_source_positions[source_vertex])
            output_uvs.append(source_uvs[source_uv] if source_uv is not None else [0.0, 0.0])
            output_source_ids.append(source_vertex)
        return vertex_map[corner]

    for face in faces:
        root = output_vertex(face[0])
        for index in range(1, len(face) - 1):
            triangles.extend([root, output_vertex(face[index]), output_vertex(face[index + 1])])

    # Accumulate by source vertex so UV seam duplicates keep a smooth normal.
    source_normals = [[0.0, 0.0, 0.0] for _ in positions]
    for face in faces:
        root = face[0][0]
        for index in range(1, len(face) - 1):
            a, b, c = root, face[index][0], face[index + 1][0]
            normal = cross(
                sub(converted_source_positions[b], converted_source_positions[a]),
                sub(converted_source_positions[c], converted_source_positions[a]),
            )
            source_normals[a] = add(source_normals[a], normal)
            source_normals[b] = add(source_normals[b], normal)
            source_normals[c] = add(source_normals[c], normal)
    source_normals = [normalized(normal, (0.0, 1.0, 0.0)) for normal in source_normals]
    normals = [source_normals[source_vertex] for source_vertex in output_source_ids]

    skeleton_source = json.loads(skeleton_path.read_text(encoding="utf-8"))
    joint_positions = {
        name: average([converted_source_positions[index] for index in indexes])
        for name, indexes in skeleton_source["joints"].items()
    }

    bone_definitions = skeleton_source["bones"]
    children = {name: [] for name in bone_definitions}
    roots = []
    for name, definition in bone_definitions.items():
        parent = definition.get("parent")
        if parent:
            children[parent].append(name)
        else:
            roots.append(name)
    ordered_bones = []
    queue = deque(roots)
    while queue:
        name = queue.popleft()
        ordered_bones.append(name)
        queue.extend(children[name])
    bone_indexes = {name: index for index, name in enumerate(ordered_bones)}

    bone_records = []
    global_matrices = {}
    for name in ordered_bones:
        definition = bone_definitions[name]
        head = joint_positions[definition["head"]]
        tail = joint_positions[definition["tail"]]
        roll = definition.get("rotation_plane")
        plane_names = roll if isinstance(roll, list) else [roll]
        plane_normals = []
        for plane_name in plane_names:
            if not plane_name or plane_name not in skeleton_source["planes"]:
                continue
            j1, j2, j3 = skeleton_source["planes"][plane_name]
            pvec = normalized(sub(joint_positions[j2], joint_positions[j1]))
            yvec = normalized(sub(joint_positions[j3], joint_positions[j2]))
            plane_normals.append(normalized(cross(yvec, pvec)))
        normal = normalized(average(plane_normals), (0.0, 1.0, 0.0)) if plane_normals else [0.0, 1.0, 0.0]
        y_axis = normalized(sub(tail, head), (0.0, 1.0, 0.0))
        z_axis = normalized(cross(normal, y_axis), (0.0, 0.0, 1.0))
        x_axis = normalized(cross(y_axis, z_axis), (1.0, 0.0, 0.0))
        rest = [
            x_axis[0], y_axis[0], z_axis[0], head[0],
            x_axis[1], y_axis[1], z_axis[1], head[1],
            x_axis[2], y_axis[2], z_axis[2], head[2],
            0.0, 0.0, 0.0, 1.0,
        ]
        global_matrices[name] = rest
        parent_name = definition.get("parent")
        local_rest = mat_multiply(mat_inverse_rigid(global_matrices[parent_name]), rest) if parent_name else rest
        bone_records.append({
            "name": name,
            "parent": bone_indexes[parent_name] if parent_name else -1,
            "head": compact(head),
            "tail": compact(tail),
            "restMatrix": compact(rest),
            "restLocalMatrix": compact(local_rest),
            "inverseBindMatrix": compact(mat_inverse_rigid(rest)),
        })

    weight_source = json.loads(weights_path.read_text(encoding="utf-8"))["weights"]
    source_influences = [[] for _ in positions]
    for bone_name, assignments in weight_source.items():
        bone_index = bone_indexes[bone_name]
        for vertex_index, weight in assignments:
            source_influences[vertex_index].append((bone_index, float(weight)))
    skin_joints = []
    skin_weights = []
    influence_offsets = [0]
    all_joints = []
    all_weights = []
    for source_vertex in output_source_ids:
        all_influences = sorted(source_influences[source_vertex], key=lambda item: item[1], reverse=True)
        total = sum(weight for _, weight in all_influences)
        if total <= 0:
            all_influences = [(bone_indexes["root"], 1.0)]
            total = 1.0
        normalized_all = [(bone, weight / total) for bone, weight in all_influences]
        all_joints.extend(bone for bone, _ in normalized_all)
        all_weights.extend(weight for _, weight in normalized_all)
        influence_offsets.append(len(all_joints))

        packed_influences = normalized_all[:4]
        packed_total = sum(weight for _, weight in packed_influences)
        packed_influences = [(bone, weight / packed_total) for bone, weight in packed_influences]
        packed_influences.extend([(0, 0.0)] * (4 - len(packed_influences)))
        skin_joints.extend(bone for bone, _ in packed_influences)
        skin_weights.extend(weight for _, weight in packed_influences)

    landmark_specs = {
        "head": ("head", "midpoint"),
        "hipL": ("upperleg01.L", "head"),
        "hipR": ("upperleg01.R", "head"),
        "kneeL": ("lowerleg01.L", "head"),
        "kneeR": ("lowerleg01.R", "head"),
        "ankleL": ("foot.L", "head"),
        "ankleR": ("foot.R", "head"),
        "toeL": ("toe1-2.L", "tail"),
        "toeR": ("toe1-2.R", "tail"),
        "shoulderL": ("upperarm01.L", "head"),
        "shoulderR": ("upperarm01.R", "head"),
        "elbowL": ("lowerarm01.L", "head"),
        "elbowR": ("lowerarm01.R", "head"),
        "wristL": ("wrist.L", "head"),
        "wristR": ("wrist.R", "head"),
        "handL": ("metacarpal2.L", "tail"),
        "handR": ("metacarpal2.R", "tail"),
    }
    bone_by_name = {bone["name"]: bone for bone in bone_records}
    landmarks = {}
    for landmark, (bone_name, endpoint) in landmark_specs.items():
        bone = bone_by_name[bone_name]
        position = average([bone["head"], bone["tail"]]) if endpoint == "midpoint" else bone[endpoint]
        landmarks[landmark] = {"bone": bone_name, "endpoint": endpoint, "position": compact(position)}
    for center, left, right in (
        ("hip", "hipL", "hipR"),
        ("shoulder", "shoulderL", "shoulderR"),
    ):
        landmarks[center] = {
            "bone": None,
            "derivedFrom": [left, right],
            "position": compact(average([landmarks[left]["position"], landmarks[right]["position"]])),
        }
    chest_fraction = 0.53
    landmarks["chest"] = {
        "bone": None,
        "derivedFrom": ["hip", "shoulder"],
        "fraction": chest_fraction,
        "position": compact(add(
            landmarks["hip"]["position"],
            mul(sub(landmarks["shoulder"]["position"], landmarks["hip"]["position"]), chest_fraction),
        )),
    }
    landmarks["face"] = {"direction": [1.0, 0.0, 0.0]}
    landmarks["up"] = {"direction": [0.0, 1.0, 0.0]}

    result = {
        "version": 1,
        "source": {
            "project": "MakeHuman",
            "repository": "https://github.com/makehumancommunity/makehuman",
            "commit": "a8bc2d54ff0ac92e78ff71431b1023eda42bf482",
            "license": "CC0-1.0",
            "shape": {
                "ageYears": 25,
                "gender": "male",
                "muscleSlider": 1.0,
                "weightSlider": 0.4,
                "detailPreset": "athletic-defined",
                "ethnicityBlend": {"african": 1 / 3, "asian": 1 / 3, "caucasian": 1 / 3},
            },
            "targets": [
                {"path": path, "weight": round(weight, 6)}
                for path, weight in TARGETS.items()
            ],
        },
        "coordinateSystem": {
            "unit": "metre",
            "rightHanded": True,
            "x": "forward",
            "y": "up",
            "z": "person-right",
            "floorY": 0.0,
            "height": 1.7,
        },
        "mesh": {
            "vertexCount": len(output_positions),
            "triangleCount": len(triangles) // 3,
            "positions": compact([value for position in output_positions for value in position]),
            "normals": compact([value for normal in normals for value in normal]),
            "uvs": compact([value for uv in output_uvs for value in uv]),
            "triangles": triangles,
            "sourceVertexIds": output_source_ids,
            "materialRanges": [{"name": "body", "startTriangle": 0, "triangleCount": len(triangles) // 3}],
        },
        "skeleton": {
            "boneCount": len(bone_records),
            "matrixLayout": "row-major",
            "localBoneAxes": {"x": "roll-reference", "y": "head-to-tail", "z": "x-cross-y"},
            "bones": bone_records,
        },
        "skin": {
            "maxInfluences": 4,
            "joints": skin_joints,
            "weights": compact(skin_weights),
            "influenceOffsets": influence_offsets,
            "allJoints": all_joints,
            "allWeights": compact(all_weights),
            "method": "MakeHuman native weights normalized per output vertex; fixed-four packing is also supplied",
        },
        "landmarks": landmarks,
        "excludedSourceGroups": ["helper-*", "joint-*"],
    }
    sculpt_flat_pectoral(result["mesh"])
    result["source"]["shape"]["customSculpt"] = {
        "name": "flat-pectoral-v1",
        "implementation": "prepare_model.py:sculpt_flat_pectoral",
        "license": "CC0-1.0",
        "description": "Analytic front-chest depth cap, six adjacency smoothing passes, and a 24 mm masked lower-pectoral lift. Applied after rest-rig construction; topology, UVs, source vertex IDs, bones and skin weights are unchanged.",
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, separators=(",", ":")) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path, help="MakeHuman checkout at the pinned commit")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).with_name("model") / "human_mesh.json",
    )
    args = parser.parse_args()
    build(args.source.resolve(), args.output.resolve())


if __name__ == "__main__":
    main()
