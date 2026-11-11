package es.altamirano.foto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.Coil
import es.altamirano.foto.ui.AppViewModel
import es.altamirano.foto.ui.FolderScreen
import es.altamirano.foto.ui.FoldersScreen
import es.altamirano.foto.ui.FotoTheme
import es.altamirano.foto.ui.Lock
import es.altamirano.foto.ui.Screen
import es.altamirano.foto.ui.SettingsScreen
import es.altamirano.foto.ui.ViewerScreen
import es.altamirano.foto.ui.buildImageLoader

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Coil.setImageLoader { buildImageLoader(applicationContext) }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = vm.onBackground()
            override fun onStart(owner: LifecycleOwner) = vm.onForeground()
        })
        setContent {
            FotoTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Root() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.checkPermission()
    }

    @Composable
    private fun Root() {
        val config by vm.config.collectAsState()
        // Sin captura en «Recientes» si la app va con bloqueo.
        LaunchedEffect(config.settings.lockApp) { setRecentsScreenshotEnabled(!config.settings.lockApp) }
        when {
            !vm.hasPermission -> PermissionScreen()
            config.settings.lockApp && !vm.appUnlocked -> LockScreen()
            else -> {
                BackHandler(vm.stack.size > 1) { vm.pop() }
                // El visor va encima de la cuadrícula, que sigue viva: al volver no se recarga nada.
                val top = vm.current
                val base = vm.stack.lastOrNull { it !is Screen.Viewer } ?: Screen.Folders(es.altamirano.foto.data.Category.NORMAL)
                key(base) {
                    when (base) {
                        is Screen.Folders -> FoldersScreen(vm, base.category)
                        is Screen.Folder -> FolderScreen(vm, base.key)
                        Screen.Settings -> SettingsScreen(vm)
                        is Screen.Viewer -> {}
                    }
                }
                if (top is Screen.Viewer) key(top) { ViewerScreen(vm, top.key, top.index) }
            }
        }
    }

    @Composable
    private fun LockScreen() {
        val unlock = { Lock.ask(this, "FotoDroid", onOk = { vm.appUnlocked = true }) }
        LaunchedEffect(Unit) { unlock() }
        Centered {
            Icon(Icons.Default.Fingerprint, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Button(onClick = unlock) { Text("Desbloquear") }
        }
    }

    @Composable
    private fun PermissionScreen() {
        Centered {
            Icon(Icons.Default.FolderOpen, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(
                "FotoDroid necesita acceso a todos los archivos para leer y borrar fotos y vídeos " +
                    "de cualquier carpeta sin pedir permiso cada vez.",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            }) { Text("Dar acceso") }
        }
    }

    @Composable
    private fun Centered(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
        }
    }
}
