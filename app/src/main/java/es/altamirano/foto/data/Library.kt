package es.altamirano.foto.data

import androidx.compose.runtime.Immutable

/** Las tres listas de carpetas, ya filtradas, agrupadas y en el orden del usuario. */
@Immutable
data class Library(
    val normal: List<Entry> = emptyList(),
    val hidden: List<Entry> = emptyList(),
    val ignored: List<Entry> = emptyList(),
) {
    fun list(c: Category) = when (c) {
        Category.NORMAL -> normal
        Category.HIDDEN -> hidden
        Category.IGNORED -> ignored
    }

    fun find(key: String) = normal.find { it.key == key } ?: hidden.find { it.key == key } ?: ignored.find { it.key == key }

    companion object {
        fun build(items: List<MediaItem>, config: Config): Library {
            val newestFirst = config.settings.newestFirst
            val ignoredPaths = config.categories.filter { (k, c) -> c == Category.IGNORED && !k.startsWith(Entry.GROUP_PREFIX) }.keys
            fun ignoredBy(folder: String) = ignoredPaths.filter { folder == it || folder.startsWith("$it/") }

            val groupOf = HashMap<String, String>()
            config.groups.forEach { (id, g) -> g.folders.forEach { groupOf[it] = Entry.GROUP_PREFIX + id } }

            val byFolder = items.groupBy { it.folder }
            val shown = HashMap<String, MutableList<MediaItem>>()   // clave de entrada → archivos
            val shownFolders = HashMap<String, MutableList<String>>()
            val underIgnored = HashMap<String, MutableList<MediaItem>>()
            for ((folder, list) in byFolder) {
                val ign = ignoredBy(folder)
                if (ign.isNotEmpty()) {
                    ign.forEach { underIgnored.getOrPut(it) { mutableListOf() }.addAll(list) }
                    continue
                }
                val key = groupOf[folder] ?: folder
                shown.getOrPut(key) { mutableListOf() }.addAll(list)
                shownFolders.getOrPut(key) { mutableListOf() }.add(folder)
            }

            fun sorted(l: List<MediaItem>): List<MediaItem> {
                val cmp = compareBy<MediaItem>({ it.date }, { it.name })
                return l.sortedWith(if (newestFirst) cmp.reversed() else cmp)
            }
            fun name(key: String) = if (key.startsWith(Entry.GROUP_PREFIX))
                config.groups[key.removePrefix(Entry.GROUP_PREFIX)]?.name ?: "Grupo"
            else folderName(key)

            val all = shown.map { (key, list) ->
                Entry(key, name(key), shownFolders[key].orEmpty().sorted(), sorted(list), config.categoryOf(key))
            }
            val normal = all.filter { it.category == Category.NORMAL }
            val hidden = all.filter { it.category == Category.HIDDEN }
            val ignored = all.filter { it.category == Category.IGNORED } +
                ignoredPaths.map { p -> Entry(p, folderName(p), listOf(p), sorted(underIgnored[p].orEmpty()), Category.IGNORED) }

            return Library(
                normal = ordered(normal, config.orderNormal),
                hidden = ordered(hidden, config.orderHidden),
                ignored = ordered(ignored, config.orderIgnored),
            )
        }

        /** Primero lo que no tiene sitio (lo más reciente antes), después el orden guardado. */
        fun ordered(entries: List<Entry>, order: List<String>): List<Entry> {
            val pos = order.withIndex().associate { it.value to it.index }
            val (known, unknown) = entries.partition { it.key in pos }
            return unknown.sortedByDescending { it.newest } + known.sortedBy { pos[it.key] }
        }

        fun folderName(path: String): String = when {
            path == "/storage/emulated/0" -> "Almacenamiento interno"
            else -> path.substringAfterLast('/').ifEmpty { path }
        }
    }
}
