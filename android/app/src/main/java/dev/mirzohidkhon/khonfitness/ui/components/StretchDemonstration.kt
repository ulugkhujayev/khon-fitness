package dev.mirzohidkhon.khonfitness.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.mirzohidkhon.khonfitness.data.DemoClock
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.forDemoSide
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private object DemoCatalog {
    @Volatile private var cached: Map<String, StretchDemo>? = null
    fun read(context: Context): Map<String, StretchDemo> = cached ?: synchronized(this) {
        cached ?: context.assets.open("stretch_demos/guides.json").bufferedReader().use {
            Json.decodeFromString<Map<String, StretchDemo>>(it.readText())
        }.also { cached = it }
    }
}

private data class DemoBitmap(val path: String, val image: ImageBitmap, val background: Color)

/** Full color avoids banding in the studio background. A sheet moves to a HARDWARE bitmap, so its 50 MiB live
 *  only in graphics memory and upload on this IO thread instead of on the first drawn frame. */
private fun decodeDemo(context: Context, path: String, poster: Boolean): DemoBitmap? {
    val decoded = context.assets.open(path).use { stream ->
        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            if (poster) inSampleSize = 2
        })
    } ?: return null
    val background = Color(decoded.getPixel(0, 0))
    val bitmap = if (poster) decoded else decoded.copy(Bitmap.Config.HARDWARE, false)?.also { decoded.recycle() } ?: decoded
    return DemoBitmap(path, bitmap.asImageBitmap(), background)
}

/** Keeps only the last sheet shown, and drops it a second after the last panel leaves (the gap covers a phase
 *  change, which disposes one panel before the next appears). Decodes run one at a time, so rapid skips never stack buffers. */
private object DemoSheets {
    private val lock = Mutex()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private var panels = 0
    var last: DemoBitmap? = null
        private set
    fun peek(path: String): DemoBitmap? = last?.takeIf { it.path == path }
    suspend fun load(context: Context, path: String): DemoBitmap? = lock.withLock {
        peek(path) ?: withContext(Dispatchers.IO) { decodeDemo(context, path, poster = false) }?.also { if (panels > 0) last = it }
    }
    fun enter() { panels++ }
    fun leave() { if (--panels == 0) main.postDelayed({ if (panels == 0) last = null }, 1000) }
}

/** Decode off the main thread. Until the new view is ready the previous image stays, so the panel never flashes empty.
 *  [frame] is read only while drawing: a new frame index redraws the canvas without a recomposition. */
@Composable
private fun DemoFrame(figure: String, guide: StretchDemo, view: Int, frame: () -> Int, mirror: Boolean,
    modifier: Modifier, poster: Boolean = false, description: String = guide.name) {
    val context = LocalContext.current
    val path = "stretch_demos/$figure-$view${if (poster) "-poster" else ""}.webp"
    if (!poster) DisposableEffect(Unit) { DemoSheets.enter(); onDispose { DemoSheets.leave() } }
    val loaded by produceState(if (poster) null else DemoSheets.peek(path) ?: DemoSheets.last?.takeIf { it.path.startsWith("stretch_demos/$figure-") }, path) {
        // A cancelled load never assigns, so a late result for the previous angle is never drawn as the new one.
        value = if (poster) withContext(Dispatchers.IO) { decodeDemo(context, path, poster = true) } else DemoSheets.load(context, path)
    }
    Canvas(modifier.clip(RoundedCornerShape(if (poster) 6.dp else 16.dp))
        .background(loaded?.background ?: Color(0xFFF2F2F2))
        .semantics { contentDescription = description }) {
        val bitmap = loaded?.image ?: return@Canvas
        val width = if (poster) bitmap.width else guide.frameWidth
        val height = if (poster) bitmap.height else guide.frameHeight
        val ratio = minOf(size.width / width, size.height / height)
        val dest = IntSize((width * ratio).roundToInt(), (height * ratio).roundToInt())
        val origin = IntOffset(((size.width - dest.width) / 2).roundToInt(), ((size.height - dest.height) / 2).roundToInt())
        val safeFrame = frame().coerceIn(0, guide.frameCount - 1)
        val source = if (poster) IntOffset.Zero else IntOffset((safeFrame % guide.columns) * width, (safeFrame / guide.columns) * height)
        scale(if (mirror) -1f else 1f, 1f) {
            drawImage(bitmap, srcOffset = source, srcSize = IntSize(width, height), dstOffset = origin, dstSize = dest)
        }
    }
}

@Composable
fun DemoPoster(figure: String, modifier: Modifier, mirror: Boolean = false) {
    val context = LocalContext.current
    val guide = remember(figure) { DemoCatalog.read(context)[figure] }
    if (guide != null) DemoFrame(figure, guide, 0, { guide.frameCount - 1 }, mirror, modifier, poster = true)
    else Box(modifier, contentAlignment = Alignment.Center) { Text("No demo", color = K.Muted, fontSize = 12.sp) }
}

/** A lesson before Start; a timer-driven demonstration during the exercise. */
@Composable
fun StretchDemonstration(figure: String, mirror: Boolean = false, preview: Boolean = true,
    exerciseProgress: Float? = null, compact: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val guide = remember(figure) { DemoCatalog.read(context)[figure] }
    if (guide == null) {
        Text("No demonstration for this stretch", color = K.Muted, modifier = modifier)
        return
    }
    var view by rememberSaveable(figure, mirror) { mutableIntStateOf(0) }
    var playing by rememberSaveable(figure, mirror) { mutableStateOf(true) }
    // Written every vsync but never read during composition, so it causes no recomposition.
    var elapsedMs by rememberSaveable(figure, mirror) { mutableLongStateOf(0L) }
    var playFrame by rememberSaveable(figure, mirror) { mutableIntStateOf(guide.previewFrame(0)) }
    var manualFrame by rememberSaveable(figure, mirror) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // The sprites play at 13-20 frames per second. The vsync clock only picks the index; an unchanged index writes nothing.
    LaunchedEffect(playing, preview, visible, figure, mirror) {
        if (playing && preview && visible) {
            val clock = DemoClock(elapsedMs)
            while (true) withFrameNanos { now ->
                elapsedMs = clock.elapsedMs(now)
                playFrame = guide.previewFrame(elapsedMs)
            }
        }
    }
    val exerciseFrame = if (preview) 0 else guide.exerciseFrame(exerciseProgress)
    val frame = { if (!preview) exerciseFrame else if (playing) playFrame else manualFrame }
    val previewStep by remember(guide, mirror) { derivedStateOf { guide.stepAt(if (playing) playFrame else manualFrame) } }
    val selectedStep = if (preview) previewStep else
        guide.steps.indices.minBy { kotlin.math.abs(guide.steps[it].frame - exerciseFrame) }
    val instruction = guide.steps[selectedStep].instruction.forDemoSide(mirror)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DemoFrame(figure, guide, view, frame, mirror,
            Modifier.fillMaxWidth().height(if (compact) 208.dp else 272.dp),
            description = "${guide.name}. ${guide.steps[selectedStep].title}. $instruction")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (preview) DemoControl(if (playing) "Pause demo" else "Play demo") {
                if (playing) manualFrame = playFrame else { elapsedMs = 0L; playFrame = guide.previewFrame(0) }
                playing = !playing
            } else Spacer(Modifier.width(1.dp))
            DemoControl("Change view", "Change camera angle. View ${view + 1} of 2") { view = 1 - view }
        }
        if (preview) Row(Modifier.fillMaxWidth()) {
            guide.steps.forEachIndexed { i, step ->
                Column(Modifier.weight(1f).heightIn(min = 48.dp)
                    .selectable(selected = selectedStep == i, role = Role.Tab,
                        onClick = { playing = false; manualFrame = step.frame })
                    .semantics { contentDescription = "Step ${i + 1}: ${step.title}" },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Text("${i + 1}. ${step.title}", color = if (selectedStep == i) K.Accent else Color.White,
                        minLines = 2, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (selectedStep == i) K.Accent else Color.White.copy(alpha = .2f)))
                }
            }
        }
        Text(instruction, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodyLarge, minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), textAlign = TextAlign.Start)
    }
}

@Composable
private fun DemoControl(text: String, description: String = text, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)
        .semantics { contentDescription = description }, contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text(text, style = MaterialTheme.typography.bodyMedium) }
}
