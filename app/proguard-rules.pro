# Media3, Coil y telephoto traen sus reglas. El decodificador FFmpeg se carga por reflexión.
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keepclassmembers class * { native <methods>; }
