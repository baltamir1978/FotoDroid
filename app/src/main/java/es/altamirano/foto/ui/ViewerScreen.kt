package es.altamirano.foto.ui

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import es.altamirano.foto.data.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animate
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.launch
import kotlin.math.abs
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

private val ZOOM = ZoomSpec(maxZoomFactor = 8f)
/** Doble toque: a 2,5× y vuelta (pellizcando se llega hasta 8×). */
private val DOUBLE_TAP = DoubleClickToZoomListener.cycle(maxZoomFactor = 2.5f)
private val GLASS = Color(0x59000000)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ViewerScreen(vm: AppViewModel, key: String, startIndex: Int) {
    val library by vm.library.collectAsState()
    val config by vm.config.collectAsState()
    val items = library?.find(key)?.items.orEmpty()
    val ctx = LocalContext.current
    if (library != null && items.isEmpty()) { LaunchedEffect(Unit) { vm.pop() }; return }
    if (items.isEmpty()) return

    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) { items.size }
    val current = items.getOrNull(pager.currentPage.coerceAtMost(items.lastIndex))
    var overlay by remember { mutableStateOf(true) }
    var info by remember { mutableStateOf<MediaItem?>(null) }
    var confirmDelete by remember { mutableStateOf<MediaItem?>(null) }

    // ---- reproductor (uno solo, se engancha a la página que toca)
    val player = remember { buildPlayer(ctx) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var playing by remember { mutableStateOf(false) }
    var firstFrame by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onVideoSizeChanged(v: VideoSize) { videoSize = v }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onPlayerError(e: PlaybackException) { error = e.errorCodeName }
            override fun onPlaybackStateChanged(state: Int) { if (player.duration != C.TIME_UNSET) duration = player.duration }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(config.settings.loopVideos) { player.repeatMode = if (config.settings.loopVideos) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }

    // Cambiar de página: cargar el vídeo o parar.
    LaunchedEffect(pager, items) {
        snapshotFlow { items.getOrNull(pager.settledPage) }.distinctUntilChanged().collect { m ->
            firstFrame = false; error = null; videoSize = VideoSize.UNKNOWN; position = 0; duration = m?.durationMs ?: 0
            if (m != null && m.isVideo) {
                player.setMediaItem(androidx.media3.common.MediaItem.fromUri(m.uri))
                player.prepare()
                player.playWhenReady = true
            } else {
                player.stop(); player.clearMediaItems()
            }
        }
    }
    // Posición para la barra
    LaunchedEffect(player) {
        while (true) {
            if (!scrubbing) position = player.currentPosition
            delay(if (playing) 100 else 300)
        }
    }
    // Pausa al salir de la app
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) player.pause() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    // Los controles se esconden solos mientras se reproduce
    LaunchedEffect(overlay, playing, scrubbing) { if (overlay && playing && !scrubbing) { delay(2500); overlay = false } }
    // Pantalla completa de verdad cuando no hay controles
    val view = LocalView.current
    DisposableEffect(overlay) {
        val window = (view.context as Activity).window
        val c = WindowCompat.getInsetsController(window, view)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.isAppearanceLightStatusBars = false
        c.isAppearanceLightNavigationBars = false
        if (overlay) c.show(WindowInsetsCompat.Type.systemBars()) else c.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as Activity).window
            val c = WindowCompat.getInsetsController(window, view)
            c.show(WindowInsetsCompat.Type.systemBars())
            val light = (view.context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) !=
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            c.isAppearanceLightStatusBars = light
            c.isAppearanceLightNavigationBars = light
            vm.lastViewed = key to pager.currentPage
        }
    }

    val toggle: (Offset) -> Unit = { overlay = !overlay }
    // Deslizar hacia abajo para cerrar (sólo sin zoom: con zoom el gesto es de la imagen)
    var dragY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val closeAt = with(density) { 120.dp.toPx() }
    val progress = (dragY / (closeAt * 2.5f)).coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 1f - progress))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var total = Offset.Zero
                    var dismissing = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.count { it.pressed } > 1 && !dismissing) break
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        if (!dismissing) {
                            if (ch.isConsumed) break
                            total += ch.positionChange()
                            if (total.y > viewConfiguration.touchSlop && total.y > 2 * abs(total.x)) { dismissing = true; overlay = false }
                            else if (abs(total.x) > viewConfiguration.touchSlop || total.y < -viewConfiguration.touchSlop) break
                        } else {
                            dragY = (dragY + ch.positionChange().y).coerceAtLeast(0f)
                            ch.consume()
                        }
                    }
                    if (dismissing) {
                        if (dragY > closeAt) vm.pop()
                        else scope.launch { animate(dragY, 0f) { v, _ -> dragY = v } }
                    }
                }
            }
    ) {
        HorizontalPager(
            state = pager,
            key = { items.getOrNull(it)?.id ?: it },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationY = dragY
                val s = 1f - progress * 0.25f
                scaleX = s; scaleY = s
            },
        ) { page ->
            val m = items.getOrNull(page) ?: return@HorizontalPager
            if (m.isVideo) VideoPage(m, isCurrent = page == pager.settledPage, player, videoSize, firstFrame && page == pager.settledPage, toggle)
            else ImagePage(m, vm.mediaThumbPx, toggle)
        }

        val isVideo = current?.isVideo == true
        val bars = WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout)

        // Barra superior
        AnimatedVisibility(overlay, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent)))
                    .windowInsetsPadding(bars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton({ vm.pop() }, Modifier.background(GLASS, CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = Color.White)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(current?.name.orEmpty(), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall)
                    Text("${pager.currentPage + 1} de ${items.size}", color = Color(0xCCFFFFFF), style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.background(GLASS, CircleShape)) {
                    IconButton({ info = current }) { Icon(Icons.Rounded.Info, "Información", tint = Color.White) }
                    IconButton({ current?.let { share(ctx, listOf(it)) } }) { Icon(Icons.Rounded.Share, "Compartir", tint = Color.White) }
                    IconButton({ confirmDelete = current }) { Icon(Icons.Rounded.Delete, "Eliminar", tint = Color.White) }
                }
            }
        }

        if (isVideo) {
            // Play/pausa en el centro, sólo con los controles a la vista
            AnimatedVisibility(overlay && error == null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                IconButton(
                    { if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() } },
                    Modifier.size(72.dp).background(GLASS, CircleShape),
                ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            }
            error?.let { e ->
                Column(Modifier.align(Alignment.Center).background(GLASS, RoundedCornerShape(28.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No se puede reproducir\n$e", color = Color.White, fontSize = 13.sp)
                    TextButton({ current?.let { openWith(ctx, it) } }) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Color.White); Text("  Abrir con…", color = Color.White)
                    }
                }
            }
            // La barra mínima de posición: siempre presente, más gruesa al tocarla.
            SeekBar(
                position = position, duration = duration, emphasized = overlay || scrubbing,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                    .padding(bottom = 8.dp),
                onStart = { scrubbing = true; overlay = true; player.setSeekParameters(SeekParameters.CLOSEST_SYNC) },
                onScrub = { position = it; player.seekTo(it) },
                onEnd = {
                    player.setSeekParameters(SeekParameters.EXACT); player.seekTo(position)
                    player.setSeekParameters(SeekParameters.DEFAULT); scrubbing = false
                },
            )
        }
    }

    info?.let { InfoDialog(it) { info = null } }
    confirmDelete?.let { m ->
        ConfirmDialog("Eliminar archivo", "${m.name}\nSe borrará para siempre.", "Eliminar",
            onConfirm = { vm.deleteItems(listOf(m)) }, onDismiss = { confirmDelete = null })
    }
}

@Composable
private fun ImagePage(m: MediaItem, placeholderPx: Int, onTap: (Offset) -> Unit) {
    val ctx = LocalContext.current
    val state = rememberZoomableImageState(rememberZoomableState(zoomSpec = ZOOM))
    val request = remember(m.id, m.modified) {
        ImageRequest.Builder(ctx).data(if (m.indexed) m.uri else java.io.File(m.path))
            .placeholderMemoryCacheKey(MemoryCache.Key("t:${m.id}:${m.modified}:$placeholderPx"))
            .build()
    }
    ZoomableAsyncImage(
        model = request, contentDescription = m.name, state = state,
        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, onClick = onTap,
        onDoubleClick = DOUBLE_TAP,
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPage(m: MediaItem, isCurrent: Boolean, player: ExoPlayer, size: VideoSize, showVideo: Boolean, onTap: (Offset) -> Unit) {
    val zoom = rememberZoomableState(zoomSpec = ZOOM)
    val w = if (size.width > 0) size.width * size.pixelWidthHeightRatio else m.width.toFloat()
    val h = if (size.height > 0) size.height.toFloat() else m.height.toFloat()
    val ratio = if (w > 0 && h > 0) w / h else 16f / 9f
    LaunchedEffect(ratio) { zoom.setContentLocation(ZoomableContentLocation.scaledInsideAndCenterAligned(Size(ratio * 1000f, 1000f))) }
    Box(Modifier.fillMaxSize().zoomable(zoom, onClick = onTap, onDoubleClick = DOUBLE_TAP), contentAlignment = Alignment.Center) {
        Box(Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = ratio < 1f)) {
            if (isCurrent) AndroidView(
                factory = { c -> TextureView(c).also { player.setVideoTextureView(it) } },
                modifier = Modifier.fillMaxSize(),
                onRelease = { player.clearVideoTextureView(it) },
            )
            // La miniatura tapa el negro hasta el primer fotograma.
            if (!showVideo) AsyncImage(Thumb(m, 512), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun SeekBar(
    position: Long, duration: Long, emphasized: Boolean, modifier: Modifier,
    onStart: () -> Unit, onScrub: (Long) -> Unit, onEnd: () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val frac = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column(modifier.padding(horizontal = 16.dp)) {
        if (emphasized) Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(formatDuration(position), color = Color.White, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(GLASS, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp))
            Box(Modifier.weight(1f))
            Text(formatDuration(duration), color = Color.White, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(GLASS, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        // Zona táctil alta (40 dp) para una barra fina: tocar salta, arrastrar recorre el vídeo.
        Canvas(
            Modifier.fillMaxWidth().height(40.dp).pointerInput(duration) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (duration <= 0) return@awaitEachGesture
                    down.consume()
                    widthPx = size.width.toFloat()
                    dragging = true; onStart()
                    fun at(x: Float) = ((x / size.width).coerceIn(0f, 1f) * duration).toLong()
                    onScrub(at(down.position.x))
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        ch.consume()
                        onScrub(at(ch.position.x))
                    }
                    dragging = false; onEnd()
                }
            }
        ) {
            // Estilo de los deslizadores actuales de Android: pista gruesa redondeada, asa vertical y un hueco a cada lado.
            val track = (if (dragging) 8.dp else if (emphasized) 6.dp else 3.dp).toPx()
            val y = size.height / 2
            val r = CornerRadius(track / 2)
            val x = size.width * frac
            if (!emphasized) {
                drawRoundRect(Color(0x55FFFFFF), Offset(0f, y - track / 2), Size(size.width, track), r)
                drawRoundRect(Color.White, Offset(0f, y - track / 2), Size(x, track), r)
            } else {
                val gapPx = 5.dp.toPx(); val thumbW = 4.dp.toPx(); val thumbH = (if (dragging) 28.dp else 22.dp).toPx()
                if (x - gapPx > 0) drawRoundRect(Color.White, Offset(0f, y - track / 2), Size(x - gapPx, track), r)
                if (x + gapPx < size.width) drawRoundRect(Color(0x55FFFFFF), Offset(x + gapPx, y - track / 2), Size(size.width - x - gapPx, track), r)
                drawRoundRect(Color.White, Offset(x - thumbW / 2, y - thumbH / 2), Size(thumbW, thumbH), CornerRadius(thumbW / 2))
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun buildPlayer(ctx: Context): ExoPlayer {
    // FFmpeg entra como último recurso para el audio que el móvil no sabe (AC3, DTS, TrueHD…).
    val renderers = DefaultRenderersFactory(ctx)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        .setEnableDecoderFallback(true)
    val extractors = DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setConstantBitrateSeekingAlwaysEnabled(true)
    return ExoPlayer.Builder(ctx, renderers, DefaultMediaSourceFactory(ctx, extractors))
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()
}
