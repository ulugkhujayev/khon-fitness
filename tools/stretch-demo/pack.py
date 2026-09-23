"""Pack original renderer output as offline Android sprite sheets and guide metadata."""
import json,sys
from pathlib import Path
from PIL import Image
source=Path(sys.argv[1] if len(sys.argv)>1 else '/tmp/khon-demo-frames')
out=Path('android/app/src/main/assets/stretch_demos');out.mkdir(parents=True,exist_ok=True)
guides=json.loads(Path('tools/stretch-demo/poses.json').read_text())
manifest={}
for key,g in guides.items():
    count=64;step_frames=[round(i*(count-1)/(len(g['steps'])-1)) for i in range(len(g['steps']))]
    for view in range(2):
        sheet=Image.new('RGB',(512*8,400*8))
        for f in range(count):
            im=Image.open(source/key/f'v{view}-{f:03}.png').convert('RGB')
            assert im.size==(512,400)
            sheet.paste(im,((f%8)*512,(f//8)*400))
        sheet.save(out/f'{key}-{view}.webp',quality=92,method=6)
        Image.open(source/key/f'v{view}-{count-1:03}.png').convert('RGB').save(out/f'{key}-{view}-poster.webp',quality=95,method=6)
    manifest[key]={'name':g['name'],'frameCount':count,'columns':8,'frameWidth':512,'frameHeight':400,
        'steps':[{'title':step[0],'instruction':step[1],'frame':step_frames[i]} for i,step in enumerate(g['steps'])],
        'loopStart':step_frames[g['loopStart']],'moving':g['reps'],
        'repHoldFraction':0.2 if key=='elephant' else 0.0}
(out/'guides.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')
print('Packed',len(manifest),'guides;',round(sum(p.stat().st_size for p in out.iterdir())/1e6,2),'MB')
