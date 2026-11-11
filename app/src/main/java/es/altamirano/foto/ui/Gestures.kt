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

data class BarAction(val icon: ImageVector, val label: String, val tint: Color? = null, val onClick: () -> Unit)

/** Barra flotante con forma de píldora (acciones de la selección), al estilo de Android actual. */
@Composable
fun FloatingBar(actions: List<BarAction>, modifier: Modifier = Modifier) {
    Surface(
        modifier, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp, shadowElevation = 8.dp,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            actions.forEach { a ->
                Column(
                    Modifier.clip(CircleShape).clickable(onClick = a.onClick).padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(a.icon, a.label, Modifier.size(22.dp), tint = a.tint ?: MaterialTheme.colorScheme.onSurface)
                    Text(a.label, style = MaterialTheme.typography.labelSmall, color = a.tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
