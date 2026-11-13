package es.altamirano.foto.ui

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import es.altamirano.foto.data.FileScanner
import es.altamirano.foto.data.MediaItem
import es.altamirano.foto.data.TrashedItem
import java.io.File

/** El archivo de la papelera como lo entienden las miniaturas (sin indexar: se lee del disco). */
private fun TrashedItem.asMedia(): MediaItem {
    val f = File(file)
    return MediaItem(
        id = FileScanner.idFor(file), path = file, name = name, folder = f.parent ?: "", isVideo = isVideo,
        mime = "", size = size, date = deletedAt, modified = deletedAt, durationMs = 0, width = 0, height = 0,
    )
}

/**
 * La papelera: lo borrado en los últimos 30 días. Tocar un archivo ofrece restaurarlo o borrarlo
 * para siempre; arriba, restaurarlo todo o vaciarla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(vm: AppViewModel) {
    val items by vm.trashed.collectAsState()
    val ctx = LocalContext.current
    var chosen by remember { mutableStateOf<TrashedItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmRestoreAll by remember { mutableStateOf(false) }
    val now = remember(items) { System.currentTimeMillis() }
    fun toast(text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BarButton(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás") { vm.pop() } },
                title = { Text("Papelera") },
                actions = {
                    if (items.isNotEmpty()) {
                        BarButton(Icons.Rounded.RestoreFromTrash, "Restaurar todo") { confirmRestoreAll = true }
                        BarButton(Icons.Rounded.DeleteForever, "Vaciar la papelera") { confirmEmpty = true }
                    }
                },
            )
        },
    ) { pad ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("La papelera está vacía.\nLo que borres se guarda aquí 30 días.", textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                GridCells.Fixed(4), Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = pad.calculateTopPadding() + 4.dp, bottom = pad.calculateBottomPadding() + 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items(items, key = { it.file }) { item ->
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { chosen = item }) {
                        AsyncImage(Thumb(item.asMedia(), THUMB_PX), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        item.daysLeft(now)?.let { days ->
                            Text(if (days == 1) "1 día" else "$days días", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).background(Color(0x80000000), CircleShape).padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                }
            }
        }
    }

    chosen?.let { item ->
        val days = item.daysLeft(now)
        AlertDialog(
            onDismissRequest = { chosen = null },
            title = { Text(item.name) },
            text = {
                Text(listOfNotNull(
                    "Estaba en ${File(item.original).parent}",
                    Formatter.formatFileSize(ctx, item.size),
                    if (days == null) "No consta cuándo se borró: no se borrará solo." else if (days == 1) "Se borrará para siempre mañana." else "Se borrará para siempre en $days días.",
                ).joinToString("\n"))
            },
            confirmButton = {
                TextButton(onClick = { chosen = null; vm.restoreTrashed(listOf(item)) { toast(if (it == 1) "Restaurado" else "No se pudo restaurar") } }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { chosen = null; vm.purgeTrashed(listOf(item)) { toast(if (it == 1) "Borrado para siempre" else "No se pudo borrar") } }) {
                    Text("Borrar para siempre", color = Color(0xFFE5484D))
                }
            },
        )
    }
    if (confirmEmpty) {
        val all = items
        ConfirmDialog("¿Vaciar la papelera?", "Se borrarán para siempre ${all.size} ${if (all.size == 1) "archivo" else "archivos"}. Esto no se puede deshacer.", "Vaciar",
            onConfirm = { vm.purgeTrashed(all) { toast("Borrados para siempre: $it") } }, onDismiss = { confirmEmpty = false })
    }
    if (confirmRestoreAll) {
        val all = items
        AlertDialog(
            onDismissRequest = { confirmRestoreAll = false },
            title = { Text("¿Restaurarlo todo?") },
            text = { Text("${all.size} ${if (all.size == 1) "archivo vuelve" else "archivos vuelven"} a su carpeta.") },
            confirmButton = { TextButton(onClick = { confirmRestoreAll = false; vm.restoreTrashed(all) { toast("Restaurados: $it") } }) { Text("Restaurar") } },
            dismissButton = { TextButton(onClick = { confirmRestoreAll = false }) { Text("Cancelar") } },
        )
    }
}
