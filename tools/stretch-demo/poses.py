"""Original 3D exercise pose authoring. Coordinates in metres, X forward, Y up, Z right."""
import json, math
from pathlib import Path

def add(a,b): return [a[i]+b[i] for i in range(3)]
def sub(a,b): return [a[i]-b[i] for i in range(3)]
def scale(a,t): return [v*t for v in a]
def norm(a):
    d=math.sqrt(sum(v*v for v in a));return scale(a,1/d)
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def body(hip, shoulder, twist=0, head=None, face=None, chest=None):
    up=norm(sub(shoulder,hip)); right=[0,0,1]
    if twist:
        r=math.radians(twist)
        right=add(scale(right,math.cos(r)),scale(cross(up,right),math.sin(r)))
    front=norm(cross(up,right))
    p={'hip':hip,'shoulder':shoulder,'chest':chest or add(hip,scale(sub(shoulder,hip),.53)),
       'head':head or add(shoulder,scale(up,.24)),'face':face or front,'up':up,
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
        leg(p,s,[.11 if bent else -.07,.48,z],[0,.075,z],[.17,.035,z])
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
    else: arm(p,'L',[.37,.33,-.20],[.42,.065,-.25],[.51,.025,-.25])
    return p

def table(kind='neutral'):
    p=body([-.31,.54,0],[.21,.54,0],head=[.42,.52,0],face=[.4,-.9,0],
           chest=[-.05,.66 if kind=='cat' else .43 if kind=='cow' else .54,0])
    if kind=='cat':p['head']=[.35,.36,0];p['face']=[-.5,-.85,0]
    if kind=='cow':p['head']=[.43,.68,0];p['face']=[1,.05,0]
    for s,z in [('L',-.18),('R',.18)]:
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

def hipflexor(shift=0):
    p=body([shift,.58,0],[shift,1.10,0],face=[1,0,0])
    leg(p,'L',[.41+shift*.3,.48,-.14],[.45,.075,-.14],[.62,.035,-.14])
    leg(p,'R',[-.34,.075,.14],[-.76,.07,.14],[-.91,.04,.14])
    arm(p,'L',[shift+.03,.85,-.34],[shift+.02,.65,-.18])
    arm(p,'R',[shift+.03,.85,.34],[shift+.02,.65,.18])
    return p

def shoulder_ir(raise_hand=False):
    p=standing()
    arm(p,'L',[-.09,1.10,-.32],[-.145,1.17 if raise_hand else .95,-.015],[-.145,1.25 if raise_hand else 1.03,.02])
    return p

def plow(stage=0):
    hip=[[0,.15,0],[0,.23,0],[.21,.62,0],[.25,.64,0]][stage]
    p=body(hip,[.48,.14,0],head=[.71,.115,0],face=[0,1,0],chest=[.28,.24 if stage<2 else .46,0])
    for s,z in [('L',-.13),('R',.13)]:
        if stage==0:k,a,t=[-.43,.095,z],[-.85,.08,z],[-1,.04,z]
        elif stage==1:k,a,t=[-.015,.67,z],[-.03,1.11,z],[-.03,1.28,z]
        elif stage==2:k,a,t=[.46,.97,z],[.86,.87,z],[1,.77,z]
        else:k,a,t=[.67,.36,z],[1,.105,z],[1.13,.025,z]
        leg(p,s,k,a,t)
        arm(p,s,[.18,.10,z*1.6],[-.13,.07,z*1.7],[-.22,.04,z*1.7])
    return p

G={}
def guide(key,name,poses,steps,view=(2.5,1.7,3),alternate=(-1.8,1.4,3),target=(0,.62,0),span=2.4,reps=False,focus='L',loop_start=1):
    G[key]={'name':name,'poses':poses,'steps':steps,'views':[view,alternate], 'target':target,'span':span,'reps':reps,'focus':focus,'loopStart':loop_start}

guide('wgs',"World's greatest stretch",[lunge(),lunge(lower=True),lunge(opened=True)],
    [['Set up','Step your left foot beside your left hand. Keep the back leg long.'],['Lower','Bring your left elbow toward the inside of your front foot.'],['Rotate','Turn your chest left and reach your left hand up. Follow it with your eyes.']],view=(2,1.35,-3.3),alternate=(.2,2.6,-3),target=(0,.67,0),span=2.2,reps=True)
guide('needle','Thread the needle',[table(),needle(True)],
    [['Set up','Start on hands and knees. Put your hands under your shoulders.'],['Thread','Slide your left arm under your right arm, palm facing up.'],['Hold','Lower your left shoulder and the side of your head to the mat. Keep your hips over your knees.']],view=(2.6,1.4,3),alternate=(.7,2.8,3),target=(-.04,.35,.10),span=2.05)
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
    [['Set up','Sit on the floor with your knees bent and your feet planted.'],['Front leg','Lower your left knee. Bring that shin across your body, with the knee bent to 90°.'],['Back leg','Fold your right leg behind you, also at 90°. Rest both legs on the floor.'],['Lean','Keep your chest long. Lean from your hips over your front shin.']],view=(2.5,2.7,3),alternate=(2.5,1.1,-3),target=(.1,.42,.15),span=1.8)
guide('plow','Plow pose',[plow(i) for i in range(4)],
    [['Set up','Lie on your back with your arms beside you, palms down.'],['Lift','Bring your legs up together. Keep your head still.'],['Roll','Lift your hips and slowly take your legs over your head.'],['Hold','Reach your toes toward the floor behind your head. Keep your weight on your shoulders.']],view=(.2,1.6,3.5),alternate=(2.8,2.3,2.8),target=(.12,.58,0),span=2.6)
guide('hipflexor','Hip flexor stretch',[hipflexor(),hipflexor(.04),hipflexor(.09)],
    [['Set up','Kneel on your right knee. Plant your left foot in front.'],['Tuck','Tuck your pelvis and gently squeeze your right glute.'],['Hold','Keep your chest upright. Shift slightly forward until the front of your right hip stretches.']],target=(-.08,.66,0),span=2.0)
guide('fold','Hamstring stretch',[standing(),fold(),fold()],
    [['Stand','Stand with your feet hip-width apart.'],['Fold','Bend at your hips and reach your hands toward the floor.'],['Hold','Relax your neck. Keep a small bend in your knees if needed.']],view=(1.7,1.5,3),target=(.04,.83,0),span=2.15,focus='')
guide('elephant','Elephant walks',[fold(),fold(True),fold(True,True)],
    [['Set up','Fold forward and place your hands on the floor.'],['Bend one knee','Keep your hands down. Bend one knee while the other leg lengthens.'],['Switch','Slowly straighten that leg as you bend the other knee.']],view=(2.2,1.4,3.1),alternate=(0,1.5,3.5),target=(.08,.50,0),span=1.75,reps=True,focus='')
guide('catcow','Cat cow',[table(),table('cat'),table('cow')],
    [['Set up','Start on hands and knees, with hands under shoulders and knees under hips.'],['Round','Push the floor away. Round your back and let your head drop.'],['Arch','Gently lower your belly and lift your chest. Move slowly between the two positions.']],view=(1.4,1.1,3.2),target=(-.15,.40,0),span=1.85,reps=True,focus='')
guide('shoulderir','Shoulder internal rotation',[standing(),shoulder_ir(),shoulder_ir(True)],
    [['Stand','Stand tall and let your shoulders relax.'],['Reach back','Bend your left elbow and place the back of your hand against your lower back.'],['Slide up','Gently slide your hand upward. Keep your chest still and avoid forcing the movement.']],view=(-2.7,1.6,-2.7),alternate=(-3.4,1.6,.3),target=(0,.90,0),span=2.0)
Path('tools/stretch-demo/poses.json').write_text(json.dumps(G,indent=2)+'\n')
print('wrote',len(G),'guides')
