package es.altamirano.foto.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Pellizcar con dos dedos cambia el número de columnas (abrir = menos y más grandes).
 * Se intercepta antes que la cuadrícula para que no se desplace mientras tanto.
 */
fun Modifier.pinchColumns(columns: Int, range: IntRange, onChange: (Int) -> Unit) = pointerInput(columns, range) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var zoom = 1f
        var done = false
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = ev.changes.count { it.pressed }
            if (pressed == 0) break
            if (pressed >= 2) {
                ev.changes.forEach { it.consume() }
                if (!done) {
                    zoom *= ev.calculateZoom()
                    val next = when {
                        zoom > 1.25f && columns > range.first -> columns - 1
                        zoom < 0.8f && columns < range.last -> columns + 1
                        else -> null
                    }
                    if (next != null) { done = true; onChange(next) }
                }
            } else if (done) ev.changes.forEach { it.consume() }
        }
    }
}

/** Tamaño de miniatura (px) para una cuadrícula de n columnas a lo ancho de la pantalla, en escalones. */
@Composable
fun thumbPxFor(columns: Int): Int {
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val need = widthPx / columns
    return listOf(192, 256, 384, 512, 768, 1024).firstOrNull { it >= need } ?: 1024
}



/**
 * Pulsación larga en una casilla que también se puede arrastrar (Reorderable): [onLongPress] en
 * cuanto se cumple el tiempo (con vibración, para notar que se ha reconocido) y [onRelease] al
 * soltar después. Quien lo usa decide al soltar si era seleccionar (el orden no ha cambiado) o
 * recolocar. Sólo mira: no consume nada, así que convive con el arrastre y con el toque normal.
 * Si el dedo se mueve antes de tiempo, es desplazar la cuadrícula y no cuenta.
 *
 * Hasta la 0.8.1 la selección dependía de `onDragStopped` de Reorderable, que sólo llega cuando
 * el arrastre ha empezado de verdad: había que mover un poco el dedo para que se reconociera.
 */
fun Modifier.longPressRelease(key: Any?, onLongPress: () -> Unit, onRelease: () -> Unit) = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        val longAt = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis
        var now = down.uptimeMillis
        var long = false
        while (true) {
            val ev = if (long) awaitPointerEvent(PointerEventPass.Initial)
            else withTimeoutOrNull((longAt - now).coerceAtLeast(1)) { awaitPointerEvent(PointerEventPass.Initial) }
            if (ev == null) { long = true; onLongPress(); continue }
            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
            now = c.uptimeMillis
            if (!c.pressed) { if (long) onRelease(); break }
            if (!long && (ev.changes.count { it.pressed } > 1 || (c.position - down.position).getDistance() > slop)) break
        }
    }
}
