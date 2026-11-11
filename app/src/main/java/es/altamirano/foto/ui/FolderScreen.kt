package es.altamirano.foto.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import es.altamirano.foto.data.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderScreen(vm: AppViewModel, key: String) {
    val library by vm.library.collectAsState()
    val config by vm.config.collectAsState()
    val entry = library?.find(key)
    val items = entry?.items.orEmpty()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val selected = remember(key) { mutableStateListOf<Long>() }
    var confirmDelete by remember { mutableStateOf(false) }
    val grid = vm.gridState("folder:$key")
    val columns = config.settings.mediaColumns
    val px = thumbPxFor(columns)
    SideEffect { vm.mediaThumbPx = px }

    LaunchedEffect(library, entry) { if (library != null && entry == null) vm.pop() }
    LaunchedEffect(items) { val ids = items.map { it.id }.toSet(); selected.retainAll(ids) }
    LaunchedEffect(vm.lastViewed) {
        val (k, i) = vm.lastViewed ?: return@LaunchedEffect
        if (k == key) {
            val visible = grid.layoutInfo.visibleItemsInfo.map { it.index }
            if (i !in visible) grid.scrollToItem(i)
            vm.lastViewed = null
        }
    }
    BackHandler(selected.isNotEmpty()) { selected.clear() }

    val scroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selected.isNotEmpty()) TopAppBar(
                navigationIcon = { IconButton({ selected.clear() }) { Icon(Icons.Default.Close, "Cancelar") } },
                title = { Text("${selected.size} seleccionados") },
                actions = { IconButton({ selected.clear(); selected.addAll(items.map { it.id }) }) { Icon(Icons.Outlined.CheckCircle, "Todos") } },
            ) else TopAppBar(
                scrollBehavior = scroll,
                navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
                title = {
                    Column {
                        Text(entry?.name.orEmpty(), maxLines = 1, style = MaterialTheme.typography.titleLarge)
                        Text("${items.size} elementos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { pad ->
        val gap = if (columns >= 6) 1.dp else 2.dp
        val corner = when { columns <= 3 -> 12.dp; columns <= 5 -> 6.dp; else -> 2.dp }
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                GridCells.Fixed(columns),
                state = grid,
                contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = pad.calculateTopPadding(),
                    bottom = pad.calculateBottomPadding() + if (selected.isNotEmpty()) 104.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxSize().pinchColumns(columns, 2..7) { n -> vm.setSettings { it.copy(mediaColumns = n) } },
            ) {
                itemsIndexed(items, key = { _, m -> m.id }, contentType = { _, _ -> 0 }) { i, m ->
                    MediaTile(
                        m, px, corner, selected = m.id in selected, selecting = selected.isNotEmpty(),
                        modifier = Modifier.animateItem().combinedClickable(
                            onClick = {
                                if (selected.isNotEmpty()) { if (m.id in selected) selected.remove(m.id) else selected.add(m.id) }
                                else vm.push(Screen.Viewer(key, i))
                            },
                            onLongClick = { if (m.id in selected) selected.remove(m.id) else selected.add(m.id) },
                        ),
                    )
                }
            }
            if (items.size > 60) FastScroller(grid, items, Modifier.align(Alignment.TopEnd)
                .padding(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 8.dp))

            AnimatedVisibility(
                selected.isNotEmpty(), enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 16.dp),
            ) {
                FloatingBar(listOf(
                    BarAction(Icons.Outlined.Share, "Compartir") { share(ctx, items.filter { it.id in selected }) },
                    BarAction(Icons.Outlined.Delete, "Eliminar", Color(0xFFE5484D)) { confirmDelete = true },
                ))
            }
        }
    }

    if (confirmDelete) {
        val n = selected.size
        ConfirmDialog(
            "¿Eliminar $n ${if (n == 1) "archivo" else "archivos"}?", "Se borrarán para siempre.", "Eliminar",
            onConfirm = {
                val s = items.filter { it.id in selected }; selected.clear()
                vm.deleteItems(s) { scope.launch { snackbar.showSnackbar("Borrados: $it") } }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun MediaTile(m: MediaItem, px: Int, corner: androidx.compose.ui.unit.Dp, selected: Boolean, selecting: Boolean, modifier: Modifier) {
    val scale by animateFloatAsState(if (selected) 0.86f else 1f, label = "s")
    val r by animateDpAsState(if (selected) 18.dp else corner, label = "r")
    Box(modifier.aspectRatio(1f)) {
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(r))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            AsyncImage(Thumb(m, px), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (m.isVideo) Row(
                Modifier.align(Alignment.BottomEnd).padding(5.dp).background(Color(0x80000000), CircleShape)
                    .padding(start = 3.dp, end = 6.dp, top = 1.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(13.dp), tint = Color.White)
                if (m.durationMs > 0) Text(formatDuration(m.durationMs), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            if (m.mime == "image/gif") Text("GIF", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).background(Color(0x80000000), CircleShape).padding(horizontal = 6.dp))
        }
        if (selecting) Icon(
            if (selected) Icons.Default.CheckCircle else Icons.Outlined.Circle, null,
            Modifier.align(Alignment.TopStart).padding(6.dp).size(22.dp)
                .background(if (selected) Color.White else Color(0x33000000), CircleShape),
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        )
    }
}

/** Asa a la derecha para saltar por carpetas con miles de archivos, con la fecha al arrastrar. */
@Composable
private fun FastScroller(grid: LazyGridState, items: List<MediaItem>, modifier: Modifier) {
    val count = items.size
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val fraction by remember(count) {
        derivedStateOf {
            val visible = grid.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            (grid.firstVisibleItemIndex.toFloat() / (count - visible).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(grid.isScrollInProgress, dragging) {
        if (grid.isScrollInProgress || dragging) shown = true else { delay(1500); shown = false }
    }
    val active = shown || dragging
    val alpha by animateFloatAsState(if (active) 1f else 0f, label = "a")
    val fmt = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    BoxWithConstraints(modifier.fillMaxHeight().width(if (dragging) 200.dp else 32.dp)) {
        val h = constraints.maxHeight.toFloat()
        val thumbH = with(LocalDensity.current) { 52.dp.toPx() }
        fun jump(y: Float) {
            val f = ((y - thumbH / 2) / (h - thumbH)).coerceIn(0f, 1f)
            scope.launch { grid.scrollToItem((f * (count - 1)).roundToInt()) }
        }
        Box(
            Modifier.fillMaxSize().then(if (!active) Modifier else Modifier.pointerInput(count) {
                detectVerticalDragGestures(
                    onDragStart = { dragging = true; jump(it.y) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ -> change.consume(); jump(change.position.y) }
            })
        ) {
            Row(
                Modifier.align(Alignment.TopEnd).offset { IntOffset(0, (fraction * (h - thumbH)).roundToInt()) }
                    .graphicsLayer { this.alpha = alpha },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dragging) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp) {
                    val d = items.getOrNull(grid.firstVisibleItemIndex)?.date
                    Text(d?.let { fmt.format(Date(it)) }.orEmpty(), style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Box(Modifier.padding(start = 8.dp, end = 4.dp).size(if (dragging) 10.dp else 6.dp, 52.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}
