"""Build a local visual review page from the actual Android sprite sheets.

Run from the repository root, then serve the root with python3 -m http.server.
No generated images or substitute artwork: the canvas draws the packaged pixels.
"""
import argparse
import json
import os
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--out', type=Path, default=Path('dist/qa-anatomy/index.html'))
parser.add_argument('--before', type=Path, default=Path('dist/qa-anatomy/before'))
args = parser.parse_args()
assets = Path('android/app/src/main/assets/stretch_demos')
catalog = json.loads((assets / 'guides.json').read_text())
args.out.parent.mkdir(parents=True, exist_ok=True)
data = json.dumps(catalog).replace('</', '<\\/')
page = '''<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Khon Fitness stretch review</title>
<style>
*{box-sizing:border-box}body{margin:32px;background:#17191d;color:#f1f2f5;font:15px system-ui}
h1{font-size:26px;margin-bottom:8px}p{color:#b7bec8;line-height:1.5}h2{font-size:18px}
main{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,620px),1fr));gap:32px}
section{min-width:0;border-top:1px solid #49505a;padding-top:8px}
.images{display:grid;grid-template-columns:1fr 1fr;gap:12px}figure{margin:0;min-width:0}
canvas,img{width:100%;height:auto;display:block;background:#edf0f4;border-radius:8px}
figcaption{color:#b7bec8;margin:6px 0 10px;font-size:13px}
button,select{background:#2c323a;border:1px solid #5c6673;color:white;padding:10px;font:inherit;border-radius:4px}
.controls{display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-top:12px}
input[type=range]{flex:1;min-width:100px}label{font-size:13px}output{font-variant-numeric:tabular-nums}
.steps{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}.cue{min-height:48px}
@media(max-width:650px){body{margin:16px}.images{grid-template-columns:1fr}.before{display:none}}
</style>
<h1>Khon Fitness stretch review</h1>
<p>Actual app assets. Each guide has two views, 64 frames, and the same timing as the Android preview.
Choose a step to inspect its position, or drag through every intermediate frame.</p><main></main>
<script>
const catalog=CATALOG, assets=ASSETS, before=BEFORE, version=ASSET_VERSION;
for(const [key,g] of Object.entries(catalog)){
  const s=document.createElement('section');
  s.innerHTML=`<h2>${g.name}</h2><div class="images">
    <figure class="before"><img src="${before}/${key}-0-poster.webp" alt="Previous ${g.name} figure"><figcaption>Previous model, held position</figcaption></figure>
    <figure><canvas width="${g.frameWidth}" height="${g.frameHeight}" aria-label="${g.name} animation"></canvas><figcaption>New model, packaged animation</figcaption></figure></div>
    <div class="controls"><button class="play">Play</button><select aria-label="Camera"><option value="0">View 1</option><option value="1">View 2</option></select>
    <label><input class="mirror" type="checkbox">Mirror</label><input class="frame" aria-label="Frame" type="range" min="0" max="${g.frameCount-1}" value="${g.frameCount-1}"><output>${g.frameCount-1}</output></div>
    <div class="steps"></div><p class="cue"></p>`;
  document.querySelector('main').append(s);
  const canvas=s.querySelector('canvas'),ctx=canvas.getContext('2d'),slider=s.querySelector('.frame');
  const img=new Image();let view=0,frame=g.frameCount-1,playing=false,start=0;
  function draw(){
    if(!img.complete||!img.naturalWidth)return;
    ctx.save();ctx.clearRect(0,0,canvas.width,canvas.height);
    if(s.querySelector('.mirror').checked){ctx.translate(canvas.width,0);ctx.scale(-1,1)}
    ctx.drawImage(img,(frame%g.columns)*g.frameWidth,Math.floor(frame/g.columns)*g.frameHeight,g.frameWidth,g.frameHeight,0,0,g.frameWidth,g.frameHeight);ctx.restore();
    slider.value=frame;s.querySelector('output').textContent=frame;
    let cue=(g.steps.find(step=>frame<=step.frame)||g.steps.at(-1)).instruction;
    if(s.querySelector('.mirror').checked)cue=cue.replace(/\\b(left|right)\\b/gi,word=>{
      const other=word.toLowerCase()==='left'?'right':'left';return word[0]===word[0].toUpperCase()?other[0].toUpperCase()+other.slice(1):other});
    s.querySelector('.cue').textContent=cue;
  }
  function load(){img.src=`${assets}/${key}-${view}.webp?v=${version}`;s.querySelector('.before img').src=`${before}/${key}-${view}-poster.webp`}
  function pause(){playing=false;s.querySelector('.play').textContent='Play'}
  img.onload=draw;load();
  slider.oninput=()=>{pause();frame=Number(slider.value);draw()};
  s.querySelector('select').onchange=e=>{view=Number(e.target.value);load()};
  s.querySelector('.mirror').onchange=draw;
  for(const [i,step] of g.steps.entries()){
    const b=document.createElement('button');b.textContent=`${i+1}. ${step.title}`;
    b.onclick=()=>{pause();frame=step.frame;draw()};s.querySelector('.steps').append(b);
  }
  s.querySelector('.play').onclick=()=>{
    if(playing){pause();return}playing=true;start=performance.now();s.querySelector('.play').textContent='Pause';
    function tick(now){if(!playing)return;const elapsed=now-start,slot=Math.floor(elapsed/2600)%g.steps.length;
      frame=slot===0?g.steps[0].frame:Math.round(g.steps[slot-1].frame+(g.steps[slot].frame-g.steps[slot-1].frame)*Math.min(1,(elapsed%2600)/1600));
      draw();requestAnimationFrame(tick)}requestAnimationFrame(tick);
  };
}
</script></html>'''
page = page.replace('CATALOG', data).replace('ASSETS', json.dumps(os.path.relpath(assets, args.out.parent)))
page = page.replace('BEFORE', json.dumps(os.path.relpath(args.before, args.out.parent)))
page = page.replace('ASSET_VERSION', json.dumps(str(max(p.stat().st_mtime_ns for p in assets.glob('*.webp')))))
args.out.write_text(page)
print(args.out)
