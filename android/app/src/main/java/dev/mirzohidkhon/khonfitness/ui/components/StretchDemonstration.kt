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
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.forDemoSide
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

private data class DemoBitmap(val path: String, val image: ImageBitmap)

/** Decode one sheet at a time on an IO thread. RGB565 limits a full sheet to about 15 MB. */
@Composable
private fun DemoFrame(figure: String, guide: StretchDemo, view: Int, frame: Int, mirror: Boolean,
    modifier: Modifier, poster: Boolean = false, description: String = guide.name) {
    val context = LocalContext.current
    val path = "stretch_demos/$figure-$view${if (poster) "-poster" else ""}.webp"
    val loaded by produceState<DemoBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            val bitmap = context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    if (poster) inSampleSize = 2
                })
            }
            bitmap?.let { DemoBitmap(path, it.asImageBitmap()) }
        }
    }
    Canvas(modifier.clip(RoundedCornerShape(if (poster) 6.dp else 16.dp))
        .background(Color(0xFFEDF0F4)).semantics { contentDescription = description }) {
        // A late result for the previous angle must never be drawn as the new one.
        val bitmap = loaded?.takeIf { it.path == path }?.image ?: return@Canvas
        val width = if (poster) bitmap.width else guide.frameWidth
        val height = if (poster) bitmap.height else guide.frameHeight
        val ratio = minOf(size.width / width, size.height / height)
        val dest = IntSize((width * ratio).roundToInt(), (height * ratio).roundToInt())
        val origin = IntOffset(((size.width - dest.width) / 2).roundToInt(), ((size.height - dest.height) / 2).roundToInt())
        val safeFrame = frame.coerceIn(0, guide.frameCount - 1)
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
    if (guide != null) DemoFrame(figure, guide, 0, guide.frameCount - 1, mirror, modifier, poster = true)
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
    var elapsedMs by rememberSaveable(figure, mirror) { mutableLongStateOf(0L) }
    var manualFrame by rememberSaveable(figure, mirror) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(playing, preview, visible, figure, mirror) {
        if (playing && preview && visible) {
            var previous = android.os.SystemClock.elapsedRealtime()
            while (true) {
                delay(80)
                val now = android.os.SystemClock.elapsedRealtime()
                elapsedMs += now - previous
                previous = now
            }
        }
    }
    val frame = if (preview) { if (playing) guide.previewFrame(elapsedMs) else manualFrame } else guide.exerciseFrame(exerciseProgress)
    val selectedStep = if (preview) guide.stepAt(frame) else guide.steps.lastIndex
    val instruction = guide.steps[selectedStep].instruction.forDemoSide(mirror)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DemoFrame(figure, guide, view, frame, mirror,
            Modifier.fillMaxWidth().height(if (compact) 208.dp else 272.dp),
            description = "${guide.name}. ${guide.steps[selectedStep].title}. $instruction")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (preview) DemoControl(if (playing) "Pause demo" else "Play demo") {
                if (playing) manualFrame = frame else elapsedMs = 0L
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
