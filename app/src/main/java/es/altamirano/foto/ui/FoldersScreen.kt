package es.altamirano.foto.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import es.altamirano.foto.data.Category
import es.altamirano.foto.data.Entry
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

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

    // Si una carpeta desaparece, sale de la selección.
    LaunchedEffect(entries) { selected.retainAll(entries.map { it.key }.toSet()) }
    BackHandler(selected.isNotEmpty()) { selected.clear() }

    val sel = entries.filter { it.key in selected }
    fun toast(msg: String) = scope.launch { snackbar.showSnackbar(msg) }

    // Lista local mientras se reordena
    val working = remember(category, reordering) { entries.toMutableStateList() }
    fun saveOrder() = vm.reorder(category, working.map { it.key })
    BackHandler(reordering) { saveOrder(); reordering = false }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            when {
                selected.isNotEmpty() -> TopAppBar(
                    navigationIcon = { IconButton({ selected.clear() }) { Icon(Icons.Default.Close, "Cancelar") } },
                    title = { Text("${selected.size}") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    actions = {
                        if (category == Category.NORMAL) IconButton({ vm.move(selected.toList(), Category.HIDDEN); toast("Ocultadas: ${selected.size}"); selected.clear() }) {
                            Icon(Icons.Default.VisibilityOff, "Ocultar")
                        }
                        if (category != Category.NORMAL) IconButton({ vm.move(selected.toList(), Category.NORMAL); toast("Visibles: ${selected.size}"); selected.clear() }) {
                            Icon(Icons.Default.Visibility, "Mostrar")
                        }
                        if (category != Category.IGNORED) IconButton({ vm.move(selected.toList(), Category.IGNORED); toast("Ignoradas: ${selected.size}"); selected.clear() }) {
                            Icon(Icons.Default.Block, "Ignorar")
                        }
                        if (selected.size >= 2 && category != Category.IGNORED) IconButton({ askMerge = true }) { Icon(Icons.Default.Layers, "Juntar") }
                        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Más") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Seleccionar todo") }, { menu = false; selected.clear(); selected.addAll(entries.map { it.key }) })
                            if (category == Category.IGNORED) DropdownMenuItem({ Text("Ocultar") }, { menu = false; vm.move(selected.toList(), Category.HIDDEN); selected.clear() })
                            if (sel.any { it.isGroup }) DropdownMenuItem({ Text("Separar grupo") }, {
                                menu = false; sel.filter { it.isGroup }.forEach { vm.split(it.key) }; selected.clear()
                            })
                            if (sel.size == 1 && sel[0].isGroup) DropdownMenuItem({ Text("Renombrar grupo") }, { menu = false; askRename = sel[0] })
                            DropdownMenuItem({ Text("Eliminar", color = Color(0xFFE5484D)) }, { menu = false; confirmDelete = true })
                        }
                    },
                )
                reordering -> TopAppBar(
                    navigationIcon = { IconButton({ saveOrder(); reordering = false }) { Icon(Icons.Default.Done, "Hecho") } },
                    title = { Text("Ordenar") },
                    actions = {
                        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Más") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Por nombre") }, {
                                menu = false
                                val s = working.sortedBy { it.name.lowercase() }; working.clear(); working.addAll(s)
                            })
                            DropdownMenuItem({ Text("Por fecha (reciente primero)") }, {
                                menu = false
                                val s = working.sortedByDescending { it.newest }; working.clear(); working.addAll(s)
                            })
                        }
                    },
                )
                else -> TopAppBar(
                    navigationIcon = {
                        if (category != Category.NORMAL) IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                    },
                    title = {
                        Text(when (category) { Category.NORMAL -> "FotoDroid"; Category.HIDDEN -> "Ocultas"; Category.IGNORED -> "Ignoradas" })
                    },
                    actions = {
                        if (category == Category.NORMAL) IconButton({
                            val go = { vm.hiddenUnlocked = true; vm.push(Screen.Folders(Category.HIDDEN)) }
                            if (config.settings.lockHidden && !vm.hiddenUnlocked) Lock.ask(activity, "Carpetas ocultas", go) else go()
                        }) { Icon(Icons.Default.VisibilityOff, "Ocultas") }
                        if (entries.size > 1) IconButton({ reordering = true }) { Icon(Icons.Default.SwapVert, "Ordenar") }
                        if (category == Category.NORMAL) IconButton({ vm.push(Screen.Settings) }) { Icon(Icons.Default.Settings, "Ajustes") }
                    },
                )
            }
        },
    ) { pad ->
        val cols = GridCells.Fixed(config.settings.folderColumns)
        val contentPad = PaddingValues(start = 12.dp, end = 12.dp, top = pad.calculateTopPadding() + 4.dp, bottom = pad.calculateBottomPadding() + 16.dp)
        when {
            library == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    when (category) {
                        Category.NORMAL -> "No hay fotos ni vídeos"
                        Category.HIDDEN -> "No hay carpetas ocultas.\nMantén pulsada una carpeta y toca el ojo tachado para ocultarla."
                        Category.IGNORED -> "No hay carpetas ignoradas."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            reordering -> {
                val gridState = rememberLazyGridState()
                val rstate = rememberReorderableLazyGridState(gridState) { from, to ->
                    val a = working.indexOfFirst { it.key == from.key }
                    val b = working.indexOfFirst { it.key == to.key }
                    if (a >= 0 && b >= 0) working.add(b, working.removeAt(a))
                }
                LazyVerticalGrid(cols, state = gridState, contentPadding = contentPad,
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(working, key = { it.key }) { e ->
                        ReorderableItem(rstate, key = e.key) { dragging ->
                            FolderTile(e, selected = false, modifier = Modifier.longPressDraggableHandle().scale(if (dragging) 1.06f else 1f), reorder = true)
                        }
                    }
                }
            }
            else -> LazyVerticalGrid(cols, state = vm.gridState("folders:$category"), contentPadding = contentPad,
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(entries, key = { it.key }) { e ->
                    FolderTile(
                        e, selected = e.key in selected,
                        modifier = Modifier.combinedClickable(
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
    }

    if (confirmDelete) {
        val files = sel.sumOf { it.items.size }
        ConfirmDialog(
            "Eliminar ${sel.size} ${if (sel.size == 1) "carpeta" else "carpetas"}",
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
private fun FolderTile(e: Entry, selected: Boolean, modifier: Modifier = Modifier, reorder: Boolean = false) {
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier)
        ) {
            e.cover?.let { AsyncImage(Thumb(it, THUMB_PX), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (e.isGroup) Icon(Icons.Outlined.Layers, "Grupo", Modifier.align(Alignment.TopStart).padding(6.dp).size(20.dp)
                .background(Color(0x66000000), CircleShape).padding(2.dp), tint = Color.White)
            if (selected) Icon(Icons.Default.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(6.dp)
                .background(Color.White, CircleShape), tint = MaterialTheme.colorScheme.primary)
            if (reorder) Icon(Icons.Default.DragIndicator, null, Modifier.align(Alignment.TopEnd).padding(6.dp)
                .background(Color(0x66000000), CircleShape).padding(2.dp), tint = Color.White)
        }
        Text(e.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, top = 6.dp))
        Text("${e.items.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp))
    }
}
