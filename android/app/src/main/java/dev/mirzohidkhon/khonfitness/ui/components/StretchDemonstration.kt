package dev.mirzohidkhon.khonfitness.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.mirzohidkhon.khonfitness.data.DemoClock
import dev.mirzohidkhon.khonfitness.data.DemoView
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.demoPhotoPath
import dev.mirzohidkhon.khonfitness.data.forDemoSide
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private object DemoCatalog {
    @Volatile private var cached: Map<String, StretchDemo>? = null
    fun read(context: Context): Map<String, StretchDemo> = cached ?: synchronized(this) {
        cached ?: context.assets.open("stretch_demos/guides.json").bufferedReader().use {
            Json.decodeFromString<Map<String, StretchDemo>>(it.readText())
        }.also { cached = it }
    }
}

/** Width over height of every demonstration photo (720 x 820 px). */
const val DEMO_PHOTO_ASPECT = 720f / 820f
val DEMO_PANEL_MIN_HEIGHT = 240.dp

/** The photo a panel draws, `<path>` or `<path> mirrored`; absent while the panel has no photo yet. */
val ShownDemoPhoto = SemanticsPropertyKey<String>("ShownDemoPhoto")
private var SemanticsPropertyReceiver.shownDemoPhoto by ShownDemoPhoto

private data class DemoPhoto(val path: String, val image: ImageBitmap)

/** Full color avoids banding in the studio background. Null when the file is missing or is no picture. */
internal fun decodePhoto(context: Context, path: String, sample: Int): ImageBitmap? = try {
    context.assets.open(path).use { stream ->
        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sample
        })
    }?.also { it.prepareToDraw() }?.asImageBitmap()
} catch (e: java.io.IOException) {
    null
}

/**
 * Holds only the photos of one stretch in one view: a load for another stretch or view drops them. [last] lets a new
 * panel start on the photo shown before it. Everything goes a second after the last panel leaves (the gap covers a
 * phase change, which disposes one panel before the next appears). Decodes run one at a time.
 */
private object DemoPhotos {
    private val lock = Mutex()
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private var panels = 0
    private var set = ""
    private val held = HashMap<String, ImageBitmap>()
    @Volatile var last: DemoPhoto? = null
        private set

    fun peek(path: String): DemoPhoto? = synchronized(held) { held[path] }?.let { DemoPhoto(path, it) }

    suspend fun load(context: Context, set: String, path: String): DemoPhoto? = lock.withLock {
        peek(path) ?: withContext(Dispatchers.IO) { decodePhoto(context, path, 1) }?.let { image ->
            synchronized(held) {
                if (panels > 0) {
                    if (this.set != set) { held.clear(); this.set = set }
                    held[path] = image
                }
            }
            DemoPhoto(path, image)
        }
    }

    fun shown(photo: DemoPhoto) { if (panels > 0) last = photo }
    fun enter() { panels++ }
    fun leave() {
        if (--panels == 0) main.postDelayed({ if (panels == 0) synchronized(held) { held.clear(); set = ""; last = null } }, 1000)
    }
}

/**
 * A black panel with the photo fitted inside it, never cropped or stretched. Decodes off the main thread; until the
 * new photo is ready the previous one stays, then the two cross-fade, so the panel never flashes empty.
 */
@Composable
private fun DemoPanel(figure: String, pose: String, view: DemoView, mirror: Boolean, description: String,
    modifier: Modifier) {
    val context = LocalContext.current
    val path = demoPhotoPath(figure, pose, view)
    val set = "$figure-${view.file}"
    DisposableEffect(Unit) { DemoPhotos.enter(); onDispose { DemoPhotos.leave() } }
    // A cancelled load never assigns, so a late photo of the previous pose or view is never drawn as the new one.
    val photo by produceState(DemoPhotos.peek(path) ?: DemoPhotos.last, path) {
        DemoPhotos.load(context, set, path)?.let { value = it }
    }
    SideEffect { photo?.let(DemoPhotos::shown) }
    LaunchedEffect(set) {
        DemoCatalog.read(context)[figure]?.let { guide ->
            (guide.steps.map { it.pose } + guide.reps).distinct().forEach { DemoPhotos.load(context, set, demoPhotoPath(figure, it, view)) }
        }
    }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black).semantics {
        contentDescription = description
        photo?.let { shownDemoPhoto = it.path + if (mirror) " mirrored" else "" }
    }) {
        Crossfade(photo, animationSpec = tween(300), label = "demo photo") { shown ->
            if (shown != null) DemoImage(shown.image, mirror)
        }
    }
}

@Composable
private fun DemoImage(image: ImageBitmap, mirror: Boolean) {
    Image(image, contentDescription = null, contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = if (mirror) -1f else 1f })
}

/** A list asks for every thumbnail at once; one decode at a time keeps a single photo buffer in memory. */
private val posterDecodes = Mutex()

/** The list thumbnail: the hold photo of the side view, decoded at a quarter of its size. */
@Composable
fun DemoPoster(figure: String, modifier: Modifier, mirror: Boolean = false) {
    val context = LocalContext.current
    val guide = remember(figure) { DemoCatalog.read(context)[figure] }
    if (guide == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("No demo", color = K.Muted, fontSize = 12.sp) }
        return
    }
    val path = demoPhotoPath(figure, guide.steps.last().pose, DemoView.SIDE)
    val image by produceState<ImageBitmap?>(null, path) {
        value = posterDecodes.withLock { withContext(Dispatchers.IO) { decodePhoto(context, path, 4) } }
    }
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black).semantics {
        contentDescription = guide.name
        if (image != null) shownDemoPhoto = path
    }) {
        image?.let { DemoImage(it, mirror) }
    }
}

/**
 * A lesson before Start; a timer-driven demonstration during the exercise. With [maxPanelHeight] the photo panel
 * takes the height its parent leaves, from [DEMO_PANEL_MIN_HEIGHT] up to that value, so the parent must give it a
 * bounded height; without it the panel is 208 dp high.
 */
@Composable
fun StretchDemonstration(figure: String, mirror: Boolean = false, preview: Boolean = true,
    exerciseProgress: Float? = null, maxPanelHeight: Dp? = null, modifier: Modifier = Modifier, onPhase: Boolean = false) {
    val context = LocalContext.current
    val guide = remember(figure) { DemoCatalog.read(context)[figure] }
    if (guide == null) {
        // On the player's phase fill the muted gray is too dim (4.1:1 on WorkFill); the player's own muted white is not.
        Text("No demonstration for this stretch", color = if (onPhase) K.OnPhaseMuted else K.Muted, modifier = modifier)
        return
    }
    var view by rememberSaveable(figure, mirror) { mutableStateOf(DemoView.SIDE) }
    var playing by rememberSaveable(figure, mirror) { mutableStateOf(true) }
    // Written every vsync but read only by the derived step and while drawing the timeline, so it causes no recomposition.
    var elapsedMs by rememberSaveable(figure, mirror) { mutableLongStateOf(0L) }
    var manualStep by rememberSaveable(figure, mirror) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(playing, preview, visible, figure, mirror) {
        if (playing && preview && visible) {
            val clock = DemoClock(elapsedMs)
            while (true) withFrameNanos { now -> elapsedMs = clock.elapsedMs(now) }
        }
    }
    val playStep by remember(guide) { derivedStateOf { guide.previewStep(elapsedMs) } }
    val step = when { !preview -> guide.exerciseStep(exerciseProgress); playing -> playStep; else -> manualStep }
    val pose = if (preview) guide.steps[step].pose else guide.exercisePose(exerciseProgress)
    val instruction = guide.steps[step].instruction.forDemoSide(mirror)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val panel = if (maxPanelHeight == null) Modifier.height(208.dp)
            else Modifier.weight(1f, fill = false).heightIn(min = DEMO_PANEL_MIN_HEIGHT, max = maxPanelHeight.coerceAtLeast(DEMO_PANEL_MIN_HEIGHT)).fillMaxHeight()
        Box(panel.fillMaxWidth()) {
            // Sized by the box, so the photo's own size never counts toward the minimum the player reserves for it.
            DemoPanel(figure, pose, view, mirror, "${guide.name}. ${guide.steps[step].title}. $instruction", Modifier.matchParentSize())
            // Over the image's bottom corners, 10 dp in (6 dp and the 48 dp target's edge), like a video player's controls.
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (preview) DemoControl(if (playing) Icons.Pause else Icons.Play, if (playing) "Pause demo" else "Play demo") {
                    if (playing) manualStep = playStep else elapsedMs = 0L
                    playing = !playing
                } else Spacer(Modifier.width(1.dp))
                DemoControl(Icons.Rotate3d, "Change camera angle. View ${view.ordinal + 1} of 2") {
                    view = if (view == DemoView.SIDE) DemoView.FRONT else DemoView.SIDE
                }
            }
        }
        // The fill is read while drawing, so the line runs on through a step without recomposing the steps.
        if (preview) StepTimeline(guide.steps.map { it.title }, step,
            fill = { if (playing) guide.previewFill(elapsedMs) else guide.stepFill(manualStep) },
            onSelect = { playing = false; manualStep = it }, modifier = Modifier.padding(top = 14.dp))
        // minLines does not count toward the text's minimum intrinsic height, which the player uses to size the photo.
        val style = MaterialTheme.typography.bodyLarge
        val measurer = rememberTextMeasurer()
        val threeLines = with(LocalDensity.current) { remember(style, this) { measurer.measure("1\n2\n3", style).size.height.toDp() } }
        Text(instruction, color = K.Text, style = style, minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp).heightIn(min = threeLines), textAlign = TextAlign.Start)
    }
}
