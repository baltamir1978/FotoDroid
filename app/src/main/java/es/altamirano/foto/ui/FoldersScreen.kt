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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import es.altamirano.foto.data.Category
import es.altamirano.foto.data.Entry
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

private val DANGER = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(vm: AppViewModel, category: Category) {
    val library by vm.library.collectAsState()
    val config by vm.config.collectAsState()
    val entries = library?.list(category).orEmpty()
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val selected = remember(category) { mutableStateListOf<String>() }
    var reordering by rememberSaveable(category) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var askMerge by remember { mutableStateOf(false) }
    var askRename by remember { mutableStateOf<Entry?>(null) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(entries) { selected.retainAll(entries.map { it.key }.toSet()) }
    BackHandler(selected.isNotEmpty()) { selected.clear() }

    val sel = entries.filter { it.key in selected }
    fun toast(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    fun moveSel(to: Category, msg: String) { val n = selected.size; vm.move(selected.toList(), to); selected.clear(); toast("$msg: $n") }

    val working = remember(category, reordering) { entries.toMutableStateList() }
    fun saveOrder() = vm.reorder(category, working.map { it.key })
    BackHandler(reordering) { saveOrder(); reordering = false }

    val title = when (category) { Category.NORMAL -> "Carpetas"; Category.HIDDEN -> "Ocultas"; Category.IGNORED -> "Ignoradas" }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val columns = config.settings.folderColumns
    val px = thumbPxFor(columns)

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            when {
                selected.isNotEmpty() -> TopAppBar(
                    navigationIcon = { IconButton({ selected.clear() }) { Icon(Icons.Default.Close, "Cancelar") } },
                    title = { Text("${selected.size} seleccionadas") },
                    actions = {
                        IconButton({ selected.clear(); selected.addAll(entries.map { it.key }) }) {
                            Icon(Icons.Outlined.CheckCircle, "Todas")
                        }
                    },
                )
                reordering -> TopAppBar(
                    navigationIcon = { IconButton({ saveOrder(); reordering = false }) { Icon(Icons.Default.Done, "Hecho") } },
                    title = { Column { Text("Ordenar"); Text("Mantén pulsada y arrastra", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    actions = {
                        IconButton({ menu = true }) { Icon(Icons.Outlined.MoreVert, "Más") }
                        DropdownMenu(menu, { menu = false }, shape = MaterialTheme.shapes.medium) {
                            DropdownMenuItem({ Text("Por nombre") }, {
                                menu = false; val s = working.sortedBy { it.name.lowercase() }; working.clear(); working.addAll(s)
                            })
                            DropdownMenuItem({ Text("Por fecha, reciente primero") }, {
                                menu = false; val s = working.sortedByDescending { it.newest }; working.clear(); working.addAll(s)
                            })
                        }
                    },
                )
                else -> LargeTopAppBar(
                    scrollBehavior = scroll,
                    navigationIcon = {
                        if (category != Category.NORMAL) IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                    },
                    title = { Text(title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    actions = {
                        if (category == Category.NORMAL) IconButton({
                            val go = { vm.hiddenUnlocked = true; vm.push(Screen.Folders(Category.HIDDEN)) }
                            if (config.settings.lockHidden && !vm.hiddenUnlocked) Lock.ask(activity, "Carpetas ocultas", go) else go()
                        }) { Icon(Icons.Outlined.VisibilityOff, "Ocultas") }
                        if (entries.size > 1) IconButton({ reordering = true }) { Icon(Icons.Outlined.SwapVert, "Ordenar") }
                        if (category == Category.NORMAL) IconButton({ vm.push(Screen.Settings) }) { Icon(Icons.Outlined.Settings, "Ajustes") }
                    },
                )
            }
        },
    ) { pad ->
        val gap = if (columns <= 2) 16.dp else 12.dp
        val contentPad = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp,
            bottom = pad.calculateBottomPadding() + if (selected.isNotEmpty()) 104.dp else 24.dp)
        Box(Modifier.fillMaxSize()) {
            when {
                library == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad).padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        when (category) {
                            Category.NORMAL -> "No hay fotos ni vídeos"
                            Category.HIDDEN -> "No hay carpetas ocultas.\n\nMantén pulsada una carpeta y toca «Ocultar»."
                            Category.IGNORED -> "No hay carpetas ignoradas."
                        },
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                reordering -> {
                    val gridState = rememberLazyGridState()
                    val rstate = rememberReorderableLazyGridState(gridState) { from, to ->
                        val a = working.indexOfFirst { it.key == from.key }
                        val b = working.indexOfFirst { it.key == to.key }
                        if (a >= 0 && b >= 0) working.add(b, working.removeAt(a))
                    }
                    LazyVerticalGrid(GridCells.Fixed(columns), state = gridState, contentPadding = contentPad,
                        horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                        items(working, key = { it.key }) { e ->
                            ReorderableItem(rstate, key = e.key) { dragging ->
                                val s by animateFloatAsState(if (dragging) 1.06f else 1f, label = "drag")
                                FolderTile(e, px, selected = false, reorder = true,
                                    modifier = Modifier.longPressDraggableHandle().graphicsLayer { scaleX = s; scaleY = s })
                            }
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    GridCells.Fixed(columns), state = vm.gridState("folders:$category"), contentPadding = contentPad,
                    horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxSize().pinchColumns(columns, 2..4) { n -> vm.setSettings { it.copy(folderColumns = n) } },
                ) {
                    items(entries, key = { it.key }) { e ->
                        FolderTile(
                            e, px, selected = e.key in selected, selecting = selected.isNotEmpty(),
                            modifier = Modifier.animateItem().combinedClickable(
                                onClick = {
                                    if (selected.isNotEmpty()) { if (e.key in selected) selected.remove(e.key) else selected.add(e.key) }
                                    else vm.push(Screen.Folder(e.key))
                                },
                                onLongClick = { if (e.key in selected) selected.remove(e.key) else selected.add(e.key) },
                            ),
                        )
                    }
                }
            }
            if (vm.walking && category == Category.NORMAL) LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(top = pad.calculateTopPadding()))

            // Acciones de la selección, flotando abajo
            AnimatedVisibility(
                selected.isNotEmpty(), enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 16.dp),
            ) {
                Box {
                    FloatingBar(buildList {
                        if (category == Category.NORMAL) add(BarAction(Icons.Outlined.VisibilityOff, "Ocultar") { moveSel(Category.HIDDEN, "Ocultadas") })
                        else add(BarAction(Icons.Outlined.Visibility, "Mostrar") { moveSel(Category.NORMAL, "Visibles") })
                        if (category != Category.IGNORED) add(BarAction(Icons.Outlined.Block, "Ignorar") { moveSel(Category.IGNORED, "Ignoradas") })
                        else add(BarAction(Icons.Outlined.VisibilityOff, "Ocultar") { moveSel(Category.HIDDEN, "Ocultadas") })
                        if (selected.size >= 2 && category != Category.IGNORED) add(BarAction(Icons.Outlined.Layers, "Juntar") { askMerge = true })
                        add(BarAction(Icons.Outlined.Delete, "Eliminar", DANGER) { confirmDelete = true })
                        if (sel.any { it.isGroup }) add(BarAction(Icons.Outlined.MoreVert, "Más") { menu = true })
                    })
                    DropdownMenu(menu && selected.isNotEmpty(), { menu = false }, shape = MaterialTheme.shapes.medium) {
                        DropdownMenuItem({ Text("Separar grupo") }, { menu = false; sel.filter { it.isGroup }.forEach { vm.split(it.key) }; selected.clear() })
                        if (sel.size == 1 && sel[0].isGroup) DropdownMenuItem({ Text("Renombrar grupo") }, { menu = false; askRename = sel[0] })
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        val files = sel.sumOf { it.items.size }
        ConfirmDialog(
            "¿Eliminar ${sel.size} ${if (sel.size == 1) "carpeta" else "carpetas"}?",
            "Se borrarán para siempre $files archivos" +
                (if (sel.any { it.isGroup }) ", incluidos los de todas las carpetas de los grupos" else "") +
                ". Las carpetas que queden vacías también se borran.",
            "Eliminar",
            onConfirm = { val s = sel; selected.clear(); vm.deleteEntries(s) { toast("Borrados: $it archivos") } },
            onDismiss = { confirmDelete = false },
        )
    }
    if (askMerge) NameDialog("Juntar en un grupo", sel.firstOrNull()?.name.orEmpty(), onOk = { name ->
        val keys = entries.map { it.key }.filter { it in selected }
        vm.merge(keys, name); selected.clear(); toast("Juntadas ${keys.size} carpetas")
    }, onDismiss = { askMerge = false })
    askRename?.let { e -> NameDialog("Renombrar grupo", e.name, onOk = { vm.rename(e.key, it); selected.clear() }, onDismiss = { askRename = null }) }
}

@Composable
private fun FolderTile(e: Entry, px: Int, selected: Boolean, modifier: Modifier = Modifier, selecting: Boolean = false, reorder: Boolean = false) {
    val corner by animateDpAsState(if (selected) 36.dp else 24.dp, label = "corner")
    val scale by animateFloatAsState(if (selected) 0.9f else 1f, label = "scale")
    val shape = RoundedCornerShape(corner)
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            e.cover?.let { AsyncImage(Thumb(it, px), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (e.isGroup) Icon(Icons.Outlined.Layers, "Grupo", Modifier.align(Alignment.TopStart).padding(10.dp).size(26.dp)
                .background(Color(0x73000000), CircleShape).padding(4.dp), tint = Color.White)
            if (selecting || selected) Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Outlined.Circle, null,
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp)
                    .background(if (selected) Color.White else Color(0x40000000), CircleShape),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            )
            if (reorder) Icon(Icons.Default.DragIndicator, null, Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp)
                .background(Color(0x73000000), CircleShape).padding(3.dp), tint = Color.White)
        }
        Text(e.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp))
        Text(countLabel(e), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp))
    }
}

private fun countLabel(e: Entry): String {
    val v = e.videoCount
    val f = e.items.size - v
    return listOfNotNull(
        if (f > 0) "$f ${if (f == 1) "foto" else "fotos"}" else null,
        if (v > 0) "$v ${if (v == 1) "vídeo" else "vídeos"}" else null,
    ).joinToString(" · ").ifEmpty { "Vacía" }
}
