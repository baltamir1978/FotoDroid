package es.altamirano.foto

import es.altamirano.foto.data.FileMover
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileMoverTest {
    private lateinit var tmp: File

    @Before fun setUp() { tmp = Files.createTempDirectory("mover").toFile() }
    @After fun tearDown() { tmp.deleteRecursively() }

    private fun file(rel: String, text: String = rel) = File(tmp, rel).apply { parentFile.mkdirs(); writeText(text) }

    @Test fun mueveYCreaLaCarpeta() {
        val f = file("DCIM/Camera/a.jpg", "hola")
        val dest = File(tmp, "Pictures/Viaje")
        val out = FileMover.move(f.path, dest)
        assertEquals(File(dest, "a.jpg").path, out)
        assertFalse(f.exists())
        assertEquals("hola", File(out!!).readText())
    }

    @Test fun noSobrescribe() {
        val f = file("DCIM/a.jpg", "nueva")
        file("Pictures/a.jpg", "vieja")
        file("Pictures/a (1).jpg", "vieja1")
        val out = FileMover.move(f.path, File(tmp, "Pictures"))
        assertEquals(File(tmp, "Pictures/a (2).jpg").path, out)
        assertEquals("vieja", File(tmp, "Pictures/a.jpg").readText())
        assertEquals("nueva", File(out!!).readText())
    }

    @Test fun aLaMismaCarpetaNoHaceNada() {
        val f = file("DCIM/a.jpg")
        assertNull(FileMover.move(f.path, File(tmp, "DCIM")))
        assertTrue(f.exists())
    }

    @Test fun siNoExisteDevuelveNull() {
        assertNull(FileMover.move(File(tmp, "no.jpg").path, File(tmp, "X")))
    }

    @Test fun nombresLibres() {
        val dir = File(tmp, "d").apply { mkdirs() }
        File(dir, "sin").writeText("")
        assertEquals("sin (1)", FileMover.freeName(dir, "sin").name)
        assertEquals("b.mp4", FileMover.freeName(dir, "b.mp4").name)
        File(dir, ".oculta").writeText("")
        assertEquals(".oculta (1)", FileMover.freeName(dir, ".oculta").name)
    }
}
