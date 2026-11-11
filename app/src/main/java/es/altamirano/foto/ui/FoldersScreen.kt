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
import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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
    var confirmDelete by remember { mutableStateOf(false) }
    var askMerge by remember { mutableStateOf(false) }
    var askRename by remember { mutableStateOf<Entry?>(null) }
    var menu by remember { mutableStateOf(false) }
    val haze = remember { HazeState() }

    LaunchedEffect(entries) { selected.retainAll(entries.map { it.key }.toSet()) }
    BackHandler(selected.isNotEmpty()) { selected.clear() }

    val sel = entries.filter { it.key in selected }
    fun toast(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    fun moveSel(to: Category, msg: String) { val n = selected.size; vm.move(selected.toList(), to); selected.clear(); toast("$msg: $n") }

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
                    modifier = Modifier.glass(haze), colors = clearBarColors(),
                    navigationIcon = { BarButton(Icons.Rounded.Close, "Cancelar") { selected.clear() } },
                    title = { Text("${selected.size} seleccionadas") },
                    actions = { BarButton(Icons.Rounded.SelectAll, "Todas") { selected.clear(); selected.addAll(entries.map { it.key }) } },
                )
                else -> LargeTopAppBar(
                    modifier = Modifier.glass(haze),
                    scrollBehavior = scroll,
                    navigationIcon = {
                        if (category != Category.NORMAL) BarButton(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás") { vm.pop() }
                    },
                    title = { Text(title) },
                    colors = clearBarColors(),
                    actions = {
                        if (category == Category.NORMAL) BarButton(
                            if (config.settings.lockHidden && !vm.hiddenUnlocked) Icons.Rounded.Lock else Icons.Rounded.VisibilityOff, "Ocultas",
                        ) {
                            val go = { vm.hiddenUnlocked = true; vm.push(Screen.Folders(Category.HIDDEN)) }
                            if (config.settings.lockHidden && !vm.hiddenUnlocked) Lock.ask(activity, "Carpetas ocultas", go) else go()
                        }
                        if (category == Category.NORMAL) BarButton(Icons.Rounded.Settings, "Ajustes") { vm.push(Screen.Settings) }
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
                else -> {
                    // Mantener pulsada una carpeta y arrastrar la recoloca; si se suelta sin moverla, se
                    // selecciona. Se decide al soltar, para que la barra no cambie (y la cuadrícula no
                    // salte) en mitad del arrastre.
                    val gridState = vm.gridState("folders:$category")
                    val live = remember(entries) { entries.toMutableStateList() }
                    var dragFrom by remember { mutableStateOf(emptyList<String>()) }
                    val haptic = LocalHapticFeedback.current
                    val rstate = rememberReorderableLazyGridState(gridState) { from, to ->
                        val a = live.indexOfFirst { it.key == from.key }
                        val b = live.indexOfFirst { it.key == to.key }
                        if (a >= 0 && b >= 0) { live.add(b, live.removeAt(a)); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    }
                    LazyVerticalGrid(
                        GridCells.Fixed(columns), state = gridState, contentPadding = contentPad,
                        horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxSize().hazeSource(haze).pinchColumns(columns, 2..4) { n -> vm.setSettings { it.copy(folderColumns = n) } },
                    ) {
                        items(live, key = { it.key }) { e ->
                            ReorderableItem(rstate, key = e.key) { dragging ->
                                val lift by animateFloatAsState(if (dragging) 1.06f else 1f, label = "drag")
                                FolderTile(
                                    e, px, selected = e.key in selected, selecting = selected.isNotEmpty(),
                                    modifier = Modifier.graphicsLayer { scaleX = lift; scaleY = lift }
                                        .longPressDraggableHandle(
                                            onDragStarted = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragFrom = live.map { it.key }
                                            },
                                            onDragStopped = {
                                                val now = live.map { it.key }
                                                if (now != dragFrom) vm.reorder(category, now)
                                                else if (e.key in selected) selected.remove(e.key) else selected.add(e.key)
                                            },
                                        )
                                        .clickable {
                                            if (selected.isNotEmpty()) { if (e.key in selected) selected.remove(e.key) else selected.add(e.key) }
                                            else vm.push(Screen.Folder(e.key))
                                        },
                                )
                            }
                        }
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
                        if (category == Category.NORMAL) add(BarAction(Icons.Rounded.VisibilityOff, "Ocultar") { moveSel(Category.HIDDEN, "Ocultadas") })
                        else add(BarAction(Icons.Rounded.Visibility, "Mostrar") { moveSel(Category.NORMAL, "Visibles") })
                        if (category != Category.IGNORED) add(BarAction(Icons.Rounded.Block, "Ignorar") { moveSel(Category.IGNORED, "Ignoradas") })
                        else add(BarAction(Icons.Rounded.VisibilityOff, "Ocultar") { moveSel(Category.HIDDEN, "Ocultadas") })
                        if (selected.size >= 2 && category != Category.IGNORED) add(BarAction(Icons.Rounded.Layers, "Juntar") { askMerge = true })
                        add(BarAction(Icons.Rounded.Delete, "Eliminar", DANGER) { confirmDelete = true })
                        if (sel.any { it.isGroup }) add(BarAction(Icons.Rounded.MoreVert, "Más") { menu = true })
                    }, haze)
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
    if (askMerge) {
        val keys = entries.map { it.key }.filter { it in selected }
        val target = config.mergeTarget(keys)
        val initial = target?.let { t -> sel.firstOrNull { it.key == t }?.name } ?: sel.firstOrNull()?.name.orEmpty()
        NameDialog(if (target != null) "Añadir al grupo" else "Juntar en un grupo", initial, onOk = { name ->
            vm.merge(keys, name); selected.clear()
            toast(if (target != null) "Añadidas al grupo «$name»" else "Juntadas ${keys.size} carpetas")
        }, onDismiss = { askMerge = false })
    }
    askRename?.let { e -> NameDialog("Renombrar grupo", e.name, onOk = { vm.rename(e.key, it); selected.clear() }, onDismiss = { askRename = null }) }
}

@Composable
private fun FolderTile(e: Entry, px: Int, selected: Boolean, modifier: Modifier = Modifier, selecting: Boolean = false) {
    val corner by animateDpAsState(if (selected) 36.dp else 24.dp, label = "corner")
    val scale by animateFloatAsState(if (selected) 0.9f else 1f, label = "scale")
    val shape = RoundedCornerShape(corner)
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            e.cover?.let { AsyncImage(Thumb(it, px), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (e.isGroup) Icon(Icons.Rounded.Layers, "Grupo", Modifier.align(Alignment.TopStart).padding(10.dp).size(26.dp)
                .background(Color(0x73000000), CircleShape).padding(4.dp), tint = Color.White)
            if (selecting || selected) Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Outlined.Circle, null,
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp)
                    .background(if (selected) Color.White else Color(0x40000000), CircleShape),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            )
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
