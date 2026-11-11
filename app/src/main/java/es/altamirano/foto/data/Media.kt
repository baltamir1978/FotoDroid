package es.altamirano.foto.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Immutable

/** Un archivo de imagen o vídeo tal como lo indexa MediaStore. */
@Immutable
data class MediaItem(
    val id: Long,
    val path: String,
    val name: String,
    /** Carpeta que lo contiene (ruta absoluta, sin barra final). */
    val folder: String,
    val isVideo: Boolean,
    val mime: String,
    val size: Long,
    /** Fecha de captura si la hay; si no, la de modificación (ms). */
    val date: Long,
    val modified: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
) {
    /** URI de MediaStore (se calcula al usarla). */
    val uri: Uri by lazy {
        ContentUris.withAppendedId(
            if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), id,
        )
    }
}

enum class Category { NORMAL, HIDDEN, IGNORED }

/**
 * Lo que se muestra como «carpeta»: una carpeta real o un grupo de carpetas juntadas, cuyos
 * archivos forman una sola lista continua.
 */
@Immutable
data class Entry(
    /** Ruta absoluta, o `g:<id>` si es un grupo. */
    val key: String,
    val name: String,
    val folders: List<String>,
    val items: List<MediaItem>,
    val category: Category,
) {
    val isGroup get() = key.startsWith(GROUP_PREFIX)
    val cover get() = items.firstOrNull()
    val newest get() = items.maxOfOrNull { it.date } ?: 0L
    val videoCount get() = items.count { it.isVideo }

    companion object { const val GROUP_PREFIX = "g:" }
}
