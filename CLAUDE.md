# FotoDroid — contexto del repositorio

Galería de fotos y vídeos **por carpetas** (sin álbumes, sin edición), escrita desde cero en
**Kotlin + Jetpack Compose**. Sólo para el móvil de Bruno: **Pixel 11 Pro con Android 17**, sin
compatibilidad hacia atrás (`minSdk 35`), autofirmada e instalada a mano. Inspiración (no código):
Fossify Gallery y QP Gallery.

## Estado

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
  Cada una con **su propio orden** (botón ⇅, arrastrar con pulsación larga; también «por nombre»
  / «por fecha»). Lo que no tiene sitio guardado sale **delante**, lo más reciente primero
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

`Tools/instalar-entorno.sh` (JDK 17, SDK 35 y Gradle en `~/Android`). Después:
`./gradlew testDebugUnitTest assembleRelease` → `app/build/outputs/apk/release/app-release.apk`,
y copiar a `~/Documentos/FotoDroid-<versión>.apk`.

**Clave de release** (creada el 11-nov-2026 en este portátil): `~/.android/claves/fotodroid-release.jks`
con sus datos en `~/.android/claves/fotodroid-release.properties` (lo lee `app/build.gradle.kts`).
Fuera del repo y sin copia: **si se pierde, no se puede actualizar la app instalada**. Certificado
`CN=Bruno, O=FotoDroid, C=ES`, SHA-256
`4A:31:10:FA:F8:5B:56:DA:40:25:EF:68:B1:31:62:9D:A4:12:5C:C1:B0:66:38:50:48:58:7E:7C:C0:FC:70:73`.
Ojo: en esta máquina no estaban las claves de las otras apps (se firmaron en otro equipo).

`applicationId`: `es.altamirano.foto`. Subir `versionCode` en cada APK que se instale.
