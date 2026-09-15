import AppKit
import SceneKit
import simd

struct Guide: Decodable {
    let name: String; let poses: [[String:[Float]]]; let steps: [[String]]
    let views: [[Float]]; let target: [Float]; let span: Float
    let reps: Bool; let focus: String; let loopStart: Int
}
typealias V = SIMD3<Float>
func v(_ a:[Float]) -> V { V(a[0],a[1],a[2]) }
func nodev(_ a:V)->SCNVector3 { SCNVector3(a.x,a.y,a.z) }
func unit(_ a:V)->V { simd_length(a)>0.0001 ? simd_normalize(a) : V(0,1,0) }
func color(_ hex:Int)->NSColor { NSColor(srgbRed:CGFloat((hex>>16)&255)/255,green:CGFloat((hex>>8)&255)/255,blue:CGFloat(hex&255)/255,alpha:1) }
func material(_ hex:Int)->SCNMaterial {
    let m=SCNMaterial();m.diffuse.contents=color(hex);m.lightingModel = .physicallyBased
    m.roughness.contents=0.83;m.metalness.contents=0.0;return m
}
let skin=material(0xBF8D70), shirt=material(0x306777), shorts=material(0x263240), hair=material(0x302B2D), eyes=material(0x292728), accent=material(0xF09832)
@discardableResult
func ellipsoid(_ parent:SCNNode,_ centre:V,_ scale:V,_ mat:SCNMaterial,up:V=V(0,1,0),front:V=V(1,0,0))->SCNNode {
    let g=SCNSphere(radius:1);g.segmentCount=40;g.materials=[mat]
    let n=SCNNode(geometry:g);n.simdPosition=centre;n.simdScale=scale
    let y=unit(up)
    let projected=front-y*simd_dot(front,y)
    let z=simd_length(projected)>0.01 ? unit(projected) : unit(simd_cross(y,V(0,0,1)))
    let x=unit(simd_cross(y,z))
    n.simdOrientation=simd_quatf(simd_float3x3(columns:(x,y,z)))
    parent.addChildNode(n);return n
}
// A continuous surface follows each limb. Shared rings keep knees and elbows
// smooth instead of exposing the end faces of overlapping cylinders.
func sweep(_ parent: SCNNode, _ anchors: [V], _ widths: [Float], _ mat: SCNMaterial, flatten: Float? = nil) {
    // Interpolate the centre line and radius before constructing the surface.
    // The joint then has a smooth bend instead of a bevel at one ring.
    var points = [V](), radii = [Float]()
    for i in 0..<anchors.count-1 {
        let a=anchors[max(0,i-1)],b=anchors[i],c=anchors[i+1],d=anchors[min(anchors.count-1,i+2)]
        for sample in 0..<4 {
            let t=Float(sample)/4,t2=t*t,t3=t2*t
            points.append((2*b+(-a+c)*t+(2*a-5*b+4*c-d)*t2+(-a+3*b-3*c+d)*t3)*0.5)
            let blend=t*t*(3-2*t)
            radii.append(widths[i]*(1-blend)+widths[i+1]*blend)
        }
    }
    points.append(anchors.last!);radii.append(widths.last!)
    let sides = 32
    var vertices = [SCNVector3](), indices = [Int32]()
    var previousRight = V(0,0,1)
    for i in points.indices {
        let tangent = unit(points[min(i+1,points.count-1)] - points[max(i-1,0)])
        var right = previousRight - tangent * simd_dot(previousRight,tangent)
        if simd_length(right) < 0.01 { right = simd_cross(tangent,V(1,0,0)) }
        right = unit(right); previousRight = right
        let forward = unit(simd_cross(tangent,right))
        for j in 0..<sides {
            let angle = Float(j) / Float(sides) * 2 * Float.pi
            var pos = points[i] + radii[i] * (right*cos(angle)+forward*sin(angle))
            if let floor = flatten { pos.y = max(floor,pos.y) }
            vertices.append(nodev(pos))
        }
    }
    for i in 0..<points.count-1 { for j in 0..<sides {
        let a=Int32(i*sides+j),b=Int32(i*sides+(j+1)%sides),c=a+Int32(sides),d=b+Int32(sides)
        indices += [a,b,c,b,d,c]
    }}
    for j in 1..<sides-1 {
        indices += [0,Int32(j+1),Int32(j)]
        let last=Int32((points.count-1)*sides)
        indices += [last,last+Int32(j),last+Int32(j+1)]
    }
    var normals = Array(repeating: V.zero, count: vertices.count)
    for t in stride(from: 0, to: indices.count, by: 3) {
        let ia=Int(indices[t]),ib=Int(indices[t+1]),ic=Int(indices[t+2])
        let va=V(Float(vertices[ia].x),Float(vertices[ia].y),Float(vertices[ia].z))
        let vb=V(Float(vertices[ib].x),Float(vertices[ib].y),Float(vertices[ib].z))
        let vc=V(Float(vertices[ic].x),Float(vertices[ic].y),Float(vertices[ic].z))
        let normal=simd_cross(vb-va,vc-va)
        normals[ia]+=normal;normals[ib]+=normal;normals[ic]+=normal
    }
    let g=SCNGeometry(sources:[SCNGeometrySource(vertices:vertices),SCNGeometrySource(normals:normals.map { nodev(unit($0)) })],elements:[SCNGeometryElement(indices:indices,primitiveType:.triangles)])
    g.materials=[mat]
    parent.addChildNode(SCNNode(geometry:g))
}
func limb(_ parent:SCNNode,_ a:V,_ b:V,_ r1:Float,_ r2:Float,_ mat:SCNMaterial) {
    let delta=b-a
    sweep(parent,[a-delta*0.03,a,a+delta*0.2,a+delta*0.55,a+delta*0.85,b,b+delta*0.03],
          [r1*0.35,r1,r1*1.04,(r1+r2)*0.53,r2,r2,r2*0.35],mat)
}
func armSurface(_ parent:SCNNode,_ a:V,_ b:V,_ c:V) {
    let upper=b-a,lower=c-b
    sweep(parent,[a,a+upper*0.22,a+upper*0.5,a+upper*0.85,b,b+lower*0.16,b+lower*0.4,b+lower*0.75,c],
          [0.060,0.059,0.052,0.040,0.039,0.044,0.042,0.033,0.027],skin)
}
func legSurface(_ parent:SCNNode,_ a:V,_ b:V,_ c:V) {
    let upper=b-a,lower=c-b
    sweep(parent,[a,a+upper*0.23,a+upper*0.55,a+upper*0.85,b,b+lower*0.18,b+lower*0.4,b+lower*0.72,c],
          [0.103,0.101,0.086,0.060,0.057,0.063,0.059,0.041,0.032],skin)
}
func foot(_ parent:SCNNode,_ ankle:V,_ toe:V) {
    let forward=unit(toe-ankle)
    let lateral=unit(simd_cross(forward,V(0,1,0)))
    let normal=unit(simd_cross(lateral,forward))
    let centres=[ankle-forward*0.04,ankle,ankle+(toe-ankle)*0.42,ankle+(toe-ankle)*0.76,toe,toe+forward*0.018]
    let widths:[Float]=[0.025,0.037,0.043,0.05,0.041,0.006]
    let heights:[Float]=[0.027,0.038,0.028,0.022,0.017,0.005]
    let sides=32
    var verts=[SCNVector3](),normals=[SCNVector3](),indices=[Int32]()
    for i in centres.indices {for j in 0..<sides {
        let angle=Float(j)/Float(sides)*2*Float.pi
        let offset=lateral*cos(angle)*widths[i]+normal*sin(angle)*heights[i]
        var point=centres[i]+offset
        point.y=max(0.007,point.y)
        verts.append(nodev(point));normals.append(nodev(unit(lateral*cos(angle)/widths[i]+normal*sin(angle)/heights[i])))
    }}
    if toe.y < 0.075, let minimum=verts.map({Float($0.y)}).min() {
        let correction=minimum-0.007
        for i in verts.indices { verts[i].y -= CGFloat(correction) }
    }
    for i in 0..<centres.count-1 {for j in 0..<sides {
        let a=Int32(i*sides+j),b=Int32(i*sides+(j+1)%sides),c=a+Int32(sides),d=b+Int32(sides)
        indices += [a,c,b,b,c,d]
    }}
    let g=SCNGeometry(sources:[SCNGeometrySource(vertices:verts),SCNGeometrySource(normals:normals)],elements:[SCNGeometryElement(indices:indices,primitiveType:.triangles)])
    let m=skin.copy() as! SCNMaterial;m.isDoubleSided=true;g.materials=[m]
    parent.addChildNode(SCNNode(geometry:g))
}
func torso(_ parent:SCNNode,_ p:[String:V]) {
    let shoulderRight=unit(p["shoulderR"]!-p["shoulderL"]!),hip=p["hip"]!,chest=p["chest"]!,shoulder=p["shoulder"]!
    let upper=unit(shoulder-hip),hipRight=unit(p["hipR"]!-p["hipL"]!)
    let front=unit(simd_cross(upper,hipRight))
    var vertices=[SCNVector3](),normals=[SCNVector3](),indices=[Int32]()
    let rings=12, sides=24
    for i in 0...rings {
        let t=Float(i)/Float(rings)
        let right=spherical(hipRight,shoulderRight,t)
        let ringFront=unit(simd_cross(upper,right))
        let center=(1-t)*(1-t)*hip+2*(1-t)*t*chest+t*t*shoulder
        let width:Float = 0.148 + 0.04*t + 0.020*sin(t*Float.pi)
        let depth:Float = 0.097 + 0.022*sin(t*Float.pi)
        for j in 0..<sides {
            let a=Float(j)/Float(sides)*2*Float.pi
            let delta=right*cos(a)*width+ringFront*sin(a)*depth
            vertices.append(nodev(center+delta));normals.append(nodev(unit(right*cos(a)/width+ringFront*sin(a)/depth)))
        }
    }
    for i in 0..<rings {for j in 0..<sides {let a=Int32(i*sides+j),b=Int32(i*sides+(j+1)%sides),c=a+Int32(sides),d=b+Int32(sides);indices += [a,c,b,b,c,d]}}
    let g=SCNGeometry(sources:[SCNGeometrySource(vertices:vertices),SCNGeometrySource(normals:normals)],elements:[SCNGeometryElement(indices:indices,primitiveType:.triangles)])
    let m=shirt.copy() as! SCNMaterial;m.isDoubleSided=true;g.materials=[m];parent.addChildNode(SCNNode(geometry:g))
    ellipsoid(parent,hip+upper*0.015,V(0.153,0.115,0.113),shorts,up:upper,front:front)
    ellipsoid(parent,shoulder-upper*0.025,V(0.186,0.065,0.105),shirt,up:upper,front:unit(simd_cross(upper,shoulderRight)))
}
func human(_ p:[String:V],focus:String)->SCNNode {
    let root=SCNNode();torso(root,p)
    for s in ["L","R"] {
        let hip=p["hip"+s]!,knee=p["knee"+s]!,ankle=p["ankle"+s]!,toe=p["toe"+s]!
        let hem=hip+(knee-hip)*0.53
        legSurface(root,hip,knee,ankle)
        limb(root,hip,hem,0.107,0.094,shorts)
        foot(root,ankle,toe)
        if s==focus {limb(root,ankle+unit(knee-ankle)*0.030,ankle+unit(knee-ankle)*0.052,0.038,0.038,accent)}
        let shoulder=p["shoulder"+s]!,elbow=p["elbow"+s]!,wrist=p["wrist"+s]!,hand=p["hand"+s]!
        let sleeve=shoulder+(elbow-shoulder)*0.37
        armSurface(root,shoulder,elbow,wrist)
        limb(root,shoulder,sleeve,0.068,0.061,shirt)
        let direction=unit(hand-wrist),side=unit(simd_cross(direction,abs(direction.y)>0.9 ? V(1,0,0) : V(0,1,0)))
        ellipsoid(root,(wrist+hand)*0.5,V(0.039,simd_length(hand-wrist)*0.62,0.019),skin,up:direction,front:abs(direction.y)>0.9 ? V(1,0,0) : V(0,1,0))
        for f in 0..<4 {
            var a=hand+side*(Float(f)-1.5)*0.016;a.y=max(0.020,a.y)
            var b=a+direction*(f==0 || f==3 ? 0.040 : 0.052);b.y=max(0.020,b.y)
            limb(root,a,b,0.009,0.008,skin)
        }
        let thumb=(wrist+hand)*0.5+side*0.040
        limb(root,thumb,thumb+direction*0.030+side*0.015,0.014,0.012,skin)
        if s==focus {limb(root,wrist-direction*0.017,wrist-direction*0.035,0.030,0.030,accent)}
    }
    let shoulder=p["shoulder"]!,head=p["head"]!,up=unit(head-shoulder),face=unit(p["face"]!)
    limb(root,shoulder+up*0.045,head-up*0.085,0.054,0.047,skin)
    let hn=SCNNode();hn.simdPosition=head
    let x=unit(simd_cross(up,face)),y=unit(simd_cross(face,x));hn.simdOrientation=simd_quatf(simd_float3x3(columns:(x,y,face)))
    root.addChildNode(hn)
    ellipsoid(hn,V(0,0,0),V(0.101,0.133,0.102),skin,front:V(0,0,1))
    ellipsoid(hn,V(0,0.075,-0.008),V(0.104,0.079,0.097),hair,front:V(0,0,1))
    for sign:Float in [-1,1] {
        ellipsoid(hn,V(sign*0.102,-0.021,-0.005),V(0.015,0.029,0.016),skin,front:V(0,0,1))
        ellipsoid(hn,V(sign*0.037,0.010,0.092),V(0.006,0.007,0.005),eyes,front:V(0,0,1))
    }
    ellipsoid(hn,V(0,-0.020,0.109),V(0.018,0.022,0.021),skin,front:V(0,0,1))
    return root
}
func spherical(_ a:V,_ b:V,_ t:Float)->V {
    let av=unit(a),bv=unit(b),dot=max(-1.0,min(1.0,simd_dot(av,bv)))
    if abs(dot)>0.999 { return unit(av*(1-t)+bv*t) }
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
    }
    return p
}
let args=CommandLine.arguments
let source=args.count>1 ? args[1] : "tools/stretch-demo/poses.json"
let out=args.count>2 ? args[2] : "/tmp/khon-demo-renders"
let only=args.count>3 ? args[3] : "all"
let mode=args.count>4 ? args[4] : "posters"
let guides=try JSONDecoder().decode([String:Guide].self,from:Data(contentsOf:URL(fileURLWithPath:source)))
try FileManager.default.createDirectory(atPath:out,withIntermediateDirectories:true)
let scene=SCNScene();scene.background.contents=color(0xEDF0F4)
let floor=SCNFloor();floor.reflectivity=0;floor.materials=[material(0xEDF0F4)]
let ground=SCNNode(geometry:floor);ground.simdPosition=V(0,-0.025,0);scene.rootNode.addChildNode(ground)
let mat=SCNBox(width:2.4,height:0.014,length:1.1,chamferRadius:0.04);mat.materials=[material(0xBACAD6)]
let matNode=SCNNode(geometry:mat);scene.rootNode.addChildNode(matNode)
let light=SCNNode();light.light=SCNLight();light.light!.type = .directional;light.light!.intensity=800
light.light!.castsShadow=true;light.light!.shadowMode = .forward;light.light!.shadowRadius=2;light.light!.shadowSampleCount=16
light.light!.shadowBias=0.001;light.light!.shadowMapSize=CGSize(width:2048,height:2048)
light.light!.orthographicScale=3.0;light.light!.zNear=0.01;light.light!.zFar=10
light.light!.shadowColor=NSColor(white:0,alpha:0.20);light.eulerAngles=SCNVector3(-0.8,-0.6,0);scene.rootNode.addChildNode(light)
let fill=SCNNode();fill.light=SCNLight();fill.light!.type = .ambient;fill.light!.intensity=400;fill.light!.color=color(0xE7F0FF);scene.rootNode.addChildNode(fill)
let camera=SCNNode();camera.camera=SCNCamera();camera.camera!.usesOrthographicProjection=true;camera.camera!.zNear=0.01;camera.camera!.zFar=100
scene.rootNode.addChildNode(camera)
let renderer=SCNRenderer(device:MTLCreateSystemDefaultDevice(),options:nil);renderer.scene=scene;renderer.pointOfView=camera
renderer.isJitteringEnabled=false
var model:SCNNode? = nil
let frameCount=64
for key in guides.keys.sorted() where only=="all" || only==key {
    let g=guides[key]!
    let dir=out+"/"+key;try FileManager.default.createDirectory(atPath:dir,withIntermediateDirectories:true)
    for view in 0..<2 {
        let target=v(g.target)
        camera.simdPosition=target+v(g.views[view]);camera.look(at:nodev(target), up:SCNVector3(0,1,0), localFront:SCNVector3(0,0,-1));camera.camera!.orthographicScale=Double(g.span/2)
        precondition(abs(camera.simdWorldRight.y) < 0.0001, "Camera horizon must remain level")
        let count=mode=="frames" ? frameCount : g.poses.count
        for f in 0..<count {
            autoreleasepool {
                let pos=mode=="frames" ? Float(f)/Float(count-1)*Float(g.poses.count-1) : Float(f)
                let i=min(g.poses.count-2,Int(pos));let t0=pos-Float(i),t=t0*t0*(3-2*t0)
                let p=interpolate(g.poses[i],g.poses[i+1],t,key)
                model?.removeFromParentNode();model=human(p,focus:g.focus);scene.rootNode.addChildNode(model!)
                let width=mode=="frames" ? 512 : 1024,height=mode=="frames" ? 400 : 800
                let img=renderer.snapshot(atTime:0,with:CGSize(width:width,height:height),antialiasingMode:.multisampling4X)
                let rep=NSBitmapImageRep(data:img.tiffRepresentation!)!
                try! rep.representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:dir+String(format:"/v%d-%03d.png",view,f)))
            }
        }
    }
    print("rendered \(key)");fflush(stdout)
}
