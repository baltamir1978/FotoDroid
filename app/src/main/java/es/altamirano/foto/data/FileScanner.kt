package es.altamirano.foto.data

import android.content.Context
import android.os.storage.StorageManager
import java.io.File

/**
 * Recorre el almacenamiento buscando fotos y vídeos que MediaStore no tiene: carpetas con
 * `.nomedia`, carpetas que empiezan por punto, o archivos que el sistema no ha indexado.
 * Necesita «acceso a todos los archivos». `Android/data` y `Android/obb` no se pueden leer.
 */
class FileScanner(private val context: Context) {

    fun roots(): List<File> {
        val sm = context.getSystemService(StorageManager::class.java)
        return sm.storageVolumes.mapNotNull { it.directory }.filter { it.canRead() }
            .ifEmpty { listOf(File("/storage/emulated/0")) }
    }

    /**
     * @param known rutas que ya tiene MediaStore (no se repiten).
     * @param skip carpetas que no hace falta recorrer (las ignoradas).
     */
    fun scan(known: Set<String>, skip: Set<String>, dotFolders: Boolean): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        for (root in roots()) walk(root, root.path, known, skip, dotFolders, out, 0)
        return out
    }

    private fun walk(dir: File, rootPath: String, known: Set<String>, skip: Set<String>, dots: Boolean, out: MutableList<MediaItem>, depth: Int) {
        if (depth > 24) return
        val path = dir.path
        if (path in skip) return
        val children = dir.listFiles() ?: return
        for (f in children) {
            val name = f.name
            if (f.isDirectory) {
                if (name.startsWith(".") && (!dots || name.lowercase() in JUNK_DIRS || name.startsWith(".trashed"))) continue
                if (depth == 0 && name == "Android") { walkAndroid(f, rootPath, known, skip, dots, out); continue }
                walk(f, rootPath, known, skip, dots, out, depth + 1)
            } else {
                val ext = name.substringAfterLast('.', "").lowercase()
                val video = ext in VIDEO
                if (!video && ext !in IMAGE) continue
                val fp = f.path
                if (fp in known) continue
                val len = f.length()
                if (len <= 0) continue
                val mod = f.lastModified()
                out += MediaItem(
                    id = idFor(fp), path = fp, name = name, folder = path, isVideo = video,
                    mime = MIME[ext] ?: if (video) "video/$ext" else "image/$ext",
                    size = len, date = mod, modified = mod, durationMs = 0, width = 0, height = 0,
                )
            }
        }
    }

    /** En `Android` sólo se puede entrar en `media` (WhatsApp, Telegram…). */
    private fun walkAndroid(dir: File, rootPath: String, known: Set<String>, skip: Set<String>, dots: Boolean, out: MutableList<MediaItem>) {
        val media = File(dir, "media")
        if (media.isDirectory) walk(media, rootPath, known, skip, dots, out, 2)
    }

    companion object {
        /** Id negativo y estable a partir de la ruta (FNV-1a de 64 bits). */
        fun idFor(path: String): Long {
            var h = -0x340d631b7bdddcdbL
            for (ch in path) { h = h xor ch.code.toLong(); h *= 0x100000001b3L }
            return if (h == Long.MIN_VALUE) -1 else -(h and Long.MAX_VALUE) - 1
        }

        private val JUNK_DIRS = setOf(".thumbnails", ".thumbs", ".cache", ".trash", ".tmp", ".temp", ".git")
        val IMAGE = setOf("jpg", "jpeg", "jpe", "png", "gif", "webp", "heic", "heif", "avif", "bmp", "jxl", "tif", "tiff",
            "dng", "arw", "cr2", "cr3", "nef", "orf", "rw2", "raf", "srw", "pef")
        val VIDEO = setOf("mp4", "m4v", "mov", "mkv", "webm", "avi", "3gp", "3g2", "ts", "mts", "m2ts", "flv", "wmv",
            "mpg", "mpeg", "ogv", "asf", "vob", "divx", "f4v")
        private val MIME = mapOf(
            "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "jpe" to "image/jpeg", "png" to "image/png", "gif" to "image/gif",
            "webp" to "image/webp", "heic" to "image/heic", "heif" to "image/heif", "avif" to "image/avif", "bmp" to "image/bmp",
            "tif" to "image/tiff", "tiff" to "image/tiff", "dng" to "image/x-adobe-dng",
            "mp4" to "video/mp4", "m4v" to "video/mp4", "mov" to "video/quicktime", "mkv" to "video/x-matroska",
            "webm" to "video/webm", "avi" to "video/x-msvideo", "3gp" to "video/3gpp", "ts" to "video/mp2t",
            "mts" to "video/mp2t", "m2ts" to "video/mp2t", "flv" to "video/x-flv", "wmv" to "video/x-ms-wmv",
            "mpg" to "video/mpeg", "mpeg" to "video/mpeg",
        )
    }
}
