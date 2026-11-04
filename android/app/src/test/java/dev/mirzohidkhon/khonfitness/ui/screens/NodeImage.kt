package dev.mirzohidkhon.khonfitness.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlin.math.roundToInt

/**
 * What [node] looks like on screen: [view]'s window drawn into a bitmap, cut to the node's bounds. Robolectric's
 * `captureToImage` waits for a redraw that never comes, so the window is drawn by hand (native graphics only).
 */
fun ComposeContentTestRule.nodeImage(view: View, node: SemanticsNodeInteraction): Bitmap {
    waitForIdle()
    val r = node.fetchSemanticsNode().boundsInWindow
    return runOnIdle {
        val root = view.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))
        val left = r.left.roundToInt().coerceIn(0, bmp.width - 1)
        val top = r.top.roundToInt().coerceIn(0, bmp.height - 1)
        Bitmap.createBitmap(bmp, left, top, (r.right.roundToInt() - left).coerceIn(1, bmp.width - left), (r.bottom.roundToInt() - top).coerceIn(1, bmp.height - top))
    }
}
