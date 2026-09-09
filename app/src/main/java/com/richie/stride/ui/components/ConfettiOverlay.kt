package com.richie.stride.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class ConfettiParticle(
    val startXFraction: Float,
    val vx: Float,
    val vyBurst: Float,
    val color: Color,
    val sizeDp: Float,
    val rotationTurns: Float
)

private val ConfettiColors = listOf(
    Color(0xFFFF6F52), // coral
    Color(0xFFFFB32B), // amber
    Color(0xFFFDF8EE), // cream
    Color(0xFF1AAE98), // teal
    Color(0xFF8C6FF7)  // purple
)

private const val GRAVITY = 1.15f
private const val TOP_FRACTION = 0.26f
private const val DURATION_MS = 1300L

private fun generateParticles(count: Int): List<ConfettiParticle> {
    val random = Random(System.nanoTime())
    return List(count) {
        ConfettiParticle(
            startXFraction = 0.5f + (random.nextFloat() - 0.5f) * 0.6f,
            vx = (random.nextFloat() - 0.5f) * 0.55f,
            vyBurst = -(0.14f + random.nextFloat() * 0.16f),
            color = ConfettiColors[random.nextInt(ConfettiColors.size)],
            sizeDp = 5f + random.nextFloat() * 6f,
            rotationTurns = (random.nextFloat() - 0.5f) * 4f
        )
    }
}

/**
 * A one-shot confetti burst. Calls [onFinished] once the animation completes - the caller
 * should remove it from the tree at that point (e.g. flip a `showConfetti` flag back to
 * false), so re-triggering it later creates a fresh instance with a fresh particle set.
 *
 * Driven by withFrameNanos rather than animateFloatAsState/tween deliberately: those live in
 * the androidx.compose.animation:animation-core artifact, which isn't explicitly declared as
 * a dependency in this project (only possibly available transitively via Material3).
 * withFrameNanos is a core Compose runtime primitive with no such uncertainty.
 */
@Composable
fun ConfettiBurst(onFinished: () -> Unit, particleCount: Int = 60) {
    val particles = remember { generateParticles(particleCount) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            val elapsedMs = (nowNanos - startNanos) / 1_000_000L
            val t = (elapsedMs.toFloat() / DURATION_MS.toFloat()).coerceIn(0f, 1f)
            progress = t
            if (t >= 1f) break
        }
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress
        val alpha = (1f - t).coerceIn(0f, 1f)
        if (alpha <= 0f) return@Canvas
        particles.forEach { p ->
            val x = p.startXFraction * size.width + p.vx * t * size.width
            val y = (TOP_FRACTION + p.vyBurst * t + GRAVITY * t * t) * size.height
            val sizePx = p.sizeDp.dp.toPx()
            rotate(degrees = p.rotationTurns * 360f * t, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - sizePx / 2, y - sizePx / 2),
                    size = Size(sizePx, sizePx)
                )
            }
        }
    }
}
