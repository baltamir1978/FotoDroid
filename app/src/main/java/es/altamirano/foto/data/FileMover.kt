package es.altamirano.foto.data

import java.io.File

/**
 * Mueve archivos a otra carpeta. En el mismo volumen es un cambio de nombre; entre volumenes
 * (memoria interna ↔ tarjeta/USB) se copia, se comprueba el tamaño y sólo entonces se borra el
 * original. Si en el destino ya hay uno con ese nombre, llega como «nombre (1).ext»: nunca se
 * sobrescribe nada.
 *
 * Sin nada de Android: se prueba en la JVM.
 */
object FileMover {
    /** Mueve [path] a la carpeta [dir] (creándola si hace falta). Devuelve la ruta nueva, o null si no se pudo. */
    fun move(path: String, dir: File): String? {
        val source = File(path)
        if (!source.isFile) return null
        if (source.parentFile?.canonicalPath == dir.canonicalPath) return null
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val target = freeName(dir, source.name)
        if (source.renameTo(target)) return target.path
        // Otro volumen: copiar, comprobar y borrar el original.
        val modified = source.lastModified()
        val ok = runCatching {
            source.inputStream().use { i -> target.outputStream().use { o -> i.copyTo(o, 1 shl 16) } }
            target.length() == source.length()
        }.getOrDefault(false)
        if (!ok) { target.delete(); return null }
        target.setLastModified(modified)
        if (!source.delete()) { target.delete(); return null }
        return target.path
    }

    /** «foto.jpg», y si ya existe «foto (1).jpg», «foto (2).jpg»… */
    fun freeName(dir: File, name: String): File {
        var target = File(dir, name)
        if (!target.exists()) return target
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 1
        while (target.exists()) target = File(dir, "$base (${n++})$ext")
        return target
    }
}
