package es.altamirano.foto.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
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

    // La carpeta se ha quedado vacía (o se ha borrado): volver.
    LaunchedEffect(library, entry) { if (library != null && entry == null) vm.pop() }
    LaunchedEffect(items) { val ids = items.map { it.id }.toSet(); selected.retainAll(ids) }
    // Al volver del visor, que se vea la foto en la que estaba.
    LaunchedEffect(vm.lastViewed) {
        val (k, i) = vm.lastViewed ?: return@LaunchedEffect
        if (k == key) {
            val visible = grid.layoutInfo.visibleItemsInfo.map { it.index }
            if (i !in visible) grid.scrollToItem(i)
            vm.lastViewed = null
        }
    }
    BackHandler(selected.isNotEmpty()) { selected.clear() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selected.isNotEmpty()) TopAppBar(
                navigationIcon = { IconButton({ selected.clear() }) { Icon(Icons.Default.Close, "Cancelar") } },
                title = { Text("${selected.size}") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                actions = {
                    IconButton({ selected.clear(); selected.addAll(items.map { it.id }) }) { Icon(Icons.Default.SelectAll, "Todo") }
                    IconButton({ share(ctx, items.filter { it.id in selected }) }) { Icon(Icons.Default.Share, "Compartir") }
                    IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, "Eliminar") }
                },
            ) else TopAppBar(
                navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry?.name.orEmpty(), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        Text("  ${items.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                GridCells.Fixed(config.settings.mediaColumns),
                state = grid,
                contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(items, key = { _, m -> m.id }, contentType = { _, _ -> 0 }) { i, m ->
                    val isSel = m.id in selected
                    Box(
                        Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .combinedClickable(
                                onClick = {
                                    if (selected.isNotEmpty()) { if (isSel) selected.remove(m.id) else selected.add(m.id) }
                                    else vm.push(Screen.Viewer(key, i))
                                },
                                onLongClick = { if (isSel) selected.remove(m.id) else selected.add(m.id) },
                            )
                    ) {
                        AsyncImage(Thumb(m, THUMB_PX), null, Modifier.fillMaxSize().then(if (isSel) Modifier.padding(10.dp) else Modifier),
                            contentScale = ContentScale.Crop)
                        if (m.isVideo) Row(
                            Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color(0x88000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(12.dp), tint = Color.White)
                            if (m.durationMs > 0) Text(formatDuration(m.durationMs), color = Color.White, fontSize = 10.sp)
                        }
                        if (m.mime == "image/gif") Text("GIF", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color(0x88000000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
                        if (isSel) Icon(Icons.Default.CheckCircle, null, Modifier.align(Alignment.TopStart).padding(4.dp)
                            .background(Color.White, CircleShape), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (items.size > 60) FastScroller(grid, items.size, Modifier.align(Alignment.TopEnd)
                .padding(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding()))
        }
    }

    if (confirmDelete) {
        val n = selected.size
        ConfirmDialog(
            "Eliminar $n ${if (n == 1) "archivo" else "archivos"}", "Se borrarán para siempre.", "Eliminar",
            onConfirm = {
                val s = items.filter { it.id in selected }; selected.clear()
                vm.deleteItems(s) { scope.launch { snackbar.showSnackbar("Borrados: $it") } }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Asa a la derecha para saltar por carpetas con miles de archivos. */
@Composable
private fun FastScroller(grid: LazyGridState, count: Int, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val fraction by remember(count) {
        derivedStateOf {
            val info = grid.layoutInfo
            val visible = info.visibleItemsInfo.size.coerceAtLeast(1)
            (grid.firstVisibleItemIndex.toFloat() / (count - visible).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
    }
    // Aparece al desplazarse y se queda un momento para poder agarrarla.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(grid.isScrollInProgress, dragging) {
        if (grid.isScrollInProgress || dragging) shown = true else { kotlinx.coroutines.delay(1500); shown = false }
    }
    val active = shown || dragging
    BoxWithConstraints(modifier.fillMaxHeight().width(36.dp)) {
        val h = constraints.maxHeight.toFloat()
        val thumbH = with(LocalDensity.current) { 48.dp.toPx() }
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
            if (active) Box(
                Modifier.offset { IntOffset(0, (fraction * (h - thumbH)).roundToInt()) }
                    .align(Alignment.TopEnd).padding(end = 4.dp).size(6.dp, 48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (dragging) 1f else 0.6f), RoundedCornerShape(3.dp))
            )
        }
    }
}
