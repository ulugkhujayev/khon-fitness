"""Original 3D exercise pose authoring. Coordinates in metres, X forward, Y up, Z right."""
import json, math
from pathlib import Path

def add(a,b): return [a[i]+b[i] for i in range(3)]
def sub(a,b): return [a[i]-b[i] for i in range(3)]
def scale(a,t): return [v*t for v in a]
def norm(a):
    d=math.sqrt(sum(v*v for v in a));return scale(a,1/d)
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def body(hip, shoulder, twist=0, head=None, face=None, chest=None, pelvis_up=None):
    up=norm(sub(shoulder,hip)); right=[0,0,1]
    if twist:
        r=math.radians(twist)
        right=add(scale(right,math.cos(r)),scale(cross(up,right),math.sin(r)))
    front=norm(cross(up,right))
    p={'hip':hip,'shoulder':shoulder,'chest':chest or add(hip,scale(sub(shoulder,hip),.53)),
       'head':head or add(shoulder,scale(up,.24)),'face':face or front,'up':up,
       'pelvisUp':pelvis_up or up,
       'hipL':add(hip,[0,0,-.12]),'hipR':add(hip,[0,0,.12]),
       'shoulderL':add(shoulder,scale(right,-.19)),'shoulderR':add(shoulder,scale(right,.19))}
    return p

def leg(p,s,knee,ankle,toe=None):
    p['knee'+s]=knee;p['ankle'+s]=ankle;p['toe'+s]=toe or add(ankle,[.14,-.025,0]);return p

def arm(p,s,elbow,wrist,hand=None):
    p['elbow'+s]=elbow;p['wrist'+s]=wrist
    p['hand'+s]=hand or add(wrist,scale(norm(sub(wrist,elbow)),.08));return p

def standing():
    p=body([0,.92,0],[0,1.43,0],face=[1,0,0])
    for s,z in [('L',-.14),('R',.14)]:
        leg(p,s,[.005,.50,z],[0,.075,z],[.17,.035,z])
        arm(p,s,[0,1.12,z*1.6],[0,.86,z*1.7])
    return p

def fold(bend=False,swap=False):
    p=body([-.16,.89,0],[.26,.47,0],head=[.35,.28,0],face=[-.45,-.9,0])
    for s,z in [('L',-.15),('R',.15)]:
        bent=bend and ((s=='L') != swap)
        if bent: leg(p,s,[.06,.52,z],[-.04,.13,z],[.13,.035,z])
        else: leg(p,s,[-.07,.48,z],[0,.075,z],[.17,.035,z])
        arm(p,s,[.31,.25,z*1.5],[.37,.055,z*1.5],[.46,.025,z*1.5])
    return p

def lunge(opened=False,lower=False):
    p=body([-.04,.56,0],[.44,.62,0] if not lower else [.34,.42,0],
           twist=85 if opened else 0,
           head=[.60,.74,0] if opened else ([.53,.38,0] if lower else [.64,.63,0]),
           face=[-.1,.96,-.2] if opened else [.55,-.84,0])
    leg(p,'L',[.44,.49,-.17],[.46,.075,-.17],[.63,.035,-.17])
    leg(p,'R',[-.51,.32,.14],[-.98,.13,.14],[-.88,.025,.14])
    arm(p,'R',[.47,.31,.20],[.50,.055,.24],[.59,.02,.24])
    if opened:
        arm(p,'L',add(p['shoulderL'],[.02,.30,-.025]),add(p['shoulderL'],[.04,.58,-.05]),add(p['shoulderL'],[.04,.67,-.05]))
    elif lower: arm(p,'L',[.43,.135,-.18],[.25,.07,-.12],[.17,.04,-.1])
    else: arm(p,'L',[.37,.33,-.12],[.42,.065,-.08],[.51,.025,-.08])
    return p

def table(kind='neutral'):
    pelvis_up=[.96,.28 if kind=='cat' else -.28,0] if kind!='neutral' else None
    p=body([-.31,.54,0],[.21,.54,0],head=[.42,.52,0],face=[.4,-.9,0],
           chest=[-.05,.66 if kind=='cat' else .43 if kind=='cow' else .54,0],pelvis_up=pelvis_up)
    if kind=='cat':p['head']=[.37,.43,0];p['face']=[.45,-.55,0]
    if kind=='cow':p['head']=[.43,.68,0];p['face']=[1,.05,0]
    for s,z in [('L',-.20),('R',.20)]:
        leg(p,s,[-.31,.075,z],[-.73,.07,z],[-.88,.04,z])
        arm(p,s,[.22,.31,z],[.24,.06,z],[.34,.025,z])
    return p

def needle(thread=False):
    if not thread:return table()
    p=body([-.31,.54,0],[.20,.22,0],twist=-85,head=[.39,.115,.015],face=[0,0,1],chest=[-.07,.41,0])
    for s,z in [('L',-.18),('R',.18)]:leg(p,s,[-.31,.075,z],[-.73,.07,z],[-.88,.04,z])
    arm(p,'L',[.20,.07,.28],[.20,.06,.57],[.20,.04,.67])
    arm(p,'R',[.36,.29,.27],[.47,.07,.24],[.57,.025,.24])
    return p

def ninety(lean=False):
    p=body([0,.15,0],[.24,.56,-.065] if lean else [0,.66,0],
           head=[.35,.74,-.09] if lean else [0,.90,0],face=[1,-.2 if lean else 0,0])
    leg(p,'L',[.42,.085,-.12],[.42,.075,.30],[.42,.04,.46])
    leg(p,'R',[0,.085,.56],[-.41,.075,.56],[-.57,.04,.56])
    if lean:
        arm(p,'L',[.39,.32,-.33],[.49,.065,-.37],[.59,.025,-.37])
        arm(p,'R',[.43,.32,.25],[.52,.065,.22],[.62,.025,.22])
    else:
        arm(p,'L',[-.05,.38,-.29],[-.11,.065,-.35],[-.02,.025,-.35])
        arm(p,'R',[-.05,.38,.29],[-.11,.065,.35],[-.02,.025,.35])
    return p

def hipflexor(shift=0,tuck=False):
    # Keep the rear knee beneath the pelvis. The previous .58m hip height
    # and -.34m rear knee stretched the thigh to .66m in the held pose.
    pelvis_up=[-.14,.99,0] if tuck else None
    p=body([shift,.48,0],[shift,1.00,0],face=[1,0,0],pelvis_up=pelvis_up)
    leg(p,'L',[.40+shift,.48,-.14],[.45,.075,-.14],[.62,.035,-.14])
    leg(p,'R',[-.05,.075,.14],[-.47,.07,.14],[-.62,.04,.14])
    arm(p,'L',[shift+.09,.76,-.29],[.24,.59,-.15])
    arm(p,'R',[shift+.09,.76,.29],[.24,.59,.15])
    return p

def shoulder_ir(raise_hand=False):
    p=standing()
    arm(p,'L',[-.07,1.12,-.26],[-.13,.94 if raise_hand else .87,-.04],[-.13,1.01 if raise_hand else .94,0])
    return p

def plow(stage=0):
    hip=[[0,.15,0],[0,.23,0],[.21,.62,0],[.39,.68,0]][stage]
    p=body(hip,[.48,.14,0],head=[.71,.115,0],face=[0,1,0],chest=[.28,.24 if stage<2 else .46,0] if stage<3 else [.40,.48,0])
    for s,z in [('L',-.13),('R',.13)]:
        if stage==0:k,a,t=[-.43,.095,z],[-.85,.08,z],[-1,.04,z]
        elif stage==1:k,a,t=[-.015,.67,z],[-.03,1.11,z],[-.03,1.28,z]
        elif stage==2:k,a,t=[.46,.97,z],[.86,.87,z],[1,.77,z]
        else:k,a,t=[.75,.44,z],[1,.105,z],[1.13,.025,z]
        leg(p,s,k,a,t)
        arm(p,s,[.18,.10,z*1.6],[-.13,.07,z*1.7],[-.22,.04,z*1.7])
    return p

G={}
def guide(key,name,poses,steps,view=(2.5,1.7,3),alternate=(-1.8,1.4,3),target=(0,.62,0),span=2.4,reps=False,focus='L',loop_start=1):
    G[key]={'name':name,'poses':poses,'steps':steps,'views':[view,alternate], 'target':target,'span':span,'reps':reps,'focus':focus,'loopStart':loop_start}

guide('wgs',"World's greatest stretch",[lunge(),lunge(lower=True),lunge(opened=True)],
    [['Set up','Step your left foot outside your left hand, knee over ankle. Keep your right leg long.'],['Lower','Plant your right hand. Exhale and lower your left elbow inside your left foot.'],['Rotate','Inhale. Open your chest left and reach your left hand up. Follow it with your eyes.']],view=(2,1.35,-3.3),alternate=(.2,2.6,-3),target=(0,.67,0),span=2.2,reps=True)
guide('needle','Thread the needle',[table(),needle(True)],
    [['Set up','Start on hands and knees. Put your hands under your shoulders.'],['Thread','Exhale. Slide your left arm under your right arm, palm up.'],['Hold','Rest your left shoulder and ear on the mat. Keep your hips over your knees and breathe.']],view=(2.6,1.4,3),alternate=(.7,2.8,-3),target=(-.04,.35,.10),span=2.05)
# Intermediate threading pose lets the instruction be seen before the shoulder lowers.
m=needle(True)
for j in ['hip','hipL','hipR','shoulder','shoulderL','shoulderR','head','chest']:
    if j not in ['hip','hipL','hipR']:m[j]=add(scale(table()[j],.45),scale(m[j],.55))
G['needle']['poses'].insert(1,m)
seated=ninety()
for side,z in [('L',-.15),('R',.15)]:
    leg(seated,side,[.34,.43,z],[.65,.075,z],[.81,.035,z])
front_leg=ninety()
leg(front_leg,'R',[.34,.43,.15],[.65,.075,.15],[.81,.035,.15])
guide('ninety','90/90 stretch',[seated,front_leg,ninety(),ninety(True)],
    [['Set up','Sit tall with both sit bones down. Bend your knees and plant your feet.'],['Front leg','Lower your left knee in front. Bend it to 90 degrees with your calf on the floor.'],['Back leg','Take your right thigh out to the side. Bend the knee to 90 degrees, with the lower leg pointing back.'],['Lean','Lengthen your spine. Hinge over your front shin and breathe.']],view=(2.5,2.7,3),alternate=(-2.3,2.5,2.6),target=(.1,.42,.15),span=1.8)
guide('plow','Plow pose',[plow(i) for i in range(4)],
    [['Set up','Lie on your back with your arms beside you, palms down.'],['Lift','Bring your legs up together. Keep your head still.'],['Roll','Slowly roll your hips up and carry your legs overhead. Do not swing.'],['Hold','Rest your toes on the floor or a support. Keep weight on your upper back, not your neck. Breathe.']],view=(.2,1.6,3.5),alternate=(2.8,2.3,2.8),target=(.12,.58,0),span=2.6)
guide('hipflexor','Hip flexor stretch',[hipflexor(),hipflexor(tuck=True),hipflexor(.04,tuck=True)],
    [['Set up','Kneel on your right knee under your hip. Place your left knee over your ankle.'],['Tuck','Tuck your tailbone. Gently squeeze your right glute and keep your ribs down.'],['Hold','Stay tall. Shift your hips slightly forward and breathe.']],target=(-.08,.66,0),span=2.0)
guide('fold','Hamstring stretch',[standing(),fold(),fold()],
    [['Stand','Stand with your feet hip-width apart.'],['Fold','Hinge at your hips with a long spine. Keep your knees soft and reach for your shins or the floor.'],['Hold','Relax your neck and breathe slowly. Keep a small bend in your knees.']],view=(1.7,1.5,3),target=(.04,.83,0),span=2.15,focus='')
guide('elephant','Elephant walks',[fold(),fold(True),fold(True,True)],
    [['Set up','Stand hip-width. Hinge forward and plant your hands on the floor or blocks.'],['Bend one knee','Keep your hands still. Lift your left heel and bend your left knee. Lengthen your right leg.'],['Switch','Lower your left heel as you lift your right heel and bend your right knee. Breathe with each switch.']],view=(2.2,1.4,3.1),alternate=(0,1.5,3.5),target=(.08,.50,0),span=1.75,reps=True,focus='')
guide('catcow','Cat cow',[table(),table('cat'),table('cow')],
    [['Set up','Start on hands and knees. Keep your knees and feet hip-width, with the tops of your feet down.'],['Round','Exhale. Push the floor away, round your spine, and tuck your chin and tailbone gently.'],['Arch','Inhale. Lift your tailbone, lower your belly, and open your chest. Gaze ahead.']],view=(1.4,1.1,3.2),alternate=(-3.1,1.2,.8),target=(-.15,.40,0),span=1.85,reps=True,focus='')
guide('shoulderir','Shoulder internal rotation',[standing(),shoulder_ir(),shoulder_ir(True)],
    [['Stand','Stand tall with your shoulders relaxed and down.'],['Reach back','Bend your left elbow. Rest the back of your hand on your sacrum and keep the elbow close.'],['Slide up','Slide your hand a little way up your low back. Breathe and stop at a mild pull.']],view=(-2.7,1.6,-2.7),alternate=(-3.4,1.6,.3),target=(0,.90,0),span=2.0)
Path('tools/stretch-demo/poses.json').write_text(json.dumps(G,indent=2)+'\n')
print('wrote',len(G),'guides')
