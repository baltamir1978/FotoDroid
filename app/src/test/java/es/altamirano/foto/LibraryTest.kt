package es.altamirano.foto

import es.altamirano.foto.data.Category
import es.altamirano.foto.data.Config
import es.altamirano.foto.data.Entry
import es.altamirano.foto.data.Library
import es.altamirano.foto.data.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private val root = "/storage/emulated/0"
    private var nextId = 1L
    private fun item(folder: String, date: Long) = MediaItem(
        nextId++, "$folder/f$nextId.jpg", "f$nextId.jpg", folder, false, "image/jpeg", 1, date, date, 0, 0, 0,
    )

    private val cam = "$root/DCIM/Camera"
    private val wa = "$root/WhatsApp/Media/Images"
    private val waSent = "$root/WhatsApp/Media/Images/Sent"
    private val shots = "$root/Pictures/Screenshots"
    private val dl = "$root/Download"
    private val items = listOf(item(cam, 100), item(cam, 300), item(wa, 200), item(waSent, 250), item(shots, 50), item(dl, 10))

    @Test fun unknownFoldersGoFirstByRecency() {
        val lib = Library.build(items, Config(orderNormal = listOf(dl, shots)))
        assertEquals(listOf(cam, waSent, wa, dl, shots), lib.normal.map { it.key })
        assertEquals(listOf(300L, 100L), lib.normal[0].items.map { it.date })
    }

    @Test fun ignoredIncludesSubfolders() {
        val c = Config().move(listOf(wa), Category.IGNORED)
        val lib = Library.build(items, c)
        assertTrue(lib.normal.none { it.key.startsWith(wa) })
        assertEquals(listOf(wa), lib.ignored.map { it.key })
        assertEquals(2, lib.ignored[0].items.size)
    }

    @Test fun hiddenHaveTheirOwnOrder() {
        var c = Config().move(listOf(shots, dl), Category.HIDDEN)
        c = c.withOrder(Category.HIDDEN, listOf(dl, shots))
        val lib = Library.build(items, c)
        assertEquals(listOf(dl, shots), lib.hidden.map { it.key })
        assertTrue(lib.normal.none { it.key == dl || it.key == shots })
        // Volver a mostrar: sale delante de las que ya tienen sitio.
        val back = Library.build(items, c.withOrder(Category.NORMAL, listOf(cam, wa, waSent)).move(listOf(dl), Category.NORMAL))
        assertEquals(listOf(dl, cam, wa, waSent), back.normal.map { it.key })
        assertEquals(listOf(shots), back.hidden.map { it.key })
    }

    @Test fun mergeMakesOneContinuousListInPlace() {
        val c0 = Config(orderNormal = listOf(dl, cam, shots, wa, waSent))
        val c = c0.merge(listOf(cam, shots), "Todo", "x")
        val lib = Library.build(items, c)
        assertEquals(listOf(dl, "g:x", wa, waSent), lib.normal.map { it.key })
        val g = lib.normal[1]
        assertEquals("Todo", g.name)
        assertEquals(listOf(300L, 100L, 50L), g.items.map { it.date })
        assertEquals(listOf(cam, shots), g.folders)
    }

    @Test fun mergeIntoGroupThenSplit() {
        val c1 = Config(orderNormal = listOf(dl, cam, shots)).merge(listOf(cam, shots), "A", "a")
        val c2 = c1.merge(listOf("g:a", dl), "B", "b")
        assertEquals(setOf("b"), c2.groups.keys)
        assertEquals(listOf(cam, shots, dl), c2.groups["b"]!!.folders)
        val c3 = c2.split("g:b")
        assertTrue(c3.groups.isEmpty())
        val lib = Library.build(items, c3.withOrder(Category.NORMAL, c3.orderNormal + listOf(wa, waSent)))
        assertEquals(listOf(cam, shots, dl, wa, waSent), lib.normal.map { it.key })
    }

    @Test fun hiddenGroupKeepsCategoryAfterSplit() {
        val c = Config().merge(listOf(cam, shots), "G", "g").move(listOf("g:g"), Category.HIDDEN)
        assertEquals(listOf("g:g"), Library.build(items, c).hidden.map { it.key })
        val s = c.split("g:g")
        assertEquals(setOf(cam, shots), Library.build(items, s).hidden.map { it.key }.toSet())
    }

    @Test fun forgetRemovesEverywhere() {
        val c = Config().merge(listOf(cam, shots), "G", "g").move(listOf(dl), Category.HIDDEN).forget(listOf("g:g", dl))
        assertTrue(c.groups.isEmpty() && c.categories.isEmpty() && c.orderHidden.isEmpty())
        assertEquals(Entry.GROUP_PREFIX, "g:")
    }
}
