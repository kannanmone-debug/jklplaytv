# BNJ MEDIA PRO TV — Targeted, Evidence-Based R8 / ProGuard Configuration
# Official AndroidX AAR consumer rules are merged automatically for:
# Media3 (ExoPlayer, Session, UI), Room, Lifecycle, Compose, Coil, and DataStore.

# Preserve stack trace line numbers for production crash diagnostics while hiding local file paths
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata required for Room KSP generated implementations and Compose runtime
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,Signature,InnerClasses,EnclosingMethod

# Keep Room @Database subclasses and KSP-generated *_Impl classes instantiated via reflection by Room.databaseBuilder
-keep class com.example.data.local.BnjMediaDatabase { *; }
-keep class com.example.data.local.BnjMediaDatabase_Impl { *; }
-keep class com.example.movies.data.BnjMovieDatabase { *; }
-keep class com.example.movies.data.BnjMovieDatabase_Impl { *; }

# Keep Media3 foreground MediaSessionService and custom AudioProcessor entry points
-keep class com.example.player.service.BnjMediaSessionService { *; }
-keep class com.example.equalizer.BnjAudioDspProcessor {
    public <init>(...);
}
