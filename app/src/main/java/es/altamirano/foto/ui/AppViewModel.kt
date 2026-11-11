package es.altamirano.foto.ui

import android.app.Application
import android.os.Environment
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.altamirano.foto.data.Category
import es.altamirano.foto.data.Config
import es.altamirano.foto.data.ConfigStore
import es.altamirano.foto.data.Entry
import es.altamirano.foto.data.Library
import es.altamirano.foto.data.MediaItem
import es.altamirano.foto.data.MediaRepository
import es.altamirano.foto.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface Screen {
    data class Folders(val category: Category) : Screen
    data class Folder(val key: String) : Screen
    data class Viewer(val key: String, val index: Int) : Screen
    data object Settings : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepository(app)
    private val store = ConfigStore(app.filesDir)

    private val media = MutableStateFlow<List<MediaItem>?>(null)
    private val _config = MutableStateFlow(store.load())
    val config: StateFlow<Config> = _config

    val library: StateFlow<Library?> = combine(media, _config) { m, c -> m?.let { Library.build(it, c) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val stack = mutableStateListOf<Screen>(Screen.Folders(Category.NORMAL))
    /** Posición a la que volver en la cuadrícula al salir del visor. */
    var lastViewed by mutableStateOf<Pair<String, Int>?>(null)

    var hasPermission by mutableStateOf(Environment.isExternalStorageManager())
        private set

    // Bloqueo
    var appUnlocked by mutableStateOf(!_config.value.settings.lockApp)
    var hiddenUnlocked by mutableStateOf(false)
    private var backgroundAt = 0L

    private var scanJob: Job? = null

    private val gridStates = HashMap<String, LazyGridState>()
    /** El desplazamiento de cada cuadrícula se conserva al entrar y salir. */
    fun gridState(key: String) = gridStates.getOrPut(key) { LazyGridState() }

    init {
        if (hasPermission) startScanning()
    }

    fun checkPermission() {
        val had = hasPermission
        hasPermission = Environment.isExternalStorageManager()
        if (hasPermission && !had) startScanning()
    }

    @OptIn(FlowPreview::class)
    private fun startScanning() {
        if (scanJob != null) return
        scanJob = viewModelScope.launch {
            rescan()
            repo.changes().debounce(800).collect { rescan() }
        }
    }

    fun rescan() = viewModelScope.launch {
        media.value = withContext(Dispatchers.IO) { repo.scan() }
    }

    // ---- navegación
    val current get() = stack.last()
    fun push(s: Screen) { stack += s }
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        val s = stack.removeAt(stack.lastIndex)
        if (s is Screen.Folders && s.category == Category.HIDDEN) hiddenUnlocked = hiddenUnlocked && !config.value.settings.lockHidden
        return true
    }

    // ---- configuración
    private fun edit(f: (Config) -> Config) {
        _config.update(f)
        val c = _config.value
        viewModelScope.launch(Dispatchers.IO) { store.save(c) }
    }

    fun setSettings(f: (Settings) -> Settings) = edit { it.copy(settings = f(it.settings)) }
    fun move(keys: Collection<String>, to: Category) = edit { it.move(keys, to) }
    fun reorder(c: Category, keys: List<String>) = edit { it.withOrder(c, keys) }
    fun merge(keys: List<String>, name: String) = edit { it.merge(keys, name, UUID.randomUUID().toString().take(8)) }
    fun split(key: String) = edit { it.split(key) }
    fun rename(key: String, name: String) = edit { it.rename(key, name) }

    // ---- borrar
    fun deleteItems(items: Collection<MediaItem>, done: (Int) -> Unit = {}) = viewModelScope.launch {
        val ids = withContext(Dispatchers.IO) { repo.delete(items) }
        media.update { m -> m?.filterNot { it.id in ids } }
        done(ids.size)
    }

    fun deleteEntries(entries: Collection<Entry>, done: (Int) -> Unit = {}) = viewModelScope.launch {
        val n = withContext(Dispatchers.IO) {
            repo.deleteFolders(entries.flatMap { it.folders }, entries.flatMap { it.items })
        }
        val gone = entries.flatMap { it.items }.map { it.id }.toSet()
        media.update { m -> m?.filterNot { it.id in gone } }
        edit { c -> c.forget(entries.filter { it.isGroup }.map { it.key }) }
        done(n)
    }

    // ---- ciclo de vida para el bloqueo
    fun onBackground() { backgroundAt = System.currentTimeMillis() }
    fun onForeground() {
        val s = config.value.settings
        val away = System.currentTimeMillis() - backgroundAt
        if (backgroundAt > 0 && away > LOCK_GRACE_MS) {
            if (s.lockApp) appUnlocked = false
            if (s.lockHidden) {
                hiddenUnlocked = false
                // Si estaba dentro de lo oculto, se vuelve a la lista normal.
                val i = stack.indexOfFirst { it is Screen.Folders && it.category == Category.HIDDEN }
                if (i >= 0) while (stack.size > i) stack.removeAt(stack.lastIndex)
            }
        }
        backgroundAt = 0
    }

    companion object { const val LOCK_GRACE_MS = 15_000L }
}
