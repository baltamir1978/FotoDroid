package es.altamirano.foto.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import es.altamirano.foto.data.MediaItem
import java.text.DateFormat
import java.util.Date

const val THUMB_PX = 320

/** Lo que dicen los diálogos de borrar cuando hay papelera. */
const val TRASH_NOTE = "Irá a la papelera, donde se guarda 30 días."

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/** URI que otra app puede abrir: la de MediaStore o, si no está indexado, la de nuestro FileProvider. */
fun MediaItem.externalUri(ctx: Context): Uri =
    if (indexed) uri else FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", File(path))

fun share(ctx: Context, items: List<MediaItem>) {
    if (items.isEmpty()) return
    val uris = items.map { it.externalUri(ctx) }
    val mime = items.map { it.mime }.distinct().singleOrNull()
        ?: if (items.all { it.isVideo }) "video/*" else if (items.none { it.isVideo }) "image/*" else "*/*"
    val intent = if (items.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
    else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    intent.type = mime
    intent.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(intent, null))
}

fun openWith(ctx: Context, item: MediaItem) {
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(item.externalUri(ctx), item.mime.ifEmpty { if (item.isVideo) "video/*" else "image/*" })
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { ctx.startActivity(Intent.createChooser(i, null)) }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm, color = Color(0xFFE5484D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun NameDialog(title: String, initial: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onDismiss(); onOk(name.trim()) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun InfoDialog(item: MediaItem, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val lines = buildList {
        add(item.name)
        add(item.folder)
        add(Formatter.formatFileSize(ctx, item.size))
        if (item.width > 0) add("${item.width} × ${item.height}")
        if (item.isVideo && item.durationMs > 0) add(formatDuration(item.durationMs))
        add(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.date)))
        add(item.mime)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(lines.joinToString("\n")) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
