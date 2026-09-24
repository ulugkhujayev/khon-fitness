import AppKit
import SceneKit
import simd

struct Guide: Decodable {
    let name: String; let poses: [[String:[Float]]]; let steps: [[String]]
    let views: [[Float]]; let target: [Float]; let span: Float
    let reps: Bool; let focus: String; let loopStart: Int
}

struct HumanMeshFile: Decodable {
    struct Mesh: Decodable {
        let positions: [Float]; let normals: [Float]?
        let uvs: [Float]?; let triangles: [Int]; let sourceVertexIds: [Int]?
    }
    struct Bone: Decodable {
        let name: String; let parent: Int
        let head: [Float]; let tail: [Float]
        let restMatrix: [Float]; let inverseBindMatrix: [Float]
    }
    struct Skeleton: Decodable { let bones: [Bone] }
    struct Skin: Decodable {
        let maxInfluences: Int; let joints: [Int]; let weights: [Float]
        let influenceOffsets: [Int]?; let allJoints: [Int]?; let allWeights: [Float]?
    }
    struct Landmark: Decodable {
        let bone: String?; let endpoint: String?; let position: [Float]?; let direction: [Float]?
    }
    let mesh: Mesh; let skeleton: Skeleton; let skin: Skin; let landmarks: [String:Landmark]
}

typealias V = SIMD3<Float>
typealias M = simd_float4x4

func v(_ a:[Float]) -> V { V(a[0],a[1],a[2]) }
func nodev(_ a:V)->SCNVector3 { SCNVector3(a.x,a.y,a.z) }
func unit(_ a:V)->V { simd_length(a)>0.0001 ? simd_normalize(a) : V(0,1,0) }
func point(_ m:M,_ p:V)->V {
    let q=m*SIMD4<Float>(p.x,p.y,p.z,1); return V(q.x,q.y,q.z)
}
func vector(_ m:M,_ p:V)->V {
    let q=m*SIMD4<Float>(p.x,p.y,p.z,0); return V(q.x,q.y,q.z)
}
func matrix(_ rowMajor:[Float])->M {
    precondition(rowMajor.count==16,"Bone matrices must contain 16 values")
    return M(columns:(
        SIMD4(rowMajor[0],rowMajor[4],rowMajor[8],rowMajor[12]),
        SIMD4(rowMajor[1],rowMajor[5],rowMajor[9],rowMajor[13]),
        SIMD4(rowMajor[2],rowMajor[6],rowMajor[10],rowMajor[14]),
        SIMD4(rowMajor[3],rowMajor[7],rowMajor[11],rowMajor[15])
    ))
}
func color(_ hex:Int)->NSColor {
    NSColor(srgbRed:CGFloat((hex>>16)&255)/255,green:CGFloat((hex>>8)&255)/255,blue:CGFloat(hex&255)/255,alpha:1)
}
func material(_ hex:Int,_ roughness:Float=0.9)->SCNMaterial {
    let m=SCNMaterial();m.diffuse.contents=color(hex);m.lightingModel = .physicallyBased
    m.roughness.contents=roughness;m.metalness.contents=0.0;m.isDoubleSided=true;return m
}
func unlitMaterial(_ hex:Int)->SCNMaterial {
    let m=SCNMaterial();m.diffuse.contents=color(hex);m.lightingModel = .constant;m.isDoubleSided=true;return m
}
let eyeMaterial=material(0x45494C,0.88)

// MARK: Illustration shading
// The figure imitates an anatomy-atlas drawing: near-white skin, thin gray
// contour strokes, soft gray grooves between muscle groups, and fine fiber
// lines inside the red-orange targets. bake_anatomy.py writes the anatomy map
// in the mesh's UV space. Its channels are distances in millimetres, so the
// shader can draw strokes of constant screen width at any camera scale:
//   R: distance to a major muscle-group border (0.1 mm per step, 25.5 mm max)
//   G: distance to a minor border, such as abdominal segments or serratus
//   B: triangle wave across muscle fibers, 0 on a fiber line
// Texture coordinate channels feed the shader per vertex:
//   0: mesh UVs for the anatomy map
//   1: (highlight intensity, shorts coverage), per frame, via the multiply slot
//   2: (outline scale, highlight region shape), via the emission slot
//   3: (hair amount, unused), via the ambient slot
// Lambert ignores unused slots, so each channel rides on a slot it samples;
// the shader resets those slots afterwards.
struct Illustration {
    // Outline color, linear light (about #33373B).
    static let lineColor="0.034, 0.038, 0.043"
    // Stroke half-widths in pixels and darkness at full strength.
    static let majorLine:(width:Float,darkness:Float)=(0.45,0.66)
    static let minorLine:(width:Float,darkness:Float)=(0.40,0.48)
    static let groove:(radiusMM:Float,darkness:Float)=(3.0,0.18)
    static let fiber:(width:Float,darkness:Float)=(0.42,0.30)
    // Muscle relief: each border is a groove; the muscle rises from it in a
    // rounded ease-out over widthMM to heightMM. The shader bends the normal,
    // so bellies stay white on the lit side and turn gray toward each border.
    static let relief:(heightMM:Float,widthMM:Float)=(7.0,22.0)
    // SceneKit screen-space ambient occlusion: gray in armpits, folds, contacts.
    static let ambientOcclusion:(intensity:Float,radius:Float)=(1.0,0.06)
    // Outline hull thickness in pixels at the packaged 400-pixel frame height.
    static let outlinePixels:Float=1.15
    // Target colors in linear light, and the threshold of the crisp target
    // contour on the 0...1 region value, with a darker red rim on the contour.
    static let accent="0.80, 0.14, 0.055",shortsAccent="0.52, 0.075, 0.04"
    static let skin="0.86, 0.86, 0.85",hair="0.10, 0.095, 0.09"
    // Smoothing passes over the surface before the threshold (about 4 cm).
    static let highlightSmoothing=24
    static let highlightEdge:(low:Float,high:Float,rimDarkness:Float)=(0.18,0.36,0.18)
}
// Review override, for example KHON_ILLUSTRATION=majorDark=0.6,reliefHeight=0
let illustrationOverrides:[String:Float]={
    var result=[String:Float]()
    for item in (ProcessInfo.processInfo.environment["KHON_ILLUSTRATION"] ?? "").split(separator:",") {
        let pair=item.split(separator:"=");if pair.count==2,let value=Float(pair[1]) { result[String(pair[0])]=value }
    }
    return result
}()
func tuned(_ key:String,_ value:Float)->Float { illustrationOverrides[key] ?? value }
func whiteImage()->NSImage {
    let image=NSImage(size:NSSize(width:2,height:2));image.lockFocus();NSColor.white.setFill()
    NSRect(x:0,y:0,width:2,height:2).fill();image.unlockFocus();return image
}
let anatomyMapPath=ProcessInfo.processInfo.environment["KHON_ANATOMY_MAP"] ?? "tools/stretch-demo/model/anatomy_map.png"
let figureSurfaceShader="""
#pragma arguments
texture2d<float> anatomyMap;
float majorWidth;
float majorDark;
float minorWidth;
float minorDark;
float grooveRadius;
float grooveDark;
float fiberWidth;
float fiberDark;
float reliefHeight;
float reliefWidth;
float pixelMM;
float hlLow;
float hlHigh;
float rimDark;
#pragma body
constexpr sampler smp(filter::linear, address::clamp_to_edge);
float3 a = anatomyMap.sample(smp, _surface.diffuseTexcoord).rgb;
float2 data = _surface.multiplyTexcoord;
float shape = _surface.emissionTexcoord.y;
// Crisp muscle contour: threshold the full-strength region, then fade it in
// with the phase level (intensity / shape).
float level = clamp(data.x / max(shape, 1e-3), 0.0, 1.0);
float edge = smoothstep(hlLow, hlHigh, shape);
float strength = edge * level;
float rim = (1.0 - smoothstep(0.0, 1.3, abs(shape - 0.5 * (hlLow + hlHigh)) / max(fwidth(shape), 1e-4))) * level;
float coverWidth = max(fwidth(data.y), 1e-4);
float skin = 1.0 - smoothstep(0.5 - coverWidth, 0.5 + coverWidth, data.y);
float hair = smoothstep(0.35, 0.65, _surface.ambientTexcoord.x);
float3 skinColor = mix(float3(SKIN), float3(HAIR), hair);
skin *= 1.0 - hair;
float dMajor = a.r * 25.5;
float dMinor = a.g * 25.5;
// The camera is orthographic, so one pixel covers pixelMM everywhere.
float pxMajor = dMajor / pixelMM;
float pxMinor = dMinor / pixelMM;
float lineMajor = (1.0 - smoothstep(majorWidth - 0.5, majorWidth + 0.5, pxMajor)) * step(dMajor, 25.0);
float lineMinor = (1.0 - smoothstep(minorWidth - 0.5, minorWidth + 0.5, pxMinor)) * step(dMinor, 25.0);
float groove = max(exp(-(dMajor * dMajor) / (grooveRadius * grooveRadius)), 0.6 * exp(-(dMinor * dMinor) / (grooveRadius * grooveRadius * 0.5)));
float tw = a.b;
float fiber = (1.0 - smoothstep(fiberWidth - 0.5, fiberWidth + 0.5, tw / max(fwidth(tw), 1e-4))) * step(tw, 0.98);
// Surface-gradient bump mapping without tangents (Mikkelsen 2010).
// Rounded bellies: height eases out from each border over reliefWidth.
float uMajor = 1.0 - min(dMajor / reliefWidth, 1.0);
float uMinor = 1.0 - min(dMinor / (reliefWidth * 0.7), 1.0);
float h = (1.0 - uMajor * uMajor) + 0.5 * (1.0 - uMinor * uMinor);
h *= reliefHeight * 0.001 * skin;
float3 dpdx = dfdx(_surface.position), dpdy = dfdy(_surface.position);
float3 n = normalize(_surface.normal);
float3 r1 = cross(dpdy, n), r2 = cross(n, dpdx);
float det = dot(dpdx, r1);
float3 grad = sign(det) * (dfdx(h) * r1 + dfdy(h) * r2);
_surface.normal = normalize(abs(det) * n - grad);
float shade = 1.0 - groove * grooveDark * skin;
shade *= 1.0 - lineMajor * majorDark * mix(0.35, 1.0, skin);
shade *= 1.0 - lineMinor * minorDark * skin;
shade *= 1.0 - fiber * fiberDark * strength;
float3 accent = mix(float3(ACCENT), float3(SHORTS_ACCENT), 1.0 - skin);
float3 base = mix(_surface.diffuse.rgb, skinColor, max(skin, hair));
float3 albedo = mix(base, accent, strength);
albedo *= 1.0 - rim * rimDark;
_surface.diffuse = float4(albedo * shade, 1.0);
_surface.ambient = _surface.diffuse;
_surface.multiply = float4(1.0);
_surface.emission = float4(0.0);
"""
let outlineGeometryShader="""
#pragma arguments
float outlineWidth;
#pragma body
_geometry.position.xyz += _geometry.normal * (outlineWidth * _geometry.texcoords[2].x);
"""
let outlineSurfaceShader="""
#pragma body
// Replace, not multiply: the hull shares the figure's vertex colors.
_surface.diffuse = float4(float3(LINE_COLOR), 1.0);
"""
func makeFigureMaterial()->SCNMaterial {
    let m=SCNMaterial();m.lightingModel = .lambert;m.isDoubleSided=true
    let white=whiteImage()
    m.diffuse.contents=white;m.diffuse.mappingChannel=0
    m.multiply.contents=white;m.multiply.mappingChannel=1
    // Lambert ignores the specular slot, so channel 2 rides on emission, zeroed below.
    m.emission.contents=white;m.emission.mappingChannel=2
    // Channel 3 rides on the ambient slot; the shader restores ambient = diffuse.
    m.ambient.contents=white;m.ambient.mappingChannel=3
    guard let map=NSImage(contentsOfFile:anatomyMapPath) else { fatalError("Missing anatomy map: \(anatomyMapPath)") }
    let property=SCNMaterialProperty(contents:map);property.mipFilter = .none;property.minificationFilter = .linear;property.magnificationFilter = .linear
    property.wrapS = .clamp;property.wrapT = .clamp
    m.setValue(property,forKey:"anatomyMap")
    let values:[String:Float]=["majorWidth":Illustration.majorLine.width,"majorDark":Illustration.majorLine.darkness,
        "minorWidth":Illustration.minorLine.width,"minorDark":Illustration.minorLine.darkness,
        "grooveRadius":Illustration.groove.radiusMM,"grooveDark":Illustration.groove.darkness,
        "fiberWidth":Illustration.fiber.width,"fiberDark":Illustration.fiber.darkness,
        "reliefHeight":Illustration.relief.heightMM,"reliefWidth":Illustration.relief.widthMM,
        "hlLow":Illustration.highlightEdge.low,"hlHigh":Illustration.highlightEdge.high,"rimDark":Illustration.highlightEdge.rimDarkness]
    for (key,value) in values { m.setValue(NSNumber(value:tuned(key,value)),forKey:key) }
    let shader=figureSurfaceShader.replacingOccurrences(of:"SHORTS_ACCENT",with:Illustration.shortsAccent).replacingOccurrences(of:"ACCENT",with:Illustration.accent)
        .replacingOccurrences(of:"SKIN",with:Illustration.skin).replacingOccurrences(of:"HAIR",with:Illustration.hair)
    m.shaderModifiers=[.surface:shader];return m
}
func makeOutlineMaterial()->SCNMaterial {
    let m=SCNMaterial();m.lightingModel = .constant;m.cullMode = .front;m.isDoubleSided=false
    m.setValue(NSNumber(value:Float(0.004)),forKey:"outlineWidth")
    m.shaderModifiers=[.geometry:outlineGeometryShader,.surface:outlineSurfaceShader.replacingOccurrences(of:"LINE_COLOR",with:Illustration.lineColor)];return m
}
// Created on first use, so pose probes that never render do not need the map.
var sharedFigureMaterial:SCNMaterial?=nil
var sharedOutlineMaterial:SCNMaterial?=nil
func figureMaterial()->SCNMaterial {
    if let m=sharedFigureMaterial { return m }
    let m=makeFigureMaterial();sharedFigureMaterial=m;return m
}
func outlineMaterial()->SCNMaterial {
    if let m=sharedOutlineMaterial { return m }
    let m=makeOutlineMaterial();sharedOutlineMaterial=m;return m
}

func smoothstep(_ edge0:Float,_ edge1:Float,_ value:Float)->Float {
    let t=min(1,max(0,(value-edge0)/(edge1-edge0)));return t*t*(3-2*t)
}

// This seam direction is deliberately outside every authored torso and limb
// direction. Building a swing from it avoids the 180-degree pole changes that
// a projected up vector produces when a limb passes through straight.
func directionFrame(_ axis:V)->simd_float3x3 {
    let seed=unit(V(0.398006,0.002228,0.917380)),x=unit(axis)
    let seedReference=unit(simd_cross(seed,V(0,1,0)))
    let swing=simd_quatf(from:seed,to:x)
    let y=unit(swing.act(seedReference)),z=unit(simd_cross(x,y))
    return simd_float3x3(columns:(x,y,z))
}

// Keep the mesh's proportions when an authored landmark is too far away. The
// connected weighted surface still bends toward the guide without inflating a
// limb to bridge a bad pose segment.
func segmentTransform(_ sourceA:V,_ sourceB:V,_ targetA:V,_ targetB:V,_ sourceReference:V,_ targetReference:V)->M {
    let sourceLength=max(0.0001,simd_length(sourceB-sourceA))
    let rawScale=simd_length(targetB-targetA)/sourceLength
    let scale=min(1.15,max(0.90,rawScale))
    let sourceAxis=unit(sourceB-sourceA),targetAxis=unit(targetB-targetA)
    let sourceFrame=directionFrame(sourceAxis),targetFrame=directionFrame(targetAxis)
    let swing=targetFrame*sourceFrame.transpose
    let mappedReference=unit(swing*(sourceReference-sourceAxis*simd_dot(sourceReference,sourceAxis)))
    let desiredReference=unit(targetReference-targetAxis*simd_dot(targetReference,targetAxis))
    let twistAngle=atan2(simd_dot(targetAxis,simd_cross(mappedReference,desiredReference)),simd_dot(mappedReference,desiredReference))
    let twist=simd_float3x3(simd_quatf(angle:twistAngle,axis:targetAxis))
    let linear=(twist*swing)*scale
    let translation=targetA-linear*sourceA
    return M(columns:(SIMD4(linear.columns.0,0),SIMD4(linear.columns.1,0),SIMD4(linear.columns.2,0),SIMD4(translation,1)))
}

func swingTransform(_ sourceA:V,_ sourceB:V,_ targetA:V,_ targetB:V)->M {
    let sourceLength=max(0.0001,simd_length(sourceB-sourceA))
    let scale=min(1.15,max(0.90,simd_length(targetB-targetA)/sourceLength))
    let swing=directionFrame(targetB-targetA)*directionFrame(sourceB-sourceA).transpose
    let linear=swing*scale,translation=targetA-linear*sourceA
    return M(columns:(SIMD4(linear.columns.0,0),SIMD4(linear.columns.1,0),SIMD4(linear.columns.2,0),SIMD4(translation,1)))
}

func spherical(_ a:V,_ b:V,_ t:Float)->V {
    let av=unit(a),bv=unit(b),dot=max(-1.0,min(1.0,simd_dot(av,bv)))
    if dot>0.999 { return unit(av*(1-t)+bv*t) }
    if dot < -0.999 {
        let seed=abs(av.y)<0.8 ? V(0,1,0):V(0,0,1),orthogonal=unit(simd_cross(av,seed))
        return av*cos(Float.pi*t)+orthogonal*sin(Float.pi*t)
    }
    let angle=acos(dot)
    return av*(sin((1-t)*angle)/sin(angle))+bv*(sin(t*angle)/sin(angle))
}
func joint(_ root:V,_ tip:V,_ pole:V,_ upper:Float,_ lower:Float)->V {
    let distance=max(0.001,simd_length(tip-root)),axis=unit(tip-root)
    let along=(upper*upper-lower*lower+distance*distance)/(2*distance)
    let height=sqrt(max(0,upper*upper-along*along))
    var bend=pole-root-axis*simd_dot(pole-root,axis)
    if simd_length(bend)<0.0001 { bend=simd_cross(axis,V(0,0,1)) }
    return root+axis*along+unit(bend)*height
}
func plantedFootDirection(_ a:V,_ b:V,_ t:Float,_ side:String)->V {
    let horizontalA=max(0.0001,simd_length(V(a.x,0,a.z))),horizontalB=max(0.0001,simd_length(V(b.x,0,b.z)))
    let angleA=atan2(a.z,a.x),angleB=atan2(b.z,b.x)
    var delta=(angleB-angleA).truncatingRemainder(dividingBy:2*Float.pi)
    if delta>Float.pi { delta-=2*Float.pi }
    if delta < -Float.pi { delta+=2*Float.pi }
    if abs(abs(delta)-Float.pi)<0.001 { delta=side=="L" ? -Float.pi:Float.pi }
    let angle=angleA+delta*t,horizontal=horizontalA*(1-t)+horizontalB*t
    return unit(V(cos(angle)*horizontal,a.y*(1-t)+b.y*t,sin(angle)*horizontal))
}
func interpolate(_ a:[String:[Float]],_ b:[String:[Float]],_ t:Float,_ key:String)->[String:V] {
    var p=[String:V]()
    for (j,value) in a {p[j]=v(value)*(1-t)+v(b[j]!)*t}
    let width=spherical(v(a["shoulderR"]!)-v(a["shoulderL"]!),v(b["shoulderR"]!)-v(b["shoulderL"]!),t)
    p["shoulderL"]=p["shoulder"]!-width*0.19;p["shoulderR"]=p["shoulder"]!+width*0.19
    for side in ["L","R"] {
        for names in [["hip","knee","ankle"],["shoulder","elbow","wrist"]] {
            let r=names[0]+side,j=names[1]+side,e=names[2]+side
            let upper=simd_length(v(a[j]!)-v(a[r]!))*(1-t)+simd_length(v(b[j]!)-v(b[r]!))*t
            let lower=simd_length(v(a[e]!)-v(a[j]!))*(1-t)+simd_length(v(b[e]!)-v(b[j]!))*t
            if key=="wgs" && side=="L" && names[0]=="shoulder" {
                p[j]=p[r]!+spherical(v(a[j]!)-v(a[r]!),v(b[j]!)-v(b[r]!),t)*upper
                let old=p[e]!
                p[e]=p[j]!+spherical(v(a[e]!)-v(a[j]!),v(b[e]!)-v(b[j]!),t)*lower
                p["hand"+side]=p["hand"+side]!+(p[e]!-old)
            } else {p[j]=joint(p[r]!,p[e]!,p[j]!,upper,lower)}
        }
        for names in [["ankle","toe"],["wrist","hand"]] {
            let root=names[0]+side,end=names[1]+side
            let sourceDirection=v(a[end]!)-v(a[root]!),targetDirection=v(b[end]!)-v(b[root]!)
            let length=simd_length(sourceDirection)*(1-t)+simd_length(targetDirection)*t
            let planted=names[0]=="ankle" && max(a[end]![1],b[end]![1])<0.10
            let direction=planted ? plantedFootDirection(sourceDirection,targetDirection,t,side):spherical(sourceDirection,targetDirection,t)
            p[end]=p[root]!+direction*length
        }
    }
    let sourceNeck=v(a["head"]!)-v(a["shoulder"]!),targetNeck=v(b["head"]!)-v(b["shoulder"]!)
    let neckLength=simd_length(sourceNeck)*(1-t)+simd_length(targetNeck)*t
    p["head"]=p["shoulder"]!+spherical(sourceNeck,targetNeck,t)*neckLength
    p["face"]=spherical(v(a["face"]!),v(b["face"]!),t)
    if let sourceUp=a["up"],let targetUp=b["up"] { p["up"]=spherical(v(sourceUp),v(targetUp),t) }
    if let sourcePelvisUp=a["pelvisUp"],let targetPelvisUp=b["pelvisUp"] { p["pelvisUp"]=spherical(v(sourcePelvisUp),v(targetPelvisUp),t) }
    return p
}

final class HumanModel {
    let file:HumanMeshFile;let positions:[V];let normals:[V];let triangles:[Int32]
    let boneNames:[String];let restMatrices:[M];let inverseBindMatrices:[M]
    let restLandmarks:[String:V]
    // Static texture channels for the illustration shader, built once.
    var uvSource:SCNGeometrySource?=nil
    var outlineValues:[Float]?=nil
    var hairValues:[Float]?=nil
    var hairChannel:SCNGeometrySource?=nil
    var smoothedShapes=[String:[Float]]()
    // Neighbors over the welded surface, so UV seams do not split a field.
    lazy var weldedNeighbors:(ids:[Int],neighbors:[[Int]])={
        let ids=file.mesh.sourceVertexIds ?? Array(positions.indices)
        let count=(ids.max() ?? 0)+1
        var sets=Array(repeating:Set<Int>(),count:count)
        for t in stride(from:0,to:triangles.count,by:3) {
            let a=ids[Int(triangles[t])],b=ids[Int(triangles[t+1])],c=ids[Int(triangles[t+2])]
            sets[a].formUnion([b,c]);sets[b].formUnion([a,c]);sets[c].formUnion([a,b])
        }
        return (ids,sets.map { Array($0) })
    }()
    // Fill unknown values (-1) from known neighbors, one ring per pass.
    func dilateOverSurface(_ values:[Float],iterations:Int)->[Float] {
        let (ids,neighbors)=weldedNeighbors
        var welded=Array(repeating:Float(-1),count:neighbors.count)
        for (i,id) in ids.enumerated() { welded[id]=max(welded[id],values[i]) }
        for _ in 0..<iterations {
            var next=welded
            for v in welded.indices where welded[v]<0 {
                for n in neighbors[v] where welded[n]>=0 { next[v]=max(next[v],welded[n]) }
            }
            welded=next
        }
        return ids.map { welded[$0] }
    }
    func smoothOverSurface(_ values:[Float],iterations:Int)->[Float] {
        let (ids,neighbors)=weldedNeighbors
        var welded=Array(repeating:Float(0),count:neighbors.count)
        for (i,id) in ids.enumerated() { welded[id]=values[i] }
        for _ in 0..<iterations {
            var next=welded
            for v in welded.indices where !neighbors[v].isEmpty {
                var sum:Float=0;for n in neighbors[v] { sum+=welded[n] }
                next[v]=0.5*welded[v]+0.5*sum/Float(neighbors[v].count)
            }
            welded=next
        }
        return ids.map { welded[$0] }
    }
    // Texture channel 3: short hair, so the head reads as a person, not a mannequin.
    func hairSource()->SCNGeometrySource {
        if let hairChannel { return hairChannel }
        var values=[Float]();values.reserveCapacity(positions.count*2)
        for i in positions.indices { values.append(hairAmount(i));values.append(0) }
        let source=HumanModel.texcoordSource(values);hairChannel=source;return source
    }

    init(path:String) throws {
        let decoded=try JSONDecoder().decode(HumanMeshFile.self,from:Data(contentsOf:URL(fileURLWithPath:path)))
        file=decoded
        precondition(decoded.mesh.positions.count%3==0,"Mesh positions are not xyz triples")
        positions=stride(from:0,to:decoded.mesh.positions.count,by:3).map { V(decoded.mesh.positions[$0],decoded.mesh.positions[$0+1],decoded.mesh.positions[$0+2]) }
        if let sourceNormals=decoded.mesh.normals,sourceNormals.count==decoded.mesh.positions.count {
            normals=stride(from:0,to:sourceNormals.count,by:3).map { unit(V(sourceNormals[$0],sourceNormals[$0+1],sourceNormals[$0+2])) }
        } else { normals=Array(repeating:V(0,1,0),count:decoded.mesh.positions.count/3) }
        precondition(decoded.skin.maxInfluences==4,"Renderer requires four skin influence slots")
        precondition(decoded.skin.joints.count==decoded.mesh.positions.count/3*4 && decoded.skin.weights.count==decoded.mesh.positions.count/3*4,"Skin arrays do not match mesh vertices")
        if let offsets=decoded.skin.influenceOffsets,let joints=decoded.skin.allJoints,let weights=decoded.skin.allWeights {
            precondition(offsets.count==decoded.mesh.positions.count/3+1 && joints.count==weights.count && offsets.last==joints.count,"Full skin influence arrays do not match mesh vertices")
        }
        precondition(decoded.mesh.triangles.count%3==0,"Triangle index count must be divisible by three")
        triangles=decoded.mesh.triangles.map { Int32($0) }
        boneNames=decoded.skeleton.bones.map(\.name)
        let boneIndex=Dictionary(uniqueKeysWithValues:boneNames.enumerated().map { ($0.element,$0.offset) })
        restMatrices=decoded.skeleton.bones.map { matrix($0.restMatrix) }
        inverseBindMatrices=decoded.skeleton.bones.map { matrix($0.inverseBindMatrix) }
        var landmarks=[String:V]()
        for (name,landmark) in decoded.landmarks {
            if let position=landmark.position { landmarks[name]=v(position) }
            else if let boneName=landmark.bone,let index=boneIndex[boneName] {
                let bone=decoded.skeleton.bones[index]
                if landmark.endpoint=="tail" { landmarks[name]=v(bone.tail) }
                else if landmark.endpoint=="midpoint" { landmarks[name]=(v(bone.head)+v(bone.tail))*0.5 }
                else { landmarks[name]=v(bone.head) }
            }
        }
        // MakeHuman's generic spine landmark sits below the arm sockets. Use
        // the actual limb roots so shoulder and hip skin weights meet cleanly.
        func boneHead(_ name:String)->V? { boneIndex[name].map { v(decoded.skeleton.bones[$0].head) } }
        if let left=boneHead("upperarm01.L"),let right=boneHead("upperarm01.R") {
            landmarks["shoulderL"]=left;landmarks["shoulderR"]=right;landmarks["shoulder"]=(left+right)*0.5
        }
        if let left=boneHead("upperleg01.L"),let right=boneHead("upperleg01.R") {
            landmarks["hipL"]=left;landmarks["hipR"]=right;landmarks["hip"]=(left+right)*0.5
        }
        restLandmarks=landmarks
        if let sourceUVs=decoded.mesh.uvs,sourceUVs.count==positions.count*2 {
            // OBJ UVs grow upward; texture rows grow downward.
            var flipped=[Float]();flipped.reserveCapacity(sourceUVs.count)
            for i in stride(from:0,to:sourceUVs.count,by:2) { flipped.append(sourceUVs[i]);flipped.append(1-sourceUVs[i+1]) }
            uvSource=HumanModel.texcoordSource(flipped)
        }
        let required=["hip","chest","shoulder","head","hipL","hipR","kneeL","kneeR","ankleL","ankleR","toeL","toeR","shoulderL","shoulderR","elbowL","elbowR","wristL","wristR","handL","handR"]
        precondition(required.allSatisfy { restLandmarks[$0] != nil },"human_mesh.json is missing pose-control landmarks")
    }

    static func texcoordSource(_ values:[Float])->SCNGeometrySource {
        let data=values.withUnsafeBufferPointer { Data(buffer:$0) }
        return SCNGeometrySource(data:data,semantic:.texcoord,vectorCount:values.count/2,usesFloatComponents:true,componentsPerVector:2,bytesPerComponent:4,dataOffset:0,dataStride:8)
    }
    // Thin the outline on fingers, toes, and the face so they keep their shape.
    func outlineScaleValues()->[Float] {
        if let outlineValues { return outlineValues }
        var values=[Float]();values.reserveCapacity(positions.count)
        for i in positions.indices {
            let hand=boneWeight(i,["finger","thumb","metacarpal","wrist"])
            let foot=boneWeight(i,["toe"])
            let face=boneWeight(i,["jaw","oris","oculi","orbicularis","levator","risorius","temporalis","special","tongue","eye"])
            values.append(max(0.30,1-0.62*min(1,hand)-0.45*min(1,foot)-0.40*min(1,face)))
        }
        outlineValues=values;return values
    }
    func side(_ lowerName:String)->String? {
        if lowerName.hasSuffix(".l") || lowerName.hasSuffix("_l") || lowerName.contains("left") { return "L" }
        if lowerName.hasSuffix(".r") || lowerName.hasSuffix("_r") || lowerName.contains("right") { return "R" }
        return nil
    }
    func influenceRange(_ vertex:Int)->Range<Int> {
        if let offsets=file.skin.influenceOffsets { return offsets[vertex]..<offsets[vertex+1] }
        return vertex*4..<vertex*4+4
    }
    func influenceJoint(_ offset:Int)->Int { file.skin.allJoints?[offset] ?? file.skin.joints[offset] }
    func influenceWeight(_ offset:Int)->Float { file.skin.allWeights?[offset] ?? file.skin.weights[offset] }
    func boneDeltas(_ pose:[String:V])->[M] {
        let r=restLandmarks
        let sourceRight=unit(r["shoulderR"]!-r["shoulderL"]!),targetRight=unit(pose["shoulderR"]!-pose["shoulderL"]!)
        let sourceUp=unit(r["shoulder"]!-r["hip"]!),targetUp=unit(pose["shoulder"]!-pose["hip"]!)
        let sourceFront=unit(simd_cross(sourceUp,sourceRight)),targetFront=unit(simd_cross(targetUp,targetRight))
        let lowerTorso=segmentTransform(r["hip"]!,r["chest"]!,pose["hip"]!,pose["chest"]!,sourceFront,targetFront)
        let upperTorso=segmentTransform(r["chest"]!,r["shoulder"]!,pose["chest"]!,pose["shoulder"]!,sourceFront,targetFront)
        let pelvis=segmentTransform(r["hipL"]!,r["hipR"]!,pose["hipL"]!,pose["hipR"]!,r["shoulder"]!-r["hip"]!,pose["pelvisUp"] ?? pose["shoulder"]!-pose["hip"]!)
        let head=segmentTransform(r["shoulder"]!,r["head"]!,pose["shoulder"]!,pose["head"]!,sourceFront,unit(pose["face"] ?? targetFront))
        var deltas=Array(repeating:matrix_identity_float4x4,count:file.skeleton.bones.count)
        for i in file.skeleton.bones.indices {
            let bone=file.skeleton.bones[i],name=bone.name.lowercased()
            var selected:M?
            if let s=side(name) {
                let rh=r["hip"+s]!,rk=r["knee"+s]!,ra=r["ankle"+s]!,rt=r["toe"+s]!
                let ph=pose["hip"+s]!,pk=pose["knee"+s]!,pa=pose["ankle"+s]!,pt=pose["toe"+s]!
                let rs=r["shoulder"+s]!,re=r["elbow"+s]!,rw=r["wrist"+s]!,rhand=r["hand"+s]!
                let ps=pose["shoulder"+s]!,pe=pose["elbow"+s]!,pw=pose["wrist"+s]!,phand=pose["hand"+s]!
                if name.contains("upperleg") || name.contains("thigh") {
                    selected=swingTransform(rh,rk,ph,pk)
                } else if name.contains("lowerleg") || name.contains("shin") || name.contains("calf") {
                    selected=swingTransform(rk,ra,pk,pa)
                } else if name.contains("foot") || name.contains("toe") {
                    selected=swingTransform(ra,rt,pa,pt)
                } else if name.contains("clavicle") || name.contains("shoulder") {
                    selected=segmentTransform(r["chest"]!,rs,pose["chest"]!,ps,r["shoulder"]!-r["hip"]!,pose["shoulder"]!-pose["hip"]!)
                } else if name.contains("upperarm") || name.contains("arm_upper") {
                    selected=swingTransform(rs,re,ps,pe)
                } else if name.contains("lowerarm") || name.contains("forearm") || name.contains("arm_lower") {
                    selected=swingTransform(re,rw,pe,pw)
                } else if name.contains("wrist") || name.contains("hand") {
                    selected=swingTransform(rw,rhand,pw,phand)
                }
            }
            if selected==nil {
                if name.contains("head") || name.contains("neck") || name.contains("jaw") || name.contains("eye") { selected=head }
                else if name.contains("spine") || name.contains("chest") || name.contains("rib") || name.contains("breast") {
                    selected=(v(bone.head).y+v(bone.tail).y)*0.5<r["chest"]!.y ? lowerTorso:upperTorso
                } else if name.contains("pelvis") || name=="root" || name.contains("hip") || name.contains("butt") { selected=pelvis }
            }
            if let selected { deltas[i]=selected }
            else if bone.parent>=0 && bone.parent<i { deltas[i]=deltas[bone.parent] }
            else { deltas[i]=pelvis }
        }
        return deltas
    }

    func boneWeight(_ vertex:Int,_ tokens:[String],side requestedSide:String?=nil)->Float {
        var total:Float=0
        for offset in influenceRange(vertex) {
            let index=influenceJoint(offset)
            guard index>=0,index<boneNames.count else { continue }
            let name=boneNames[index].lowercased()
            if tokens.contains(where:{name.contains($0)}) && (requestedSide==nil || side(name)==requestedSide) { total += influenceWeight(offset) }
        }
        return total
    }
    func along(_ value:V,_ a:V,_ b:V)->Float {
        let d=b-a;return simd_dot(value-a,d)/max(0.0001,simd_dot(d,d))
    }
    func shortsAmount(_ vertex:Int)->Float {
        // Rest-space color follows the weighted body through every pose.
        let p=positions[vertex],r=restLandmarks
        let waist=r["hip"]!.y+0.13
        let hem=min(r["kneeL"]!.y,r["kneeR"]!.y)-0.06
        let legWeight=boneWeight(vertex,["upperleg","lowerleg"],side:nil)
        let armWeight=boneWeight(vertex,["clavicle","shoulder","upperarm","lowerarm","wrist","hand","metacarpal","finger","thumb"])
        guard legWeight>0.18 || (p.y>r["hip"]!.y-0.12 && armWeight<0.35) else { return 0 }
        // A short color fade hides jagged edges from the low-poly waist and knees.
        let upper=1-smoothstep(waist-0.025,waist+0.015,p.y)
        let lower=smoothstep(hem-0.015,hem+0.025,p.y)
        return upper*lower
    }
    func isShorts(_ vertex:Int)->Bool { shortsAmount(vertex)>0.5 }
    // Short cropped hair above a hairline that runs from the forehead down to the nape.
    func hairAmount(_ vertex:Int)->Float {
        if let hairValues { return hairValues[vertex] }
        var values=[Float]()
        let head=restLandmarks["head"]!
        for i in positions.indices {
            let p=positions[i],weight=boneWeight(i,["head"])
            let dx=p.x-head.x
            let line=dx>0 ? head.y-0.004+dx*0.40:head.y-0.004+dx*0.75
            let ear=smoothstep(0.064,0.074,abs(p.z))*(1-smoothstep(1.655,1.675,p.y))*smoothstep(-0.03,0.0,dx+0.02)
            values.append(smoothstep(0.55,0.85,weight)*smoothstep(line-0.004,line+0.006,p.y)*(1-ear))
        }
        hairValues=values;return values[vertex]
    }
    func phaseStrength(_ values:[Float],_ progress:Float)->Float {
        precondition(!values.isEmpty,"Highlight phases cannot be empty")
        let p=min(Float(values.count-1),max(0,progress)),lower=Int(p.rounded(.down)),upper=min(values.count-1,lower+1)
        let t=p-Float(lower)
        return values[lower]*(1-t)+values[upper]*t
    }
    func softBand(_ value:Float,_ lower:Float,_ upper:Float,_ edge:Float)->Float {
        smoothstep(lower-edge,lower+edge,value)*(1-smoothstep(upper-edge,upper+edge,value))
    }
    func accentIntensity(_ vertex:Int,_ exercise:String,_ focus:String,_ progress:Float)->Float {
        accentLevels(vertex,exercise,focus,progress).intensity
    }
    // intensity: the phase-weighted highlight used everywhere else.
    // shape: the same regions at full strength. The shader draws a crisp muscle
    // contour where shape crosses a threshold and fills it with intensity/shape.
    func accentLevels(_ vertex:Int,_ exercise:String,_ focus:String,_ progress:Float)->(intensity:Float,shape:Float) {
        let p=positions[vertex],r=restLandmarks
        let s=p.z<(r["hipL"]!.z+r["hipR"]!.z)*0.5 ? "L":"R"
        let thigh=boneWeight(vertex,["upperleg","thigh"],side:s)
        let lowerLeg=boneWeight(vertex,["lowerleg","shin","calf"],side:s)
        let shoulder=boneWeight(vertex,["clavicle","shoulder","upperarm"],side:s)
        let torso=boneWeight(vertex,["spine","chest","rib","breast","pelvis"])
        let pelvis=boneWeight(vertex,["pelvis","hip","butt"])
        let hip=r["hip"+s]!,knee=r["knee"+s]!,ankle=r["ankle"+s]!,shoulderRoot=r["shoulder"+s]!
        let thighT=along(p,hip,knee),calfT=along(p,knee,ankle),upperArmT=along(p,shoulderRoot,r["elbow"+s]!)
        let torsoT=along(p,r["hip"]!,r["shoulder"]!)
        let thighAxis=hip+(knee-hip)*thighT,calfAxis=knee+(ankle-knee)*calfT
        let thighBack=1-smoothstep(-0.025,0.035,p.x-thighAxis.x)
        let calfBack=1-smoothstep(-0.022,0.030,p.x-calfAxis.x)
        let back=1-smoothstep(-0.020,0.035,p.x-r["hip"]!.x)
        let front=smoothstep(-0.015,0.045,p.x-r["hip"]!.x)
        let sideReach=s=="L" ? smoothstep(0.015,0.070,-p.z):smoothstep(0.015,0.070,p.z)
        let medialOffset=s=="L" ? p.z-thighAxis.z:thighAxis.z-p.z
        let hamstring=thigh*smoothstep(0.20,0.52,thigh)*softBand(thighT,0.10,0.90,0.10)*thighBack
        let calf=lowerLeg*smoothstep(0.18,0.48,lowerLeg)*softBand(calfT,0.08,0.82,0.10)*calfBack
        let posteriorHip=max(thigh,pelvis)*softBand(thighT,-0.24,0.38,0.10)*softBand(p.y,hip.y-0.22,hip.y+0.10,0.045)*thighBack
        let frontHip=max(thigh,pelvis)*softBand(thighT,-0.18,0.34,0.10)*softBand(p.y,hip.y-0.18,hip.y+0.09,0.045)*front
        let adductor=thigh*smoothstep(0.20,0.50,thigh)*softBand(thighT,0.08,0.68,0.10)*smoothstep(-0.005,0.040,medialOffset)
        let rearShoulder=shoulder*smoothstep(0.18,0.48,shoulder)*softBand(upperArmT,-0.14,0.34,0.10)*(1-smoothstep(-0.020,0.045,p.x-shoulderRoot.x))
        let scapula=torso*smoothstep(0.18,0.48,torso)*softBand(torsoT,0.54,0.96,0.10)*back*sideReach
        let midBack=torso*smoothstep(0.20,0.50,torso)*softBand(torsoT,0.34,0.82,0.10)*back*sideReach
        let spinalBack=torso*smoothstep(0.22,0.52,torso)*softBand(torsoT,0.12,0.90,0.10)*back
        let abdomen=torso*smoothstep(0.22,0.52,torso)*softBand(torsoT,0.12,0.68,0.10)*front
        let chest=torso*smoothstep(0.18,0.48,torso)*softBand(torsoT,0.56,0.98,0.10)*front
        let oblique=torso*smoothstep(0.18,0.48,torso)*softBand(torsoT,0.20,0.72,0.10)*sideReach*smoothstep(-0.055,0.025,p.x-r["hip"]!.x)
        func sideMask(_ target:String,_ region:Float)->Float { target==s ? region:0 }
        var intensity:Float=0,shape:Float=0
        func add(_ region:Float,_ values:[Float]) {
            intensity=max(intensity,region*phaseStrength(values,progress))
            if values.contains(where:{$0>0}) { shape=max(shape,region) }
        }
        switch exercise {
        case "hipflexor": add(sideMask("R",frontHip),[0,0.48,1])
        case "shoulderir": add(sideMask("L",rearShoulder),[0,0.48,1])
        case "needle":
            add(sideMask("L",rearShoulder),[0,0.48,1]);add(sideMask("L",scapula),[0,0.42,0.92]);add(sideMask("L",midBack),[0,0.30,0.72])
        case "fold":
            add(hamstring,[0,0.68,1]);add(calf,[0,0.34,0.58])
        case "elephant":
            add(sideMask("R",hamstring),[0.18,1,0]);add(sideMask("R",calf),[0.14,0.92,0])
            add(sideMask("L",hamstring),[0.18,0,1]);add(sideMask("L",calf),[0.14,0,0.92])
        case "catcow":
            add(spinalBack,[0,1,0]);add(abdomen,[0,0,1]);add(chest,[0,0,0.72])
        case "ninety":
            add(sideMask("L",posteriorHip),[0,0.64,0.68,1]);add(sideMask("R",frontHip),[0,0,0.68,0.34])
        case "plow":
            add(hamstring,[0,0.38,0.72,1]);add(calf,[0,0.30,0.66,0.92])
            add(posteriorHip,[0,0,0.58,0.92]);add(spinalBack,[0,0,0.58,1])
        case "wgs":
            add(sideMask("R",frontHip),[0.18,0.48,0.62]);add(sideMask("R",calf),[0.14,0.42,0.56])
            add(sideMask("L",posteriorHip),[0.14,0.20,0.24]);add(sideMask("L",adductor),[0,0.68,0.74])
            add(sideMask("L",chest),[0,0,0.92]);add(sideMask("L",oblique),[0,0,0.86]);add(sideMask("L",midBack),[0,0,0.82])
        default: break
        }
        return (min(1,max(0,intensity)),min(1,max(0,shape)))
    }

    func geometry(_ pose:[String:V],exercise:String,focus:String,progress:Float=0)->SCNGeometry {
        var deltas=boneDeltas(pose)
        var skinMatrices=file.skeleton.bones.indices.map { deltas[$0]*restMatrices[$0]*inverseBindMatrices[$0] }
        func skinnedPoint(_ vertex:Int)->V {
            var result=V.zero,total:Float=0
            for offset in influenceRange(vertex) {
                let index=influenceJoint(offset),weight=influenceWeight(offset)
                if weight<=0 || index<0 || index>=skinMatrices.count { continue }
                result += point(skinMatrices[index],positions[vertex])*weight;total += weight
            }
            return total>0.0001 ? result:positions[vertex]
        }
        // Align the real sole surface, not the old abstract toe handle, with
        // the mat. Translating foot bone matrices preserves weighted seams.
        for s in ["L","R"] {
            let contact=1-smoothstep(0.03,0.50,pose["toe"+s]!.y)
            if contact<=0 { continue }
            var sole=Float.greatestFiniteMagnitude
            for vertex in positions.indices where boneWeight(vertex,["foot","toe"],side:s)>0.42 { sole=min(sole,skinnedPoint(vertex).y) }
            if sole.isFinite {
                let correction=min(0.08,max(-0.08,0.004-sole))*contact
                for i in boneNames.indices {
                    let name=boneNames[i].lowercased()
                    if side(name)==s && (name.contains("foot") || name.contains("toe")) { deltas[i].columns.3.y += correction }
                }
            }
        }
        for s in ["L","R"] {
            let contact=1-smoothstep(0.03,0.45,max(pose["wrist"+s]!.y,pose["hand"+s]!.y))
            if contact<=0 { continue }
            var surface=Float.greatestFiniteMagnitude
            let handTokens=["wrist","hand","metacarpal","finger","thumb"]
            for vertex in positions.indices where boneWeight(vertex,handTokens,side:s)>0.42 { surface=min(surface,skinnedPoint(vertex).y) }
            if surface.isFinite {
                let correction=min(0.06,max(-0.06,0.004-surface))*contact
                for i in boneNames.indices {
                    let name=boneNames[i].lowercased()
                    if side(name)==s && handTokens.contains(where:{name.contains($0)}) { deltas[i].columns.3.y += correction }
                }
            }
        }
        skinMatrices=file.skeleton.bones.indices.map { deltas[$0]*restMatrices[$0]*inverseBindMatrices[$0] }
        let normalMatrices=skinMatrices.map { m in
            simd_transpose(simd_inverse(simd_float3x3(columns:(V(m.columns.0.x,m.columns.0.y,m.columns.0.z),V(m.columns.1.x,m.columns.1.y,m.columns.1.z),V(m.columns.2.x,m.columns.2.y,m.columns.2.z)))))
        }
        var posedPositions=Array(repeating:SCNVector3Zero,count:positions.count),posedNormals=Array(repeating:SCNVector3Zero,count:positions.count)
        for i in positions.indices {
            var p=V.zero,n=V.zero,total:Float=0
            for offset in influenceRange(i) {
                let index=influenceJoint(offset),weight=influenceWeight(offset)
                if weight<=0 || index<0 || index>=skinMatrices.count { continue }
                p += point(skinMatrices[index],positions[i])*weight;n += normalMatrices[index]*normals[i]*weight;total += weight
            }
            if total<0.0001 { p=positions[i];n=normals[i] }
            posedPositions[i]=nodev(p);posedNormals[i]=nodev(unit(n))
        }
        // Linear-light albedo: near-white skin and a saturated red-orange target.
        let shorts=V(0.075,0.083,0.092),shortsTrim=V(0.13,0.14,0.15)
        let waist=restLandmarks["hip"]!.y+0.13
        let hem=min(restLandmarks["kneeL"]!.y,restLandmarks["kneeR"]!.y)-0.06
        var colors=[SIMD4<Float>]();colors.reserveCapacity(positions.count)
        // The shader mixes in the red-orange target from these channels.
        var channel=[Float](),extra=[Float]();channel.reserveCapacity(positions.count*2);extra.reserveCapacity(positions.count*2)
        let outline=outlineScaleValues()
        // Smooth both highlight fields over the surface. The shader thresholds
        // the smoothed shape, so each target ends in a rounded, tapered edge
        // instead of the straight cut of a band limit or the shorts hem.
        var intensity=[Float](),shape=[Float]();intensity.reserveCapacity(positions.count);shape.reserveCapacity(positions.count)
        for i in positions.indices { let levels=accentLevels(i,exercise,focus,progress);intensity.append(levels.intensity);shape.append(levels.shape) }
        let iterations=Illustration.highlightSmoothing
        // The phase level belongs to each region; spread it outward without
        // blending, so a resting region never picks up a neighbor's tint.
        let level=dilateOverSurface(positions.indices.map { shape[$0]>0.05 ? min(1,intensity[$0]/shape[$0]):-1 },iterations:iterations)
        if let cached=smoothedShapes[exercise] { shape=cached } else { shape=smoothOverSurface(shape,iterations:iterations);smoothedShapes[exercise]=shape }
        intensity=positions.indices.map { max(0,level[$0])*shape[$0] }
        for i in positions.indices {
            let coverage=shortsAmount(i)
            channel.append(intensity[i]);channel.append(coverage)
            extra.append(outline[i]);extra.append(shape[i])
            let band=positions[i].y>waist-0.018 || positions[i].y<hem+0.018
            let cloth=band ? shortsTrim:shorts
            // The color carries the cloth; the shader cuts the skin/cloth edge per pixel.
            colors.append(SIMD4(cloth.x,cloth.y,cloth.z,1))
        }
        let colorData=colors.withUnsafeBufferPointer { Data(buffer:$0) }
        let colorSource=SCNGeometrySource(data:colorData,semantic:.color,vectorCount:colors.count,usesFloatComponents:true,componentsPerVector:4,bytesPerComponent:4,dataOffset:0,dataStride:16)
        var sources=[SCNGeometrySource(vertices:posedPositions),SCNGeometrySource(normals:posedNormals),colorSource]
        if let uvSource { sources+=[uvSource,HumanModel.texcoordSource(channel),HumanModel.texcoordSource(extra),hairSource()] }
        return SCNGeometry(sources:sources,elements:[SCNGeometryElement(indices:triangles,primitiveType:.triangles)])
    }

    func posedBoneHead(_ name:String,_ pose:[String:V])->V? {
        guard let index=boneNames.firstIndex(of:name) else { return nil }
        return point(boneDeltas(pose)[index],v(file.skeleton.bones[index].head))
    }
}

func humanNode(_ mesh:HumanModel,_ pose:[String:V],exercise:String,focus:String,progress:Float=0)->SCNNode {
    let root=SCNNode(),geometry=mesh.geometry(pose,exercise:exercise,focus:focus,progress:progress)
    geometry.materials=[figureMaterial()];root.geometry=geometry
    // Inverted hull: back faces pushed out along the normal draw the contour.
    let hull=geometry.copy() as! SCNGeometry;hull.materials=[outlineMaterial()]
    let outline=SCNNode(geometry:hull);outline.castsShadow=false;root.addChildNode(outline)
    for name in ["eye.L","eye.R"] {
        guard let position=mesh.posedBoneHead(name,pose) else { continue }
        let sphere=SCNSphere(radius:0.006);sphere.segmentCount=16;sphere.materials=[eyeMaterial]
        let eye=SCNNode(geometry:sphere);eye.simdPosition=position;root.addChildNode(eye)
    }
    return root
}

// MARK: Stage
// Scene, lights, and camera shared by the frame renderer and review probes.
final class Stage {
    let scene=SCNScene(),keyLight=SCNNode(),camera=SCNNode(),renderer:SCNRenderer
    init() {
        let backdrop=0xF4F5F4
        scene.background.contents=color(backdrop);scene.background.intensity=1
        let floor=SCNFloor();floor.reflectivity=0;floor.materials=[unlitMaterial(backdrop)]
        let ground=SCNNode(geometry:floor);ground.simdPosition=V(0,-0.025,0);scene.rootNode.addChildNode(ground)
        let matGeometry=SCNBox(width:2.4,height:0.009,length:1.1,chamferRadius:0.025);matGeometry.materials=[material(0xE7E8E8)]
        let matNode=SCNNode(geometry:matGeometry);matNode.simdPosition=V(0,-0.008,0);scene.rootNode.addChildNode(matNode)
        keyLight.light=SCNLight();keyLight.light!.type = .directional;keyLight.light!.intensity=900
        keyLight.light!.castsShadow=true;keyLight.light!.shadowMode = .forward;keyLight.light!.shadowRadius=8;keyLight.light!.shadowSampleCount=32
        keyLight.light!.shadowBias=0.002;keyLight.light!.shadowMapSize=CGSize(width:2048,height:2048)
        keyLight.light!.orthographicScale=3.0;keyLight.light!.zNear=0.01;keyLight.light!.zFar=10
        keyLight.light!.shadowColor=NSColor(white:0,alpha:0.13);keyLight.eulerAngles=SCNVector3(-0.60,0.80,0);scene.rootNode.addChildNode(keyLight)
        let fill=SCNNode();fill.light=SCNLight();fill.light!.type = .ambient;fill.light!.intensity=90;fill.light!.color=color(0xFFFFFF);scene.rootNode.addChildNode(fill)
        let rim=SCNNode();rim.light=SCNLight();rim.light!.type = .directional;rim.light!.intensity=170;rim.light!.color=color(0xF2F4F5);rim.eulerAngles=SCNVector3(-0.4,2.3,0);scene.rootNode.addChildNode(rim)
        camera.camera=SCNCamera();camera.camera!.usesOrthographicProjection=true;camera.camera!.zNear=0.01;camera.camera!.zFar=100;scene.rootNode.addChildNode(camera)
        let ao=tuned("ssao",Illustration.ambientOcclusion.intensity)
        if ao>0 {
            camera.camera!.screenSpaceAmbientOcclusionIntensity=CGFloat(ao)
            camera.camera!.screenSpaceAmbientOcclusionRadius=CGFloat(tuned("ssaoRadius",Illustration.ambientOcclusion.radius))
            camera.camera!.screenSpaceAmbientOcclusionDepthThreshold=CGFloat(tuned("ssaoDepth",0.2))
            camera.camera!.screenSpaceAmbientOcclusionNormalThreshold=CGFloat(tuned("ssaoNormal",0.3))
        }
        renderer=SCNRenderer(device:MTLCreateSystemDefaultDevice(),options:nil);renderer.scene=scene;renderer.pointOfView=camera;renderer.isJitteringEnabled=false
    }
    // Aim the camera and key light. The outline keeps a constant pixel width.
    func aim(target:V,offset:V,orthographicScale:Float,height:Int) {
        camera.simdPosition=target+offset;camera.look(at:nodev(target),up:SCNVector3(0,1,0),localFront:SCNVector3(0,0,-1))
        camera.camera!.orthographicScale=Double(orthographicScale)
        keyLight.simdPosition=camera.simdPosition-camera.simdWorldRight*0.55+V(0,0.75,0)
        keyLight.look(at:nodev(target+V(0,0.35,0)),up:SCNVector3(0,1,0),localFront:SCNVector3(0,0,-1))
        precondition(abs(camera.simdWorldRight.y)<0.0001,"Camera horizon must remain level")
        let pixel=2*orthographicScale/Float(height*Stage.supersample)
        outlineMaterial().setValue(NSNumber(value:Illustration.outlinePixels*pixel),forKey:"outlineWidth")
        figureMaterial().setValue(NSNumber(value:pixel*1000),forKey:"pixelMM")
    }
    // Render at twice the size and average each 2x2 block. Hairline strokes
    // stay one output pixel wide instead of smearing over two.
    static let supersample=2
    func snapshot(width:Int,height:Int)->NSBitmapImageRep {
        let k=Stage.supersample
        let image=renderer.snapshot(atTime:0,with:CGSize(width:width*k,height:height*k),antialiasingMode:.multisampling4X)
        let big=NSBitmapImageRep(data:image.tiffRepresentation!)!
        precondition(big.pixelsWide==width*k && big.pixelsHigh==height*k,"Unexpected snapshot size")
        let out=NSBitmapImageRep(bitmapDataPlanes:nil,pixelsWide:width,pixelsHigh:height,bitsPerSample:8,samplesPerPixel:3,hasAlpha:false,isPlanar:false,colorSpaceName:.deviceRGB,bytesPerRow:width*3,bitsPerPixel:24)!
        let source=big.bitmapData!,target=out.bitmapData!
        let stride=big.bytesPerRow,step=big.bitsPerPixel/8
        precondition(!big.isPlanar && step>=3 && big.bitsPerSample==8,"Unexpected snapshot format")
        for y in 0..<height { for x in 0..<width { for c in 0..<3 {
            var total=0
            for dy in 0..<k { for dx in 0..<k { total+=Int(source[(y*k+dy)*stride+(x*k+dx)*step+c]) } }
            target[y*width*3+x*3+c]=UInt8((total+k*k/2)/(k*k))
        } } }
        return out
    }
}

let args=CommandLine.arguments
let source=args.count>1 ? args[1]:"tools/stretch-demo/poses.json"
let out=args.count>2 ? args[2]:"/tmp/khon-demo-renders"
let only=args.count>3 ? args[3]:"all"
let mode=args.count>4 ? args[4]:"posters"
let modelPath=ProcessInfo.processInfo.environment["KHON_HUMAN_MESH"] ?? "tools/stretch-demo/model/human_mesh.json"
let guides=try JSONDecoder().decode([String:Guide].self,from:Data(contentsOf:URL(fileURLWithPath:source)))
let humanMesh=try HumanModel(path:modelPath)
try FileManager.default.createDirectory(atPath:out,withIntermediateDirectories:true)

let stage=Stage()
// Review renders may set KHON_FRAMES=0,63 to write only some frames.
let frameList=ProcessInfo.processInfo.environment["KHON_FRAMES"].map { Set($0.split(separator:",").compactMap { Int($0) }) }
var model:SCNNode?=nil
let frameCount=64
for key in guides.keys.sorted() where only=="all" || only.split(separator:",").contains(Substring(key)) {
    let g=guides[key]!,dir=out+"/"+key;try FileManager.default.createDirectory(atPath:dir,withIntermediateDirectories:true)
    for view in 0..<2 {
        let zoom:[String:Float]=["hipflexor":0.82,"plow":0.72,"wgs":0.93,"ninety":0.90,"catcow":0.90,"fold":0.90,"elephant":0.90,"shoulderir":0.90,"needle":0.90]
        let compact=mode=="frames" || mode=="preview"
        let width=compact ? 512:1024,height=compact ? 400:800
        stage.aim(target:v(g.target),offset:v(g.views[view]),orthographicScale:g.span/2*(zoom[key] ?? 1),height:height)
        let count=mode=="frames" ? frameCount:g.poses.count
        for f in 0..<count where frameList==nil || frameList!.contains(f) {
            autoreleasepool {
                let pos=mode=="frames" ? Float(f)/Float(count-1)*Float(g.poses.count-1):Float(f)
                let i=min(g.poses.count-2,Int(pos)),t0=pos-Float(i),t=t0*t0*(3-2*t0)
                let pose=interpolate(g.poses[i],g.poses[i+1],t,key)
                let progress=Float(i)+t
                model?.removeFromParentNode();model=humanNode(humanMesh,pose,exercise:key,focus:g.focus,progress:progress);stage.scene.rootNode.addChildNode(model!)
                let representation=stage.snapshot(width:width,height:height)
                try! representation.representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:dir+String(format:"/v%d-%03d.png",view,f)))
            }
        }
    }
    print("rendered \(key)");fflush(stdout)
}
