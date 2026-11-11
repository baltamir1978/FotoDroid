package es.altamirano.foto.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/*
 * «Cristal»: las barras son translúcidas y desenfocan lo que pasa por detrás (Haze). El contenido
 * que se desplaza lleva `Modifier.hazeSource(state)` y la barra `Modifier.glass(state)`.
 */

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun glassStyle(color: Color = MaterialTheme.colorScheme.surface): HazeStyle = HazeMaterials.thin(color)

@Composable
fun Modifier.glass(state: HazeState, color: Color = MaterialTheme.colorScheme.surface): Modifier =
    hazeEffect(state, glassStyle(color))

/** Colores de barra superior transparentes: el fondo lo pone el cristal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun clearBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)

/** Botón de barra: icono relleno dentro de un círculo tonal. */
@Composable
fun BarButton(icon: ImageVector, description: String, highlight: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    FilledTonalIconButton(
        onClick, Modifier.padding(horizontal = 2.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (highlight) c.primaryContainer else c.surfaceContainerHighest.copy(alpha = 0.75f),
            contentColor = if (highlight) c.onPrimaryContainer else c.onSurface,
        ),
    ) { Icon(icon, description, Modifier.size(22.dp)) }
}

data class BarAction(val icon: ImageVector, val label: String, val tint: Color? = null, val onClick: () -> Unit)

/** Barra flotante de cristal con forma de píldora (acciones de la selección). */
@Composable
fun FloatingBar(actions: List<BarAction>, haze: HazeState, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.shadow(12.dp, CircleShape).clip(CircleShape).glass(haze, c.surfaceContainerHigh)
            .border(1.dp, c.outlineVariant.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        actions.forEach { a ->
            Column(
                Modifier.clip(CircleShape).clickable(onClick = a.onClick).padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(a.icon, a.label, Modifier.padding(top = 4.dp, bottom = 2.dp).size(24.dp), tint = a.tint ?: c.onSurface)
                Text(a.label, style = MaterialTheme.typography.labelSmall, color = a.tint ?: c.onSurfaceVariant)
            }
        }
    }
}
