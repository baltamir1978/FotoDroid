package es.altamirano.foto.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Un archivo en la papelera: dónde está ahora y a dónde vuelve. */
@Serializable
data class TrashedItem(
    /** Ruta dentro de la carpeta de la papelera. */
    val file: String,
    /** Ruta que tenía antes de borrarlo. */
    val original: String,
    val deletedAt: Long,
    val size: Long = 0,
    val isVideo: Boolean = false,
) {
    val name get() = original.substringAfterLast('/')

    /** Días que le quedan antes de borrarse solo; null si no caduca (ver [Trash.list]). */
    fun daysLeft(now: Long): Int? =
        if (deletedAt <= 0) null else (Trash.KEEP_DAYS - (now - deletedAt) / (24L * 3600 * 1000)).toInt().coerceAtLeast(0)
}

/**
 * La papelera: lo borrado se **mueve** a `.FotoDroid-papelera` en la raíz de su mismo volumen
 * (un cambio de nombre, sin copiar nada) y se borra de verdad a los [KEEP_DAYS] días. Si un
 * archivo no se puede mover, se queda donde está: nunca se borra «por si acaso».
 *
 * El índice (`papelera.json`) dice de dónde vino cada uno. Lo que haya en la carpeta sin estar
 * en el índice (índice perdido, datos de la app borrados) sale igualmente en la lista, vuelve a
 * `Pictures/FotoDroid recuperado` y **no caduca solo**: sin saber cuándo se borró, no se toca.
 *
 * Sin nada de Android: se prueba en la JVM.
 */
class Trash(private val indexFile: File, private val roots: () -> List<File>) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun readIndex(): List<TrashedItem> =
        runCatching { json.decodeFromString(ListSerializer(TrashedItem.serializer()), indexFile.readText()) }.getOrDefault(emptyList())

    private fun writeIndex(items: List<TrashedItem>) {
        val tmp = File(indexFile.parentFile, indexFile.name + ".tmp")
        tmp.writeText(json.encodeToString(ListSerializer(TrashedItem.serializer()), items))
        tmp.renameTo(indexFile)
    }

    private fun rootOf(path: String): File? =
        roots().filter { path.startsWith(it.path + "/") }.maxByOrNull { it.path.length }

    /** Lo que hay en la papelera, lo último borrado primero. */
    @Synchronized
    fun list(): List<TrashedItem> {
        val indexed = readIndex().filter { File(it.file).isFile }
        val known = indexed.mapTo(HashSet()) { it.file }
        val orphans = roots().flatMap { root ->
            File(root, DIR).listFiles().orEmpty()
                .filter { it.isFile && it.name != ".nomedia" && it.path !in known }
                .map { f ->
                    val name = f.name.replaceFirst(PREFIX, "")
                    val ext = name.substringAfterLast('.', "").lowercase()
                    TrashedItem(f.path, File(File(root, RECOVERED), name).path, deletedAt = 0, size = f.length(), isVideo = ext in FileScanner.VIDEO)
                }
        }
        return indexed.sortedByDescending { it.deletedAt } + orphans.sortedBy { it.file }
    }

    /** Mueve el archivo a la papelera. Devuelve null (y no toca nada) si no se puede. */
    @Synchronized
    fun trash(path: String, isVideo: Boolean, now: Long = System.currentTimeMillis()): TrashedItem? {
        val source = File(path)
        if (!source.isFile) return null
        val root = rootOf(path) ?: return null
        val dir = File(root, DIR)
        if (!dir.isDirectory && !dir.mkdirs()) return null
        runCatching { File(dir, ".nomedia").createNewFile() }
        var n = 0
        var target: File
        do { target = File(dir, "$now-${n++}-${source.name}") } while (target.exists())
        val size = source.length()
        if (!source.renameTo(target)) return null
        val item = TrashedItem(target.path, path, now, size, isVideo)
        writeIndex(readIndex() + item)
        return item
    }

    /**
     * Devuelve el archivo a su sitio (rehaciendo la carpeta si ya no existe). Si allí hay ahora
     * otro con el mismo nombre, vuelve como «nombre (1).ext». Devuelve la ruta final, o null.
     */
    @Synchronized
    fun restore(item: TrashedItem): String? {
        val source = File(item.file)
        if (!source.isFile) return null
        val wanted = File(item.original)
        val parent = wanted.parentFile ?: return null
        if (!parent.isDirectory && !parent.mkdirs()) return null
        var target = wanted
        var n = 1
        while (target.exists()) {
            val base = wanted.name.substringBeforeLast('.', wanted.name)
            val ext = wanted.name.substringAfterLast('.', "").let { if (it.isEmpty() || !wanted.name.contains('.')) "" else ".$it" }
            target = File(parent, "$base (${n++})$ext")
        }
        if (!source.renameTo(target)) return null
        writeIndex(readIndex().filterNot { it.file == item.file })
        return target.path
    }

    /** Borra para siempre. Devuelve cuántos. */
    @Synchronized
    fun purge(items: Collection<TrashedItem>): Int {
        var done = 0
        val gone = HashSet<String>()
        for (item in items) {
            val f = File(item.file)
            // Sólo se borra lo que está dentro de una carpeta de papelera: nada de fuera, pase lo que pase.
            if (f.parentFile?.name != DIR) continue
            if (!f.exists() || f.delete()) { gone += item.file; done++ }
        }
        writeIndex(readIndex().filterNot { it.file in gone })
        return done
    }

    /** Borra lo que lleva más de [KEEP_DAYS] días. Lo que no está en el índice no caduca. */
    @Synchronized
    fun purgeExpired(now: Long = System.currentTimeMillis()): Int {
        val limit = now - KEEP_DAYS * 24L * 3600 * 1000
        return purge(readIndex().filter { it.deletedAt in 1..limit })
    }

    companion object {
        /** La carpeta de la papelera, en la raíz de cada volumen. Con `.nomedia`: el sistema no la indexa. */
        const val DIR = ".FotoDroid-papelera"
        const val KEEP_DAYS = 30
        /** A dónde vuelve lo que está en la papelera sin constar de dónde vino. */
        const val RECOVERED = "Pictures/FotoDroid recuperado"
        private val PREFIX = Regex("""^\d+-\d+-""")
    }
}
