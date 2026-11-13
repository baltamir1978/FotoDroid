package es.altamirano.foto

import es.altamirano.foto.data.FileScanner
import es.altamirano.foto.data.Trash
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** La papelera mueve archivos de verdad: aquí, sobre una carpeta temporal que hace de almacenamiento. */
class TrashTest {
    private lateinit var tmp: File
    private lateinit var root: File
    private lateinit var trash: Trash
    private val day = 24L * 3600 * 1000

    @Before fun setUp() {
        tmp = Files.createTempDirectory("papelera").toFile()
        root = File(tmp, "sdcard").apply { mkdirs() }
        trash = Trash(File(tmp, "papelera.json")) { listOf(root) }
    }

    @After fun tearDown() { tmp.deleteRecursively() }

    private fun photo(path: String, content: String = "x".repeat(20_000)) =
        File(root, path).apply { parentFile!!.mkdirs(); writeText(content) }

    @Test fun trashedFileLeavesItsFolderAndCanComeBack() {
        val f = photo("DCIM/Camera/a.jpg", "foto")
        val item = trash.trash(f.path, isVideo = false, now = 1000)!!
        assertFalse(f.exists())
        assertTrue(File(item.file).isFile)
        assertEquals(Trash.DIR, File(item.file).parentFile!!.name)
        assertTrue(File(root, Trash.DIR + "/.nomedia").exists())
        assertEquals(listOf(item), trash.list())

        assertEquals(f.path, trash.restore(item))
        assertEquals("foto", f.readText())
        assertTrue(trash.list().isEmpty())
    }

    @Test fun whatCannotBeMovedStaysWhereItIs() {
        // Fuera de todo volumen conocido: no hay papelera a la que llevarlo, así que no se toca.
        val outside = File(tmp, "otro/b.jpg").apply { parentFile!!.mkdirs(); writeText("x") }
        assertNull(trash.trash(outside.path, false))
        assertTrue(outside.exists())
        assertNull(trash.trash(File(root, "no-existe.jpg").path, false))
        assertTrue(trash.list().isEmpty())
    }

    @Test fun twoFilesWithTheSameNameDoNotCollide() {
        val a = trash.trash(photo("A/foto.jpg", "a").path, false, now = 5)!!
        val b = trash.trash(photo("B/foto.jpg", "b").path, false, now = 5)!!
        assertTrue(a.file != b.file)
        trash.restore(a); trash.restore(b)
        assertEquals("a", File(root, "A/foto.jpg").readText())
        assertEquals("b", File(root, "B/foto.jpg").readText())
    }

    @Test fun restoreNeverOverwritesAndRebuildsTheFolder() {
        val f = photo("Viaje/c.jpg", "vieja")
        val item = trash.trash(f.path, false)!!
        // La carpeta se borró al quedar vacía y luego apareció otra foto con el mismo nombre.
        f.parentFile!!.deleteRecursively()
        photo("Viaje/c.jpg", "nueva")
        val back = trash.restore(item)
        assertEquals(File(root, "Viaje/c (1).jpg").path, back)
        assertEquals("nueva", f.readText())
        assertEquals("vieja", File(back!!).readText())
    }

    @Test fun onlyWhatIsOlderThanThirtyDaysExpires() {
        val now = 100 * day
        val old = trash.trash(photo("A/vieja.jpg").path, false, now = now - 31 * day)!!
        val recent = trash.trash(photo("A/reciente.jpg").path, false, now = now - 29 * day)!!
        assertEquals(1, trash.purgeExpired(now))
        assertFalse(File(old.file).exists())
        assertEquals(listOf(recent), trash.list())
        assertEquals(1, recent.daysLeft(now))
    }

    @Test fun purgeOnlyDeletesInsideTheTrash() {
        val safe = photo("DCIM/d.jpg")
        val item = trash.trash(photo("DCIM/e.jpg").path, false)!!
        // Un índice estropeado que apuntara a una foto de verdad no debe poder borrarla.
        assertEquals(0, trash.purge(listOf(item.copy(file = safe.path))))
        assertTrue(safe.exists())
        assertEquals(1, trash.purge(listOf(item)))
        assertTrue(trash.list().isEmpty())
    }

    @Test fun filesWithoutIndexAreListedRecoverableAndNeverExpire() {
        val item = trash.trash(photo("DCIM/f.mp4").path, true, now = 1)!!
        File(tmp, "papelera.json").delete()          // se pierden los datos de la app
        val orphan = trash.list().single()
        assertEquals(item.file, orphan.file)
        assertTrue(orphan.isVideo)
        assertNull(orphan.daysLeft(1000 * day))
        assertEquals(0, trash.purgeExpired(1000 * day))
        val back = trash.restore(orphan)
        assertNotNull(back)
        assertEquals(File(root, Trash.RECOVERED + "/f.mp4").path, back)
    }

    @Test fun theScannerDoesNotShowTheTrash() {
        trash.trash(photo("DCIM/g.jpg").path, false)
        photo("DCIM/h.jpg")
        val found = FileScanner.scanRoots(listOf(root), known = emptySet(), skip = emptySet(), dotFolders = true)
        assertEquals(listOf("h.jpg"), found.map { it.name })
    }
}
