# FotoDroid — contexto del repositorio

Galería de fotos y vídeos **por carpetas** (sin álbumes, sin edición), escrita desde cero en
**Kotlin + Jetpack Compose**. Sólo para el móvil de Bruno: **Pixel 11 Pro con Android 17**, sin
compatibilidad hacia atrás (`minSdk 35`), autofirmada. El móvil suele estar conectado por adb (`~/Android/Sdk/platform-tools/adb`):
instalar con `adb install -r` después de compilar. Inspiración (no código):
Fossify Gallery y QP Gallery.

## Estado

**0.8 (13-nov-2026): papelera y pila al día. Hecha en el servidor (`plex`), sin móvil y sin la
clave de firma.** El mismo día se compiló y firmó en el portátil (pasan pruebas y lint): APK en
`~/Documentos/FotoDroid-0.8.apk`, con el mismo certificado que la 0.7. **Sin probar en el móvil**
(en el emulador no se instala: sólo trae librerías arm64). Falta **probar la papelera con fotos que no importen antes de fiarse de ella**.
- **Papelera** (`data/Trash.kt`, `ui/TrashScreen.kt`, Ajustes → Papelera; se puede desactivar con
  `Settings.useTrash`). Borrar **mueve** el archivo a `.FotoDroid-papelera` en la raíz de su
  volumen (un cambio de nombre, con `.nomedia`) y lo apunta en `files/papelera.json`; a los 30
  días se borra de verdad, al abrir la app. Desde la pantalla se restaura a su sitio (rehace la
  carpeta; si hay otro con ese nombre, vuelve como «nombre (1).ext») o se borra para siempre.
  - **Si no se puede mover, no se borra.** `purge` sólo borra dentro de una carpeta de papelera.
  - **A la papelera se va sin tocar MediaStore**: el sistema sigue el cambio de nombre y su fila
    pasa a apuntar a la papelera, así que un `resolver.delete` después borraría para siempre lo
    recién guardado. Esto está razonado, no medido: es lo primero que hay que comprobar en el
    móvil (borrar una foto indexada y ver que sigue en la papelera y deja de salir en la galería).
  - Lo que esté en la carpeta sin constar en el índice (datos de la app borrados) sale igual, no
    caduca solo y vuelve a `Pictures/FotoDroid recuperado`.
  - El recorrido y la lectura de MediaStore saltan la carpeta. Con «Bloquear las ocultas», entrar
    en la papelera pide huella (puede haber fotos de carpetas ocultas).
  - Al borrar una carpeta entera se pierde su `.nomedia`: restaurada, el sistema la indexa.
  - 8 pruebas en `TrashTest`, sobre una carpeta temporal (18 en total).
- **`FileProvider`** acotado al almacenamiento compartido (`external-path` y `/storage`), antes `/`.
- **Pila a la par que los demás `*Droid`**: Gradle 9.9.1, AGP 9.5.1 (Kotlin integrado: sin plugin
  `kotlin-android` ni `kotlinOptions`), Kotlin 2.4.21, Compose BOM 2026.09, Media3 1.11.1,
  `compileSdk` 37, `targetSdk` 36. **Haze sigue en 1.2.2, telephoto en 0.15.1, Reorderable en 2.4.3
  y el FFmpeg de Jellyfin en 1.5.0+1**: sus versiones nuevas cambian de API y no se pueden probar
  aquí; compilan con el Compose nuevo, pero hay que ver en el móvil el cristal, el zoom, el
  arrastre y el audio AC3/DTS.
- **CI** en GitHub Actions; `lintDebug` pasa sin errores (`NonObservableLocale` y
  `LocalContextGetResourceValueCall` van como avisos).

**0.7 (11-nov-2026): fuera el botón ⇅** (Bruno: «ya no aporta»). Se quita el modo de ordenar
entero y con él «por nombre» / «por fecha»; recolocar es sólo arrastrar. Instalada por adb.

**0.6 (11-nov-2026): arrastrar para recolocar sin entrar en ⇅.** En los tres listados de carpetas,
mantener pulsada y arrastrar recoloca (se guarda al soltar); mantener pulsada y soltar sin mover
selecciona. La selección se decide al soltar a propósito: si se seleccionara al empezar, la barra
grande pasaría a la de selección y la cuadrícula saltaría en mitad del arrastre. Instalada por adb. APK en `~/Documentos/FotoDroid-0.6.apk`.
Bruno vio que «no hay ocultas» cuando sí las había: era porque estaban dentro de una carpeta
ignorada (ignorar se hereda a las subcarpetas, por diseño desde la 0.1).

**0.5 (11-nov-2026): cristal, iconos y juntar en un grupo existente.** Sin probar en el móvil.
APK en `~/Documentos/FotoDroid-0.5.apk`.
- **Barras de cristal** con **Haze 1.2.2** (la versión que va con Compose 1.7.6 / BOM 2024.12; las
  2.x piden Compose más nuevo): el contenido lleva `hazeSource(state)` y la barra `Modifier.glass(state)`
  (`Glass.kt`, `HazeMaterials.thin`). En Carpetas, carpeta, Ajustes y la barra flotante de selección.
- **Iconos**: todos `Icons.Rounded` (menos el círculo vacío de selección); los de las barras van
  en círculos tonales (`BarButton`); el botón de ocultas muestra un candado si están bloqueadas;
  en Ajustes los iconos van en cuadrados redondeados con un color por sección (`Tone`).
- **Juntar con un grupo ya creado**: lo demás se añade a ese grupo (`Config.mergeTarget`), que
  conserva id, categoría y sitio, y el diálogo («Añadir al grupo») propone su nombre.

**0.4 (11-nov-2026): demasiadas fotos** (Bruno: «hay demasiadas fotos, incluyendo las de
carpetas de sistema»). El recorrido de la 0.2 metía basura que MediaStore no indexa. Ahora
`FileScanner`: salta los archivos que empiezan por punto (sobre todo `.trashed-…`, la papelera de
Google Fotos, que volvía a enseñar lo borrado, y `.pending-…`); salta carpetas de basura por
nombre (`JUNK_DIRS`: cache, thumbs, stickers, avatars…, con o sin punto); en `Android/media` no
entra en paquetes `android*`, `com.android.*`, `com.google.*`, `com.samsung.*`; deja fuera las
imágenes de menos de 12 KB (`MIN_IMAGE_BYTES`). «Carpetas que empiezan por punto» pasa a estar
desactivado por defecto. Prueba en `FileScannerTest`. Sin probar en el móvil. APK en
`~/Documentos/FotoDroid-0.4.apk`.

**0.3 (11-nov-2026): escalado y aspecto** (Bruno: «una app sencilla, pero más en línea con el
diseño de este siglo»). Sin probar en el móvil. APK en `~/Documentos/FotoDroid-0.3.apk`.
- **Escalado**: las miniaturas se piden al tamaño real de la casilla (`thumbPxFor`, escalones
  192…1024 px según columnas y ancho de pantalla); antes eran 320 px fijos y salían borrosas en
  casillas grandes. El visor usa la de la cuadrícula como marcador (`vm.mediaThumbPx`).
  **Pellizcar** cambia columnas (`pinchColumns`, en el pase `Initial` para que la cuadrícula no se
  desplace): carpetas 2–4, archivos 2–7, con animación (`animateItem`). Doble toque a 2,5× y vuelta
  (antes iba directo a 8×; pellizcando se sigue llegando a 8×).
- **Aspecto** (Material 3 Expressive): fuentes del Pixel (`DeviceFontFamilyName("google-sans")` y
  `google-sans-text`, con Roboto si no están), formas más redondeadas, título grande plegable en
  Carpetas y Ajustes, barra que se esconde al bajar en la cuadrícula, **barra flotante en píldora**
  con las acciones de la selección (`FloatingBar`), selección que encoge y redondea la casilla,
  círculos de selección, recuento «12 fotos · 3 vídeos», asa rápida con burbuja de mes y año,
  Ajustes en tarjetas agrupadas con icono (como Android 16), pantallas de permiso y bloqueo nuevas.
- **Visor**: botones en círculos translúcidos, **deslizar hacia abajo para cerrar** (sólo sin
  zoom: si un hijo consume el gesto, no se activa), barra de vídeo con pista gruesa y asa vertical
  con hueco (estilo de los deslizadores actuales), tiempos en píldoras.

**0.2 (11-nov-2026): faltaban carpetas con fotos** (Bruno, probando la 0.1 en el móvil). La 0.1
sólo leía MediaStore, que no indexa carpetas con `.nomedia`, carpetas que empiezan por punto ni lo
que algunas apps no registran. Ahora `FileScanner` recorre el almacenamiento (todos los volúmenes;
de `Android/` sólo `media`, porque `data` y `obb` no se pueden leer; salta `.thumbnails`, `.cache`,
`.trashed*`… y las carpetas ignoradas) y añade lo que MediaStore no tiene, con **id negativo**
(FNV-1a de la ruta) y URI `file://`. Para compartir esos archivos o abrirlos con otra app se usa un
`FileProvider` (`${applicationId}.files`). MediaStore se vuelve a leer con cada cambio; el recorrido
se hace al arrancar, desde «Volver a leer el almacenamiento» y al volver a la app si han pasado más
de 2 min (barra de progreso fina arriba mientras tanto). En Ajustes se puede desactivar, y también
quitar las carpetas que empiezan por punto. Las carpetas encontradas así salen al principio
(«sin sitio»). Sin probar en el móvil. APK en `~/Documentos/FotoDroid-0.2.apk`. Si siguen
faltando carpetas, preguntar la ruta de una para ver por qué.

**0.1 (11-nov-2026): compila y pasan 7 pruebas** (`LibraryTest`: categorías, órdenes, juntar y
separar). APK firmado en `~/Documentos/FotoDroid-0.1.apk` (5,9 MB, sólo arm64). **Sin probar en el
móvil.**

Hecho:
- Lectura de MediaStore (todos los volúmenes, imágenes y vídeos), refresco automático con un
  `ContentObserver` (agrupado 800 ms). Permiso **acceso a todos los archivos**
  (`MANAGE_EXTERNAL_STORAGE`): se borra sin el diálogo del sistema.
- Tres categorías por carpeta: normal, **ocultas** (pantalla aparte desde el ojo tachado) e
  **ignoradas** (Ajustes → Carpetas ignoradas; **ignorar una carpeta ignora sus subcarpetas**).
  Cada una con **su propio orden** (mantener pulsada y arrastrar). Lo que no tiene sitio guardado sale **delante**, lo más reciente primero
  (carpetas nuevas o recién pasadas de categoría).
- Selección múltiple (pulsación larga) de carpetas: ocultar / mostrar / ignorar / **juntar** /
  separar / renombrar grupo / eliminar.
- **Juntar carpetas**: grupo lógico `g:<id>` cuyas fotos forman una sola lista por fecha; hereda
  categoría y sitio de la primera; se puede juntar un grupo con más carpetas y separar.
- Cuadrícula de archivos (columnas en Ajustes), asa de desplazamiento rápido, selección múltiple
  para compartir y borrar. El scroll se conserva (`AppViewModel.gridState`) y al volver del visor
  se ve la última foto.
- Visor: `HorizontalPager` con **telephoto** (zoom hasta 8×, submuestreo de imágenes enormes,
  GIF/WebP animados con el `ImageDecoder` de Coil, HEIF/AVIF del sistema). Miniatura como
  marcador mientras carga. El visor va **encima** de la cuadrícula (no se recompone al volver).
- Vídeo: **un solo ExoPlayer** (Media3 1.5.1) enganchado a la página asentada, en `TextureView`
  para poder ampliarlo; **FFmpeg de Jellyfin** (`media3-ffmpeg-decoder`) como último recurso para
  audio (AC3, DTS, TrueHD…). Barra mínima siempre visible abajo, con zona táctil de 40 dp:
  tocar salta, arrastrar recorre (busca al keyframe mientras arrastra y exacto al soltar). Si no
  se puede reproducir: «Abrir con…».
- Bloqueo: `BiometricPrompt` con huella **o PIN/patrón del móvil**, para toda la app y/o sólo
  para las ocultas. Se vuelve a pedir tras 15 s fuera. Con bloqueo, sin captura en Recientes.
- Insets: pantallas con `Scaffold`+`TopAppBar`; en el visor la barra superior usa
  `statusBarsIgnoringVisibility ∪ displayCutout` (no salta al ocultar las barras) y la de vídeo
  va por encima de la barra de gestos.

Falta / ideas: probar en el móvil (sobre todo MKV/AVI con audio raro, HEIF grandes, borrar con
MediaStore), abrir archivos desde otras apps (`ACTION_VIEW`), vídeo con códec de vídeo no soportado por el Pixel (MPEG-2, WMV: FFmpeg de Jellyfin
sólo trae audio; la alternativa sería libVLC), papelera en vez de borrar para siempre.

## Datos

`files/config.json` (`ConfigStore`): categorías, los tres órdenes, grupos y ajustes. La lógica es
pura (`Config`, `Library.build`) y está probada en `app/src/test`.

## Compilar y firmar

`Tools/instalar-entorno.sh` (JDK 17, SDK y Gradle en `~/Android`; la plataforma 37 la baja Gradle
sola). Después: `./gradlew testDebugUnitTest lintDebug assembleRelease` → `app/build/outputs/apk/release/app-release.apk`,
y copiar a `~/Documentos/FotoDroid-<versión>.apk`.

**Clave de release** (creada el 11-nov-2026 en el portátil; en `plex` no está): `~/.android/claves/fotodroid-release.jks`
con sus datos en `~/.android/claves/fotodroid-release.properties` (lo lee `app/build.gradle.kts`).
Fuera del repo, con copia en `~/Documentos/ClavesAndroid` desde el 13-nov-2026: **si se pierde, no se puede actualizar la app instalada**. Certificado
`CN=Bruno, O=FotoDroid, C=ES`, SHA-256
`4A:31:10:FA:F8:5B:56:DA:40:25:EF:68:B1:31:62:9D:A4:12:5C:C1:B0:66:38:50:48:58:7E:7C:C0:FC:70:73`.
Ojo: en esta máquina no estaban las claves de las otras apps (se firmaron en otro equipo).

`applicationId`: `es.altamirano.foto`. Subir `versionCode` en cada APK que se instale.
