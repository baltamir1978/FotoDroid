package es.altamirano.foto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.altamirano.foto.data.Entry
import es.altamirano.foto.data.Library
import es.altamirano.foto.data.MediaItem

/** Una carpeta real a la que se puede mover (los grupos se despliegan en sus carpetas). */
private data class Target(val path: String, val name: String, val group: String?, val cover: MediaItem?)

/**
 * «Mover a…»: las carpetas en el orden de la pantalla principal (y las ocultas si se ven), sin
 * aquellas en las que ya está todo lo elegido, y al principio «Carpeta nueva» (en Imágenes).
 */
@Composable
fun MoveDialog(entries: List<Entry>, from: Collection<MediaItem>, picturesDir: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var askNew by remember { mutableStateOf(false) }
    val sources = remember(from) { from.mapTo(HashSet()) { it.folder } }
    val targets = remember(entries, sources) {
        entries.flatMap { e ->
            e.folders.map { f ->
                Target(f, Library.folderName(f), e.name.takeIf { e.isGroup }, e.items.firstOrNull { it.folder == f } ?: e.cover)
            }
        }.filterNot { sources.size == 1 && it.path in sources }.distinctBy { it.path }
    }
    if (askNew) {
        NameDialog("Carpeta nueva en Imágenes", "", onOk = { name ->
            val clean = name.replace('/', ' ').trim().trimStart('.')
            if (clean.isNotEmpty()) { onDismiss(); onPick("$picturesDir/$clean") }
        }, onDismiss = { askNew = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mover ${from.size} ${if (from.size == 1) "archivo" else "archivos"} a…") },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { askNew = true }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CreateNewFolder, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text("Carpeta nueva", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 14.dp))
                    }
                }
                items(targets, key = { it.path }) { t ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onDismiss(); onPick(t.path) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center) {
                            if (t.cover != null) AsyncImage(Thumb(t.cover, 192), null, Modifier.size(48.dp), contentScale = ContentScale.Crop)
                            else Icon(Icons.Rounded.Folder, null)
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(t.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.group?.let { "En el grupo «$it»" } ?: t.path.removePrefix("/storage/emulated/0/"),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
