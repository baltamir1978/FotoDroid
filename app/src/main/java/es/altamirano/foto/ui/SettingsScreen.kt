package es.altamirano.foto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import es.altamirano.foto.BuildConfig
import es.altamirano.foto.data.Category
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val config by vm.config.collectAsState()
    val library by vm.library.collectAsState()
    val trashed by vm.trashed.collectAsState()
    val s = config.settings
    val activity = androidx.activity.compose.LocalActivity.current as FragmentActivity
    val canLock = Lock.available(activity)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haze = remember { HazeState() }
    val bg = MaterialTheme.colorScheme.surfaceContainerLow

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = bg,
        topBar = {
            LargeTopAppBar(
                modifier = Modifier.glass(haze, bg),
                scrollBehavior = scroll,
                navigationIcon = { BarButton(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás") { vm.pop() } },
                title = { Text("Ajustes") },
                colors = clearBarColors(),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().hazeSource(haze).verticalScroll(rememberScrollState())
                .padding(top = pad.calculateTopPadding()).padding(horizontal = 16.dp).navigationBarsPadding(),
        ) {
            Section("Privacidad", Tone.TERTIARY) {
                add { SwitchRow(Icons.Rounded.Fingerprint, "Bloquear la app",
                    if (canLock) "Huella o PIN del móvil al abrir" else "Configura huella o PIN en el móvil", s.lockApp, canLock) { on ->
                    Lock.ask(activity, "Confirmar", { vm.setSettings { it.copy(lockApp = on) } })
                } }
                add { SwitchRow(Icons.Rounded.Lock, "Bloquear las ocultas", "Pedir huella o PIN para verlas", s.lockHidden, canLock) { on ->
                    Lock.ask(activity, "Confirmar", { vm.setSettings { it.copy(lockHidden = on) } })
                } }
            }
            Section("Carpetas", Tone.PRIMARY) {
                add { NavRow(Icons.Rounded.Block, "Carpetas ignoradas",
                    "${library?.ignored?.size ?: 0} · no aparecen en ningún sitio, con sus subcarpetas") { vm.push(Screen.Folders(Category.IGNORED)) } }
                add { SwitchRow(Icons.Rounded.TravelExplore, "Buscar en todo el almacenamiento", "También carpetas con .nomedia o sin indexar", s.scanFiles) { on ->
                    vm.setSettings { it.copy(scanFiles = on) }
                } }
                add { SwitchRow(Icons.Rounded.VisibilityOff, "Incluir carpetas que empiezan por punto", "Se pueden ignorar después", s.dotFolders, s.scanFiles) { on ->
                    vm.setSettings { it.copy(dotFolders = on) }
                } }
                add { ChoiceRow(Icons.Rounded.GridView, "Columnas", listOf(2, 3, 4), s.folderColumns) { n -> vm.setSettings { it.copy(folderColumns = n) } } }
            }
            Section("Fotos y vídeos", Tone.SECONDARY) {
                add { SwitchRow(Icons.Rounded.History, "Recientes primero", "Si no, de más antiguo a más nuevo", s.newestFirst) { on ->
                    vm.setSettings { it.copy(newestFirst = on) }
                } }
                add { ChoiceRow(Icons.Rounded.ViewModule, "Columnas", listOf(3, 4, 5, 6), s.mediaColumns) { n -> vm.setSettings { it.copy(mediaColumns = n) } } }
                add { SwitchRow(Icons.Rounded.Repeat, "Repetir vídeos", "Al terminar, vuelven a empezar", s.loopVideos) { on ->
                    vm.setSettings { it.copy(loopVideos = on) }
                } }
            }
            Section("Papelera", Tone.TERTIARY) {
                add { SwitchRow(Icons.Rounded.Delete, "Usar la papelera", "Lo borrado se guarda 30 días; si no, se borra para siempre", s.useTrash) { on ->
                    vm.setSettings { it.copy(useTrash = on) }
                } }
                add { NavRow(Icons.Rounded.RestoreFromTrash, "Ver la papelera",
                    if (trashed.isEmpty()) "Vacía" else "${trashed.size} · ${android.text.format.Formatter.formatFileSize(activity, trashed.sumOf { it.size })}") {
                    // En la papelera puede haber fotos de carpetas ocultas: con ese bloqueo puesto, se pide.
                    if (s.lockHidden && !vm.hiddenUnlocked) Lock.ask(activity, "Papelera", { vm.push(Screen.Trash) }) else vm.push(Screen.Trash)
                } }
            }
            Section(null, Tone.NEUTRAL) {
                add { NavRow(Icons.Rounded.Sync, "Volver a leer el almacenamiento", "Si falta algo recién copiado") { vm.rescan() } }
            }
            Text("Pellizca en cualquier cuadrícula para cambiar el número de columnas.\nFotoDroid ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp))
        }
    }
}

/** Grupo de filas en tarjetas: esquinas grandes fuera, pequeñas entre filas, como los Ajustes de Android 16. */
@Composable
private fun Section(title: String?, tone: Tone, rows: MutableList<@Composable () -> Unit>.() -> Unit) {
    val list = mutableListOf<@Composable () -> Unit>().apply(rows)
    if (title != null) Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
    else Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { CompositionLocalProvider(LocalTone provides tone) {
        list.forEachIndexed { i, row ->
            val top = if (i == 0) 24.dp else 6.dp
            val bottom = if (i == list.lastIndex) 24.dp else 6.dp
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))) { row() }
        }
    } }
}

/** Color de los iconos de cada sección, como los Ajustes de Android 16. */
private enum class Tone { PRIMARY, SECONDARY, TERTIARY, NEUTRAL }
private val LocalTone = staticCompositionLocalOf { Tone.PRIMARY }

@Composable
private fun Lead(icon: ImageVector) {
    val c = MaterialTheme.colorScheme
    val (bg, fg) = when (LocalTone.current) {
        Tone.PRIMARY -> c.primary to c.onPrimary
        Tone.SECONDARY -> c.secondary to c.onSecondary
        Tone.TERTIARY -> c.tertiary to c.onTertiary
        Tone.NEUTRAL -> c.surfaceVariant to c.onSurfaceVariant
    }
    Box(Modifier.size(40.dp).background(bg, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = fg)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, sub: String, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled) { onChange(!value) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Lead(icon)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(value, onChange, enabled = enabled)
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Lead(icon)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceRow(icon: ImageVector, title: String, options: List<Int>, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Lead(icon)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { i, n ->
                SegmentedButton(selected = n == value, onClick = { onChange(n) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size), icon = {}) { Text("$n") }
            }
        }
    }
}
