# Keep Room entities/DAOs metadata
-keep class com.nono.iptv.data.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
