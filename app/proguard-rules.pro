# ==========================================
# Room Database Rules
# ==========================================
# Protect all Room Entities and DAOs from being obfuscated or removed,
# as Room generates code that expects these exact names.
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep TypeConverters (like your ShuffleMode enum converter)
-keep @androidx.room.TypeConverters class *
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Keep the Enums used in Room (Crucial for ShuffleMode)
-keepclassmembers enum com.festerhead.cygnusplayer.data.entities.ShuffleMode {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==========================================
# Jetpack Glance (Widgets) Rules
# ==========================================
# Glance AppWidgets are instantiated via broadcast receivers declared in the manifest.
# AAPT2 usually keeps manifest-declared classes, but it's safest to explicitly keep Glance definitions.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# ==========================================
# Media3 & ExoPlayer Rules
# ==========================================
# Media3 mostly handles its own obfuscation, but you are utilizing a custom
# MediaLibraryService. Keep the service and callback structures.
-keep class com.festerhead.cygnusplayer.service.CygnusPlaybackService { *; }

# Prevent R8 from messing with Media3's internal reflection if it misses anything
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ==========================================
# Coroutines & Serialization
# ==========================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
