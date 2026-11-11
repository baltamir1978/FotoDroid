# FotoDroid

Galería de fotos y vídeos para Android **por carpetas**, rápida y minimalista. Sin álbumes y sin
edición: ver, ampliar, compartir y borrar.

**Estado: 0.1** (11-nov-2026), sin probar todavía en el móvil. Pensada para un Pixel 11 Pro con
Android 17 (no funciona por debajo de Android 15).

## Qué hace

- **Carpetas reales** del almacenamiento, con su portada y número de archivos.
- **Ocultas**: carpetas aparte que sólo se ven al entrar en «Ocultas» (con huella si se quiere).
- **Ignoradas**: carpetas (y sus subcarpetas) que no aparecen en ningún sitio.
- **Orden personalizado** arrastrando, distinto para las normales, las ocultas y las ignoradas.
- **Juntar carpetas**: varias carpetas se ven como una sola, con todos sus archivos seguidos por
  fecha. Se pueden separar después.
- Selección individual o múltiple de carpetas y de archivos.
- Visor con **zoom** (también en vídeos), imágenes enormes sin perder detalle, GIF animados, HEIF,
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
Reorderable (Apache 2.0), decodificador FFmpeg de Jellyfin (GPL 3.0 por FFmpeg).
