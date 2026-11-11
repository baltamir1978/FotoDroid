# FotoDroid

Galería de fotos y vídeos para Android **por carpetas**, rápida y minimalista. Sin álbumes y sin
edición: ver, ampliar, compartir y borrar.

**Estado: 0.7** (11-nov-2026), en uso en el móvil. Pensada para un Pixel 11 Pro con
Android 17 (no funciona por debajo de Android 15).

## Qué hace

- **Carpetas reales** del almacenamiento, con su portada y número de archivos. Además de lo que
  indexa el sistema, recorre el almacenamiento y encuentra también las carpetas con `.nomedia` (y,
  si se activa, las que empiezan por punto), sin traerse la basura de las apps: papelera de Google
  Fotos, cachés, miniaturas, stickers, iconos ni las carpetas de apps del sistema en `Android/media`.
- **Ocultas**: carpetas aparte que sólo se ven al entrar en «Ocultas» (con huella si se quiere).
- **Ignoradas**: carpetas (y sus subcarpetas) que no aparecen en ningún sitio.
- **Orden personalizado**: mantener pulsada una carpeta y arrastrarla, distinto para las normales, las ocultas y las ignoradas.
- **Juntar carpetas**: varias carpetas se ven como una sola, con todos sus archivos seguidos por
  fecha. Juntar carpetas con un grupo que ya existe las añade a ese grupo (con su nombre). Se
  pueden separar después.
- Selección individual o múltiple de carpetas y de archivos, con las acciones en una barra flotante.
- **Pellizcar** en las cuadrículas para cambiar el número de columnas; miniaturas nítidas a cualquier tamaño.
- Diseño Material 3 actual: colores del fondo de pantalla, fuentes del Pixel, título grande plegable,
  **barras de cristal** translúcidas y desenfocadas e iconos redondeados en círculos tonales.
- Visor con **zoom** (también en vídeos; doble toque a 2,5×), deslizar hacia abajo para cerrar, imágenes enormes sin perder detalle, GIF animados, HEIF,
  AVIF, WebP, RAW que entienda el sistema…
- Vídeo con Media3/ExoPlayer: MP4, MOV, MKV, WebM, AVI, TS, 3GP…, con FFmpeg para el audio que el
  móvil no sabe decodificar (AC3, DTS…). Una **barra mínima** abajo para saltar y recorrer el
  vídeo arrastrando mientras se reproduce.
- **Bloqueo** con huella o PIN del móvil (toda la app o sólo las ocultas).
- Borrar archivos y carpetas.

## Compilar

```sh
Tools/instalar-entorno.sh       # JDK 17, Android SDK y Gradle en ~/Android, sin sudo
./gradlew testDebugUnitTest assembleRelease
```

La release se firma con una clave que no está en el repositorio (ver `CLAUDE.md`); sin ella sale
sin firmar.

Necesita el permiso de **acceso a todos los archivos** (la app lo pide al abrirla).

## Licencias

Bibliotecas: AndroidX/Media3 (Apache 2.0), Coil (Apache 2.0), telephoto (Apache 2.0),
Reorderable (Apache 2.0), Haze (Apache 2.0), decodificador FFmpeg de Jellyfin (GPL 3.0 por FFmpeg).
