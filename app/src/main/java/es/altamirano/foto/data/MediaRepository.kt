package es.altamirano.foto.data

import android.content.Context
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class MediaRepository(private val context: Context) {
    private val resolver get() = context.contentResolver

    /** Todas las imágenes y vídeos que indexa el sistema (todos los volúmenes). */
    fun scan(): List<MediaItem> {
        val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            FileColumns._ID, FileColumns.DATA, FileColumns.DISPLAY_NAME, FileColumns.MIME_TYPE,
            FileColumns.MEDIA_TYPE, FileColumns.SIZE, FileColumns.DATE_TAKEN, FileColumns.DATE_MODIFIED,
            FileColumns.DURATION, FileColumns.WIDTH, FileColumns.HEIGHT,
        )
        val selection = "${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO})" +
            " AND ${FileColumns.SIZE} > 0"
        val out = ArrayList<MediaItem>(8192)
        resolver.query(uri, projection, selection, null, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(FileColumns._ID)
            val iData = c.getColumnIndexOrThrow(FileColumns.DATA)
            val iName = c.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val iMime = c.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
            val iType = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val iSize = c.getColumnIndexOrThrow(FileColumns.SIZE)
            val iTaken = c.getColumnIndexOrThrow(FileColumns.DATE_TAKEN)
            val iMod = c.getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
            val iDur = c.getColumnIndexOrThrow(FileColumns.DURATION)
            val iW = c.getColumnIndexOrThrow(FileColumns.WIDTH)
            val iH = c.getColumnIndexOrThrow(FileColumns.HEIGHT)
            while (c.moveToNext()) {
                val path = c.getString(iData) ?: continue
                val slash = path.lastIndexOf('/')
                if (slash <= 0) continue
                val folder = path.substring(0, slash)
                if (isTrash(folder)) continue
                val id = c.getLong(iId)
                val video = c.getInt(iType) == FileColumns.MEDIA_TYPE_VIDEO
                val modified = c.getLong(iMod) * 1000
                val taken = c.getLong(iTaken)
                out += MediaItem(
                    id = id,
                    path = path,
                    name = c.getString(iName) ?: path.substring(slash + 1),
                    folder = folder,
                    isVideo = video,
                    mime = c.getString(iMime) ?: "",
                    size = c.getLong(iSize),
                    date = if (taken > 0) taken else modified,
                    modified = modified,
                    durationMs = c.getLong(iDur),
                    width = c.getInt(iW),
                    height = c.getInt(iH),
                )
            }
        }
        return out
    }

    private fun isTrash(folder: String) = folder.contains("/.trashed") || folder.contains("/.Trash")

    /** Avisa (agrupando ráfagas) cuando cambia algo en MediaStore. */
    fun changes(): Flow<Unit> = callbackFlow {
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        resolver.registerContentObserver(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), true, obs)
        awaitClose { resolver.unregisterContentObserver(obs) }
    }

    /**
     * Borra los archivos para siempre. Con «acceso a todos los archivos» no hace falta pedir
     * confirmación al sistema. Devuelve los que se han borrado.
     */
    fun delete(items: Collection<MediaItem>): Set<Long> {
        val done = HashSet<Long>()
        val toScan = ArrayList<String>()
        for (m in items) {
            val f = File(m.path)
            val viaStore = m.indexed && runCatching { resolver.delete(m.uri, null, null) > 0 }.getOrDefault(false)
            if (f.exists() && !f.delete()) continue
            if (!viaStore && m.indexed) toScan += m.path
            done += m.id
        }
        if (toScan.isNotEmpty()) MediaScannerConnection.scanFile(context, toScan.toTypedArray(), null, null)
        return done
    }

    /** Borra el contenido multimedia de las carpetas y, si quedan vacías, las carpetas. */
    fun deleteFolders(folders: Collection<String>, items: Collection<MediaItem>): Int {
        val n = delete(items).size
        for (path in folders) {
            val dir = File(path)
            val rest = dir.listFiles().orEmpty().filterNot { it.name == ".nomedia" || it.name == ".thumbnails" }
            if (rest.isEmpty()) {
                dir.listFiles()?.forEach { it.deleteRecursively() }
                dir.delete()
            }
        }
        return n
    }
}
