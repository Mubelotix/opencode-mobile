package page.dera.opencode.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlin.time.TimeSource

// Instrumentation to find where frames go. Everything logs with the OC_PERF
// prefix: `adb logcat -s System.out | grep OC_PERF`. Flip ENABLED off for
// normal use — FrameStatsLogger alone keeps the UI redrawing every vsync.
internal object Perf {
    const val ENABLED = true

    val clock = TimeSource.Monotonic

    fun ms(mark: TimeSource.Monotonic.ValueTimeMark): Double =
        mark.elapsedNow().inWholeMicroseconds / 1000.0

    inline fun <T> timed(tag: String, thresholdMs: Double = 0.0, block: () -> T): T {
        if (!ENABLED) return block()
        val t0 = clock.markNow()
        val result = block()
        val elapsed = ms(t0)
        if (elapsed >= thresholdMs) println("OC_PERF: $tag took=${elapsed}ms")
        return result
    }
}

// Logs every frame gap above ~1.5 vsyncs, plus a summary every ~5s of frames.
@Composable
internal fun FrameStatsLogger() {
    if (!Perf.ENABLED) return
    LaunchedEffect(Unit) {
        var last = 0L
        var frames = 0
        var sum = 0.0
        var worst = 0.0
        var slow = 0
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val ms = (now - last) / 1_000_000.0
                    frames++
                    sum += ms
                    if (ms > worst) worst = ms
                    if (ms > 24) {
                        slow++
                        println("OC_PERF: JANK frame=${ms}ms")
                    }
                    if (frames >= 300) {
                        println("OC_PERF: frame-summary n=$frames avg=${sum / frames}ms worst=${worst}ms slow(>24ms)=$slow")
                        frames = 0; sum = 0.0; worst = 0.0; slow = 0
                    }
                }
                last = now
            }
        }
    }
}

@Composable
internal fun ScrollStatsLogger(listState: LazyListState) {
    if (!Perf.ENABLED) return
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            println(
                "OC_PERF: scroll ${if (scrolling) "START" else "STOP"} " +
                    "firstVisible=${listState.firstVisibleItemIndex} offset=${listState.firstVisibleItemScrollOffset}"
            )
        }
    }
}

// Times measure+layout of the wrapped row (captures text layout cost, which
// composition timing does not see).
internal fun Modifier.timedMeasure(tag: String, thresholdMs: Double = 2.0): Modifier {
    if (!Perf.ENABLED) return fillMaxWidth()
    return fillMaxWidth().layout { measurable, constraints ->
        val t0 = Perf.clock.markNow()
        val placeable = measurable.measure(constraints)
        val elapsed = Perf.ms(t0)
        if (elapsed >= thresholdMs) println("OC_PERF: measure $tag took=${elapsed}ms")
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
