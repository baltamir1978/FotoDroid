package es.altamirano.foto.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import es.altamirano.foto.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Miniatura de un archivo: la pide al sistema (que las guarda), y si no sabe, la saca él. */
data class Thumb(val item: MediaItem, val px: Int = 320)

private class ThumbFetcher(private val t: Thumb, private val context: Context) : Fetcher {
    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val m = t.item
        val size = Size(t.px, t.px)
        val bmp: Bitmap = (if (m.indexed) runCatching { context.contentResolver.loadThumbnail(m.uri, size, null) }.getOrNull() else null)
            ?: if (m.isVideo) videoFrame(m.path, t.px) else imageDecode(m, t.px)
        DrawableResult(BitmapDrawable(context.resources, bmp), isSampled = true, dataSource = DataSource.DISK)
    }

    private fun videoFrame(path: String, px: Int): Bitmap = MediaMetadataRetriever().use { r ->
        r.setDataSource(path)
        r.getScaledFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, px, px)
            ?: r.frameAtTime ?: error("sin fotograma")
    }

    private fun imageDecode(m: MediaItem, px: Int): Bitmap =
        ImageDecoder.decodeBitmap(if (m.indexed) ImageDecoder.createSource(context.contentResolver, m.uri) else ImageDecoder.createSource(java.io.File(m.path))) { d, info, _ ->
            val s = maxOf(1, minOf(info.size.width, info.size.height) / px)
            d.setTargetSize(maxOf(1, info.size.width / s), maxOf(1, info.size.height / s))
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

    class Factory : Fetcher.Factory<Thumb> {
        override fun create(data: Thumb, options: Options, imageLoader: ImageLoader) = ThumbFetcher(data, options.context)
    }
}

private class ThumbKeyer : Keyer<Thumb> {
    override fun key(data: Thumb, options: Options) = "t:${data.item.id}:${data.item.modified}:${data.px}"
}

fun buildImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
    .components {
        add(ThumbFetcher.Factory())
        add(ThumbKeyer())
        add(ImageDecoderDecoder.Factory())
        add(VideoFrameDecoder.Factory())
    }
    .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.30).build() }
    .crossfade(false)
    .respectCacheHeaders(false)
    .build()
