package es.altamirano.foto.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Group(val name: String, val folders: List<String>)

@Serializable
data class Settings(
    val lockApp: Boolean = false,
    val lockHidden: Boolean = false,
    val newestFirst: Boolean = true,
    val mediaColumns: Int = 4,
    val folderColumns: Int = 3,
    val loopVideos: Boolean = true,
    /** Recorrer el almacenamiento para encontrar lo que el sistema no indexa. */
    val scanFiles: Boolean = true,
    /** Incluir carpetas que empiezan por punto (casi siempre son de apps). */
    val dotFolders: Boolean = false,
    /** Lo borrado va a la papelera 30 días en vez de borrarse para siempre. */
    val useTrash: Boolean = true,
)

/** Todo lo que el usuario decide: categorías, órdenes, grupos y ajustes. */
@Serializable
data class Config(
    /** Clave (ruta o `g:<id>`) → categoría. Lo que no está aquí es NORMAL. */
    val categories: Map<String, Category> = emptyMap(),
    val orderNormal: List<String> = emptyList(),
    val orderHidden: List<String> = emptyList(),
    val orderIgnored: List<String> = emptyList(),
    /** id → grupo (la clave del grupo es `g:<id>`). */
    val groups: Map<String, Group> = emptyMap(),
    val settings: Settings = Settings(),
) {
    fun categoryOf(key: String) = categories[key] ?: Category.NORMAL

    fun order(c: Category) = when (c) {
        Category.NORMAL -> orderNormal
        Category.HIDDEN -> orderHidden
        Category.IGNORED -> orderIgnored
    }

    fun withOrder(c: Category, order: List<String>) = when (c) {
        Category.NORMAL -> copy(orderNormal = order)
        Category.HIDDEN -> copy(orderHidden = order)
        Category.IGNORED -> copy(orderIgnored = order)
    }

    /** Pasa las claves a otra categoría; allí salen al principio, con lo nuevo, hasta que se reordene. */
    fun move(keys: Collection<String>, to: Category): Config {
        val cats = categories.toMutableMap()
        keys.forEach { if (to == Category.NORMAL) cats.remove(it) else cats[it] = to }
        val ks = keys.toSet()
        var c = copy(categories = cats)
        for (cat in Category.entries) {
            val o = c.order(cat).filterNot { it in ks }
            c = c.withOrder(cat, o)
        }
        return c
    }

    /** El grupo al que se añadirían estas claves al juntarlas (el primer grupo de la selección), si hay. */
    fun mergeTarget(keys: List<String>): String? =
        keys.firstOrNull { it.startsWith(Entry.GROUP_PREFIX) && it.removePrefix(Entry.GROUP_PREFIX) in groups }

    /**
     * Junta claves (carpetas o grupos). Si hay un grupo entre ellas, lo demás se añade a ese grupo
     * (conserva su id, su categoría y su sitio); si no, se crea uno con la categoría y el sitio del primero.
     */
    fun merge(keys: List<String>, name: String, newId: String): Config {
        val folders = keys.flatMap { k ->
            if (k.startsWith(Entry.GROUP_PREFIX)) groups[k.removePrefix(Entry.GROUP_PREFIX)]?.folders.orEmpty() else listOf(k)
        }.distinct()
        val target = mergeTarget(keys)
        val id = target?.removePrefix(Entry.GROUP_PREFIX) ?: newId
        val newKey = Entry.GROUP_PREFIX + id
        val first = target ?: keys.first()
        val cat = categoryOf(first)
        val ks = keys.toSet()
        val cats = categories.filterKeys { it !in ks && it !in folders }.toMutableMap()
        if (cat != Category.NORMAL) cats[newKey] = cat
        var c = copy(
            groups = groups.filterKeys { Entry.GROUP_PREFIX + it !in ks } + (id to Group(name, folders)),
            categories = cats,
        )
        for (cg in Category.entries) {
            val o = c.order(cg)
            val pos = o.indexOf(first)
            val rest = o.filterNot { it in ks || it in folders }
            c = c.withOrder(cg, if (cg == cat) {
                val at = if (pos < 0) 0 else o.take(pos).count { it !in ks && it !in folders }
                rest.take(at) + newKey + rest.drop(at)
            } else rest)
        }
        return c
    }

    /** Deshace un grupo: sus carpetas vuelven sueltas, con su categoría, en su sitio. */
    fun split(groupKey: String): Config {
        val g = groups[groupKey.removePrefix(Entry.GROUP_PREFIX)] ?: return this
        val cat = categoryOf(groupKey)
        val cats = categories.toMutableMap().apply {
            remove(groupKey)
            if (cat != Category.NORMAL) g.folders.forEach { put(it, cat) }
        }
        val o = order(cat)
        val pos = o.indexOf(groupKey)
        val newOrder = if (pos < 0) g.folders + o else o.take(pos) + g.folders + o.drop(pos + 1)
        return copy(groups = groups - groupKey.removePrefix(Entry.GROUP_PREFIX), categories = cats).withOrder(cat, newOrder)
    }

    fun rename(groupKey: String, name: String): Config {
        val id = groupKey.removePrefix(Entry.GROUP_PREFIX)
        val g = groups[id] ?: return this
        return copy(groups = groups + (id to g.copy(name = name)))
    }

    /** Olvida claves (carpetas borradas o grupos eliminados). */
    fun forget(keys: Collection<String>): Config {
        val ks = keys.toSet()
        val ids = ks.filter { it.startsWith(Entry.GROUP_PREFIX) }.map { it.removePrefix(Entry.GROUP_PREFIX) }.toSet()
        return copy(
            categories = categories.filterKeys { it !in ks },
            orderNormal = orderNormal.filterNot { it in ks },
            orderHidden = orderHidden.filterNot { it in ks },
            orderIgnored = orderIgnored.filterNot { it in ks },
            groups = groups.filterKeys { it !in ids }.mapValues { (_, g) -> g.copy(folders = g.folders.filterNot { it in ks }) },
        )
    }
}

class ConfigStore(dir: File) {
    private val file = File(dir, "config.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun load(): Config = runCatching { json.decodeFromString<Config>(file.readText()) }.getOrDefault(Config())

    @Synchronized
    fun save(c: Config) {
        val tmp = File(file.parentFile, "config.json.tmp")
        tmp.writeText(json.encodeToString(Config.serializer(), c))
        tmp.renameTo(file)
    }
}
