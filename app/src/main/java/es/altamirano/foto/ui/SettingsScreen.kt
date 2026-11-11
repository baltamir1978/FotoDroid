package es.altamirano.foto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
            title = { Text("Ajustes") },
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            Header("Privacidad")
            SwitchRow("Bloquear la app", if (canLock) "Huella o PIN del móvil al abrir" else "Configura huella o PIN en el móvil",
                s.lockApp, canLock) { on -> Lock.ask(activity, "Confirmar", { vm.setSettings { it.copy(lockApp = on) } }) }
            SwitchRow("Bloquear las ocultas", "Pedir huella o PIN para ver las carpetas ocultas", s.lockHidden, canLock) { on ->
                Lock.ask(activity, "Confirmar", { vm.setSettings { it.copy(lockHidden = on) } })
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Header("Carpetas")
            NavRow("Carpetas ignoradas", "${library?.ignored?.size ?: 0} · no aparecen en ningún sitio (con sus subcarpetas)") {
                vm.push(Screen.Folders(Category.IGNORED))
            }
            SwitchRow("Buscar en todo el almacenamiento", "También carpetas con .nomedia o que el sistema no indexa", s.scanFiles) { on ->
                vm.setSettings { it.copy(scanFiles = on) }
            }
            SwitchRow("Incluir carpetas ocultas", "Las que empiezan por punto (se pueden ignorar después)", s.dotFolders, s.scanFiles) { on ->
                vm.setSettings { it.copy(dotFolders = on) }
            }
            ChoiceRow("Columnas de carpetas", listOf(2, 3, 4), s.folderColumns) { n -> vm.setSettings { it.copy(folderColumns = n) } }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Header("Archivos")
            SwitchRow("Recientes primero", "Si no, de más antiguo a más nuevo", s.newestFirst) { on -> vm.setSettings { it.copy(newestFirst = on) } }
            ChoiceRow("Columnas", listOf(3, 4, 5, 6), s.mediaColumns) { n -> vm.setSettings { it.copy(mediaColumns = n) } }
            SwitchRow("Repetir vídeos", "Al terminar, vuelven a empezar", s.loopVideos) { on -> vm.setSettings { it.copy(loopVideos = on) } }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            NavRow("Volver a leer el almacenamiento", "Si falta algo recién copiado") { vm.rescan() }
            Text("FotoDroid ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun Header(t: String) = Text(t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))

@Composable
private fun SwitchRow(title: String, sub: String, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled) { onChange(!value) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(value, onChange, enabled = enabled)
    }
}

@Composable
private fun NavRow(title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceRow(title: String, options: List<Int>, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { i, n ->
                SegmentedButton(selected = n == value, onClick = { onChange(n) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size), icon = {}) { Text("$n") }
            }
        }
    }
}
