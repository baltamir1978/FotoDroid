package es.altamirano.foto

import es.altamirano.foto.data.FileScanner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileScannerTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file(root: File, rel: String, bytes: Int = 50_000) =
        File(root, rel).apply { parentFile!!.mkdirs(); writeBytes(ByteArray(bytes)) }

    private fun scan(root: File, dots: Boolean = false, known: Set<String> = emptySet()) =
        FileScanner.scanRoots(listOf(root), known, emptySet(), dots).map { it.path.removePrefix(root.path + "/") }.toSortedSet()

    @Test fun saltaLoQueNoEsDelUsuario() {
        val r = tmp.newFolder("sd")
        file(r, "DCIM/Camera/IMG_1.jpg")
        file(r, "DCIM/Camera/.trashed-1765000000-IMG_2.jpg")
        file(r, "DCIM/Camera/.pending-1765000000-IMG_3.jpg")
        file(r, "Pictures/Secreto/foto.jpg").also { File(it.parentFile, ".nomedia").createNewFile() }
        file(r, "Pictures/icono.png", 800)
        file(r, "Download/clip.mp4", 800)
        file(r, ".gs_fs0/a.jpg")
        file(r, "App/cache/a.jpg")
        file(r, "App/Thumbnails/a.jpg")
        file(r, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent/a.jpg")
        file(r, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers/s.webp")
        file(r, "Android/media/com.google.android.gm/a.jpg")
        file(r, "Android/media/com.android.systemui/a.png")
        file(r, "Android/data/com.x/a.jpg")

        assertEquals(
            sortedSetOf(
                "DCIM/Camera/IMG_1.jpg", "Pictures/Secreto/foto.jpg", "Download/clip.mp4",
                "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent/a.jpg",
            ),
            scan(r),
        )
        assertEquals(true, ".gs_fs0/a.jpg" in scan(r, dots = true))
        assertEquals(false, "DCIM/Camera/IMG_1.jpg" in scan(r, known = setOf(File(r, "DCIM/Camera/IMG_1.jpg").path)))
    }
}
