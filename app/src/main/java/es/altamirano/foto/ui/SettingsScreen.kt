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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.VisibilityOff
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val config by vm.config.collectAsState()
    val library by vm.library.collectAsState()
    val s = config.settings
    val activity = LocalContext.current as FragmentActivity
    val canLock = Lock.available(activity)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scroll,
                navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
                title = { Text("Ajustes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
        ) {
            Section("Privacidad") {
                add { SwitchRow(Icons.Outlined.Fingerprint, "Bloquear la app",
                    if (canLock) "Huella o PIN del móvil al abrir" else "Configura huella o PIN en el móvil", s.lockApp, canLock) { on ->
                    Lock.ask(activity, "Confirmar", { vm.setSettings { it.copy(lockApp = on) } })
                } }
                add { SwitchRow(Icons.Outlined.Lock, "Bloquear las ocultas", "Pedir huella o PIN para verlas", s.lockHidden, canLock) { on ->
                    Lock.ask(activity, "Confirmar", { vm.setSettings { it.copy(lockHidden = on) } })
                } }
            }
            Section("Carpetas") {
                add { NavRow(Icons.Outlined.Block, "Carpetas ignoradas",
                    "${library?.ignored?.size ?: 0} · no aparecen en ningún sitio, con sus subcarpetas") { vm.push(Screen.Folders(Category.IGNORED)) } }
                add { SwitchRow(Icons.Outlined.Search, "Buscar en todo el almacenamiento", "También carpetas con .nomedia o sin indexar", s.scanFiles) { on ->
                    vm.setSettings { it.copy(scanFiles = on) }
                } }
                add { SwitchRow(Icons.Outlined.VisibilityOff, "Incluir carpetas que empiezan por punto", "Se pueden ignorar después", s.dotFolders, s.scanFiles) { on ->
                    vm.setSettings { it.copy(dotFolders = on) }
                } }
                add { ChoiceRow(Icons.Outlined.GridView, "Columnas", listOf(2, 3, 4), s.folderColumns) { n -> vm.setSettings { it.copy(folderColumns = n) } } }
            }
            Section("Fotos y vídeos") {
                add { SwitchRow(Icons.Outlined.DateRange, "Recientes primero", "Si no, de más antiguo a más nuevo", s.newestFirst) { on ->
                    vm.setSettings { it.copy(newestFirst = on) }
                } }
                add { ChoiceRow(Icons.Outlined.ViewModule, "Columnas", listOf(3, 4, 5, 6), s.mediaColumns) { n -> vm.setSettings { it.copy(mediaColumns = n) } } }
                add { SwitchRow(Icons.Outlined.Repeat, "Repetir vídeos", "Al terminar, vuelven a empezar", s.loopVideos) { on ->
                    vm.setSettings { it.copy(loopVideos = on) }
                } }
            }
            Section(null) {
                add { NavRow(Icons.Outlined.Refresh, "Volver a leer el almacenamiento", "Si falta algo recién copiado") { vm.rescan() } }
            }
            Text("Pellizca en cualquier cuadrícula para cambiar el número de columnas.\nFotoDroid ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp))
        }
    }
}

/** Grupo de filas en tarjetas: esquinas grandes fuera, pequeñas entre filas, como los Ajustes de Android 16. */
@Composable
private fun Section(title: String?, rows: MutableList<@Composable () -> Unit>.() -> Unit) {
    val list = mutableListOf<@Composable () -> Unit>().apply(rows)
    if (title != null) Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
    else Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        list.forEachIndexed { i, row ->
            val top = if (i == 0) 24.dp else 6.dp
            val bottom = if (i == list.lastIndex) 24.dp else 6.dp
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))) { row() }
        }
    }
}

@Composable
private fun Lead(icon: ImageVector) {
    Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
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
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
